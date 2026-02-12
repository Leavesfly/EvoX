package io.leavesfly.evox.channels.slack;

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
public class SlackConfig extends ChannelConfig {
    private String botToken;
    private String appToken;
    private List<String> allowFrom;
    @lombok.Builder.Default
    private String groupPolicy = "mention";
}
