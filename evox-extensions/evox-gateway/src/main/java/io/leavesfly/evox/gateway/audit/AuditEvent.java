package io.leavesfly.evox.gateway.audit;

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
public class AuditEvent {
    private String eventId;
    private String userId;
    private String channelId;
    private String action;
    private String input;
    private String output;
    private boolean success;
    private long durationMs;
    private Instant timestamp;
    private Map<String, Object> metadata;
}
