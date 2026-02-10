package io.leavesfly.evox.frameworks.debate;


import io.leavesfly.evox.models.base.LLMProvider;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;

import java.util.stream.Collectors;

/**
 * 多智能体辩论框架
 * 允许多个智能体通过辩论达成共识
 *
 * @author EvoX Team
 */
@Slf4j
@Data
public class MultiAgentDebate {

    /**
     * 参与辩论的智能体列表
     */
    private List<DebateAgent> agents;

    /**
     * 最大辩论轮数
     */
    private int maxRounds;

    /**
     * 当前辩论轮数
     */
    private int currentRound;

    /**
     * 辩论历史
     */
    private List<DebateRecord> history;

    /**
     * 辩论主持人（可选，用于检查共识和生成最终答案）
     */
    private LLMProvider moderator;

    /**
     * 辩论模式
     */
    private DebateMode mode = DebateMode.ROUND_ROBIN;

    /**
     * 辩论配置
     */
    private DebateConfig config;

    /**
     * 观点跟踪器 (agentName -> 观点列表)
     */
    private final Map<String, List<Viewpoint>> viewpointTracker = new ConcurrentHashMap<>();

    /**
     * 评分记录 (agentName -> 得分)
     */
    private final Map<String, Double> scores = new ConcurrentHashMap<>();

    /**
     * 辩论状态
     */
    private DebateStatus status = DebateStatus.NOT_STARTED;

    /**
     * 辩论开始时间
     */
    private long startTime;

    /**
     * 事件监听器
     */
    private final List<DebateEventListener> eventListeners = new ArrayList<>();

    public MultiAgentDebate(List<DebateAgent> agents, int maxRounds) {
        this(agents, maxRounds, null);
    }

    public MultiAgentDebate(List<DebateAgent> agents, int maxRounds, LLMProvider moderator) {
        this.agents = agents;
        this.maxRounds = maxRounds;
        this.moderator = moderator;
        this.currentRound = 0;
        this.history = new ArrayList<>();
        this.config = DebateConfig.builder().build();

        // 初始化每个智能体的分数
        agents.forEach(agent -> scores.put(agent.getName(), 0.0));
    }

    public MultiAgentDebate(List<DebateAgent> agents, int maxRounds, LLMProvider moderator, DebateConfig config) {
        this(agents, maxRounds, moderator);
        this.config = config != null ? config : DebateConfig.builder().build();
    }

    /**
     * 开始辩论
     */
    public DebateResult debate(String question) {
        log.info("开始辩论: {}", question);

        startTime = System.currentTimeMillis();
        status = DebateStatus.IN_PROGRESS;
        fireEvent(DebateEventType.DEBATE_STARTED, null);

        try {
            // 根据模式执行辩论
            String finalAnswer = switch (mode) {
                case ROUND_ROBIN -> executeRoundRobin(question);
                case ADVERSARIAL -> executeAdversarial(question);
                case PANEL -> executePanel(question);
                case SOCRATIC -> executeSocratic(question);
            };

            status = DebateStatus.COMPLETED;
            fireEvent(DebateEventType.DEBATE_ENDED, null);

            return buildDebateResult(question, finalAnswer);

        } catch (Exception e) {
            status = DebateStatus.ERROR;
            log.error("辩论执行失败", e);
            return DebateResult.builder()
                    .success(false)
                    .error(e.getMessage())
                    .build();
        }
    }

    /**
     * 轮流模式
     */
    private String executeRoundRobin(String question) {
        for (currentRound = 1; currentRound <= maxRounds; currentRound++) {
            log.info("第 {}/{} 轮", currentRound, maxRounds);
            fireEvent(DebateEventType.ROUND_STARTED, currentRound);

            for (DebateAgent agent : agents) {
                String response = executeAgentResponse(agent, question);
                processResponse(agent, response);
            }

            fireEvent(DebateEventType.ROUND_ENDED, currentRound);

            if (checkConsensus()) {
                log.info("第 {} 轮达成共识", currentRound);
                break;
            }
        }

        return generateFinalAnswer();
    }

