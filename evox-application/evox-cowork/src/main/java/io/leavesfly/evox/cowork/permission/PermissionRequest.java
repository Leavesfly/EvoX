package io.leavesfly.evox.cowork.permission;

import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
public class PermissionRequest {
    
    private final String requestId;
    private final String sessionId;
    private final String toolName;
    private final Map<String, Object> parameters;
    private PermissionStatus status;
    private final long createdAt;
    private long resolvedAt;
    private PermissionReply reply;
    
    public PermissionRequest(String sessionId, String toolName, Map<String, Object> parameters) {
        this.requestId = UUID.randomUUID().toString();
        this.sessionId = sessionId;
        this.toolName = toolName;
        this.parameters = parameters;
        this.status = PermissionStatus.PENDING;
        this.createdAt = System.currentTimeMillis();
        this.resolvedAt = 0L;
        this.reply = null;
    }
    
    public void approve(PermissionReply reply) {
        this.status = PermissionStatus.APPROVED;
        this.reply = reply;
        this.resolvedAt = System.currentTimeMillis();
    }
    
    public void reject() {
        this.status = PermissionStatus.REJECTED;
        this.reply = PermissionReply.REJECT;
        this.resolvedAt = System.currentTimeMillis();
    }
    
    public boolean isPending() {
        return this.status == PermissionStatus.PENDING;
    }
    
    public enum PermissionStatus {
        PENDING,
        APPROVED,
        REJECTED
    }
    
    public enum PermissionReply {
        ONCE,
        ALWAYS,
        REJECT
    }
}
