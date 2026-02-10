package io.leavesfly.evox.channels.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelMessage {

    @Builder.Default
    private String messageId = UUID.randomUUID().toString();

    private String channelId;

    private String senderId;

    private String senderName;

    private String targetId;

    private String content;

    @Builder.Default
    private MessageContentType contentType = MessageContentType.TEXT;

    private byte[] mediaData;

    private String mediaUrl;

    private String mediaFileName;

    @Builder.Default
    private Instant timestamp = Instant.now();

    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    private ChannelMessage replyTo;

    private String threadId;

    public static ChannelMessage textMessage(String channelId, String targetId, String content) {
        return ChannelMessage.builder()
                .channelId(channelId)
                .targetId(targetId)
                .content(content)
                .contentType(MessageContentType.TEXT)
                .build();
    }

    public static ChannelMessage replyMessage(String channelId, String targetId, String content, ChannelMessage original) {
        return ChannelMessage.builder()
                .channelId(channelId)
                .targetId(targetId)
                .content(content)
                .contentType(MessageContentType.TEXT)
                .replyTo(original)
                .threadId(original.getThreadId() != null ? original.getThreadId() : original.getMessageId())
                .build();
    }
}
