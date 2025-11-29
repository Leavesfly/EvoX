package io.leavesfly.evox.memory.longterm;

import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.memory.base.BaseMemory;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 内存长期记忆实现 - 基于哈希去重的内存存储
 * 对应 Python 版本的 LongTermMemory 的简化版本（不依赖 RAG）
 */
@Slf4j
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class InMemoryLongTermMemory extends BaseMemory {

    /**
     * 记忆 ID 到消息的映射
     */
    private Map<String, Message> memoryIdToMessage;

    /**
     * 内容哈希到记忆 ID 的映射（用于去重）
     */
    private Map<String, String> contentHashToMemoryId;

    /**
     * 记忆 ID 到内容哈希的映射
     */
    private Map<String, String> memoryIdToContentHash;

    @Override
    public void initModule() {
        super.initModule();
        if (memoryIdToMessage == null) {
            memoryIdToMessage = new ConcurrentHashMap<>();
        }
        if (contentHashToMemoryId == null) {
            contentHashToMemoryId = new ConcurrentHashMap<>();
        }
        if (memoryIdToContentHash == null) {
            memoryIdToContentHash = new ConcurrentHashMap<>();
        }
    }

    /**
     * 计算内容哈希
     */
    private String calculateContentHash(Object content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(String.valueOf(content).getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not available", e);
            return UUID.randomUUID().toString();
        }
    }

    /**
     * 字节数组转十六进制字符串
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * 添加消息到长期记忆
     *
     * @param message 要添加的消息
     * @return 记忆 ID
     */
    public String add(Message message) {
        if (message == null || message.getContent() == null) {
            log.warn("Cannot add null message or message with null content");
            return null;
        }

        // 计算内容哈希
        String contentHash = calculateContentHash(message.getContent());

        // 检查是否已存在
        if (contentHashToMemoryId.containsKey(contentHash)) {
            String existingId = contentHashToMemoryId.get(contentHash);
            log.debug("Duplicate message found (hash: {}), returning existing ID: {}", 
                    contentHash.substring(0, 8), existingId);
            return existingId;
        }

        // 生成新的记忆 ID
        String memoryId = UUID.randomUUID().toString();

        // 存储映射关系
        memoryIdToMessage.put(memoryId, message);
        contentHashToMemoryId.put(contentHash, memoryId);
        memoryIdToContentHash.put(memoryId, contentHash);

        // 添加到基础记忆
        super.addMessage(message);

        log.debug("Added message to long-term memory: {} (hash: {})", 
                memoryId, contentHash.substring(0, 8));
        return memoryId;
    }

    /**
     * 批量添加消息
     *
     * @param messages 消息列表
     * @return 记忆 ID 列表
     */
    public List<String> addAll(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> memoryIds = new ArrayList<>();
        for (Message message : messages) {
            String memoryId = add(message);
            if (memoryId != null) {
                memoryIds.add(memoryId);
            }
        }

        log.info("Added {} messages to long-term memory (total: {})", 
                memoryIds.size(), memoryIdToMessage.size());
        return memoryIds;
    }

    /**
     * 根据记忆 ID 获取消息
     *
     * @param memoryId 记忆 ID
     * @return 消息，不存在则返回 null
     */
    public Message getByMemoryId(String memoryId) {
        return memoryIdToMessage.get(memoryId);
    }

    /**
     * 根据多个记忆 ID 获取消息
     *
     * @param memoryIds 记忆 ID 列表
     * @return 记忆 ID 到消息的映射
     */
    public Map<String, Message> getByMemoryIds(List<String> memoryIds) {
        if (memoryIds == null || memoryIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return memoryIds.stream()
                .filter(memoryIdToMessage::containsKey)
                .collect(Collectors.toMap(
                        id -> id,
                        memoryIdToMessage::get
                ));
    }

    /**
     * 删除指定记忆 ID 的消息
     *
     * @param memoryId 记忆 ID
     * @return 是否成功删除
     */
    public boolean delete(String memoryId) {
        if (!memoryIdToMessage.containsKey(memoryId)) {
            log.warn("Memory ID not found: {}", memoryId);
            return false;
        }

        Message message = memoryIdToMessage.remove(memoryId);
        String contentHash = memoryIdToContentHash.remove(memoryId);
        
        if (contentHash != null) {
            contentHashToMemoryId.remove(contentHash);
        }

        // 从基础记忆中移除
        super.removeMessage(message);

        log.debug("Deleted message from long-term memory: {}", memoryId);
        return true;
    }

    /**
     * 批量删除
     *
     * @param memoryIds 记忆 ID 列表
     * @return 每个 ID 的删除结果
     */
    public Map<String, Boolean> deleteAll(List<String> memoryIds) {
        if (memoryIds == null || memoryIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return memoryIds.stream()
                .collect(Collectors.toMap(
                        id -> id,
                        this::delete
                ));
    }

    /**
     * 更新指定记忆 ID 的消息
     *
     * @param memoryId 记忆 ID
     * @param newMessage 新消息
     * @return 是否成功更新
     */
    public boolean update(String memoryId, Message newMessage) {
        if (!memoryIdToMessage.containsKey(memoryId)) {
            log.warn("Memory ID not found for update: {}", memoryId);
            return false;
        }

        if (newMessage == null || newMessage.getContent() == null) {
            log.warn("Cannot update with null message or null content");
            return false;
        }

        // 先删除旧消息
        Message oldMessage = memoryIdToMessage.get(memoryId);
        String oldContentHash = memoryIdToContentHash.get(memoryId);
        
        if (oldContentHash != null) {
            contentHashToMemoryId.remove(oldContentHash);
        }
        super.removeMessage(oldMessage);

        // 添加新消息
        String newContentHash = calculateContentHash(newMessage.getContent());
        memoryIdToMessage.put(memoryId, newMessage);
        memoryIdToContentHash.put(memoryId, newContentHash);
        contentHashToMemoryId.put(newContentHash, memoryId);
        super.addMessage(newMessage);

        log.debug("Updated message in long-term memory: {}", memoryId);
        return true;
    }

    /**
     * 搜索包含指定关键词的消息
     *
     * @param keyword 关键词
     * @param limit 最大结果数
     * @return 匹配的消息及其记忆 ID
     */
    public Map<String, Message> search(String keyword, Integer limit) {
        if (keyword == null || keyword.isEmpty()) {
            return Collections.emptyMap();
        }

        return memoryIdToMessage.entrySet().stream()
                .filter(entry -> {
                    Message msg = entry.getValue();
                    return msg.getContent() != null && 
                           msg.getContent().toString().toLowerCase().contains(keyword.toLowerCase());
                })
                .limit(limit != null ? limit : Integer.MAX_VALUE)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    /**
     * 获取统计信息
     */
    public MemoryStatistics getStatistics() {
        return new MemoryStatistics(
                memoryIdToMessage.size(),
                contentHashToMemoryId.size(),
                super.size()
        );
    }

    /**
     * 清空长期记忆
     */
    @Override
    public void clear() {
        super.clear();
        memoryIdToMessage.clear();
        contentHashToMemoryId.clear();
        memoryIdToContentHash.clear();
        log.info("Long-term memory cleared");
    }

    /**
     * 内存统计信息
     */
    @Data
    public static class MemoryStatistics {
        private final int totalMemories;
        private final int uniqueContents;
        private final int baseMemorySize;

        public MemoryStatistics(int totalMemories, int uniqueContents, int baseMemorySize) {
            this.totalMemories = totalMemories;
            this.uniqueContents = uniqueContents;
            this.baseMemorySize = baseMemorySize;
        }

        @Override
        public String toString() {
            return String.format("MemoryStatistics(total=%d, unique=%d, base=%d)", 
                    totalMemories, uniqueContents, baseMemorySize);
        }
    }
}
