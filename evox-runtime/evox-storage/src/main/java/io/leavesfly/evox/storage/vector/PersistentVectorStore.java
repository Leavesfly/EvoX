package io.leavesfly.evox.storage.vector;

import io.leavesfly.evox.exception.StorageException;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 持久化向量存储实现
 * 支持将向量数据保存到文件并从文件加载
 * 
 * @author EvoX Team
 */
@Slf4j
public class PersistentVectorStore implements VectorStore {

    /**
     * 向量存储映射
     */
    private final Map<String, VectorEntry> vectors = new ConcurrentHashMap<>();
    
    /**
     * 向量维度
     */
    private final int dimension;
    
    /**
     * 是否已初始化
     */
    private boolean initialized = false;
    
    /**
     * 是否有未保存的修改
     */
    private volatile boolean modified = false;
    
    /**
     * 自动保存路径（可选）
     */
    private String autoSavePath;

    /**
     * 存储数据容器（用于序列化）
     */
    private static class StorageData implements Serializable {
        private static final long serialVersionUID = 1L;
        
        int dimension;
        List<VectorEntry> entries;
        
        StorageData(int dimension, Collection<VectorEntry> entries) {
            this.dimension = dimension;
            this.entries = new ArrayList<>(entries);
        }
    }

    /**
     * 创建持久化向量存储
     * 
     * @param dimension 向量维度
     */
    public PersistentVectorStore(int dimension) {
        this.dimension = dimension;
        log.info("创建持久化向量存储，维度: {}", dimension);
    }

    /**
     * 创建持久化向量存储并指定自动保存路径
     * 
     * @param dimension 向量维度
     * @param autoSavePath 自动保存路径
     */
    public PersistentVectorStore(int dimension, String autoSavePath) {
        this(dimension);
        this.autoSavePath = autoSavePath;
    }

    @Override
    public void initialize() {
        if (!initialized) {
            vectors.clear();
            
            // 如果有自动保存路径，尝试加载
            if (autoSavePath != null && Files.exists(Paths.get(autoSavePath))) {
                try {
                    load(autoSavePath);
                    log.info("从 {} 自动加载了向量存储", autoSavePath);
                } catch (StorageException e) {
                    log.warn("自动加载失败，使用空存储: {}", e.getMessage());
                } catch (Exception e) {
                    log.warn("自动加载失败，使用空存储: {}", e.getMessage());
                }
            }
            
            initialized = true;
            log.info("持久化向量存储初始化完成");
        }
    }

    @Override
    public void close() {
        // 如果有未保存的修改且配置了自动保存路径，则自动保存
        if (modified && autoSavePath != null) {
            try {
                save(autoSavePath);
                log.info("关闭时自动保存到 {}", autoSavePath);
            } catch (StorageException e) {
                log.error("自动保存失败: {}", e.getMessage(), e);
            } catch (Exception e) {
                log.error("自动保存失败: {}", e.getMessage(), e);
            }
        }
        
        vectors.clear();
        initialized = false;
        modified = false;
        log.info("持久化向量存储已关闭");
    }

    @Override
    public void clear() {
        vectors.clear();
        modified = true;
        log.info("向量存储已清空");
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
        
        // 验证维度
        if (vector != null && vector.length != dimension) {
            throw new IllegalArgumentException(
                String.format("向量维度不匹配: 期望 %d, 实际 %d", dimension, vector.length));
        }
        
        vectors.put(id, new VectorEntry(id, vector, metadata));
        modified = true;
        log.debug("添加向量: {}", id);
    }

    @Override
    public void addVectors(List<String> ids, List<float[]> vecs, List<Map<String, Object>> metadataList) {
        if (!initialized) {
            initialize();
        }

        if (ids.size() != vecs.size() || ids.size() != metadataList.size()) {
            throw new IllegalArgumentException("ids, vectors, metadataList 长度必须相同");
        }

        for (int i = 0; i < ids.size(); i++) {
            addVector(ids.get(i), vecs.get(i), metadataList.get(i));
        }
        
        log.info("批量添加 {} 个向量", ids.size());
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

        return vectors.values().stream()
            .filter(entry -> matchesFilter(entry.getMetadata(), filter))
            .map(entry -> {
                float score = cosineSimilarity(queryVector, entry.getVector());
                return new SearchResult(entry.getId(), entry.getVector(), entry.getMetadata(), score);
            })
            .sorted((a, b) -> Float.compare(b.getScore(), a.getScore()))
            .limit(topK)
            .collect(Collectors.toList());
    }

