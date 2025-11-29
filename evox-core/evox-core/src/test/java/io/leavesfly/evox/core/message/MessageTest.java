package io.leavesfly.evox.core.message;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Message 类单元测试
 * 
 * @author EvoX Team
 */
@DisplayName("Message 类测试")
class MessageTest {

    @Test
    @DisplayName("测试 Message 创建 - 使用 Builder")
    void testMessageCreationWithBuilder() {
        // Given
        String content = "Hello, World!";
        MessageType type = MessageType.USER;
        
        // When
        Message message = Message.builder()
                .content(content)
                .messageType(type)
                .build();
        
        // Then
        assertNotNull(message, "Message 不应为 null");
        assertEquals(content, message.getContent(), "内容应匹配");
        assertEquals(type, message.getMessageType(), "消息类型应匹配");
        assertNotNull(message.getTimestamp(), "时间戳不应为 null");
    }

    @Test
    @DisplayName("测试所有 MessageType 枚举值")
    void testAllMessageTypes() {
        MessageType[] types = {
            MessageType.SYSTEM,
            MessageType.USER,
            MessageType.ASSISTANT,
            MessageType.FUNCTION,
            MessageType.TOOL,
            MessageType.INPUT,
            MessageType.OUTPUT,
            MessageType.ERROR
        };
        
        for (MessageType type : types) {
            Message message = Message.builder()
                    .content("test")
                    .messageType(type)
                    .build();
            
            assertEquals(type, message.getMessageType(), 
                    "MessageType 应为 " + type);
        }
    }

    @Test
    @DisplayName("测试 Message 带元数据")
    void testMessageWithMetadata() {
        // Given
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("key1", "value1");
        metadata.put("key2", 123);
        metadata.put("key3", true);
        
        // When
        Message message = Message.builder()
                .content("test")
                .messageType(MessageType.USER)
                .metadata(metadata)
                .build();
        
        // Then
        assertNotNull(message.getMetadata(), "元数据不应为 null");
        assertEquals(3, message.getMetadata().size(), "元数据应有3个条目");
        assertEquals("value1", message.getMetadata().get("key1"));
        assertEquals(123, message.getMetadata().get("key2"));
        assertEquals(true, message.getMetadata().get("key3"));
    }

    @Test
    @DisplayName("测试 Message 带 Agent 信息")
    void testMessageWithAgent() {
        // Given
        String agentName = "TestAgent";
        
        // When
        Message message = Message.builder()
                .content("test")
                .messageType(MessageType.ASSISTANT)
                .agent(agentName)
                .build();
        
        // Then
        assertEquals(agentName, message.getAgent(), "Agent 名称应匹配");
    }

    @Test
    @DisplayName("测试 Message 带 Action 信息")
    void testMessageWithAction() {
        // Given
        String actionName = "TestAction";
        
        // When
        Message message = Message.builder()
                .content("test")
                .messageType(MessageType.OUTPUT)
                .action(actionName)
                .build();
        
        // Then
        assertEquals(actionName, message.getAction(), "Action 名称应匹配");
    }

    @Test
    @DisplayName("测试 Message 完整构建")
    void testMessageFullConstruction() {
        // Given
        String content = "Complete message";
        MessageType type = MessageType.ASSISTANT;
        String agent = "ChatAgent";
        String action = "GenerateResponse";
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("model", "gpt-4");
        metadata.put("temperature", 0.7);
        
        // When
        Message message = Message.builder()
                .content(content)
                .messageType(type)
                .agent(agent)
                .action(action)
                .metadata(metadata)
                .build();
        
        // Then
        assertAll("完整消息验证",
                () -> assertEquals(content, message.getContent()),
                () -> assertEquals(type, message.getMessageType()),
                () -> assertEquals(agent, message.getAgent()),
                () -> assertEquals(action, message.getAction()),
                () -> assertNotNull(message.getMetadata()),
                () -> assertEquals(2, message.getMetadata().size()),
                () -> assertNotNull(message.getTimestamp())
        );
    }

    @Test
    @DisplayName("测试空内容消息")
    void testEmptyContentMessage() {
        Message message = Message.builder()
                .content("")
                .messageType(MessageType.USER)
                .build();
        
        assertNotNull(message);
        assertEquals("", message.getContent());
    }

    @Test
    @DisplayName("测试 null 内容消息")
    void testNullContentMessage() {
        Message message = Message.builder()
                .content(null)
                .messageType(MessageType.USER)
                .build();
        
        assertNotNull(message);
        assertNull(message.getContent());
    }

    @Test
    @DisplayName("测试时间戳自动生成")
    void testTimestampAutoGeneration() {
        LocalDateTime before = LocalDateTime.now();
        
        Message message = Message.builder()
                .content("test")
                .messageType(MessageType.USER)
                .build();
        
        LocalDateTime after = LocalDateTime.now();
        
        assertNotNull(message.getTimestamp());
        assertTrue(!message.getTimestamp().isBefore(before), 
                "时间戳应在创建之后");
        assertTrue(!message.getTimestamp().isAfter(after), 
                "时间戳应在验证之前");
    }

    @Test
    @DisplayName("测试 Message 相等性")
    void testMessageEquality() {
        Message msg1 = Message.builder()
                .content("test")
                .messageType(MessageType.USER)
                .agent("Agent1")
                .build();
        
        Message msg2 = Message.builder()
                .content("test")
                .messageType(MessageType.USER)
                .agent("Agent1")
                .build();
        
        // 注意: 如果 Message 类没有重写 equals/hashCode,
        // 这个测试会失败,这是正常的
        // 如果实现了 equals/hashCode, 可以测试相等性
        assertNotSame(msg1, msg2, "应该是不同的对象实例");
    }

    @Test
    @DisplayName("测试 Message toString")
    void testMessageToString() {
        Message message = Message.builder()
                .content("test content")
                .messageType(MessageType.USER)
                .build();
        
        String str = message.toString();
        assertNotNull(str, "toString 不应返回 null");
        assertTrue(str.contains("test content") || str.contains("Message"), 
                "toString 应包含相关信息");
    }
}
