package io.leavesfly.evox.assistant.cli;

import io.leavesfly.evox.agents.skill.SkillRegistry;
import io.leavesfly.evox.assistant.config.AssistantProperties;
import io.leavesfly.evox.assistant.evolution.SelfEvolutionService;
import io.leavesfly.evox.assistant.evolution.SkillGenerator;
import io.leavesfly.evox.channels.core.ChannelRegistry;
import io.leavesfly.evox.core.agent.IAgentManager;
import io.leavesfly.evox.gateway.routing.GatewayRouter;
import io.leavesfly.evox.scheduler.core.TaskScheduler;
import io.leavesfly.evox.scheduler.heartbeat.HeartbeatRunner;
import io.leavesfly.evox.scheduler.heartbeat.SystemEventQueue;
import io.leavesfly.evox.tools.api.ToolRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;

/**
 * OpenClaw CLI 启动入口
 * 作为 Spring Boot CommandLineRunner，在应用启动完成后自动启动交互式 CLI。
 * 通过 evox.assistant.cli.enabled=true 开启。
 */
@Slf4j
public class CliRunner implements CommandLineRunner {

    private final GatewayRouter gatewayRouter;
    private final SkillRegistry skillRegistry;
    private final ToolRegistry toolRegistry;
    private final ChannelRegistry channelRegistry;
    private final TaskScheduler taskScheduler;
    private final IAgentManager agentManager;
    private final SystemEventQueue systemEventQueue;
    private final AssistantProperties properties;
    private final HeartbeatRunner heartbeatRunner;
    private final SelfEvolutionService selfEvolutionService;
    private final SkillGenerator skillGenerator;

    public CliRunner(GatewayRouter gatewayRouter,
                     SkillRegistry skillRegistry,
                     ToolRegistry toolRegistry,
                     ChannelRegistry channelRegistry,
                     TaskScheduler taskScheduler,
                     IAgentManager agentManager,
                     SystemEventQueue systemEventQueue,
                     AssistantProperties properties,
                     HeartbeatRunner heartbeatRunner,
                     SelfEvolutionService selfEvolutionService,
                     SkillGenerator skillGenerator) {
        this.gatewayRouter = gatewayRouter;
        this.skillRegistry = skillRegistry;
        this.toolRegistry = toolRegistry;
        this.channelRegistry = channelRegistry;
        this.taskScheduler = taskScheduler;
        this.agentManager = agentManager;
        this.systemEventQueue = systemEventQueue;
        this.properties = properties;
        this.heartbeatRunner = heartbeatRunner;
        this.selfEvolutionService = selfEvolutionService;
        this.skillGenerator = skillGenerator;
    }

    @Override
    public void run(String... args) {
        log.info("Starting OpenClaw CLI (interactive mode)...");

        boolean colorEnabled = properties.getCli().isColorEnabled();

        OpenClawRepl repl = new OpenClawRepl(
                gatewayRouter,
                skillRegistry,
                toolRegistry,
                channelRegistry,
                taskScheduler,
                agentManager,
                heartbeatRunner,
                systemEventQueue,
                selfEvolutionService,
                skillGenerator,
                colorEnabled);

        repl.start();
    }
}
