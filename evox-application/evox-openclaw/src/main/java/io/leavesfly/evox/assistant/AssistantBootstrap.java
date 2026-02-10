package io.leavesfly.evox.assistant;

import io.leavesfly.evox.channels.adapter.AgentChannelListener;
import io.leavesfly.evox.channels.core.ChannelRegistry;
import io.leavesfly.evox.channels.core.IChannel;
import io.leavesfly.evox.assistant.config.AssistantProperties;
import io.leavesfly.evox.core.agent.IAgent;
import io.leavesfly.evox.core.agent.IAgentManager;
import io.leavesfly.evox.scheduler.core.TaskScheduler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;

@Slf4j
public class AssistantBootstrap implements SmartLifecycle {

    private final ChannelRegistry channelRegistry;
    private final TaskScheduler taskScheduler;
    private final IAgentManager agentManager;
    private final AssistantProperties properties;
    private volatile boolean running = false;

    public AssistantBootstrap(ChannelRegistry channelRegistry,
                              TaskScheduler taskScheduler,
                              IAgentManager agentManager,
                              AssistantProperties properties) {
        this.channelRegistry = channelRegistry;
        this.taskScheduler = taskScheduler;
        this.agentManager = agentManager;
        this.properties = properties;
    }

    @Override
    public void start() {
        log.info("========================================");
        log.info("  Starting EvoX Assistant: {}", properties.getName());
        log.info("========================================");

        bindAgentToChannels();
        startChannels();
        startScheduler();

        running = true;
        log.info("EvoX Assistant started successfully!");
        log.info("  Channels: {}", channelRegistry.getChannelCount());
        log.info("  Scheduled Tasks: {}", taskScheduler.getTaskCount());
        log.info("  Agents: {}", agentManager.getAgentCount());
    }

    @Override
    public void stop() {
        log.info("Stopping EvoX Assistant...");
        taskScheduler.shutdown();
        channelRegistry.stopAll();
        running = false;
        log.info("EvoX Assistant stopped.");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 1;
    }

    private void bindAgentToChannels() {
        String defaultAgentName = properties.getDefaultAgent();
        IAgent defaultAgent = agentManager.getAgent(defaultAgentName);

        if (defaultAgent == null && agentManager.getAgentCount() > 0) {
            defaultAgent = agentManager.getAllAgents().values().iterator().next();
            log.warn("Default agent '{}' not found, using first available: {}",
                    defaultAgentName, defaultAgent.getName());
        }

        if (defaultAgent == null) {
            log.warn("No agents available. Channels will not process messages until an agent is registered.");
            return;
        }

        AgentChannelListener listener = new AgentChannelListener(defaultAgent, channelRegistry);
        for (IChannel channel : channelRegistry.getAllChannels()) {
            channel.addListener(listener);
            log.info("Bound agent '{}' to channel '{}'", defaultAgent.getName(), channel.getChannelId());
        }
    }

    private void startChannels() {
        channelRegistry.startAll();
    }

    private void startScheduler() {
        if (properties.getScheduler().isEnabled()) {
            taskScheduler.start();
            log.info("Task scheduler started");
        }
    }
}
