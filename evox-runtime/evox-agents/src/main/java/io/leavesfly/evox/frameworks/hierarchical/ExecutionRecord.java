package io.leavesfly.evox.frameworks.hierarchical;

import lombok.Data;

/**
 * 执行记录
 *
 * @param <T> 决策结果类型
 * @author EvoX Team
 */
@Data
public class ExecutionRecord<T> {

    /**
     * 层级ID
     */
    private String layerId;

    /**
     * 任务
     */
    private String task;

    /**
     * 决策结果
     */
    private LayerDecision<T> decision;

    /**
     * 耗时(毫秒)
     */
    private long duration;

    /**
     * 时间戳
     */
    private long timestamp;

    public ExecutionRecord(String layerId, String task, LayerDecision<T> decision, 
                          long duration, long timestamp) {
        this.layerId = layerId;
        this.task = task;
        this.decision = decision;
        this.duration = duration;
        this.timestamp = timestamp;
    }
}
