package io.leavesfly.evox.mcp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MCP模块单元测试
 *
 * @author EvoX Team
 */
@DisplayName("MCP模块测试")
class MCPTest {

    private MCPServer server;
    private MCPClient client;

    @BeforeEach
    void setUp() {
        server = new MCPServer("TestServer", "1.0.0", "测试服务器");
        client = new MCPClient("http://localhost:8080");
    }

    // ============= MCPServer 测试 =============

    @Test
    @DisplayName("服务器创建和基本信息")
    void testServerCreation() {
        assertNotNull(server);
        assertEquals("TestServer", server.getName());
        assertEquals("1.0.0", server.getVersion());
        assertEquals("测试服务器", server.getInstructions());
    }

    @Test
    @DisplayName("注册和获取资源")
    void testResourceRegistration() {
        MCPResource resource = MCPResource.builder()
                .uri("file:///test.txt")
                .name("测试文件")
                .description("一个测试文件")
                .type(MCPResource.ResourceType.FILE)
                .mimeType("text/plain")
                .build();

        server.registerResource(resource);

        MCPResource retrieved = server.getResource("file:///test.txt");
        assertNotNull(retrieved);
        assertEquals("测试文件", retrieved.getName());
        assertEquals(MCPResource.ResourceType.FILE, retrieved.getType());
    }

    @Test
    @DisplayName("注册和获取工具")
    void testToolRegistration() {
        MCPTool tool = MCPTool.builder()
                .name("calculator")
                .description("计算器工具")
                .executor(args -> {
                    int a = (int) args.getOrDefault("a", 0);
                    int b = (int) args.getOrDefault("b", 0);
                    return a + b;
                })
                .build();

        server.registerTool(tool);

        MCPTool retrieved = server.getTool("calculator");
        assertNotNull(retrieved);
        assertEquals("calculator", retrieved.getName());
        assertEquals("计算器工具", retrieved.getDescription());
    }

    @Test
    @DisplayName("调用工具")
    void testToolInvocation() {
        MCPTool tool = MCPTool.builder()
                .name("adder")
                .description("加法工具")
                .executor(args -> {
                    int a = ((Number) args.getOrDefault("a", 0)).intValue();
                    int b = ((Number) args.getOrDefault("b", 0)).intValue();
                    return String.valueOf(a + b);
                })
                .build();

        server.registerTool(tool);

        Map<String, Object> args = new HashMap<>();
        args.put("a", 5);
        args.put("b", 3);

        MCPProtocol.ToolCallResult result = server.invokeTool("adder", args);
        assertNotNull(result);
        assertFalse(result.isError());
        assertEquals(1, result.getContent().size());
        assertEquals("8", result.getContent().get(0).getText());
    }

    @Test
    @DisplayName("工具不存在异常")
    void testToolNotFound() {
        assertThrows(MCPException.class, () -> {
            server.invokeTool("nonexistent", new HashMap<>());
        });
    }

    @Test
    @DisplayName("注册和获取提示")
    void testPromptRegistration() {
        MCPPrompt prompt = MCPPrompt.builder()
                .name("greeting")
                .description("问候提示")
                .generator(args -> {
                    String name = args.getOrDefault("name", "用户");
                    return MCPPrompt.GetPromptResult.builder()
                            .description("问候语")
                            .messages(List.of(
                                    MCPPrompt.PromptMessage.builder()
                                            .role("user")
                                            .content(MCPPrompt.Content.text("你好, " + name))
                                            .build()
                            ))
                            .build();
                })
                .build();

        server.registerPrompt(prompt);

        MCPPrompt retrieved = server.getPrompt("greeting");
        assertNotNull(retrieved);
        assertEquals("greeting", retrieved.getName());
    }

    @Test
    @DisplayName("获取提示内容")
    void testGetPromptContent() {
        MCPPrompt prompt = MCPPrompt.simple("test", "测试提示", "你好 {name}!");
        server.registerPrompt(prompt);

        Map<String, String> args = new HashMap<>();
        args.put("name", "张三");

        MCPPrompt.GetPromptResult result = server.getPrompt("test", args);
        assertNotNull(result);
        assertFalse(result.getMessages().isEmpty());
    }

    @Test
    @DisplayName("列出所有资源和工具")
    void testListing() {
        // 添加测试数据
        server.registerResource(MCPResource.builder()
                .uri("file:///a.txt").name("文件A").build());
        server.registerResource(MCPResource.builder()
                .uri("file:///b.txt").name("文件B").build());
        server.registerTool(MCPTool.builder()
                .name("tool1").description("工具1").build());

        List<MCPResource> resources = server.listResources();
        List<MCPTool> tools = server.listTools();

        assertEquals(2, resources.size());
        assertEquals(1, tools.size());
    }

