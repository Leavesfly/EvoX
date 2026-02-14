package io.leavesfly.evox.channels.whatsapp;

import io.leavesfly.evox.channels.core.ChannelConfig;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class WhatsAppConfig extends ChannelConfig {
    private String accessToken;
    private String phoneNumberId;
    private String verifyToken;
    @Builder.Default
    private String webhookPath = "/api/whatsapp/webhook";
    private List<String> allowFrom;
}
