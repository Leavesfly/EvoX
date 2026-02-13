package io.leavesfly.evox.assistant.controller;

import io.leavesfly.evox.assistant.evolution.SelfEvolutionService;
import io.leavesfly.evox.assistant.evolution.SkillGenerator;
import io.leavesfly.evox.scheduler.heartbeat.HeartbeatRunner;
import io.leavesfly.evox.scheduler.heartbeat.SystemEvent;
import io.leavesfly.evox.scheduler.heartbeat.SystemEventQueue;
import lombok.Data;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 进化能力控制器 — 暴露 Heartbeat 主动唤醒、自进化、Skill 自动生成的 REST API。
 *
 * <p>这是 EvoX OpenClaw 的核心差异化能力入口，提供：
 * <ul>
 *   <li><b>Heartbeat</b>：查看心跳状态、手动触发唤醒、发送系统事件</li>
 *   <li><b>Self-Evolution</b>：提交反馈、查看优化历史、回滚 Prompt 版本</li>
 *   <li><b>Skill Generator</b>：描述需求自动生成新 Skill、查看已生成技能</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/evolution")
public class EvolutionController {

    private final HeartbeatRunner heartbeatRunner;
    private final SelfEvolutionService selfEvolutionService;
    private final SkillGenerator skillGenerator;
    private final SystemEventQueue systemEventQueue;

    public EvolutionController(ObjectProvider<HeartbeatRunner> heartbeatRunnerProvider,
                               ObjectProvider<SelfEvolutionService> selfEvolutionServiceProvider,
                               ObjectProvider<SkillGenerator> skillGeneratorProvider,
                               SystemEventQueue systemEventQueue) {
        this.heartbeatRunner = heartbeatRunnerProvider.getIfAvailable();
        this.selfEvolutionService = selfEvolutionServiceProvider.getIfAvailable();
        this.skillGenerator = skillGeneratorProvider.getIfAvailable();
        this.systemEventQueue = systemEventQueue;
    }

    // ========== 综合状态 ==========

    /**
     * 获取所有进化能力的综合状态
     */
    @GetMapping("/status")
    public Map<String, Object> getEvolutionStatus() {
        Map<String, Object> status = new LinkedHashMap<>();

        // Heartbeat 状态
        Map<String, Object> heartbeatStatus = new LinkedHashMap<>();
        if (heartbeatRunner != null) {
            heartbeatStatus.put("enabled", true);
            heartbeatStatus.put("running", heartbeatRunner.isRunning());
            heartbeatStatus.put("totalHeartbeats", heartbeatRunner.getTotalHeartbeats());
            heartbeatStatus.put("totalEventsProcessed", heartbeatRunner.getTotalEventsProcessed());
            heartbeatStatus.put("pendingEvents", heartbeatRunner.getPendingEventCount());
            Instant lastTime = heartbeatRunner.getLastHeartbeatTime();
            heartbeatStatus.put("lastHeartbeatTime", lastTime != null ? lastTime.toString() : null);
        } else {
            heartbeatStatus.put("enabled", false);
        }
        status.put("heartbeat", heartbeatStatus);

        // Self-Evolution 状态
        Map<String, Object> evolutionStatus = new LinkedHashMap<>();
        if (selfEvolutionService != null) {
            evolutionStatus.put("enabled", true);
            evolutionStatus.putAll(selfEvolutionService.getStatistics());
        } else {
            evolutionStatus.put("enabled", false);
        }
        status.put("selfEvolution", evolutionStatus);

        // Skill Generator 状态
        Map<String, Object> generatorStatus = new LinkedHashMap<>();
        if (skillGenerator != null) {
            generatorStatus.put("enabled", true);
            generatorStatus.put("generatedSkillCount", skillGenerator.getGeneratedSkills().size());
            generatorStatus.put("generatedSkillNames",
                    new ArrayList<>(skillGenerator.getGeneratedSkills().keySet()));
        } else {
            generatorStatus.put("enabled", false);
        }
        status.put("skillGenerator", generatorStatus);

        return status;
    }

    // ========== Heartbeat 主动唤醒 ==========

    /**
     * 获取 Heartbeat 详细状态
     */
    @GetMapping("/heartbeat")
    public Map<String, Object> getHeartbeatStatus() {
        Map<String, Object> result = new LinkedHashMap<>();
        if (heartbeatRunner == null) {
            result.put("enabled", false);
            result.put("message", "HeartbeatRunner is not configured");
            return result;
        }

        result.put("enabled", true);
        result.put("running", heartbeatRunner.isRunning());
        result.put("totalHeartbeats", heartbeatRunner.getTotalHeartbeats());
        result.put("totalEventsProcessed", heartbeatRunner.getTotalEventsProcessed());
        result.put("pendingEvents", heartbeatRunner.getPendingEventCount());
        Instant lastTime = heartbeatRunner.getLastHeartbeatTime();
        result.put("lastHeartbeatTime", lastTime != null ? lastTime.toString() : null);
        return result;
    }

