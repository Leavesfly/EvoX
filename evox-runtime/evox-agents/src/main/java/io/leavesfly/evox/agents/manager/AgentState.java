package io.leavesfly.evox.agents.manager;

/**
 * 智能体状态枚举
 *
 * @author EvoX Team
 */
public enum AgentState {
    /**
     * 空闲
     */
    IDLE,

    /**
     * 忙碌
     */
    BUSY,

    /**
     * 等待
     */
    WAITING,

    /**
     * 完成
     */
    COMPLETED,

    /**
     * 失败
     */
    FAILED
}
