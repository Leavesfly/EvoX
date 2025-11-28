package io.leavesfly.evox.hitl;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * HITL request sent to human for review/approval.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HITLRequest {
    
    /**
     * Unique request ID
     */
    @Builder.Default
    private String requestId = UUID.randomUUID().toString();
    
    /**
     * Interaction type
     */
    private HITLInteractionType interactionType;
    
    /**
     * Execution mode
     */
    private HITLMode mode;
    
    /**
     * Context information
     */
    private HITLContext context;
    
    /**
     * Prompt message to display to user
     */
    private String promptMessage;
}
