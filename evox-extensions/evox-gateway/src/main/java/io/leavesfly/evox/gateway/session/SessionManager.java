package io.leavesfly.evox.gateway.session;

import io.leavesfly.evox.gateway.auth.UserSession;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class SessionManager {

    private final Map<String, UserSession> sessionsByUserId = new ConcurrentHashMap<>();
    private final Map<String, UserSession> sessionsByChannelUser = new ConcurrentHashMap<>();
    private final Duration sessionTimeout;
    private ScheduledExecutorService cleanupExecutor;

    public SessionManager() {
        this(Duration.ofHours(24));
    }

    public SessionManager(Duration sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    public void start() {
        cleanupExecutor = Executors.newSingleThreadScheduledExecutor(
                r -> new Thread(r, "session-cleanup"));
        cleanupExecutor.scheduleWithFixedDelay(this::cleanupExpiredSessions,
                1, 1, TimeUnit.HOURS);
        log.info("SessionManager started with timeout: {}", sessionTimeout);
    }

    public void stop() {
        if (cleanupExecutor != null) {
            cleanupExecutor.shutdown();
        }
    }

    public void registerSession(UserSession session) {
        sessionsByUserId.put(session.getUserId(), session);
        if (session.getChannelId() != null && session.getChannelUserId() != null) {
            String channelKey = buildChannelKey(session.getChannelId(), session.getChannelUserId());
            sessionsByChannelUser.put(channelKey, session);
        }
    }

    public Optional<UserSession> getSession(String userId) {
        UserSession session = sessionsByUserId.get(userId);
        if (session != null && !isExpired(session)) {
            session.touch();
            return Optional.of(session);
        }
        return Optional.empty();
    }

    public Optional<UserSession> getSessionByChannel(String channelId, String channelUserId) {
        String channelKey = buildChannelKey(channelId, channelUserId);
        UserSession session = sessionsByChannelUser.get(channelKey);
        if (session != null && !isExpired(session)) {
            session.touch();
            return Optional.of(session);
        }
        return Optional.empty();
    }

    public void removeSession(String userId) {
        UserSession removed = sessionsByUserId.remove(userId);
        if (removed != null && removed.getChannelId() != null) {
            String channelKey = buildChannelKey(removed.getChannelId(), removed.getChannelUserId());
            sessionsByChannelUser.remove(channelKey);
        }
    }

    public int getActiveSessionCount() {
        return sessionsByUserId.size();
    }

    private boolean isExpired(UserSession session) {
        return Duration.between(session.getLastActiveAt(), Instant.now()).compareTo(sessionTimeout) > 0;
    }

    private void cleanupExpiredSessions() {
        int removed = 0;
        for (Map.Entry<String, UserSession> entry : sessionsByUserId.entrySet()) {
            if (isExpired(entry.getValue())) {
                removeSession(entry.getKey());
                removed++;
            }
        }
        if (removed > 0) {
            log.info("Cleaned up {} expired sessions", removed);
        }
    }

    private String buildChannelKey(String channelId, String channelUserId) {
        return channelId + ":" + channelUserId;
    }
}
