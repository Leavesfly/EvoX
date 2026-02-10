package io.leavesfly.evox.scheduler.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskContext {
    private String taskId;
    private String userId;
    private Instant scheduledTime;
    private Instant actualFireTime;
    private int executionCount;
    private TaskResult lastResult;

    @Builder.Default
    private Map<String, Object> parameters = new HashMap<>();

    public Object getParameter(String key) {
        return parameters.get(key);
    }

    public void setParameter(String key, Object value) {
        parameters.put(key, value);
    }
}
