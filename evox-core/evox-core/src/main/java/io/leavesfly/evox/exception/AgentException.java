package io.leavesfly.evox.exception;

/**
 * Agent 相关异常
 * 用于 Agent 执行过程中的错误情况
 */
public class AgentException extends EvoXException {

    private static final int AGENT_ERROR_CODE = 1000;

    public AgentException(String message) {
        super(message);
    }

    public AgentException(String message, Throwable cause) {
        super(message, cause);
    }

    public AgentException(int errorCode, String message) {
        super(errorCode, message);
    }

    public AgentException(int errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    /**
     * 创建 Agent 执行异常
     */
    public static AgentException executionError(String agentName, String reason) {
        return new AgentException(AGENT_ERROR_CODE + 1, 
            String.format("Agent [%s] 执行失败: %s", agentName, reason));
    }

    /**
     * 创建 Agent 执行异常（带原因）
     */
    public static AgentException executionError(String agentName, String reason, Throwable cause) {
        return new AgentException(AGENT_ERROR_CODE + 1, 
            String.format("Agent [%s] 执行失败: %s", agentName, reason), cause);
    }

    /**
     * 创建 Agent 配置异常
     */
    public static AgentException configurationError(String agentName, String reason) {
        return new AgentException(AGENT_ERROR_CODE + 2, 
            String.format("Agent [%s] 配置错误: %s", agentName, reason));
    }

    /**
     * 创建 Agent 初始化异常
     */
    public static AgentException initializationError(String agentName, String reason) {
        return new AgentException(AGENT_ERROR_CODE + 3, 
            String.format("Agent [%s] 初始化失败: %s", agentName, reason));
    }
}
