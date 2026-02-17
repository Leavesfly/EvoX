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
 * MCP（Model Context Protocol）服务器核心实现
 * 
 * <p>作为 MCP 协议的服务器端实现，提供以下核心能力：</p>
 * <ul>
 *     <li>资源管理：注册、发布和订阅机制，支持动态资源更新通知</li>
 *     <li>工具暴露：将本地工具封装为标准 MCP 工具接口</li>
 *     <li>提示管理：支持可参数化的提示模板生成</li>
 *     <li>采样能力：处理客户端的采样请求</li>
 *     <li>日志系统：分级日志记录和转发机制</li>
 * </ul>
 * 
 * <p>线程安全性：本类使用 ConcurrentHashMap 等线程安全数据结构，
 * 支持多线程并发访问和资源订阅通知</p>
 *
 * @author EvoX Team
 * @see MCPProtocol
 * @see MCPResource
 * @see MCPTool
 * @see MCPPrompt
 */
@Slf4j
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MCPServer {

    // ==================== 基础配置属性 ====================
    
    /**
     * 服务器唯一标识名称
     * <p>用于客户端识别和连接特定服务器实例</p>
     */
    private String name;

    /**
     * 服务器版本号
     * <p>遵循语义化版本规范（Semantic Versioning），格式：主版本号。次版本号。修订号</p>
     */
    private String version;

    /**
     * 服务器使用说明和描述信息
     * <p>提供给客户端了解服务器功能和使用方式</p>
     */
    private String instructions;

    // ==================== 核心组件注册表 ====================
    
    /**
     * 资源注册表（URI -> 资源对象）
     * <p>存储所有已注册的 MCP 资源，支持通过 URI 快速检索</p>
     */
    private Map<String, MCPResource> resources;

    /**
     * 工具注册表（名称 -> 工具对象）
     * <p>存储所有已注册的 MCP 工具，支持通过工具名快速检索</p>
     */
    private Map<String, MCPTool> tools;

    /**
     * 提示注册表（名称 -> 提示对象）
     * <p>存储所有已注册的 MCP 提示模板，支持通过提示名快速检索</p>
     */
    private Map<String, MCPPrompt> prompts;

    /**
     * 资源内容读取器映射（URI -> 读取函数）
     * <p>为每个资源注册自定义的内容读取逻辑，实现资源内容的动态生成</p>
     */
    private Map<String, Function<String, MCPProtocol.ResourceContent>> resourceReaders;

    /**
     * 服务器能力配置声明
     * <p>向客户端宣告服务器支持的功能特性，如资源订阅、工具列表变更通知等</p>
     */
    private MCPProtocol.ServerCapabilities capabilities;

    // ==================== 资源订阅机制 ====================
    
    /**
     * 资源订阅者映射表（URI -> 订阅者集合）
     * <p>使用 ConcurrentHashMap 保证线程安全，支持多个订阅者监听同一资源变更</p>
     * <p>当资源内容更新时，自动通知所有订阅者</p>
     */
    private final Map<String, Set<Consumer<String>>> resourceSubscribers = new ConcurrentHashMap<>();

    // ==================== 采样与日志能力 ====================
    
    /**
     * 采样请求处理器
     * <p>处理来自客户端的采样请求，用于获取模型输出或中间结果</p>
     */
    private Function<MCPProtocol.SamplingCreateMessageParams, MCPProtocol.SamplingCreateMessageResult> samplingHandler;

    /**
     * 当前日志级别配置
     * <p>控制日志输出的详细程度，默认为 INFO 级别</p>
     */
    private MCPProtocol.LogLevel logLevel = MCPProtocol.LogLevel.INFO;

    /**
     * 日志消息处理器
     * <p>接收并处理服务器产生的日志消息，可转发到外部日志系统</p>
     */
    private Consumer<MCPProtocol.LoggingMessageParams> logHandler;

    // ==================== 工具类字段 ====================
    
    /**
     * JSON 序列化/反序列化工具
     * <p>用于对象与 JSON 字符串之间的转换，线程安全</p>
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 构造函数：初始化基础配置
     * 
     * @param name 服务器名称
     * @param version 服务器版本
     */
    public MCPServer(String name, String version) {
        this.name = name;
        this.version = version;
        // 初始化各组件注册表
        this.resources = new HashMap<>();
        this.tools = new HashMap<>();
        this.prompts = new HashMap<>();
        this.resourceReaders = new HashMap<>();
        // 构建默认能力配置
        this.capabilities = buildDefaultCapabilities();
    }

    /**
     * 构造函数：带使用说明的初始化
     * 
     * @param name 服务器名称
     * @param version 服务器版本
     * @param instructions 服务器使用说明
     */
    public MCPServer(String name, String version, String instructions) {
        this(name, version);
        this.instructions = instructions;
    }

    /**
     * 构建默认服务器能力配置
     * 
     * <p>配置服务器支持的核心功能：</p>
     * <ul>
     *     <li>资源管理：启用订阅和列表变更通知</li>
     *     <li>工具管理：启用列表变更通知</li>
     *     <li>提示管理：启用列表变更通知</li>
     *     <li>日志系统：启用日志记录能力</li>
     * </ul>
     * 
     * @return 默认能力配置对象
     */
    private MCPProtocol.ServerCapabilities buildDefaultCapabilities() {
        return MCPProtocol.ServerCapabilities.builder()
                // 资源配置：支持订阅机制和资源列表变更通知
                .resources(MCPProtocol.ResourcesCapability.builder()
                        .subscribe(true)      // 启用资源订阅功能
                        .listChanged(true)    // 启用资源列表变更通知
                        .build())
                // 工具配置：支持工具列表变更通知
                .tools(MCPProtocol.ToolsCapability.builder()
                        .listChanged(true)    // 启用工具列表变更通知
                        .build())
                // 提示配置：支持提示列表变更通知
                .prompts(MCPProtocol.PromptsCapability.builder()
                        .listChanged(true)    // 启用提示列表变更通知
                        .build())
                // 日志配置：启用日志记录能力
                .logging(MCPProtocol.LoggingCapability.builder().build())
                .build();
    }

    // ==================== 组件注册方法 ====================

    /**
     * 注册 MCP 资源
     * 
     * <p>将资源添加到资源注册表中，使客户端可以发现和访问该资源</p>
     * 
     * @param resource 要注册的资源对象，必须包含有效的 URI
     * @throws IllegalArgumentException 当资源对象或 URI 为 null 时抛出
     */
    public void registerResource(MCPResource resource) {
        // 参数校验：确保资源和 URI 不为空
        if (resource == null || resource.getUri() == null) {
            throw new IllegalArgumentException("资源或资源 URI 不能为空");
        }

        // 注册到资源表
        resources.put(resource.getUri(), resource);
        log.info("注册 MCP 资源：{} - {}", resource.getUri(), resource.getName());
    }

    /**
     * 注册 MCP 资源（带自定义内容读取器）
     * 
     * <p>注册资源的同时配置内容读取函数，用于动态生成资源内容</p>
     * 
     * @param resource 要注册的资源对象
     * @param reader 资源内容读取函数，接收 URI 参数返回资源内容
     *               可为 null，为 null 时使用默认读取逻辑
     */
    public void registerResource(MCPResource resource, 
                                  Function<String, MCPProtocol.ResourceContent> reader) {
        // 先执行基础注册
        registerResource(resource);
        // 注册自定义读取器
        if (reader != null) {
            resourceReaders.put(resource.getUri(), reader);
        }
    }

    /**
     * 注册 MCP 工具
     * 
     * <p>将工具添加到工具注册表中，使客户端可以调用该工具</p>
     * 
     * @param tool 要注册的工具对象，必须包含有效的名称
     * @throws IllegalArgumentException 当工具对象或名称为 null 时抛出
     */
    public void registerTool(MCPTool tool) {
        // 参数校验：确保工具和名称不为空
        if (tool == null || tool.getName() == null) {
            throw new IllegalArgumentException("工具或工具名称不能为空");
        }

        // 注册到工具表
        tools.put(tool.getName(), tool);
        log.info("注册 MCP 工具：{} - {}", tool.getName(), tool.getDescription());
    }

    /**
     * 注册 MCP 提示
     * 
     * <p>将提示模板添加到提示注册表中，使客户端可以使用该提示</p>
     * 
     * @param prompt 要注册的提示对象，必须包含有效的名称
     * @throws IllegalArgumentException 当提示对象或名称为 null 时抛出
     */
    public void registerPrompt(MCPPrompt prompt) {
        // 参数校验：确保提示和名称不为空
        if (prompt == null || prompt.getName() == null) {
            throw new IllegalArgumentException("提示或提示名称不能为空");
        }

        // 注册到提示表
        prompts.put(prompt.getName(), prompt);
        log.info("注册 MCP 提示：{} - {}", prompt.getName(), prompt.getDescription());
    }

    // ==================== 组件查询方法 ====================

    /**
     * 根据 URI 获取已注册的 MCP 资源
     * 
     * @param uri 资源 URI
     * @return 资源对象，不存在时返回 null
     */
    public MCPResource getResource(String uri) {
        return resources.get(uri);
    }

    /**
     * 根据名称获取已注册的 MCP 工具
     * 
     * @param name 工具名称
     * @return 工具对象，不存在时返回 null
     */
    public MCPTool getTool(String name) {
        return tools.get(name);
    }

    /**
     * 根据名称获取已注册的 MCP 提示
     * 
     * @param name 提示名称
     * @return 提示对象，不存在时返回 null
     */
    public MCPPrompt getPrompt(String name) {
        return prompts.get(name);
    }

    /**
     * 获取所有已注册的资源列表
     * 
     * @return 资源列表，返回新 ArrayList 避免外部修改
     */
    public List<MCPResource> listResources() {
        return new ArrayList<>(resources.values());
    }

    /**
     * 获取所有已注册的工具列表
     * 
     * @return 工具列表，返回新 ArrayList 避免外部修改
     */
    public List<MCPTool> listTools() {
        return new ArrayList<>(tools.values());
    }

    /**
     * 获取所有已注册的提示列表
     * 
     * @return 提示列表，返回新 ArrayList 避免外部修改
     */
    public List<MCPPrompt> listPrompts() {
        return new ArrayList<>(prompts.values());
    }

    // ==================== 工具调用方法 ====================

    /**
     * 调用指定的 MCP 工具
     * 
     * <p>根据工具名称查找并执行工具，将返回值转换为标准的 MCP 响应格式</p>
     * 
     * @param toolName 工具名称
     * @param arguments 工具调用参数
     * @return 工具调用结果，包含返回内容和错误标志
     * @throws MCPException 当工具不存在或未配置执行器时抛出
     */
    public MCPProtocol.ToolCallResult invokeTool(String toolName, Map<String, Object> arguments) {
        // 查找工具
        MCPTool tool = tools.get(toolName);
        if (tool == null) {
            throw MCPException.toolNotFound(toolName);
        }

        // 检查执行器是否已配置
        if (tool.getExecutor() == null) {
            throw MCPException.internalError("工具未配置执行器：" + toolName);
        }

        log.debug("调用 MCP 工具：{} with arguments: {}", toolName, arguments);

        try {
            // 执行工具并获取结果
            Object result = tool.getExecutor().execute(arguments);
            
            // 转换为标准结果格式
            List<MCPProtocol.Content> contents = convertResultToContents(result);
            
            return MCPProtocol.ToolCallResult.builder()
                    .content(contents)
                    .isError(false)
                    .build();
        } catch (Exception e) {
            log.error("工具调用失败：{}", toolName, e);
            
            // 构建错误响应
            List<MCPProtocol.Content> errorContents = createErrorContents(e.getMessage());
            
            return MCPProtocol.ToolCallResult.builder()
                    .content(errorContents)
                    .isError(true)
                    .build();
        }
    }

    /**
     * 将工具执行结果转换为标准内容格式
     * 
     * @param result 工具执行结果
     * @return 标准化的内容列表
     */
    private List<MCPProtocol.Content> convertResultToContents(Object result) {
        List<MCPProtocol.Content> contents = new ArrayList<>();
        
        if (result instanceof String) {
            // 字符串类型直接作为文本内容
            contents.add(MCPProtocol.Content.builder()
                    .type("text")
                    .text((String) result)
                    .build());
        } else if (result instanceof MCPProtocol.Content) {
            // Content 类型直接使用
            contents.add((MCPProtocol.Content) result);
        } else if (result instanceof List) {
            // 列表类型遍历处理
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
            // 其他类型转换为 JSON 字符串
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
        
        return contents;
    }

    /**
     * 创建错误响应内容
     * 
     * @param errorMessage 错误消息
     * @return 错误内容列表
     */
    private List<MCPProtocol.Content> createErrorContents(String errorMessage) {
        List<MCPProtocol.Content> errorContents = new ArrayList<>();
        errorContents.add(MCPProtocol.Content.builder()
                .type("text")
                .text("工具调用失败：" + errorMessage)
                .build());
        return errorContents;
    }

    // ==================== 资源读取方法 ====================

    /**
     * 读取指定 MCP 资源的内容
     * 
     * <p>使用注册的读取器或默认逻辑返回资源内容</p>
     * 
     * @param uri 资源 URI
     * @return 资源读取结果，包含资源内容列表
     * @throws MCPException 当资源不存在时抛出
     */
    public MCPProtocol.ResourceReadResult readResource(String uri) {
        // 查找资源
        MCPResource resource = resources.get(uri);
        if (resource == null) {
            throw MCPException.resourceNotFound(uri);
        }

        log.debug("读取 MCP 资源：{}", uri);

        // 使用注册的读取器或默认逻辑
        Function<String, MCPProtocol.ResourceContent> reader = resourceReaders.get(uri);
        MCPProtocol.ResourceContent content;
        
        if (reader != null) {
            // 使用自定义读取器
            content = reader.apply(uri);
        } else {
            // 默认返回元数据作为文本
            content = createDefaultResourceContent(resource, uri);
        }

        // 构建返回结果
        List<MCPProtocol.ResourceContent> contents = new ArrayList<>();
        contents.add(content);
        return MCPProtocol.ResourceReadResult.builder()
                .contents(contents)
                .build();
    }

    /**
     * 创建默认资源内容（基于元数据）
     * 
     * @param resource 资源对象
     * @param uri 资源 URI
     * @return 默认资源内容
     */
    private MCPProtocol.ResourceContent createDefaultResourceContent(MCPResource resource, String uri) {
        String text = "";
        if (resource.getMetadata() != null) {
            try {
                text = objectMapper.writeValueAsString(resource.getMetadata());
            } catch (JsonProcessingException e) {
                text = resource.getMetadata().toString();
            }
        }
        return MCPProtocol.ResourceContent.builder()
                .uri(uri)
                .mimeType(resource.getMimeType() != null ? resource.getMimeType() : "application/json")
                .text(text)
                .build();
    }

    // ==================== 提示生成方法 ====================

    /**
     * 生成指定 MCP 提示的内容
     * 
     * <p>使用提示模板和参数生成最终的提示消息</p>
     * 
     * @param name 提示名称
     * @param arguments 提示参数
     * @return 提示生成结果
     * @throws MCPException 当提示不存在时抛出
     */
    public MCPPrompt.GetPromptResult getPrompt(String name, Map<String, String> arguments) {
        // 查找提示
        MCPPrompt prompt = prompts.get(name);
        if (prompt == null) {
            throw MCPException.promptNotFound(name);
        }

        log.debug("获取 MCP 提示：{} with arguments: {}", name, arguments);
        return prompt.generate(arguments);
    }

    // ==================== 服务器信息管理 ====================

    /**
     * 获取服务器基本信息
     * 
     * @return 服务器信息对象，包含名称、版本和组件数量统计
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
     * 处理客户端的初始化请求
     * 
     * <p>返回服务器信息和能力声明，完成 MCP 协议握手</p>
     * 
     * @param params 初始化参数，包含客户端信息
     * @return 初始化结果，包含协议版本、能力配置和服务器信息
     */
    public MCPProtocol.InitializeResult handleInitialize(MCPProtocol.InitializeParams params) {
        String clientName = params.getClientInfo() != null 
                ? params.getClientInfo().getName() 
                : "unknown";
        
        log.info("处理初始化请求，客户端：{}, 协议版本：{}", 
                clientName,
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

    // ==================== 资源订阅功能 ====================

    /**
     * 订阅资源变更通知
     * 
     * <p>添加订阅者到指定资源的观察者列表，当资源更新时会收到通知</p>
     * 
     * @param uri 资源 URI
     * @param subscriber 订阅者回调函数，接收更新的 URI 参数
     * @throws IllegalArgumentException 当 URI 或订阅者为 null 时抛出
     * @throws MCPException 当资源不存在时抛出
     */
    public void subscribeResource(String uri, Consumer<String> subscriber) {
        // 参数校验
        if (uri == null || subscriber == null) {
            throw new IllegalArgumentException("URI 和订阅者不能为空");
        }

        // 检查资源是否存在
        if (!resources.containsKey(uri)) {
            throw MCPException.resourceNotFound(uri);
        }

        // 添加订阅者
        resourceSubscribers.computeIfAbsent(uri, k -> ConcurrentHashMap.newKeySet())
                .add(subscriber);
        
        log.info("添加资源订阅：{}", uri);
    }

    /**
     * 取消资源订阅
     * 
     * <p>从指定资源的订阅者列表中移除订阅者</p>
     * 
     * @param uri 资源 URI
     * @param subscriber 要移除的订阅者回调函数
     */
    public void unsubscribeResource(String uri, Consumer<String> subscriber) {
        Set<Consumer<String>> subscribers = resourceSubscribers.get(uri);
        if (subscribers != null) {
            subscribers.remove(subscriber);
            log.info("取消资源订阅：{}", uri);
        }
    }

    /**
     * 通知资源已更新
     * 
     * <p>向指定资源的所有订阅者发送更新通知</p>
     * 
     * @param uri 资源 URI
     */
    public void notifyResourceUpdated(String uri) {
        Set<Consumer<String>> subscribers = resourceSubscribers.get(uri);
        if (subscribers != null && !subscribers.isEmpty()) {
            log.debug("通知资源更新：{}, 订阅者数量：{}", uri, subscribers.size());
            
            // 遍历通知所有订阅者
            for (Consumer<String> subscriber : subscribers) {
                try {
                    subscriber.accept(uri);
                } catch (Exception e) {
                    log.error("通知资源更新失败：{}", uri, e);
                }
            }
        }
    }

    /**
     * 获取资源订阅者数量
     * 
     * @param uri 资源 URI
     * @return 订阅者数量，资源不存在时返回 0
     */
    public int getResourceSubscriberCount(String uri) {
        Set<Consumer<String>> subscribers = resourceSubscribers.get(uri);
        return subscribers != null ? subscribers.size() : 0;
    }

    // ==================== 采样能力 ====================

    /**
     * 设置采样请求处理器
     * 
     * @param handler 采样处理函数，接收采样参数返回采样结果
     */
    public void setSamplingHandler(
            Function<MCPProtocol.SamplingCreateMessageParams, MCPProtocol.SamplingCreateMessageResult> handler) {
        this.samplingHandler = handler;
        log.info("设置采样处理器");
    }

    /**
     * 处理采样请求
     * 
     * <p>委托给配置的采样处理器执行实际采样逻辑</p>
     * 
     * @param params 采样请求参数
     * @return 采样结果
     * @throws MCPException 当未配置采样处理器或处理失败时抛出
     */
    public MCPProtocol.SamplingCreateMessageResult handleSampling(
            MCPProtocol.SamplingCreateMessageParams params) {
        if (samplingHandler == null) {
            throw MCPException.internalError("未配置采样处理器");
        }

        log.debug("处理采样请求，消息数：{}", 
                params.getMessages() != null ? params.getMessages().size() : 0);
        
        try {
            return samplingHandler.apply(params);
        } catch (Exception e) {
            log.error("采样处理失败", e);
            throw MCPException.internalError("采样处理失败：" + e.getMessage());
        }
    }

    /**
     * 检查服务器是否支持采样能力
     * 
     * @return true 表示已配置采样处理器，支持采样功能
     */
    public boolean supportsSampling() {
        return samplingHandler != null;
    }

    // ==================== 日志能力 ====================

    /**
     * 设置日志级别
     * 
     * @param level 日志级别配置
     */
    public void setLogLevel(MCPProtocol.LogLevel level) {
        this.logLevel = level;
        log.info("设置日志级别：{}", level);
    }

    /**
     * 获取当前日志级别配置
     * 
     * @return 当前日志级别
     */
    public MCPProtocol.LogLevel getLogLevel() {
        return logLevel;
    }

    /**
     * 设置日志消息处理器
     * 
     * @param handler 日志处理函数，接收日志消息参数
     */
    public void setLogHandler(Consumer<MCPProtocol.LoggingMessageParams> handler) {
        this.logHandler = handler;
    }

    /**
     * 发送日志消息
     * 
     * <p>根据当前日志级别判断是否记录，并通过日志处理器转发</p>
     * 
     * @param level 日志级别
     * @param logger 日志来源标识
     * @param message 日志消息内容
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
     * 判断指定级别是否应该记录
     * 
     * @param level 待检查的日志级别
     * @return true 表示应该记录，false 表示忽略
     */
    private boolean shouldLog(MCPProtocol.LogLevel level) {
        return level.ordinal() >= logLevel.ordinal();
    }

    // ==================== 内部静态类 ====================

    /**
     * 服务器信息数据传输对象
     * 
     * <p>封装服务器的基本统计信息，用于外部查询和监控</p>
     */
    @Data
    @lombok.Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServerInfo {
        /** 服务器名称 */
        private String name;
        
        /** 服务器版本 */
        private String version;
        
        /** 已注册资源数量 */
        private int resourceCount;
        
        /** 已注册工具数量 */
        private int toolCount;
        
        /** 已注册提示数量 */
        private int promptCount;
    }

    // ==================== 构建器模式 ====================

    /**
     * 创建 MCPServer 构建器
     * 
     * @return 新的构建器实例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * MCPServer 构建器
     * 
     * <p>使用流式 API 逐步配置并创建 MCPServer 实例</p>
     */
    public static class Builder {
        private String name;
        private String version;
        private String instructions;
        private final Map<String, MCPResource> resources = new HashMap<>();
        private final Map<String, MCPTool> tools = new HashMap<>();
        private final Map<String, MCPPrompt> prompts = new HashMap<>();

        /**
         * 设置服务器名称
         * 
         * @param name 服务器唯一标识
         * @return 当前构建器实例，支持链式调用
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * 设置服务器版本
         * 
         * @param version 服务器版本号
         * @return 当前构建器实例，支持链式调用
         */
        public Builder version(String version) {
            this.version = version;
            return this;
        }

        /**
         * 设置服务器使用说明
         * 
         * @param instructions 服务器功能描述和使用指南
         * @return 当前构建器实例，支持链式调用
         */
        public Builder instructions(String instructions) {
            this.instructions = instructions;
            return this;
        }

        /**
         * 添加资源到构建器
         * 
         * @param resource 要添加的 MCP 资源
         * @return 当前构建器实例，支持链式调用
         */
        public Builder resource(MCPResource resource) {
            this.resources.put(resource.getUri(), resource);
            return this;
        }

        /**
         * 添加工具到构建器
         * 
         * @param tool 要添加的 MCP 工具
         * @return 当前构建器实例，支持链式调用
         */
        public Builder tool(MCPTool tool) {
            this.tools.put(tool.getName(), tool);
            return this;
        }

        /**
         * 添加提示到构建器
         * 
         * @param prompt 要添加的 MCP 提示
         * @return 当前构建器实例，支持链式调用
         */
        public Builder prompt(MCPPrompt prompt) {
            this.prompts.put(prompt.getName(), prompt);
            return this;
        }

        /**
         * 构建并返回配置完成的 MCPServer 实例
         * 
         * @return 新的 MCPServer 实例
         */
        public MCPServer build() {
            MCPServer server = new MCPServer(name, version, instructions);
            server.resources.putAll(resources);
            server.tools.putAll(tools);
            server.prompts.putAll(prompts);
            return server;
        }
    }
}
