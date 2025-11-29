package io.leavesfly.evox.frameworks.consensus;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 共识框架
 * 允许多个智能体通过不同的共识策略达成一致决策
 *
 * @author EvoX Team
 */
@Slf4j
@Data
public class ConsensusFramework<T> {

    /**
     * 参与共识的智能体列表
     */
    private List<ConsensusAgent<T>> agents;

    /**
     * 共识策略
     */
    private ConsensusStrategy<T> strategy;

    /**
     * 最大迭代轮数
     */
    private int maxRounds;

    /**
     * 当前迭代轮数
     */
    private int currentRound;

    /**
     * 共识历史记录
     */
    private List<ConsensusRecord<T>> history;

    /**
     * 共识配置
     */
    private ConsensusConfig config;

    public ConsensusFramework(List<ConsensusAgent<T>> agents, ConsensusStrategy<T> strategy) {
        this(agents, strategy, ConsensusConfig.builder().build());
    }

    public ConsensusFramework(List<ConsensusAgent<T>> agents, ConsensusStrategy<T> strategy, ConsensusConfig config) {
        this.agents = agents;
        this.strategy = strategy;
        this.config = config;
        this.maxRounds = config.getMaxRounds();
        this.currentRound = 0;
        this.history = new ArrayList<>();
    }

    /**
     * 执行共识过程
     *
     * @param question 需要达成共识的问题
     * @return 共识结果
     */
    public ConsensusResult<T> reachConsensus(String question) {
        log.info("Starting consensus process for question: {}", question);
        
        long startTime = System.currentTimeMillis();
        
        for (currentRound = 1; currentRound <= maxRounds; currentRound++) {
            log.info("Consensus round {}/{}", currentRound, maxRounds);
            
            // 收集所有智能体的提议
            List<T> proposals = collectProposals(question);
            
            // 记录本轮提议
            ConsensusRecord<T> record = new ConsensusRecord<>(currentRound, proposals, System.currentTimeMillis());
            history.add(record);
            
            // 使用策略评估共识
            ConsensusEvaluation<T> evaluation = strategy.evaluate(proposals, agents);
            record.setEvaluation(evaluation);
            
            log.debug("Round {} evaluation: consensus={}, confidence={}", 
                currentRound, evaluation.isConsensusReached(), evaluation.getConfidence());
            
            // 检查是否达成共识
            if (evaluation.isConsensusReached()) {
                log.info("Consensus reached in round {} with confidence {}", 
                    currentRound, evaluation.getConfidence());
                
                return ConsensusResult.<T>builder()
                    .reached(true)
                    .result(evaluation.getConsensusValue())
                    .confidence(evaluation.getConfidence())
                    .rounds(currentRound)
                    .duration(System.currentTimeMillis() - startTime)
                    .history(new ArrayList<>(history))
                    .metadata(evaluation.getMetadata())
                    .build();
            }
            
            // 通知智能体本轮结果,允许调整
            notifyAgents(evaluation);
            
            // 检查是否需要早停
            if (shouldEarlyStop(evaluation)) {
                log.info("Early stopping triggered in round {}", currentRound);
                break;
            }
        }
        
        // 未达成共识,返回最佳结果
        log.warn("Consensus not reached after {} rounds", maxRounds);
        ConsensusEvaluation<T> finalEvaluation = strategy.fallback(history, agents);
        
        return ConsensusResult.<T>builder()
            .reached(false)
            .result(finalEvaluation.getConsensusValue())
            .confidence(finalEvaluation.getConfidence())
            .rounds(currentRound - 1)
            .duration(System.currentTimeMillis() - startTime)
            .history(new ArrayList<>(history))
            .metadata(finalEvaluation.getMetadata())
            .build();
    }

    /**
     * 收集所有智能体的提议
     */
    private List<T> collectProposals(String question) {
        List<T> proposals = new ArrayList<>();
        for (ConsensusAgent<T> agent : agents) {
            try {
                T proposal = agent.propose(question, history);
                proposals.add(proposal);
                log.debug("Agent {} proposed: {}", agent.getName(), proposal);
            } catch (Exception e) {
                log.error("Agent {} failed to propose: {}", agent.getName(), e.getMessage(), e);
                if (!config.isIgnoreFailedProposals()) {
                    throw new ConsensusException("Failed to collect proposal from agent: " + agent.getName(), e);
                }
            }
        }
        return proposals;
    }

    /**
     * 通知智能体本轮评估结果
     */
    private void notifyAgents(ConsensusEvaluation<T> evaluation) {
        if (!config.isEnableAgentFeedback()) {
            return;
        }
        
        for (ConsensusAgent<T> agent : agents) {
            try {
                agent.onEvaluation(currentRound, evaluation);
            } catch (Exception e) {
                log.warn("Failed to notify agent {}: {}", agent.getName(), e.getMessage());
            }
        }
    }

    /**
     * 判断是否应该早停
     */
    private boolean shouldEarlyStop(ConsensusEvaluation<T> evaluation) {
        if (!config.isEnableEarlyStopping()) {
            return false;
        }
        
        // 如果连续多轮置信度没有提升,则早停
        if (history.size() >= config.getEarlyStoppingPatience()) {
            double currentConfidence = evaluation.getConfidence();
            double previousConfidence = history.get(history.size() - config.getEarlyStoppingPatience())
                .getEvaluation().getConfidence();
            
            if (currentConfidence <= previousConfidence + config.getEarlyStoppingThreshold()) {
                return true;
            }
        }
        
        return false;
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
