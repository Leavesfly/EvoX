package io.leavesfly.evox.channels.webhook;

import io.leavesfly.evox.channels.core.AbstractChannel;
import io.leavesfly.evox.channels.core.ChannelConfig;
import io.leavesfly.evox.channels.core.ChannelMessage;
import io.leavesfly.evox.channels.core.MessageContentType;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class WebhookChannel extends AbstractChannel {

    private final WebhookConfig config;
    private final Map<String, CompletableFuture<ChannelMessage>> pendingResponses = new ConcurrentHashMap<>();

    public WebhookChannel(WebhookConfig config) {
        this.config = config;
    }

    @Override
    public String getChannelId() {
        return config.getChannelId() != null ? config.getChannelId() : "webhook";
    }

    @Override
    public String getChannelName() {
        return config.getChannelName() != null ? config.getChannelName() : "Webhook";
    }

    @Override
    public ChannelConfig getConfig() {
        return config;
    }

    @Override
    protected void doStart() throws Exception {
        log.info("WebhookChannel started, listening on path: {}", config.getWebhookPath());
    }

    @Override
    protected void doStop() throws Exception {
        pendingResponses.values().forEach(f -> f.cancel(true));
        pendingResponses.clear();
        log.info("WebhookChannel stopped");
    }

    @Override
    public CompletableFuture<Void> sendMessage(String targetId, ChannelMessage message) {
        CompletableFuture<ChannelMessage> pending = pendingResponses.remove(targetId);
        if (pending != null) {
            pending.complete(message);
        } else {
            log.warn("No pending response for target: {}", targetId);
        }
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<ChannelMessage> handleIncomingMessage(String senderId, String content,
                                                                    Map<String, Object> metadata) {
        ChannelMessage incomingMessage = ChannelMessage.builder()
                .channelId(getChannelId())
                .senderId(senderId)
                .targetId(senderId)
                .content(content)
                .contentType(MessageContentType.TEXT)
                .metadata(metadata != null ? metadata : Map.of())
                .build();

        CompletableFuture<ChannelMessage> responseFuture = new CompletableFuture<>();
        pendingResponses.put(senderId, responseFuture);

        notifyMessage(incomingMessage);

        return responseFuture;
    }

    public boolean validateAuthToken(String token) {
        if (config.getAuthToken() == null || config.getAuthToken().isEmpty()) {
            return true;
        }
        return config.getAuthToken().equals(token);
    }
}
