package io.leavesfly.evox.gateway.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GatewayConfig {
    
    @Builder.Default
    private List<String> apiKeys = List.of();
    
    @Builder.Default
    private int rateLimitPerMinute = 60;
    
    @Builder.Default
    private int sessionTimeoutHours = 24;
    
    @Builder.Default
    private int maxAuditEvents = 10000;
    
    @Builder.Default
    private boolean allowAnonymousAccess = false;
}
