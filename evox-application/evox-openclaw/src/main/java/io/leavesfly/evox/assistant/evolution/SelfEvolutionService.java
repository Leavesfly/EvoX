package io.leavesfly.evox.assistant.evolution;

import io.leavesfly.evox.core.agent.IAgent;
import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.core.message.MessageType;
import io.leavesfly.evox.scheduler.core.IScheduledTask;
import io.leavesfly.evox.scheduler.core.TaskContext;
import io.leavesfly.evox.scheduler.core.TaskResult;
import io.leavesfly.evox.scheduler.core.TaskScheduler;
import io.leavesfly.evox.scheduler.trigger.HeartbeatTrigger;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 自进化服务 — 让 Agent 的 Prompt 和行为随使用不断优化。
 *
 * <p>核心机制：
 * <ul>
 *   <li>收集用户交互的反馈信号（满意度、任务完成率等）</li>
 *   <li>基于进化算法（EvoPrompt）对 Agent 的系统提示词进行迭代优化</li>
 *   <li>通过定时任务周期性触发优化流程</li>
 *   <li>维护 Prompt 版本历史，支持回滚</li>
 * </ul>
 *
 * <p>与 OpenClaw 的"自我改进"能力对标，但基于 EvoX 的进化算法框架实现更系统化的优化。
 */
@Slf4j
public class SelfEvolutionService {

    private final IAgent agent;
    private final SelfEvolutionConfig config;

    /** 反馈信号收集 */
    private final List<FeedbackSignal> feedbackBuffer = Collections.synchronizedList(new ArrayList<>());

    /** Prompt 版本历史 */
    private final List<PromptVersion> promptHistory = Collections.synchronizedList(new ArrayList<>());

    /** 当前活跃的 Prompt */
    private volatile String currentSystemPrompt;

    /** 优化统计 */
    private volatile int totalOptimizations = 0;
    private volatile int totalImprovements = 0;
    private volatile Instant lastOptimizationTime;

    /** 候选 Prompt 种群 */
    private final Map<String, Double> candidateScores = new ConcurrentHashMap<>();

    public SelfEvolutionService(IAgent agent, SelfEvolutionConfig config) {
        this.agent = agent;
        this.config = config;
    }

    /**
     * 注册为定时任务到 TaskScheduler
     */
    public void registerOptimizationTask(TaskScheduler taskScheduler) {
        if (!config.isEnabled()) {
            log.info("SelfEvolutionService is disabled");
            return;
        }

        IScheduledTask optimizationTask = new IScheduledTask() {
            @Override
            public String getTaskId() {
                return "self-evolution-optimization";
            }

            @Override
            public String getTaskName() {
                return "Self Evolution Prompt Optimization";
            }

            @Override
            public String getDescription() {
                return "Periodically optimizes Agent prompts based on collected feedback signals";
            }

            @Override
            public HeartbeatTrigger getTrigger() {
                return new HeartbeatTrigger(config.getOptimizationIntervalMs());
            }

            @Override
            public TaskResult execute(TaskContext context) {
                return runOptimization();
            }

            @Override
            public boolean isEnabled() {
                return config.isEnabled();
            }
        };

        taskScheduler.scheduleTask(optimizationTask);
        log.info("SelfEvolutionService registered optimization task: interval={}ms, minFeedback={}",
                config.getOptimizationIntervalMs(), config.getMinFeedbackForOptimization());
    }

    /**
     * 收集反馈信号
     */
    public void recordFeedback(FeedbackSignal feedback) {
        feedbackBuffer.add(feedback);
        log.debug("Feedback recorded: type={}, score={}", feedback.getType(), feedback.getScore());
    }

