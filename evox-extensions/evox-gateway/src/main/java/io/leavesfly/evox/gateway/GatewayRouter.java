package io.leavesfly.evox.gateway;

import io.leavesfly.evox.gateway.audit.AuditLogger;
import io.leavesfly.evox.gateway.auth.ApiKeyAuthProvider;
import io.leavesfly.evox.gateway.auth.UserSession;
import io.leavesfly.evox.gateway.config.GatewayConfig;
import io.leavesfly.evox.gateway.ratelimit.RateLimiter;
import io.leavesfly.evox.gateway.session.SessionManager;

import java.time.Duration;
import java.util.UUID;

public class GatewayRouter {
    
    private final ApiKeyAuthProvider authProvider;
    private final RateLimiter rateLimiter;
    private final SessionManager sessionManager;
    private final AuditLogger auditLogger;
    private final GatewayConfig config;
    
    public GatewayRouter(GatewayConfig config) {
        this.config = config;
        this.authProvider = new ApiKeyAuthProvider(config.getApiKeys(), config.isAllowAnonymousAccess());
        this.rateLimiter = new RateLimiter(config.getRateLimitPerMinute());
        this.sessionManager = new SessionManager(Duration.ofHours(config.getSessionTimeoutHours()));
        this.auditLogger = new AuditLogger(config.getMaxAuditEvents());
    }
    
    public GatewayContext processRequest(GatewayRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            if (!authenticate(request)) {
                throw new GatewayException("AUTH_FAILED", "Authentication failed");
            }
            
            if (!checkRateLimit(request)) {
                throw new GatewayException("RATE_LIMIT_EXCEEDED", "Rate limit exceeded");
            }
            
            UserSession session = createOrRecoverSession(request);
            
            return new GatewayContext(
                    request.getUserId(),
                    request.getChannelId(),
                    session,
                    true
            );
            
        } catch (GatewayException e) {
            throw e;
        } catch (Exception e) {
            throw new GatewayException("INTERNAL_ERROR", "Internal server error: " + e.getMessage());
        }
    }
    
    public void recordResponse(GatewayContext context, String response, long durationMs) {
        auditLogger.logEvent(
                context.getUserId(),
                context.getChannelId(),
                "request",
                "Request processed",
                response,
                true,
                durationMs
        );
    }
    
    private boolean authenticate(GatewayRequest request) {
        return authProvider.authenticate(request.getApiKey()).isPresent();
    }
    
    private boolean checkRateLimit(GatewayRequest request) {
        return rateLimiter.tryAcquire(request.getUserId());
    }
    
    private UserSession createOrRecoverSession(GatewayRequest request) {
        return sessionManager.getSession(request.getUserId())
                .orElseGet(() -> {
                    UserSession newSession = UserSession.builder()
                            .sessionId(UUID.randomUUID().toString())
                            .userId(request.getUserId())
                            .channelId(request.getChannelId())
                            .build();
                    sessionManager.registerSession(newSession);
                    return newSession;
                });
    }
    
    public ApiKeyAuthProvider getAuthProvider() {
        return authProvider;
    }
    
    public RateLimiter getRateLimiter() {
        return rateLimiter;
    }
    
    public SessionManager getSessionManager() {
        return sessionManager;
    }
    
    public AuditLogger getAuditLogger() {
        return auditLogger;
    }
    
    public GatewayConfig getConfig() {
        return config;
    }
    
    public static class GatewayRequest {
        private final String apiKey;
        private final String userId;
        private final String channelId;
        private final String message;
        
        public GatewayRequest(String apiKey, String userId, String channelId, String message) {
            this.apiKey = apiKey;
            this.userId = userId;
            this.channelId = channelId;
            this.message = message;
        }
        
        public String getApiKey() {
            return apiKey;
        }
        
        public String getUserId() {
            return userId;
        }
        
        public String getChannelId() {
            return channelId;
        }
        
        public String getMessage() {
            return message;
        }
    }
    
    public static class GatewayContext {
        private final String userId;
        private final String channelId;
        private final UserSession session;
        private final boolean authenticated;
        
        public GatewayContext(String userId, String channelId, UserSession session, boolean authenticated) {
            this.userId = userId;
            this.channelId = channelId;
            this.session = session;
            this.authenticated = authenticated;
        }
        
        public String getUserId() {
            return userId;
        }
        
        public String getChannelId() {
            return channelId;
        }
        
        public UserSession getSession() {
            return session;
        }
        
        public boolean isAuthenticated() {
            return authenticated;
        }
    }
    
    public static class GatewayException extends RuntimeException {
        private final String errorCode;
        
        public GatewayException(String errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }
        
        public String getErrorCode() {
            return errorCode;
        }
    }
}
