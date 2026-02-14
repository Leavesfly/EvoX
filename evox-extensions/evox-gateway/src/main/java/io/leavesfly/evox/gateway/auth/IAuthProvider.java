package io.leavesfly.evox.gateway.auth;

import java.util.Optional;

public interface IAuthProvider {

    Optional<UserSession> authenticate(String credential);

    Optional<UserSession> authenticateByChannel(String channelId, String channelUserId);

    String getProviderType();
}
