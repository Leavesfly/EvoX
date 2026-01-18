package io.leavesfly.evox.examples.memory;

import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.core.message.MessageType;
import io.leavesfly.evox.memory.longterm.InMemoryLongTermMemory;
import io.leavesfly.evox.memory.shortterm.ShortTermMemory;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * 记忆系统示例：演示短期与长期记忆的基本用法。
 */
@Slf4j
public class MemoryBasicsExample {

    public static void main(String[] args) {
        log.info("=== EvoX Memory Basics Demo ===");

        // 短期记忆：容量为 3
        ShortTermMemory shortTerm = new ShortTermMemory(3);
        shortTerm.initModule();

        // 长期记忆：使用内存实现，带内容去重
        InMemoryLongTermMemory longTerm = new InMemoryLongTermMemory();
        longTerm.initModule();

        List<Message> conversation = List.of(
                message(MessageType.INPUT, "Hi, I am Alex."),
                message(MessageType.RESPONSE, "Hello Alex, how can I help?"),
                message(MessageType.INPUT, "I like workflow automation."),
                message(MessageType.RESPONSE, "Got it. EvoX can help with workflows."),
                message(MessageType.INPUT, "Remember that I prefer DAGs.")
        );

        // 写入短期与长期记忆
        conversation.forEach(shortTerm::addMessage);
        longTerm.addAll(conversation);

        log.info("Short-term memory size: {}", shortTerm.size());
        log.info("Short-term latest messages:");
        shortTerm.getLatestMessages(3).forEach(msg ->
                log.info(" - {}: {}", msg.getMessageType(), msg.getContent())
        );

        log.info("Long-term memory stats: {}", longTerm.getStatistics());

        // 从长期记忆检索关键词
        Map<String, Message> matches = longTerm.search("workflow", 5);
        log.info("Long-term search results: {}", matches.size());
        matches.forEach((id, msg) -> log.info(" - {}: {}", id, msg.getContent()));

        // 触发去重：同内容会返回已有记忆 ID
        String duplicateId = longTerm.add(message(MessageType.INPUT, "I like workflow automation."));
        log.info("Duplicate message resolved to ID: {}", duplicateId);
    }

    private static Message message(MessageType type, String content) {
        return Message.builder()
                .messageType(type)
                .content(content)
                .build();
    }
}
