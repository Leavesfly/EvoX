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
            String normalizedCommand = command.trim().toLowerCase();
            for (String blocked : config.getBlockedCommands()) {
                if (normalizedCommand.contains(blocked.toLowerCase())) {
                    return true;
                }
            }
        }
        return false;
    }
}
