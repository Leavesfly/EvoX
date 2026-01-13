package io.leavesfly.evox.hitl;

/**
 * HITL决策类型,用于人类反馈
 */
public enum HITLDecision {
    /**
     * 批准该动作/结果
     */
    APPROVE,
    
    /**
     * 拒绝该动作/结果
     */
    REJECT,
    
    /**
     * 修改该动作/结果
     */
    MODIFY,
    
    /**
     * 继续当前状态
     */
    CONTINUE
}
