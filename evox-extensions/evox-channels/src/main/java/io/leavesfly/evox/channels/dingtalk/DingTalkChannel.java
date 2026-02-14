package io.leavesfly.evox.channels.dingtalk;

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
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

@Slf4j
public class DingTalkChannel extends AbstractChannel {

    private static final String DINGTALK_API_BASE = "https://api.dingtalk.com";
    private static final String OAPI_BASE = "https://oapi.dingtalk.com";

    private final DingTalkConfig config;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private volatile String accessToken;
    private volatile long tokenExpireTime = 0;

    public DingTalkChannel(DingTalkConfig config) {
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
        return config.getChannelId() != null ? config.getChannelId() : "dingtalk";
    }

    @Override
    public String getChannelName() {
        return config.getChannelName() != null ? config.getChannelName() : "DingTalk";
    }

    @Override
    public ChannelConfig getConfig() {
        return config;
    }

    @Override
    protected void doStart() throws Exception {
        if (config.getAppKey() == null || config.getAppSecret() == null) {
            throw new IllegalStateException("DingTalk appKey and appSecret are required");
        }
        refreshAccessToken();
        log.info("DingTalk channel started, robotCode: {}", config.getRobotCode());
    }

    @Override
    protected void doStop() throws Exception {
        accessToken = null;
        tokenExpireTime = 0;
        log.info("DingTalk channel stopped");
    }

    @Override
    public CompletableFuture<Void> sendMessage(String targetId, ChannelMessage message) {
        return CompletableFuture.runAsync(() -> {
            try {
                ensureAccessToken();
                sendDingTalkMessage(targetId, message.getContent());
            } catch (Exception e) {
                log.error("Failed to send DingTalk message to {}", targetId, e);
                throw new CompletionException(e);
            }
        });
    }

    public void handleCallback(String requestBody) {
        try {
            JsonNode root = objectMapper.readTree(requestBody);

            String msgType = root.path("msgtype").asText("");
            if (!"text".equals(msgType)) {
                log.debug("Ignoring non-text DingTalk message type: {}", msgType);
                return;
            }

            String content = root.path("text").path("content").asText("").trim();
            String senderId = root.path("senderStaffId").asText(
                    root.path("senderId").asText(""));
            String senderNick = root.path("senderNick").asText("");
            String conversationId = root.path("conversationId").asText("");
            String conversationType = root.path("conversationType").asText("");

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("conversationId", conversationId);
            metadata.put("conversationType", conversationType);
            metadata.put("msgId", root.path("msgId").asText(""));
            if (root.has("robotCode")) {
                metadata.put("robotCode", root.path("robotCode").asText());
            }

            String replyTargetId = "1".equals(conversationType) ? senderId : conversationId;

            ChannelMessage channelMessage = ChannelMessage.builder()
                    .channelId(getChannelId())
                    .senderId(senderId)
                    .senderName(senderNick)
                    .targetId(replyTargetId)
                    .content(content)
                    .contentType(MessageContentType.TEXT)
                    .metadata(metadata)
                    .build();

            notifyMessage(channelMessage);
        } catch (Exception e) {
            log.error("Error handling DingTalk callback", e);
        }
    }

    private void sendDingTalkMessage(String targetId, String text) throws IOException {
        Map<String, Object> body = new HashMap<>();
        body.put("robotCode", config.getRobotCode());
        body.put("msgKey", "sampleText");

        Map<String, String> msgParam = new HashMap<>();
        msgParam.put("content", text);
        body.put("msgParam", objectMapper.writeValueAsString(msgParam));

        if (targetId.startsWith("cid")) {
            body.put("openConversationId", targetId);
            postDingTalkApi("/v1.0/robot/groupMessages/send", body);
        } else {
            body.put("userIds", new String[]{targetId});
            postDingTalkApi("/v1.0/robot/oToMessages/batchSend", body);
        }
    }

    private void postDingTalkApi(String path, Map<String, Object> body) throws IOException {
        RequestBody requestBody = RequestBody.create(
                objectMapper.writeValueAsString(body),
                MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(DINGTALK_API_BASE + path)
                .addHeader("x-acs-dingtalk-access-token", accessToken)
                .post(requestBody)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                log.error("DingTalk API call failed: {} - {}", response.code(), responseBody);
            }
        }
    }

    private void ensureAccessToken() throws IOException {
        if (accessToken == null || System.currentTimeMillis() >= tokenExpireTime) {
            refreshAccessToken();
        }
    }

    private synchronized void refreshAccessToken() throws IOException {
        if (accessToken != null && System.currentTimeMillis() < tokenExpireTime) {
            return;
        }

        Map<String, String> body = new HashMap<>();
        body.put("appKey", config.getAppKey());
        body.put("appSecret", config.getAppSecret());

        RequestBody requestBody = RequestBody.create(
                objectMapper.writeValueAsString(body),
                MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(OAPI_BASE + "/v2/accessToken")
                .post(requestBody)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("Failed to get DingTalk access token: " + response.code());
            }

            JsonNode root = objectMapper.readTree(response.body().string());
            this.accessToken = root.path("accessToken").asText();
            long expireIn = root.path("expireIn").asLong(7200);
            this.tokenExpireTime = System.currentTimeMillis() + (expireIn - 300) * 1000;
            log.info("DingTalk access token refreshed, expires in {}s", expireIn);
        }
    }
}
