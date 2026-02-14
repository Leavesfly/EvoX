package io.leavesfly.evox.scheduler.task;

import io.leavesfly.evox.core.agent.IAgent;
import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.scheduler.core.IScheduledTask;
import io.leavesfly.evox.scheduler.core.TaskContext;
import io.leavesfly.evox.scheduler.core.TaskResult;
import io.leavesfly.evox.scheduler.trigger.ITrigger;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;

@Slf4j
@Getter
@Builder
public class AgentTask implements IScheduledTask {

    private final String taskId;
    private final String taskName;
    private final String description;
    private final IAgent agent;
    private final String actionName;
    private final String prompt;
    private final ITrigger trigger;

    @Builder.Default
    private boolean enabled = true;

    @Builder.Default
    private TaskResultCallback resultCallback = null;

    @Override
    public TaskResult execute(TaskContext context) {
        long startTime = System.currentTimeMillis();
        try {
            Message input = Message.inputMessage(prompt);
            Message result = agent.execute(actionName, Collections.singletonList(input));

            long duration = System.currentTimeMillis() - startTime;
            TaskResult taskResult = TaskResult.success(result, duration);

            if (resultCallback != null) {
                resultCallback.onResult(taskId, taskResult);
            }

            log.info("AgentTask [{}] completed in {}ms", taskId, duration);
            return taskResult;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("AgentTask [{}] failed after {}ms", taskId, duration, e);
            TaskResult taskResult = TaskResult.failure(e.getMessage(), duration);

            if (resultCallback != null) {
                resultCallback.onResult(taskId, taskResult);
            }

            return taskResult;
        }
    }

    @FunctionalInterface
    public interface TaskResultCallback {
        void onResult(String taskId, TaskResult result);
    }
}
