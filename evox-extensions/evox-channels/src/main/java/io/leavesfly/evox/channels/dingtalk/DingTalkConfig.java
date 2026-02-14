package io.leavesfly.evox.channels.dingtalk;

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
public class DingTalkConfig extends ChannelConfig {
    private String appKey;
    private String appSecret;
    private String robotCode;
    private String callbackPath = "/api/dingtalk/callback";
    private String callbackToken;
    private String aesKey;
}
