package io.leavesfly.evox.channels.core;

import java.util.concurrent.CompletableFuture;

public interface IChannel {

    String getChannelId();

    String getChannelName();

    void start() throws ChannelException;

    void stop();

    CompletableFuture<Void> sendMessage(String targetId, ChannelMessage message);

    default CompletableFuture<Void> sendTextMessage(String targetId, String text) {
        ChannelMessage message = ChannelMessage.textMessage(getChannelId(), targetId, text);
        return sendMessage(targetId, message);
    }

    void addListener(IChannelListener listener);

    void removeListener(IChannelListener listener);

    ChannelStatus getStatus();

    ChannelConfig getConfig();
}
