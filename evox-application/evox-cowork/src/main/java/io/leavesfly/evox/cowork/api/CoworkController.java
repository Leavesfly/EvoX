package io.leavesfly.evox.cowork.api;

import io.leavesfly.evox.cowork.config.CoworkConfig;
import io.leavesfly.evox.cowork.connector.ConnectorManager;
import io.leavesfly.evox.cowork.event.CoworkEventBus;
import io.leavesfly.evox.cowork.permission.InteractivePermissionManager;
import io.leavesfly.evox.cowork.permission.PermissionRequest;
import io.leavesfly.evox.cowork.plugin.CoworkPlugin;
import io.leavesfly.evox.cowork.plugin.PluginManager;
import io.leavesfly.evox.cowork.session.CoworkSession;
import io.leavesfly.evox.cowork.session.SessionManager;
import io.leavesfly.evox.cowork.task.CoworkTask;
import io.leavesfly.evox.cowork.task.TaskDecomposer;
import io.leavesfly.evox.cowork.task.TaskManager;
import io.leavesfly.evox.cowork.template.TemplateManager;
import io.leavesfly.evox.cowork.template.WorkflowTemplate;
import io.leavesfly.evox.cowork.workspace.Workspace;
import io.leavesfly.evox.cowork.workspace.WorkspaceManager;
import io.leavesfly.evox.models.factory.LLMFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Cowork REST API Controller - Desktop Application Backend.
 *
 * Provides a complete API surface for the Cowork desktop application,
 * modeled after OpenWork's architecture with Session-based task execution,
 * SSE real-time events, interactive permission approval, workspace management,
 * and workflow templates.
 */
@Slf4j
@RestController
@RequestMapping("/api/cowork")
public class CoworkController {

    private final CoworkConfig config;
    private final CoworkEventBus eventBus;
    private final InteractivePermissionManager permissionManager;
    private final SessionManager sessionManager;
    private final TaskManager taskManager;
    private final PluginManager pluginManager;
    private final ConnectorManager connectorManager;
    private final TemplateManager templateManager;
    private final WorkspaceManager workspaceManager;

    public CoworkController() {
        this.config = CoworkConfig.createDefault(System.getProperty("user.dir"));

        this.eventBus = new CoworkEventBus();
        this.permissionManager = new InteractivePermissionManager(config, eventBus);

        this.sessionManager = new SessionManager(config, permissionManager.getPermissionManager());
        this.sessionManager.setEventCallback(sessionEvent -> {
            eventBus.emitSessionUpdate(
                sessionEvent.sessionId(),
                sessionEvent.type().name(),
                sessionEvent.data()
            );
        });

        this.taskManager = new TaskManager(
            new TaskDecomposer(LLMFactory.create(config.getLlmConfig()))
        );
        this.pluginManager = new PluginManager(config.getPluginDirectory());
        this.connectorManager = new ConnectorManager(config.getNetworkAllowlist());
        this.templateManager = new TemplateManager(
            System.getProperty("user.home") + "/.evox/cowork/templates"
        );
        this.workspaceManager = new WorkspaceManager(
            System.getProperty("user.home") + "/.evox/cowork"
        );

        log.info("Cowork API initialized with working directory: {}", config.getWorkingDirectory());
    }

    // ==================== Health & Status ====================

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

    // ==================== SSE Event Stream ====================

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeEvents() {
        log.info("New SSE client connected");
        return eventBus.subscribe();
    }

    // ==================== Session API ====================