    @Test
    @DisplayName("服务器信息")
    void testServerInfo() {
        server.registerResource(MCPResource.builder()
                .uri("file:///test.txt").name("测试").build());
        server.registerTool(MCPTool.builder()
                .name("tool").description("工具").build());

        MCPServer.ServerInfo info = server.getServerInfo();
        assertNotNull(info);
        assertEquals("TestServer", info.getName());
        assertEquals("1.0.0", info.getVersion());
        assertEquals(1, info.getResourceCount());
        assertEquals(1, info.getToolCount());
    }

    // ============= MCPClient 测试 =============

    @Test
    @DisplayName("客户端创建")
    void testClientCreation() {
        assertNotNull(client);
        assertEquals(MCPClient.ConnectionStatus.DISCONNECTED, client.getStatus());
    }

    @Test
    @DisplayName("客户端连接到服务器")
    void testClientConnect() {
        client.connect(server);

        assertEquals(MCPClient.ConnectionStatus.CONNECTED, client.getStatus());
        assertTrue(client.isInitialized());
    }

    @Test
    @DisplayName("通过客户端列出资源")
    void testClientListResources() {
        server.registerResource(MCPResource.builder()
                .uri("file:///test.txt").name("测试文件").build());

        client.connect(server);

        List<MCPResource> resources = client.listResources();
        assertEquals(1, resources.size());
        assertEquals("测试文件", resources.get(0).getName());
    }

    @Test
    @DisplayName("通过客户端调用工具")
    void testClientCallTool() {
        server.registerTool(MCPTool.builder()
                .name("echo")
                .description("回声工具")
                .executor(args -> args.getOrDefault("message", ""))
                .build());

        client.connect(server);

        Map<String, Object> args = new HashMap<>();
        args.put("message", "Hello MCP!");

        MCPProtocol.ToolCallResult result = client.callTool("echo", args);
        assertNotNull(result);
        assertFalse(result.isError());
    }

    @Test
    @DisplayName("客户端断开连接")
    void testClientDisconnect() {
        client.connect(server);
        assertEquals(MCPClient.ConnectionStatus.CONNECTED, client.getStatus());

        client.disconnect();
        assertEquals(MCPClient.ConnectionStatus.DISCONNECTED, client.getStatus());
    }

    @Test
    @DisplayName("未连接时操作应抛出异常")
    void testOperationWithoutConnection() {
        assertThrows(MCPException.class, () -> {
            client.listResources();
        });
    }

    // ============= MCPSession 测试 =============

    @Test
    @DisplayName("会话创建和状态")
    void testSessionCreation() {
        MCPSession session = new MCPSession();

        assertNotNull(session.getSessionId());
        assertEquals(MCPSession.SessionState.CREATED, session.getState());
        assertFalse(session.isInitialized());
    }

    @Test
    @DisplayName("会话状态转换")
    void testSessionStateTransition() {
        MCPSession session = new MCPSession();

        session.markInitializing();
        assertEquals(MCPSession.SessionState.INITIALIZING, session.getState());

        MCPProtocol.InitializeResult result = MCPProtocol.InitializeResult.builder()
                .protocolVersion("2024-11-05")
                .serverInfo(MCPProtocol.ServerInfo.builder()
                        .name("TestServer")
                        .version("1.0")
                        .build())
                .build();

        session.markInitialized(result);
        assertEquals(MCPSession.SessionState.INITIALIZED, session.getState());
        assertTrue(session.isInitialized());
    }

    @Test
    @DisplayName("会话请求ID生成")
    void testSessionRequestIdGeneration() {
        MCPSession session = new MCPSession();

        long id1 = session.nextRequestId();
        long id2 = session.nextRequestId();
        long id3 = session.nextRequestId();

        assertEquals(1, id1);
        assertEquals(2, id2);
        assertEquals(3, id3);
    }

    // ============= MCPProtocol 测试 =============

    @Test
    @DisplayName("协议版本常量")
    void testProtocolConstants() {
        assertEquals("2024-11-05", MCPProtocol.PROTOCOL_VERSION);
        assertEquals("2.0", MCPProtocol.JSONRPC_VERSION);
    }

    @Test
    @DisplayName("请求消息构建")
    void testRequestMessage() {
        MCPProtocol.Request request = MCPProtocol.Request.builder()
                .jsonrpc(MCPProtocol.JSONRPC_VERSION)
                .id(1L)
                .method(MCPProtocol.Methods.TOOLS_LIST)
                .build();

        assertEquals(1L, request.getId());
        assertEquals(MCPProtocol.Methods.TOOLS_LIST, request.getMethod());
        assertEquals("2.0", request.getJsonrpc());
    }

