package io.leavesfly.evox.scheduler.push;

import io.leavesfly.evox.scheduler.core.TaskResult;

public interface IPushService {

    void push(String targetId, String message);

    void pushTaskResult(String targetId, String taskName, TaskResult result);
}