    @PostMapping("/sessions")
    public ResponseEntity<Map<String, Object>> createSession(@RequestBody(required = false) Map<String, String> request) {
        String workingDirectory = request != null ? request.get("workingDirectory") : null;
        CoworkSession session = sessionManager.createSession(workingDirectory);
        permissionManager.setCurrentSessionId(session.getSessionId());
        return ResponseEntity.ok(session.toSummary());
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<Map<String, Object>>> listSessions() {
        List<Map<String, Object>> sessionSummaries = sessionManager.listSessions().stream()
            .map(CoworkSession::toSummary)
            .collect(Collectors.toList());
        return ResponseEntity.ok(sessionSummaries);
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<Map<String, Object>> getSession(@PathVariable String sessionId) {
        CoworkSession session = sessionManager.getSession(sessionId);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(session.toSummary());
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<List<CoworkSession.SessionMessage>> getSessionMessages(@PathVariable String sessionId) {
        try {
            List<CoworkSession.SessionMessage> messages = sessionManager.getMessages(sessionId);
            return ResponseEntity.ok(messages);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/sessions/{sessionId}/prompt")
    public ResponseEntity<Map<String, Object>> prompt(
            @PathVariable String sessionId,
            @RequestBody Map<String, String> request) {
        String message = request.get("message");
        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Message is required"));
        }

        try {
            permissionManager.setCurrentSessionId(sessionId);
            String response = sessionManager.prompt(sessionId, message);
            Map<String, Object> result = new HashMap<>();
            result.put("response", response);
            result.put("sessionId", sessionId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error processing prompt for session {}", sessionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/sessions/{sessionId}/abort")
    public ResponseEntity<Map<String, String>> abortSession(@PathVariable String sessionId) {
        try {
            sessionManager.abortSession(sessionId);
            return ResponseEntity.ok(Map.of("message", "Session aborted: " + sessionId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/sessions/{sessionId}/summarize")
    public ResponseEntity<Map<String, String>> summarizeSession(@PathVariable String sessionId) {
        try {
            String summary = sessionManager.summarizeSession(sessionId);
            return ResponseEntity.ok(Map.of("summary", summary));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Map<String, String>> deleteSession(@PathVariable String sessionId) {
        try {
            sessionManager.deleteSession(sessionId);
            return ResponseEntity.ok(Map.of("message", "Session deleted: " + sessionId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/sessions/{sessionId}/switch")
    public ResponseEntity<Map<String, String>> switchSession(@PathVariable String sessionId) {
        try {
            sessionManager.switchSession(sessionId);
            permissionManager.setCurrentSessionId(sessionId);
            return ResponseEntity.ok(Map.of("message", "Switched to session: " + sessionId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ==================== Permission API ====================

    @GetMapping("/permissions/pending")
    public ResponseEntity<List<PermissionRequest>> getPendingPermissions() {
        return ResponseEntity.ok(permissionManager.getPendingRequests());
    }

    @PostMapping("/permissions/{requestId}/reply")
    public ResponseEntity<Map<String, Object>> replyPermission(
            @PathVariable String requestId,
            @RequestBody Map<String, String> request) {
        String replyStr = request.get("reply");
        if (replyStr == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Reply is required (once/always/reject)"));
        }

        PermissionRequest.PermissionReply reply;
        try {
            reply = PermissionRequest.PermissionReply.valueOf(replyStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid reply. Must be: once, always, or reject"));
        }

        boolean success = permissionManager.replyPermission(requestId, reply);
        if (!success) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Permission request not found or already resolved"));
        }

        return ResponseEntity.ok(Map.of(
            "message", "Permission replied",
            "requestId", requestId,
            "reply", reply.name()
        ));
    }

    // ==================== Task API ====================

    @PostMapping("/tasks")
    public ResponseEntity<Map<String, Object>> submitTask(@RequestBody Map<String, String> request) {
        String description = request.get("description");
        String prompt = request.get("prompt");

        if (description == null || description.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Description is required"));
        }

        CoworkTask task = taskManager.decomposeAndSubmit(description, prompt);
        return ResponseEntity.ok(Map.of("task", task));
    }

    @GetMapping("/tasks")
    public ResponseEntity<List<CoworkTask>> getTasks() {
        return ResponseEntity.ok(taskManager.getAllTasks());
    }

    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<CoworkTask> getTask(@PathVariable String taskId) {
        CoworkTask task = taskManager.getTask(taskId);
        if (task == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(task);
    }

    @PostMapping("/tasks/{taskId}/cancel")
    public ResponseEntity<Map<String, String>> cancelTask(@PathVariable String taskId) {
        taskManager.cancelTask(taskId);
        return ResponseEntity.ok(Map.of("message", "Task cancelled: " + taskId));
    }

    // ==================== Workspace API ====================

    @GetMapping("/workspaces")
    public ResponseEntity<List<Workspace>> getWorkspaces() {
        return ResponseEntity.ok(workspaceManager.getAllWorkspaces());
    }

    @PostMapping("/workspaces")
    public ResponseEntity<Workspace> addWorkspace(@RequestBody Map<String, String> request) {
        String name = request.get("name");
        String directory = request.get("directory");

        if (name == null || directory == null) {
            return ResponseEntity.badRequest().build();
        }

        Workspace workspace = workspaceManager.addWorkspace(name, directory);
        return ResponseEntity.ok(workspace);
    }

    @PostMapping("/workspaces/{workspaceId}/switch")
    public ResponseEntity<Workspace> switchWorkspace(@PathVariable String workspaceId) {
        Workspace workspace = workspaceManager.switchWorkspace(workspaceId);
        if (workspace == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(workspace);
    }

    @PostMapping("/workspaces/{workspaceId}/pin")
    public ResponseEntity<Map<String, String>> pinWorkspace(@PathVariable String workspaceId) {
        workspaceManager.pinWorkspace(workspaceId);
        return ResponseEntity.ok(Map.of("message", "Workspace pinned: " + workspaceId));
    }

    @PostMapping("/workspaces/{workspaceId}/unpin")
    public ResponseEntity<Map<String, String>> unpinWorkspace(@PathVariable String workspaceId) {
        workspaceManager.unpinWorkspace(workspaceId);
        return ResponseEntity.ok(Map.of("message", "Workspace unpinned: " + workspaceId));
    }

    @DeleteMapping("/workspaces/{workspaceId}")
    public ResponseEntity<Map<String, String>> removeWorkspace(@PathVariable String workspaceId) {
        workspaceManager.removeWorkspace(workspaceId);
        return ResponseEntity.ok(Map.of("message", "Workspace removed: " + workspaceId));
    }

    // ==================== Template API ====================

    @GetMapping("/templates")
    public ResponseEntity<List<WorkflowTemplate>> getTemplates() {
        return ResponseEntity.ok(templateManager.getAllTemplates());
    }

    @GetMapping("/templates/{templateId}")
    public ResponseEntity<WorkflowTemplate> getTemplate(@PathVariable String templateId) {
        WorkflowTemplate template = templateManager.getTemplate(templateId);
        if (template == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(template);
    }

    @PostMapping("/templates")
    public ResponseEntity<WorkflowTemplate> saveTemplate(@RequestBody WorkflowTemplate template) {
        templateManager.saveTemplate(template);
        return ResponseEntity.ok(template);
    }

    @DeleteMapping("/templates/{templateId}")
    public ResponseEntity<Map<String, String>> deleteTemplate(@PathVariable String templateId) {
        templateManager.deleteTemplate(templateId);
        return ResponseEntity.ok(Map.of("message", "Template deleted: " + templateId));
    }

    @PostMapping("/templates/{templateId}/render")
    public ResponseEntity<Map<String, String>> renderTemplate(
            @PathVariable String templateId,
            @RequestBody Map<String, String> variables) {
        try {
            String rendered = templateManager.renderTemplate(templateId, variables);
            return ResponseEntity.ok(Map.of("rendered", rendered));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/templates/search")
    public ResponseEntity<List<WorkflowTemplate>> searchTemplates(@RequestParam String keyword) {
        return ResponseEntity.ok(templateManager.searchTemplates(keyword));
    }

    // ==================== Plugin API ====================

    @GetMapping("/plugins")
    public ResponseEntity<List<Map<String, Object>>> getPlugins() {
        List<Map<String, Object>> plugins = pluginManager.getAllPlugins().stream()
            .map(plugin -> {
                Map<String, Object> pluginInfo = new HashMap<>();
                pluginInfo.put("id", plugin.getPluginId());
                pluginInfo.put("name", plugin.getName());
                pluginInfo.put("description", plugin.getDescription());
                pluginInfo.put("category", plugin.getCategory());
                pluginInfo.put("enabled", plugin.isEnabled());
                pluginInfo.put("commands", plugin.getCommands().stream()
                    .map(CoworkPlugin.PluginCommand::getName)
                    .collect(Collectors.toList()));
                return pluginInfo;
            })
            .collect(Collectors.toList());
        return ResponseEntity.ok(plugins);
    }

    @PostMapping("/plugins/{pluginId}/enable")
    public ResponseEntity<Map<String, String>> enablePlugin(@PathVariable String pluginId) {
        pluginManager.enablePlugin(pluginId);
        return ResponseEntity.ok(Map.of("message", "Plugin enabled: " + pluginId));
    }

    @PostMapping("/plugins/{pluginId}/disable")
    public ResponseEntity<Map<String, String>> disablePlugin(@PathVariable String pluginId) {
        pluginManager.disablePlugin(pluginId);
        return ResponseEntity.ok(Map.of("message", "Plugin disabled: " + pluginId));
    }

    @GetMapping("/plugins/commands")
    public ResponseEntity<Map<String, String>> getAvailableCommands() {
        return ResponseEntity.ok(pluginManager.getAvailableCommands());
    }

    // ==================== Connector API ====================

    @GetMapping("/connectors")
    public ResponseEntity<List<Map<String, Object>>> getConnectors() {
        List<Map<String, Object>> connectors = connectorManager.getAllConnectors().stream()
            .map(connector -> {
                Map<String, Object> connectorInfo = new HashMap<>();
                connectorInfo.put("id", connector.getId());
                connectorInfo.put("name", connector.getName());
                connectorInfo.put("type", connector.getType());
                connectorInfo.put("connected", connector.isConnected());
                connectorInfo.put("actions", connector.getSupportedActions());
                return connectorInfo;
            })
            .collect(Collectors.toList());
        return ResponseEntity.ok(connectors);
    }

    @PostMapping("/connectors/{connectorId}/connect")
    public ResponseEntity<Map<String, String>> connectConnector(
            @PathVariable String connectorId,
            @RequestBody Map<String, String> credentials) {
        try {
            connectorManager.connectConnector(connectorId, credentials);
            return ResponseEntity.ok(Map.of("message", "Connected: " + connectorId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to connect: " + e.getMessage()));
        }
    }

    @PostMapping("/connectors/{connectorId}/disconnect")
    public ResponseEntity<Map<String, String>> disconnectConnector(@PathVariable String connectorId) {
        try {
            connectorManager.disconnectConnector(connectorId);
            return ResponseEntity.ok(Map.of("message", "Disconnected: " + connectorId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to disconnect: " + e.getMessage()));
        }
    }

    // ==================== Config API ====================

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

    // ==================== History ====================

    @DeleteMapping("/history")
    public ResponseEntity<Map<String, String>> clearHistory() {
        taskManager.clearHistory();
        return ResponseEntity.ok(Map.of("message", "History cleared"));
    }
}
