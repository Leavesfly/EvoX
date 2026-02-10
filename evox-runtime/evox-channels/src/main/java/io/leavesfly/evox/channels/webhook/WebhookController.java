package io.leavesfly.evox.channels.webhook;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.leavesfly.evox.channels.core.ChannelMessage;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
public class WebhookController {

    private final WebhookChannel webhookChannel;
    private static final long RESPONSE_TIMEOUT_SECONDS = 60;

    public WebhookController(WebhookChannel webhookChannel) {
        this.webhookChannel = webhookChannel;
    }

    @PostMapping("${evox.channels.webhook.path:/api/webhook}")
    public ResponseEntity<WebhookResponse> handleWebhook(
            @RequestBody WebhookRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (!webhookChannel.validateAuthToken(extractToken(authHeader))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(WebhookResponse.error("Unauthorized"));
        }

        try {
            CompletableFuture<ChannelMessage> responseFuture = webhookChannel.handleIncomingMessage(
                    request.getSenderId(),
                    request.getMessage(),
                    request.getMetadata());

            ChannelMessage response = responseFuture.get(RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            return ResponseEntity.ok(WebhookResponse.success(response.getContent()));
        } catch (Exception e) {
            log.error("Error processing webhook request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(WebhookResponse.error("Internal server error: " + e.getMessage()));
        }
    }

    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return authHeader;
    }

    @Data
    public static class WebhookRequest {
        private String senderId;
        private String message;
        private Map<String, Object> metadata;
    }

    @Data
    public static class WebhookResponse {
        private boolean success;
        private String message;
        private String error;

        public static WebhookResponse success(String message) {
            WebhookResponse response = new WebhookResponse();
            response.setSuccess(true);
            response.setMessage(message);
            return response;
        }

        public static WebhookResponse error(String error) {
            WebhookResponse response = new WebhookResponse();
            response.setSuccess(false);
            response.setError(error);
            return response;
        }
    }
}
