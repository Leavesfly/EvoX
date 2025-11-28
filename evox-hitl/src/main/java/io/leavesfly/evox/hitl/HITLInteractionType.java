package io.leavesfly.evox.hitl;

/**
 * HITL interaction types defining how humans interact with the system.
 */
public enum HITLInteractionType {
    /**
     * Simple approve/reject decision
     */
    APPROVE_REJECT,
    
    /**
     * Collect user input
     */
    COLLECT_USER_INPUT,
    
    /**
     * Review and edit state
     */
    REVIEW_EDIT_STATE,
    
    /**
     * Review tool calls
     */
    REVIEW_TOOL_CALLS,
    
    /**
     * Multi-turn conversation
     */
    MULTI_TURN_CONVERSATION
}
