package io.leavesfly.evox.channels.discord;

import io.leavesfly.evox.channels.core.ChannelConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DiscordConfig extends ChannelConfig {
    private String botToken;
    private String applicationId;
    private List<String> allowFrom;
    private int maxMessageLength = 2000;
}