    @Override
    public boolean deleteVector(String id) {
        if (!initialized) {
            return false;
        }
        
        boolean removed = vectors.remove(id) != null;
        if (removed) {
            modified = true;
            log.debug("删除向量: {}", id);
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
        if (path == null || path.trim().isEmpty()) {
            throw StorageException.vectorStoreError("save", "保存路径不能为空");
        }

        Path filePath = Paths.get(path);
        Path tempFile = null;
        
        try {
            // 创建父目录
            if (filePath.getParent() != null) {
                Files.createDirectories(filePath.getParent());
            }

            // 创建临时文件（与目标文件同目录，确保原子重命名可行）
            tempFile = Files.createTempFile(filePath.getParent(), "vector-store-", ".tmp");

            // 创建存储数据
            StorageData data = new StorageData(dimension, vectors.values());
            
            // 先序列化保存到临时文件
            try (ObjectOutputStream oos = new ObjectOutputStream(
                    new BufferedOutputStream(new FileOutputStream(tempFile.toFile())))) {
                oos.writeObject(data);
            }
            
            // 原子重命名：临时文件 -> 目标文件
            // ATOMIC_MOVE 确保操作是原子的，REPLACE_EXISTING 允许覆盖已存在的文件
            Files.move(tempFile, filePath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            
            modified = false;
            log.info("保存 {} 个向量到: {}", vectors.size(), path);

        } catch (IOException e) {
            log.error("保存向量存储失败: {}", e.getMessage(), e);
            // 清理临时文件
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException cleanupError) {
                    log.warn("清理临时文件失败: {}", tempFile, cleanupError);
                }
            }
            throw StorageException.vectorStoreError("save", "保存向量存储失败", e);
        } catch (UnsupportedOperationException e) {
            // ATOMIC_MOVE 在某些文件系统上可能不支持，降级为普通重命名
            log.warn("文件系统不支持原子移动，使用普通重命名: {}", path);
            try {
                if (tempFile != null && Files.exists(tempFile)) {
                    Files.move(tempFile, filePath, StandardCopyOption.REPLACE_EXISTING);
                }
                modified = false;
                log.info("保存 {} 个向量到: {}", vectors.size(), path);
            } catch (IOException fallbackError) {
                log.error("降级保存也失败", fallbackError);
                throw StorageException.vectorStoreError("save", "保存向量存储失败", fallbackError);
            }
        }
    }

    @Override
    public void load(String path) {
        if (path == null || path.trim().isEmpty()) {
            throw StorageException.vectorStoreError("load", "加载路径不能为空");
        }

        Path filePath = Paths.get(path);
        if (!Files.exists(filePath)) {
            log.warn("向量存储文件不存在: {}", path);
            return;
        }

        try {
            // 反序列化加载
            try (ObjectInputStream ois = new ObjectInputStream(
                    new BufferedInputStream(new FileInputStream(filePath.toFile())))) {
                
                StorageData data = (StorageData) ois.readObject();
                
                // 验证维度
                if (data.dimension != dimension) {
                    throw new IllegalStateException(
                        String.format("维度不匹配: 期望 %d, 文件中为 %d", dimension, data.dimension));
                }
                
                // 清空并加载
                vectors.clear();
                for (VectorEntry entry : data.entries) {
                    vectors.put(entry.getId(), entry);
                }
                
                modified = false;
                log.info("从 {} 加载了 {} 个向量", path, vectors.size());
            }

        } catch (StorageException e) {
            log.error("加载向量存储失败: {}", e.getMessage(), e);
            throw e;
        } catch (IOException | ClassNotFoundException e) {
            log.error("加载向量存储失败: {}", e.getMessage(), e);
            throw StorageException.vectorStoreError("load", "加载向量存储失败", e);
        }
    }

    @Override
    public boolean supportsPersistence() {
        return true;
    }

    /**
     * 检查是否有未保存的修改
     */
    public boolean isModified() {
        return modified;
    }

    /**
     * 获取向量维度
     */
    public int getDimension() {
        return dimension;
    }

    /**
     * 设置自动保存路径
     */
    public void setAutoSavePath(String path) {
        this.autoSavePath = path;
    }

    /**
     * 计算余弦相似度
     */
    private float cosineSimilarity(float[] vec1, float[] vec2) {
        if (vec1 == null || vec2 == null) {
            return 0.0f;
        }
        
        if (vec1.length != vec2.length) {
            throw new IllegalArgumentException("向量维度必须相同");
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
