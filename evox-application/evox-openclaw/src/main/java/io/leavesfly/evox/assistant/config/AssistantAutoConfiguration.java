package io.leavesfly.evox.assistant.config;

import io.leavesfly.evox.assistant.AssistantBootstrap;
import io.leavesfly.evox.assistant.cli.CliRunner;
import io.leavesfly.evox.assistant.evolution.SelfEvolutionService;
import io.leavesfly.evox.assistant.evolution.SkillGenerator;
import io.leavesfly.evox.skill.SkillMarketplace;
import io.leavesfly.evox.skill.SkillRegistry;

import io.leavesfly.evox.channels.core.ChannelRegistry;
import io.leavesfly.evox.channels.dingtalk.DingTalkChannel;
import io.leavesfly.evox.channels.dingtalk.DingTalkConfig;
import io.leavesfly.evox.channels.telegram.TelegramChannel;
import io.leavesfly.evox.channels.telegram.TelegramConfig;
import io.leavesfly.evox.channels.webhook.WebhookChannel;
import io.leavesfly.evox.channels.webhook.WebhookConfig;
import io.leavesfly.evox.channels.webhook.WebhookController;
import io.leavesfly.evox.channels.dingtalk.DingTalkCallbackController;
import io.leavesfly.evox.core.agent.IAgent;
import io.leavesfly.evox.core.agent.IAgentManager;
import io.leavesfly.evox.gateway.audit.AuditLogger;
import io.leavesfly.evox.gateway.auth.ApiKeyAuthProvider;
import io.leavesfly.evox.gateway.auth.IAuthProvider;
import io.leavesfly.evox.gateway.auth.UserManager;
import io.leavesfly.evox.gateway.ratelimit.RateLimiter;
import io.leavesfly.evox.gateway.routing.GatewayRouter;
import io.leavesfly.evox.gateway.session.SessionManager;
import io.leavesfly.evox.scheduler.core.TaskScheduler;
import io.leavesfly.evox.scheduler.event.EventBus;
import io.leavesfly.evox.scheduler.heartbeat.HeartbeatRunner;
import io.leavesfly.evox.scheduler.heartbeat.SystemEventQueue;
import io.leavesfly.evox.tools.api.ToolRegistry;
import io.leavesfly.evox.tools.calendar.CalendarTool;
import io.leavesfly.evox.tools.clipboard.ClipboardTool;
import io.leavesfly.evox.tools.email.EmailTool;
import io.leavesfly.evox.tools.system.NotificationTool;
import io.leavesfly.evox.tools.system.ProcessManagerTool;
import io.leavesfly.evox.tools.system.SystemInfoTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Slf4j
@Configuration
@EnableConfigurationProperties(AssistantProperties.class)
public class AssistantAutoConfiguration {

    // ========== Gateway Beans ==========

    @Bean
    @ConditionalOnMissingBean
    public IAuthProvider authProvider(AssistantProperties properties) {
        return new ApiKeyAuthProvider(
                properties.getGateway().getApiKeys(),
                properties.getGateway().isAllowAnonymousChannelAccess());
    }

    @Bean
    @ConditionalOnMissingBean
    public SessionManager sessionManager(AssistantProperties properties) {
        SessionManager manager = new SessionManager(
                Duration.ofHours(properties.getGateway().getSessionTimeoutHours()));
        manager.start();
        return manager;
    }