    /**
     * 执行一轮优化
     */
    public TaskResult runOptimization() {
        totalOptimizations++;
        lastOptimizationTime = Instant.now();

        if (feedbackBuffer.size() < config.getMinFeedbackForOptimization()) {
            log.debug("Not enough feedback for optimization: {}/{}", 
                    feedbackBuffer.size(), config.getMinFeedbackForOptimization());
            return TaskResult.success("Skipped: insufficient feedback (" 
                    + feedbackBuffer.size() + "/" + config.getMinFeedbackForOptimization() + ")");
        }

        List<FeedbackSignal> feedbackBatch;
        synchronized (feedbackBuffer) {
            feedbackBatch = new ArrayList<>(feedbackBuffer);
            feedbackBuffer.clear();
        }

        log.info("Starting self-evolution optimization with {} feedback signals", feedbackBatch.size());

        try {
            String analysisPrompt = buildOptimizationPrompt(feedbackBatch);

            Message inputMessage = Message.builder()
                    .content(analysisPrompt)
                    .messageType(MessageType.SYSTEM)
                    .build();
            inputMessage.putMetadata("evolutionRound", totalOptimizations);
            inputMessage.putMetadata("feedbackCount", feedbackBatch.size());

            Message result = agent.execute("optimize-prompt", List.of(inputMessage));

            if (result != null && result.getContent() != null) {
                String optimizedPrompt = result.getContent().toString();
                double averageFeedbackScore = feedbackBatch.stream()
                        .mapToDouble(FeedbackSignal::getScore)
                        .average()
                        .orElse(0.0);

                evaluateAndApplyPrompt(optimizedPrompt, averageFeedbackScore);

                log.info("Self-evolution optimization #{} completed. Feedback avg score: {:.2f}",
                        totalOptimizations, averageFeedbackScore);
                return TaskResult.success(Map.of(
                        "round", totalOptimizations,
                        "feedbackCount", feedbackBatch.size(),
                        "averageScore", averageFeedbackScore,
                        "promptUpdated", true
                ));
            }

            return TaskResult.success("Optimization completed but no prompt update generated");
        } catch (Exception e) {
            log.error("Self-evolution optimization failed", e);
            return TaskResult.failure("Optimization failed: " + e.getMessage());
        }
    }

    /**
     * 构建优化提示词
     */
    private String buildOptimizationPrompt(List<FeedbackSignal> feedbackBatch) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("## Self-Evolution Prompt Optimization\n\n");
        prompt.append("You are optimizing your own system prompt based on user feedback.\n\n");

        prompt.append("### Current System Prompt\n");
        prompt.append("```\n");
        prompt.append(currentSystemPrompt != null ? currentSystemPrompt : "(default system prompt)");
        prompt.append("\n```\n\n");

        prompt.append("### User Feedback Summary (").append(feedbackBatch.size()).append(" signals)\n\n");

        Map<FeedbackSignal.FeedbackType, List<FeedbackSignal>> grouped = new LinkedHashMap<>();
        for (FeedbackSignal signal : feedbackBatch) {
            grouped.computeIfAbsent(signal.getType(), k -> new ArrayList<>()).add(signal);
        }

        for (Map.Entry<FeedbackSignal.FeedbackType, List<FeedbackSignal>> entry : grouped.entrySet()) {
            List<FeedbackSignal> signals = entry.getValue();
            double avgScore = signals.stream().mapToDouble(FeedbackSignal::getScore).average().orElse(0);
            prompt.append("- **").append(entry.getKey()).append("**: ")
                    .append(signals.size()).append(" signals, avg score: ")
                    .append(String.format("%.2f", avgScore)).append("\n");

            for (FeedbackSignal signal : signals) {
                if (signal.getComment() != null && !signal.getComment().isBlank()) {
                    prompt.append("  - \"").append(signal.getComment()).append("\" (score: ")
                            .append(signal.getScore()).append(")\n");
                }
            }
        }

        prompt.append("\n### Instructions\n\n");
        prompt.append("Based on the feedback above, generate an improved version of the system prompt.\n");
        prompt.append("Focus on:\n");
        prompt.append("1. Addressing areas with low scores\n");
        prompt.append("2. Reinforcing behaviors that received positive feedback\n");
        prompt.append("3. Maintaining existing strengths\n");
        prompt.append("4. Being more specific and actionable\n\n");
        prompt.append("Output ONLY the improved system prompt text, without any explanation or markdown formatting.\n");

