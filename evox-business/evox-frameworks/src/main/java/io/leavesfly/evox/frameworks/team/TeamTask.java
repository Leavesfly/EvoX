package io.leavesfly.evox.frameworks.team;

import lombok.Data;

/**
 * 任务
 *
 * @param <T> 任务结果类型
 * @author EvoX Team
 */
@Data
public class TeamTask<T> {

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 任务描述
     */
    private String description;

    /**
     * 所需技能
     */
    private String[] requiredSkills;

    /**
     * 优先级
     */
    private int priority;

    /**
     * 任务状态
     */
    private TaskStatus status;

    public TeamTask(String taskId, String description) {
        this.taskId = taskId;
        this.description = description;
        this.status = TaskStatus.PENDING;
        this.priority = 0;
    }

    /**
     * 任务状态
     */
    public enum TaskStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED
    }
}
