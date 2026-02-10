package io.leavesfly.evox.models.base;

import io.leavesfly.evox.core.llm.ILLM;
import io.leavesfly.evox.core.llm.ILLMToolUse;
import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.models.client.ChatCompletionResponse;
import io.leavesfly.evox.models.client.ChatCompletionResult;
import io.leavesfly.evox.models.client.ToolDefinition;
import io.leavesfly.evox.models.config.LLMConfig;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * LLM 提供者接口
 * 继承核心层的 {@link ILLM} 和 {@link ILLMToolUse} 接口，并扩展模型层特有的能力。
 * 所有 LLM 实现必须实现此接口。
 *
 * <p>命名准确地反映其"提供者"语义。
 *
 * @author EvoX Team
 */
public interface LLMProvider extends ILLM, ILLMToolUse {

    /**
     * 获取配置
     *
     * @return LLM 配置
     */
    LLMConfig getConfig();

    /**
     * 带工具定义的对话调用（强类型版本）
     * 使用 ToolDefinition 和 ChatCompletionResult 类型，提供更好的类型安全
     *
     * @param messages        消息列表
     * @param toolDefinitions 工具定义列表
     * @param toolChoice      工具选择策略
     * @return 包含文本和/或工具调用的完整结果
     */
    default ChatCompletionResult chatWithToolDefinitions(List<Message> messages,
                                                         List<ToolDefinition> toolDefinitions,
                                                         String toolChoice) {
        throw new UnsupportedOperationException(
                "This LLM provider does not support native function calling. Model: " + getModelName());
    }

    /**
     * 带工具定义的流式对话调用
     * 返回原始 SSE chunk 流，由调用方负责拼接 ToolCall 和文本内容
     *
     * @param messages        消息列表
     * @param toolDefinitions 工具定义列表
     * @param toolChoice      工具选择策略
     * @return SSE chunk 流
     */
    default Flux<ChatCompletionResponse> chatWithToolDefinitionsStream(List<Message> messages,
                                                                       List<ToolDefinition> toolDefinitions,
                                                                       String toolChoice) {
        throw new UnsupportedOperationException(
                "This LLM provider does not support streaming function calling. Model: " + getModelName());
    }
}
