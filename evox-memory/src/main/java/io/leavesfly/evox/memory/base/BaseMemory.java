package io.leavesfly.evox.memory.base;

import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.core.module.BaseModule;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 基础记忆类 - 提供消息存储、检索和过滤的核心功能
 * 对应 Python 版本的 BaseMemory
 */
@Slf4j
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BaseMemory extends BaseModule {

    /**
     * 存储的消息列表
     */
    private List<Message> messages;

    /**
     * 记忆唯一标识
     */
    private String memoryId;

    /**
     * 创建时间戳
     */
    private Instant timestamp;

    /**
     * 容量限制（null 表示无限制）
     */
    private Integer capacity;

    /**
     * 按动作索引的消息
     */
    private Map<String, List<Message>> byAction;

    /**
     * 按工作流目标索引的消息
     */
    private Map<String, List<Message>> byWorkflowGoal;

    @Override
    public void initModule() {
        if (messages == null) {
            messages = Collections.synchronizedList(new ArrayList<>());
        }
        if (memoryId == null) {
            memoryId = UUID.randomUUID().toString();
        }
        if (timestamp == null) {
            timestamp = Instant.now();
        }
        if (byAction == null) {
            byAction = new ConcurrentHashMap<>();
        }
        if (byWorkflowGoal == null) {
            byWorkflowGoal = new ConcurrentHashMap<>();
        }
    }

    /**
     * 获取当前消息数量
     */
    public int size() {
        return messages.size();
    }

    /**
     * 清空所有消息
     */
    public void clear() {
        messages.clear();
        byAction.clear();
        byWorkflowGoal.clear();
        log.debug("Memory cleared");
    }

    /**
     * 添加单个消息
     */
    public void addMessage(Message message) {
        if (message == null) {
            return;
        }
        
        // 检查是否已存在
        if (messages.contains(message)) {
            log.debug("Message already exists in memory: {}", message.getMessageId());
            return;
        }

        // 检查容量限制
        if (capacity != null && messages.size() >= capacity) {
            // 移除最旧的消息
            Message oldest = messages.remove(0);
            removeFromIndices(oldest);
            log.debug("Capacity limit reached, removed oldest message");
        }

        // 添加到主列表
        messages.add(message);

        // 更新索引
        if (message.getAction() != null) {
            byAction.computeIfAbsent(message.getAction(), k -> Collections.synchronizedList(new ArrayList<>()))
                    .add(message);
        }
        if (message.getWorkflowGoal() != null) {
            byWorkflowGoal.computeIfAbsent(message.getWorkflowGoal(), k -> Collections.synchronizedList(new ArrayList<>()))
                    .add(message);
        }

        log.debug("Added message to memory: {}", message.getMessageId());
    }

    /**
     * 添加多个消息
     */
    public void addMessages(List<Message> messagesToAdd) {
        if (messagesToAdd == null) {
            return;
        }
        for (Message message : messagesToAdd) {
            addMessage(message);
        }
    }

    /**
     * 移除单个消息
     */
    public void removeMessage(Message message) {
        if (message == null || !messages.contains(message)) {
            return;
        }

        messages.remove(message);
        removeFromIndices(message);
        log.debug("Removed message from memory: {}", message.getMessageId());
    }

    /**
     * 从索引中移除消息
     */
    private void removeFromIndices(Message message) {
        if (message.getAction() != null && byAction.containsKey(message.getAction())) {
            byAction.get(message.getAction()).remove(message);
        }
        if (message.getWorkflowGoal() != null && byWorkflowGoal.containsKey(message.getWorkflowGoal())) {
            byWorkflowGoal.get(message.getWorkflowGoal()).remove(message);
        }
    }

    /**
     * 获取最近的 n 条消息
     *
     * @param n 数量限制，null 表示全部
     * @return 消息列表
     */
    public List<Message> get(Integer n) {
        if (n == null || n >= messages.size()) {
            return new ArrayList<>(messages);
        }
        
        if (n < 0) {
            throw new IllegalArgumentException("n must be null or a positive integer");
        }

        int startIndex = Math.max(0, messages.size() - n);
        return new ArrayList<>(messages.subList(startIndex, messages.size()));
    }

    /**
     * 获取所有消息
     */
    public List<Message> getAll() {
        return get(null);
    }

    /**
     * 根据动作获取消息
     *
     * @param action 动作名称
     * @param n 数量限制
     * @return 消息列表
     */
    public List<Message> getByAction(String action, Integer n) {
        return getByType(byAction, action, n);
    }

    /**
     * 根据多个动作获取消息
     */
    public List<Message> getByActions(List<String> actions, Integer n) {
        if (actions == null || actions.isEmpty()) {
            return Collections.emptyList();
        }

        List<Message> result = new ArrayList<>();
        for (String action : actions) {
            result.addAll(getByAction(action, n));
        }

        // 按时间戳排序
        return sortByTimestamp(result);
    }

    /**
     * 根据工作流目标获取消息
     */
    public List<Message> getByWorkflowGoal(String workflowGoal, Integer n) {
        return getByType(byWorkflowGoal, workflowGoal, n);
    }

    /**
     * 根据多个工作流目标获取消息
     */
    public List<Message> getByWorkflowGoals(List<String> workflowGoals, Integer n) {
        if (workflowGoals == null || workflowGoals.isEmpty()) {
            return Collections.emptyList();
        }

        List<Message> result = new ArrayList<>();
        for (String goal : workflowGoals) {
            result.addAll(getByWorkflowGoal(goal, n));
        }

        return sortByTimestamp(result);
    }

    /**
     * 通用的按类型获取方法
     */
    private List<Message> getByType(Map<String, List<Message>> data, String key, Integer n) {
        if (data == null || !data.containsKey(key)) {
            return Collections.emptyList();
        }

        List<Message> msgs = data.get(key);
        if (n == null || n >= msgs.size()) {
            return new ArrayList<>(msgs);
        }

        if (n < 0) {
            throw new IllegalArgumentException("n must be null or a positive integer");
        }

        int startIndex = Math.max(0, msgs.size() - n);
        return new ArrayList<>(msgs.subList(startIndex, msgs.size()));
    }

    /**
     * 按时间戳排序消息
     */
    protected List<Message> sortByTimestamp(List<Message> messagesToSort) {
        return messagesToSort.stream()
                .sorted(Comparator.comparing(Message::getTimestamp))
                .collect(Collectors.toList());
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
     * 检查是否为空
     */
    public boolean isEmpty() {
        return messages.isEmpty();
    }
}
