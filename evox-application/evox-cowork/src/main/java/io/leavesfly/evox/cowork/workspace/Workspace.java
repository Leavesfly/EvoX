package io.leavesfly.evox.cowork.workspace;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
public class Workspace {
    private String workspaceId;
    private String name;
    private String directory;
    private String description;
    private long lastAccessedAt;
    private long createdAt;
    private boolean pinned;
    private Map<String, Object> settings;

    public Workspace() {
        this.workspaceId = UUID.randomUUID().toString();
        this.createdAt = System.currentTimeMillis();
        this.lastAccessedAt = System.currentTimeMillis();
        this.pinned = false;
        this.settings = new HashMap<>();
    }

    public void touch() {
        this.lastAccessedAt = System.currentTimeMillis();
    }

    public Map<String, Object> toSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("workspaceId", workspaceId);
        summary.put("name", name);
        summary.put("directory", directory);
        summary.put("description", description);
        summary.put("lastAccessedAt", lastAccessedAt);
        summary.put("createdAt", createdAt);
        summary.put("pinned", pinned);
        return summary;
    }
}
