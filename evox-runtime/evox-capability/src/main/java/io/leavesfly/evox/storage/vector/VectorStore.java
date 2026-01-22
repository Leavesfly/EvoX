package io.leavesfly.evox.storage.vector;

import io.leavesfly.evox.storage.base.BaseStorage;

import java.util.List;
import java.util.Map;

/**
 * 向量存储接口
 * 定义向量数据存储和检索的统一接口
 * 
 * @author EvoX Team
 */
public interface VectorStore extends BaseStorage {

    /**
     * 添加向量
     * 
     * @param id 向量ID
     * @param vector 向量数据
     * @param metadata 元数据
     */
    void addVector(String id, float[] vector, Map<String, Object> metadata);

    /**
     * 批量添加向量
     * 
     * @param ids 向量ID列表
     * @param vectors 向量数据列表
     * @param metadataList 元数据列表
     */
    void addVectors(List<String> ids, List<float[]> vectors, List<Map<String, Object>> metadataList);

    /**
     * 相似度搜索
     * 
     * @param queryVector 查询向量
     * @param topK 返回前K个结果
     * @return 搜索结果
     */
    List<SearchResult> search(float[] queryVector, int topK);

    /**
     * 相似度搜索（带过滤条件）
     * 
     * @param queryVector 查询向量
     * @param topK 返回前K个结果
     * @param filter 过滤条件
     * @return 搜索结果
     */
    List<SearchResult> search(float[] queryVector, int topK, Map<String, Object> filter);

    /**
     * 删除向量
     * 
     * @param id 向量ID
     * @return 是否删除成功
     */
    boolean deleteVector(String id);

    /**
     * 批量删除向量
     * 
     * @param ids 向量ID列表
     * @return 删除数量
     */
    int deleteVectors(List<String> ids);

    /**
     * 获取向量总数
     * 
     * @return 向量数量
     */
    long getVectorCount();

    /**
     * 保存向量存储到指定路径
     * 
     * @param path 保存路径
     */
    void save(String path);

    /**
     * 从指定路径加载向量存储
     * 
     * @param path 加载路径
     */
    void load(String path);

    /**
     * 检查是否支持持久化
     * 
     * @return true如果支持持久化
     */
    default boolean supportsPersistence() {
        return false;
    }

    /**
     * 搜索结果类
     */
    class SearchResult {
        private String id;
        private float[] vector;
        private Map<String, Object> metadata;
        private float score;

        public SearchResult() {}

        public SearchResult(String id, float[] vector, Map<String, Object> metadata, float score) {
            this.id = id;
            this.vector = vector;
            this.metadata = metadata;
            this.score = score;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public float[] getVector() {
            return vector;
        }

        public void setVector(float[] vector) {
            this.vector = vector;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }

        public float getScore() {
            return score;
        }

        public void setScore(float score) {
            this.score = score;
        }
    }
}
