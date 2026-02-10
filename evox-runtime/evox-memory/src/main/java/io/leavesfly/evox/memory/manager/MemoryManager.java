package io.leavesfly.evox.memory.manager;

import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.core.module.BaseModule;
import io.leavesfly.evox.memory.shortterm.ShortTermMemory;
import io.leavesfly.evox.memory.longterm.LongTermMemory;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 记忆管理器
 * 统一管理短期和长期记忆
 * 
 * @author EvoX Team
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class MemoryManager extends BaseModule {

    /**
     * 短期记忆
     */
    private ShortTermMemory shortTermMemory;

    /**
     * 长期记忆（可选）
     */
    private LongTermMemory longTermMemory;

    /**
     * 是否启用长期记忆
     */
    private boolean useLongTermMemory = false;

    /**
     * 自动同步阈值（短期记忆达到此大小时自动同步到长期记忆）
     */
    private int autoSyncThreshold = 100;

    public MemoryManager() {
        this.shortTermMemory = new ShortTermMemory();
    }

    public MemoryManager(ShortTermMemory shortTermMemory) {
        this.shortTermMemory = shortTermMemory;
    }

    public MemoryManager(ShortTermMemory shortTermMemory, LongTermMemory longTermMemory) {
        this.shortTermMemory = shortTermMemory;
        this.longTermMemory = longTermMemory;
        this.useLongTermMemory = (longTermMemory != null);
    }

    @Override
    public void initModule() {
        super.initModule();
        if (shortTermMemory != null) {
            shortTermMemory.initModule();
        }
        if (useLongTermMemory && longTermMemory != null) {
            longTermMemory.initModule();
        }
    }

    /**
     * 添加消息
     */
    public void addMessage(Message message) {
        if (message == null) {
            return;
        }

        // 添加到短期记忆
        shortTermMemory.addMessage(message);

        // 检查是否需要同步到长期记忆
        if (useLongTermMemory && shouldSync()) {
            syncToLongTerm();
        }
    }

    /**
     * 批量添加消息
     */
    public void addMessages(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }

        for (Message message : messages) {
            addMessage(message);
        }
    }

    /**
     * 获取最新的消息
     */
    public List<Message> getLatestMessages(int n) {
        return shortTermMemory.getLatestMessages(n);
    }

    /**
     * 获取所有短期记忆消息
     */
    public List<Message> getAllMessages() {
        return shortTermMemory.getMessages();
    }

    /**
     * 搜索相关记忆（从长期记忆）
     */
    public List<Message> searchRelevantMemories(String query, int topK) {
        if (!useLongTermMemory || longTermMemory == null) {
            log.warn("Long-term memory is not enabled");
            return new ArrayList<>();
        }

        return longTermMemory.searchSimilar(query, topK);
    }

    /**
     * 同步短期记忆到长期记忆
     */
    public void syncToLongTerm() {
        if (!useLongTermMemory || longTermMemory == null) {
            return;
        }

        List<Message> messages = shortTermMemory.getMessages();
        if (!messages.isEmpty()) {
            longTermMemory.storeMessages(messages);
            log.debug("Synced {} messages to long-term memory", messages.size());
        }
    }

    /**
     * 检查是否需要同步
     */
    private boolean shouldSync() {
        return shortTermMemory.size() >= autoSyncThreshold;
    }

    /**
     * 清空短期记忆
     */
    public void clearShortTerm() {
        shortTermMemory.clear();
        log.debug("Short-term memory cleared");
    }

    /**
     * 清空长期记忆
     */
    public void clearLongTerm() {
        if (longTermMemory != null) {
            longTermMemory.clear();
            log.debug("Long-term memory cleared");
        }
    }

    /**
     * 清空所有记忆
     */
    public void clearAll() {
        clearShortTerm();
        clearLongTerm();
        log.info("All memories cleared");
    }

    /**
     * 获取短期记忆大小
     */
    public int getShortTermSize() {
        return shortTermMemory.size();
    }

    @Override
    public String toString() {
        return String.format("MemoryManager(shortTerm=%d messages, longTerm=%s)", 
                           shortTermMemory.size(),
                           useLongTermMemory ? "enabled" : "disabled");
    }
}
