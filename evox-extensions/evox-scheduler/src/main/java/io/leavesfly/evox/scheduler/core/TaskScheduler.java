package io.leavesfly.evox.scheduler.core;

import io.leavesfly.evox.scheduler.trigger.ITrigger;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
public class TaskScheduler {

    private final Map<String, IScheduledTask> tasks = new ConcurrentHashMap<>();
    private final Map<String, TaskStatus> taskStatuses = new ConcurrentHashMap<>();
    private final Map<String, TaskResult> lastResults = new ConcurrentHashMap<>();
    private final Map<String, Integer> executionCounts = new ConcurrentHashMap<>();

    private ScheduledExecutorService schedulerExecutor;
    private ExecutorService taskExecutor;
    private volatile boolean running = false;
    private final long checkIntervalMs;

    public TaskScheduler() {
        this(1000);
    }

    public TaskScheduler(long checkIntervalMs) {
        this.checkIntervalMs = checkIntervalMs;
    }

    public void scheduleTask(IScheduledTask task) {
        String taskId = task.getTaskId();
        if (tasks.containsKey(taskId)) {
            throw new IllegalArgumentException("Task already scheduled: " + taskId);
        }
        tasks.put(taskId, task);
        taskStatuses.put(taskId, TaskStatus.SCHEDULED);
        executionCounts.put(taskId, 0);
        log.info("Task scheduled: {} ({}), trigger: {}",
                taskId, task.getTaskName(), task.getTrigger().getType());
    }

    public void cancelTask(String taskId) {
        IScheduledTask removed = tasks.remove(taskId);
        if (removed != null) {
            taskStatuses.put(taskId, TaskStatus.CANCELLED);
            log.info("Task cancelled: {}", taskId);
        }
    }

    public void pauseTask(String taskId) {
        if (tasks.containsKey(taskId)) {
            taskStatuses.put(taskId, TaskStatus.PAUSED);
            log.info("Task paused: {}", taskId);
        }
    }

    public void resumeTask(String taskId) {
        if (tasks.containsKey(taskId)) {
            taskStatuses.put(taskId, TaskStatus.SCHEDULED);
            log.info("Task resumed: {}", taskId);
        }
    }

    public TaskResult executeNow(String taskId) {
        IScheduledTask task = tasks.get(taskId);
        if (task == null) {
            throw new IllegalArgumentException("Task not found: " + taskId);
        }
        return executeTask(task);
    }

    public void start() {
        if (running) {
            log.warn("TaskScheduler is already running");
            return;
        }
        running = true;
        schedulerExecutor = Executors.newSingleThreadScheduledExecutor(
                r -> new Thread(r, "evox-scheduler"));
        taskExecutor = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors(),
                r -> {
                    Thread t = new Thread(r, "evox-task-worker");
                    t.setDaemon(true);
                    return t;
                });

        schedulerExecutor.scheduleWithFixedDelay(
                this::checkAndFireTasks, 0, checkIntervalMs, TimeUnit.MILLISECONDS);
        log.info("TaskScheduler started with {} tasks", tasks.size());
    }

    public void shutdown() {
        running = false;
        if (schedulerExecutor != null) {
            schedulerExecutor.shutdown();
            try {
                if (!schedulerExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    schedulerExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                schedulerExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        if (taskExecutor != null) {
            taskExecutor.shutdown();
            try {
                if (!taskExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    taskExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                taskExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        log.info("TaskScheduler shutdown");
    }

    public TaskStatus getTaskStatus(String taskId) {
        return taskStatuses.get(taskId);
    }

    public TaskResult getLastResult(String taskId) {
        return lastResults.get(taskId);
    }

    public int getTaskCount() {
        return tasks.size();
    }

    public List<TaskInfo> getAllTaskInfo() {
        List<TaskInfo> infos = new ArrayList<>();
        for (Map.Entry<String, IScheduledTask> entry : tasks.entrySet()) {
            String taskId = entry.getKey();
            IScheduledTask task = entry.getValue();
            infos.add(TaskInfo.builder()
                    .taskId(taskId)
                    .taskName(task.getTaskName())
                    .description(task.getDescription())
                    .triggerType(task.getTrigger().getType())
                    .status(taskStatuses.getOrDefault(taskId, TaskStatus.CREATED))
                    .nextFireTime(task.getTrigger().getNextFireTime())
                    .executionCount(executionCounts.getOrDefault(taskId, 0))
                    .lastResult(lastResults.get(taskId))
                    .enabled(task.isEnabled())
                    .build());
        }
        return infos;
    }

    private void checkAndFireTasks() {
        if (!running) {
            return;
        }
        for (Map.Entry<String, IScheduledTask> entry : tasks.entrySet()) {
            String taskId = entry.getKey();
            IScheduledTask task = entry.getValue();

            TaskStatus status = taskStatuses.get(taskId);
            if (status != TaskStatus.SCHEDULED || !task.isEnabled()) {
                continue;
            }

            ITrigger trigger = task.getTrigger();
            if (trigger.shouldFire()) {
                taskExecutor.submit(() -> {
                    TaskResult result = executeTask(task);
                    trigger.onFired();

                    if (!trigger.isRepeating()) {
                        taskStatuses.put(taskId, TaskStatus.COMPLETED);
                    }
                });
            }
        }
    }

    private TaskResult executeTask(IScheduledTask task) {
        String taskId = task.getTaskId();
        taskStatuses.put(taskId, TaskStatus.RUNNING);

        int count = executionCounts.getOrDefault(taskId, 0) + 1;
        executionCounts.put(taskId, count);

        TaskContext context = TaskContext.builder()
                .taskId(taskId)
                .scheduledTime(task.getTrigger().getNextFireTime())
                .actualFireTime(Instant.now())
                .executionCount(count)
                .lastResult(lastResults.get(taskId))
                .build();

        try {
            TaskResult result = task.execute(context);
            lastResults.put(taskId, result);

            if (task.getTrigger().isRepeating() && task.isEnabled()) {
                taskStatuses.put(taskId, TaskStatus.SCHEDULED);
            } else {
                taskStatuses.put(taskId, TaskStatus.COMPLETED);
            }

            return result;
        } catch (Exception e) {
            log.error("Task execution failed: {}", taskId, e);
            TaskResult failResult = TaskResult.failure(e.getMessage());
            lastResults.put(taskId, failResult);
            taskStatuses.put(taskId, TaskStatus.FAILED);
            return failResult;
        }
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class TaskInfo {
        private String taskId;
        private String taskName;
        private String description;
        private String triggerType;
        private TaskStatus status;
        private Instant nextFireTime;
        private int executionCount;
        private TaskResult lastResult;
        private boolean enabled;
    }
}
