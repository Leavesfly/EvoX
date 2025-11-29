package io.leavesfly.evox.workflow.integration;

import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.core.message.MessageType;
import io.leavesfly.evox.memory.longterm.InMemoryLongTermMemory;
import io.leavesfly.evox.memory.shortterm.ShortTermMemory;
import io.leavesfly.evox.storage.inmemory.InMemoryStorageHandler;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Memory + Storage 集成测试
 * 
 * 测试场景：
 * 1. Memory 使用 Storage 持久化消息
 * 2. Storage 存储和检索记忆数据
 * 3. 长期记忆与短期记忆的协同工作
 */
@Slf4j
class MemoryStorageIntegrationTest {

    private InMemoryStorageHandler storage;
    private ShortTermMemory shortTermMemory;
    private InMemoryLongTermMemory longTermMemory;

    @BeforeEach
    void setUp() {
        storage = new InMemoryStorageHandler();
        shortTermMemory = new ShortTermMemory(10);
        longTermMemory = new InMemoryLongTermMemory();
        longTermMemory.initModule();
    }

    /**
     * 测试 1: 使用 Storage 持久化短期记忆
     */
    @Test
    void testPersistShortTermMemory() {
        log.info("=== 测试持久化短期记忆 ===");
        
        // 添加消息到短期记忆
        for (int i = 1; i <= 5; i++) {
            Message msg = Message.builder()
                    .content("Message " + i)
                    .messageType(MessageType.INPUT)
                    .build();
            shortTermMemory.addMessage(msg);
        }
        
        assertEquals(5, shortTermMemory.size());
        
        // 将短期记忆序列化到 Storage
        List<Message> messages = shortTermMemory.getMessages();
        
        for (int i = 0; i < messages.size(); i++) {
            Message msg = messages.get(i);
            MemoryRecord record = new MemoryRecord();
            record.setId(UUID.randomUUID().toString());
            record.setMessageType(msg.getMessageType().name());
            record.setContent(msg.getContent().toString());
            record.setTimestamp(System.currentTimeMillis());
            
            Map<String, Object> data = new HashMap<>();
            data.put("id", record.getId());
            data.put("messageType", record.getMessageType());
            data.put("content", record.getContent());
            data.put("timestamp", record.getTimestamp());
            storage.save("short_term_memory", record.getId(), data);
        }
        
        // 验证存储
        Map<String, Map<String, Object>> stored = storage.loadAll("short_term_memory");
        assertEquals(5, stored.size(), "应该存储了5条记录");
        
        log.info("成功持久化 {} 条短期记忆", stored.size());
    }

    /**
     * 测试 2: 使用 Storage 持久化长期记忆
     */
    @Test
    void testPersistLongTermMemory() {
        log.info("=== 测试持久化长期记忆 ===");
        
        // 添加消息到长期记忆
        for (int i = 1; i <= 10; i++) {
            Message msg = Message.builder()
                    .content("Long term message " + i)
                    .messageType(MessageType.RESPONSE)
                    .build();
            longTermMemory.add(msg);
        }
        
        assertEquals(10, longTermMemory.size());
        
        // 序列化到 Storage
        List<Message> messages = longTermMemory.getAll();
        
        for (Message msg : messages) {
            MemoryRecord record = new MemoryRecord();
            record.setId(UUID.randomUUID().toString());
            record.setMessageType(msg.getMessageType().name());
            record.setContent(msg.getContent().toString());
            record.setTimestamp(System.currentTimeMillis());
            
            Map<String, Object> data = new HashMap<>();
            data.put("id", record.getId());
            data.put("messageType", record.getMessageType());
            data.put("content", record.getContent());
            data.put("timestamp", record.getTimestamp());
            storage.save("long_term_memory", record.getId(), data);
        }
        
        // 验证存储
        Map<String, Map<String, Object>> stored = storage.loadAll("long_term_memory");
        assertEquals(10, stored.size(), "应该存储了10条记录");
        
        log.info("成功持久化 {} 条长期记忆", stored.size());
    }

    /**
     * 测试 3: 从 Storage 恢复 Memory
     */
    @Test
    void testRestoreMemoryFromStorage() {
        log.info("=== 测试从 Storage 恢复 Memory ===");
        
        // 先存储一些数据
        for (int i = 1; i <= 5; i++) {
            MemoryRecord record = new MemoryRecord();
            record.setId("msg-" + i);
            record.setMessageType(MessageType.INPUT.name());
            record.setContent("Stored message " + i);
            record.setTimestamp(System.currentTimeMillis());
            
            Map<String, Object> data = new HashMap<>();
            data.put("id", record.getId());
            data.put("messageType", record.getMessageType());
            data.put("content", record.getContent());
            data.put("timestamp", record.getTimestamp());
            storage.save("memory_backup", record.getId(), data);
        }
        
        // 创建新的 Memory 并恢复
        ShortTermMemory restoredMemory = new ShortTermMemory(10);
        
        Map<String, Map<String, Object>> records = storage.loadAll("memory_backup");
        
        for (Map<String, Object> data : records.values()) {
            Message msg = Message.builder()
                    .content(data.get("content").toString())
                    .messageType(MessageType.valueOf(data.get("messageType").toString()))
                    .build();
            restoredMemory.addMessage(msg);
        }
        
        // 验证恢复
        assertEquals(5, restoredMemory.size(), "应该恢复了5条消息");
        
        List<Message> messages = restoredMemory.getMessages();
        assertEquals(5, messages.size());
        
        log.info("成功从 Storage 恢复 {} 条记忆", messages.size());
    }

