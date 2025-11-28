package io.leavesfly.evox.memory;

import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.core.message.MessageType;
import io.leavesfly.evox.memory.base.BaseMemory;
import io.leavesfly.evox.memory.longterm.InMemoryLongTermMemory;
import io.leavesfly.evox.memory.shortterm.ShortTermMemory;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 记忆系统测试
 */
@Slf4j
class MemoryTest {

    private Message message1;
    private Message message2;
    private Message message3;

    @BeforeEach
    void setUp() {
        message1 = Message.builder()
                .content("Hello World")
                .messageType(MessageType.INPUT)
                .action("test_action")
                .workflowGoal("test_goal")
                .build();

        message2 = Message.builder()
                .content("Second message")
                .messageType(MessageType.RESPONSE)
                .action("test_action")
                .workflowGoal("test_goal")
                .build();

        message3 = Message.builder()
                .content("Third message")
                .messageType(MessageType.OUTPUT)
                .action("another_action")
                .workflowGoal("another_goal")
                .build();
    }

    // ==================== BaseMemory 测试 ====================

    @Test
    void testBaseMemoryInitialization() {
        BaseMemory memory = new BaseMemory();
        memory.initModule();

        assertNotNull(memory.getMemoryId());
        assertNotNull(memory.getTimestamp());
        assertEquals(0, memory.size());
        assertTrue(memory.isEmpty());
    }

    @Test
    void testBaseMemoryAddMessage() {
        BaseMemory memory = new BaseMemory();
        memory.initModule();

        memory.addMessage(message1);
        assertEquals(1, memory.size());
        assertFalse(memory.isEmpty());

        // 重复添加相同消息应该被忽略
        memory.addMessage(message1);
        assertEquals(1, memory.size());
    }

    @Test
    void testBaseMemoryAddMessages() {
        BaseMemory memory = new BaseMemory();
        memory.initModule();

        memory.addMessages(Arrays.asList(message1, message2, message3));
        assertEquals(3, memory.size());
    }

    @Test
    void testBaseMemoryCapacity() {
        BaseMemory memory = new BaseMemory();
        memory.setCapacity(2);
        memory.initModule();

        memory.addMessage(message1);
        memory.addMessage(message2);
        assertEquals(2, memory.size());

        // 超过容量应该删除最旧的
        memory.addMessage(message3);
        assertEquals(2, memory.size());
        
        List<Message> messages = memory.getAll();
        assertFalse(messages.contains(message1)); // 最旧的应该被删除
        assertTrue(messages.contains(message2));
        assertTrue(messages.contains(message3));
    }

    @Test
    void testBaseMemoryGet() {
        BaseMemory memory = new BaseMemory();
        memory.initModule();
        memory.addMessages(Arrays.asList(message1, message2, message3));

        // 获取全部
        List<Message> all = memory.get(null);
        assertEquals(3, all.size());

        // 获取最近 2 条
        List<Message> recent = memory.get(2);
        assertEquals(2, recent.size());
        assertEquals(message2, recent.get(0));
        assertEquals(message3, recent.get(1));

        // 获取超过总数
        List<Message> more = memory.get(10);
        assertEquals(3, more.size());
    }

    @Test
    void testBaseMemoryGetByAction() {
        BaseMemory memory = new BaseMemory();
        memory.initModule();
        memory.addMessages(Arrays.asList(message1, message2, message3));

        List<Message> byAction = memory.getByAction("test_action", null);
        assertEquals(2, byAction.size());

        List<Message> limited = memory.getByAction("test_action", 1);
        assertEquals(1, limited.size());
    }

    @Test
    void testBaseMemoryGetByWorkflowGoal() {
        BaseMemory memory = new BaseMemory();
        memory.initModule();
        memory.addMessages(Arrays.asList(message1, message2, message3));

        List<Message> byGoal = memory.getByWorkflowGoal("test_goal", null);
        assertEquals(2, byGoal.size());
    }

    @Test
    void testBaseMemoryRemoveMessage() {
        BaseMemory memory = new BaseMemory();
        memory.initModule();
        memory.addMessages(Arrays.asList(message1, message2));

        memory.removeMessage(message1);
        assertEquals(1, memory.size());
        assertFalse(memory.getAll().contains(message1));
    }

    @Test
    void testBaseMemoryClear() {
        BaseMemory memory = new BaseMemory();
        memory.initModule();
        memory.addMessages(Arrays.asList(message1, message2, message3));

        memory.clear();
        assertEquals(0, memory.size());
        assertTrue(memory.isEmpty());
    }

