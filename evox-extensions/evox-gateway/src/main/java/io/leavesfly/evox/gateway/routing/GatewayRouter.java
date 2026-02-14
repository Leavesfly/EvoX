package io.leavesfly.evox.gateway.routing;

import io.leavesfly.evox.core.agent.IAgent;
import io.leavesfly.evox.core.agent.IAgentManager;
import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.gateway.audit.AuditLogger;
import io.leavesfly.evox.gateway.auth.IAuthProvider;
import io.leavesfly.evox.gateway.auth.UserSession;
import io.leavesfly.evox.gateway.ratelimit.RateLimiter;
import io.leavesfly.evox.gateway.session.SessionManager;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Optional;

@Slf4j
@Builder
public class GatewayRouter {

    private final IAuthProvider authProvider;
    private final SessionManager sessionManager;
    private final RateLimiter rateLimiter;
    private final AuditLogger auditLogger;
    private final IAgentManager agentManager;
    private final String defaultAgentName;

    public Message route(String channelId, String channelUserId, Message inputMessage) {
        long startTime = System.currentTimeMillis();
        String userId = channelUserId;

        try {
            Optional<UserSession> sessionOpt = sessionManager.getSessionByChannel(channelId, channelUserId);
            UserSession session;
            if (sessionOpt.isPresent()) {
                session = sessionOpt.get();
            } else {
                Optional<UserSession> authResult = authProvider.authenticateByChannel(channelId, channelUserId);
                if (authResult.isEmpty()) {
                    return Message.errorMessage("Authentication failed");
                }
                session = authResult.get();
                sessionManager.registerSession(session);
            }
            userId = session.getUserId();

            if (!rateLimiter.tryAcquire(userId)) {
                return Message.errorMessage("Rate limit exceeded. Please try again later.");
            }

            IAgent agent = selectAgent(inputMessage);
            if (agent == null) {
                return Message.errorMessage("No available agent to handle your request.");
            }

            Message response = agent.execute(null, Collections.singletonList(inputMessage));

            long duration = System.currentTimeMillis() - startTime;
            String inputContent = inputMessage.getContent() != null ? inputMessage.getContent().toString() : "";
            String outputContent = response != null && response.getContent() != null ? response.getContent().toString() : "";
            auditLogger.logEvent(userId, channelId, "chat", inputContent, outputContent, true, duration);

            return response;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Error routing message from channel [{}], user [{}]", channelId, userId, e);
            auditLogger.logEvent(userId, channelId, "chat",
                    inputMessage.getContent() != null ? inputMessage.getContent().toString() : "",
                    e.getMessage(), false, duration);
            return Message.errorMessage("Internal error: " + e.getMessage());
        }
    }

    private IAgent selectAgent(Message inputMessage) {
        if (defaultAgentName != null && agentManager != null) {
            IAgent agent = agentManager.getAgent(defaultAgentName);
            if (agent != null) {
                return agent;
            }
        }

        if (agentManager != null) {
            var agents = agentManager.getAllAgents();
            if (!agents.isEmpty()) {
                return agents.values().iterator().next();
            }
        }

        return null;
    }
}
