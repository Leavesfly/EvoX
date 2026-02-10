package io.leavesfly.evox.channels.core;

import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ChannelRegistry {

    private final Map<String, IChannel> channels = new ConcurrentHashMap<>();

    public void register(IChannel channel) {
        String channelId = channel.getChannelId();
        if (channels.containsKey(channelId)) {
            throw new ChannelException(channelId, "Channel already registered: " + channelId);
        }
        channels.put(channelId, channel);
        log.info("Channel registered: {} ({})", channelId, channel.getChannelName());
    }

    public void unregister(String channelId) {
        IChannel removed = channels.remove(channelId);
        if (removed != null) {
            if (removed.getStatus() == ChannelStatus.RUNNING) {
                removed.stop();
            }
            log.info("Channel unregistered: {}", channelId);
        }
    }

    public IChannel getChannel(String channelId) {
        return channels.get(channelId);
    }

    public Collection<IChannel> getAllChannels() {
        return Collections.unmodifiableCollection(channels.values());
    }

    public boolean hasChannel(String channelId) {
        return channels.containsKey(channelId);
    }

    public void startAll() {
        for (IChannel channel : channels.values()) {
            if (channel.getConfig().isEnabled() && channel.getStatus() != ChannelStatus.RUNNING) {
                try {
                    channel.start();
                    log.info("Channel started: {}", channel.getChannelId());
                } catch (Exception e) {
                    log.error("Failed to start channel: {}", channel.getChannelId(), e);
                }
            }
        }
    }

    public void stopAll() {
        for (IChannel channel : channels.values()) {
            if (channel.getStatus() == ChannelStatus.RUNNING) {
                try {
                    channel.stop();
                    log.info("Channel stopped: {}", channel.getChannelId());
                } catch (Exception e) {
                    log.error("Failed to stop channel: {}", channel.getChannelId(), e);
                }
            }
        }
    }

    public int getChannelCount() {
        return channels.size();
    }
}
