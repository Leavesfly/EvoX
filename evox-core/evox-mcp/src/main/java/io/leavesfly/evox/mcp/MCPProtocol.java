package io.leavesfly.evox.mcp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * MCP协议定义
 * 包含协议版本、方法名、消息格式等
 *
 * @author EvoX Team
 */
public class MCPProtocol {

    /**
     * 协议版本
     */
    public static final String PROTOCOL_VERSION = "2024-11-05";

    /**
     * JSON-RPC版本
     */
    public static final String JSONRPC_VERSION = "2.0";

    /**
     * 方法名常量
     */
    public static final class Methods {
        // 初始化
        public static final String INITIALIZE = "initialize";
        public static final String INITIALIZED = "notifications/initialized";

        // 资源相关
        public static final String RESOURCES_LIST = "resources/list";
        public static final String RESOURCES_READ = "resources/read";
        public static final String RESOURCES_SUBSCRIBE = "resources/subscribe";
        public static final String RESOURCES_UNSUBSCRIBE = "resources/unsubscribe";

        // 工具相关
        public static final String TOOLS_LIST = "tools/list";
        public static final String TOOLS_CALL = "tools/call";

        // 提示相关
        public static final String PROMPTS_LIST = "prompts/list";
        public static final String PROMPTS_GET = "prompts/get";

        // 补全
        public static final String COMPLETION_COMPLETE = "completion/complete";

        // 日志
        public static final String LOGGING_SET_LEVEL = "logging/setLevel";

        // 采样
        public static final String SAMPLING_CREATE_MESSAGE = "sampling/createMessage";

        // 通知
        public static final String NOTIFICATION_CANCELLED = "notifications/cancelled";
        public static final String NOTIFICATION_PROGRESS = "notifications/progress";
        public static final String NOTIFICATION_RESOURCES_UPDATED = "notifications/resources/updated";
        public static final String NOTIFICATION_RESOURCES_LIST_CHANGED = "notifications/resources/list_changed";
        public static final String NOTIFICATION_TOOLS_LIST_CHANGED = "notifications/tools/list_changed";
        public static final String NOTIFICATION_PROMPTS_LIST_CHANGED = "notifications/prompts/list_changed";

        private Methods() {}
    }

    /**
     * 错误码常量
     */
    public static final class ErrorCodes {
        // 标准JSON-RPC错误码
        public static final int PARSE_ERROR = -32700;
        public static final int INVALID_REQUEST = -32600;
        public static final int METHOD_NOT_FOUND = -32601;
        public static final int INVALID_PARAMS = -32602;
        public static final int INTERNAL_ERROR = -32603;

        // MCP自定义错误码
        public static final int RESOURCE_NOT_FOUND = -32001;
        public static final int TOOL_NOT_FOUND = -32002;
        public static final int PROMPT_NOT_FOUND = -32003;
        public static final int UNAUTHORIZED = -32004;
        public static final int TIMEOUT = -32005;
        public static final int CANCELLED = -32006;

        private ErrorCodes() {}
    }

    /**
     * JSON-RPC请求消息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        private String jsonrpc = JSONRPC_VERSION;
        private Object id;
        private String method;
        private Object params;
    }

    /**
     * JSON-RPC响应消息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String jsonrpc = JSONRPC_VERSION;
        private Object id;
        private Object result;
        private Error error;
    }

    /**
     * JSON-RPC通知消息（无id）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Notification {
        private String jsonrpc = JSONRPC_VERSION;
        private String method;
        private Object params;
    }

    /**
     * JSON-RPC错误对象
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Error {
        private int code;
        private String message;
        private Object data;
    }

    /**
     * 初始化请求参数
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InitializeParams {
        private String protocolVersion;
        private ClientCapabilities capabilities;
        private ClientInfo clientInfo;
    }

    /**
     * 初始化响应结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InitializeResult {
        private String protocolVersion;
        private ServerCapabilities capabilities;
        private ServerInfo serverInfo;
        private String instructions;
    }

    /**
     * 客户端能力
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientCapabilities {
        private RootsCapability roots;
        private SamplingCapability sampling;
        private ExperimentalCapabilities experimental;
    }

    /**
     * 服务端能力
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServerCapabilities {
        private PromptsCapability prompts;
        private ResourcesCapability resources;
        private ToolsCapability tools;
        private LoggingCapability logging;
        private ExperimentalCapabilities experimental;
    }

    /**
     * Roots能力
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RootsCapability {
        private boolean listChanged;
    }

    /**
     * 采样能力
     */
    @Data
    @Builder
    public static class SamplingCapability {
        // 空对象表示支持
    }

