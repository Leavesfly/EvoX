package io.leavesfly.evox.channels.telegram;

import io.leavesfly.evox.channels.core.ChannelConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TelegramConfig extends ChannelConfig {
    private String botToken;
    private String botUsername;
    private long pollingIntervalMs = 1000;
    private int maxMessageLength = 4096;
}
