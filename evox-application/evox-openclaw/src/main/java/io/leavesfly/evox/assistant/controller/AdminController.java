package io.leavesfly.evox.assistant.controller;

import io.leavesfly.evox.agents.skill.BaseSkill;
import io.leavesfly.evox.agents.skill.SkillRegistry;
import io.leavesfly.evox.channels.core.ChannelRegistry;
import io.leavesfly.evox.channels.core.IChannel;
import io.leavesfly.evox.core.agent.IAgent;
import io.leavesfly.evox.core.agent.IAgentManager;
import io.leavesfly.evox.gateway.audit.AuditEvent;
import io.leavesfly.evox.gateway.audit.AuditLogger;
import io.leavesfly.evox.gateway.session.SessionManager;
import io.leavesfly.evox.tools.api.ToolRegistry;
import io.leavesfly.evox.tools.base.BaseTool;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final IAgentManager agentManager;
    private final ChannelRegistry channelRegistry;
    private final SkillRegistry skillRegistry;
    private final ToolRegistry toolRegistry;
    private final SessionManager sessionManager;
    private final AuditLogger auditLogger;

    @GetMapping("/dashboard")
    public Map<String, Object> getDashboard() {
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("agentCount", agentManager.getAgentCount());
        dashboard.put("channelCount", channelRegistry.getChannelCount());
        dashboard.put("skillCount", skillRegistry.getSkillCount());
        dashboard.put("toolCount", toolRegistry.size());
        dashboard.put("activeSessionCount", sessionManager.getActiveSessionCount());
        dashboard.put("totalAuditEventCount", auditLogger.getTotalEventCount());
        return dashboard;
    }

    @GetMapping("/agents")
    public List<Map<String, Object>> getAllAgents() {
        return agentManager.getAllAgents().entrySet().stream()
                .map(entry -> {
                    Map<String, Object> agentInfo = new HashMap<>();
                    agentInfo.put("name", entry.getKey());
                    agentInfo.put("className", entry.getValue().getClass().getName());
                    return agentInfo;
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/agents/{name}")
    public Map<String, Object> getAgent(@PathVariable String name) {
        try {
            IAgent agent = agentManager.getAgent(name);
            if (agent == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Agent not found: " + name);
                return error;
            }
            Map<String, Object> agentInfo = new HashMap<>();
            agentInfo.put("name", name);
            agentInfo.put("className", agent.getClass().getName());
            return agentInfo;
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to get agent: " + e.getMessage());
            return error;
        }
    }

    @GetMapping("/channels")
    public List<Map<String, Object>> getAllChannels() {
        return channelRegistry.getAllChannels().stream()
                .map(channel -> {
                    Map<String, Object> channelInfo = new HashMap<>();
                    channelInfo.put("id", channel.getChannelId());
                    channelInfo.put("name", channel.getChannelName());
                    channelInfo.put("status", channel.getStatus().toString());
                    return channelInfo;
                })
                .collect(Collectors.toList());
    }

    @PostMapping("/channels/{channelId}/start")
    public Map<String, Object> startChannel(@PathVariable String channelId) {
        try {
            IChannel channel = channelRegistry.getChannel(channelId);
            if (channel == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Channel not found: " + channelId);
                return error;
            }
            channel.start();
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Channel started: " + channelId);
            return result;
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to start channel: " + e.getMessage());
            return error;
        }
    }

    @PostMapping("/channels/{channelId}/stop")
    public Map<String, Object> stopChannel(@PathVariable String channelId) {
        try {
            IChannel channel = channelRegistry.getChannel(channelId);
            if (channel == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Channel not found: " + channelId);
                return error;
            }
            channel.stop();
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Channel stopped: " + channelId);
            return result;
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to stop channel: " + e.getMessage());
            return error;
        }
    }

    @GetMapping("/skills")
    public List<Map<String, Object>> getAllSkills() {
        return skillRegistry.getAllSkills().stream()
                .map(skill -> {
                    Map<String, Object> skillInfo = new HashMap<>();
                    skillInfo.put("name", skill.getName());
                    skillInfo.put("description", skill.getDescription());
                    skillInfo.put("requiredTools", skill.getRequiredTools());
                    skillInfo.put("inputParameters", skill.getInputParameters());
                    return skillInfo;
                })
                .collect(Collectors.toList());
    }

    @PostMapping("/skills/{skillName}/execute")
    public Map<String, Object> executeSkill(@PathVariable String skillName, @RequestBody Map<String, Object> requestBody) {
        try {
            String input = (String) requestBody.get("input");
            Map<String, Object> parameters = (Map<String, Object>) requestBody.get("parameters");
            
            BaseSkill.SkillContext context;
            if (parameters != null) {
                context = new BaseSkill.SkillContext(input, parameters);
            } else {
                context = new BaseSkill.SkillContext(input);
            }
            
            BaseSkill.SkillResult result = skillRegistry.executeSkill(skillName, context);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("output", result.getOutput());
            response.put("error", result.getError());
            return response;
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to execute skill: " + e.getMessage());
            return error;
        }
    }

    @DeleteMapping("/skills/{skillName}")
    public Map<String, Object> removeSkill(@PathVariable String skillName) {
        try {
            BaseSkill removedSkill = skillRegistry.removeSkill(skillName);
            if (removedSkill == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Skill not found: " + skillName);
                return error;
            }
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Skill removed: " + skillName);
            return result;
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to remove skill: " + e.getMessage());
            return error;
        }
    }

    @GetMapping("/tools")
    public List<Map<String, Object>> getAllTools() {
        return toolRegistry.getAllTools().stream()
                .map(tool -> {
                    Map<String, Object> toolInfo = new HashMap<>();
                    toolInfo.put("name", tool.getName());
                    toolInfo.put("description", tool.getDescription());
                    return toolInfo;
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/tools/categories")
    public Set<String> getToolCategories() {
        return toolRegistry.getCategories();
    }

    @GetMapping("/sessions")
    public Map<String, Object> getActiveSessions() {
        Map<String, Object> sessionsInfo = new HashMap<>();
        sessionsInfo.put("activeSessionCount", sessionManager.getActiveSessionCount());
        return sessionsInfo;
    }

    @DeleteMapping("/sessions/{userId}")
    public Map<String, Object> removeSession(@PathVariable String userId) {
        try {
            sessionManager.removeSession(userId);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Session removed for user: " + userId);
            return result;
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to remove session: " + e.getMessage());
            return error;
        }
    }

    @GetMapping("/audit")
    public List<AuditEvent> getRecentAuditEvents(@RequestParam(defaultValue = "50") int count) {
        return auditLogger.getRecentEvents(count);
    }

    @GetMapping("/audit/user/{userId}")
    public List<AuditEvent> getUserAuditEvents(@PathVariable String userId, @RequestParam(defaultValue = "50") int count) {
        return auditLogger.getEventsByUser(userId, count);
    }
}