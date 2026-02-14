package io.leavesfly.evox.channels.qq;

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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class QQChannel extends AbstractChannel {

    private static final String PROD_API_BASE = "https://api.sgroup.qq.com";
    private static final String SANDBOX_API_BASE = "https://sandbox.api.sgroup.qq.com";
    private static final int INTENT_C2C_MESSAGE_CREATE = 1 << 25;
    private static final int TOKEN_EXPIRE_SECONDS = 7200;
    private static final int HEARTBEAT_INTERVAL_MS = 30000;

    private final QQConfig config;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private ScheduledExecutorService tokenRefreshExecutor;
    private ScheduledExecutorService heartbeatExecutor;
    private WebSocket webSocket;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private String accessToken;
    private final AtomicLong tokenExpireTime = new AtomicLong(0);
    private final AtomicInteger msgSeq = new AtomicInteger(1);
    private final AtomicLong lastHeartbeatTime = new AtomicLong(System.currentTimeMillis());
    private String sessionId;
    private String currentWebSocketUrl;

    public QQChannel(QQConfig config) {
        this.config = config;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .pingInterval(30, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getChannelId() {
        return config.getChannelId() != null ? config.getChannelId() : "qq";
    }

    @Override
    public String getChannelName() {
        return config.getChannelName() != null ? config.getChannelName() : "QQ";
    }

    @Override
    public ChannelConfig getConfig() {
        return config;
    }

    @Override
    protected void doStart() throws Exception {
        if (config.getAppId() == null || config.getAppId().isEmpty()) {
            throw new IllegalStateException("QQ appId is required");
        }
        if (config.getAppSecret() == null || config.getAppSecret().isEmpty()) {
            throw new IllegalStateException("QQ appSecret is required");
        }

        running.set(true);

        refreshAccessToken();
        connectWebSocket();
        startTokenRefreshScheduler();
        startHeartbeatScheduler();

        log.info("QQ channel started successfully, sandboxMode: {}", config.isSandboxMode());
    }

    @Override
    protected void doStop() throws Exception {
        running.set(false);

        if (webSocket != null) {
            webSocket.close(1000, "Normal closure");
            webSocket = null;
        }

        if (tokenRefreshExecutor != null) {
            tokenRefreshExecutor.shutdown();
            if (!tokenRefreshExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                tokenRefreshExecutor.shutdownNow();
            }
        }

        if (heartbeatExecutor != null) {
            heartbeatExecutor.shutdown();
            if (!heartbeatExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                heartbeatExecutor.shutdownNow();
            }
        }

        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
            httpClient.connectionPool().evictAll();
        }

        log.info("QQ channel stopped successfully");
    }

    @Override
    public CompletableFuture<Void> sendMessage(String targetId, ChannelMessage message) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (!isAllowed(targetId)) {
                    log.warn("Message from {} is not in allowFrom list, skipping", targetId);
                    return;
                }

                String content = message.getContent();
                if (content == null || content.isEmpty()) {
                    return;
                }

                sendQQMessage(targetId, content);
            } catch (Exception e) {
                log.error("Failed to send QQ message to {}", targetId, e);
                throw new CompletionException(e);
            }
        });
    }

    private String getApiBaseUrl() {
        return config.isSandboxMode() ? SANDBOX_API_BASE : PROD_API_BASE;
    }

    private void refreshAccessToken() throws IOException {
        String url = getApiBaseUrl() + "/getAppAccessToken";

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("appId", config.getAppId());
        requestBody.put("clientSecret", config.getAppSecret());

        RequestBody body = RequestBody.create(
                objectMapper.writeValueAsString(requestBody),
                MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("Failed to get access token: " + response.code());
            }

            JsonNode root = objectMapper.readTree(response.body().string());
            accessToken = root.path("access_token").asText(null);
            long expiresIn = root.path("expires_in").asLong(TOKEN_EXPIRE_SECONDS);

            if (accessToken == null || accessToken.isEmpty()) {
                throw new IOException("Access token is null in response");
            }

            tokenExpireTime.set(System.currentTimeMillis() + expiresIn * 1000);
            log.info("QQ access token refreshed, expires in {} seconds", expiresIn);
        }
    }

    private void connectWebSocket() throws IOException {
        String url = getApiBaseUrl() + "/gateway";

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "QQBot " + accessToken)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("Failed to get gateway URL: " + response.code());
            }

            JsonNode root = objectMapper.readTree(response.body().string());
            currentWebSocketUrl = root.path("url").asText(null);

            if (currentWebSocketUrl == null || currentWebSocketUrl.isEmpty()) {
                throw new IOException("WebSocket URL is null in response");
            }

            log.info("Got WebSocket gateway URL: {}", currentWebSocketUrl);
        }

        connectToWebSocket(currentWebSocketUrl);
    }

    private void connectToWebSocket(String wsUrl) {
        Request request = new Request.Builder()
                .url(wsUrl)
                .build();

        WebSocketListener listener = new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                log.info("QQ WebSocket connection opened");
                sendIdentify(webSocket);
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                handleWebSocketMessage(text);
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                log.info("QQ WebSocket closing: {} - {}", code, reason);
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                log.info("QQ WebSocket closed: {} - {}", code, reason);
                if (running.get()) {
                    scheduleReconnect();
                }
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                log.error("QQ WebSocket failure", t);
                if (running.get()) {
                    scheduleReconnect();
                }
            }
        };

        webSocket = httpClient.newWebSocket(request, listener);
    }

    private void sendIdentify(WebSocket ws) {
        try {
            Map<String, Object> identify = new HashMap<>();
            identify.put("op", 2);

            Map<String, Object> data = new HashMap<>();
            data.put("token", "QQBot " + accessToken);
            data.put("intents", INTENT_C2C_MESSAGE_CREATE);
            data.put("shard", new int[]{0, 1});

            identify.put("d", data);

            ws.send(objectMapper.writeValueAsString(identify));
            log.info("Identify packet sent");
        } catch (Exception e) {
            log.error("Failed to send identify", e);
        }
    }

    private void handleWebSocketMessage(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            int opCode = root.path("op").asInt();
            JsonNode data = root.path("d");

            switch (opCode) {
                case 0:
                    handleDispatch(data);
                    break;
                case 1:
                    log.debug("Received heartbeat ack");
                    lastHeartbeatTime.set(System.currentTimeMillis());
                    break;
                case 10:
                    handleHello(data);
                    break;
                case 11:
                    log.debug("Heartbeat acknowledged");
                    lastHeartbeatTime.set(System.currentTimeMillis());
                    break;
                default:
                    log.debug("Unhandled opcode: {}", opCode);
            }
        } catch (Exception e) {
            log.error("Failed to handle WebSocket message", e);
        }
    }

    private void handleHello(JsonNode data) {
        JsonNode heartbeatInterval = data.path("heartbeat_interval");
        if (heartbeatInterval.isInt()) {
            log.info("Received Hello, heartbeat interval: {}ms", heartbeatInterval.asInt());
        }
    }

    private void handleDispatch(JsonNode data) {
        String eventType = data.path("t").asText();

        if ("C2C_MESSAGE_CREATE".equals(eventType)) {
            handleC2CMessageCreate(data);
        } else if ("READY".equals(eventType)) {
            sessionId = data.path("session_id").asText();
            log.info("Received READY, session_id: {}", sessionId);
        }
    }

    private void handleC2CMessageCreate(JsonNode data) {
        JsonNode author = data.path("author");
        String openid = author.path("openid").asText();

        if (!isAllowed(openid)) {
            log.debug("Message from {} is not in allowFrom list, ignoring", openid);
            return;
        }

        String content = data.path("content").asText("");
        String senderName = author.path("username").asText("");
        String msgId = data.path("id").asText("");

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("msgId", msgId);
        metadata.put("openid", openid);
        metadata.put("timestamp", data.path("timestamp").asLong());

        ChannelMessage channelMessage = ChannelMessage.builder()
                .channelId(getChannelId())
                .senderId(openid)
                .senderName(senderName)
                .targetId(openid)
                .content(content)
                .contentType(MessageContentType.TEXT)
                .metadata(metadata)
                .build();

        notifyMessage(channelMessage);
        log.debug("Received C2C message from {}: {}", openid, content);
    }

    private void sendHeartbeat(WebSocket ws) {
        try {
            Map<String, Object> heartbeat = new HashMap<>();
            heartbeat.put("op", 1);
            heartbeat.put("d", System.currentTimeMillis());

            ws.send(objectMapper.writeValueAsString(heartbeat));
            log.debug("Heartbeat sent");
        } catch (Exception e) {
            log.error("Failed to send heartbeat", e);
        }
    }

    private void startHeartbeatScheduler() {
        heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(
                r -> new Thread(r, "qq-heartbeat"));

        heartbeatExecutor.scheduleWithFixedDelay(
                () -> {
                    if (webSocket != null && running.get()) {
                        sendHeartbeat(webSocket);
                    }
                },
                HEARTBEAT_INTERVAL_MS,
                HEARTBEAT_INTERVAL_MS,
                TimeUnit.MILLISECONDS);
    }

    private void startTokenRefreshScheduler() {
        tokenRefreshExecutor = Executors.newSingleThreadScheduledExecutor(
                r -> new Thread(r, "qq-token-refresh"));

        long refreshDelay = (TOKEN_EXPIRE_SECONDS - 300) * 1000L;

        tokenRefreshExecutor.scheduleWithFixedDelay(
                () -> {
                    if (running.get()) {
                        try {
                            refreshAccessToken();
                        } catch (Exception e) {
                            log.error("Failed to refresh access token", e);
                        }
                    }
                },
                refreshDelay,
                refreshDelay,
                TimeUnit.MILLISECONDS);
    }

    private void scheduleReconnect() {
        ScheduledExecutorService reconnectExecutor = Executors.newSingleThreadScheduledExecutor();
        reconnectExecutor.schedule(() -> {
            if (running.get()) {
                try {
                    log.info("Attempting to reconnect WebSocket...");
                    connectWebSocket();
                } catch (Exception e) {
                    log.error("Failed to reconnect", e);
                }
            }
            reconnectExecutor.shutdown();
        }, 5, TimeUnit.SECONDS);
    }

    private void sendQQMessage(String openid, String content) throws IOException {
        String url = getApiBaseUrl() + "/v2/users/" + openid + "/messages";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("content", content);
        requestBody.put("msg_type", 0);
        requestBody.put("msg_id", "");
        requestBody.put("msg_seq", msgSeq.getAndIncrement());

        RequestBody body = RequestBody.create(
                objectMapper.writeValueAsString(requestBody),
                MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Authorization", "QQBot " + accessToken)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                throw new IOException("Failed to send QQ message: " + response.code() + " - " + responseBody);
            }

            JsonNode root = objectMapper.readTree(response.body().string());
            log.debug("QQ message sent successfully, response: {}", root);
        }
    }

    private boolean isAllowed(String openid) {
        if (config.getAllowFrom() == null || config.getAllowFrom().isEmpty()) {
            return true;
        }
        return config.getAllowFrom().contains(openid);
    }
}
