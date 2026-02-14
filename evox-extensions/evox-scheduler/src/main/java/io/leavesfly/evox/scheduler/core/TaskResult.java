package io.leavesfly.evox.scheduler.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResult {
    private boolean success;
    private Object data;
    private String error;
    private Instant executedAt;
    private long durationMs;
    private Map<String, Object> metadata;

    public static TaskResult success(Object data) {
        return TaskResult.builder()
                .success(true)
                .data(data)
                .executedAt(Instant.now())
                .build();
    }

    public static TaskResult success(Object data, long durationMs) {
        return TaskResult.builder()
                .success(true)
                .data(data)
                .executedAt(Instant.now())
                .durationMs(durationMs)
                .build();
    }

    public static TaskResult failure(String error) {
        return TaskResult.builder()
                .success(false)
                .error(error)
                .executedAt(Instant.now())
                .build();
    }

    public static TaskResult failure(String error, long durationMs) {
        return TaskResult.builder()
                .success(false)
                .error(error)
                .executedAt(Instant.now())
                .durationMs(durationMs)
                .build();
    }
}
