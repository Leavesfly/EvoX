package io.leavesfly.evox.claudecode.agent;

import io.leavesfly.evox.models.spi.LLMProvider;
import io.leavesfly.evox.models.protocol.ChatCompletionResponse;
import io.leavesfly.evox.models.protocol.ChatCompletionResult;
import io.leavesfly.evox.models.protocol.ToolCall;
import io.leavesfly.evox.models.protocol.ToolDefinition;
import io.leavesfly.evox.core.message.Message;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * 流式响应收集器
 * 负责消费 LLM 的 SSE 流，实时输出文本 token，拼接 ToolCall 增量，
 * 并在流结束后返回完整的 ChatCompletionResult。
 */
@Slf4j
public class StreamCollector {

    private static final int MAX_LLM_RETRIES = 2;
    private static final long STREAM_TIMEOUT_MINUTES = 5;

    private final LLMProvider llm;
    private final Consumer<String> streamEmitter;

    public StreamCollector(LLMProvider llm, Consumer<String> streamEmitter) {
        this.llm = llm;
        this.streamEmitter = streamEmitter;
    }

    /**
     * 带重试的流式响应收集。
     * 当 LLM 调用失败时（返回 null），最多重试 MAX_LLM_RETRIES 次。
     */
    public ChatCompletionResult collectWithRetry(List<Message> conversationMessages,
                                                  List<ToolDefinition> toolDefinitions) {
        ChatCompletionResult result = collect(conversationMessages, toolDefinitions);
        if (result != null) {
            return result;
        }

        for (int retry = 1; retry <= MAX_LLM_RETRIES; retry++) {
            emitStream("\n⚠️ LLM request failed. Retrying (" + retry + "/" + MAX_LLM_RETRIES + ")...\n");
            try {
                Thread.sleep(1000L * retry);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
            result = collect(conversationMessages, toolDefinitions);
            if (result != null) {
                return result;
            }
        }

        log.error("LLM request failed after {} retries", MAX_LLM_RETRIES);
        return null;
    }

    /**
     * 消费流式 SSE chunk，实时输出文本 token，拼接 ToolCall 增量，
     * 流结束后返回完整的 ChatCompletionResult。
     */
    public ChatCompletionResult collect(List<Message> conversationMessages,
                                         List<ToolDefinition> toolDefinitions) {
        StringBuilder contentAccumulator = new StringBuilder();
        Map<Integer, ToolCall> toolCallAccumulator = new LinkedHashMap<>();
        AtomicReference<String> finishReasonRef = new AtomicReference<>();
        AtomicReference<ChatCompletionResult.TokenUsage> usageRef = new AtomicReference<>();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        llm.chatWithToolDefinitionsStream(conversationMessages, toolDefinitions, "auto")
                .doOnNext(chunk -> processChunk(chunk, contentAccumulator, toolCallAccumulator,
                        finishReasonRef, usageRef))
                .doOnError(errorRef::set)
                .doFinally(signal -> latch.countDown())
                .subscribe();

        try {
            boolean completed = latch.await(STREAM_TIMEOUT_MINUTES, TimeUnit.MINUTES);
            if (!completed) {
                log.warn("Streaming response timed out after {} minutes", STREAM_TIMEOUT_MINUTES);
                return null;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Streaming response interrupted", e);
            return null;
        }

        if (errorRef.get() != null) {
            log.error("Streaming response error", errorRef.get());
            return null;
        }

        List<ToolCall> completedToolCalls = toolCallAccumulator.isEmpty()
                ? null
                : new ArrayList<>(toolCallAccumulator.values());

        String content = contentAccumulator.isEmpty() ? null : contentAccumulator.toString();

        return ChatCompletionResult.builder()
                .content(content)
                .toolCalls(completedToolCalls)
                .finishReason(finishReasonRef.get())
                .usage(usageRef.get())
                .build();
    }

    /**
     * 处理单个 SSE chunk：
     * - 文本 delta → 实时输出到终端 + 追加到 accumulator
     * - ToolCall delta → 按 index 拼接到 accumulator
     * - finish_reason / usage → 记录到 ref
     */
    private void processChunk(ChatCompletionResponse chunk,
                              StringBuilder contentAccumulator,
                              Map<Integer, ToolCall> toolCallAccumulator,
                              AtomicReference<String> finishReasonRef,
                              AtomicReference<ChatCompletionResult.TokenUsage> usageRef) {
        if (chunk == null || chunk.getChoices() == null || chunk.getChoices().isEmpty()) {
            if (chunk != null && chunk.getUsage() != null) {
                usageRef.set(ChatCompletionResult.TokenUsage.builder()
                        .promptTokens(chunk.getUsage().getPromptTokens() != null
                                ? chunk.getUsage().getPromptTokens() : 0)
                        .completionTokens(chunk.getUsage().getCompletionTokens() != null
                                ? chunk.getUsage().getCompletionTokens() : 0)
                        .totalTokens(chunk.getUsage().getTotalTokens() != null
                                ? chunk.getUsage().getTotalTokens() : 0)
                        .build());
            }
            return;
        }

        ChatCompletionResponse.Choice choice = chunk.getChoices().get(0);

        if (choice.getFinishReason() != null) {
            finishReasonRef.set(choice.getFinishReason());
        }

        ChatCompletionResponse.Delta delta = choice.getDelta();
        if (delta == null) {
            return;
        }

        if (delta.getContent() != null) {
            emitStream(delta.getContent());
            contentAccumulator.append(delta.getContent());
        }

        if (delta.getToolCalls() != null) {
            for (ToolCall deltaToolCall : delta.getToolCalls()) {
                int index = deltaToolCall.getIndex() != null ? deltaToolCall.getIndex() : 0;

                ToolCall existing = toolCallAccumulator.get(index);
                if (existing == null) {
                    existing = ToolCall.builder()
                            .id(deltaToolCall.getId())
                            .type(deltaToolCall.getType() != null ? deltaToolCall.getType() : "function")
                            .index(index)
                            .function(ToolCall.FunctionCall.builder()
                                    .name(deltaToolCall.getFunction() != null
                                            ? deltaToolCall.getFunction().getName() : null)
                                    .arguments(deltaToolCall.getFunction() != null
                                            && deltaToolCall.getFunction().getArguments() != null
                                            ? deltaToolCall.getFunction().getArguments() : "")
                                    .build())
                            .build();
                    toolCallAccumulator.put(index, existing);
                } else {
                    if (deltaToolCall.getFunction() != null
                            && deltaToolCall.getFunction().getArguments() != null) {
                        existing.getFunction().setArguments(
                                existing.getFunction().getArguments()
                                        + deltaToolCall.getFunction().getArguments());
                    }
                    if (deltaToolCall.getId() != null && existing.getId() == null) {
                        existing.setId(deltaToolCall.getId());
                    }
                }
            }
        }
    }

    private void emitStream(String text) {
        if (streamEmitter != null) {
            streamEmitter.accept(text);
        }
    }
}
