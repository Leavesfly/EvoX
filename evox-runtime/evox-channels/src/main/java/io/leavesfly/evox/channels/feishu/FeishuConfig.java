package io.leavesfly.evox.channels.feishu;

import io.leavesfly.evox.channels.core.ChannelConfig;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class FeishuConfig extends ChannelConfig {
    private String appId;
    private String appSecret;
    @Builder.Default
    private String encryptKey = "";
    @Builder.Default
    private String verificationToken = "";
    private List<String> allowFrom;
}
