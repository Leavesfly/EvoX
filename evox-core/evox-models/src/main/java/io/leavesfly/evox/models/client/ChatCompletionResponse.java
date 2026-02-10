package io.leavesfly.evox.models.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * OpenAI 兼容的 Chat Completion 响应体
 * 支持 Function Calling / Tool Use 响应
 *
 * @author EvoX Team
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatCompletionResponse {

    private String id;

    private String object;

    private Long created;

    private String model;

    private List<Choice> choices;

    private Usage usage;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Choice {
        private Integer index;
        private Message message;
        private Delta delta;
        @JsonProperty("finish_reason")
        private String finishReason;
    }

    /**
     * 完整消息（非流式响应使用）
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Message {
        private String role;
        private String content;

        /**
         * 工具调用列表（当 LLM 决定调用工具时）
         */
        @JsonProperty("tool_calls")
        private List<ToolCall> toolCalls;
    }

    /**
     * 增量消息（流式响应使用）
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Delta {
        private String role;
        private String content;

        /**
         * 工具调用增量（流式模式下逐步返回工具调用信息）
         */
        @JsonProperty("tool_calls")
        private List<ToolCall> toolCalls;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Usage {
        @JsonProperty("prompt_tokens")
        private Integer promptTokens;
        @JsonProperty("completion_tokens")
        private Integer completionTokens;
        @JsonProperty("total_tokens")
        private Integer totalTokens;
    }
}
