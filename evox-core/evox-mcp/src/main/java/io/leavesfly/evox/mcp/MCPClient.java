package io.leavesfly.evox.mcp;

import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * MCP客户端
 * 连接到MCP服务器并调用其功能
 *
 * @author EvoX Team
 */
public class MCPClient {

    private final String url;
    @Getter
    private volatile ConnectionStatus status;
    private volatile MCPServer connectedServer;
    private final Object connectionLock = new Object();

    public MCPClient(String url) {
        this.url = url;
        this.status = ConnectionStatus.DISCONNECTED;
    }

    /**
     * 连接到服务器（线程安全）
     */
    public void connect(MCPServer server) {
        synchronized (connectionLock) {
            this.connectedServer = server;
            this.status = ConnectionStatus.CONNECTED;
        }
    }

    /**
     * 断开连接（线程安全）
     */
    public void disconnect() {
        synchronized (connectionLock) {
            this.connectedServer = null;
            this.status = ConnectionStatus.DISCONNECTED;
        }
    }

    /**
     * 是否已初始化
     */
    public boolean isInitialized() {
        return status == ConnectionStatus.CONNECTED;
    }

    /**
     * 列出资源
     */
    public List<MCPResource> listResources() {
        MCPServer server = this.connectedServer;
        if (status != ConnectionStatus.CONNECTED || server == null) {
            throw MCPException.connectionError("客户端未连接");
        }
        return server.listResources();
    }

    /**
     * 调用工具
     */
    public MCPProtocol.ToolCallResult callTool(String name, Map<String, Object> args) {
        MCPServer server = this.connectedServer;
        if (status != ConnectionStatus.CONNECTED || server == null) {
            throw MCPException.connectionError("客户端未连接");
        }
        return server.invokeTool(name, args);
    }

    /**
     * 连接状态枚举
     */
    public enum ConnectionStatus {
        DISCONNECTED,
        CONNECTED
    }
}
