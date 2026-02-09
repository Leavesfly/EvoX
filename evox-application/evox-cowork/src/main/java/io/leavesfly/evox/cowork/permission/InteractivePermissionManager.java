package io.leavesfly.evox.cowork.permission;

import io.leavesfly.evox.cowork.config.CoworkConfig;
import io.leavesfly.evox.cowork.event.CoworkEventBus;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Slf4j
@Data
public class InteractivePermissionManager {
    
    private final CoworkPermissionManager delegateManager;
    private final Map<String, PermissionRequest> pendingRequests;
    private final CoworkEventBus eventBus;
    private volatile String currentSessionId;
    private final long requestTimeout;
    private Consumer<PermissionRequest> uiPermissionCallback;
    
    public InteractivePermissionManager(CoworkConfig config, CoworkEventBus eventBus) {
        this.eventBus = eventBus;
        this.pendingRequests = new ConcurrentHashMap<>();
        this.requestTimeout = 300000L;
        this.delegateManager = new CoworkPermissionManager(config, this::handlePermissionRequest);
    }
    
    public boolean checkPermission(String toolName, Map<String, Object> parameters) {
        return delegateManager.checkPermission(toolName, parameters);
    }
    
    public CoworkPermissionManager getPermissionManager() {
        return delegateManager;
    }
    
    private boolean handlePermissionRequest(String toolName, Map<String, Object> parameters) {
        PermissionRequest request = new PermissionRequest(currentSessionId, toolName, parameters);
        pendingRequests.put(request.getRequestId(), request);
        
        eventBus.emitPermissionRequest(
            currentSessionId,
            request.getRequestId(),
            toolName,
            parameters
        );

        if (uiPermissionCallback != null) {
            uiPermissionCallback.accept(request);
        }
        
        long startTime = System.currentTimeMillis();
        long checkInterval = 200L;
        
        while (request.isPending()) {
            try {
                Thread.sleep(checkInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Permission request check interrupted");
                break;
            }
            
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed >= requestTimeout) {
                log.warn("Permission request timed out for tool: {}", toolName);
                request.reject();
            }
        }
        
        return request.getStatus() == PermissionRequest.PermissionStatus.APPROVED;
    }
    
    public boolean replyPermission(String requestId, PermissionRequest.PermissionReply reply) {
        PermissionRequest request = pendingRequests.get(requestId);
        
        if (request == null || !request.isPending()) {
            log.warn("Invalid permission request: {}", requestId);
            return false;
        }
        
        switch (reply) {
            case ONCE:
                request.approve(PermissionRequest.PermissionReply.ONCE);
                break;
            case ALWAYS:
                request.approve(PermissionRequest.PermissionReply.ALWAYS);
                delegateManager.approveToolForSession(request.getToolName());
                break;
            case REJECT:
                request.reject();
                break;
        }
        
        eventBus.emitSessionUpdate(
            currentSessionId,
            "permission_reply",
            Map.of(
                "requestId", requestId,
                "toolName", request.getToolName(),
                "status", request.getStatus().name(),
                "reply", reply.name()
            )
        );
        
        return true;
    }
    
    public List<PermissionRequest> getPendingRequests() {
        List<PermissionRequest> pending = new ArrayList<>();
        for (PermissionRequest request : pendingRequests.values()) {
            if (request.isPending()) {
                pending.add(request);
            }
        }
        return pending;
    }
    
    public void setCurrentSessionId(String sessionId) {
        this.currentSessionId = sessionId;
    }
}