    @Bean
    @ConditionalOnMissingBean
    public RateLimiter rateLimiter(AssistantProperties properties) {
        return new RateLimiter(properties.getGateway().getRateLimitPerMinute());
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditLogger auditLogger(AssistantProperties properties) {
        return new AuditLogger(properties.getGateway().getMaxAuditEvents());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "evox.assistant.gateway", name = "enabled", havingValue = "true", matchIfMissing = true)
    public GatewayRouter gatewayRouter(IAuthProvider authProvider,
                                       SessionManager sessionManager,
                                       RateLimiter rateLimiter,
                                       AuditLogger auditLogger,
                                       IAgentManager agentManager,
                                       AssistantProperties properties) {
        return GatewayRouter.builder()
                .authProvider(authProvider)
                .sessionManager(sessionManager)
                .rateLimiter(rateLimiter)
                .auditLogger(auditLogger)
                .agentManager(agentManager)
                .defaultAgentName(properties.getDefaultAgent())
                .build();
    }

    // ========== Channel Beans ==========

    @Bean
    @ConditionalOnMissingBean
    public ChannelRegistry channelRegistry() {
        return new ChannelRegistry();
    }

    @Bean
    @ConditionalOnProperty(prefix = "evox.assistant.webhook", name = "enabled", havingValue = "true", matchIfMissing = true)
    public WebhookChannel webhookChannel(AssistantProperties properties,
                                         ChannelRegistry registry) {
        WebhookConfig config = WebhookConfig.builder()
                .channelId("webhook")
                .channelName("Webhook")
                .enabled(properties.getWebhook().isEnabled())
                .webhookPath(properties.getWebhook().getPath())
                .authToken(properties.getWebhook().getAuthToken())
                .build();
        WebhookChannel channel = new WebhookChannel(config);
        registry.register(channel);
        return channel;
    }

    @Bean
    @ConditionalOnProperty(prefix = "evox.assistant.webhook", name = "enabled", havingValue = "true", matchIfMissing = true)
    public WebhookController webhookController(WebhookChannel webhookChannel) {
        return new WebhookController(webhookChannel);
    }

    @Bean
    @ConditionalOnProperty(prefix = "evox.assistant.telegram", name = "enabled", havingValue = "true")
    public TelegramChannel telegramChannel(AssistantProperties properties,
                                           ChannelRegistry registry) {
        TelegramConfig config = TelegramConfig.builder()
                .channelId("telegram")
                .channelName("Telegram")
                .enabled(true)
                .botToken(properties.getTelegram().getBotToken())
                .botUsername(properties.getTelegram().getBotUsername())
                .pollingIntervalMs(properties.getTelegram().getPollingIntervalMs())
                .build();
        TelegramChannel channel = new TelegramChannel(config);
        registry.register(channel);
        return channel;
    }

    @Bean
    @ConditionalOnProperty(prefix = "evox.assistant.dingtalk", name = "enabled", havingValue = "true")
    public DingTalkChannel dingTalkChannel(AssistantProperties properties,
                                           ChannelRegistry registry) {
        DingTalkConfig config = DingTalkConfig.builder()
                .channelId("dingtalk")
                .channelName("DingTalk")
                .enabled(true)
                .appKey(properties.getDingtalk().getAppKey())
                .appSecret(properties.getDingtalk().getAppSecret())
                .robotCode(properties.getDingtalk().getRobotCode())
                .callbackPath(properties.getDingtalk().getCallbackPath())
                .build();
        DingTalkChannel channel = new DingTalkChannel(config);
        registry.register(channel);
        return channel;
    }

    @Bean
    @ConditionalOnProperty(prefix = "evox.assistant.dingtalk", name = "enabled", havingValue = "true")
    public DingTalkCallbackController dingTalkCallbackController(DingTalkChannel dingTalkChannel) {
        return new DingTalkCallbackController(dingTalkChannel);
    }

    // ========== Scheduler Beans ==========

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "evox.assistant.scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
    public TaskScheduler taskScheduler(AssistantProperties properties) {
        return new TaskScheduler(properties.getScheduler().getCheckIntervalMs());
    }

    @Bean
    @ConditionalOnMissingBean
    public EventBus eventBus() {
        return new EventBus();
    }

    // ========== Skills ==========

    @Bean
    @ConditionalOnMissingBean
    public SkillRegistry skillRegistry() {
        SkillRegistry registry = new SkillRegistry();

        // 从 classpath 的 skills/ 目录加载内置 SKILL.md 文件
        int builtinCount = registry.loadBuiltinSkills();

        log.info("Registered {} personal assistant skills from SKILL.md files", builtinCount);
        return registry;
    }

    // ========== User Management ==========

    @Bean
    @ConditionalOnMissingBean
    public UserManager userManager() {
        UserManager manager = new UserManager();
        manager.initDefaultAdmin();
        log.info("UserManager initialized with default admin user");
        return manager;
    }

    // ========== Skills Marketplace ==========

    @Bean
    @ConditionalOnMissingBean
    public SkillMarketplace skillMarketplace(SkillRegistry skillRegistry) {
        SkillMarketplace marketplace = new SkillMarketplace(skillRegistry);
        marketplace.initBuiltinSkills();
        log.info("SkillMarketplace initialized with {} builtin skills",
                marketplace.getInstalledSkills().size());
        return marketplace;
    }

    // ========== Tools ==========

    @Bean
    @ConditionalOnMissingBean
    public ToolRegistry toolRegistry() {
        ToolRegistry registry = ToolRegistry.createDefault();

        registry.register(new SystemInfoTool(), "system");
        registry.register(new ProcessManagerTool(), "system");
        registry.register(new NotificationTool(), "system");
        registry.register(new ClipboardTool(), "utility");
        registry.register(new EmailTool(), "utility");
        registry.register(new CalendarTool(), "utility");

        log.info("Registered {} tools (including system and utility tools)", registry.size());
        return registry;
    }

    // ========== Heartbeat (主动唤醒) ==========

    @Bean
    @ConditionalOnMissingBean
    public SystemEventQueue systemEventQueue() {
        return new SystemEventQueue();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "evox.assistant.heartbeat", name = "enabled", havingValue = "true", matchIfMissing = true)
    public HeartbeatRunner heartbeatRunner(IAgentManager agentManager,
                                           SystemEventQueue systemEventQueue,
                                           AssistantProperties properties) {
        AssistantProperties.HeartbeatConfig heartbeatConfig = properties.getHeartbeat();

        String defaultAgentName = properties.getDefaultAgent();
        IAgent defaultAgent = agentManager.getAgent(defaultAgentName);
        if (defaultAgent == null && agentManager.getAgentCount() > 0) {
            defaultAgent = agentManager.getAllAgents().values().iterator().next();
        }

        if (defaultAgent == null) {
            log.warn("No agent available for HeartbeatRunner. Heartbeat will not start.");
            return null;
        }

        HeartbeatRunner.HeartbeatConfig config = HeartbeatRunner.HeartbeatConfig.builder()
                .enabled(heartbeatConfig.isEnabled())
                .intervalMs(heartbeatConfig.getIntervalMs())
                .initialDelayMs(heartbeatConfig.getInitialDelayMs())
                .build();

        HeartbeatRunner runner = new HeartbeatRunner(defaultAgent, systemEventQueue, config);
        log.info("HeartbeatRunner created: interval={}ms, initialDelay={}ms",
                heartbeatConfig.getIntervalMs(), heartbeatConfig.getInitialDelayMs());
        return runner;
    }

    // ========== Self Evolution (自进化) ==========

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "evox.assistant.self-evolution", name = "enabled", havingValue = "true")
    public SelfEvolutionService selfEvolutionService(IAgentManager agentManager,
                                                     AssistantProperties properties) {
        AssistantProperties.SelfEvolutionConfig evolutionConfig = properties.getSelfEvolution();

        String defaultAgentName = properties.getDefaultAgent();
        IAgent defaultAgent = agentManager.getAgent(defaultAgentName);
        if (defaultAgent == null && agentManager.getAgentCount() > 0) {
            defaultAgent = agentManager.getAllAgents().values().iterator().next();
        }

        if (defaultAgent == null) {
            log.warn("No agent available for SelfEvolutionService. Self-evolution will not start.");
            return null;
        }

        SelfEvolutionService.SelfEvolutionConfig config = SelfEvolutionService.SelfEvolutionConfig.builder()
                .enabled(evolutionConfig.isEnabled())
                .optimizationIntervalMs(evolutionConfig.getOptimizationIntervalMs())
                .minFeedbackForOptimization(evolutionConfig.getMinFeedbackForOptimization())
                .improvementThreshold(evolutionConfig.getImprovementThreshold())
                .build();

        SelfEvolutionService service = new SelfEvolutionService(defaultAgent, config);
        log.info("SelfEvolutionService created: interval={}ms, minFeedback={}",
                evolutionConfig.getOptimizationIntervalMs(), evolutionConfig.getMinFeedbackForOptimization());
        return service;
    }

