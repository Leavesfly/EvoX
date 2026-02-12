package io.leavesfly.evox.channels.feishu;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.leavesfly.evox.channels.core.AbstractChannel;
import io.leavesfly.evox.channels.core.ChannelConfig;
import io.leavesfly.evox.channels.core.ChannelMessage;
import io.leavesfly.evox.channels.core.MessageContentType;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class FeishuChannel extends AbstractChannel {
    private static final String BASE_URL = "https://open.feishu.cn/open-apis";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    private final FeishuConfig config;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private ScheduledExecutorService scheduler;
    private String tenantAccessToken;
    private long tokenExpireTime;
    
    public FeishuChannel(FeishuConfig config) {
        this.config = config;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public String getChannelId() {
        return config.getChannelId();
    }
    
    @Override
    public String getChannelName() {
        return config.getChannelName();
    }
    
    @Override
    public ChannelConfig getConfig() {
        return config;
    }
    
    @Override
    protected void doStart() {
        log.info("Starting Feishu channel: {}", getChannelId());
        
        scheduler = Executors.newSingleThreadScheduledExecutor();
        
        scheduler.scheduleAtFixedRate(() -> {
            try {
                refreshTokenIfNeeded();
                pollMessages();
            } catch (Exception e) {
                log.error("Error polling messages from Feishu", e);
            }
        }, 0, 5, TimeUnit.SECONDS);
        
        log.info("Feishu channel started: {}", getChannelId());
    }
    
    @Override
    protected void doStop() {
        log.info("Stopping Feishu channel: {}", getChannelId());
        
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        log.info("Feishu channel stopped: {}", getChannelId());
    }
    
    @Override
    public CompletableFuture<Void> sendMessage(String targetId, ChannelMessage message) {
        return CompletableFuture.runAsync(() -> {
            try {
                refreshTokenIfNeeded();
                
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("receive_id", targetId);
                requestBody.put("msg_type", "text");
                
                Map<String, String> content = new HashMap<>();
                content.put("text", message.getContent());
                requestBody.put("content", objectMapper.writeValueAsString(content));
                
                Request request = new Request.Builder()
                        .url(BASE_URL + "/im/v1/messages")
                        .addHeader("Authorization", "Bearer " + tenantAccessToken)
                        .post(RequestBody.create(objectMapper.writeValueAsString(requestBody), JSON))
                        .build();
                
                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Failed to send message to Feishu: " + response.code());
                    }
                    
                    JsonNode responseBody = objectMapper.readTree(response.body().string());
                    if (responseBody.has("code") && responseBody.get("code").asInt() != 0) {
                        throw new IOException("Feishu API error: " + responseBody.get("msg").asText());
                    }
                    
                    log.info("Message sent to Feishu: targetId={}", targetId);
                }
            } catch (Exception e) {
                log.error("Error sending message to Feishu", e);
                throw new RuntimeException(e);
            }
        });
    }
    
    private void refreshTokenIfNeeded() throws IOException {
        if (tenantAccessToken != null && System.currentTimeMillis() < tokenExpireTime) {
            return;
        }
        
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("app_id", config.getAppId());
        requestBody.put("app_secret", config.getAppSecret());
        
        Request request = new Request.Builder()
                .url(BASE_URL + "/auth/v3/tenant_access_token/internal")
                .post(RequestBody.create(objectMapper.writeValueAsString(requestBody), JSON))
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to get tenant access token: " + response.code());
            }
            
            JsonNode responseBody = objectMapper.readTree(response.body().string());
            if (responseBody.has("code") && responseBody.get("code").asInt() != 0) {
                throw new IOException("Feishu auth error: " + responseBody.get("msg").asText());
            }
            
            tenantAccessToken = responseBody.get("tenant_access_token").asText();
            int expireSeconds = responseBody.get("expire").asInt();
            tokenExpireTime = System.currentTimeMillis() + (expireSeconds - 300) * 1000L;
            
            log.info("Feishu tenant access token refreshed, expires in {} seconds", expireSeconds);
        }
    }
    
    private void pollMessages() throws IOException {
        refreshTokenIfNeeded();
        
        Request request = new Request.Builder()
                .url(BASE_URL + "/im/v1/messages?container_access_token_type=tenant")
                .addHeader("Authorization", "Bearer " + tenantAccessToken)
                .get()
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("Failed to poll messages from Feishu: {}", response.code());
                return;
            }
            
            JsonNode responseBody = objectMapper.readTree(response.body().string());
            if (responseBody.has("code") && responseBody.get("code").asInt() != 0) {
                log.error("Feishu poll error: {}", responseBody.get("msg").asText());
                return;
            }
            
            JsonNode items = responseBody.get("data").get("items");
            if (items != null && items.isArray()) {
                for (JsonNode item : items) {
                    processMessage(item);
                }
            }
        }
    }
    
    private void processMessage(JsonNode messageNode) {
        try {
            String senderId = messageNode.get("sender").get("sender_id").get("open_id").asText();
            String senderName = messageNode.get("sender").has("sender_name") ? 
                    messageNode.get("sender").get("sender_name").asText() : senderId;
            
            if (!isAllowed(senderId)) {
                log.debug("Message from {} not in allow list, ignored", senderId);
                return;
            }
            
            String msgType = messageNode.get("msg_type").asText();
            if (!"text".equals(msgType)) {
                log.debug("Unsupported message type: {}", msgType);
                return;
            }
            
            JsonNode contentNode = objectMapper.readTree(messageNode.get("content").asText());
            String content = contentNode.get("text").asText();
            
            ChannelMessage channelMessage = ChannelMessage.builder()
                    .channelId(getChannelId())
                    .senderId(senderId)
                    .senderName(senderName)
                    .targetId(getChannelId())
                    .content(content)
                    .contentType(MessageContentType.TEXT)
                    .metadata(new HashMap<>())
                    .build();
            
            notifyMessage(channelMessage);
            log.info("Received message from Feishu: senderId={}, content={}", senderId, content);
            
        } catch (Exception e) {
            log.error("Error processing Feishu message", e);
        }
    }
    
    private boolean isAllowed(String senderId) {
        if (config.getAllowFrom() == null || config.getAllowFrom().isEmpty()) {
            return true;
        }
        return config.getAllowFrom().contains(senderId);
    }
}
