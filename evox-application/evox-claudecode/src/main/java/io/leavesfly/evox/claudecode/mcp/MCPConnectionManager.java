package io.leavesfly.evox.claudecode.mcp;

import io.leavesfly.evox.claudecode.tool.ToolRegistry;
import io.leavesfly.evox.mcp.MCPTool;
import io.leavesfly.evox.mcp.runtime.MCPClient;
import io.leavesfly.evox.mcp.runtime.MCPServer;
import io.leavesfly.evox.mcp.runtime.MCPTransport;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP 连接管理器
 * 管理多个 MCP Server 连接，支持动态连接/断开，
 * 并将 MCP 工具自动注册到 ToolRegistry。
 */
@Slf4j
public class MCPConnectionManager {

    private final ToolRegistry toolRegistry;

    /** 工具列表变化时的回调（用于通知 CodingAgent 清除缓存） */
    @Setter
    private Runnable onToolListChanged;

    /** 已连接的 MCP 服务器：serverName → MCPClient */
    @Getter
    private final Map<String, MCPClient> connectedServers = new ConcurrentHashMap<>();

    /** 每个服务器注册的工具名列表：serverName → List<toolName> */
    private final Map<String, List<String>> serverToolNames = new ConcurrentHashMap<>();

    public MCPConnectionManager(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    /**
     * 连接到本地 MCP Server 实例
     *
     * @param serverName 服务器名称（用于标识和工具前缀）
     * @param server     MCP Server 实例
     * @return 注册的工具数量
     */
    public int connectLocal(String serverName, MCPServer server) {
        if (connectedServers.containsKey(serverName)) {
            log.warn("Server '{}' is already connected, disconnecting first", serverName);
            disconnect(serverName);
        }

        MCPClient client = MCPClient.builder()
                .clientInfo("EvoX-ClaudeCode", "1.0.0")
                .build();
        client.connect(server);

        connectedServers.put(serverName, client);
        int toolCount = registerMCPTools(serverName, client);

        log.info("Connected to local MCP server '{}', registered {} tools", serverName, toolCount);
        return toolCount;
    }

    /**
     * 连接到远程 MCP Server（通过 URL）
     *
     * @param serverName 服务器名称
     * @param serverUrl  服务器 URL（如 http://localhost:3000/mcp）
     * @return 注册的工具数量
     */
    /**
     * 连接到远程 MCP Server（通过 URL）
     * 支持 SSE 和 HTTP 两种传输方式：
     * - URL 以 /sse 结尾或包含 sse → 使用 SSETransport
     * - 其他情况 → 默认使用 SSETransport（MCP 标准远程传输）
     *
     * @param serverName 服务器名称
     * @param serverUrl  服务器基础 URL（如 http://localhost:3000）
     * @return 注册的工具数量
     */
    public int connectRemote(String serverName, String serverUrl) {
        if (connectedServers.containsKey(serverName)) {
            log.warn("Server '{}' is already connected, disconnecting first", serverName);
            disconnect(serverName);
        }

        MCPTransport transport = createTransport(serverUrl);
        MCPClient client = MCPClient.builder()
                .transport(transport)
                .serverUrl(serverUrl)
                .clientInfo("EvoX-ClaudeCode", "1.0.0")
                .build();

        try {
            client.connectRemote().get();
        } catch (Exception e) {
            log.error("Failed to connect to remote MCP server '{}' at {}", serverName, serverUrl, e);
            throw new RuntimeException("Failed to connect to MCP server: " + e.getMessage(), e);
        }

        connectedServers.put(serverName, client);
        int toolCount = registerMCPTools(serverName, client);

        log.info("Connected to remote MCP server '{}' at {}, registered {} tools",
                serverName, serverUrl, toolCount);
        return toolCount;
    }

    /**
     * 通过 STDIO 方式连接到 MCP Server（启动子进程）
     *
     * @param serverName 服务器名称
     * @param command    启动命令（如 "npx"）
     * @param args       命令参数（如 ["-y", "@modelcontextprotocol/server-filesystem", "/tmp"]）
     * @return 注册的工具数量
     */
    public int connectStdio(String serverName, String command, String[] args) {
        if (connectedServers.containsKey(serverName)) {
            log.warn("Server '{}' is already connected, disconnecting first", serverName);
            disconnect(serverName);
        }

        // build full command array: [command, args...]
        String[] fullArgs = args != null ? args : new String[0];
        MCPTransport transport = new MCPTransport.StdioTransport(command, fullArgs);
        MCPClient client = MCPClient.builder()
                .transport(transport)
                .clientInfo("EvoX-ClaudeCode", "1.0.0")
                .build();

        try {
            client.connectRemote().get();
        } catch (Exception e) {
            log.error("Failed to connect to STDIO MCP server '{}' (command: {})", serverName, command, e);
            throw new RuntimeException("Failed to connect to MCP server via STDIO: " + e.getMessage(), e);
        }

        connectedServers.put(serverName, client);
        int toolCount = registerMCPTools(serverName, client);

        log.info("Connected to STDIO MCP server '{}' (command: {}), registered {} tools",
                serverName, command, toolCount);
        return toolCount;
    }

    /**
     * 根据 URL 创建合适的传输层
     */
    private MCPTransport createTransport(String serverUrl) {
        String normalizedUrl = serverUrl.toLowerCase();
        if (normalizedUrl.endsWith("/sse") || normalizedUrl.contains("/sse/")) {
            // URL 直接指向 SSE 端点
            String baseUrl = serverUrl.replaceAll("/sse/?$", "");
            return new MCPTransport.SSETransport(baseUrl);
        }
        // 默认使用 SSE 传输（MCP 标准远程传输方式）
        return new MCPTransport.SSETransport(serverUrl);
    }

    /**
     * 断开指定服务器连接并移除其工具
     */
    public void disconnect(String serverName) {
        MCPClient client = connectedServers.remove(serverName);
        if (client == null) {
            log.warn("Server '{}' is not connected", serverName);
            return;
        }

        // unregister tools from ToolRegistry
        List<String> toolNames = serverToolNames.remove(serverName);
        if (toolNames != null) {
            for (String toolName : toolNames) {
                toolRegistry.getToolkit().removeTool(toolName);
            }
            log.info("Unregistered {} tools from server '{}'", toolNames.size(), serverName);
        }

        client.disconnect();
        log.info("Disconnected from MCP server '{}'", serverName);
    }

    /**
     * 断开所有连接
     */
    public void disconnectAll() {
        List<String> serverNames = new ArrayList<>(connectedServers.keySet());
        for (String serverName : serverNames) {
            disconnect(serverName);
        }
    }

    /**
     * 获取所有已连接服务器的摘要信息
     */
    public List<ServerSummary> listConnections() {
        List<ServerSummary> summaries = new ArrayList<>();
        connectedServers.forEach((name, client) -> {
            List<String> tools = serverToolNames.getOrDefault(name, List.of());
            String status = client.isInitialized() ? "connected" : "disconnected";
            String protocolVersion = client.getProtocolVersion();
            summaries.add(new ServerSummary(name, status, protocolVersion, tools.size()));
        });
        return summaries;
    }

    /**
     * 将 MCP Server 的工具注册到 ToolRegistry
     */
    private int registerMCPTools(String serverName, MCPClient client) {
        List<MCPTool> mcpTools = client.listTools();
        if (mcpTools == null || mcpTools.isEmpty()) {
            serverToolNames.put(serverName, List.of());
            return 0;
        }

        List<String> registeredNames = new ArrayList<>();
        for (MCPTool mcpTool : mcpTools) {
            MCPToolBridge bridge = new MCPToolBridge(client, mcpTool, serverName);
            toolRegistry.registerTool(bridge);
            registeredNames.add(bridge.getName());
        }

        serverToolNames.put(serverName, registeredNames);

        // notify CodingAgent to invalidate cached tool definitions
        if (onToolListChanged != null) {
            onToolListChanged.run();
        }

        return registeredNames.size();
    }

    /**
     * 服务器连接摘要
     */
    public record ServerSummary(String name, String status, String protocolVersion, int toolCount) {
    }
}
