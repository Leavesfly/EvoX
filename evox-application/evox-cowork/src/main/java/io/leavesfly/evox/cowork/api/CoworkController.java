package io.leavesfly.evox.cowork.api;

import io.leavesfly.evox.cowork.config.CoworkConfig;
import io.leavesfly.evox.cowork.event.CoworkEventBus;
import io.leavesfly.evox.cowork.permission.InteractivePermissionManager;
import io.leavesfly.evox.cowork.session.SessionManager;
import io.leavesfly.evox.cowork.task.TaskManager;
import io.leavesfly.evox.cowork.plugin.PluginManager;
import io.leavesfly.evox.cowork.connector.ConnectorManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;

/**
 * Cowork REST API Controller - System-level endpoints.
 *
 * Domain-specific endpoints have been moved to dedicated controllers:
 * - {@link SessionController} - Session management
 * - {@link PermissionController} - Permission management
 * - {@link TaskController} - Task management
 * - {@link WorkspaceController} - Workspace management
 * - {@link TemplateController} - Template management
 * - {@link PluginController} - Plugin management
 * - {@link ConnectorController} - Connector management
 */
@Slf4j
@RestController
@RequestMapping("/api/cowork")
@RequiredArgsConstructor
public class CoworkController {

    private final CoworkConfig config;
    private final CoworkEventBus eventBus;
    private final SessionManager sessionManager;
    private final TaskManager taskManager;
    private final PluginManager pluginManager;
    private final ConnectorManager connectorManager;
    private final InteractivePermissionManager permissionManager;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "version", "1.0.0-SNAPSHOT",
                "uptime", System.currentTimeMillis()
        ));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "running");
        status.put("sessions", sessionManager.listSessions().size());
        status.put("activeSession", sessionManager.getActiveSessionId());
        status.put("activeTasks", taskManager.getActiveTasks().size());
        status.put("plugins", pluginManager.getEnabledPlugins().size());
        status.put("connectors", connectorManager.getConnectedConnectors().size());
        status.put("sseConnections", eventBus.getActiveConnectionCount());
        status.put("pendingPermissions", permissionManager.getPendingRequests().size());
        return ResponseEntity.ok(status);
    }

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeEvents() {
        log.info("New SSE client connected");
        return eventBus.subscribe();
    }

    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfig() {
        Map<String, Object> configInfo = new HashMap<>();
        configInfo.put("workingDirectory", config.getWorkingDirectory());
        configInfo.put("maxIterations", config.getMaxIterations());
        configInfo.put("requireApproval", config.isRequireApproval());
        configInfo.put("sandboxEnabled", config.isSandboxEnabled());
        configInfo.put("deleteProtectionEnabled", config.isDeleteProtectionEnabled());
        configInfo.put("streamingEnabled", config.isStreamingEnabled());
        configInfo.put("autoApprovedTools", config.getAutoApprovedTools());
        configInfo.put("approvalRequiredTools", config.getApprovalRequiredTools());
        return ResponseEntity.ok(configInfo);
    }

    @DeleteMapping("/history")
    public ResponseEntity<Map<String, String>> clearHistory() {
        taskManager.clearHistory();
        return ResponseEntity.ok(Map.of("message", "History cleared"));
    }
}
