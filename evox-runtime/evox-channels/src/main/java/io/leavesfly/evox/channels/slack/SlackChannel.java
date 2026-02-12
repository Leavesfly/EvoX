package io.leavesfly.evox.channels.slack;

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
import java.util.concurrent.*;

@Slf4j
public class SlackChannel extends AbstractChannel {

    private static final String SLACK_API_BASE = "https://slack.com/api";
    private static final String CONNECTIONS_OPEN_URL = SLACK_API_BASE + "/apps.connections.open";
    private static final String CHAT_POST_MESSAGE_URL = SLACK_API_BASE + "/chat.postMessage";

    private final SlackConfig config;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private volatile boolean running = false;
    private WebSocket webSocket;
    private ScheduledExecutorService reconnectExecutor;

    public SlackChannel(SlackConfig config) {
        this.config = config;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getChannelId() {
        return config.getChannelId() != null ? config.getChannelId() : "slack";
    }

    @Override
    public String getChannelName() {
        return config.getChannelName() != null ? config.getChannelName() : "Slack";
    }

    @Override
    public ChannelConfig getConfig() {
        return config;
    }

    @Override
    protected void doStart() throws Exception {
        if (config.getBotToken() == null || config.getBotToken().isEmpty()) {
            throw new IllegalStateException("Slack bot token is required");
        }
        if (config.getAppToken() == null || config.getAppToken().isEmpty()) {
            throw new IllegalStateException("Slack app token is required");
        }

        running = true;
        connectWebSocket();
        log.info("Slack channel started");
    }

    @Override
    protected void doStop() throws Exception {
        running = false;

        if (reconnectExecutor != null) {
            reconnectExecutor.shutdown();
            if (!reconnectExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                reconnectExecutor.shutdownNow();
            }
        }

        if (webSocket != null) {
            webSocket.close(1000, "Closing");
        }

        log.info("Slack channel stopped");
    }

    @Override
    public CompletableFuture<Void> sendMessage(String targetId, ChannelMessage message) {
        return CompletableFuture.runAsync(() -> {
            try {
                String text = message.getContent();
                if (text == null || text.isEmpty()) {
                    return;
                }

                sendSlackMessage(targetId, text, message.getThreadId());
            } catch (Exception e) {
                log.error("Failed to send Slack message to {}", targetId, e);
                throw new CompletionException(e);
            }
        });
    }

    private void connectWebSocket() {
        try {
            String wsUrl = getWebSocketUrl();
            if (wsUrl == null || wsUrl.isEmpty()) {
                log.error("Failed to get Slack WebSocket URL");
                scheduleReconnect();
                return;
            }

            Request request = new Request.Builder()
                    .url(wsUrl)
                    .build();

            webSocket = httpClient.newWebSocket(request, new WebSocketListener() {
                @Override
                public void onOpen(WebSocket webSocket, Response response) {
                    log.info("Slack WebSocket connected");
                }

                @Override
                public void onMessage(WebSocket webSocket, String text) {
                    try {
                        JsonNode envelope = objectMapper.readTree(text);
                        String type = envelope.path("type").asText();
                        JsonNode payload = envelope.path("payload");

                        switch (type) {
                            case "events_api":
                                handleEventsApi(payload);
                                break;
                            case "disconnect":
                                log.warn("Slack disconnect received");
                                if (running) {
                                    scheduleReconnect();
                                }
                                break;
                            case "error":
                                log.error("Slack WebSocket error: {}", envelope.path("error").asText());
                                break;
                        }
                    } catch (Exception e) {
                        log.error("Error processing Slack WebSocket message", e);
                    }
                }

                @Override
                public void onClosing(WebSocket webSocket, int code, String reason) {
                    log.info("Slack WebSocket closing: {} - {}", code, reason);
                }

                @Override
                public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                    log.error("Slack WebSocket failure", t);
                    if (running) {
                        scheduleReconnect();
                    }
                }
            });
        } catch (Exception e) {
            log.error("Error connecting to Slack WebSocket", e);
            scheduleReconnect();
        }
    }

    private String getWebSocketUrl() throws IOException {
        Request request = new Request.Builder()
                .url(CONNECTIONS_OPEN_URL)
                .post(RequestBody.create("", MediaType.parse("application/json")))
                .addHeader("Authorization", "Bearer " + config.getAppToken())
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                log.error("Failed to open Slack connection: {}", response.code());
                return null;
            }

            JsonNode json = objectMapper.readTree(response.body().string());
            if (!json.path("ok").asBoolean(false)) {
                log.error("Slack API returned error: {}", json.path("error").asText());
                return null;
            }

