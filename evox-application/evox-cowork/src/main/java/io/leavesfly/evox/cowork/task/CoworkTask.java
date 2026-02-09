package io.leavesfly.evox.cowork.task;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
public class CoworkTask {
    private String taskId;
    private String parentTaskId;
    private String description;
    private String prompt;
    private TaskStatus status;
    private int priority;
    private String result;
    private String errorMessage;
    private List<CoworkTask> subTasks;
    private long createdAt;
    private long completedAt;
    private Map<String, Object> metadata;

    public CoworkTask() {
        this.taskId = UUID.randomUUID().toString();
        this.status = TaskStatus.PENDING;
        this.priority = 0;
        this.subTasks = new ArrayList<>();
        this.createdAt = System.currentTimeMillis();
        this.metadata = new HashMap<>();
    }

    public static CoworkTask of(String description, String prompt) {
        CoworkTask task = new CoworkTask();
        task.setDescription(description);
        task.setPrompt(prompt);
        return task;
    }

    public static CoworkTask ofSubTask(String parentTaskId, String description, String prompt) {
        CoworkTask task = new CoworkTask();
        task.setParentTaskId(parentTaskId);
        task.setDescription(description);
        task.setPrompt(prompt);
        return task;
    }

    public void markInProgress() {
        this.status = TaskStatus.IN_PROGRESS;
    }

    public void markCompleted(String result) {
        this.status = TaskStatus.COMPLETED;
        this.result = result;
        this.completedAt = System.currentTimeMillis();
    }

    public void markFailed(String errorMessage) {
        this.status = TaskStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = System.currentTimeMillis();
    }

    public void markCancelled() {
        this.status = TaskStatus.CANCELLED;
        this.completedAt = System.currentTimeMillis();
    }

    public void addSubTask(CoworkTask subTask) {
        this.subTasks.add(subTask);
    }

    public long getDurationMs() {
        if (completedAt > 0) {
            return completedAt - createdAt;
        }
        return System.currentTimeMillis() - createdAt;
    }

    public boolean isTerminal() {
        return status == TaskStatus.COMPLETED || status == TaskStatus.FAILED || status == TaskStatus.CANCELLED;
    }

    public enum TaskStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED,
        CANCELLED
    }
}
