package io.leavesfly.evox.channels.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelUser {
    private String userId;
    private String username;
    private String displayName;
    private String channelId;
    private String avatarUrl;
    private Map<String, Object> metadata;
}
