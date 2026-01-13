package io.leavesfly.evox.frameworks.consensus.strategy;

import io.leavesfly.evox.frameworks.consensus.*;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 多数投票共识策略
 * 通过简单多数投票达成共识
 *
 * @param <T> 提议类型
 * @author EvoX Team
 */
@Slf4j
public class MajorityVotingStrategy<T> implements ConsensusStrategy<T> {

    private final ConsensusConfig config;

    public MajorityVotingStrategy(ConsensusConfig config) {
        this.config = config;
    }

    public MajorityVotingStrategy() {
        this(ConsensusConfig.builder().build());
    }

    @Override
    public ConsensusEvaluation<T> evaluate(List<T> proposals, List<ConsensusFramework.ConsensusAgent<T>> agents) {
        if (proposals == null || proposals.isEmpty()) {
            return buildNoConsensus();
        }

        // 统计每个提议的投票数
        Map<T, Long> voteCounts = proposals.stream()
            .collect(Collectors.groupingBy(p -> p, Collectors.counting()));

        // 找出得票最多的提议
        Map.Entry<T, Long> winner = voteCounts.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .orElse(null);

        if (winner == null) {
            return buildNoConsensus();
        }

        T consensusValue = winner.getKey();
        long voteCount = winner.getValue();
        int totalVotes = proposals.size();
        
        // 计算支持率
        double supportRate = (double) voteCount / totalVotes;
        
        // 计算置信度(支持率即置信度)
        double confidence = supportRate;
        
        // 检查是否达成共识
        boolean consensusReached = confidence >= config.getConsensusThreshold() 
            && supportRate >= config.getMinSupportRate();

        log.debug("Majority voting result: value={}, votes={}/{}, support={}, confidence={}, reached={}", 
            consensusValue, voteCount, totalVotes, supportRate, confidence, consensusReached);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("voteCounts", voteCounts);
        metadata.put("totalVotes", totalVotes);
        metadata.put("winnerVotes", voteCount);

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
        // 回退策略: 统计所有历史提议,选择出现次数最多的
        Map<T, Long> allVotes = history.stream()
            .flatMap(record -> record.getProposals().stream())
            .collect(Collectors.groupingBy(p -> p, Collectors.counting()));

        Map.Entry<T, Long> winner = allVotes.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .orElse(null);

        if (winner == null) {
            return buildNoConsensus();
        }

        long totalVotes = allVotes.values().stream().mapToLong(Long::longValue).sum();
        double supportRate = (double) winner.getValue() / totalVotes;
        double confidence = supportRate * 0.8; // 降低置信度,因为是回退策略

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("fallbackUsed", true);
        metadata.put("historicalVotes", allVotes);
        metadata.put("totalHistoricalVotes", totalVotes);

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
        return "MajorityVoting";
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
