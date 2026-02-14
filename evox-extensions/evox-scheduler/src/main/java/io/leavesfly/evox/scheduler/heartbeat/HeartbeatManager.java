package io.leavesfly.evox.scheduler.heartbeat;

import io.leavesfly.evox.scheduler.core.IScheduledTask;
import io.leavesfly.evox.scheduler.core.TaskContext;
import io.leavesfly.evox.scheduler.core.TaskResult;
import io.leavesfly.evox.scheduler.core.TaskScheduler;
import io.leavesfly.evox.scheduler.trigger.HeartbeatTrigger;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class HeartbeatManager {
    
    private final TaskScheduler taskScheduler;
    private final Map<String, IScheduledTask> heartbeatTasks;
    
    public HeartbeatManager(TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
        this.heartbeatTasks = new ConcurrentHashMap<>();
    }
    
    public void registerHeartbeat(String name, long intervalMs, Runnable action) {
        registerHeartbeat(name, intervalMs, null, action);
    }
    
    public void registerHeartbeat(String name, long intervalMs, Supplier<Boolean> condition, Runnable action) {
        if (heartbeatTasks.containsKey(name)) {
            throw new IllegalArgumentException("Heartbeat task '" + name + "' already registered");
        }
        
        HeartbeatTrigger trigger = new HeartbeatTrigger(intervalMs, condition);
        
        IScheduledTask heartbeatTask = new IScheduledTask() {
            @Override
            public String getTaskId() {
                return "heartbeat-" + name;
            }
            
            @Override
            public String getTaskName() {
                return name;
            }
            
            @Override
            public String getDescription() {
                return "Heartbeat task for " + name;
            }
            
            @Override
            public HeartbeatTrigger getTrigger() {
                return trigger;
            }
            
            @Override
            public TaskResult execute(TaskContext context) {
                action.run();
                return TaskResult.success("Heartbeat executed: " + name);
            }
            
            @Override
            public boolean isEnabled() {
                return true;
            }
        };
        
        taskScheduler.scheduleTask(heartbeatTask);
        heartbeatTasks.put(name, heartbeatTask);
    }
    
    public void unregisterHeartbeat(String name) {
        IScheduledTask task = heartbeatTasks.remove(name);
        if (task != null) {
            taskScheduler.cancelTask(task.getTaskId());
        }
    }
    
    public Set<String> getActiveHeartbeats() {
        return heartbeatTasks.keySet();
    }
    
    public void start() {
        taskScheduler.start();
    }
    
    public void shutdown() {
        for (String name : heartbeatTasks.keySet()) {
            unregisterHeartbeat(name);
        }
        taskScheduler.shutdown();
    }
}
