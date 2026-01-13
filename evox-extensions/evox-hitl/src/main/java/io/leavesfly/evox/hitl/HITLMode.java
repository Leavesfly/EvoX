package io.leavesfly.evox.hitl;

/**
 * HITL执行模式,定义人类干预何时发生
 */
public enum HITLMode {
    /**
     * 在动作执行前拦截
     */
    PRE_EXECUTION,
    
    /**
     * 在动作执行后拦截
     */
    POST_EXECUTION
}
