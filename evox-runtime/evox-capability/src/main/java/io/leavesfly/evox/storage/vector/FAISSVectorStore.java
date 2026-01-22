package io.leavesfly.evox.storage.vector;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * FAISS向量存储实现
 * Facebook AI Similarity Search
 *
 * @author EvoX Team
 */
@Slf4j
@Data
public class FAISSVectorStore implements VectorStore {

    private int dimension;
    private String indexType;
    private Map<String, float[]> vectors;
    private Map<String, Map<String, Object>> metadata;
    private boolean initialized = false;

    public FAISSVectorStore(int dimension, String indexType) {
        this.dimension = dimension;
        this.indexType = indexType;
        this.vectors = new HashMap<>();
        this.metadata = new HashMap<>();
    }

    @Override
    public void initialize() {
        if (!initialized) {
            log.info("Initializing FAISS vector store with dimension: {}", dimension);
            vectors.clear();
            metadata.clear();
            initialized = true;
        }
    }

    @Override
    public void close() {
        log.info("Closing FAISS vector store");
        vectors.clear();
        metadata.clear();
        initialized = false;
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
        if (vector.length != dimension) {
            throw new IllegalArgumentException(
                String.format("Vector dimension %d does not match index dimension %d", 
                             vector.length, dimension));
        }
        
        vectors.put(id, vector);
        this.metadata.put(id, metadata);
        log.debug("Added vector to FAISS index: {}", id);
    }

    @Override
    public void addVectors(List<String> ids, List<float[]> vectors, List<Map<String, Object>> metadataList) {
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
        
        List<SearchResult> results = new ArrayList<>();
        
        // 计算余弦相似度
        for (Map.Entry<String, float[]> entry : vectors.entrySet()) {
            if (filter == null || matchesFilter(metadata.get(entry.getKey()), filter)) {
                float similarity = cosineSimilarity(queryVector, entry.getValue());
                results.add(new SearchResult(entry.getKey(), entry.getValue(), 
                                            metadata.get(entry.getKey()), similarity));
            }
        }
        
        // 排序并返回topK
        results.sort((a, b) -> Float.compare(b.getScore(), a.getScore()));
        return results.subList(0, Math.min(topK, results.size()));
    }

    @Override
    public boolean deleteVector(String id) {
        if (!initialized) {
            return false;
        }
        boolean removed = vectors.remove(id) != null;
        metadata.remove(id);
        if (removed) {
            log.debug("Deleted vector from FAISS index: {}", id);
        }
        return removed;
    }

    @Override
    public int deleteVectors(List<String> ids) {
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
    public void clear() {
        vectors.clear();
        metadata.clear();
        log.info("Cleared FAISS index");
    }

    @Override
    public void save(String path) {
        // FAISS 索引保存需要原生 FAISS 库支持
        throw new UnsupportedOperationException("FAISS 索引保存需要原生库支持，请使用 PersistentVectorStore");
    }

    @Override
    public void load(String path) {
        // FAISS 索引加载需要原生 FAISS 库支持
        throw new UnsupportedOperationException("FAISS 索引加载需要原生库支持，请使用 PersistentVectorStore");
    }

    /**
     * 计算余弦相似度
     */
    private float cosineSimilarity(float[] vec1, float[] vec2) {
        float dotProduct = 0.0f;
        float norm1 = 0.0f;
        float norm2 = 0.0f;
        
        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
            norm1 += vec1[i] * vec1[i];
            norm2 += vec2[i] * vec2[i];
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

    /**
     * 获取索引大小
     */
    public int size() {
        return vectors.size();
    }
}
