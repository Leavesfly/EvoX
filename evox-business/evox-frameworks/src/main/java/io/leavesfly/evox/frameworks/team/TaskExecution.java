package io.leavesfly.evox.frameworks.team;

import lombok.Data;

/**
 * 任务执行记录
 *
 * @param <T> 结果类型
 * @author EvoX Team
 */
@Data
public class TaskExecution<T> {

    /**
     * 成员ID
     */
    private String memberId;

    /**
     * 任务
     */
    private String task;

    /**
     * 结果
     */
    private T result;

    /**
     * 耗时(毫秒)
     */
    private long duration;

    /**
     * 时间戳
     */
    private long timestamp;

    public TaskExecution(String memberId, String task, T result, long duration, long timestamp) {
        this.memberId = memberId;
        this.task = task;
        this.result = result;
        this.duration = duration;
        this.timestamp = timestamp;
    }
}
