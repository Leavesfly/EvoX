package io.leavesfly.evox.rag.vectorstore;

import io.leavesfly.evox.rag.schema.Chunk;
import io.leavesfly.evox.rag.schema.Query;
import io.leavesfly.evox.rag.schema.RetrievalResult;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 支持持久化的向量存储实现
 * 基于内存索引 + 文件持久化
 *
 * @author EvoX Team
 */
@Slf4j
public class PersistentVectorStore implements DocumentVectorStore {

    private final Map<String, Chunk> chunkStore = new ConcurrentHashMap<>();
    private final int dimensions;
    private volatile boolean modified = false;

    public PersistentVectorStore(int dimensions) {
        this.dimensions = dimensions;
        log.info("Initialized PersistentVectorStore with dimensions: {}", dimensions);
    }

    @Override
    public void add(Chunk chunk) {
        if (chunk == null || chunk.getId() == null) {
            throw new IllegalArgumentException("Chunk and chunk ID cannot be null");
        }
        
        // 验证向量维度
        if (chunk.getEmbedding() != null && chunk.getEmbedding().size() != dimensions) {
            throw new IllegalArgumentException(
                    String.format("Embedding dimension mismatch: expected %d, got %d", 
                            dimensions, chunk.getEmbedding().size()));
        }
        
        chunkStore.put(chunk.getId(), chunk);
        modified = true;
        log.debug("Added chunk: {}", chunk.getId());
    }

    @Override
    public void addBatch(List<Chunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
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
        if (removed != null) {
            modified = true;
            return true;
        }
        return false;
    }

    @Override
    public void clear() {
        chunkStore.clear();
        modified = true;
        log.info("Cleared all chunks from vector store");
    }

    @Override
    public int size() {
        return chunkStore.size();
    }

    @Override
    public void save(String pathStr) {
        if (pathStr == null || pathStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Save path cannot be null or empty");
        }

        try {
            Path path = Paths.get(pathStr);
            
            // 创建父目录
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }

            // 序列化到文件
            try (ObjectOutputStream oos = new ObjectOutputStream(
                    new BufferedOutputStream(new FileOutputStream(path.toFile())))) {
                
                // 写入元数据
                oos.writeInt(dimensions);
                oos.writeInt(chunkStore.size());
                
                // 写入所有分块
                for (Chunk chunk : chunkStore.values()) {
                    oos.writeObject(chunk);
                }
                
                modified = false;
                log.info("Saved {} chunks to: {}", chunkStore.size(), pathStr);
            }

        } catch (IOException e) {
            log.error("Failed to save vector store to {}: {}", pathStr, e.getMessage(), e);
            throw new RuntimeException("Failed to save vector store", e);
        }
    }

    @Override
    public void load(String pathStr) {
        if (pathStr == null || pathStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Load path cannot be null or empty");
        }

        Path path = Paths.get(pathStr);
        if (!Files.exists(path)) {
            log.warn("Vector store file not found: {}", pathStr);
            return;
        }

        try {
            // 反序列化文件
            try (ObjectInputStream ois = new ObjectInputStream(
                    new BufferedInputStream(new FileInputStream(path.toFile())))) {
                
                // 读取元数据
                int savedDimensions = ois.readInt();
                if (savedDimensions != dimensions) {
                    throw new IllegalStateException(
                            String.format("Dimension mismatch: expected %d, file has %d", 
                                    dimensions, savedDimensions));
                }
                
                int count = ois.readInt();
                
                // 清空现有数据
                chunkStore.clear();
                
                // 读取所有分块
                for (int i = 0; i < count; i++) {
                    Chunk chunk = (Chunk) ois.readObject();
                    chunkStore.put(chunk.getId(), chunk);
                }
                
                modified = false;
                log.info("Loaded {} chunks from: {}", count, pathStr);
            }

        } catch (IOException | ClassNotFoundException e) {
            log.error("Failed to load vector store from {}: {}", pathStr, e.getMessage(), e);
            throw new RuntimeException("Failed to load vector store", e);
        }
    }

    /**
     * 检查是否有未保存的修改
     */
    public boolean isModified() {
        return modified;
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
