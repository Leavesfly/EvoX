package io.leavesfly.evox.channels.discord;

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
public class DiscordChannel extends AbstractChannel {

    private static final String DISCORD_GATEWAY_URL = "wss://gateway.discord.gg/?v=10&encoding=json";
    private static final String DISCORD_API_BASE = "https://discord.com/api/v10";
    
    private static final int OPCODE_DISPATCH = 0;
    private static final int OPCODE_HEARTBEAT = 1;
    private static final int OPCODE_IDENTIFY = 2;
    private static final int OPCODE_RECONNECT = 7;
    private static final int OPCODE_INVALID_SESSION = 9;
    private static final int OPCODE_HEARTBEAT_ACK = 11;
    
    private static final int INTENT_GUILDS = 1 << 0;
    private static final int INTENT_GUILD_MESSAGES = 1 << 9;
    private static final int INTENT_DIRECT_MESSAGES = 1 << 12;
    private static final int INTENT_MESSAGE_CONTENT = 1 << 15;
    
    private static final int INTENTS = INTENT_GUILDS | INTENT_GUILD_MESSAGES | 
                                       INTENT_DIRECT_MESSAGES | INTENT_MESSAGE_CONTENT;

    private final DiscordConfig config;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private volatile boolean running = false;
    private WebSocket webSocket;
    private ScheduledExecutorService heartbeatExecutor;
    private long heartbeatInterval;
    private long lastHeartbeatAck;
    private long lastHeartbeatSent;
    private String sessionId;
    private int sequence;

    public DiscordChannel(DiscordConfig config) {
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
        return config.getChannelId() != null ? config.getChannelId() : "discord";
    }

    @Override
    public String getChannelName() {
        return config.getChannelName() != null ? config.getChannelName() : "Discord";
    }

    @Override
    public ChannelConfig getConfig() {
        return config;
    }

    @Override
    protected void doStart() throws Exception {
        if (config.getBotToken() == null || config.getBotToken().isEmpty()) {
            throw new IllegalStateException("Discord bot token is required");
        }
        
        running = true;
        connectWebSocket();
        log.info("Discord channel started");
    }

    @Override
    protected void doStop() throws Exception {
        running = false;
        
        if (heartbeatExecutor != null) {
            heartbeatExecutor.shutdown();
            if (!heartbeatExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                heartbeatExecutor.shutdownNow();
            }
        }
        
        if (webSocket != null) {
            webSocket.close(1000, "Closing");
        }
        
        log.info("Discord channel stopped");
    }

    @Override
    public CompletableFuture<Void> sendMessage(String targetId, ChannelMessage message) {
        return CompletableFuture.runAsync(() -> {
            try {
                String text = message.getContent();
                if (text == null || text.isEmpty()) {
                    return;
                }

                for (int offset = 0; offset < text.length(); offset += config.getMaxMessageLength()) {
                    String chunk = text.substring(offset,
                            Math.min(offset + config.getMaxMessageLength(), text.length()));
                    sendDiscordMessage(targetId, chunk, message.getThreadId());
                }
            } catch (Exception e) {
                log.error("Failed to send Discord message to {}", targetId, e);
                throw new CompletionException(e);
            }
        });
    }

