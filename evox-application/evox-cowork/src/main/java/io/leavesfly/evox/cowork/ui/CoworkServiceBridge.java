package io.leavesfly.evox.cowork.ui;

import io.leavesfly.evox.cowork.config.CoworkConfig;
import io.leavesfly.evox.cowork.event.CoworkEventBus;
import io.leavesfly.evox.cowork.permission.InteractivePermissionManager;
import io.leavesfly.evox.cowork.permission.PermissionRequest;
import io.leavesfly.evox.cowork.session.CoworkSession;
import io.leavesfly.evox.cowork.session.SessionManager;
import io.leavesfly.evox.cowork.template.TemplateManager;
import io.leavesfly.evox.cowork.template.WorkflowTemplate;
import io.leavesfly.evox.cowork.workspace.Workspace;
import io.leavesfly.evox.cowork.workspace.WorkspaceManager;
import javafx.application.Platform;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@Slf4j
@Getter
public class CoworkServiceBridge {

    private final CoworkConfig config;
    private final CoworkEventBus eventBus;
    private final InteractivePermissionManager permissionManager;
    private final SessionManager sessionManager;
    private final TemplateManager templateManager;
    private final WorkspaceManager workspaceManager;
    private final ExecutorService executor;

    // 回调函数接口
    private Consumer<String> onStreamContent;
    private Consumer<PermissionRequest> onPermissionRequest;
    private Consumer<CoworkSession> onSessionCreated;
    private Consumer<String> onError;

    private CoworkServiceBridge(CoworkConfig config) {
        this.config = config;
        this.eventBus = new CoworkEventBus();
        this.permissionManager = new InteractivePermissionManager(config, eventBus);
        this.sessionManager = new SessionManager(config, permissionManager.getPermissionManager());
        this.templateManager = new TemplateManager(
            System.getProperty("user.home") + "/.evox/cowork/templates"
        );
        this.workspaceManager = new WorkspaceManager(
            System.getProperty("user.home") + "/.evox/cowork"
        );
        this.executor = Executors.newFixedThreadPool(4, runnable -> {
            Thread thread = new Thread(runnable, "cowork-worker");
            thread.setDaemon(true);
            return thread;
        });

        setupEventCallbacks();
    }

    // 初始化服务实例
    public static CoworkServiceBridge initialize() {
        CoworkConfig config = CoworkConfig.createDefault(System.getProperty("user.dir"));
        return new CoworkServiceBridge(config);
    }

    // 设置内部事件监听与回调转发
    private void setupEventCallbacks() {
        sessionManager.setEventCallback(sessionEvent -> {
            if (sessionEvent.type() == SessionManager.SessionEventType.STREAM) {
                if (onStreamContent != null) {
                    String content = sessionEvent.data() != null ? sessionEvent.data().toString() : "";
                    Platform.runLater(() -> onStreamContent.accept(content));
                }
            }
        });

        permissionManager.setUiPermissionCallback(request -> {
            if (onPermissionRequest != null) {
                Platform.runLater(() -> onPermissionRequest.accept(request));
            }
        });
    }

    public void setOnStreamContent(Consumer<String> callback) {
        this.onStreamContent = callback;
    }

    public void setOnPermissionRequest(Consumer<PermissionRequest> callback) {
        this.onPermissionRequest = callback;
    }

    public void setOnSessionCreated(Consumer<CoworkSession> callback) {
        this.onSessionCreated = callback;
    }

    public void setOnError(Consumer<String> callback) {
        this.onError = callback;
    }

    // 创建新会话
    public CoworkSession createSession(String workingDirectory) {
        CoworkSession session = sessionManager.createSession(workingDirectory);
        permissionManager.setCurrentSessionId(session.getSessionId());
        if (onSessionCreated != null) {
            Platform.runLater(() -> onSessionCreated.accept(session));
        }
        return session;
    }

    public List<CoworkSession> listSessions() {
        return sessionManager.listSessions();
    }

    public CoworkSession getSession(String sessionId) {
        return sessionManager.getSession(sessionId);
    }

    public void switchSession(String sessionId) {
        sessionManager.switchSession(sessionId);
        permissionManager.setCurrentSessionId(sessionId);
    }

    public void deleteSession(String sessionId) {
        sessionManager.deleteSession(sessionId);
    }

    public List<CoworkSession.SessionMessage> getMessages(String sessionId) {
        return sessionManager.getMessages(sessionId);
    }

    public void abortSession(String sessionId) {
        sessionManager.abortSession(sessionId);
    }

    // 发送用户指令
    public void sendPrompt(String sessionId, String message, Consumer<String> onComplete) {
        CompletableFuture.supplyAsync(() -> {
            try {
                permissionManager.setCurrentSessionId(sessionId);
                return sessionManager.prompt(sessionId, message);
            } catch (Exception exception) {
                log.error("Error processing prompt for session {}", sessionId, exception);
                if (onError != null) {
                    Platform.runLater(() -> onError.accept(exception.getMessage()));
                }
                return null;
            }
        }, executor).thenAccept(response -> {
            if (onComplete != null && response != null) {
                Platform.runLater(() -> onComplete.accept(response));
            }
        });
    }

    // 回复权限请求
    public boolean replyPermission(String requestId, PermissionRequest.PermissionReply reply) {
        return permissionManager.replyPermission(requestId, reply);
    }

    public List<PermissionRequest> getPendingPermissions() {
        return permissionManager.getPendingRequests();
    }

    public List<WorkflowTemplate> getAllTemplates() {
        return templateManager.getAllTemplates();
    }

    public WorkflowTemplate getTemplate(String templateId) {
        return templateManager.getTemplate(templateId);
    }

    // 渲染模板
    public String renderTemplate(String templateId, Map<String, String> variables) {
        try {
            return templateManager.renderTemplate(templateId, variables);
        } catch (Exception exception) {
            log.error("Error rendering template {}", templateId, exception);
            return null;
        }
    }

    public List<Workspace> getAllWorkspaces() {
        return workspaceManager.getAllWorkspaces();
    }

    public Workspace addWorkspace(String name, String directory) {
        return workspaceManager.addWorkspace(name, directory);
    }

    public Workspace switchWorkspace(String workspaceId) {
        return workspaceManager.switchWorkspace(workspaceId);
    }

    public void pinWorkspace(String workspaceId) {
        workspaceManager.pinWorkspace(workspaceId);
    }

    public void unpinWorkspace(String workspaceId) {
        workspaceManager.unpinWorkspace(workspaceId);
    }

    public void removeWorkspace(String workspaceId) {
        workspaceManager.removeWorkspace(workspaceId);
    }

    // 关闭服务
    public void shutdown() {
        log.info("Shutting down CoworkServiceBridge...");
        executor.shutdownNow();
    }
}