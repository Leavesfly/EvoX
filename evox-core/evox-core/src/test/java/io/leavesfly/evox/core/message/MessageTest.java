package io.leavesfly.evox.core.message;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;

/**
 * Message类测试
 */
class MessageTest {

    @Test
    void testCreateInputMessage() {
        Message msg = Message.inputMessage("test content");
        assertEquals(MessageType.INPUT, msg.getMessageType());
        assertEquals("test content", msg.getContent());
        assertNotNull(msg.getMessageId());
        assertNotNull(msg.getTimestamp());
    }

    @Test
    void testCreateResponseMessage() {
        Message msg = Message.responseMessage("response", "agent1", "action1");
        assertEquals(MessageType.RESPONSE, msg.getMessageType());
        assertEquals("agent1", msg.getAgent());
        assertEquals("action1", msg.getAction());
    }

    @Test
    void testBuilderPattern() {
        Message msg = Message.builder()
                .content("test")
                .messageType(MessageType.SYSTEM)
                .agent("testAgent")
                .workflowGoal("testGoal")
                .nextActions(Arrays.asList("action1", "action2"))
                .build();
        
        assertEquals("test", msg.getContent());
        assertEquals(MessageType.SYSTEM, msg.getMessageType());
        assertEquals("testAgent", msg.getAgent());
        assertEquals("testGoal", msg.getWorkflowGoal());
        assertEquals(2, msg.getNextActions().size());
    }
}