    /**
     * 对抗模式（正方vs反方）
     */
    private String executeAdversarial(String question) {
        if (agents.size() < 2) {
            throw new IllegalStateException("对抗模式至少需要2个智能体");
        }

        DebateAgent proponent = agents.get(0);  // 正方
        DebateAgent opponent = agents.get(1);   // 反方

        for (currentRound = 1; currentRound <= maxRounds; currentRound++) {
            log.info("对抗第 {}/{} 轮", currentRound, maxRounds);

            // 正方发言
            String proResponse = executeAgentResponse(proponent,
                    "作为正方，请针对'" + question + "'提出支持观点");
            processResponse(proponent, proResponse);

            // 反方发言
            String oppResponse = executeAgentResponse(opponent,
                    "作为反方，请针对'" + question + "'提出反对观点");
            processResponse(opponent, oppResponse);

            // 交叉质询
            if (config.isEnableCrossExamination()) {
                executeCrossExamination(proponent, opponent);
            }
        }

        return generateFinalAnswer();
    }

    /**
     * 圆桌讨论模式
     */
    private String executePanel(String question) {
        for (currentRound = 1; currentRound <= maxRounds; currentRound++) {
            log.info("圆桌讨论第 {}/{} 轮", currentRound, maxRounds);

            // 所有智能体同时发言
            List<String> responses = new ArrayList<>();
            for (DebateAgent agent : agents) {
                String response = executeAgentResponse(agent, question);
                responses.add(response);
                processResponse(agent, response);
            }

            // 回应彼此的观点
            if (currentRound < maxRounds) {
                for (int i = 0; i < agents.size(); i++) {
                    DebateAgent agent = agents.get(i);
                    String rebuttal = agent.respond(
                            "请回应其他人的观点: " + String.join("; ", responses),
                            history
                    );
                    processResponse(agent, rebuttal);
                }
            }

            if (checkConsensus()) {
                break;
            }
        }

        return generateFinalAnswer();
    }

    /**
     * 苏格拉底式讨论模式（主持人引导）
     */
    private String executeSocratic(String question) {
        if (moderator == null) {
            throw new IllegalStateException("苏格拉底模式需要主持人");
        }

        String currentQuestion = question;

        for (currentRound = 1; currentRound <= maxRounds; currentRound++) {
            log.info("苏格拉底讨论第 {}/{} 轮", currentRound, maxRounds);

            // 每个智能体回答当前问题
            for (DebateAgent agent : agents) {
                String response = executeAgentResponse(agent, currentQuestion);
                processResponse(agent, response);
            }

            // 主持人提出后续问题
            if (currentRound < maxRounds) {
                currentQuestion = generateFollowUpQuestion();
                log.info("主持人后续问题: {}", currentQuestion);
            }
        }

        return generateFinalAnswer();
    }

    /**
     * 执行智能体回应
     */
    private String executeAgentResponse(DebateAgent agent, String question) {
        long startTime = System.currentTimeMillis();

        // 检查时间限制
        if (config.getResponseTimeoutMs() > 0) {
            try {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                Future<String> future = executor.submit(() -> agent.respond(question, history));
                String response = future.get(config.getResponseTimeoutMs(), TimeUnit.MILLISECONDS);
                executor.shutdown();
                return response;
            } catch (TimeoutException e) {
                log.warn("智能体 {} 响应超时", agent.getName());
                return "[响应超时]";
            } catch (Exception e) {
                log.error("智能体 {} 响应失败", agent.getName(), e);
                return "[响应失败: " + e.getMessage() + "]";
            }
        }

        return agent.respond(question, history);
    }

    /**
     * 处理响应
     */
    private void processResponse(DebateAgent agent, String response) {
        DebateRecord record = new DebateRecord(currentRound, agent.getName(), response);
        history.add(record);

        // 跟踪观点
        extractAndTrackViewpoints(agent.getName(), response);

        // 评分
        if (config.isEnableScoring()) {
            evaluateResponse(agent.getName(), response);
        }

        fireEvent(DebateEventType.AGENT_RESPONDED, record);
        log.debug("{}: {}", agent.getName(), response);
    }

    /**
     * 提取并跟踪观点
     */
    private void extractAndTrackViewpoints(String agentName, String response) {
        Viewpoint viewpoint = Viewpoint.builder()
                .round(currentRound)
                .content(response)
                .timestamp(System.currentTimeMillis())
                .build();

        viewpointTracker.computeIfAbsent(agentName, k -> new ArrayList<>()).add(viewpoint);
    }

