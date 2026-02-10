package io.leavesfly.evox.mcp.runtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.leavesfly.evox.mcp.MCPException;
import io.leavesfly.evox.mcp.MCPProtocol;
import io.leavesfly.evox.mcp.MCPPrompt;
import io.leavesfly.evox.mcp.MCPResource;
import io.leavesfly.evox.mcp.MCPTool;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * MCP服务器
 * 提供资源、工具和提示的暴露接口
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
     * 服务器说明
     */
    private String instructions;

    /**
     * 资源注册表
     */
    private Map<String, MCPResource> resources;

    /**
     * 工具注册表
     */
    private Map<String, MCPTool> tools;

    /**
     * 提示注册表
     */
    private Map<String, MCPPrompt> prompts;

    /**
     * 资源内容读取器
     */
    private Map<String, Function<String, MCPProtocol.ResourceContent>> resourceReaders;

    /**
     * 服务器能力配置
     */
    private MCPProtocol.ServerCapabilities capabilities;

    /**
     * 资源订阅者映射 (uri -> 订阅者列表)
     */
    private final Map<String, Set<Consumer<String>>> resourceSubscribers = new ConcurrentHashMap<>();

    /**
     * 采样处理器
     */
    private Function<MCPProtocol.SamplingCreateMessageParams, MCPProtocol.SamplingCreateMessageResult> samplingHandler;

    /**
     * 日志级别
     */
    private MCPProtocol.LogLevel logLevel = MCPProtocol.LogLevel.INFO;

    /**
     * 日志处理器
     */
    private Consumer<MCPProtocol.LoggingMessageParams> logHandler;

    /**
     * JSON序列化器
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MCPServer(String name, String version) {
        this.name = name;
        this.version = version;
        this.resources = new HashMap<>();
        this.tools = new HashMap<>();
        this.prompts = new HashMap<>();
        this.resourceReaders = new HashMap<>();
        this.capabilities = buildDefaultCapabilities();
    }

    public MCPServer(String name, String version, String instructions) {
        this(name, version);
        this.instructions = instructions;
    }

    /**
     * 构建默认能力配置
     */
    private MCPProtocol.ServerCapabilities buildDefaultCapabilities() {
        return MCPProtocol.ServerCapabilities.builder()
                .resources(MCPProtocol.ResourcesCapability.builder()
                        .subscribe(true)  // 启用资源订阅
                        .listChanged(true)
                        .build())
                .tools(MCPProtocol.ToolsCapability.builder()
                        .listChanged(true)
                        .build())
                .prompts(MCPProtocol.PromptsCapability.builder()
                        .listChanged(true)
                        .build())
                .logging(MCPProtocol.LoggingCapability.builder().build())
                .build();
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
     * 注册资源（带内容读取器）
     */
    public void registerResource(MCPResource resource, 
                                  Function<String, MCPProtocol.ResourceContent> reader) {
        registerResource(resource);
        if (reader != null) {
            resourceReaders.put(resource.getUri(), reader);
        }
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
     * 注册提示
     */
    public void registerPrompt(MCPPrompt prompt) {
        if (prompt == null || prompt.getName() == null) {
            throw new IllegalArgumentException("提示或提示名称不能为空");
        }

        prompts.put(prompt.getName(), prompt);
        log.info("注册MCP提示: {} - {}", prompt.getName(), prompt.getDescription());
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
     * 获取提示
     */
    public MCPPrompt getPrompt(String name) {
        return prompts.get(name);
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
     * 列出所有提示
     */
    public List<MCPPrompt> listPrompts() {
        return new ArrayList<>(prompts.values());
    }

    /**
     * 调用工具
     */
    public MCPProtocol.ToolCallResult invokeTool(String toolName, Map<String, Object> arguments) {
        MCPTool tool = tools.get(toolName);
        if (tool == null) {
            throw MCPException.toolNotFound(toolName);
        }

        if (tool.getExecutor() == null) {
            throw MCPException.internalError("工具未配置执行器: " + toolName);
        }

        log.debug("调用MCP工具: {} with arguments: {}", toolName, arguments);

        try {
            Object result = tool.getExecutor().execute(arguments);
            
            // 转换为标准结果格式
            List<MCPProtocol.Content> contents = new ArrayList<>();
            if (result instanceof String) {
                contents.add(MCPProtocol.Content.builder()
                        .type("text")
                        .text((String) result)
                        .build());
            } else if (result instanceof MCPProtocol.Content) {
                contents.add((MCPProtocol.Content) result);
            } else if (result instanceof List) {
                for (Object item : (List<?>) result) {
                    if (item instanceof MCPProtocol.Content) {
                        contents.add((MCPProtocol.Content) item);
                    } else {
                        contents.add(MCPProtocol.Content.builder()
                                .type("text")
                                .text(String.valueOf(item))
                                .build());
                    }
                }
            } else {
                // 其他类型转JSON
                String text;
                try {
                    text = objectMapper.writeValueAsString(result);
                } catch (JsonProcessingException e) {
                    text = String.valueOf(result);
                }
                contents.add(MCPProtocol.Content.builder()
                        .type("text")
                        .text(text)
                        .build());
            }
            
            return MCPProtocol.ToolCallResult.builder()
                    .content(contents)
                    .isError(false)
                    .build();
        } catch (Exception e) {
            log.error("工具调用失败: {}", toolName, e);
            
            List<MCPProtocol.Content> errorContents = new ArrayList<>();
            errorContents.add(MCPProtocol.Content.builder()
                    .type("text")
                    .text("工具调用失败: " + e.getMessage())
                    .build());
            
            return MCPProtocol.ToolCallResult.builder()
                    .content(errorContents)
                    .isError(true)
                    .build();
        }
    }

    /**
     * 读取资源内容
     */
    public MCPProtocol.ResourceReadResult readResource(String uri) {
        MCPResource resource = resources.get(uri);
        if (resource == null) {
            throw MCPException.resourceNotFound(uri);
        }

        log.debug("读取MCP资源: {}", uri);

        // 使用注册的读取器
        Function<String, MCPProtocol.ResourceContent> reader = resourceReaders.get(uri);
        MCPProtocol.ResourceContent content;
        
        if (reader != null) {
            content = reader.apply(uri);
        } else {
            // 默认返回元数据作为文本
            String text = "";
            if (resource.getMetadata() != null) {
                try {
                    text = objectMapper.writeValueAsString(resource.getMetadata());
                } catch (JsonProcessingException e) {
                    text = resource.getMetadata().toString();
                }
            }
            content = MCPProtocol.ResourceContent.builder()
                    .uri(uri)
                    .mimeType(resource.getMimeType() != null ? resource.getMimeType() : "application/json")
                    .text(text)
                    .build();
        }

        List<MCPProtocol.ResourceContent> contents = new ArrayList<>();
        contents.add(content);
        return MCPProtocol.ResourceReadResult.builder()
                .contents(contents)
                .build();
    }

    /**
     * 获取提示内容
     */
    public MCPPrompt.GetPromptResult getPrompt(String name, Map<String, String> arguments) {
        MCPPrompt prompt = prompts.get(name);
        if (prompt == null) {
            throw MCPException.promptNotFound(name);
        }

        log.debug("获取MCP提示: {} with arguments: {}", name, arguments);
        return prompt.generate(arguments);
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
                .promptCount(prompts.size())
                .build();
    }

    /**
     * 处理初始化请求
     */
    public MCPProtocol.InitializeResult handleInitialize(MCPProtocol.InitializeParams params) {
        log.info("处理初始化请求, 客户端: {}, 协议版本: {}", 
                params.getClientInfo() != null ? params.getClientInfo().getName() : "unknown",
                params.getProtocolVersion());

        return MCPProtocol.InitializeResult.builder()
                .protocolVersion(MCPProtocol.PROTOCOL_VERSION)
                .capabilities(capabilities)
                .serverInfo(MCPProtocol.ServerInfo.builder()
                        .name(name)
                        .version(version)
                        .build())
                .instructions(instructions)
                .build();
    }

    // ============= 资源订阅功能 =============

    /**
     * 订阅资源变更
     */
    public void subscribeResource(String uri, Consumer<String> subscriber) {
        if (uri == null || subscriber == null) {
            throw new IllegalArgumentException("URI和订阅者不能为空");
        }

        if (!resources.containsKey(uri)) {
            throw MCPException.resourceNotFound(uri);
        }

        resourceSubscribers.computeIfAbsent(uri, k -> ConcurrentHashMap.newKeySet())
                .add(subscriber);
        
        log.info("添加资源订阅: {}", uri);
    }

    /**
     * 取消资源订阅
     */
    public void unsubscribeResource(String uri, Consumer<String> subscriber) {
        Set<Consumer<String>> subscribers = resourceSubscribers.get(uri);
        if (subscribers != null) {
            subscribers.remove(subscriber);
            log.info("取消资源订阅: {}", uri);
        }
    }

    /**
     * 通知资源更新
     */
    public void notifyResourceUpdated(String uri) {
        Set<Consumer<String>> subscribers = resourceSubscribers.get(uri);
        if (subscribers != null && !subscribers.isEmpty()) {
            log.debug("通知资源更新: {}, 订阅者数量: {}", uri, subscribers.size());
            
            for (Consumer<String> subscriber : subscribers) {
                try {
                    subscriber.accept(uri);
                } catch (Exception e) {
                    log.error("通知资源更新失败: {}", uri, e);
                }
            }
        }
    }

    /**
     * 获取资源订阅者数量
     */
    public int getResourceSubscriberCount(String uri) {
        Set<Consumer<String>> subscribers = resourceSubscribers.get(uri);
        return subscribers != null ? subscribers.size() : 0;
    }

    // ============= 采样能力 =============

    /**
     * 设置采样处理器
     */
    public void setSamplingHandler(
            Function<MCPProtocol.SamplingCreateMessageParams, MCPProtocol.SamplingCreateMessageResult> handler) {
        this.samplingHandler = handler;
        log.info("设置采样处理器");
    }

    /**
     * 执行采样请求
     */
    public MCPProtocol.SamplingCreateMessageResult handleSampling(
            MCPProtocol.SamplingCreateMessageParams params) {
        if (samplingHandler == null) {
            throw MCPException.internalError("未配置采样处理器");
        }

        log.debug("处理采样请求, 消息数: {}", 
                params.getMessages() != null ? params.getMessages().size() : 0);
        
        try {
            return samplingHandler.apply(params);
        } catch (Exception e) {
            log.error("采样处理失败", e);
            throw MCPException.internalError("采样处理失败: " + e.getMessage());
        }
    }

    /**
     * 检查是否支持采样
     */
    public boolean supportsSampling() {
        return samplingHandler != null;
    }

    // ============= 日志能力 =============

    /**
     * 设置日志级别
     */
    public void setLogLevel(MCPProtocol.LogLevel level) {
        this.logLevel = level;
        log.info("设置日志级别: {}", level);
    }

    /**
     * 获取日志级别
     */
    public MCPProtocol.LogLevel getLogLevel() {
        return logLevel;
    }

    /**
     * 设置日志处理器
     */
    public void setLogHandler(Consumer<MCPProtocol.LoggingMessageParams> handler) {
        this.logHandler = handler;
    }

    /**
     * 发送日志消息
     */
    public void sendLog(MCPProtocol.LogLevel level, String logger, String message) {
        if (shouldLog(level) && logHandler != null) {
            MCPProtocol.LoggingMessageParams params = MCPProtocol.LoggingMessageParams.builder()
                    .level(level)
                    .logger(logger)
                    .data(message)
                    .build();
            
            try {
                logHandler.accept(params);
            } catch (Exception e) {
                log.error("发送日志失败", e);
            }
        }
    }

    /**
     * 检查是否应该记录日志
     */
    private boolean shouldLog(MCPProtocol.LogLevel level) {
        return level.ordinal() >= logLevel.ordinal();
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
        private int promptCount;
    }

    /**
     * 构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String version;
        private String instructions;
        private final Map<String, MCPResource> resources = new HashMap<>();
        private final Map<String, MCPTool> tools = new HashMap<>();
        private final Map<String, MCPPrompt> prompts = new HashMap<>();

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder instructions(String instructions) {
            this.instructions = instructions;
            return this;
        }

        public Builder resource(MCPResource resource) {
            this.resources.put(resource.getUri(), resource);
            return this;
        }

        public Builder tool(MCPTool tool) {
            this.tools.put(tool.getName(), tool);
            return this;
        }

        public Builder prompt(MCPPrompt prompt) {
            this.prompts.put(prompt.getName(), prompt);
            return this;
        }

        public MCPServer build() {
            MCPServer server = new MCPServer(name, version, instructions);
            server.resources.putAll(resources);
            server.tools.putAll(tools);
            server.prompts.putAll(prompts);
            return server;
        }
    }
}
