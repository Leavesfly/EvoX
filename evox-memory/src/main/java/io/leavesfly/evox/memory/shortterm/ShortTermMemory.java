package io.leavesfly.evox.memory.shortterm;

import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.memory.base.Memory;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 短期记忆实现
 * 存储工作流执行过程中的临时消息
 * 
 * @author EvoX Team
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class ShortTermMemory extends Memory {

    /**
     * 记忆唯一标识
     */
    private String memoryId;

    /**
     * 消息列表
     */
    private List<Message> messages = new ArrayList<>();

    @Override
    public void initModule() {
        super.initModule();
        if (memoryId == null) {
            memoryId = UUID.randomUUID().toString();
        }
    }

    /**
     * 最大消息数量（0表示无限制）
     */
    private int maxMessages = 0;

    public ShortTermMemory() {
        super();
        initModule();
    }

    public ShortTermMemory(int maxMessages) {
        super();
        this.maxMessages = maxMessages;
        initModule();
    }

    /**
     * 添加单条消息
     */
    public void addMessage(Message message) {
        if (message == null) {
            return;
        }

        messages.add(message);

        // 如果超过最大限制，移除最旧的消息
        if (maxMessages > 0 && messages.size() > maxMessages) {
            messages.remove(0);
            log.debug("Removed oldest message, current size: {}", messages.size());
        }
    }

    /**
     * 批量添加消息
     */
    public void addMessages(List<Message> newMessages) {
        if (newMessages == null || newMessages.isEmpty()) {
            return;
        }

        for (Message message : newMessages) {
            addMessage(message);
        }
    }

    /**
     * 获取所有消息
     */
    public List<Message> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    /**
     * 获取最新的N条消息
     */
    public List<Message> getLatestMessages(int n) {
        if (n <= 0 || messages.isEmpty()) {
            return Collections.emptyList();
        }

        int fromIndex = Math.max(0, messages.size() - n);
        return new ArrayList<>(messages.subList(fromIndex, messages.size()));
    }

    /**
     * 获取内存ID
     */
    public String getMemoryId() {
        return memoryId;
    }

    /**
     * 获取最大大小
     */
    public int getMaxSize() {
        return maxMessages;
    }

    /**
     * 检查是否已满
     */
    public boolean isFull() {
        return maxMessages > 0 && messages.size() >= maxMessages;
    }

    /**
     * 获取所有消息
     */
    public List<Message> getAll() {
        return getMessages();
    }

    /**
     * 获取最近N条消息
     */
    public List<Message> get(int n) {
        return getLatestMessages(n);
    }

    /**
     * 获取最后一条消息
     */
    public Message getLastMessage() {
        if (messages.isEmpty()) {
            return null;
        }
        return messages.get(messages.size() - 1);
    }

    /**
     * 获取第一条消息
     */
    public Message getFirstMessage() {
        if (messages.isEmpty()) {
            return null;
        }
        return messages.get(0);
    }

    /**
     * 调整大小
     */
    public void resize(int newMaxSize) {
        if (newMaxSize < 0) {
            throw new IllegalArgumentException("Max size cannot be negative");
        }
        
        this.maxMessages = newMaxSize;
        
        // 如果新大小小于当前消息数量，移除多余的消息
        while (maxMessages > 0 && messages.size() > maxMessages) {
            messages.remove(0);
        }
    }

    /**
     * 获取剩余容量
     */
    public int getRemainingCapacity() {
        if (maxMessages <= 0) {
            return Integer.MAX_VALUE; // 无限制
        }
        return Math.max(0, maxMessages - messages.size());
    }

    /**
     * 根据智能体名称过滤消息
     */
    public List<Message> getMessagesByAgent(String agentName) {
        return messages.stream()
            .filter(msg -> agentName.equals(msg.getAgent()))
            .collect(Collectors.toList());
    }

    /**
     * 根据动作名称过滤消息
     */
    public List<Message> getMessagesByAction(String actionName) {
        return messages.stream()
            .filter(msg -> actionName.equals(msg.getAction()))
            .collect(Collectors.toList());
    }

    /**
     * 清空所有消息
     */
    @Override
    public void clear() {
        messages.clear();
        log.debug("Short-term memory cleared");
    }

    /**
     * 获取消息数量
     */
    public int size() {
        return messages.size();
    }

    /**
     * 检查是否为空
     */
    public boolean isEmpty() {
        return messages.isEmpty();
    }

    @Override
    public String toString() {
        return String.format("ShortTermMemory(messages=%d, maxMessages=%d)", 
                           messages.size(), maxMessages);
    }
}
