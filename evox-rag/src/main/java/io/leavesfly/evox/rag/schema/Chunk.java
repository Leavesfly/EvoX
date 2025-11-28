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
 * 文档分块类 - 文档切分后的片段
 *
 * @author EvoX Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Chunk {

    /**
     * 分块ID
     */
    @Builder.Default
    private String id = UUID.randomUUID().toString();

    /**
     * 分块文本内容
     */
    private String text;

    /**
     * 分块嵌入向量
     */
    private List<Float> embedding;

    /**
     * 分块元数据
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * 来源文档ID
     */
    private String documentId;

    /**
     * 在文档中的索引位置
     */
    private int chunkIndex;

    /**
     * 在文档中的起始位置
     */
    private int startPosition;

    /**
     * 在文档中的结束位置
     */
    private int endPosition;

    /**
     * 相似度分数(用于检索结果)
     */
    private Float similarityScore;

    /**
     * 创建时间戳
     */
    @Builder.Default
    private long timestamp = System.currentTimeMillis();

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
     * 计算分块大小
     */
    public int size() {
        return text != null ? text.length() : 0;
    }

    /**
     * 从文档创建分块
     */
    public static Chunk fromDocument(Document document, int chunkIndex, int startPos, int endPos) {
        String chunkText = document.getText().substring(startPos, endPos);
        
        return Chunk.builder()
                .text(chunkText)
                .documentId(document.getId())
                .chunkIndex(chunkIndex)
                .startPosition(startPos)
                .endPosition(endPos)
                .metadata(new HashMap<>(document.getMetadata()))
                .build();
    }
}
