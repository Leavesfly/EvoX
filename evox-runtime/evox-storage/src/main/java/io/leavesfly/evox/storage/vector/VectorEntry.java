package io.leavesfly.evox.storage.vector;

import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 通用向量条目定义。
 * <p>
 * 在 InMemory、Persistent、Chroma 等向量存储实现中复用，
 * 用于统一向量的 id、向量值和元数据结构。
 * </p>
 */
@Data
public class VectorEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 向量唯一标识 */
    private final String id;

    /** 向量数据 */
    private final float[] vector;

    /** 向量元数据 */
    private final Map<String, Object> metadata;

    public VectorEntry(String id, float[] vector, Map<String, Object> metadata) {
        this.id = id;
        this.vector = vector;
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }
}
