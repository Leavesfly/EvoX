package io.leavesfly.evox.mcp;

import lombok.Getter;

/**
 * MCP会话
 * 管理客户端与服务器之间的会话状态
 *
 * @author EvoX Team
 */
public class MCPSession {

    @Getter
    private final String sessionId;
    @Getter
    private SessionState state;
    private long requestIdCounter;

    public MCPSession() {
        this.sessionId = java.util.UUID.randomUUID().toString();
        this.state = SessionState.CREATED;
        this.requestIdCounter = 0;
    }

    /**
     * 是否已初始化
     */
    public boolean isInitialized() {
        return state == SessionState.INITIALIZED;
    }

    /**
     * 标记为初始化中
     */
    public void markInitializing() {
        this.state = SessionState.INITIALIZING;
    }

    /**
     * 标记为已初始化
     */
    public void markInitialized(MCPProtocol.InitializeResult result) {
        this.state = SessionState.INITIALIZED;
    }

    /**
     * 生成下一个请求ID
     */
    public long nextRequestId() {
        return ++requestIdCounter;
    }

    /**
     * 会话状态枚举
     */
    public enum SessionState {
        CREATED,
        INITIALIZING,
        INITIALIZED
    }
}
