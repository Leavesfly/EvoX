package io.leavesfly.evox.assistant.controller;

import io.leavesfly.evox.agents.skill.SkillRegistry;
import io.leavesfly.evox.channels.core.ChannelRegistry;
import io.leavesfly.evox.channels.core.ChannelStatus;
import io.leavesfly.evox.channels.core.IChannel;
import io.leavesfly.evox.core.agent.IAgentManager;
import io.leavesfly.evox.gateway.audit.AuditLogger;
import io.leavesfly.evox.gateway.session.SessionManager;
import io.leavesfly.evox.scheduler.core.TaskScheduler;
import io.leavesfly.evox.tools.api.ToolRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HealthController {

    private final ChannelRegistry channelRegistry;
    private final TaskScheduler taskScheduler;
    private final IAgentManager agentManager;
    private final SessionManager sessionManager;
    private final AuditLogger auditLogger;
    private final SkillRegistry skillRegistry;
    private final ToolRegistry toolRegistry;

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "UP");
        result.put("timestamp", Instant.now().toString());

        Map<String, Object> components = new LinkedHashMap<>();

        // Channels
        Map<String, String> channelStatuses = new LinkedHashMap<>();
        for (IChannel channel : channelRegistry.getAllChannels()) {
            channelStatuses.put(channel.getChannelId(), channel.getStatus().name());
        }
        components.put("channels", channelStatuses);

        // Agents
        components.put("agentCount", agentManager.getAgentCount());

        // Scheduler
        components.put("scheduledTasks", taskScheduler.getTaskCount());

        // Sessions
        components.put("activeSessions", sessionManager.getActiveSessionCount());

        // Audit
        components.put("auditEvents", auditLogger.getTotalEventCount());

        // Skills
        Map<String, Object> skillInfo = new LinkedHashMap<>();
        skillInfo.put("count", skillRegistry.getSkillCount());
        skillInfo.put("names", skillRegistry.getSkillNames());
        components.put("skills", skillInfo);

        // Tools
        Map<String, Object> toolInfo = new LinkedHashMap<>();
        toolInfo.put("count", toolRegistry.size());
        toolInfo.put("names", toolRegistry.getAllToolNames());
        components.put("tools", toolInfo);

        result.put("components", components);
        return result;
    }
}