    /**
     * 手动触发立即唤醒
     */
    @PostMapping("/heartbeat/wake")
    public Map<String, Object> triggerWake() {
        Map<String, Object> result = new LinkedHashMap<>();
        if (heartbeatRunner == null) {
            result.put("success", false);
            result.put("message", "HeartbeatRunner is not configured");
            return result;
        }
        if (!heartbeatRunner.isRunning()) {
            result.put("success", false);
            result.put("message", "HeartbeatRunner is not running");
            return result;
        }

        heartbeatRunner.wakeNow();
        result.put("success", true);
        result.put("message", "Immediate wake triggered");
        return result;
    }

    /**
     * 发送系统事件到事件队列
     */
    @PostMapping("/heartbeat/event")
    public Map<String, Object> sendSystemEvent(@RequestBody SystemEventRequest request) {
        Map<String, Object> result = new LinkedHashMap<>();

        SystemEvent.WakeMode wakeMode = "NOW".equalsIgnoreCase(request.getWakeMode())
                ? SystemEvent.WakeMode.NOW
                : SystemEvent.WakeMode.NEXT_HEARTBEAT;

        SystemEvent event = SystemEvent.builder()
                .source(request.getSource() != null ? request.getSource() : "web-ui")
                .message(request.getMessage())
                .wakeMode(wakeMode)
                .build();

        if (heartbeatRunner != null) {
            heartbeatRunner.enqueueEvent(event);
        } else {
            systemEventQueue.enqueue(event);
        }

        result.put("success", true);
        result.put("message", "System event enqueued");
        result.put("wakeMode", wakeMode.name());
        result.put("pendingEvents", systemEventQueue.size());
        return result;
    }

    // ========== Self-Evolution 自进化 ==========

    /**
     * 获取自进化统计信息
     */
    @GetMapping("/self-evolution")
    public Map<String, Object> getSelfEvolutionStatus() {
        Map<String, Object> result = new LinkedHashMap<>();
        if (selfEvolutionService == null) {
            result.put("enabled", false);
            result.put("message", "SelfEvolutionService is not configured. Enable it in application.yml");
            return result;
        }

        result.put("enabled", true);
        result.putAll(selfEvolutionService.getStatistics());

        String currentPrompt = selfEvolutionService.getCurrentSystemPrompt();
        result.put("currentPromptPreview", currentPrompt != null
                ? currentPrompt.substring(0, Math.min(200, currentPrompt.length())) + "..."
                : "(default)");

        return result;
    }

    /**
     * 提交反馈信号
     */
    @PostMapping("/self-evolution/feedback")
    public Map<String, Object> submitFeedback(@RequestBody FeedbackRequest request) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (selfEvolutionService == null) {
            result.put("success", false);
            result.put("message", "SelfEvolutionService is not configured");
            return result;
        }

        SelfEvolutionService.FeedbackSignal.FeedbackType feedbackType;
        try {
            feedbackType = SelfEvolutionService.FeedbackSignal.FeedbackType.valueOf(
                    request.getType() != null ? request.getType().toUpperCase() : "USER_RATING");
        } catch (IllegalArgumentException e) {
            feedbackType = SelfEvolutionService.FeedbackSignal.FeedbackType.USER_RATING;
        }

        SelfEvolutionService.FeedbackSignal feedback = SelfEvolutionService.FeedbackSignal.builder()
                .type(feedbackType)
                .score(Math.max(0.0, Math.min(1.0, request.getScore())))
                .comment(request.getComment())
                .userInput(request.getUserInput())
                .agentOutput(request.getAgentOutput())
                .build();

        selfEvolutionService.recordFeedback(feedback);