    @Test
    @DisplayName("响应消息构建")
    void testResponseMessage() {
        MCPProtocol.Response response = MCPProtocol.Response.builder()
                .id(1L)
                .result(Map.of("tools", List.of()))
                .build();

        assertEquals(1L, response.getId());
        assertNotNull(response.getResult());
        assertNull(response.getError());
    }

    @Test
    @DisplayName("错误消息构建")
    void testErrorMessage() {
        MCPProtocol.Error error = MCPProtocol.Error.builder()
                .code(MCPProtocol.ErrorCodes.METHOD_NOT_FOUND)
                .message("方法未找到")
                .build();

        assertEquals(MCPProtocol.ErrorCodes.METHOD_NOT_FOUND, error.getCode());
        assertEquals("方法未找到", error.getMessage());
    }

    // ============= MCPPrompt 测试 =============

    @Test
    @DisplayName("简单提示创建")
    void testSimplePrompt() {
        MCPPrompt prompt = MCPPrompt.simple("greeting", "问候提示", "你好 {name}!");

        assertNotNull(prompt);
        assertEquals("greeting", prompt.getName());

        Map<String, String> args = new HashMap<>();
        args.put("name", "世界");

        MCPPrompt.GetPromptResult result = prompt.generate(args);
        assertNotNull(result);
        assertFalse(result.getMessages().isEmpty());
        assertTrue(result.getMessages().get(0).getContent().getText().contains("世界"));
    }

    @Test
    @DisplayName("系统提示创建")
    void testSystemPrompt() {
        MCPPrompt prompt = MCPPrompt.systemPrompt(
                "assistant",
                "AI助手提示",
                "你是一个有帮助的AI助手。",
                "请帮我 {task}"
        );

        Map<String, String> args = new HashMap<>();
        args.put("task", "写代码");

        MCPPrompt.GetPromptResult result = prompt.generate(args);
        assertEquals(2, result.getMessages().size());
        assertEquals("system", result.getMessages().get(0).getRole());
        assertEquals("user", result.getMessages().get(1).getRole());
    }

    @Test
    @DisplayName("提示参数验证")
    void testPromptArgumentValidation() {
        MCPPrompt prompt = MCPPrompt.builder()
                .name("test")
                .description("测试提示")
                .build();

        prompt.addArgument("required_arg", "必需参数", true);

        assertThrows(IllegalArgumentException.class, () -> {
            prompt.generate(new HashMap<>());
        });
    }

    // ============= MCPException 测试 =============

    @Test
    @DisplayName("异常工厂方法")
    void testExceptionFactoryMethods() {
        MCPException parseError = MCPException.parseError("解析失败");
        assertEquals(MCPProtocol.ErrorCodes.PARSE_ERROR, parseError.getErrorCode());

        MCPException methodNotFound = MCPException.methodNotFound("test");
        assertEquals(MCPProtocol.ErrorCodes.METHOD_NOT_FOUND, methodNotFound.getErrorCode());

        MCPException resourceNotFound = MCPException.resourceNotFound("file:///test");
        assertEquals(MCPProtocol.ErrorCodes.RESOURCE_NOT_FOUND, resourceNotFound.getErrorCode());
    }

    @Test
    @DisplayName("异常转协议错误")
    void testExceptionToProtocolError() {
        MCPException exception = MCPException.toolNotFound("calculator");

        MCPProtocol.Error error = exception.toProtocolError();
        assertEquals(MCPProtocol.ErrorCodes.TOOL_NOT_FOUND, error.getCode());
        assertTrue(error.getMessage().contains("calculator"));
    }

    // ============= MCPTransport 测试 =============

    @Test
    @DisplayName("内存传输测试")
    void testInMemoryTransport() throws Exception {
        MCPTransport.InMemoryTransport transport1 = new MCPTransport.InMemoryTransport();
        MCPTransport.InMemoryTransport transport2 = new MCPTransport.InMemoryTransport();

        transport1.connectTo(transport2);

        transport1.start();
        transport2.start();

        assertTrue(transport1.isConnected());
        assertTrue(transport2.isConnected());

        // 测试消息传递
        final String[] received = {null};
        transport2.setMessageHandler(msg -> received[0] = msg);

        transport1.send("Hello!");

        assertEquals("Hello!", received[0]);

        transport1.stop();
        transport2.stop();
    }
}
