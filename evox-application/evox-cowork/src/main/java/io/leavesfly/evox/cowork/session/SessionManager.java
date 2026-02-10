package io.leavesfly.evox.cowork.session;

import io.leavesfly.evox.cowork.agent.CoworkAgent;
import io.leavesfly.evox.cowork.config.CoworkConfig;
import io.leavesfly.evox.cowork.context.CoworkContext;
import io.leavesfly.evox.cowork.permission.CoworkPermissionManager;
import io.leavesfly.evox.cowork.tool.CoworkToolRegistry;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Slf4j
@Data
public class SessionManager {
    private final Map<String, CoworkSession> sessions;
    private final Map<String, CoworkAgent> sessionAgents;
    private final AtomicReference<String> activeSessionId = new AtomicReference<>();
    private final CoworkConfig config;
    private final CoworkPermissionManager permissionManager;
    private Consumer<SessionEvent> eventCallback;

    public SessionManager(CoworkConfig config, CoworkPermissionManager permissionManager) {
        this.sessions = new ConcurrentHashMap<>();
        this.sessionAgents = new ConcurrentHashMap<>();
        this.config = config;
        this.permissionManager = permissionManager;
    }

    public CoworkSession createSession(String workingDirectory) {
        CoworkSession session = new CoworkSession();
        
        String effectiveWorkingDir = workingDirectory != null 
            ? workingDirectory 
            : config.getWorkingDirectory();
        session.setWorkingDirectory(effectiveWorkingDir);

        CoworkContext coworkContext = new CoworkContext(effectiveWorkingDir);
        coworkContext.scanWorkspace();

        CoworkToolRegistry toolRegistry = new CoworkToolRegistry(effectiveWorkingDir);

        CoworkAgent agent = new CoworkAgent(config, permissionManager, toolRegistry, coworkContext);
        agent.setStreamCallback(content -> {
            SessionEvent event = new SessionEvent(
                session.getSessionId(),
                SessionEventType.STREAM,
                content,
                System.currentTimeMillis()
            );
            emitEvent(event);
        });

        sessions.put(session.getSessionId(), session);
        sessionAgents.put(session.getSessionId(), agent);

        activeSessionId.set(session.getSessionId());

        emitEvent(new SessionEvent(
            session.getSessionId(),
            SessionEventType.SESSION_CREATED,
            session.toSummary(),
            System.currentTimeMillis()
        ));

        return session;
    }

    public CoworkSession getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    public List<CoworkSession> listSessions() {
        return sessions.values().stream()
            .sorted(Comparator.comparingLong(CoworkSession::getUpdatedAt).reversed())
            .toList();
    }

    public String getActiveSessionId() {
        return activeSessionId.get();
    }

    public CoworkSession getActiveSession() {
        String currentId = activeSessionId.get();
        if (currentId == null) {
            return null;
        }
        return sessions.get(currentId);
    }

    public String prompt(String sessionId, String message) {
        CoworkSession session = getSession(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }

        CoworkAgent agent = sessionAgents.get(sessionId);
        if (agent == null) {
            throw new IllegalStateException("Agent not found for session: " + sessionId);
        }

        session.markActive();
        session.addMessage("user", message, "text");

        emitEvent(new SessionEvent(
            sessionId,
            SessionEventType.MESSAGE_ADDED,
            session.getLastMessage(),
            System.currentTimeMillis()
        ));

        String response = agent.chat(message);

        session.addMessage("assistant", response, "text");

        emitEvent(new SessionEvent(
            sessionId,
            SessionEventType.MESSAGE_ADDED,
            session.getLastMessage(),
            System.currentTimeMillis()
        ));

        return response;
    }

    public void abortSession(String sessionId) {
        CoworkSession session = getSession(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }

        session.markAborted();

        emitEvent(new SessionEvent(
            sessionId,
            SessionEventType.SESSION_ABORTED,
            session.toSummary(),
            System.currentTimeMillis()
        ));
    }

    public List<CoworkSession.SessionMessage> getMessages(String sessionId) {
        CoworkSession session = getSession(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }
        return session.getMessages();
    }

    public String summarizeSession(String sessionId) {
        CoworkSession session = getSession(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }

        long duration = session.getUpdatedAt() - session.getCreatedAt();
        return String.format("Session: %s, Messages: %d, Status: %s, Duration: %d ms",
            session.getTitle(),
            session.getMessageCount(),
            session.getStatus(),
            duration
        );
    }

    public void deleteSession(String sessionId) {
        CoworkSession session = sessions.remove(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }

        sessionAgents.remove(sessionId);

        activeSessionId.compareAndSet(sessionId, null);

        emitEvent(new SessionEvent(
            sessionId,
            SessionEventType.SESSION_DELETED,
            session.toSummary(),
            System.currentTimeMillis()
        ));
    }

    public void switchSession(String sessionId) {
        CoworkSession session = getSession(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }

        activeSessionId.set(sessionId);

        emitEvent(new SessionEvent(
            sessionId,
            SessionEventType.SESSION_SWITCHED,
            session.toSummary(),
            System.currentTimeMillis()
        ));
    }

    public void setEventCallback(Consumer<SessionEvent> callback) {
        this.eventCallback = callback;
    }

    private void emitEvent(SessionEvent event) {
        if (eventCallback != null) {
            eventCallback.accept(event);
        }
    }

    public record SessionEvent(
        String sessionId,
        SessionEventType type,
        Object data,
        long timestamp
    ) {}

    public enum SessionEventType {
        SESSION_CREATED,
        SESSION_DELETED,
        SESSION_SWITCHED,
        SESSION_ABORTED,
        MESSAGE_ADDED,
        STREAM,
        TOOL_EXECUTION,
        PERMISSION_REQUEST,
        PROGRESS_UPDATE
    }
}
