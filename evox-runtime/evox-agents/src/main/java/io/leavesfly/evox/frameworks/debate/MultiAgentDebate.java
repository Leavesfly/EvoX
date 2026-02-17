package io.leavesfly.evox.frameworks.debate;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.leavesfly.evox.frameworks.base.MultiAgentFramework;
import io.leavesfly.evox.models.base.LLMProvider;
import io.leavesfly.evox.workflow.base.Workflow;
import io.leavesfly.evox.workflow.base.WorkflowNode;
import io.leavesfly.evox.workflow.graph.WorkflowGraph;
import io.leavesfly.evox.workflow.node.NodeHandler;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 多智能体辩论框架
 * 允许多个智能体通过辩论达成共识
 *
 * <p>基于 evox-workflow DAG 引擎实现，通过 DebateNodeHandler 封装辩论核心逻辑。
 *
 * @author EvoX Team
 */
@Slf4j
@Data
public class MultiAgentDebate extends MultiAgentFramework {

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
        this.frameworkName = "MultiAgentDebate";

        // 初始化每个智能体的分数
        agents.forEach(agent -> scores.put(agent.getName(), 0.0));
    }

    public MultiAgentDebate(List<DebateAgent> agents, int maxRounds, LLMProvider moderator, DebateConfig config) {
        this(agents, maxRounds, moderator);
        this.config = config != null ? config : DebateConfig.builder().build();
    }

    /**
     * 构建工作流 DAG 图
     * 辩论框架使用多节点 DAG：LOOP + COLLECT(debate_round) + COLLECT(debate_final)
     *
     * @param task 任务描述（辩论问题）
     * @return 工作流图
     */
    @Override
    protected WorkflowGraph buildWorkflowGraph(String task) {
        WorkflowGraph graph = new WorkflowGraph(task);

        // 创建 LOOP 节点
        WorkflowNode loopNode = createLoopNode(
                "debate_loop",
                maxRounds,
                "debate_finished == false"
        );

        // 创建 COLLECT 节点作为循环体（单轮辩论）
        Map<String, Object> roundHandlerConfig = new HashMap<>();
        roundHandlerConfig.put("mode", mode);
        WorkflowNode roundNode = createCollectNode(
                "debate_round",
                "debate_round_handler",
                roundHandlerConfig
        );

        // 创建 COLLECT 节点用于生成最终答案
        Map<String, Object> finalHandlerConfig = new HashMap<>();
        WorkflowNode finalNode = createCollectNode(
                "debate_final",
                "debate_final_handler",
                finalHandlerConfig
        );

        // 设置循环体节点
        loopNode.setLoopBodyNodeId(roundNode.getNodeId());

        // 添加节点到图
        graph.addNode(roundNode);
        graph.addNode(loopNode);
        graph.addNode(finalNode);

        // 添加边：LOOP → final
        graph.addEdge(loopNode.getNodeId(), finalNode.getNodeId());

        return graph;
    }

    /**
     * 注册节点处理器
     *
     * @param workflow 工作流实例
     */
    @Override
    protected void registerNodeHandlers(Workflow workflow) {
        // 注册单轮辩论处理器
        DebateNodeHandler.DebateRoundHandler roundHandler = new DebateNodeHandler.DebateRoundHandler();
        workflow.registerHandler("debate_round_handler", roundHandler);

        // 注册最终答案处理器
        DebateNodeHandler.DebateFinalHandler finalHandler = new DebateNodeHandler.DebateFinalHandler();
        workflow.registerHandler("debate_final_handler", finalHandler);
    }

    /**
     * 执行前的初始化钩子
     *
     * @param task 任务描述
     */
    @Override
    protected void beforeExecute(String task) {
        startTime = System.currentTimeMillis();
        status = DebateStatus.IN_PROGRESS;
        fireEvent(DebateEventType.DEBATE_STARTED, task);
    }

    /**
     * 开始辩论
     */
    public DebateResult debate(String question) {
        log.info("开始辩论: {}", question);

        try {
            // 准备工作流输入参数
            Map<String, Object> inputs = new HashMap<>();
            inputs.put("debate_agents", agents);
            inputs.put("debate_moderator", moderator);
            inputs.put("debate_mode", mode);
            inputs.put("debate_config", config);
            inputs.put("debate_max_rounds", maxRounds);
            inputs.put("debate_question", question);
            inputs.put("debate_event_listeners", eventListeners);
            
            // 初始化控制变量
            inputs.put("debate_finished", false);
            inputs.put("start_time", System.currentTimeMillis());
            inputs.put("current_round", 0);
            inputs.put("history", new ArrayList<>());
            inputs.put("viewpoint_tracker", new HashMap<>());
            inputs.put("scores", new HashMap<>());

            // 执行工作流
            String workflowResult = executeWorkflow(question, inputs);

            // DebateNodeHandler 将结果以 JSON 格式返回，包含 all debate state
            // 解析结果并更新本地状态
            try {
                // 尝试解析 JSON 结果
                ObjectMapper  objectMapper = new ObjectMapper();
                @SuppressWarnings("unchecked")
                Map<String, Object> resultMap = objectMapper.readValue(workflowResult, Map.class);

                // 更新本地状态
                if (resultMap.containsKey("history")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> historyData = (List<Map<String, Object>>) resultMap.get("history");
                    this.history = new ArrayList<>();
                    for (Map<String, Object> recordData : historyData) {
                        int round = ((Number) recordData.get("round")).intValue();
                        String agentName = (String) recordData.get("agentName");
                        String response = (String) recordData.get("response");
                        this.history.add(new DebateRecord(round, agentName, response));
                    }
                }

                if (resultMap.containsKey("currentRound")) {
                    this.currentRound = ((Number) resultMap.get("currentRound")).intValue();
                }

                if (resultMap.containsKey("viewpoints")) {
                    @SuppressWarnings("unchecked")
                    Map<String, List<Map<String, Object>>> viewpointsData = (Map<String, List<Map<String, Object>>>) resultMap.get("viewpoints");
                    this.viewpointTracker.clear();
                    for (Map.Entry<String, List<Map<String, Object>>> entry : viewpointsData.entrySet()) {
                        List<Viewpoint> viewpoints = new ArrayList<>();
                        for (Map<String, Object> vpData : entry.getValue()) {
                            int round = ((Number) vpData.get("round")).intValue();
                            String content = (String) vpData.get("content");
                            viewpoints.add(Viewpoint.builder().round(round).content(content).build());
                        }
                        this.viewpointTracker.put(entry.getKey(), viewpoints);
                    }
                }

                if (resultMap.containsKey("scores")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Double> scoresData = (Map<String, Double>) resultMap.get("scores");
                    this.scores.clear();
                    this.scores.putAll(scoresData);
                }

                String finalAnswer = (String) resultMap.getOrDefault("finalAnswer", workflowResult);

                status = DebateStatus.COMPLETED;
                fireEvent(DebateEventType.DEBATE_ENDED, finalAnswer);

                return buildDebateResult(question, finalAnswer);

            } catch (Exception e) {
                // 如果解析失败，使用原始结果
                log.warn("解析辩论结果失败，使用原始结果: {}", e.getMessage());
                status = DebateStatus.COMPLETED;
                fireEvent(DebateEventType.DEBATE_ENDED, workflowResult);
                return buildDebateResult(question, workflowResult);
            }

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

    // ============= 配置和枚举类 =============

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
