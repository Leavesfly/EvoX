package io.leavesfly.evox.mcp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * MCP服务器
 * 管理资源、工具和提示的注册与调用
 *
 * @author EvoX Team
 */
public class MCPServer {

    private final String name;
    private final String version;
    private final String instructions;
    private final Map<String, MCPResource> resources;
    private final Map<String, MCPTool> tools;
    private final Map<String, MCPPrompt> prompts;

    public MCPServer(String name, String version, String instructions) {
        this.name = name;
        this.version = version;
        this.instructions = instructions;
        this.resources = new HashMap<>();
        this.tools = new HashMap<>();
        this.prompts = new HashMap<>();
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getInstructions() {
        return instructions;
    }

    /**
     * 注册资源
     */
    public void registerResource(MCPResource resource) {
        resources.put(resource.getUri(), resource);
    }

    /**
     * 获取资源
     */
    public MCPResource getResource(String uri) {
        return resources.get(uri);
    }

    /**
     * 注册工具
     */
    public void registerTool(MCPTool tool) {
        tools.put(tool.getName(), tool);
    }

    /**
     * 获取工具
     */
    public MCPTool getTool(String name) {
        return tools.get(name);
    }

    /**
     * 调用工具
     */
    public MCPProtocol.ToolCallResult invokeTool(String name, Map<String, Object> args) {
        MCPTool tool = tools.get(name);
        if (tool == null) {
            throw MCPException.toolNotFound(name);
        }

        try {
            Object result = tool.getExecutor().execute(args);
            return MCPProtocol.ToolCallResult.builder()
                    .content(List.of(
                            MCPProtocol.Content.builder()
                                    .type("text")
                                    .text(result != null ? result.toString() : "")
                                    .build()
                    ))
                    .isError(false)
                    .build();
        } catch (Exception e) {
            return MCPProtocol.ToolCallResult.builder()
                    .content(List.of(
                            MCPProtocol.Content.builder()
                                    .type("text")
                                    .text("工具执行错误: " + e.getMessage())
                                    .build()
                    ))
                    .isError(true)
                    .build();
        }
    }

    /**
     * 注册提示
     */
    public void registerPrompt(MCPPrompt prompt) {
        prompts.put(prompt.getName(), prompt);
    }

    /**
     * 获取提示
     */
    public MCPPrompt getPrompt(String name) {
        return prompts.get(name);
    }

    /**
     * 获取提示内容
     */
    public MCPPrompt.GetPromptResult getPrompt(String name, Map<String, String> args) {
        MCPPrompt prompt = prompts.get(name);
        if (prompt == null) {
            throw MCPException.promptNotFound(name);
        }
        return prompt.generate(args);
    }

    /**
     * 列出所有资源
     */
    public List<MCPResource> listResources() {
        return List.copyOf(resources.values());
    }

    /**
     * 列出所有工具
     */
    public List<MCPTool> listTools() {
        return List.copyOf(tools.values());
    }

    /**
     * 获取服务器信息
     */
    public ServerInfo getServerInfo() {
        return ServerInfo.builder()
                .name(name)
                .version(version)
                .resourceCount(resources.size())
                .toolCount(tools.size())
                .build();
    }

    /**
     * 服务器信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServerInfo {
        private String name;
        private String version;
        private int resourceCount;
        private int toolCount;
    }
}
