package io.leavesfly.evox.channels.whatsapp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RestController
@RequestMapping
public class WhatsAppWebhookController {
    
    private static final Map<String, WhatsAppChannel> channelRegistry = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public static void registerChannel(String channelId, WhatsAppChannel channel) {
        channelRegistry.put(channelId, channel);
    }
    
    public static void unregisterChannel(String channelId) {
        channelRegistry.remove(channelId);
    }
    
    @GetMapping("/api/whatsapp/webhook")
    public ResponseEntity<String> verifyWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String verifyToken,
            @RequestParam("hub.challenge") String challenge) {
        
        log.info("Received webhook verification request: mode={}, verifyToken={}", mode, verifyToken);
        
        for (WhatsAppChannel channel : channelRegistry.values()) {
            WhatsAppConfig config = (WhatsAppConfig) channel.getConfig();
            if (verifyToken.equals(config.getVerifyToken())) {
                log.info("Webhook verification successful");
                return ResponseEntity.ok(challenge);
            }
        }
        
        log.warn("Webhook verification failed: invalid verify token");
        return ResponseEntity.status(403).build();
    }
    
    @PostMapping("/api/whatsapp/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody String payload) {
        log.info("Received webhook event from WhatsApp");
        
        try {
            JsonNode root = objectMapper.readTree(payload);
            
            for (WhatsAppChannel channel : channelRegistry.values()) {
                channel.handleWebhookEvent(root);
            }
            
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error processing webhook event", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
