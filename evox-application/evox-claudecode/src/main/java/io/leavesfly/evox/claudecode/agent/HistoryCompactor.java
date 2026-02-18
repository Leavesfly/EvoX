package io.leavesfly.evox.claudecode.agent;

import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.memory.manager.MemoryManager;
import io.leavesfly.evox.models.spi.LLMProvider;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.function.Consumer;

/**
 * 对话历史压缩器
 * 负责估算 token 数量、判断是否需要压缩、使用 LLM 生成摘要并替换历史。
 */
@Slf4j
public class HistoryCompactor {

    private static final int COMPACT_THRESHOLD = 6;

    private final MemoryManager memoryManager;
    private final LLMProvider llm;
    private final int contextWindow;
    private final Consumer<String> streamEmitter;

    public HistoryCompactor(MemoryManager memoryManager, LLMProvider llm,
                            int contextWindow, Consumer<String> streamEmitter) {
        this.memoryManager = memoryManager;
        this.llm = llm;
        this.contextWindow = contextWindow;
        this.streamEmitter = streamEmitter;
    }

    /**
     * 估算消息列表的 token 数量（粗略估算：每 4 个字符约 1 个 token）
     */
    public int estimateTokenCount(List<Message> messages) {
        int totalChars = 0;
        for (Message message : messages) {
            if (message.getContent() != null) {
                totalChars += message.getContent().toString().length();
            }
        }
        return totalChars / 4;
    }

    /**
     * 检查是否需要自动压缩，如果需要则执行压缩
     */
    public void autoCompactIfNeeded(List<Message> conversationMessages) {
        int estimatedTokens = estimateTokenCount(conversationMessages);
        if (estimatedTokens > contextWindow * 0.8) {
            emitStream("\n📦 Compacting conversation history to free up tokens...\n");
            compact();
        }
    }

    /**
     * 压缩对话历史：使用 LLM 生成摘要，替换原始历史
     */
    public void compact() {
        List<Message> allMessages = memoryManager.getAllMessages();
        if (allMessages.size() < COMPACT_THRESHOLD) {
            emitStream("ℹ️ Not enough messages to compact (need at least " + COMPACT_THRESHOLD + ").\n");
            return;
        }

        emitStream("📦 Compacting " + allMessages.size() + " messages...\n");

        String summary = generateSummary(allMessages);

        memoryManager.clearAll();
        memoryManager.addMessage(Message.systemMessage(
                "[Conversation Summary]\n" + summary
                        + "\n\n[Note: Previous conversation was compacted. "
                        + "The above is a summary of what was discussed and accomplished.]"));

        emitStream("✅ Compacted to summary (" + summary.length() + " chars)\n");
    }

    /**
     * 使用 LLM 生成对话摘要
     */
    private String generateSummary(List<Message> messages) {
        StringBuilder conversationText = new StringBuilder();
        for (Message message : messages) {
            String role = message.getMessageType() != null ? message.getMessageType().name() : "UNKNOWN";
            String content = message.getContent() != null ? message.getContent().toString() : "";
            if (content.length() > 500) {
                content = content.substring(0, 500) + "... (truncated)";
            }
            conversationText.append(role).append(": ").append(content).append("\n");
        }

        String summaryPrompt = """
                Summarize the following conversation between a user and a coding assistant.
                Focus on:
                1. What the user asked for (their goals and requirements)
                2. What was accomplished (files created, modified, commands run)
                3. What tools were used and their key results
                4. Any important decisions or context established
                5. Any errors encountered and how they were resolved
                
                Be concise but preserve all actionable information. Use bullet points.
                
                Conversation:
                """ + conversationText;

        try {
            return llm.generate(summaryPrompt);
        } catch (Exception e) {
            log.warn("Failed to generate LLM summary, falling back to simple marker", e);
            return "[Summary generation failed. " + messages.size()
                    + " messages were removed to reduce context length.]";
        }
    }

    private void emitStream(String text) {
        if (streamEmitter != null) {
            streamEmitter.accept(text);
        }
    }
}
