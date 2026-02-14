package io.leavesfly.evox.channels.core;

public interface IChannelListener {

    void onMessage(ChannelMessage message);

    default void onMediaMessage(ChannelMessage message) {
        onMessage(message);
    }

    default void onUserEvent(ChannelUser user, String eventType) {
    }

    default void onStatusChange(String channelId, ChannelStatus oldStatus, ChannelStatus newStatus) {
    }

    default void onError(String channelId, Throwable error) {
    }
}
