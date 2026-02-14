package io.leavesfly.evox.channels.telegram;

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
public class TelegramChannel extends AbstractChannel {

    private static final String TELEGRAM_API_BASE = "https://api.telegram.org/bot";

    private final TelegramConfig config;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private ScheduledExecutorService pollingExecutor;
    private volatile boolean polling = false;
    private long lastUpdateId = 0;

    public TelegramChannel(TelegramConfig config) {
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
        return config.getChannelId() != null ? config.getChannelId() : "telegram";
    }

    @Override
    public String getChannelName() {
        return config.getChannelName() != null ? config.getChannelName() : "Telegram";
    }

    @Override
    public ChannelConfig getConfig() {
        return config;
    }

    @Override
    protected void doStart() throws Exception {
        if (config.getBotToken() == null || config.getBotToken().isEmpty()) {
            throw new IllegalStateException("Telegram bot token is required");
        }
        polling = true;
        pollingExecutor = Executors.newSingleThreadScheduledExecutor(
                r -> new Thread(r, "telegram-polling"));
        pollingExecutor.scheduleWithFixedDelay(
                this::pollUpdates, 0, config.getPollingIntervalMs(), TimeUnit.MILLISECONDS);
        log.info("Telegram long-polling started for bot: {}", config.getBotUsername());
    }

    @Override
    protected void doStop() throws Exception {
        polling = false;
        if (pollingExecutor != null) {
            pollingExecutor.shutdown();
            if (!pollingExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                pollingExecutor.shutdownNow();
            }
        }
        log.info("Telegram polling stopped");
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
                    sendTelegramMessage(targetId, chunk, message.getThreadId());
                }
            } catch (Exception e) {
                log.error("Failed to send Telegram message to {}", targetId, e);
                throw new CompletionException(e);
            }
        });
    }

    private void pollUpdates() {
        if (!polling) {
            return;
        }
        try {
            String url = buildApiUrl("getUpdates") +
                    "?offset=" + (lastUpdateId + 1) +
                    "&timeout=30" +
                    "&allowed_updates=[\"message\"]";

            Request request = new Request.Builder().url(url).get().build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    log.warn("Telegram getUpdates failed: {}", response.code());
                    return;
                }

                JsonNode root = objectMapper.readTree(response.body().string());
                if (!root.path("ok").asBoolean(false)) {
                    log.warn("Telegram API returned error: {}", root.path("description").asText());
                    return;
                }

                JsonNode results = root.path("result");
                for (JsonNode update : results) {
                    long updateId = update.path("update_id").asLong();
                    if (updateId > lastUpdateId) {
                        lastUpdateId = updateId;
                    }
                    processUpdate(update);
                }
            }
        } catch (Exception e) {
            log.error("Error polling Telegram updates", e);
        }
    }

    private void processUpdate(JsonNode update) {
        JsonNode messageNode = update.path("message");
        if (messageNode.isMissingNode()) {
            return;
        }

        String text = messageNode.path("text").asText(null);
        if (text == null || text.isEmpty()) {
            return;
        }

        JsonNode fromNode = messageNode.path("from");
        String senderId = String.valueOf(messageNode.path("chat").path("id").asLong());
        String senderName = fromNode.path("first_name").asText("");
        String lastName = fromNode.path("last_name").asText("");
        if (!lastName.isEmpty()) {
            senderName = senderName + " " + lastName;
        }
        String username = fromNode.path("username").asText(null);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("telegramMessageId", messageNode.path("message_id").asLong());
        metadata.put("chatType", messageNode.path("chat").path("type").asText());
        if (username != null) {
            metadata.put("username", username);
        }

        Integer replyToMessageId = null;
        if (messageNode.has("reply_to_message")) {
            replyToMessageId = messageNode.path("reply_to_message").path("message_id").asInt();
            metadata.put("replyToMessageId", replyToMessageId);
        }

        ChannelMessage channelMessage = ChannelMessage.builder()
                .channelId(getChannelId())
                .senderId(senderId)
                .senderName(senderName)
                .targetId(senderId)
                .content(text)
                .contentType(MessageContentType.TEXT)
                .metadata(metadata)
                .build();

        notifyMessage(channelMessage);
    }

    private void sendTelegramMessage(String chatId, String text, String replyToMessageId) throws IOException {
        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("text", text);
        body.put("parse_mode", "Markdown");
        if (replyToMessageId != null) {
            try {
                body.put("reply_to_message_id", Long.parseLong(replyToMessageId));
            } catch (NumberFormatException ignored) {
            }
        }

        RequestBody requestBody = RequestBody.create(
                objectMapper.writeValueAsString(body),
                MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(buildApiUrl("sendMessage"))
                .post(requestBody)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                log.error("Failed to send Telegram message: {} - {}", response.code(), responseBody);
            }
        }
    }

    private String buildApiUrl(String method) {
        return TELEGRAM_API_BASE + config.getBotToken() + "/" + method;
    }
}