    @Test
    void testBaseMemoryGetLastMessage() {
        BaseMemory memory = new BaseMemory();
        memory.initModule();

        assertNull(memory.getLastMessage());

        memory.addMessage(message1);
        assertEquals(message1, memory.getLastMessage());

        memory.addMessage(message2);
        assertEquals(message2, memory.getLastMessage());
    }

    // ==================== ShortTermMemory 测试 ====================

    @Test
    void testShortTermMemoryInitialization() {
        ShortTermMemory memory = new ShortTermMemory(5);

        assertNotNull(memory.getMemoryId());
        assertEquals(5, memory.getMaxSize());
        assertEquals(0, memory.size());
        assertTrue(memory.isEmpty());
    }

    @Test
    void testShortTermMemoryAddMessage() {
        ShortTermMemory memory = new ShortTermMemory(3);

        memory.addMessage(message1);
        assertEquals(1, memory.size());

        memory.addMessage(message2);
        assertEquals(2, memory.size());

        memory.addMessage(message3);
        assertEquals(3, memory.size());
        assertTrue(memory.isFull());
    }

    @Test
    void testShortTermMemorySlidingWindow() {
        ShortTermMemory memory = new ShortTermMemory(2);

        memory.addMessage(message1);
        memory.addMessage(message2);
        assertEquals(2, memory.size());

        // 添加第三条消息应该删除第一条
        memory.addMessage(message3);
        assertEquals(2, memory.size());
        
        List<Message> messages = memory.getAll();
        assertFalse(messages.contains(message1));
        assertTrue(messages.contains(message2));
        assertTrue(messages.contains(message3));
    }

    @Test
    void testShortTermMemoryGet() {
        ShortTermMemory memory = new ShortTermMemory(5);
        memory.addMessages(Arrays.asList(message1, message2, message3));

        List<Message> all = memory.getAll();
        assertEquals(3, all.size());

        List<Message> recent = memory.get(2);
        assertEquals(2, recent.size());
        assertEquals(message2, recent.get(0));
        assertEquals(message3, recent.get(1));
    }

    @Test
    void testShortTermMemoryGetLastMessage() {
        ShortTermMemory memory = new ShortTermMemory(5);

        assertNull(memory.getLastMessage());

        memory.addMessage(message1);
        assertEquals(message1, memory.getLastMessage());

        memory.addMessage(message2);
        assertEquals(message2, memory.getLastMessage());
    }

    @Test
    void testShortTermMemoryGetFirstMessage() {
        ShortTermMemory memory = new ShortTermMemory(5);

        assertNull(memory.getFirstMessage());

        memory.addMessages(Arrays.asList(message1, message2, message3));
        assertEquals(message1, memory.getFirstMessage());
    }

    @Test
    void testShortTermMemoryResize() {
        ShortTermMemory memory = new ShortTermMemory(5);
        memory.addMessages(Arrays.asList(message1, message2, message3));

        assertEquals(3, memory.size());

        // 扩大容量
        memory.resize(10);
        assertEquals(10, memory.getMaxSize());
        assertEquals(3, memory.size());

        // 缩小容量
        memory.resize(2);
        assertEquals(2, memory.getMaxSize());
        assertEquals(2, memory.size());
        
        List<Message> messages = memory.getAll();
        assertFalse(messages.contains(message1)); // 最旧的被删除
    }

    @Test
    void testShortTermMemoryRemainingCapacity() {
        ShortTermMemory memory = new ShortTermMemory(5);

        assertEquals(5, memory.getRemainingCapacity());

        memory.addMessage(message1);
        assertEquals(4, memory.getRemainingCapacity());

        memory.addMessages(Arrays.asList(message2, message3));
        assertEquals(2, memory.getRemainingCapacity());
    }

    @Test
    void testShortTermMemoryClear() {
        ShortTermMemory memory = new ShortTermMemory(5);
        memory.addMessages(Arrays.asList(message1, message2, message3));

        memory.clear();
        assertEquals(0, memory.size());
        assertTrue(memory.isEmpty());
    }

    // ==================== InMemoryLongTermMemory 测试 ====================

    @Test
    void testLongTermMemoryInitialization() {
        InMemoryLongTermMemory memory = new InMemoryLongTermMemory();
        memory.initModule();

        assertNotNull(memory.getMemoryId());
        assertEquals(0, memory.size());
    }

    @Test
    void testLongTermMemoryAdd() {
        InMemoryLongTermMemory memory = new InMemoryLongTermMemory();
        memory.initModule();

        String memoryId = memory.add(message1);
        assertNotNull(memoryId);
        assertEquals(1, memory.size());
    }