    private void connectWebSocket() {
        Request request = new Request.Builder()
                .url(DISCORD_GATEWAY_URL)
                .build();

        webSocket = httpClient.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                log.info("Discord WebSocket connected");
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                try {
                    JsonNode payload = objectMapper.readTree(text);
                    int op = payload.path("op").asInt();
                    String event = payload.path("t").asText();
                    JsonNode data = payload.path("d");
                    
                    if (payload.has("s")) {
                        sequence = payload.path("s").asInt();
                    }

                    switch (op) {
                        case OPCODE_DISPATCH:
                            handleDispatch(event, data);
                            break;
                        case OPCODE_HEARTBEAT:
                            sendHeartbeat();
                            break;
                        case OPCODE_HEARTBEAT_ACK:
                            lastHeartbeatAck = System.currentTimeMillis();
                            break;
                        case OPCODE_RECONNECT:
                            log.info("Discord requesting reconnect");
                            closeAndReconnect();
                            break;
                        case OPCODE_INVALID_SESSION:
                            log.warn("Discord invalid session, reconnecting");
                            closeAndReconnect();
                            break;
                    }
                } catch (Exception e) {
                    log.error("Error processing Discord WebSocket message", e);
                }
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                log.info("Discord WebSocket closing: {} - {}", code, reason);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                log.error("Discord WebSocket failure", t);
                if (running) {
                    scheduleReconnect();
                }
            }
        });
    }

    private void handleDispatch(String event, JsonNode data) {
        switch (event) {
            case "READY":
                sessionId = data.path("session_id").asText();
                log.info("Discord ready, session: {}", sessionId);
                break;
            case "MESSAGE_CREATE":
                processMessageCreate(data);
                break;
            case "RESUMED":
                log.info("Discord session resumed");
                break;
        }
    }

    private void processMessageCreate(JsonNode data) {
        try {
            String content = data.path("content").asText(null);
            if (content == null || content.isEmpty()) {
                return;
            }

            JsonNode author = data.path("author");
            if (author.path("bot").asBoolean(false)) {
                return;
            }

            String channelId = data.path("channel_id").asText();
            String senderId = author.path("id").asText();
            String senderName = author.path("username").asText();
            
            if (config.getAllowFrom() != null && !config.getAllowFrom().isEmpty()) {
                if (!config.getAllowFrom().contains(channelId) && 
                    !config.getAllowFrom().contains(senderId)) {
                    return;
                }
            }

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("discordMessageId", data.path("id").asText());
            metadata.put("guildId", data.path("guild_id").asText());
            metadata.put("channelType", data.path("channel_type").asText());
            
            if (data.has("message_reference")) {
                metadata.put("replyToMessageId", data.path("message_reference").path("message_id").asText());
            }

            ChannelMessage channelMessage = ChannelMessage.builder()
                    .channelId(getChannelId())
                    .senderId(senderId)
                    .senderName(senderName)
                    .targetId(channelId)
                    .content(content)
                    .contentType(MessageContentType.TEXT)
                    .metadata(metadata)
                    .build();

            notifyMessage(channelMessage);
        } catch (Exception e) {
            log.error("Error processing Discord MESSAGE_CREATE", e);
        }
    }

    private void sendHeartbeat() {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("op", OPCODE_HEARTBEAT);
            payload.put("d", sequence);
            
            String json = objectMapper.writeValueAsString(payload);
            webSocket.send(json);
            lastHeartbeatSent = System.currentTimeMillis();
        } catch (Exception e) {
            log.error("Error sending Discord heartbeat", e);
        }
    }

    private void sendIdentify() {
        try {
            Map<String, Object> identifyPayload = new HashMap<>();
            identifyPayload.put("token", config.getBotToken());
            identifyPayload.put("intents", INTENTS);
            
            Map<String, Object> properties = new HashMap<>();
            properties.put("os", System.getProperty("os.name"));
            properties.put("browser", "EvoX");
            properties.put("device", "EvoX");
            identifyPayload.put("properties", properties);
            
            Map<String, Object> payload = new HashMap<>();
            payload.put("op", OPCODE_IDENTIFY);
            payload.put("d", identifyPayload);
            
            String json = objectMapper.writeValueAsString(payload);
            webSocket.send(json);
            log.info("Discord identify sent");
        } catch (Exception e) {
            log.error("Error sending Discord identify", e);
        }
    }

    private void startHeartbeat(long interval) {
        this.heartbeatInterval = interval;
        heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(
                r -> new Thread(r, "discord-heartbeat"));
        
        heartbeatExecutor.scheduleAtFixedRate(() -> {
            if (running && webSocket != null) {
                sendHeartbeat();
            }
        }, 0, interval, TimeUnit.MILLISECONDS);
    }

    private void closeAndReconnect() {
        if (webSocket != null) {
            webSocket.close(1000, "Reconnecting");
        }
        if (running) {
            scheduleReconnect();
        }
    }

    private void scheduleReconnect() {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.schedule(() -> {
            if (running) {
                log.info("Attempting to reconnect to Discord...");
                connectWebSocket();
            }
            executor.shutdown();
        }, 5, TimeUnit.SECONDS);
    }

    private void sendDiscordMessage(String channelId, String content, String replyToMessageId) throws IOException {
        String url = DISCORD_API_BASE + "/channels/" + channelId + "/messages";
        
        Map<String, Object> body = new HashMap<>();
        body.put("content", content);
        
        if (replyToMessageId != null && !replyToMessageId.isEmpty()) {
            Map<String, String> messageReference = new HashMap<>();
            messageReference.put("message_id", replyToMessageId);
            body.put("message_reference", messageReference);
        }

        RequestBody requestBody = RequestBody.create(
                objectMapper.writeValueAsString(body),
                MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("Authorization", "Bot " + config.getBotToken())
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                log.error("Failed to send Discord message: {} - {}", response.code(), responseBody);
            }
        }
    }
}