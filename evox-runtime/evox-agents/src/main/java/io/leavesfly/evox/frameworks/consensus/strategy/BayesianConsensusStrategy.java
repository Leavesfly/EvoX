package io.leavesfly.evox.frameworks.consensus.strategy;

import io.leavesfly.evox.frameworks.consensus.*;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 贝叶斯共识策略
 * 基于贝叶斯推理进行共识评估,考虑历史表现和先验概率
 *
 * @param <T> 提议类型
 * @author EvoX Team
 */
@Slf4j
public class BayesianConsensusStrategy<T> implements ConsensusStrategy<T> {

    private final ConsensusConfig config;
    
    /**
     * 智能体历史准确率
     */
    private final Map<String, Double> agentAccuracy;
    
    /**
     * 先验概率(默认均匀分布)
     */
    private final Map<T, Double> priorProbability;

    public BayesianConsensusStrategy(ConsensusConfig config) {
        this.config = config;
        this.agentAccuracy = new HashMap<>();
        this.priorProbability = new HashMap<>();
    }

    public BayesianConsensusStrategy() {
        this(ConsensusConfig.builder().build());
    }

    /**
     * 设置智能体的历史准确率
     */
    public void setAgentAccuracy(String agentName, double accuracy) {
        if (accuracy < 0.0 || accuracy > 1.0) {
            throw new IllegalArgumentException("Accuracy must be between 0.0 and 1.0");
        }
        agentAccuracy.put(agentName, accuracy);
    }

    /**
     * 设置先验概率
     */
    public void setPriorProbability(T value, double probability) {
        if (probability < 0.0 || probability > 1.0) {
            throw new IllegalArgumentException("Probability must be between 0.0 and 1.0");
        }
        priorProbability.put(value, probability);
    }

    @Override
    public ConsensusEvaluation<T> evaluate(List<T> proposals, List<ConsensusFramework.ConsensusAgent<T>> agents) {
        if (proposals == null || proposals.isEmpty() || agents == null || agents.isEmpty()) {
            return buildNoConsensus();
        }

        if (proposals.size() != agents.size()) {
            log.warn("Proposals size {} != agents size {}", proposals.size(), agents.size());
            return buildNoConsensus();
        }

        // 计算后验概率
        Map<T, Double> posteriorProbabilities = calculatePosteriorProbabilities(proposals, agents);

        // 找出后验概率最高的提议
        Map.Entry<T, Double> winner = posteriorProbabilities.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .orElse(null);

        if (winner == null) {
            return buildNoConsensus();
        }

        T consensusValue = winner.getKey();
        double posteriorProb = winner.getValue();
        
        // 计算支持率
        long supportCount = proposals.stream().filter(p -> p.equals(consensusValue)).count();
        double supportRate = (double) supportCount / proposals.size();
        
        // 置信度为后验概率
        double confidence = posteriorProb;
        
        // 检查是否达成共识
        boolean consensusReached = confidence >= config.getConsensusThreshold() 
            && supportRate >= config.getMinSupportRate();

        log.debug("Bayesian consensus result: value={}, posterior={}, support={}, confidence={}, reached={}", 
            consensusValue, posteriorProb, supportRate, confidence, consensusReached);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("posteriorProbabilities", posteriorProbabilities);
        metadata.put("agentAccuracies", new HashMap<>(agentAccuracy));
        metadata.put("priorProbabilities", new HashMap<>(priorProbability));

        return ConsensusEvaluation.<T>builder()
            .consensusReached(consensusReached)
            .consensusValue(consensusValue)
            .confidence(confidence)
            .supportRate(supportRate)
            .metadata(metadata)
            .build();
    }

    /**
     * 计算后验概率: P(H|E) = P(E|H) * P(H) / P(E)
     */
    private Map<T, Double> calculatePosteriorProbabilities(List<T> proposals, 
                                                           List<ConsensusFramework.ConsensusAgent<T>> agents) {
        // 收集所有唯一的提议
        Set<T> uniqueProposals = new HashSet<>(proposals);
        
        // 计算先验概率(如果没有设置,使用均匀分布)
        Map<T, Double> priors = new HashMap<>();
        for (T proposal : uniqueProposals) {
            priors.put(proposal, priorProbability.getOrDefault(proposal, 1.0 / uniqueProposals.size()));
        }
        
        // 计算似然度 P(E|H)
        Map<T, Double> likelihoods = new HashMap<>();
        for (T hypothesis : uniqueProposals) {
            double likelihood = 1.0;
            for (int i = 0; i < proposals.size(); i++) {
                T proposal = proposals.get(i);
                String agentName = agents.get(i).getName();
                double accuracy = agentAccuracy.getOrDefault(agentName, 0.7); // 默认70%准确率
                
                // 如果智能体支持该假设,乘以准确率;否则乘以错误率
                if (proposal.equals(hypothesis)) {
                    likelihood *= accuracy;
                } else {
                    likelihood *= (1.0 - accuracy) / (uniqueProposals.size() - 1);
                }
            }
            likelihoods.put(hypothesis, likelihood);
        }
        
        // 计算证据概率 P(E) = sum(P(E|H_i) * P(H_i))
        double evidence = 0.0;
        for (T hypothesis : uniqueProposals) {
            evidence += likelihoods.get(hypothesis) * priors.get(hypothesis);
        }
        
        // 计算后验概率 P(H|E)
        Map<T, Double> posteriors = new HashMap<>();
        for (T hypothesis : uniqueProposals) {
            double posterior = (likelihoods.get(hypothesis) * priors.get(hypothesis)) / evidence;
            posteriors.put(hypothesis, posterior);
        }
        
        return posteriors;
    }

    @Override
    public ConsensusEvaluation<T> fallback(List<ConsensusRecord<T>> history, 
                                           List<ConsensusFramework.ConsensusAgent<T>> agents) {
        // 回退策略: 综合所有历史记录进行贝叶斯推理
        List<T> allProposals = new ArrayList<>();
        List<ConsensusFramework.ConsensusAgent<T>> allAgents = new ArrayList<>();
        
        for (ConsensusRecord<T> record : history) {
            allProposals.addAll(record.getProposals());
            // 重复添加agents以匹配proposals数量
            for (int i = 0; i < record.getProposals().size(); i++) {
                if (i < agents.size()) {
                    allAgents.add(agents.get(i));
                }
            }
        }
        
        if (allProposals.isEmpty()) {
            return buildNoConsensus();
        }
        
        Map<T, Double> posteriors = calculatePosteriorProbabilities(allProposals, allAgents);
        
        Map.Entry<T, Double> winner = posteriors.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .orElse(null);

        if (winner == null) {
            return buildNoConsensus();
        }

        long supportCount = allProposals.stream().filter(p -> p.equals(winner.getKey())).count();
        double supportRate = (double) supportCount / allProposals.size();
        double confidence = winner.getValue() * 0.8; // 降低置信度

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("fallbackUsed", true);
        metadata.put("historicalPosteriors", posteriors);
        metadata.put("totalHistoricalProposals", allProposals.size());

        return ConsensusEvaluation.<T>builder()
            .consensusReached(false)
            .consensusValue(winner.getKey())
            .confidence(confidence)
            .supportRate(supportRate)
            .metadata(metadata)
            .build();
    }

    @Override
    public String getStrategyName() {
        return "BayesianConsensus";
    }

    private ConsensusEvaluation<T> buildNoConsensus() {
        return ConsensusEvaluation.<T>builder()
            .consensusReached(false)
            .consensusValue(null)
            .confidence(0.0)
            .supportRate(0.0)
            .build();
    }
}
