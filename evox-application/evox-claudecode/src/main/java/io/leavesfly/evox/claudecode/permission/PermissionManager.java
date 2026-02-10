package io.leavesfly.evox.claudecode.permission;

import io.leavesfly.evox.claudecode.config.ClaudeCodeConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 权限管理器
 * 控制工具调用的审批流程，拦截危险操作
 */
@Slf4j
public class PermissionManager {

    private final ClaudeCodeConfig config;
    private final Set<String> sessionApprovedTools;
    private final PermissionCallback callback;

    /**
     * 权限审批回调接口
     */
    public interface PermissionCallback {
        /**
         * 请求用户审批
         *
         * @param toolName   工具名称
         * @param parameters 工具参数
         * @return 用户是否批准
         */
        boolean requestApproval(String toolName, Map<String, Object> parameters);
    }

    public PermissionManager(ClaudeCodeConfig config, PermissionCallback callback) {
        this.config = config;
        this.callback = callback;
        this.sessionApprovedTools = new HashSet<>();
    }

    /**
     * 检查工具调用是否被允许
     *
     * @param toolName   工具名称
     * @param parameters 工具参数
     * @return 是否允许执行
     */
    public boolean checkPermission(String toolName, Map<String, Object> parameters) {
        // check if the tool is blocked
        if (isBlockedOperation(toolName, parameters)) {
            log.warn("Blocked dangerous operation: {} with params: {}", toolName, parameters);
            return false;
        }

        // check if approval is required
        if (!config.isApprovalRequired(toolName)) {
            return true;
        }

        // check if already approved in this session
        if (sessionApprovedTools.contains(toolName)) {
            return true;
        }

        // request user approval
        boolean approved = callback.requestApproval(toolName, parameters);
        if (approved) {
            log.info("User approved tool: {}", toolName);
        } else {
            log.info("User denied tool: {}", toolName);
        }
        return approved;
    }

    /**
     * 将工具标记为本次会话中已批准（后续调用不再询问）
     */
    public void approveToolForSession(String toolName) {
        sessionApprovedTools.add(toolName);
    }

    /**
     * 清除会话级别的审批记录
     */
    public void clearSessionApprovals() {
        sessionApprovedTools.clear();
    }

    private boolean isBlockedOperation(String toolName, Map<String, Object> parameters) {
        if ("shell".equals(toolName)) {
            String command = (String) parameters.getOrDefault("command", "");
            String normalizedCommand = command.trim();
            for (String blocked : config.getBlockedCommands()) {
                if (matchesBlockedPattern(normalizedCommand, blocked)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 检查命令是否匹配黑名单模式。
     * 支持两种匹配方式：
     * - 完整命令串匹配（如 "rm -rf /"）：检查命令是否包含该完整子串
     * - 管道/链式命令中的首词匹配（如 "mkfs"）：检查命令中是否有以该词开头的子命令
     */
    private boolean matchesBlockedPattern(String command, String blockedPattern) {
        String lowerCommand = command.toLowerCase();
        String lowerPattern = blockedPattern.toLowerCase().trim();

        // exact substring match for multi-word patterns (e.g. "rm -rf /", "dd if=/dev/zero")
        if (lowerPattern.contains(" ")) {
            return lowerCommand.contains(lowerPattern);
        }

        // single-word pattern: match as command start (handles pipes, semicolons, &&, ||)
        // split by common shell separators and check if any segment starts with the pattern
        String[] segments = lowerCommand.split("[;|&]+");
        for (String segment : segments) {
            String trimmedSegment = segment.trim();
            if (trimmedSegment.equals(lowerPattern)
                    || trimmedSegment.startsWith(lowerPattern + " ")) {
                return true;
            }
        }
        return false;
    }
}
