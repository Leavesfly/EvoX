package io.leavesfly.evox.cowork.task;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

@Slf4j
@Data
public class TaskManager {
    private final TaskDecomposer decomposer;
    private final ConcurrentHashMap<String, CoworkTask> taskMap;
    private final ConcurrentLinkedQueue<CoworkTask> taskQueue;
    private final List<CoworkTask> executionHistory;

    public TaskManager(TaskDecomposer decomposer) {
        this.decomposer = decomposer;
        this.taskMap = new ConcurrentHashMap<>();
        this.taskQueue = new ConcurrentLinkedQueue<>();
        this.executionHistory = Collections.synchronizedList(new ArrayList<>());
    }

    // 提交任务
    public void submitTask(CoworkTask task) {
        taskMap.put(task.getTaskId(), task);
        taskQueue.add(task);
        log.info("Submitted task: {} - {}", task.getTaskId(), task.getDescription());
    }

    public CoworkTask getTask(String taskId) {
        return taskMap.get(taskId);
    }

    public List<CoworkTask> getAllTasks() {
        return new ArrayList<>(taskMap.values());
    }

    // 获取活跃任务（Pending 或 InProgress）
    public List<CoworkTask> getActiveTasks() {
        return taskMap.values().stream()
            .filter(task -> task.getStatus() == CoworkTask.TaskStatus.PENDING || 
                          task.getStatus() == CoworkTask.TaskStatus.IN_PROGRESS)
            .collect(Collectors.toList());
    }

    // 取消任务
    public void cancelTask(String taskId) {
        CoworkTask task = getTask(taskId);
        if (task != null) {
            task.markCancelled();
            log.info("Cancelled task: {} - {}", taskId, task.getDescription());
        }
    }

    // 分解任务并提交子任务
    public CoworkTask decomposeAndSubmit(String description, String prompt) {
        CoworkTask parentTask = CoworkTask.of(description, prompt);
        List<CoworkTask> subtasks = decomposer.decompose(description);

        if (subtasks.size() > 1) {
            for (CoworkTask subtask : subtasks) {
                subtask.setParentTaskId(parentTask.getTaskId());
                parentTask.addSubTask(subtask);
                submitTask(subtask);
            }
            log.info("Decomposed task into {} subtasks", subtasks.size());
        }

        submitTask(parentTask);
        return parentTask;
    }

    public List<CoworkTask> getTaskHistory() {
        synchronized (executionHistory) {
            return new ArrayList<>(executionHistory);
        }
    }

    // 标记任务完成
    public void completeTask(String taskId, String result) {
        CoworkTask task = getTask(taskId);
        if (task != null) {
            task.markCompleted(result);
            synchronized (executionHistory) {
                executionHistory.add(task);
            }
            log.info("Completed task: {} - {}", taskId, task.getDescription());
        }
    }

    // 标记任务失败
    public void failTask(String taskId, String errorMessage) {
        CoworkTask task = getTask(taskId);
        if (task != null) {
            task.markFailed(errorMessage);
            synchronized (executionHistory) {
                executionHistory.add(task);
            }
            log.warn("Failed task: {} - {}: {}", taskId, task.getDescription(), errorMessage);
        }
    }

    // 清除任务历史
    public void clearHistory() {
        synchronized (executionHistory) {
            executionHistory.clear();
        }
        log.info("Cleared task execution history");
    }
}