            return json.path("url").asText();
        }
    }

    private void handleEventsApi(JsonNode payload) {
        try {
            String envelopeType = payload.path("envelope_id").asText();
            JsonNode event = payload.path("event");
            String eventType = event.path("type").asText();

            if ("message".equals(eventType)) {
                processMessage(event);
            }

            acknowledgeEvent(envelopeType);
        } catch (Exception e) {
            log.error("Error handling Slack events_api", e);
        }
    }

    private void processMessage(JsonNode event) {
        try {
            String text = event.path("text").asText(null);
            if (text == null || text.isEmpty()) {
                return;
            }

            JsonNode user = event.path("user");
            if (user.isMissingNode()) {
                return;
            }

            String userId = user.asText();
            String channelId = event.path("channel").asText();
            String botId = event.path("bot_id").asText();
            
            if (!botId.isEmpty()) {
                return;
            }

            if (config.getAllowFrom() != null && !config.getAllowFrom().isEmpty()) {
                if (!config.getAllowFrom().contains(channelId) && 
                    !config.getAllowFrom().contains(userId)) {
                    return;
                }
            }

            boolean shouldRespond = false;
            if ("mention".equals(config.getGroupPolicy())) {
                String botUserId = getBotUserId();
                if (botUserId != null && text.contains("<@" + botUserId + ">")) {
                    shouldRespond = true;
                }
            } else if ("open".equals(config.getGroupPolicy())) {
                shouldRespond = true;
            }

            if (!shouldRespond) {
                return;
            }

            String senderName = getUserName(userId);
            if (senderName == null || senderName.isEmpty()) {
                senderName = userId;
            }

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("slackTs", event.path("ts").asText());
            metadata.put("channelType", event.path("channel_type").asText());
            
            if (event.has("thread_ts")) {
                metadata.put("threadTs", event.path("thread_ts").asText());
            }
            
            if (event.has("parent_user_message_ts")) {
                metadata.put("threadTs", event.path("parent_user_message_ts").asText());
            }

            String threadId = event.has("thread_ts") ? event.path("thread_ts").asText() : null;

            ChannelMessage channelMessage = ChannelMessage.builder()
                    .channelId(getChannelId())
                    .senderId(userId)
                    .senderName(senderName)
                    .targetId(channelId)
                    .content(text)
                    .contentType(MessageContentType.TEXT)
                    .metadata(metadata)
                    .threadId(threadId)
                    .build();

            notifyMessage(channelMessage);
        } catch (Exception e) {
            log.error("Error processing Slack message", e);
        }
    }

    private void acknowledgeEvent(String envelopeId) {
        try {
            Map<String, Object> ack = new HashMap<>();
            ack.put("envelope_id", envelopeId);

            String json = objectMapper.writeValueAsString(ack);
            webSocket.send(json);
        } catch (Exception e) {
            log.error("Error acknowledging Slack event", e);
        }
    }

    private String getBotUserId() {
        try {
            Request request = new Request.Builder()
                    .url(SLACK_API_BASE + "/auth.test")
                    .post(RequestBody.create("", MediaType.parse("application/json")))
                    .addHeader("Authorization", "Bearer " + config.getBotToken())
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    return null;
                }

                JsonNode json = objectMapper.readTree(response.body().string());
                if (json.path("ok").asBoolean(false)) {
                    return json.path("user_id").asText();
                }
            }
        } catch (Exception e) {
            log.error("Error getting bot user ID", e);
        }
        return null;
    }

    private String getUserName(String userId) {
        try {
            Request request = new Request.Builder()
                    .url(SLACK_API_BASE + "/users.info?user=" + userId)
                    .get()
                    .addHeader("Authorization", "Bearer " + config.getBotToken())
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    return null;
                }

                JsonNode json = objectMapper.readTree(response.body().string());
                if (json.path("ok").asBoolean(false)) {
                    JsonNode user = json.path("user");
                    return user.path("real_name").asText(user.path("name").asText());
                }
            }
        } catch (Exception e) {
            log.error("Error getting user name for {}", userId, e);
        }
        return null;
    }

    private void sendSlackMessage(String channelId, String text, String threadTs) throws IOException {
        Map<String, Object> body = new HashMap<>();
        body.put("channel", channelId);
        body.put("text", text);
        
        if (threadTs != null && !threadTs.isEmpty()) {
            body.put("thread_ts", threadTs);
        }

        RequestBody requestBody = RequestBody.create(
                objectMapper.writeValueAsString(body),
                MediaType.parse("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(CHAT_POST_MESSAGE_URL)
                .post(requestBody)
                .addHeader("Authorization", "Bearer " + config.getBotToken())
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                log.error("Failed to send Slack message: {} - {}", response.code(), responseBody);
            }
        }
    }

    private void scheduleReconnect() {
        if (reconnectExecutor == null || reconnectExecutor.isShutdown()) {
            reconnectExecutor = Executors.newSingleThreadScheduledExecutor(
                    r -> new Thread(r, "slack-reconnect"));
        }

        reconnectExecutor.schedule(() -> {
            if (running) {
                log.info("Attempting to reconnect to Slack...");
                connectWebSocket();
            }
        }, 5, TimeUnit.SECONDS);
    }
}