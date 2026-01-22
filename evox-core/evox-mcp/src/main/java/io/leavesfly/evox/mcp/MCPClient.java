package io.leavesfly.evox.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * MCP客户端
 * 连接到MCP服务器并使用其资源、工具和提示
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
     * 连接的服务器（本地模式）
     */
    private MCPServer connectedServer;

    /**
     * 传输层（远程模式）
     */
    private MCPTransport transport;

    /**
     * 会话
     */
    private MCPSession session;

    /**
     * 客户端信息
     */
    private MCPProtocol.ClientInfo clientInfo;

    /**
     * 客户端能力
     */
    private MCPProtocol.ClientCapabilities clientCapabilities;

    /**
     * 请求超时时间（毫秒）
     */
    private long requestTimeoutMs = 30000;

    /**
     * JSON序列化器
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MCPClient(String serverUrl) {
        this.serverUrl = serverUrl;
        this.status = ConnectionStatus.DISCONNECTED;
        this.session = new MCPSession();
        this.clientInfo = MCPProtocol.ClientInfo.builder()
                .name("EvoX-MCP-Client")
                .version("1.0.0")
                .build();
        this.clientCapabilities = MCPProtocol.ClientCapabilities.builder()
                .roots(MCPProtocol.RootsCapability.builder().listChanged(true).build())
                .build();
    }

    /**
     * 创建带传输层的客户端
     */
    public MCPClient(MCPTransport transport) {
        this.transport = transport;
        this.status = ConnectionStatus.DISCONNECTED;
        this.session = new MCPSession();
        this.clientInfo = MCPProtocol.ClientInfo.builder()
                .name("EvoX-MCP-Client")
                .version("1.0.0")
                .build();
        this.clientCapabilities = MCPProtocol.ClientCapabilities.builder()
                .roots(MCPProtocol.RootsCapability.builder().listChanged(true).build())
                .build();
    }

    /**
     * 连接到服务器（本地模式）
     */
    public void connect(MCPServer server) {
        if (server == null) {
            throw new IllegalArgumentException("服务器不能为空");
        }

        log.info("连接到MCP服务器: {}", server.getName());
        this.connectedServer = server;
        this.status = ConnectionStatus.CONNECTED;

        // 执行初始化握手
        initializeLocal(server);
    }

    /**
     * 本地初始化
     */
    private void initializeLocal(MCPServer server) {
        session.markInitializing();
        
        MCPProtocol.InitializeParams params = MCPProtocol.InitializeParams.builder()
                .protocolVersion(MCPProtocol.PROTOCOL_VERSION)
                .clientInfo(clientInfo)
                .capabilities(clientCapabilities)
                .build();

        MCPProtocol.InitializeResult result = server.handleInitialize(params);
        session.markInitialized(result);
        
        log.info("初始化完成, 服务器: {}, 协议版本: {}", 
                result.getServerInfo() != null ? result.getServerInfo().getName() : "unknown",
                result.getProtocolVersion());
    }

    /**
     * 连接到远程服务器
     */
    public CompletableFuture<Void> connectRemote() {
        if (transport == null) {
            throw MCPException.connectionError("未配置传输层");
        }

        return CompletableFuture.runAsync(() -> {
            try {
                log.info("连接到远程MCP服务器...");
                this.status = ConnectionStatus.CONNECTING;
                
                transport.start();
                initializeRemote();
                
                this.status = ConnectionStatus.CONNECTED;
                log.info("连接成功");
            } catch (Exception e) {
                this.status = ConnectionStatus.ERROR;
                throw MCPException.connectionError("连接失败", e);
            }
        });
    }

    /**
     * 远程初始化
     */
    private void initializeRemote() {
        session.markInitializing();
        
        MCPProtocol.InitializeParams params = MCPProtocol.InitializeParams.builder()
                .protocolVersion(MCPProtocol.PROTOCOL_VERSION)
                .clientInfo(clientInfo)
                .capabilities(clientCapabilities)
                .build();

        try {
            MCPProtocol.Request request = MCPProtocol.Request.builder()
                    .id(session.nextRequestId())
                    .method(MCPProtocol.Methods.INITIALIZE)
                    .params(params)
                    .build();

            String requestJson = objectMapper.writeValueAsString(request);
            CompletableFuture<String> future = transport.sendAndReceive(requestJson);
            
            String responseJson = future.get(requestTimeoutMs, TimeUnit.MILLISECONDS);
            MCPProtocol.Response response = objectMapper.readValue(responseJson, MCPProtocol.Response.class);
            
            if (response.getError() != null) {
                throw new MCPException(response.getError());
            }

            MCPProtocol.InitializeResult result = objectMapper.convertValue(
                    response.getResult(), MCPProtocol.InitializeResult.class);
            session.markInitialized(result);

            // 发送initialized通知
            MCPProtocol.Notification notification = MCPProtocol.Notification.builder()
                    .method(MCPProtocol.Methods.INITIALIZED)
                    .build();
            transport.send(objectMapper.writeValueAsString(notification));

        } catch (MCPException e) {
            session.markError(e.getMessage());
            throw e;
        } catch (Exception e) {
            session.markError(e.getMessage());
            throw MCPException.connectionError("初始化失败", e);
        }
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        log.info("断开MCP连接");
        
        if (transport != null) {
            transport.stop();
        }
        
        if (session != null) {
            session.close();
        }
        
        this.connectedServer = null;
        this.status = ConnectionStatus.DISCONNECTED;
    }

    /**
     * 列出可用资源
     */
    public List<MCPResource> listResources() {
        ensureInitialized();
        
        if (connectedServer != null) {
            return connectedServer.listResources();
        }
        
        // 远程调用
        throw new UnsupportedOperationException("远程资源列表尚未实现");
    }

    /**
     * 列出可用工具
     */
    public List<MCPTool> listTools() {
        ensureInitialized();
        
        if (connectedServer != null) {
            return connectedServer.listTools();
        }
        
        throw new UnsupportedOperationException("远程工具列表尚未实现");
    }

    /**
     * 列出可用提示
     */
    public List<MCPPrompt> listPrompts() {
        ensureInitialized();
        
        if (connectedServer != null) {
            return connectedServer.listPrompts();
        }
        
        throw new UnsupportedOperationException("远程提示列表尚未实现");
    }

    /**
     * 读取资源
     */
    public MCPProtocol.ResourceReadResult readResource(String uri) {
        ensureInitialized();
        log.debug("读取资源: {}", uri);
        
        if (connectedServer != null) {
            return connectedServer.readResource(uri);
        }
        
        throw new UnsupportedOperationException("远程资源读取尚未实现");
    }

    /**
     * 调用工具
     */
    public MCPProtocol.ToolCallResult callTool(String toolName, Map<String, Object> arguments) {
        ensureInitialized();
        log.debug("调用工具: {} with args: {}", toolName, arguments);
        
        if (connectedServer != null) {
            return connectedServer.invokeTool(toolName, arguments);
        }
        
        throw new UnsupportedOperationException("远程工具调用尚未实现");
    }

    /**
     * 调用工具(无参数)
     */
    public MCPProtocol.ToolCallResult callTool(String toolName) {
        return callTool(toolName, new HashMap<>());
    }

    /**
     * 获取提示
     */
    public MCPPrompt.GetPromptResult getPrompt(String promptName, Map<String, String> arguments) {
        ensureInitialized();
        log.debug("获取提示: {} with args: {}", promptName, arguments);
        
        if (connectedServer != null) {
            return connectedServer.getPrompt(promptName, arguments);
        }
        
        throw new UnsupportedOperationException("远程提示获取尚未实现");
    }

    /**
     * 获取提示(无参数)
     */
    public MCPPrompt.GetPromptResult getPrompt(String promptName) {
        return getPrompt(promptName, new HashMap<>());
    }

    /**
     * 获取服务器信息
     */
    public MCPServer.ServerInfo getServerInfo() {
        ensureInitialized();
        
        if (connectedServer != null) {
            return connectedServer.getServerInfo();
        }
        
        // 从会话中获取
        MCPProtocol.ServerInfo info = session.getServerInfo();
        if (info != null) {
            return MCPServer.ServerInfo.builder()
                    .name(info.getName())
                    .version(info.getVersion())
                    .build();
        }
        
        return null;
    }

    /**
     * 确保已初始化
     */
    private void ensureInitialized() {
        if (status != ConnectionStatus.CONNECTED) {
            throw MCPException.connectionError("未连接到MCP服务器");
        }
        if (session == null || !session.isInitialized()) {
            throw MCPException.sessionNotInitialized();
        }
    }

    /**
     * 检查是否已初始化
     */
    public boolean isInitialized() {
        return status == ConnectionStatus.CONNECTED && 
               session != null && session.isInitialized();
    }

    /**
     * 获取协议版本
     */
    public String getProtocolVersion() {
        return session != null ? session.getProtocolVersion() : null;
    }

    /**
     * 获取服务器能力
     */
    public MCPProtocol.ServerCapabilities getServerCapabilities() {
        return session != null ? session.getServerCapabilities() : null;
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

    /**
     * 构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String serverUrl;
        private MCPTransport transport;
        private MCPProtocol.ClientInfo clientInfo;
        private long requestTimeoutMs = 30000;

        public Builder serverUrl(String serverUrl) {
            this.serverUrl = serverUrl;
            return this;
        }

        public Builder transport(MCPTransport transport) {
            this.transport = transport;
            return this;
        }

        public Builder clientInfo(String name, String version) {
            this.clientInfo = MCPProtocol.ClientInfo.builder()
                    .name(name)
                    .version(version)
                    .build();
            return this;
        }

        public Builder requestTimeout(long timeoutMs) {
            this.requestTimeoutMs = timeoutMs;
            return this;
        }

        public MCPClient build() {
            MCPClient client;
            if (transport != null) {
                client = new MCPClient(transport);
            } else {
                client = new MCPClient(serverUrl);
            }
            
            if (clientInfo != null) {
                client.setClientInfo(clientInfo);
            }
            client.setRequestTimeoutMs(requestTimeoutMs);
            
            return client;
        }
    }
}
