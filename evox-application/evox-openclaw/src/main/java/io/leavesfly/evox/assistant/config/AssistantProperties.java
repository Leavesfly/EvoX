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
    private HeartbeatConfig heartbeat = new HeartbeatConfig();
    private SelfEvolutionConfig selfEvolution = new SelfEvolutionConfig();
    private SkillGeneratorConfig skillGenerator = new SkillGeneratorConfig();
    private CliConfig cli = new CliConfig();

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

    @Data
    public static class HeartbeatConfig {
        private boolean enabled = true;
        /** 心跳间隔（毫秒），默认 5 分钟 */
        private long intervalMs = 300_000;
        /** 启动延迟（毫秒），默认 10 秒 */
        private long initialDelayMs = 10_000;
    }

    @Data
    public static class SelfEvolutionConfig {
        private boolean enabled = false;
        /** 优化间隔（毫秒），默认 1 小时 */
        private long optimizationIntervalMs = 3_600_000;
        /** 触发优化所需的最少反馈数量 */
        private int minFeedbackForOptimization = 10;
        /** 改进阈值（反馈平均分高于此值才更新 Prompt） */
        private double improvementThreshold = 0.5;
    }

    @Data
    public static class SkillGeneratorConfig {
        private boolean enabled = true;
    }

    @Data
    public static class CliConfig {
        /** 是否启用 CLI 交互模式 */
        private boolean enabled = false;
        /** 是否启用终端颜色输出 */
        private boolean colorEnabled = true;
    }
}