    /**
     * Prompts能力
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PromptsCapability {
        private boolean listChanged;
    }

    /**
     * Resources能力
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourcesCapability {
        private boolean subscribe;
        private boolean listChanged;
    }

    /**
     * Tools能力
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolsCapability {
        private boolean listChanged;
    }

    /**
     * Logging能力
     */
    @Data
    @Builder
    public static class LoggingCapability {
        // 空对象表示支持
    }

    /**
     * 实验性能力
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExperimentalCapabilities {
        private Map<String, Object> capabilities;
    }

    /**
     * 客户端信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientInfo {
        private String name;
        private String version;
    }

    /**
     * 服务端信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServerInfo {
        private String name;
        private String version;
    }

    /**
     * 工具调用参数
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolCallParams {
        private String name;
        private Map<String, Object> arguments;
    }

    /**
     * 工具调用结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolCallResult {
        private java.util.List<Content> content;
        private boolean isError;
    }

    /**
     * 资源读取参数
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourceReadParams {
        private String uri;
    }

    /**
     * 资源读取结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourceReadResult {
        private java.util.List<ResourceContent> contents;
    }

    /**
     * 资源内容
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourceContent {
        private String uri;
        private String mimeType;
        private String text;
        private String blob; // base64编码的二进制数据
    }

    /**
     * 内容块
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Content {
        private String type; // text, image, resource
        private String text;
        private String mimeType;
        private String data; // base64 for images
        private String uri;
    }

    /**
     * 进度通知参数
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProgressParams {
        private Object progressToken;
        private double progress;
        private Double total;
    }

    /**
     * 日志级别
     */
    public enum LogLevel {
        DEBUG,
        INFO,
        NOTICE,
        WARNING,
        ERROR,
        CRITICAL,
        ALERT,
        EMERGENCY
    }

    // ============= 采样能力相关 =============

    /**
     * 采样请求参数
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SamplingCreateMessageParams {
        private java.util.List<SamplingMessage> messages;
        private String modelPreferences;
        private String systemPrompt;
        private ContextInclude includeContext;
        private Integer maxTokens;
        private Float temperature;
        private java.util.List<String> stopSequences;
        private Map<String, Object> metadata;
    }

    /**
     * 采样消息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SamplingMessage {
        private String role;  // "user" 或 "assistant"
        private Content content;
    }

    /**
     * 采样结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SamplingCreateMessageResult {
        private String role;
        private Content content;
        private String model;
        private String stopReason;  // "endTurn", "stopSequence", "maxTokens"
    }

    /**
     * 上下文包含设置
     */
    public enum ContextInclude {
        NONE,      // 不包含上下文
        THIS_SERVER,  // 只包含当前服务器的上下文
        ALL_SERVERS   // 包含所有服务器的上下文
    }

    // ============= 资源订阅相关 =============

    /**
     * 资源订阅参数
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourceSubscribeParams {
        private String uri;
    }

    /**
     * 资源更新通知参数
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourceUpdatedParams {
        private String uri;
    }

    /**
     * 资源列表变更通知
     */
    @Data
    @Builder
    public static class ResourcesListChangedParams {
        // 空参数，通知客户端重新获取列表
    }

    // ============= 日志能力相关 =============

    /**
     * 设置日志级别参数
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoggingSetLevelParams {
        private LogLevel level;
    }

    /**
     * 日志消息通知参数
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoggingMessageParams {
        private LogLevel level;
        private String logger;
        private String data;
    }

    // ============= 进度和取消相关 =============

    /**
     * 取消通知参数
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CancelledParams {
        private Object requestId;
        private String reason;
    }

    private MCPProtocol() {}
}
