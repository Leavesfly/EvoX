package io.leavesfly.evox.cowork.permission;

import io.leavesfly.evox.cowork.config.CoworkConfig;
import io.leavesfly.evox.cowork.event.CoworkEventBus;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

@Slf4j
@Data
public class InteractivePermissionManager {
    
    private final CoworkPermissionManager delegateManager;
    private final Map<String, PermissionRequest> pendingRequests;
    private final Map<String, CompletableFuture<Boolean>> pendingFutures;
    private final CoworkEventBus eventBus;
    private volatile String currentSessionId;
    private final long requestTimeout;
    private Consumer<PermissionRequest> uiPermissionCallback;
    
    public InteractivePermissionManager(CoworkConfig config, CoworkEventBus eventBus) {
        this.eventBus = eventBus;
        this.pendingRequests = new ConcurrentHashMap<>();
        this.pendingFutures = new ConcurrentHashMap<>();
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
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        pendingRequests.put(request.getRequestId(), request);
        pendingFutures.put(request.getRequestId(), future);

        eventBus.emitPermissionRequest(
            currentSessionId,
            request.getRequestId(),
            toolName,
            parameters
        );

        if (uiPermissionCallback != null) {
            uiPermissionCallback.accept(request);
        }

        try {
            return future.get(requestTimeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            log.warn("Permission request timed out for tool: {}", toolName);
            request.reject();
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Permission request interrupted for tool: {}", toolName);
            request.reject();
            return false;
        } catch (Exception e) {
            log.error("Error waiting for permission approval", e);
            request.reject();
            return false;
        } finally {
            pendingFutures.remove(request.getRequestId());
        }
    }
    
    public boolean replyPermission(String requestId, PermissionRequest.PermissionReply reply) {
        PermissionRequest request = pendingRequests.get(requestId);

        if (request == null || !request.isPending()) {
            log.warn("Invalid permission request: {}", requestId);
            return false;
        }

        boolean approved;
        switch (reply) {
            case ONCE:
                request.approve(PermissionRequest.PermissionReply.ONCE);
                approved = true;
                break;
            case ALWAYS:
                request.approve(PermissionRequest.PermissionReply.ALWAYS);
                delegateManager.approveToolForSession(request.getToolName());
                approved = true;
                break;
            case REJECT:
                request.reject();
                approved = false;
                break;
            default:
                approved = false;
        }

        CompletableFuture<Boolean> future = pendingFutures.remove(requestId);
        if (future != null) {
            future.complete(approved);
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
