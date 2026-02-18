package io.leavesfly.evox.models.protocol;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容的 Chat Completion 请求体
 * 支持 Function Calling / Tool Use
 *
 * @author EvoX Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatCompletionRequest {

    private String model;

    private List<ChatMessage> messages;

    private Float temperature;

    @JsonProperty("max_tokens")
    private Integer maxTokens;

    @JsonProperty("top_p")
    private Float topP;

    @JsonProperty("frequency_penalty")
    private Float frequencyPenalty;

    @JsonProperty("presence_penalty")
    private Float presencePenalty;

    private Boolean stream;

    @JsonProperty("n")
    private Integer numberOfResults;

    /**
     * 工具定义列表（Function Calling）
     */
    private List<ToolDefinition> tools;

    /**
     * 工具选择策略：
     * - "none": 不调用工具
     * - "auto": 模型自动决定（默认）
     * - "required": 必须调用工具
     * - 或指定具体工具：{"type": "function", "function": {"name": "xxx"}}
     */
    @JsonProperty("tool_choice")
    private Object toolChoice;

    /**
     * 是否启用并行工具调用
     */
    @JsonProperty("parallel_tool_calls")
    private Boolean parallelToolCalls;

    /**
     * 流式选项（用于流式模式下返回 usage 信息）
     */
    @JsonProperty("stream_options")
    private Map<String, Object> streamOptions;

    /**
     * 聊天消息
     * 支持 user/system/assistant/tool 四种角色
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChatMessage {

        /**
         * 角色：system / user / assistant / tool
         */
        private String role;

        /**
         * 消息文本内容
         */
        private String content;

        /**
         * 工具调用列表（仅 assistant 角色使用）
         * 当 LLM 决定调用工具时，assistant 消息会包含此字段
         */
        @JsonProperty("tool_calls")
        private List<ToolCall> toolCalls;

        /**
         * 工具调用 ID（仅 tool 角色使用）
         * 用于将工具执行结果关联回对应的工具调用
         */
        @JsonProperty("tool_call_id")
        private String toolCallId;

        /**
         * 消息名称（可选，用于区分同角色的不同消息来源）
         */
        private String name;
    }
}
