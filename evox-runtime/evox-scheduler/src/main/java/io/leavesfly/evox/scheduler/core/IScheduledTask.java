package io.leavesfly.evox.scheduler.core;

import io.leavesfly.evox.scheduler.trigger.ITrigger;

public interface IScheduledTask {

    String getTaskId();

    String getTaskName();

    TaskResult execute(TaskContext context);

    ITrigger getTrigger();

    boolean isEnabled();

    default String getDescription() {
        return getTaskName();
    }
}
