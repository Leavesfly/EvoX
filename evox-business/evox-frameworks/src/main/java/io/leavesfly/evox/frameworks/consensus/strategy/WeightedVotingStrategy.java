package io.leavesfly.evox.frameworks.consensus.strategy;

import io.leavesfly.evox.frameworks.consensus.*;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 加权投票共识策略
 * 根据智能体权重进行加权投票
 *
 * @param <T> 提议类型
 * @author EvoX Team
 */
@Slf4j
public class WeightedVotingStrategy<T> implements ConsensusStrategy<T> {

    private final ConsensusConfig config;

    public WeightedVotingStrategy(ConsensusConfig config) {
        this.config = config;
    }

    public WeightedVotingStrategy() {
        this(ConsensusConfig.builder().build());
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

        // 计算每个提议的加权得分
        Map<T, Double> weightedScores = new HashMap<>();
        double totalWeight = 0.0;

        for (int i = 0; i < proposals.size(); i++) {
            T proposal = proposals.get(i);
            double weight = agents.get(i).getWeight();
            totalWeight += weight;
            weightedScores.merge(proposal, weight, Double::sum);
        }

        // 找出加权得分最高的提议
        Map.Entry<T, Double> winner = weightedScores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .orElse(null);

        if (winner == null) {
            return buildNoConsensus();
        }

        T consensusValue = winner.getKey();
        double winnerScore = winner.getValue();
        
        // 计算支持率(加权)
        double supportRate = winnerScore / totalWeight;
        
        // 计算置信度
        double confidence = supportRate;
        
        // 检查是否达成共识
        boolean consensusReached = confidence >= config.getConsensusThreshold() 
            && supportRate >= config.getMinSupportRate();

        log.debug("Weighted voting result: value={}, score={}/{}, support={}, confidence={}, reached={}", 
            consensusValue, winnerScore, totalWeight, supportRate, confidence, consensusReached);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("weightedScores", weightedScores);
        metadata.put("totalWeight", totalWeight);
        metadata.put("winnerScore", winnerScore);

        return ConsensusEvaluation.<T>builder()
            .consensusReached(consensusReached)
            .consensusValue(consensusValue)
            .confidence(confidence)
            .supportRate(supportRate)
            .metadata(metadata)
            .build();
    }

    @Override
    public ConsensusEvaluation<T> fallback(List<ConsensusRecord<T>> history, 
                                           List<ConsensusFramework.ConsensusAgent<T>> agents) {
        // 回退策略: 统计历史加权得分
        Map<T, Double> historicalScores = new HashMap<>();
        double totalWeight = 0.0;

        for (ConsensusRecord<T> record : history) {
            List<T> proposals = record.getProposals();
            for (int i = 0; i < proposals.size() && i < agents.size(); i++) {
                T proposal = proposals.get(i);
                double weight = agents.get(i).getWeight();
                totalWeight += weight;
                historicalScores.merge(proposal, weight, Double::sum);
            }
        }

        Map.Entry<T, Double> winner = historicalScores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .orElse(null);

        if (winner == null) {
            return buildNoConsensus();
        }

        double supportRate = winner.getValue() / totalWeight;
        double confidence = supportRate * 0.8; // 降低置信度,因为是回退策略

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("fallbackUsed", true);
        metadata.put("historicalScores", historicalScores);
        metadata.put("totalHistoricalWeight", totalWeight);

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
        return "WeightedVoting";
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
