package io.leavesfly.evox.storage.vector;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Chroma向量存储实现
 *
 * @author EvoX Team
 */
@Slf4j
@Data
public class ChromaVectorStore implements VectorStore {

    private String host;
    private int port;
    private String collectionName;
    private boolean initialized = false;
    private String baseUrl;
    
    // 内存存储，用于模拟实现
    private final Map<String, VectorEntry> vectorStorage = new HashMap<>();

    public ChromaVectorStore(String host, int port, String collectionName) {
        this.host = host;
        this.port = port;
        this.collectionName = collectionName;
        this.baseUrl = String.format("http://%s:%d/api/v1", host, port);
    }

    @Override
    public void initialize() {
        if (!initialized) {
            log.info("Initializing Chroma vector store: {}", collectionName);
            try {
                // 尝试创建或连接到集合
                createCollection();
                initialized = true;
                log.info("Chroma vector store initialized successfully");
            } catch (Exception e) {
                log.warn("Failed to connect to Chroma server, using in-memory fallback: {}", e.getMessage());
                // 使用内存备用方案
                initialized = true;
            }
        }
    }

    @Override
    public void close() {
        log.info("Closing Chroma vector store: {}", collectionName);
        vectorStorage.clear();
        initialized = false;
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public void addVector(String id, float[] vector, Map<String, Object> metadata) {
        log.debug("Adding vector to Chroma: {}", id);
        try {
            // 尝试通过HTTP API添加
            if (isChromaServerAvailable()) {
                addVectorViaApi(id, vector, metadata);
            } else {
                // 备用方案：内存存储
                vectorStorage.put(id, new VectorEntry(id, vector, metadata));
                log.debug("Vector added to in-memory storage: {}", id);
            }
        } catch (Exception e) {
            log.error("Failed to add vector: {}", id, e);
            // 备用到内存
            vectorStorage.put(id, new VectorEntry(id, vector, metadata));
        }
    }

    @Override
    public void addVectors(List<String> ids, List<float[]> vectors, List<Map<String, Object>> metadataList) {
        log.info("Adding {} vectors to Chroma", ids.size());
        for (int i = 0; i < ids.size(); i++) {
            addVector(ids.get(i), vectors.get(i), 
                     i < metadataList.size() ? metadataList.get(i) : new HashMap<>());
        }
    }

    @Override
    public List<SearchResult> search(float[] queryVector, int topK) {
        return search(queryVector, topK, new HashMap<>());
    }

    @Override
    public List<SearchResult> search(float[] queryVector, int topK, Map<String, Object> filter) {
        log.debug("Searching in Chroma collection with filter: {}", collectionName);
        
        try {
            if (isChromaServerAvailable()) {
                return searchViaApi(queryVector, topK, filter);
            } else {
                // 备用方案：内存搜索
                return searchInMemory(queryVector, topK, filter);
            }
        } catch (Exception e) {
            log.error("Search failed, using in-memory fallback", e);
            return searchInMemory(queryVector, topK, filter);
        }
    }

    @Override
    public boolean deleteVector(String id) {
        log.debug("Deleting vector from Chroma: {}", id);
        try {
            if (isChromaServerAvailable()) {
                return deleteVectorViaApi(id);
            } else {
                return vectorStorage.remove(id) != null;
            }
        } catch (Exception e) {
            log.error("Failed to delete vector: {}", id, e);
            return vectorStorage.remove(id) != null;
        }
    }

    @Override
    public int deleteVectors(List<String> ids) {
        log.info("Deleting {} vectors from Chroma", ids.size());
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
        return vectorStorage.size();
    }

    @Override
    public void clear() {
        log.info("Clearing Chroma collection: {}", collectionName);
        vectorStorage.clear();
    }

    @Override
    public void save(String path) {
        // Chroma 的持久化由 Chroma 服务器管理
        throw new UnsupportedOperationException("Chroma 存储由服务器管理，请使用 PersistentVectorStore");
    }

    @Override
    public void load(String path) {
        // Chroma 的持久化由 Chroma 服务器管理
        throw new UnsupportedOperationException("Chroma 存储由服务器管理，请使用 PersistentVectorStore");
    }

    /**
     * 创建集合
     */
    public void createCollection() {
        log.info("Creating Chroma collection: {}", collectionName);
        // 尝试通过API创建，如果失败则使用内存
        try {
            if (isChromaServerAvailable()) {
                createCollectionViaApi();
            }
        } catch (Exception e) {
            log.debug("Failed to create collection via API: {}", e.getMessage());
        }
    }
    
    /**
     * 检查Chroma服务器是否可用
     */
    private boolean isChromaServerAvailable() {
        // 简化实现，假设本地有Chroma服务
        return false; // 默认使用内存存储
    }
    
    /**
     * 通过API创建集合
     */
    private void createCollectionViaApi() throws IOException {
        String url = baseUrl + "/collections";
        String json = String.format("{\"name\":\"%s\"}", collectionName);
        sendHttpPost(url, json);
    }
    
    /**
     * 通过API添加向量
     */
    private void addVectorViaApi(String id, float[] vector, Map<String, Object> metadata) throws IOException {
        String url = baseUrl + "/collections/" + collectionName + "/add";
        String json = buildAddVectorJson(id, vector, metadata);
        sendHttpPost(url, json);
    }
    
    /**
     * 通过API搜索
     */
    private List<SearchResult> searchViaApi(float[] queryVector, int topK, Map<String, Object> filter) {
        // API调用实现
        return new ArrayList<>();
    }
    
    /**
     * 通过API删除
     */
    private boolean deleteVectorViaApi(String id) {
        return false;
    }
    
    /**
     * 内存搜索实现
     */
    private List<SearchResult> searchInMemory(float[] queryVector, int topK, Map<String, Object> filter) {
        List<SearchResult> results = new ArrayList<>();
        
        for (VectorEntry entry : vectorStorage.values()) {
            // 计算余弦相似度
            float similarity = cosineSimilarity(queryVector, entry.vector);
            SearchResult result = new SearchResult();
            result.setId(entry.id);
            result.setScore(similarity);
            result.setMetadata(entry.metadata);
            results.add(result);
        }
        
        // 按相似度排序
        results.sort((a, b) -> Float.compare(b.getScore(), a.getScore()));
        
        // 返回topK
        return results.subList(0, Math.min(topK, results.size()));
    }
    
    /**
     * 计算余弦相似度
     */
    private float cosineSimilarity(float[] vec1, float[] vec2) {
        if (vec1.length != vec2.length) {
            return 0.0f;
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
     * 构建添加向量JSON
     */
    private String buildAddVectorJson(String id, float[] vector, Map<String, Object> metadata) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"ids\":[\"" + id + "\"],");
        json.append("\"embeddings\":[");
        json.append("[");
        for (int i = 0; i < vector.length; i++) {
            json.append(vector[i]);
            if (i < vector.length - 1) json.append(",");
        }
        json.append("]],");
        json.append("\"metadatas\":[{}");
        json.append("}]");
        json.append("}");
        return json.toString();
    }
    
    /**
     * 发送HTTP POST请求
     */
    private String sendHttpPost(String urlString, String json) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = json.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        
        int responseCode = conn.getResponseCode();
        log.debug("HTTP POST response code: {}", responseCode);
        
        return "";
    }
    
    /**
     * 向量存储项
     */
    @Data
    private static class VectorEntry {
        private final String id;
        private final float[] vector;
        private final Map<String, Object> metadata;
    }
}
