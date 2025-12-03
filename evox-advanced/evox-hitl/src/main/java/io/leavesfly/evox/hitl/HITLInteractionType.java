package io.leavesfly.evox.hitl;

/**
 * HITL交互类型,定义人类如何与系统交互
 */
public enum HITLInteractionType {
    /**
     * 简单的批准/拒绝决策
     */
    APPROVE_REJECT,
    
    /**
     * 收集用户输入
     */
    COLLECT_USER_INPUT,
    
    /**
     * 审查并编辑状态
     */
    REVIEW_EDIT_STATE,
    
    /**
     * 审查工具调用
     */
    REVIEW_TOOL_CALLS,
    
    /**
     * 多轮对话
     */
    MULTI_TURN_CONVERSATION
}
