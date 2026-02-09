package io.leavesfly.evox.rag.retriever;

import io.leavesfly.evox.rag.embedding.EmbeddingService;
import io.leavesfly.evox.rag.schema.Query;
import io.leavesfly.evox.rag.schema.RetrievalResult;
import io.leavesfly.evox.rag.vectorstore.DocumentVectorStore;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 向量检索器
 * 使用向量存储执行相似度检索
 *
 * @author EvoX Team
 */
@Slf4j
public class VectorRetriever implements Retriever {

    private final DocumentVectorStore vectorStore;
    private final EmbeddingService embeddingService;
    private final int defaultTopK;

    public VectorRetriever(DocumentVectorStore vectorStore, EmbeddingService embeddingService) {
        this(vectorStore, embeddingService, 5);
    }

    public VectorRetriever(DocumentVectorStore vectorStore, EmbeddingService embeddingService, int defaultTopK) {
        this.vectorStore = vectorStore;
        this.embeddingService = embeddingService;
        this.defaultTopK = defaultTopK;
        log.info("Initialized VectorRetriever with defaultTopK: {}", defaultTopK);
    }

    @Override
    public RetrievalResult retrieve(Query query) {
        if (query == null) {
            throw new IllegalArgumentException("Query cannot be null");
        }

        long startTime = System.currentTimeMillis();

        try {
            // 如果查询没有嵌入向量，生成它
            if (query.getEmbedding() == null || query.getEmbedding().isEmpty()) {
                if (query.getQueryText() == null || query.getQueryText().trim().isEmpty()) {
                    throw new IllegalArgumentException("Query must have either embedding or text");
                }
                
                log.debug("Generating embedding for query text: {}", query.getQueryText());
                List<Float> embedding = embeddingService.embed(query.getQueryText());
                query.setEmbedding(embedding);
            }

            // 确保topK有值
            int topK = query.getTopK() > 0 ? query.getTopK() : defaultTopK;
            query.setTopK(topK);

            // 执行向量检索
            RetrievalResult result = vectorStore.search(query);
            
            // 更新查询对象和耗时
            result.setQuery(query);
            if (result.getLatencyMs() == 0) {
                result.setLatencyMs(System.currentTimeMillis() - startTime);
            }

            log.info("Vector retrieval completed: {} results in {}ms for query: '{}'",
                    result.getTotalResults(), result.getLatencyMs(), 
                    truncateText(query.getQueryText(), 50));

            return result;

        } catch (Exception e) {
            log.error("Vector retrieval failed for query: '{}'. Error: {}", 
                    truncateText(query.getQueryText(), 50), e.getMessage(), e);
            throw new RuntimeException("Vector retrieval failed", e);
        }
    }

    @Override
    public String getType() {
        return "vector";
    }

    /**
     * 截断文本用于日志
     */
    private String truncateText(String text, int maxLength) {
        if (text == null) {
            return "null";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
