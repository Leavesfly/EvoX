package io.leavesfly.evox.rag.vectorstore;

import io.leavesfly.evox.rag.schema.Chunk;
import io.leavesfly.evox.rag.schema.Query;
import io.leavesfly.evox.rag.schema.RetrievalResult;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 内存向量存储实现
 * 使用余弦相似度进行检索
 *
 * @author EvoX Team
 */
@Slf4j
public class InMemoryVectorStore implements DocumentVectorStore {

    private final Map<String, Chunk> chunkStore = new ConcurrentHashMap<>();

    @Override
    public void add(Chunk chunk) {
        if (chunk == null || chunk.getId() == null) {
            throw new IllegalArgumentException("Chunk and chunk ID cannot be null");
        }
        chunkStore.put(chunk.getId(), chunk);
        log.debug("Added chunk: {}", chunk.getId());
    }

    @Override
    public void addBatch(List<Chunk> chunks) {
        if (chunks == null) {
            return;
        }
        for (Chunk chunk : chunks) {
            add(chunk);
        }
        log.info("Added {} chunks to vector store", chunks.size());
    }

    @Override
    public RetrievalResult search(Query query) {
        if (query == null || query.getEmbedding() == null) {
            throw new IllegalArgumentException("Query and query embedding cannot be null");
        }

        long startTime = System.currentTimeMillis();
        
        // 计算所有分块与查询的相似度
        List<Chunk> rankedChunks = chunkStore.values().stream()
                .filter(chunk -> chunk.getEmbedding() != null)
                .map(chunk -> {
                    float similarity = cosineSimilarity(query.getEmbedding(), chunk.getEmbedding());
                    Chunk scoredChunk = cloneChunk(chunk);
                    scoredChunk.setSimilarityScore(similarity);
                    return scoredChunk;
                })
                .filter(chunk -> chunk.getSimilarityScore() >= query.getSimilarityThreshold())
                .sorted((a, b) -> Float.compare(b.getSimilarityScore(), a.getSimilarityScore()))
                .limit(query.getTopK())
                .collect(Collectors.toList());

        RetrievalResult result = RetrievalResult.builder()
                .chunks(rankedChunks)
                .totalResults(rankedChunks.size())
                .latencyMs(System.currentTimeMillis() - startTime)
                .build();

        log.debug("Search completed: {} results in {}ms", 
                result.getTotalResults(), result.getLatencyMs());
        
        return result;
    }

    @Override
    public boolean delete(String chunkId) {
        Chunk removed = chunkStore.remove(chunkId);
        return removed != null;
    }

    @Override
    public void clear() {
        chunkStore.clear();
        log.info("Cleared all chunks from vector store");
    }

    @Override
    public int size() {
        return chunkStore.size();
    }

    @Override
    public void save(String path) {
        throw new UnsupportedOperationException("InMemoryVectorStore does not support persistence");
    }

    @Override
    public void load(String path) {
        throw new UnsupportedOperationException("InMemoryVectorStore does not support persistence");
    }

    /**
     * 计算余弦相似度
     */
    private float cosineSimilarity(List<Float> vec1, List<Float> vec2) {
        if (vec1.size() != vec2.size()) {
            throw new IllegalArgumentException("Vectors must have same dimension");
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vec1.size(); i++) {
            dotProduct += vec1.get(i) * vec2.get(i);
            norm1 += vec1.get(i) * vec1.get(i);
            norm2 += vec2.get(i) * vec2.get(i);
        }

        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0f;
        }

        return (float) (dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2)));
    }

    /**
     * 克隆分块对象
     */
    private Chunk cloneChunk(Chunk original) {
        return Chunk.builder()
                .id(original.getId())
                .text(original.getText())
                .embedding(original.getEmbedding())
                .metadata(new HashMap<>(original.getMetadata()))
                .documentId(original.getDocumentId())
                .chunkIndex(original.getChunkIndex())
                .startPosition(original.getStartPosition())
                .endPosition(original.getEndPosition())
                .timestamp(original.getTimestamp())
                .build();
    }
}
