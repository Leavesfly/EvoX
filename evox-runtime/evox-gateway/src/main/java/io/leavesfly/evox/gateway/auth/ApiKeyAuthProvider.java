package io.leavesfly.evox.gateway.auth;

import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class ApiKeyAuthProvider implements IAuthProvider {

    private final Map<String, UserSession> apiKeyToSession = new HashMap<>();
    private final boolean allowAnonymousChannelAccess;

    public ApiKeyAuthProvider(List<String> apiKeys, boolean allowAnonymousChannelAccess) {
        this.allowAnonymousChannelAccess = allowAnonymousChannelAccess;
        for (String apiKey : apiKeys) {
            apiKeyToSession.put(apiKey, UserSession.builder()
                    .sessionId(UUID.randomUUID().toString())
                    .userId("api-user-" + apiKey.substring(0, Math.min(8, apiKey.length())))
                    .username("API User")
                    .permissions(Set.of("*"))
                    .build());
        }
    }

    public ApiKeyAuthProvider(List<String> apiKeys) {
        this(apiKeys, true);
    }

    @Override
    public Optional<UserSession> authenticate(String credential) {
        if (credential == null || credential.isEmpty()) {
            return Optional.empty();
        }
        String apiKey = credential.startsWith("Bearer ") ? credential.substring(7) : credential;
        UserSession session = apiKeyToSession.get(apiKey);
        if (session != null) {
            session.touch();
            return Optional.of(session);
        }
        return Optional.empty();
    }

    @Override
    public Optional<UserSession> authenticateByChannel(String channelId, String channelUserId) {
        if (allowAnonymousChannelAccess) {
            UserSession session = UserSession.builder()
                    .sessionId(UUID.randomUUID().toString())
                    .userId(channelUserId)
                    .username(channelUserId)
                    .channelId(channelId)
                    .channelUserId(channelUserId)
                    .permissions(Set.of("chat", "tools"))
                    .build();
            return Optional.of(session);
        }
        return Optional.empty();
    }

    @Override
    public String getProviderType() {
        return "api-key";
    }
}