    /**
     * 测试 4: 短期记忆溢出到长期记忆，并持久化
     */
    @Test
    void testMemoryOverflowWithStorage() {
        log.info("=== 测试记忆溢出与持久化 ===");
        
        ShortTermMemory stm = new ShortTermMemory(5);  // 容量为5
        InMemoryLongTermMemory ltm = new InMemoryLongTermMemory();
        ltm.initModule();
        
        // 添加超过容量的消息
        for (int i = 1; i <= 10; i++) {
            Message msg = Message.builder()
                    .content("Message " + i)
                    .messageType(MessageType.INPUT)
                    .build();
            
            stm.addMessage(msg);
            
            // 当短期记忆满时，转移到长期记忆
            if (stm.size() >= stm.getMaxMessages()) {
                List<Message> toArchive = stm.getMessages().subList(0, 1);
                for (Message m : toArchive) {
                    ltm.add(m);
                }
            }
        }
        
        // 验证短期记忆
        assertEquals(5, stm.size(), "短期记忆应该维持在最大容量");
        
        // 持久化长期记忆到 Storage
        List<Message> ltmMessages = ltm.getAll();
        
        for (Message msg : ltmMessages) {
            MemoryRecord record = new MemoryRecord();
            record.setId(UUID.randomUUID().toString());
            record.setMessageType(msg.getMessageType().name());
            record.setContent(msg.getContent().toString());
            record.setTimestamp(System.currentTimeMillis());
            
            Map<String, Object> data = new HashMap<>();
            data.put("id", record.getId());
            data.put("messageType", record.getMessageType());
            data.put("content", record.getContent());
            data.put("timestamp", record.getTimestamp());
            storage.save("ltm_archive", record.getId(), data);
        }
        
        // 验证存储
        Map<String, Map<String, Object>> archived = storage.loadAll("ltm_archive");
        assertTrue(archived.size() > 0, "应该有归档的长期记忆");
        
        log.info("短期记忆: {} 条，长期记忆: {} 条，已归档: {} 条", 
                stm.size(), ltm.size(), archived.size());
    }

    /**
     * 测试 5: 按条件查询存储的记忆
     */
    @Test
    void testQueryStoredMemory() {
        log.info("=== 测试查询存储的记忆 ===");
        
        // 存储不同类型的消息
        String[] messageTypes = {"INPUT", "RESPONSE", "ERROR", "SYSTEM"};
        
        for (int i = 0; i < 10; i++) {
            MemoryRecord record = new MemoryRecord();
            record.setId("msg-" + i);
            record.setMessageType(messageTypes[i % messageTypes.length]);
            record.setContent("Message " + i);
            record.setTimestamp(System.currentTimeMillis());
            
            Map<String, Object> data = new HashMap<>();
            data.put("id", record.getId());
            data.put("messageType", record.getMessageType());
            data.put("content", record.getContent());
            data.put("timestamp", record.getTimestamp());
            storage.save("messages", record.getId(), data);
        }
        
        // 查询所有 INPUT 类型的消息
        Map<String, Map<String, Object>> allRecords = storage.loadAll("messages");
        
        long inputCount = allRecords.values().stream()
                .filter(r -> "INPUT".equals(r.get("messageType")))
                .count();
        
        assertTrue(inputCount > 0, "应该有 INPUT 类型的消息");
        
        log.info("存储了 {} 条消息，其中 INPUT 类型: {} 条", allRecords.size(), inputCount);
    }

    /**
     * 测试 6: 批量存储和恢复记忆
     */
    @Test
    void testBatchMemoryOperations() {
        log.info("=== 测试批量记忆操作 ===");
        
        // 批量创建记忆记录
        Map<String, Map<String, Object>> records = new HashMap<>();
        for (int i = 1; i <= 20; i++) {
            MemoryRecord record = new MemoryRecord();
            record.setId("batch-" + i);
            record.setMessageType(MessageType.RESPONSE.name());
            record.setContent("Batch message " + i);
            record.setTimestamp(System.currentTimeMillis());
            
            Map<String, Object> data = new HashMap<>();
            data.put("id", record.getId());
            data.put("messageType", record.getMessageType());
            data.put("content", record.getContent());
            data.put("timestamp", record.getTimestamp());
            records.put(record.getId(), data);
        }
        
        // 批量保存
        storage.saveAll("batch_memory", records);
        
        // 验证
        Map<String, Map<String, Object>> loaded = storage.loadAll("batch_memory");
        assertEquals(20, loaded.size(), "应该批量保存了20条记录");
        
        // 批量恢复到 Memory
        InMemoryLongTermMemory restoredMemory = new InMemoryLongTermMemory();
        restoredMemory.initModule();
        
        for (Map<String, Object> data : loaded.values()) {
            Message msg = Message.builder()
                    .content(data.get("content").toString())
                    .messageType(MessageType.valueOf(data.get("messageType").toString()))
                    .build();
            restoredMemory.add(msg);
        }
        
        assertEquals(20, restoredMemory.size(), "应该恢复了20条消息");
        
        log.info("批量操作成功: 存储 {} 条 -> 恢复 {} 条", loaded.size(), restoredMemory.size());
    }

    /**
     * 记忆记录数据模型
     */
    @Data
    static class MemoryRecord {
        private String id;
        private String messageType;
        private String content;
        private Long timestamp;
        private Map<String, Object> metadata = new HashMap<>();
    }
}