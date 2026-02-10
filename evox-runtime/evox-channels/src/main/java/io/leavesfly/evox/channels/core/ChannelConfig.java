package io.leavesfly.evox.channels.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class ChannelConfig {
    private String channelId;
    private String channelName;
    private boolean enabled = true;
}
