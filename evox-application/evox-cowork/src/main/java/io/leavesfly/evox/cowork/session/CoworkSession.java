package io.leavesfly.evox.cowork.session;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
public class CoworkSession {
    private String sessionId;
    private String title;
    private String workingDirectory;
    private SessionStatus status;
    private List<SessionMessage> messages;
    private long createdAt;
    private long updatedAt;
    private Map<String, Object> metadata;

    public CoworkSession() {
        this.sessionId = UUID.randomUUID().toString();
        this.status = SessionStatus.IDLE;
        this.messages = new ArrayList<>();
        this.metadata = new HashMap<>();
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public SessionMessage addMessage(String role, String content, String type) {
        SessionMessage message = new SessionMessage();
        message.setMessageId(UUID.randomUUID().toString());
        message.setRole(role);
        message.setContent(content);
        message.setType(type);
        message.setTimestamp(System.currentTimeMillis());
        message.setMetadata(new HashMap<>());

        this.messages.add(message);
        this.updatedAt = System.currentTimeMillis();

        if (this.title == null || this.title.isEmpty()) {
            if ("user".equals(role)) {
                String titleCandidate = content.length() > 50 
                    ? content.substring(0, 50) 
                    : content;
                this.title = titleCandidate;
            }
        }

        return message;
    }

    public int getMessageCount() {
        return this.messages.size();
    }

    public SessionMessage getLastMessage() {
        if (this.messages.isEmpty()) {
            return null;
        }
        return this.messages.get(this.messages.size() - 1);
    }

    public void markActive() {
        this.status = SessionStatus.ACTIVE;
        this.updatedAt = System.currentTimeMillis();
    }

    public void markCompleted() {
        this.status = SessionStatus.COMPLETED;
        this.updatedAt = System.currentTimeMillis();
    }

    public void markAborted() {
        this.status = SessionStatus.ABORTED;
        this.updatedAt = System.currentTimeMillis();
    }

    public Map<String, Object> toSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("id", this.sessionId);
        summary.put("title", this.title);
        summary.put("status", this.status);
        summary.put("messageCount", this.getMessageCount());
        summary.put("createdAt", this.createdAt);
        summary.put("updatedAt", this.updatedAt);
        return summary;
    }

    public enum SessionStatus {
        ACTIVE,
        IDLE,
        COMPLETED,
        ABORTED
    }

    @Data
    public static class SessionMessage {
        private String messageId;
        private String role;
        private String content;
        private String type;
        private long timestamp;
        private Map<String, Object> metadata;
    }
}
