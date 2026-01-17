package io.leavesfly.evox.rag.reranker;

import io.leavesfly.evox.rag.schema.Chunk;
import io.leavesfly.evox.rag.schema.Query;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 交叉编码器重排序器 - 基于query-document交叉编码计算相关性
 *
 * @author EvoX Team
 */
@Slf4j
public class CrossEncoderReranker implements Reranker {

    private final String modelName;
    private final double scoreThreshold;

    public CrossEncoderReranker(String modelName, double scoreThreshold) {
        this.modelName = modelName;
        this.scoreThreshold = scoreThreshold;
        log.info("Initialized CrossEncoderReranker with model: {}, threshold: {}", 
                modelName, scoreThreshold);
    }

    public CrossEncoderReranker(String modelName) {
        this(modelName, 0.0);
    }

    @Override
    public List<Chunk> rerank(Query query, List<Chunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return new ArrayList<>();
        }

        log.debug("Reranking {} chunks with CrossEncoder model: {}", chunks.size(), modelName);

        // 创建副本避免修改原始列表
        List<Chunk> rerankedChunks = new ArrayList<>(chunks);

        // 计算交叉编码分数
        for (Chunk chunk : rerankedChunks) {
            double crossEncoderScore = computeCrossEncoderScore(query.getQueryText(), chunk.getText());
            // 更新similarityScore为交叉编码分数
            chunk.setSimilarityScore((float) crossEncoderScore);
        }

        // 按分数降序排序
        rerankedChunks.sort(Comparator.comparing(
                Chunk::getSimilarityScore, 
                Comparator.nullsLast(Comparator.reverseOrder())
        ));

        // 过滤低于阈值的结果
        if (scoreThreshold > 0) {
            rerankedChunks.removeIf(chunk -> 
                chunk.getSimilarityScore() == null || chunk.getSimilarityScore() < scoreThreshold
            );
        }

        log.debug("Reranking completed. Returned {} chunks", rerankedChunks.size());
        return rerankedChunks;
    }

    /**
     * 计算交叉编码分数
     * TODO: 实际实现需要调用真实的CrossEncoder模型
     */
    private double computeCrossEncoderScore(String query, String document) {
        // 当前为模拟实现
        // 实际应该调用模型服务，例如：
        // - Hugging Face sentence-transformers/cross-encoder模型
        // - OpenAI embeddings with dot product
        // - 本地部署的BERT cross-encoder
        
        // 简单的基于词重叠的相似度计算（仅用于演示）
        String[] queryTokens = query.toLowerCase().split("\\s+");
        String[] docTokens = document.toLowerCase().split("\\s+");
        
        int matchCount = 0;
        for (String qToken : queryTokens) {
            for (String dToken : docTokens) {
                if (qToken.equals(dToken)) {
                    matchCount++;
                    break;
                }
            }
        }
        
        return (double) matchCount / queryTokens.length;
    }

    @Override
    public String getName() {
        return "CrossEncoderReranker(" + modelName + ")";
    }
}
