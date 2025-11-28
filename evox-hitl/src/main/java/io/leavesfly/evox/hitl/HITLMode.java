package io.leavesfly.evox.hitl;

/**
 * HITL execution modes defining when human intervention occurs.
 */
public enum HITLMode {
    /**
     * Intercept before action execution
     */
    PRE_EXECUTION,
    
    /**
     * Intercept after action execution
     */
    POST_EXECUTION
}
