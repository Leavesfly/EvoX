package io.leavesfly.evox.gateway.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSession {
    private String sessionId;
    private String userId;
    private String username;
    private String channelId;
    private String channelUserId;

    @Builder.Default
    private Instant createdAt = Instant.now();

    @Builder.Default
    private Instant lastActiveAt = Instant.now();

    @Builder.Default
    private Set<String> permissions = Set.of();

    @Builder.Default
    private Map<String, Object> attributes = new HashMap<>();

    public void touch() {
        this.lastActiveAt = Instant.now();
    }

    public boolean hasPermission(String permission) {
        return permissions.contains(permission) || permissions.contains("*");
    }
}
