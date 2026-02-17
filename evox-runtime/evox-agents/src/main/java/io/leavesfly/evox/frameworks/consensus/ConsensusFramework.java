package io.leavesfly.evox.frameworks.consensus;

import io.leavesfly.evox.frameworks.base.MultiAgentFramework;
import io.leavesfly.evox.workflow.base.Workflow;
import io.leavesfly.evox.workflow.base.WorkflowNode;
import io.leavesfly.evox.workflow.graph.WorkflowGraph;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 共识框架
 * 允许多个智能体通过不同的共识策略达成一致决策
 *
 * <p>基于 Workflow DAG 引擎实现，将多轮共识过程抽象为工作流执行
 *
 * @author EvoX Team
 */
@Slf4j
@Data
public class ConsensusFramework<T> extends MultiAgentFramework {

    /**
     * 参与共识的智能体列表
     */
    private List<ConsensusAgent<T>> agents;

    /**
     * 共识策略
     */
    private ConsensusStrategy<T> strategy;

    /**
     * 共识配置
     */
    private ConsensusConfig config;

    /**
     * 共识结果缓存（通过回调从 NodeHandler 获取）
     */
    private ConsensusResult<T> cachedResult;

    public ConsensusFramework(List<ConsensusAgent<T>> agents, ConsensusStrategy<T> strategy) {
        this(agents, strategy, ConsensusConfig.builder().build());
    }

    public ConsensusFramework(List<ConsensusAgent<T>> agents, ConsensusStrategy<T> strategy, ConsensusConfig config) {
        this.agents = agents;
        this.strategy = strategy;
        this.config = config;
        this.frameworkName = "ConsensusFramework";
    }

    /**
     * 执行共识过程
     *
     * @param question 需要达成共识的问题
     * @return 共识结果
     */
    public ConsensusResult<T> reachConsensus(String question) {
        log.info("Starting consensus process for question: {}", question);
        
        // 重置缓存
        this.cachedResult = null;
        
        // 准备输入参数
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("question", question);
        inputs.put("start_time", System.currentTimeMillis());
        inputs.put("consensus_reached", false);
        
        // 执行工作流
        executeWorkflow(question, inputs);
        
        // 从回调缓存中获取结果
        if (cachedResult != null) {
            log.info("Consensus process completed. Reached: {}, Confidence: {}", 
                cachedResult.isReached(), cachedResult.getConfidence());
            return cachedResult;
        }
        
        log.error("Consensus result not found after workflow execution");
        throw new ConsensusException("Failed to get consensus result from workflow");
    }

    /**
     * 构建工作流 DAG 图
     * 使用 LOOP + COLLECT 多节点结构，循环控制由 Workflow 引擎承担
     *
     * DAG 结构：
     *   LOOP(consensus_loop, maxRounds, "consensus_reached == false")
     *     └── COLLECT(consensus_round)  ← 单轮原子操作
     *   COLLECT(consensus_fallback)     ← 回退处理
     *
     * @param task 任务描述
     * @return 工作流图
     */
    @Override
    protected WorkflowGraph buildWorkflowGraph(String task) {
        log.info("Building consensus workflow graph for task: {}", task);

        WorkflowGraph graph = new WorkflowGraph(task);

        // 1. 创建单轮共识节点（LOOP 的循环体）
        WorkflowNode roundNode = createCollectNode("consensus_round", "consensus_round", null);
        graph.addNode(roundNode);

        // 2. 创建 LOOP 节点，循环体指向 roundNode
        WorkflowNode loopNode = createLoopNode(
            "consensus_loop",
            config.getMaxRounds(),
            "consensus_reached == false"
        );
        loopNode.setLoopBodyNodeId(roundNode.getNodeId());
        graph.addNode(loopNode);

        // 3. 创建回退节点（LOOP 结束后执行）
        WorkflowNode fallbackNode = createCollectNode("consensus_fallback", "consensus_fallback", null);
        graph.addNode(fallbackNode);

        // 4. 连接边：LOOP → fallback
        graph.addEdge(loopNode.getNodeId(), fallbackNode.getNodeId());

        log.info("Consensus workflow graph built with {} nodes", graph.getNodeCount());
        return graph;
    }

    /**
     * 注册节点处理器
     *
     * @param workflow 工作流实例
     */
    @Override
    protected void registerNodeHandlers(Workflow workflow) {
        // 注册单轮共识处理器
        ConsensusNodeHandler.ConsensusRoundHandler<T> roundHandler =
            new ConsensusNodeHandler.ConsensusRoundHandler<>(agents, strategy, config);
        roundHandler.setResultCallback(result -> this.cachedResult = result);
        workflow.registerHandler("consensus_round", roundHandler);

        // 注册回退处理器
        ConsensusNodeHandler.ConsensusFallbackHandler<T> fallbackHandler =
            new ConsensusNodeHandler.ConsensusFallbackHandler<>(agents, strategy, config);
        fallbackHandler.setResultCallback(result -> this.cachedResult = result);
        workflow.registerHandler("consensus_fallback", fallbackHandler);
    }

    /**
     * 共识智能体接口
     */
    public interface ConsensusAgent<T> {
        /**
         * 获取智能体名称
         */
        String getName();
        
        /**
         * 提出提议
         *
         * @param question 问题
         * @param history 历史记录
         * @return 提议
         */
        T propose(String question, List<ConsensusRecord<T>> history);
        
        /**
         * 接收本轮评估结果的通知
         *
         * @param round 轮次
         * @param evaluation 评估结果
         */
        default void onEvaluation(int round, ConsensusEvaluation<T> evaluation) {
            // 默认不处理
        }
        
        /**
         * 获取智能体权重(用于加权共识策略)
         */
        default double getWeight() {
            return 1.0;
        }
    }

    /**
     * 共识异常
     */
    public static class ConsensusException extends RuntimeException {
        public ConsensusException(String message) {
            super(message);
        }
        
        public ConsensusException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
