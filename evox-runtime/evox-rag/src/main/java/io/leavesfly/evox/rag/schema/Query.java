package io.leavesfly.evox.rag.schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 查询类 - 用于RAG检索的查询对象
 *
 * @author EvoX Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Query {

    /**
     * 查询文本
     */
    private String queryText;

    /**
     * 查询嵌入向量
     */
    private List<Float> embedding;

    /**
     * 返回结果数量
     */
    @Builder.Default
    private int topK = 5;

    /**
     * 相似度阈值
     */
    @Builder.Default
    private float similarityThreshold = 0.0f;

    /**
     * 查询元数据过滤条件
     */
    @Builder.Default
    private Map<String, Object> metadataFilter = new HashMap<>();

    /**
     * 查询选项
     */
    @Builder.Default
    private QueryOptions options = new QueryOptions();

    /**
     * 查询选项类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QueryOptions {
        /**
         * 是否包含嵌入向量
         */
        @Builder.Default
        private boolean includeEmbeddings = false;

        /**
         * 是否包含元数据
         */
        @Builder.Default
        private boolean includeMetadata = true;

        /**
         * 重排序
         */
        @Builder.Default
        private boolean rerank = false;

        /**
         * 重排序模型
         */
        private String rerankModel;
    }

    /**
     * 添加元数据过滤条件
     */
    public void addMetadataFilter(String key, Object value) {
        if (metadataFilter == null) {
            metadataFilter = new HashMap<>();
        }
        metadataFilter.put(key, value);
    }
}
