package io.leavesfly.evox.frameworks.consensus;

import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 共识评估结果
 *
 * @param <T> 提议的类型
 * @author EvoX Team
 */
@Data
@Builder
public class ConsensusEvaluation<T> {

    /**
     * 是否达成共识
     */
    private boolean consensusReached;

    /**
     * 共识值(达成共识时的结果)
     */
    private T consensusValue;

    /**
     * 置信度 (0.0 - 1.0)
     */
    private double confidence;

    /**
     * 支持度(支持该结果的智能体比例)
     */
    private double supportRate;

    /**
     * 元数据(策略特定的额外信息)
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * 添加元数据
     */
    public void addMetadata(String key, Object value) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put(key, value);
    }

    /**
     * 获取元数据
     */
    public Object getMetadata(String key) {
        return metadata != null ? metadata.get(key) : null;
    }
}