        return prompt.toString();
    }

    /**
     * 评估并应用优化后的 Prompt
     */
    private void evaluateAndApplyPrompt(String candidatePrompt, double feedbackScore) {
        if (candidatePrompt == null || candidatePrompt.isBlank()) {
            return;
        }

        String trimmedPrompt = candidatePrompt.trim();
        if (trimmedPrompt.length() < 20) {
            log.warn("Candidate prompt too short, skipping: length={}", trimmedPrompt.length());
            return;
        }

        PromptVersion version = PromptVersion.builder()
                .version(promptHistory.size() + 1)
                .prompt(trimmedPrompt)
                .feedbackScore(feedbackScore)
                .createdAt(Instant.now())
                .optimizationRound(totalOptimizations)
                .build();

        promptHistory.add(version);
        candidateScores.put(trimmedPrompt, feedbackScore);

        if (currentSystemPrompt == null || feedbackScore >= config.getImprovementThreshold()) {
            String previousPrompt = currentSystemPrompt;
            currentSystemPrompt = trimmedPrompt;
            totalImprovements++;
            log.info("System prompt updated to version {} (score: {:.2f})", version.getVersion(), feedbackScore);

            if (previousPrompt != null) {
                log.debug("Previous prompt length: {}, new prompt length: {}",
                        previousPrompt.length(), trimmedPrompt.length());
            }
        }
    }

    /**
     * 回滚到指定版本的 Prompt
     */
    public boolean rollbackToVersion(int version) {
        for (PromptVersion pv : promptHistory) {
            if (pv.getVersion() == version) {
                currentSystemPrompt = pv.getPrompt();
                log.info("Rolled back system prompt to version {}", version);
                return true;
            }
        }
        log.warn("Prompt version {} not found", version);
        return false;
    }

    /**
     * 获取当前系统提示词
     */
    public String getCurrentSystemPrompt() {
        return currentSystemPrompt;
    }

    /**
     * 获取 Prompt 版本历史
     */
    public List<PromptVersion> getPromptHistory() {
        return Collections.unmodifiableList(promptHistory);
    }

    /**
     * 获取优化统计
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalOptimizations", totalOptimizations);
        stats.put("totalImprovements", totalImprovements);
        stats.put("lastOptimizationTime", lastOptimizationTime);
        stats.put("pendingFeedback", feedbackBuffer.size());
        stats.put("promptVersions", promptHistory.size());
        stats.put("currentPromptLength", currentSystemPrompt != null ? currentSystemPrompt.length() : 0);
        return stats;
    }

    // ---- 内部数据类 ----

    /**
     * 反馈信号
     */
    @Data
    @Builder
    public static class FeedbackSignal {
        /** 反馈类型 */
        private FeedbackType type;
        /** 分数（0.0 ~ 1.0） */
        private double score;
        /** 用户评论 */
        private String comment;
        /** 关联的用户输入 */
        private String userInput;
        /** 关联的 Agent 输出 */
        private String agentOutput;
        /** 时间戳 */
        @Builder.Default
        private Instant timestamp = Instant.now();

        public enum FeedbackType {
            /** 用户显式评分 */
            USER_RATING,
            /** 任务完成状态 */
            TASK_COMPLETION,
            /** 响应质量 */
            RESPONSE_QUALITY,
            /** 工具调用成功率 */
            TOOL_SUCCESS_RATE,
            /** 对话满意度 */
            CONVERSATION_SATISFACTION
        }
    }

    /**
     * Prompt 版本
     */
    @Data
    @Builder
    public static class PromptVersion {
        private int version;
        private String prompt;
        private double feedbackScore;
        private Instant createdAt;
        private int optimizationRound;
    }

    /**
     * 自进化配置
     */
    @Data
    @Builder
    public static class SelfEvolutionConfig {
        /** 是否启用自进化 */
        @Builder.Default
        private boolean enabled = true;

        /** 优化间隔（毫秒），默认 1 小时 */
        @Builder.Default
        private long optimizationIntervalMs = 3_600_000;

        /** 触发优化所需的最少反馈数量 */
        @Builder.Default
        private int minFeedbackForOptimization = 10;

        /** 改进阈值（反馈平均分高于此值才更新 Prompt） */
        @Builder.Default
        private double improvementThreshold = 0.5;
    }
}
