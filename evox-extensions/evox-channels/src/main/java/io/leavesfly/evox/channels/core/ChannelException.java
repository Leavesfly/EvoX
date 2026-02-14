package io.leavesfly.evox.channels.core;

public class ChannelException extends RuntimeException {

    private final String channelId;

    public ChannelException(String message) {
        super(message);
        this.channelId = null;
    }

    public ChannelException(String channelId, String message) {
        super("[" + channelId + "] " + message);
        this.channelId = channelId;
    }

    public ChannelException(String channelId, String message, Throwable cause) {
        super("[" + channelId + "] " + message, cause);
        this.channelId = channelId;
    }

    public String getChannelId() {
        return channelId;
    }
}
