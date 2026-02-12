package io.leavesfly.evox.channels.qq;

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
public class QQConfig extends ChannelConfig {
    private String appId;
    private String appSecret;
    private List<String> allowFrom;
    private boolean sandboxMode = true;
}