    // ========== Skill Generator (技能自动生成) ==========

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "evox.assistant.skill-generator", name = "enabled", havingValue = "true", matchIfMissing = true)
    public SkillGenerator skillGenerator(IAgentManager agentManager,
                                         SkillRegistry skillRegistry,
                                         SkillMarketplace skillMarketplace,
                                         AssistantProperties properties) {
        String defaultAgentName = properties.getDefaultAgent();
        IAgent defaultAgent = agentManager.getAgent(defaultAgentName);
        if (defaultAgent == null && agentManager.getAgentCount() > 0) {
            defaultAgent = agentManager.getAllAgents().values().iterator().next();
        }

        if (defaultAgent == null) {
            log.warn("No agent available for SkillGenerator. Skill generation will not be available.");
            return null;
        }

        SkillGenerator generator = new SkillGenerator(defaultAgent, skillRegistry, skillMarketplace);
        log.info("SkillGenerator created for agent: {}", defaultAgent.getName());
        return generator;
    }

    // ========== CLI ==========

    @Bean
    @ConditionalOnProperty(prefix = "evox.assistant.cli", name = "enabled", havingValue = "true")
    public CliRunner cliRunner(
            GatewayRouter gatewayRouter,
            SkillRegistry skillRegistry,
            ToolRegistry toolRegistry,
            ChannelRegistry channelRegistry,
            TaskScheduler taskScheduler,
            IAgentManager agentManager,
            SystemEventQueue systemEventQueue,
            AssistantProperties properties,
            ObjectProvider<HeartbeatRunner> heartbeatRunnerProvider,
            ObjectProvider<SelfEvolutionService> selfEvolutionServiceProvider,
            ObjectProvider<io.leavesfly.evox.assistant.evolution.SkillGenerator> skillGeneratorProvider) {
        return new CliRunner(
                gatewayRouter,
                skillRegistry,
                toolRegistry,
                channelRegistry,
                taskScheduler,
                agentManager,
                systemEventQueue,
                properties,
                heartbeatRunnerProvider.getIfAvailable(),
                selfEvolutionServiceProvider.getIfAvailable(),
                skillGeneratorProvider.getIfAvailable());
    }

    // ========== Bootstrap ==========

    @Bean
    public AssistantBootstrap assistantBootstrap(ChannelRegistry channelRegistry,
                                                 TaskScheduler taskScheduler,
                                                 IAgentManager agentManager,
                                                 AssistantProperties properties,
                                                 SystemEventQueue systemEventQueue,
                                                 org.springframework.beans.factory.ObjectProvider<HeartbeatRunner> heartbeatRunnerProvider,
                                                 org.springframework.beans.factory.ObjectProvider<SelfEvolutionService> selfEvolutionServiceProvider,
                                                 org.springframework.beans.factory.ObjectProvider<SkillGenerator> skillGeneratorProvider) {
        return new AssistantBootstrap(
                channelRegistry, taskScheduler, agentManager, properties,
                systemEventQueue,
                heartbeatRunnerProvider.getIfAvailable(),
                selfEvolutionServiceProvider.getIfAvailable(),
                skillGeneratorProvider.getIfAvailable());
    }
}
