package io.leavesfly.evox.rag.postprocessor;

import io.leavesfly.evox.rag.schema.RetrievalResult;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 重排序后处理器
 * 基于相似度对检索结果进行重排序
 *
 * @author EvoX Team
 */
@Slf4j
@Data
public class SimpleReranker implements Postprocessor {

    private int topK;

    public SimpleReranker(int topK) {
        this.topK = topK;
    }

    @Override
    public List<RetrievalResult> process(List<RetrievalResult> results) {
        log.debug("Reranking {} results, keeping top {}", results.size(), topK);
        
        // Sort by the top chunk's similarity score
        return results.stream()
                .sorted((a, b) -> {
                    Float scoreA = a.getTopChunk() != null ? a.getTopChunk().getSimilarityScore() : 0.0f;
                    Float scoreB = b.getTopChunk() != null ? b.getTopChunk().getSimilarityScore() : 0.0f;
                    return Float.compare(scoreB, scoreA);
                })
                .limit(topK)
                .collect(Collectors.toList());
    }
}
