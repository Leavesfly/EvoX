package io.leavesfly.evox.models.protocol;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Chat Completion 的统一结果封装
 * 同时承载文本响应和工具调用，供上层 Agent 使用
 *
 * @author EvoX Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatCompletionResult {

    /**
     * LLM 返回的文本内容（可能为 null，当 LLM 选择调用工具时）
     */
    private String content;

    /**
     * LLM 请求的工具调用列表（可能为空，当 LLM 直接返回文本时）
     */
    private List<ToolCall> toolCalls;

    /**
     * 结束原因：stop / tool_calls / length / content_filter
     */
    private String finishReason;

    /**
     * Token 使用统计
     */
    private TokenUsage usage;

    /**
     * 是否包含工具调用
     */
    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }

    /**
     * 是否为纯文本响应
     */
    public boolean isTextResponse() {
        return !hasToolCalls() && content != null;
    }

    /**
     * Token 使用统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenUsage {
        private int promptTokens;
        private int completionTokens;
        private int totalTokens;
    }

    /**
     * 从 ChatCompletionResponse 构建
     */
    public static ChatCompletionResult fromResponse(ChatCompletionResponse response) {
        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
            return ChatCompletionResult.builder().content("").build();
        }

        ChatCompletionResponse.Choice firstChoice = response.getChoices().get(0);
        ChatCompletionResultBuilder builder = ChatCompletionResult.builder()
                .finishReason(firstChoice.getFinishReason());

        if (firstChoice.getMessage() != null) {
            builder.content(firstChoice.getMessage().getContent());
            builder.toolCalls(firstChoice.getMessage().getToolCalls());
        }

        if (response.getUsage() != null) {
            builder.usage(TokenUsage.builder()
                    .promptTokens(response.getUsage().getPromptTokens() != null
                            ? response.getUsage().getPromptTokens() : 0)
                    .completionTokens(response.getUsage().getCompletionTokens() != null
                            ? response.getUsage().getCompletionTokens() : 0)
                    .totalTokens(response.getUsage().getTotalTokens() != null
                            ? response.getUsage().getTotalTokens() : 0)
                    .build());
        }

        return builder.build();
    }
}
