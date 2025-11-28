package io.leavesfly.evox.rag.schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 文档类 - RAG系统的基本单元
 *
 * @author EvoX Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    /**
     * 文档ID
     */
    @Builder.Default
    private String id = UUID.randomUUID().toString();

    /**
     * 文档文本内容
     */
    private String text;

    /**
     * 文档元数据
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * 文档嵌入向量
     */
    private List<Float> embedding;

    /**
     * 文档来源
     */
    private String source;

    /**
     * 文档类型
     */
    private DocumentType type;

    /**
     * 创建时间戳
     */
    @Builder.Default
    private long timestamp = System.currentTimeMillis();

    /**
     * 文档类型枚举
     */
    public enum DocumentType {
        TEXT,
        PDF,
        DOCX,
        HTML,
        MARKDOWN,
        JSON,
        CSV,
        UNKNOWN
    }

    /**
     * 设置元数据项
     */
    public void setMetadata(String key, Object value) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put(key, value);
    }

    /**
     * 获取元数据项
     */
    @SuppressWarnings("unchecked")
    public <T> T getMetadata(String key) {
        if (metadata == null) {
            return null;
        }
        return (T) metadata.get(key);
    }

    /**
     * 计算文档哈希
     */
    public String computeHash() {
        return Integer.toHexString((text + source).hashCode());
    }
}
