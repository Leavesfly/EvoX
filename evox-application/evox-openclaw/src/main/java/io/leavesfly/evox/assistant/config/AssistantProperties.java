package io.leavesfly.evox.assistant.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "evox.assistant")
public class AssistantProperties {

    private String name = "EvoX Assistant";
    private String defaultAgent = "default";

    private GatewayConfig gateway = new GatewayConfig();
    private WebhookChannelConfig webhook = new WebhookChannelConfig();
    private TelegramChannelConfig telegram = new TelegramChannelConfig();
    private DingTalkChannelConfig dingtalk = new DingTalkChannelConfig();
    private SchedulerConfig scheduler = new SchedulerConfig();

    @Data
    public static class GatewayConfig {
        private boolean enabled = true;
        private List<String> apiKeys = new ArrayList<>();
        private int rateLimitPerMinute = 60;
        private boolean allowAnonymousChannelAccess = true;
        private int sessionTimeoutHours = 24;
        private int maxAuditEvents = 10000;
    }

    @Data
    public static class WebhookChannelConfig {
        private boolean enabled = true;
        private String path = "/api/webhook";
        private String authToken;
    }

    @Data
    public static class TelegramChannelConfig {
        private boolean enabled = false;
        private String botToken;
        private String botUsername;
        private long pollingIntervalMs = 1000;
    }

    @Data
    public static class DingTalkChannelConfig {
        private boolean enabled = false;
        private String appKey;
        private String appSecret;
        private String robotCode;
        private String callbackPath = "/api/dingtalk/callback";
    }

    @Data
    public static class SchedulerConfig {
        private boolean enabled = true;
        private long checkIntervalMs = 1000;
    }
}