        result.put("success", true);
        result.put("message", "Feedback recorded");
        result.put("feedbackType", feedbackType.name());
        result.put("score", feedback.getScore());
        return result;
    }

    /**
     * 获取 Prompt 版本历史
     */
    @GetMapping("/self-evolution/history")
    public Map<String, Object> getPromptHistory() {
        Map<String, Object> result = new LinkedHashMap<>();
        if (selfEvolutionService == null) {
            result.put("enabled", false);
            result.put("versions", Collections.emptyList());
            return result;
        }

        List<Map<String, Object>> versions = selfEvolutionService.getPromptHistory().stream()
                .map(version -> {
                    Map<String, Object> versionInfo = new LinkedHashMap<>();
                    versionInfo.put("version", version.getVersion());
                    versionInfo.put("feedbackScore", version.getFeedbackScore());
                    versionInfo.put("createdAt", version.getCreatedAt().toString());
                    versionInfo.put("optimizationRound", version.getOptimizationRound());
                    String promptText = version.getPrompt();
                    versionInfo.put("promptPreview",
                            promptText.substring(0, Math.min(100, promptText.length())) + "...");
                    return versionInfo;
                })
                .collect(Collectors.toList());

        result.put("enabled", true);
        result.put("versions", versions);
        result.put("currentVersion", versions.isEmpty() ? 0 : versions.size());
        return result;
    }

    /**
     * 回滚到指定版本的 Prompt
     */
    @PostMapping("/self-evolution/rollback/{version}")
    public Map<String, Object> rollbackPrompt(@PathVariable int version) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (selfEvolutionService == null) {
            result.put("success", false);
            result.put("message", "SelfEvolutionService is not configured");
            return result;
        }

        boolean success = selfEvolutionService.rollbackToVersion(version);
        result.put("success", success);
        result.put("message", success
                ? "Rolled back to prompt version " + version
                : "Version " + version + " not found");
        return result;
    }

    /**
     * 手动触发一轮优化
     */
    @PostMapping("/self-evolution/optimize")
    public Map<String, Object> triggerOptimization() {
        Map<String, Object> result = new LinkedHashMap<>();
        if (selfEvolutionService == null) {
            result.put("success", false);
            result.put("message", "SelfEvolutionService is not configured");
            return result;
        }

        var taskResult = selfEvolutionService.runOptimization();
        result.put("success", taskResult.isSuccess());
        result.put("optimizationResult", taskResult.isSuccess()
                ? String.valueOf(taskResult.getData())
                : taskResult.getError());
        return result;
    }

    // ========== Skill Generator 技能自动生成 ==========

    /**
     * 获取 Skill Generator 状态和已生成技能列表
     */
    @GetMapping("/skill-generator")
    public Map<String, Object> getSkillGeneratorStatus() {
        Map<String, Object> result = new LinkedHashMap<>();
        if (skillGenerator == null) {
            result.put("enabled", false);
            result.put("message", "SkillGenerator is not configured");
            return result;
        }

        result.put("enabled", true);

        List<Map<String, Object>> generatedSkillList = skillGenerator.getGeneratedSkills().values().stream()
                .map(record -> {
                    Map<String, Object> skillInfo = new LinkedHashMap<>();
                    skillInfo.put("name", record.getSkillName());
                    skillInfo.put("description", record.getDescription());
                    skillInfo.put("originalRequest", record.getOriginalRequest());
                    skillInfo.put("generatedAt", record.getGeneratedAt().toString());
                    return skillInfo;
                })
                .collect(Collectors.toList());

        result.put("generatedSkills", generatedSkillList);
        result.put("generatedSkillCount", generatedSkillList.size());
        return result;
    }

    /**
     * 根据描述生成并安装新 Skill
     */
    @PostMapping("/skill-generator/generate")
    public Map<String, Object> generateSkill(@RequestBody SkillGenerationRequest request) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (skillGenerator == null) {
            result.put("success", false);
            result.put("message", "SkillGenerator is not configured");
            return result;
        }

        if (request.getDescription() == null || request.getDescription().isBlank()) {
            result.put("success", false);
            result.put("message", "Skill description is required");
            return result;
        }

        SkillGenerator.GenerationResult generationResult =
                skillGenerator.generateAndInstall(request.getDescription());

        result.put("success", generationResult.isSuccess());
        result.put("message", generationResult.getMessage());
        if (generationResult.isSuccess()) {
            result.put("skillName", generationResult.getSkillName());
            result.put("skillDescription", generationResult.getSkillDescription());
        }
        return result;
    }

    /**
     * 卸载已生成的 Skill
     */
    @DeleteMapping("/skill-generator/{skillName}")
    public Map<String, Object> uninstallGeneratedSkill(@PathVariable String skillName) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (skillGenerator == null) {
            result.put("success", false);
            result.put("message", "SkillGenerator is not configured");
            return result;
        }

        boolean success = skillGenerator.uninstallGeneratedSkill(skillName);
        result.put("success", success);
        result.put("message", success
                ? "Generated skill '" + skillName + "' uninstalled"
                : "Generated skill '" + skillName + "' not found");
        return result;
    }

    // ========== 请求体 DTO ==========

    @Data
    public static class SystemEventRequest {
        private String source;
        private String message;
        /** NOW 或 NEXT_HEARTBEAT */
        private String wakeMode;
    }

    @Data
    public static class FeedbackRequest {
        /** USER_RATING / TASK_COMPLETION / RESPONSE_QUALITY / TOOL_SUCCESS_RATE / CONVERSATION_SATISFACTION */
        private String type;
        /** 0.0 ~ 1.0 */
        private double score;
        private String comment;
        private String userInput;
        private String agentOutput;
    }

    @Data
    public static class SkillGenerationRequest {
        private String description;
    }
}
