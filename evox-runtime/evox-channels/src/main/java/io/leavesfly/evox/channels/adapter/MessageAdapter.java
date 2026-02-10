package io.leavesfly.evox.channels.adapter;

import io.leavesfly.evox.channels.core.ChannelMessage;
import io.leavesfly.evox.channels.core.MessageContentType;
import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.core.message.MessageType;

import java.util.HashMap;
import java.util.Map;

public class MessageAdapter {

    private MessageAdapter() {
    }

    public static Message toAgentMessage(ChannelMessage channelMessage) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("channelId", channelMessage.getChannelId());
        metadata.put("senderId", channelMessage.getSenderId());
        metadata.put("senderName", channelMessage.getSenderName());
        metadata.put("targetId", channelMessage.getTargetId());
        metadata.put("channelMessageId", channelMessage.getMessageId());
        if (channelMessage.getThreadId() != null) {
            metadata.put("threadId", channelMessage.getThreadId());
        }
        if (channelMessage.getMetadata() != null) {
            metadata.putAll(channelMessage.getMetadata());
        }

        return Message.builder()
                .content(channelMessage.getContent())
                .messageType(MessageType.INPUT)
                .metadata(metadata)
                .build();
    }

    public static ChannelMessage fromAgentMessage(Message agentMessage, String channelId, String targetId) {
        String content;
        if (agentMessage.getContent() instanceof String) {
            content = (String) agentMessage.getContent();
        } else if (agentMessage.getContent() != null) {
            content = agentMessage.getContent().toString();
        } else {
            content = "";
        }

        return ChannelMessage.builder()
                .channelId(channelId)
                .targetId(targetId)
                .content(content)
                .contentType(MessageContentType.TEXT)
                .senderName(agentMessage.getAgent())
                .build();
    }
}
