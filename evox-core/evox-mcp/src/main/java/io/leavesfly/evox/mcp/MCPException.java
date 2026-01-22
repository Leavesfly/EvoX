package io.leavesfly.evox.mcp;

/**
 * MCP异常
 * 统一的MCP错误处理
 *
 * @author EvoX Team
 */
public class MCPException extends RuntimeException {

    /**
     * 错误码
     */
    private final int errorCode;

    /**
     * 错误数据
     */
    private final Object errorData;

    public MCPException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.errorData = null;
    }

    public MCPException(int errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.errorData = null;
    }

    public MCPException(int errorCode, String message, Object errorData) {
        super(message);
        this.errorCode = errorCode;
        this.errorData = errorData;
    }

    public MCPException(int errorCode, String message, Object errorData, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.errorData = errorData;
    }

    public MCPException(MCPProtocol.Error error) {
        super(error.getMessage());
        this.errorCode = error.getCode();
        this.errorData = error.getData();
    }

    public int getErrorCode() {
        return errorCode;
    }

    public Object getErrorData() {
        return errorData;
    }

    /**
     * 转换为协议错误对象
     */
    public MCPProtocol.Error toProtocolError() {
        return MCPProtocol.Error.builder()
                .code(errorCode)
                .message(getMessage())
                .data(errorData)
                .build();
    }

    // ============= 便捷工厂方法 =============

    /**
     * 解析错误
     */
    public static MCPException parseError(String message) {
        return new MCPException(MCPProtocol.ErrorCodes.PARSE_ERROR, message);
    }

    /**
     * 无效请求
     */
    public static MCPException invalidRequest(String message) {
        return new MCPException(MCPProtocol.ErrorCodes.INVALID_REQUEST, message);
    }

    /**
     * 方法未找到
     */
    public static MCPException methodNotFound(String method) {
        return new MCPException(MCPProtocol.ErrorCodes.METHOD_NOT_FOUND, 
                "方法未找到: " + method);
    }

    /**
     * 无效参数
     */
    public static MCPException invalidParams(String message) {
        return new MCPException(MCPProtocol.ErrorCodes.INVALID_PARAMS, message);
    }

    /**
     * 内部错误
     */
    public static MCPException internalError(String message) {
        return new MCPException(MCPProtocol.ErrorCodes.INTERNAL_ERROR, message);
    }

    /**
     * 内部错误（带原因）
     */
    public static MCPException internalError(String message, Throwable cause) {
        return new MCPException(MCPProtocol.ErrorCodes.INTERNAL_ERROR, message, cause);
    }

    /**
     * 资源未找到
     */
    public static MCPException resourceNotFound(String uri) {
        return new MCPException(MCPProtocol.ErrorCodes.RESOURCE_NOT_FOUND, 
                "资源未找到: " + uri);
    }

    /**
     * 工具未找到
     */
    public static MCPException toolNotFound(String toolName) {
        return new MCPException(MCPProtocol.ErrorCodes.TOOL_NOT_FOUND, 
                "工具未找到: " + toolName);
    }

    /**
     * 提示未找到
     */
    public static MCPException promptNotFound(String promptName) {
        return new MCPException(MCPProtocol.ErrorCodes.PROMPT_NOT_FOUND, 
                "提示未找到: " + promptName);
    }

    /**
     * 未授权
     */
    public static MCPException unauthorized(String message) {
        return new MCPException(MCPProtocol.ErrorCodes.UNAUTHORIZED, message);
    }

    /**
     * 超时
     */
    public static MCPException timeout(String message) {
        return new MCPException(MCPProtocol.ErrorCodes.TIMEOUT, message);
    }

    /**
     * 已取消
     */
    public static MCPException cancelled(String message) {
        return new MCPException(MCPProtocol.ErrorCodes.CANCELLED, message);
    }

    /**
     * 会话未初始化
     */
    public static MCPException sessionNotInitialized() {
        return new MCPException(MCPProtocol.ErrorCodes.INVALID_REQUEST, 
                "会话未初始化");
    }

    /**
     * 连接错误
     */
    public static MCPException connectionError(String message) {
        return new MCPException(MCPProtocol.ErrorCodes.INTERNAL_ERROR, 
                "连接错误: " + message);
    }

    /**
     * 连接错误（带原因）
     */
    public static MCPException connectionError(String message, Throwable cause) {
        return new MCPException(MCPProtocol.ErrorCodes.INTERNAL_ERROR, 
                "连接错误: " + message, cause);
    }

    @Override
    public String toString() {
        return String.format("MCPException[code=%d, message=%s]", errorCode, getMessage());
    }
}
