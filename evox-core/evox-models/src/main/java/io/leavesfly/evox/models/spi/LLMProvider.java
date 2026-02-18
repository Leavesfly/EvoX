package io.leavesfly.evox.models.spi;

import io.leavesfly.evox.core.llm.ILLM;
import io.leavesfly.evox.core.llm.ILLMToolUse;
import io.leavesfly.evox.core.llm.LLMConfig;
import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.models.protocol.ChatCompletionResponse;
import io.leavesfly.evox.models.protocol.ChatCompletionResult;
import io.leavesfly.evox.models.protocol.ToolDefinition;

import reactor.core.publisher.Flux;

import java.util.List;

public interface LLMProvider extends ILLM, ILLMToolUse {
    LLMConfig getConfig();
    default ChatCompletionResult chatWithToolDefinitions(List<Message> messages,
                                                         List<ToolDefinition> toolDefinitions,
                                                         String toolChoice) {
        throw new UnsupportedOperationException(
                "This LLM provider does not support native function calling. Model: " + getModelName());
    }
    default Flux<ChatCompletionResponse> chatWithToolDefinitionsStream(List<Message> messages,
                                                                       List<ToolDefinition> toolDefinitions,
                                                                       String toolChoice) {
        throw new UnsupportedOperationException(
                "This LLM provider does not support streaming function calling. Model: " + getModelName());
    }
}
