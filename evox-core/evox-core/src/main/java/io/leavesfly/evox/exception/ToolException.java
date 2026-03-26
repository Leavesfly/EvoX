package io.leavesfly.evox.exception;

/**
 * Tool 相关异常
 * 用于工具执行过程中的错误情况
 */
public class ToolException extends EvoXException {

    private static final int TOOL_ERROR_CODE = 5000;

    public ToolException(String message) {
        super(message);
    }

    public ToolException(String message, Throwable cause) {
        super(message, cause);
    }

    public ToolException(int errorCode, String message) {
        super(errorCode, message);
    }

    public ToolException(int errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    /**
     * 创建工具执行异常
     */
    public static ToolException executionError(String toolName, String reason) {
        return new ToolException(TOOL_ERROR_CODE + 1, 
            String.format("工具 [%s] 执行失败: %s", toolName, reason));
    }

    /**
     * 创建工具执行异常（带原因）
     */
    public static ToolException executionError(String toolName, String reason, Throwable cause) {
        return new ToolException(TOOL_ERROR_CODE + 1, 
            String.format("工具 [%s] 执行失败: %s", toolName, reason), cause);
    }

    /**
     * 创建工具配置异常
     */
    public static ToolException configurationError(String toolName, String reason) {
        return new ToolException(TOOL_ERROR_CODE + 2, 
            String.format("工具 [%s] 配置错误: %s", toolName, reason));
    }

    /**
     * 创建工具配置异常（带原因）
     */
    public static ToolException configurationError(String toolName, String reason, Throwable cause) {
        return new ToolException(TOOL_ERROR_CODE + 2, 
            String.format("工具 [%s] 配置错误: %s", toolName, reason), cause);
    }

    /**
     * 创建工具参数异常
     */
    public static ToolException invalidParameter(String toolName, String paramName, String reason) {
        return new ToolException(TOOL_ERROR_CODE + 3, 
            String.format("工具 [%s] 参数 [%s] 无效: %s", toolName, paramName, reason));
    }

    /**
     * 创建工具未找到异常
     */
    public static ToolException toolNotFound(String toolName) {
        return new ToolException(TOOL_ERROR_CODE + 4, 
            String.format("工具未找到: %s", toolName));
    }
}