    @Test
    void testLongTermMemoryDeduplication() {
        InMemoryLongTermMemory memory = new InMemoryLongTermMemory();
        memory.initModule();

        String id1 = memory.add(message1);
        
        // 添加相同内容的消息
        Message duplicate = Message.builder()
                .content("Hello World")
                .messageType(MessageType.RESPONSE)
                .build();
        
        String id2 = memory.add(duplicate);
        
        // 应该返回相同的 ID（去重）
        assertEquals(id1, id2);
        assertEquals(1, memory.size());
    }

    @Test
    void testLongTermMemoryAddAll() {
        InMemoryLongTermMemory memory = new InMemoryLongTermMemory();
        memory.initModule();

        List<String> ids = memory.addAll(Arrays.asList(message1, message2, message3));
        assertEquals(3, ids.size());
        assertEquals(3, memory.size());
    }

    @Test
    void testLongTermMemoryGetByMemoryId() {
        InMemoryLongTermMemory memory = new InMemoryLongTermMemory();
        memory.initModule();

        String memoryId = memory.add(message1);
        Message retrieved = memory.getByMemoryId(memoryId);

        assertNotNull(retrieved);
        assertEquals(message1.getContent(), retrieved.getContent());
    }

    @Test
    void testLongTermMemoryGetByMemoryIds() {
        InMemoryLongTermMemory memory = new InMemoryLongTermMemory();
        memory.initModule();

        String id1 = memory.add(message1);
        String id2 = memory.add(message2);

        Map<String, Message> results = memory.getByMemoryIds(Arrays.asList(id1, id2));
        assertEquals(2, results.size());
        assertTrue(results.containsKey(id1));
        assertTrue(results.containsKey(id2));
    }

    @Test
    void testLongTermMemoryDelete() {
        InMemoryLongTermMemory memory = new InMemoryLongTermMemory();
        memory.initModule();

        String memoryId = memory.add(message1);
        assertEquals(1, memory.size());

        boolean deleted = memory.delete(memoryId);
        assertTrue(deleted);
        assertEquals(0, memory.size());
        assertNull(memory.getByMemoryId(memoryId));
    }

    @Test
    void testLongTermMemoryUpdate() {
        InMemoryLongTermMemory memory = new InMemoryLongTermMemory();
        memory.initModule();

        String memoryId = memory.add(message1);
        
        Message updatedMessage = Message.builder()
                .content("Updated content")
                .messageType(MessageType.SYSTEM)
                .build();

        boolean updated = memory.update(memoryId, updatedMessage);
        assertTrue(updated);

        Message retrieved = memory.getByMemoryId(memoryId);
        assertEquals("Updated content", retrieved.getContent());
    }

    @Test
    void testLongTermMemorySearch() {
        InMemoryLongTermMemory memory = new InMemoryLongTermMemory();
        memory.initModule();

        memory.addAll(Arrays.asList(message1, message2, message3));

        Map<String, Message> results = memory.search("message", null);
        assertEquals(2, results.size()); // message2 和 message3 包含 "message"

        Map<String, Message> limited = memory.search("message", 1);
        assertEquals(1, limited.size());
    }

    @Test
    void testLongTermMemoryStatistics() {
        InMemoryLongTermMemory memory = new InMemoryLongTermMemory();
        memory.initModule();

        memory.addAll(Arrays.asList(message1, message2, message3));

        InMemoryLongTermMemory.MemoryStatistics stats = memory.getStatistics();
        assertEquals(3, stats.getTotalMemories());
        assertEquals(3, stats.getUniqueContents());
        assertEquals(3, stats.getBaseMemorySize());
    }

    @Test
    void testLongTermMemoryClear() {
        InMemoryLongTermMemory memory = new InMemoryLongTermMemory();
        memory.initModule();

        memory.addAll(Arrays.asList(message1, message2, message3));
        assertEquals(3, memory.size());

        memory.clear();
        assertEquals(0, memory.size());
        assertEquals(0, memory.getStatistics().getTotalMemories());
    }

    // ==================== 集成测试 ====================

    @Test
    void testMemoryIntegration() {
        // 短期记忆
        ShortTermMemory shortTerm = new ShortTermMemory(3);
        shortTerm.addMessages(Arrays.asList(message1, message2, message3));

        // 长期记忆
        InMemoryLongTermMemory longTerm = new InMemoryLongTermMemory();
        longTerm.initModule();
        
        // 将短期记忆转移到长期记忆
        List<Message> shortTermMessages = shortTerm.getAll();
        List<String> memoryIds = longTerm.addAll(shortTermMessages);

        assertEquals(3, memoryIds.size());
        assertEquals(3, longTerm.size());

        // 清空短期记忆
        shortTerm.clear();
        assertEquals(0, shortTerm.size());

        // 长期记忆仍然保留
        assertEquals(3, longTerm.size());

        log.info("Memory integration test completed successfully");
    }
}
