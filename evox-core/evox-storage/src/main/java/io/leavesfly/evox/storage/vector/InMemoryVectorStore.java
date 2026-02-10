package io.leavesfly.evox.storage.vector;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 内存向量存储实现
 * 使用内存进行向量存储和检索（适用于开发和测试）
 * 
 * @author EvoX Team
 */
@Slf4j
public class InMemoryVectorStore implements VectorStore {

    private final Map<String, VectorEntry> vectors = new ConcurrentHashMap<>();
    private boolean initialized = false;

    /**
     * 向量条目
     */
    private static class VectorEntry {
        String id;
        float[] vector;
        Map<String, Object> metadata;

        VectorEntry(String id, float[] vector, Map<String, Object> metadata) {
            this.id = id;
            this.vector = vector;
            this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
        }
    }

    @Override
    public void initialize() {
        if (!initialized) {
            vectors.clear();
            initialized = true;
            log.info("InMemoryVectorStore initialized");
        }
    }

    @Override
    public void close() {
        vectors.clear();
        initialized = false;
        log.info("InMemoryVectorStore closed");
    }

    @Override
    public void clear() {
        vectors.clear();
        log.info("InMemoryVectorStore cleared");
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public void addVector(String id, float[] vector, Map<String, Object> metadata) {
        if (!initialized) {
            initialize();
        }
        vectors.put(id, new VectorEntry(id, vector, metadata));
        log.debug("Added vector with ID: {}", id);
    }

    @Override
    public void addVectors(List<String> ids, List<float[]> vectors, List<Map<String, Object>> metadataList) {
        if (!initialized) {
            initialize();
        }

        if (ids.size() != vectors.size() || ids.size() != metadataList.size()) {
            throw new IllegalArgumentException("ids, vectors, and metadataList must have the same size");
        }

        for (int i = 0; i < ids.size(); i++) {
            addVector(ids.get(i), vectors.get(i), metadataList.get(i));
        }
    }

    @Override
    public List<SearchResult> search(float[] queryVector, int topK) {
        return search(queryVector, topK, null);
    }

    @Override
    public List<SearchResult> search(float[] queryVector, int topK, Map<String, Object> filter) {
        if (!initialized) {
            initialize();
        }

        // 计算所有向量的相似度
        List<SearchResult> results = vectors.values().stream()
            .filter(entry -> matchesFilter(entry.metadata, filter))
            .map(entry -> {
                float score = cosineSimilarity(queryVector, entry.vector);
                return new SearchResult(entry.id, entry.vector, entry.metadata, score);
            })
            .sorted((a, b) -> Float.compare(b.getScore(), a.getScore())) // 降序排列
            .limit(topK)
            .collect(Collectors.toList());

        log.debug("Search returned {} results", results.size());
        return results;
    }

    @Override
    public boolean deleteVector(String id) {
        if (!initialized) {
            return false;
        }
        boolean removed = vectors.remove(id) != null;
        if (removed) {
            log.debug("Deleted vector with ID: {}", id);
        }
        return removed;
    }

    @Override
    public int deleteVectors(List<String> ids) {
        if (!initialized) {
            return 0;
        }

        int count = 0;
        for (String id : ids) {
            if (deleteVector(id)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public long getVectorCount() {
        return vectors.size();
    }

    @Override
    public void save(String path) {
        throw new UnsupportedOperationException("InMemoryVectorStore 不支持持久化，请使用 PersistentVectorStore");
    }

    @Override
    public void load(String path) {
        throw new UnsupportedOperationException("InMemoryVectorStore 不支持持久化，请使用 PersistentVectorStore");
    }

    @Override
    public boolean supportsPersistence() {
        return false;
    }

    /**
     * 计算余弦相似度
     */
    private float cosineSimilarity(float[] vec1, float[] vec2) {
        if (vec1.length != vec2.length) {
            throw new IllegalArgumentException("Vectors must have the same dimension");
        }

        float dotProduct = 0.0f;
        float norm1 = 0.0f;
        float norm2 = 0.0f;

        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
            norm1 += vec1[i] * vec1[i];
            norm2 += vec2[i] * vec2[i];
        }

        if (norm1 == 0.0f || norm2 == 0.0f) {
            return 0.0f;
        }

        return dotProduct / (float) (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * 检查元数据是否匹配过滤条件
     */
    private boolean matchesFilter(Map<String, Object> metadata, Map<String, Object> filter) {
        if (filter == null || filter.isEmpty()) {
            return true;
        }

        for (Map.Entry<String, Object> entry : filter.entrySet()) {
            Object metadataValue = metadata.get(entry.getKey());
            Object filterValue = entry.getValue();

            if (metadataValue == null || !metadataValue.equals(filterValue)) {
                return false;
            }
        }

        return true;
    }
}
