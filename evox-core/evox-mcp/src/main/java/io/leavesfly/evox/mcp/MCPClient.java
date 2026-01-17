package io.leavesfly.evox.mcp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP客户端
 * 连接到MCP服务器并使用其资源和工具
 *
 * @author EvoX Team
 */
@Slf4j
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MCPClient {

    /**
     * 服务器地址
     */
    private String serverUrl;

    /**
     * 连接状态
     */
    private ConnectionStatus status;

    /**
     * 连接的服务器
     */
    private MCPServer connectedServer;

    public MCPClient(String serverUrl) {
        this.serverUrl = serverUrl;
        this.status = ConnectionStatus.DISCONNECTED;
    }

    /**
     * 连接到服务器
     */
    public void connect(MCPServer server) {
        if (server == null) {
            throw new IllegalArgumentException("服务器不能为空");
        }

        log.info("连接到MCP服务器: {}", server.getName());
        this.connectedServer = server;
        this.status = ConnectionStatus.CONNECTED;
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        log.info("断开MCP连接");
        this.connectedServer = null;
        this.status = ConnectionStatus.DISCONNECTED;
    }

    /**
     * 列出可用资源
     */
    public List<MCPResource> listResources() {
        ensureConnected();
        return connectedServer.listResources();
    }

    /**
     * 列出可用工具
     */
    public List<MCPTool> listTools() {
        ensureConnected();
        return connectedServer.listTools();
    }

    /**
     * 读取资源
     */
    public Object readResource(String uri) {
        ensureConnected();
        log.debug("读取资源: {}", uri);
        return connectedServer.readResource(uri);
    }

    /**
     * 调用工具
     */
    public Object callTool(String toolName, Map<String, Object> arguments) {
        ensureConnected();
        log.debug("调用工具: {} with args: {}", toolName, arguments);
        return connectedServer.invokeTool(toolName, arguments);
    }

    /**
     * 调用工具(无参数)
     */
    public Object callTool(String toolName) {
        return callTool(toolName, new HashMap<>());
    }

    /**
     * 获取服务器信息
     */
    public MCPServer.ServerInfo getServerInfo() {
        ensureConnected();
        return connectedServer.getServerInfo();
    }

    /**
     * 确保已连接
     */
    private void ensureConnected() {
        if (status != ConnectionStatus.CONNECTED || connectedServer == null) {
            throw new IllegalStateException("未连接到MCP服务器");
        }
    }

    /**
     * 连接状态
     */
    public enum ConnectionStatus {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        ERROR
    }
}
