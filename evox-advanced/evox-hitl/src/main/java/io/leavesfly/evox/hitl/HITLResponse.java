package io.leavesfly.evox.hitl;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * HITL response from human containing decision and feedback.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HITLResponse {
    
    /**
     * Request ID this response is for
     */
    private String requestId;
    
    /**
     * Human decision
     */
    private HITLDecision decision;
    
    /**
     * Modified content (if decision is MODIFY)
     */
    private Object modifiedContent;
    
    /**
     * Human feedback/comment
     */
    private String feedback;
}