    /**
     * 评估响应并评分
     */
    private void evaluateResponse(String agentName, String response) {
        // 简单的评分逻辑：基于响应长度和内容质量
        double score = 0.0;

        // 基础分：响应长度
        score += Math.min(response.length() / 100.0, 5.0);

        // 包含论据支持
        if (response.contains("因为") || response.contains("根据") || response.contains("研究表明")) {
            score += 2.0;
        }

        // 包含引用或数据
        if (response.matches(".*\\d+%.*") || response.matches(".*\\d+年.*")) {
            score += 1.0;
        }

        scores.merge(agentName, score, Double::sum);
    }

    /**
     * 执行交叉质询
     */
    private void executeCrossExamination(DebateAgent proponent, DebateAgent opponent) {
        // 正方质询反方
        String proQuestion = proponent.respond("请向反方提出一个关键问题", history);
        String oppAnswer = opponent.respond("请回答: " + proQuestion, history);
        history.add(new DebateRecord(currentRound, proponent.getName(), "质询: " + proQuestion));
        history.add(new DebateRecord(currentRound, opponent.getName(), "回答: " + oppAnswer));

        // 反方质询正方
        String oppQuestion = opponent.respond("请向正方提出一个关键问题", history);
        String proAnswer = proponent.respond("请回答: " + oppQuestion, history);
        history.add(new DebateRecord(currentRound, opponent.getName(), "质询: " + oppQuestion));
        history.add(new DebateRecord(currentRound, proponent.getName(), "回答: " + proAnswer));
    }

    /**
     * 生成后续问题
     */
    private String generateFollowUpQuestion() {
        StringBuilder sb = new StringBuilder();
        sb.append("基于以下辩论历史，请提出一个能够深化讨论的后续问题：\n\n");

        int startIdx = Math.max(0, history.size() - agents.size());
        for (int i = startIdx; i < history.size(); i++) {
            DebateRecord record = history.get(i);
            sb.append(String.format("%s: %s\n", record.getAgentName(), record.getResponse()));
        }
        sb.append("\n请生成一个能够引导更深入讨论的问题：");

        return moderator.generate(sb.toString());
    }

    /**
     * 检查是否达成共识
     */
    private boolean checkConsensus() {
        if (moderator == null || history.isEmpty()) {
            return false;
        }

        log.debug("Checking for consensus among agents...");

        // 构建共识检查提示词
        StringBuilder sb = new StringBuilder();
        sb.append("分析以下辩论历史，并判断所有参与者是否已达成明确的共识或一致意见。\n\n");
        sb.append("辩论历史：\n");
        for (DebateRecord record : history) {
            sb.append(String.format("[%d] %s: %s\n", record.getRound(), record.getAgentName(), record.getResponse()));
        }
        sb.append("\n是否已达成共识？请以 'YES' 或 'NO' 开头回答，并简要说明理由。");

        String response = moderator.generate(sb.toString());
        boolean reached = response.trim().toUpperCase().startsWith("YES");

        if (reached) {
            log.info("Consensus detected by moderator: {}", response);
        }

        return reached;
    }

    /**
     * 生成最终答案
     */
    private String generateFinalAnswer() {
        if (history.isEmpty()) {
            return "没有可用的辩论历史。";
        }

        if (moderator == null) {
            DebateRecord last = history.get(history.size() - 1);
            return String.format("辩论结束，未配置主持人。来自 %s 的最后观点：%s",
                    last.getAgentName(), last.getResponse());
        }

        log.info("生成最终答案...");

        StringBuilder sb = new StringBuilder();
        sb.append("基于以下多智能体辩论历史，请提供一个全面的最终答案或总结，综合关键观点及达成的任何共识。\n\n");
        sb.append("辩论历史：\n");
        for (DebateRecord record : history) {
            sb.append(String.format("[%d] %s: %s\n", record.getRound(), record.getAgentName(), record.getResponse()));
        }
        sb.append("\n最终全面回答：");

        return moderator.generate(sb.toString());
    }

