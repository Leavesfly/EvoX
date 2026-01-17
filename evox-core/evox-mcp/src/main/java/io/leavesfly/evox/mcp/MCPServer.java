package io.leavesfly.evox.mcp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP服务器
 * 提供资源和工具的暴露接口
 *
 * @author EvoX Team
 */
@Slf4j
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MCPServer {

    /**
     * 服务器名称
     */
    private String name;

    /**
     * 服务器版本
     */
    private String version;

    /**
     * 资源注册表
     */
    private Map<String, MCPResource> resources;

    /**
     * 工具注册表
     */
    private Map<String, MCPTool> tools;

    public MCPServer(String name, String version) {
        this.name = name;
        this.version = version;
        this.resources = new HashMap<>();
        this.tools = new HashMap<>();
    }

    /**
     * 注册资源
     */
    public void registerResource(MCPResource resource) {
        if (resource == null || resource.getUri() == null) {
            throw new IllegalArgumentException("资源或资源URI不能为空");
        }

        resources.put(resource.getUri(), resource);
        log.info("注册MCP资源: {} - {}", resource.getUri(), resource.getName());
    }

    /**
     * 注册工具
     */
    public void registerTool(MCPTool tool) {
        if (tool == null || tool.getName() == null) {
            throw new IllegalArgumentException("工具或工具名称不能为空");
        }

        tools.put(tool.getName(), tool);
        log.info("注册MCP工具: {} - {}", tool.getName(), tool.getDescription());
    }

    /**
     * 获取资源
     */
    public MCPResource getResource(String uri) {
        return resources.get(uri);
    }

    /**
     * 获取工具
     */
    public MCPTool getTool(String name) {
        return tools.get(name);
    }

    /**
     * 列出所有资源
     */
    public List<MCPResource> listResources() {
        return new ArrayList<>(resources.values());
    }

    /**
     * 列出所有工具
     */
    public List<MCPTool> listTools() {
        return new ArrayList<>(tools.values());
    }

    /**
     * 调用工具
     */
    public Object invokeTool(String toolName, Map<String, Object> arguments) {
        MCPTool tool = tools.get(toolName);
        if (tool == null) {
            throw new IllegalArgumentException("工具不存在: " + toolName);
        }

        if (tool.getExecutor() == null) {
            throw new IllegalStateException("工具未配置执行器: " + toolName);
        }

        log.debug("调用MCP工具: {} with arguments: {}", toolName, arguments);

        try {
            return tool.getExecutor().execute(arguments);
        } catch (Exception e) {
            log.error("工具调用失败: {}", toolName, e);
            throw new RuntimeException("工具调用失败: " + toolName, e);
        }
    }

    /**
     * 读取资源内容
     */
    public Object readResource(String uri) {
        MCPResource resource = resources.get(uri);
        if (resource == null) {
            throw new IllegalArgumentException("资源不存在: " + uri);
        }

        log.debug("读取MCP资源: {}", uri);

        // 实际应该根据资源类型读取内容
        return resource.getMetadata();
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
    @lombok.Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServerInfo {
        private String name;
        private String version;
        private int resourceCount;
        private int toolCount;
    }
}
