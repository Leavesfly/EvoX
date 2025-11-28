package io.leavesfly.evox.hitl;

/**
 * HITL decision types for human feedback.
 */
public enum HITLDecision {
    /**
     * Approve the action/result
     */
    APPROVE,
    
    /**
     * Reject the action/result
     */
    REJECT,
    
    /**
     * Modify the action/result
     */
    MODIFY,
    
    /**
     * Continue with current state
     */
    CONTINUE
}