    /**
     * 构建辩论结果
     */
    private DebateResult buildDebateResult(String question, String finalAnswer) {
        long duration = System.currentTimeMillis() - startTime;

        // 计算获胜者
        String winner = scores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        return DebateResult.builder()
                .success(true)
                .question(question)
                .finalAnswer(finalAnswer)
                .totalRounds(currentRound)
                .history(new ArrayList<>(history))
                .scores(new HashMap<>(scores))
                .winner(winner)
                .duration(duration)
                .consensusReached(status == DebateStatus.COMPLETED && currentRound < maxRounds)
                .viewpoints(new HashMap<>(viewpointTracker))
                .build();
    }

    // ============= 事件系统 =============

    /**
     * 添加事件监听器
     */
    public void addEventListener(DebateEventListener listener) {
        eventListeners.add(listener);
    }

    /**
     * 触发事件
     */
    private void fireEvent(DebateEventType type, Object data) {
        DebateEvent event = new DebateEvent(type, data, System.currentTimeMillis());
        eventListeners.forEach(listener -> {
            try {
                listener.onEvent(event);
            } catch (Exception e) {
                log.warn("事件监听器处理失败", e);
            }
        });
    }

    // ============= 统计信息 =============

    /**
     * 获取智能体得分排名
     */
    public List<Map.Entry<String, Double>> getScoreRanking() {
        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .collect(Collectors.toList());
    }

    /**
     * 获取观点变化跟踪
     */
    public Map<String, List<Viewpoint>> getViewpointTracker() {
        return new HashMap<>(viewpointTracker);
    }

    /**
     * 设置辩论模式
     */
    public void setMode(DebateMode mode) {
        this.mode = mode;
    }

    /**
     * 辩论智能体接口
     */
    public interface DebateAgent {
        String getName();

        String respond(String question, List<DebateRecord> history);
    }

    /**
     * 辩论记录
     */
    @Data
    public static class DebateRecord {
        private int round;
        private String agentName;
        private String response;
        private long timestamp;

        public DebateRecord(int round, String agentName, String response) {
            this.round = round;
            this.agentName = agentName;
            this.response = response;
            this.timestamp = System.currentTimeMillis();
        }
    }

    // ============= 配置和枣举类 =============

    /**
     * 辩论模式
     */
    public enum DebateMode {
        ROUND_ROBIN,   // 轮流模式
        ADVERSARIAL,   // 对抗模式(正反方)
        PANEL,         // 圆桌讨论
        SOCRATIC       // 苏格拉底式
    }

    /**
     * 辩论状态
     */
    public enum DebateStatus {
        NOT_STARTED,   // 未开始
        IN_PROGRESS,   // 进行中
        COMPLETED,     // 已完成
        ERROR          // 错误
    }

    /**
     * 辩论配置
     */
    @Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class DebateConfig {
        @lombok.Builder.Default
        private boolean enableScoring = true;
        @lombok.Builder.Default
        private boolean enableCrossExamination = false;
        @lombok.Builder.Default
        private long responseTimeoutMs = 0; // 0表示无限制
        @lombok.Builder.Default
        private int minResponseLength = 10;
        @lombok.Builder.Default
        private int maxResponseLength = 2000;
    }

    /**
     * 辩论结果
     */
    @Data
    @lombok.Builder
    public static class DebateResult {
        private boolean success;
        private String error;
        private String question;
        private String finalAnswer;
        private int totalRounds;
        private List<DebateRecord> history;
        private Map<String, Double> scores;
        private String winner;
        private long duration;
        private boolean consensusReached;
        private Map<String, List<Viewpoint>> viewpoints;
    }

    /**
     * 观点
     */
    @Data
    @lombok.Builder
    public static class Viewpoint {
        private int round;
        private String content;
        private long timestamp;
    }

    /**
     * 辩论事件类型
     */
    public enum DebateEventType {
        DEBATE_STARTED,
        DEBATE_ENDED,
        ROUND_STARTED,
        ROUND_ENDED,
        AGENT_RESPONDED,
        CONSENSUS_REACHED
    }

    /**
     * 辩论事件
     */
    @Data
    @lombok.AllArgsConstructor
    public static class DebateEvent {
        private DebateEventType type;
        private Object data;
        private long timestamp;
    }

    /**
     * 辩论事件监听器
     */
    public interface DebateEventListener {
        void onEvent(DebateEvent event);
    }
}
