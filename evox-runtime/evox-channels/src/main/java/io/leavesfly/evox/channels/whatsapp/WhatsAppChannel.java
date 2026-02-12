package io.leavesfly.evox.channels.whatsapp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.leavesfly.evox.channels.core.AbstractChannel;
import io.leavesfly.evox.channels.core.ChannelConfig;
import io.leavesfly.evox.channels.core.ChannelMessage;
import io.leavesfly.evox.channels.core.MessageContentType;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class WhatsAppChannel extends AbstractChannel {
    private static final String BASE_URL = "https://graph.facebook.com/v18.0";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    private final WhatsAppConfig config;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public WhatsAppChannel(WhatsAppConfig config) {
        this.config = config;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
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
        log.info("Starting WhatsApp channel: {}", getChannelId());
        log.info("WhatsApp channel started: {}", getChannelId());
        log.info("Webhook endpoint: {}", config.getWebhookPath());
    }
    
    @Override
    protected void doStop() {
        log.info("Stopping WhatsApp channel: {}", getChannelId());
        log.info("WhatsApp channel stopped: {}", getChannelId());
    }
    
    @Override
    public CompletableFuture<Void> sendMessage(String targetId, ChannelMessage message) {
        return CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("messaging_product", "whatsapp");
                requestBody.put("to", targetId);
                requestBody.put("type", "text");
                
                Map<String, String> textContent = new HashMap<>();
                textContent.put("body", message.getContent());
                requestBody.put("text", textContent);
                
                Request request = new Request.Builder()
                        .url(BASE_URL + "/" + config.getPhoneNumberId() + "/messages")
                        .addHeader("Authorization", "Bearer " + config.getAccessToken())
                        .post(RequestBody.create(objectMapper.writeValueAsString(requestBody), JSON))
                        .build();
                
                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Failed to send message to WhatsApp: " + response.code());
                    }
                    
                    JsonNode responseBody = objectMapper.readTree(response.body().string());
                    if (responseBody.has("error")) {
                        throw new IOException("WhatsApp API error: " + responseBody.get("error").get("message").asText());
                    }
                    
                    log.info("Message sent to WhatsApp: targetId={}", targetId);
                }
            } catch (Exception e) {
                log.error("Error sending message to WhatsApp", e);
                throw new RuntimeException(e);
            }
        });
    }
    
    public void handleWebhookEvent(JsonNode payload) {
        try {
            JsonNode entry = payload.get("entry");
            if (entry == null || !entry.isArray() || entry.size() == 0) {
                return;
            }
            
            for (JsonNode entryNode : entry) {
                JsonNode changes = entryNode.get("changes");
                if (changes == null || !changes.isArray()) {
                    continue;
                }
                
                for (JsonNode changeNode : changes) {
                    JsonNode value = changeNode.get("value");
                    if (value == null) {
                        continue;
                    }
                    
                    JsonNode messages = value.get("messages");
                    if (messages == null || !messages.isArray()) {
                        continue;
                    }
                    
                    for (JsonNode messageNode : messages) {
                        processMessage(messageNode);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error processing WhatsApp webhook event", e);
        }
    }
    
    private void processMessage(JsonNode messageNode) {
        try {
            String from = messageNode.get("from").asText();
            String senderId = from;
            
            if (!isAllowed(from)) {
                log.debug("Message from {} not in allow list, ignored", from);
                return;
            }
            
            String senderName = from;
            
            JsonNode textNode = messageNode.get("text");
            if (textNode == null) {
                log.debug("Non-text message received, ignored");
                return;
            }
            
            String content = textNode.get("body").asText();
            
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
            log.info("Received message from WhatsApp: senderId={}, content={}", senderId, content);
            
        } catch (Exception e) {
            log.error("Error processing WhatsApp message", e);
        }
    }
    
    private boolean isAllowed(String senderId) {
        if (config.getAllowFrom() == null || config.getAllowFrom().isEmpty()) {
            return true;
        }
        return config.getAllowFrom().contains(senderId);
    }
}
