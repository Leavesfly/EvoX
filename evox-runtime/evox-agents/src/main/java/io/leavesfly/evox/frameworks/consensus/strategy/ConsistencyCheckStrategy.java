package io.leavesfly.evox.frameworks.consensus.strategy;

import io.leavesfly.evox.frameworks.consensus.*;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * 一致性检查共识策略
 * 基于提议之间的相似度/一致性进行共识判断
 *
 * @param <T> 提议类型
 * @author EvoX Team
 */
@Slf4j
public class ConsistencyCheckStrategy<T> implements ConsensusStrategy<T> {

    private final ConsensusConfig config;
    
    /**
     * 相似度计算函数
     */
    private final BiFunction<T, T, Double> similarityFunction;
    
    /**
     * 相似度阈值
     */
    private final double similarityThreshold;

    /**
     * 构造函数
     *
     * @param config 配置
     * @param similarityFunction 相似度计算函数,返回0.0-1.0之间的值
     * @param similarityThreshold 相似度阈值
     */
    public ConsistencyCheckStrategy(ConsensusConfig config, 
                                   BiFunction<T, T, Double> similarityFunction,
                                   double similarityThreshold) {
        this.config = config;
        this.similarityFunction = similarityFunction;
        this.similarityThreshold = similarityThreshold;
    }

    /**
     * 使用默认相似度函数(equals判断)
     */
    public ConsistencyCheckStrategy(ConsensusConfig config) {
        this(config, (a, b) -> a.equals(b) ? 1.0 : 0.0, 0.9);
    }

    public ConsistencyCheckStrategy() {
        this(ConsensusConfig.builder().build());
    }

    @Override
    public ConsensusEvaluation<T> evaluate(List<T> proposals, List<ConsensusFramework.ConsensusAgent<T>> agents) {
        if (proposals == null || proposals.isEmpty()) {
            return buildNoConsensus();
        }

        // 构建相似度矩阵
        int n = proposals.size();
        double[][] similarityMatrix = new double[n][n];
        
        for (int i = 0; i < n; i++) {
            for (int j = i; j < n; j++) {
                double similarity = similarityFunction.apply(proposals.get(i), proposals.get(j));
                similarityMatrix[i][j] = similarity;
                similarityMatrix[j][i] = similarity;
            }
        }

        // 找出最大相似度簇
        ClusterResult<T> clusterResult = findLargestConsistentCluster(proposals, similarityMatrix);

        if (clusterResult == null || clusterResult.members.isEmpty()) {
            return buildNoConsensus();
        }

        T consensusValue = clusterResult.centroid;
        int clusterSize = clusterResult.members.size();
        
        // 计算支持率
        double supportRate = (double) clusterSize / n;
        
        // 计算置信度(簇内平均相似度)
        double confidence = clusterResult.avgSimilarity;
        
        // 检查是否达成共识
        boolean consensusReached = confidence >= config.getConsensusThreshold() 
            && supportRate >= config.getMinSupportRate();

        log.debug("Consistency check result: value={}, clusterSize={}/{}, support={}, confidence={}, reached={}", 
            consensusValue, clusterSize, n, supportRate, confidence, consensusReached);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("clusterSize", clusterSize);
        metadata.put("totalProposals", n);
        metadata.put("avgSimilarity", clusterResult.avgSimilarity);
        metadata.put("clusterMembers", clusterResult.members);

        return ConsensusEvaluation.<T>builder()
            .consensusReached(consensusReached)
            .consensusValue(consensusValue)
            .confidence(confidence)
            .supportRate(supportRate)
            .metadata(metadata)
            .build();
    }

    /**
     * 找出最大的一致性簇
     */
    private ClusterResult<T> findLargestConsistentCluster(List<T> proposals, double[][] similarityMatrix) {
        int n = proposals.size();
        ClusterResult<T> bestCluster = null;
        int maxClusterSize = 0;

        // 尝试以每个提议为中心构建簇
        for (int i = 0; i < n; i++) {
            List<Integer> cluster = new ArrayList<>();
            cluster.add(i);
            
            // 找出与当前提议相似度超过阈值的其他提议
            for (int j = 0; j < n; j++) {
                if (i != j && similarityMatrix[i][j] >= similarityThreshold) {
                    cluster.add(j);
                }
            }
            
            // 如果该簇更大,更新最佳簇
            if (cluster.size() > maxClusterSize) {
                maxClusterSize = cluster.size();
                
                // 计算簇内平均相似度
                double totalSimilarity = 0.0;
                int pairCount = 0;
                for (int m : cluster) {
                    for (int n_idx : cluster) {
                        if (m != n_idx) {
                            totalSimilarity += similarityMatrix[m][n_idx];
                            pairCount++;
                        }
                    }
                }
                double avgSimilarity = pairCount > 0 ? totalSimilarity / pairCount : 1.0;
                
                // 选择簇中心(相似度总和最高的提议)
                int centroidIdx = i;
                double maxSimilaritySum = 0.0;
                for (int m : cluster) {
                    double similaritySum = 0.0;
                    for (int n_idx : cluster) {
                        similaritySum += similarityMatrix[m][n_idx];
                    }
                    if (similaritySum > maxSimilaritySum) {
                        maxSimilaritySum = similaritySum;
                        centroidIdx = m;
                    }
                }
                
                List<T> members = cluster.stream()
                    .map(proposals::get)
                    .collect(Collectors.toList());
                
                bestCluster = new ClusterResult<>(proposals.get(centroidIdx), members, avgSimilarity);
            }
        }

        return bestCluster;
    }

    @Override
    public ConsensusEvaluation<T> fallback(List<ConsensusRecord<T>> history, 
                                           List<ConsensusFramework.ConsensusAgent<T>> agents) {
        // 回退策略: 综合所有历史提议进行一致性检查
        List<T> allProposals = history.stream()
            .flatMap(record -> record.getProposals().stream())
            .collect(Collectors.toList());

        if (allProposals.isEmpty()) {
            return buildNoConsensus();
        }

        return evaluate(allProposals, agents);
    }

    @Override
    public String getStrategyName() {
        return "ConsistencyCheck";
    }

    private ConsensusEvaluation<T> buildNoConsensus() {
        return ConsensusEvaluation.<T>builder()
            .consensusReached(false)
            .consensusValue(null)
            .confidence(0.0)
            .supportRate(0.0)
            .build();
    }

    /**
     * 簇结果
     */
    private static class ClusterResult<T> {
        T centroid;
        List<T> members;
        double avgSimilarity;

        ClusterResult(T centroid, List<T> members, double avgSimilarity) {
            this.centroid = centroid;
            this.members = members;
            this.avgSimilarity = avgSimilarity;
        }
    }
}
