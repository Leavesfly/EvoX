package io.leavesfly.evox.memory.shortterm;

import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.memory.base.Memory;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
     * 消息列表
     */
    private List<Message> messages = new ArrayList<>();

    /**
     * 最大消息数量（0表示无限制）
     */
    private int maxMessages = 0;

    public ShortTermMemory() {
        super();
    }

    public ShortTermMemory(int maxMessages) {
        super();
        this.maxMessages = maxMessages;
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
