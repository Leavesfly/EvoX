package io.leavesfly.evox.channels.core;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
public abstract class AbstractChannel implements IChannel {

    @Getter
    protected ChannelStatus status = ChannelStatus.CREATED;

    protected final List<IChannelListener> listeners = new CopyOnWriteArrayList<>();

    @Override
    public void addListener(IChannelListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(IChannelListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void start() throws ChannelException {
        ChannelStatus oldStatus = this.status;
        this.status = ChannelStatus.STARTING;
        notifyStatusChange(oldStatus, ChannelStatus.STARTING);
        try {
            doStart();
            this.status = ChannelStatus.RUNNING;
            notifyStatusChange(ChannelStatus.STARTING, ChannelStatus.RUNNING);
            log.info("Channel [{}] started successfully", getChannelId());
        } catch (Exception e) {
            this.status = ChannelStatus.ERROR;
            notifyStatusChange(ChannelStatus.STARTING, ChannelStatus.ERROR);
            notifyError(e);
            throw new ChannelException(getChannelId(), "Failed to start channel", e);
        }
    }

    @Override
    public void stop() {
        ChannelStatus oldStatus = this.status;
        this.status = ChannelStatus.STOPPING;
        notifyStatusChange(oldStatus, ChannelStatus.STOPPING);
        try {
            doStop();
            this.status = ChannelStatus.STOPPED;
            notifyStatusChange(ChannelStatus.STOPPING, ChannelStatus.STOPPED);
            log.info("Channel [{}] stopped successfully", getChannelId());
        } catch (Exception e) {
            this.status = ChannelStatus.ERROR;
            notifyStatusChange(ChannelStatus.STOPPING, ChannelStatus.ERROR);
            notifyError(e);
            log.error("Failed to stop channel [{}]", getChannelId(), e);
        }
    }

    protected abstract void doStart() throws Exception;

    protected abstract void doStop() throws Exception;

    protected void notifyMessage(ChannelMessage message) {
        for (IChannelListener listener : listeners) {
            try {
                if (message.getContentType() == MessageContentType.TEXT) {
                    listener.onMessage(message);
                } else {
                    listener.onMediaMessage(message);
                }
            } catch (Exception e) {
                log.error("Error in channel listener for channel [{}]", getChannelId(), e);
            }
        }
    }

    protected void notifyStatusChange(ChannelStatus oldStatus, ChannelStatus newStatus) {
        for (IChannelListener listener : listeners) {
            try {
                listener.onStatusChange(getChannelId(), oldStatus, newStatus);
            } catch (Exception e) {
                log.error("Error notifying status change for channel [{}]", getChannelId(), e);
            }
        }
    }

    protected void notifyError(Throwable error) {
        for (IChannelListener listener : listeners) {
            try {
                listener.onError(getChannelId(), error);
            } catch (Exception e) {
                log.error("Error notifying error for channel [{}]", getChannelId(), e);
            }
        }
    }
}
