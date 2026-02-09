package io.leavesfly.evox.cowork.permission;

import io.leavesfly.evox.cowork.config.CoworkConfig;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class CoworkPermissionManager {
    private final CoworkConfig config;
    private final Set<String> sessionApprovedTools;
    private final PermissionCallback callback;
    private final List<AccessLogEntry> accessLog;

    public CoworkPermissionManager(CoworkConfig config, PermissionCallback callback) {
        this.config = config;
        this.callback = callback;
        this.sessionApprovedTools = ConcurrentHashMap.newKeySet();
        this.accessLog = Collections.synchronizedList(new ArrayList<>());
    }

    public boolean checkPermission(String toolName, Map<String, Object> parameters) {
        if (isBlockedOperation(toolName, parameters)) {
            log.warn("Blocked operation attempted: {} with parameters: {}", toolName, parameters);
            return false;
        }

        if (isDeleteOperation(toolName, parameters) && config.isDeleteProtectionEnabled()) {
            boolean approved = callback.requestApproval(toolName, parameters);
            logAccess(toolName, parameters, approved);
            return approved;
        }

        if (isSandboxViolation(toolName, parameters)) {
            log.warn("Sandbox violation detected: {} with parameters: {}", toolName, parameters);
            return false;
        }

        if (!config.isApprovalRequired(toolName)) {
            log.debug("Tool {} does not require approval", toolName);
            return true;
        }

        if (sessionApprovedTools.contains(toolName)) {
            log.debug("Tool {} already approved for session", toolName);
            return true;
        }

        boolean approved = callback.requestApproval(toolName, parameters);
        logAccess(toolName, parameters, approved);
        return approved;
    }

    public boolean checkFileAccess(String filePath) {
        if (!config.isSandboxEnabled()) {
            return true;
        }

        String normalizedPath = Path.of(filePath).normalize().toString();

        for (String allowedDir : config.getAllowedDirectories()) {
            if (normalizedPath.startsWith(allowedDir)) {
                return true;
            }
        }

        if (normalizedPath.startsWith(config.getWorkingDirectory())) {
            return true;
        }

        return false;
    }

    public void approveToolForSession(String toolName) {
        sessionApprovedTools.add(toolName);
        log.info("Approved tool {} for current session", toolName);
    }

    public void clearSessionApprovals() {
        sessionApprovedTools.clear();
        log.info("Cleared all session tool approvals");
    }

    public List<AccessLogEntry> getAccessLog() {
        synchronized (accessLog) {
            return new ArrayList<>(accessLog);
        }
    }

    private boolean isBlockedOperation(String toolName, Map<String, Object> parameters) {
        if ("shell".equals(toolName)) {
            String command = (String) parameters.get("command");
            if (command != null) {
                for (String blockedPattern : config.getBlockedCommands()) {
                    if (command.contains(blockedPattern)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isDeleteOperation(String toolName, Map<String, Object> parameters) {
        if ("file_system".equals(toolName)) {
            String operation = (String) parameters.get("operation");
            return "delete".equals(operation);
        }
        if ("shell".equals(toolName)) {
            String command = (String) parameters.get("command");
            return command != null && command.contains("rm ");
        }
        return false;
    }

    private boolean isSandboxViolation(String toolName, Map<String, Object> parameters) {
        if (!config.isSandboxEnabled()) {
            return false;
        }

        if ("file_system".equals(toolName) || "file_edit".equals(toolName)) {
            String filePath = (String) parameters.get("filePath");
            if (filePath != null) {
                return !checkFileAccess(filePath);
            }
        }

        return false;
    }

    private void logAccess(String toolName, Map<String, Object> parameters, boolean approved) {
        AccessLogEntry entry = new AccessLogEntry(toolName, parameters, approved, System.currentTimeMillis());
        synchronized (accessLog) {
            accessLog.add(entry);
        }
    }

    public interface PermissionCallback {
        boolean requestApproval(String toolName, Map<String, Object> parameters);
    }

    public record AccessLogEntry(
        String toolName,
        Map<String, Object> parameters,
        boolean approved,
        long timestamp
    ) {}
}
