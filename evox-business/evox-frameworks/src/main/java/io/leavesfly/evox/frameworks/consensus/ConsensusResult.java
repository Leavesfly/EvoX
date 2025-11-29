package io.leavesfly.evox.frameworks.consensus;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 共识结果
 *
 * @param <T> 结果类型
 * @author EvoX Team
 */
@Data
@Builder
public class ConsensusResult<T> {

    /**
     * 是否达成共识
     */
    private boolean reached;

    /**
     * 共识结果
     */
    private T result;

    /**
     * 置信度
     */
    private double confidence;

    /**
     * 使用的轮数
     */
    private int rounds;

    /**
     * 耗时(毫秒)
     */
    private long duration;

    /**
     * 历史记录
     */
    private List<ConsensusRecord<T>> history;

    /**
     * 元数据
     */
    private Map<String, Object> metadata;
}
