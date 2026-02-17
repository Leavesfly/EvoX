package io.leavesfly.evox.frameworks.consensus;

import io.leavesfly.evox.workflow.base.WorkflowNode;
import io.leavesfly.evox.workflow.execution.WorkflowContext;
import io.leavesfly.evox.workflow.node.NodeHandler;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * 共识框架的节点处理器
 * 每个 Handler 只负责单步原子操作，循环控制由 Workflow LOOP 节点承担
 *
 * @author EvoX Team
 */
@Slf4j
public class ConsensusNodeHandler {

    /**
     * 单轮共识处理器（LOOP 循环体）
     * 执行一轮完整的共识流程：收集提议 → 评估 → 通知
     * 将 consensus_reached 写入 WorkflowContext，由 LOOP 节点判断是否继续
     */
    public static class ConsensusRoundHandler<T> implements NodeHandler {

        private final List<ConsensusFramework.ConsensusAgent<T>> agents;
        private final ConsensusStrategy<T> strategy;
        private final ConsensusConfig config;
        private java.util.function.Consumer<ConsensusResult<T>> resultCallback;

        public ConsensusRoundHandler(List<ConsensusFramework.ConsensusAgent<T>> agents,
                                     ConsensusStrategy<T> strategy,
                                     ConsensusConfig config) {
            this.agents = agents;
            this.strategy = strategy;
            this.config = config;
        }

        public void setResultCallback(java.util.function.Consumer<ConsensusResult<T>> callback) {
            this.resultCallback = callback;
        }

        @Override
        public String getHandlerName() {
            return "consensus_round";
        }

        @Override
        public Mono<Object> handle(WorkflowContext context, WorkflowNode node) {
            // 递增轮次
            Integer currentRound = (Integer) context.getExecutionData("current_round");
            if (currentRound == null) {
                currentRound = 0;
            }
            currentRound++;
            context.updateExecutionData("current_round", currentRound);

            String question = (String) context.getExecutionData("question");
            @SuppressWarnings("unchecked")
            List<ConsensusRecord<T>> history = (List<ConsensusRecord<T>>) context.getExecutionData("history");
            if (history == null) {
                history = new ArrayList<>();
                context.updateExecutionData("history", history);
            }

            log.info("Consensus round {}/{}", currentRound, config.getMaxRounds());

            // 1. 收集提议
            List<T> proposals = new ArrayList<>();
            for (ConsensusFramework.ConsensusAgent<T> agent : agents) {
                try {
                    proposals.add(agent.propose(question, history));
                } catch (Exception e) {
                    log.error("Agent {} failed to propose: {}", agent.getName(), e.getMessage(), e);
                    if (!config.isIgnoreFailedProposals()) {
                        return Mono.error(new ConsensusFramework.ConsensusException(
                            "Failed to collect proposal from agent: " + agent.getName(), e));
                    }
                }
            }

            // 2. 评估共识
            ConsensusEvaluation<T> evaluation = strategy.evaluate(proposals, agents);
            ConsensusRecord<T> record = new ConsensusRecord<>(currentRound, proposals, System.currentTimeMillis());
            record.setEvaluation(evaluation);
            history.add(record);

            // 3. 写入循环控制变量，LOOP 节点通过 loopCondition 读取
            context.updateExecutionData("consensus_reached", evaluation.isConsensusReached());
            context.updateExecutionData("evaluation", evaluation);

            log.info("Round {} result: consensus={}, confidence={}",
                currentRound, evaluation.isConsensusReached(), evaluation.getConfidence());

            // 4. 如果达成共识，构建结果并通过回调传递
            if (evaluation.isConsensusReached()) {
                long startTime = context.getExecutionData("start_time") != null
                    ? (Long) context.getExecutionData("start_time") : System.currentTimeMillis();
                ConsensusResult<T> result = ConsensusResult.<T>builder()
                    .reached(true)
                    .result(evaluation.getConsensusValue())
                    .confidence(evaluation.getConfidence())
                    .rounds(currentRound)
                    .duration(System.currentTimeMillis() - startTime)
                    .history(new ArrayList<>(history))
                    .metadata(evaluation.getMetadata())
                    .build();
                if (resultCallback != null) {
                    resultCallback.accept(result);
                }
            }

            // 5. 通知智能体（如果启用反馈）
            if (config.isEnableAgentFeedback()) {
                for (ConsensusFramework.ConsensusAgent<T> agent : agents) {
                    try {
                        agent.onEvaluation(currentRound, evaluation);
                    } catch (Exception e) {
                        log.warn("Failed to notify agent {}: {}", agent.getName(), e.getMessage());
                    }
                }
            }

            // 6. 早停检查，写入控制变量让 LOOP 节点退出
            if (config.isEnableEarlyStopping() && history.size() >= config.getEarlyStoppingPatience()) {
                double prevConfidence = history.get(history.size() - config.getEarlyStoppingPatience())
                    .getEvaluation().getConfidence();
                if (evaluation.getConfidence() <= prevConfidence + config.getEarlyStoppingThreshold()) {
                    log.info("Early stopping triggered in round {}", currentRound);
                    context.updateExecutionData("consensus_reached", true);
                }
            }

            return Mono.just("round_" + currentRound + "_completed");
        }
    }

    /**
     * 回退处理器（LOOP 结束后执行）
     * 当 LOOP 结束但未达成共识时，使用回退策略生成最终结果
     */
    public static class ConsensusFallbackHandler<T> implements NodeHandler {

        private final List<ConsensusFramework.ConsensusAgent<T>> agents;
        private final ConsensusStrategy<T> strategy;
        private final ConsensusConfig config;
        private java.util.function.Consumer<ConsensusResult<T>> resultCallback;

        public ConsensusFallbackHandler(List<ConsensusFramework.ConsensusAgent<T>> agents,
                                        ConsensusStrategy<T> strategy,
                                        ConsensusConfig config) {
            this.agents = agents;
            this.strategy = strategy;
            this.config = config;
        }

        public void setResultCallback(java.util.function.Consumer<ConsensusResult<T>> callback) {
            this.resultCallback = callback;
        }

        @Override
        public String getHandlerName() {
            return "consensus_fallback";
        }

        @Override
        public Mono<Object> handle(WorkflowContext context, WorkflowNode node) {
            Boolean reached = (Boolean) context.getExecutionData("consensus_reached");
            if (Boolean.TRUE.equals(reached)) {
                log.info("Consensus already reached, skipping fallback");
                return Mono.just("fallback_skipped");
            }

            @SuppressWarnings("unchecked")
            List<ConsensusRecord<T>> history = (List<ConsensusRecord<T>>) context.getExecutionData("history");
            Integer totalRounds = (Integer) context.getExecutionData("current_round");

            log.warn("Consensus not reached after {} rounds, applying fallback strategy", totalRounds);

            ConsensusEvaluation<T> finalEvaluation = strategy.fallback(history, agents);
            long startTime = context.getExecutionData("start_time") != null
                ? (Long) context.getExecutionData("start_time") : System.currentTimeMillis();

            ConsensusResult<T> result = ConsensusResult.<T>builder()
                .reached(false)
                .result(finalEvaluation.getConsensusValue())
                .confidence(finalEvaluation.getConfidence())
                .rounds(totalRounds != null ? totalRounds : 0)
                .duration(System.currentTimeMillis() - startTime)
                .history(history != null ? new ArrayList<>(history) : new ArrayList<>())
                .metadata(finalEvaluation.getMetadata())
                .build();

            if (resultCallback != null) {
                resultCallback.accept(result);
            }
            return Mono.just("fallback_completed");
        }
    }
}
