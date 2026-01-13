package io.leavesfly.evox.frameworks.team;

/**
 * 协作模式枚举
 *
 * @author EvoX Team
 */
public enum CollaborationMode {
    /**
     * 并行模式:所有成员同时工作
     */
    PARALLEL,
    
    /**
     * 顺序模式:成员依次工作,可传递结果
     */
    SEQUENTIAL,
    
    /**
     * 分层模式:按角色层级工作
     */
    HIERARCHICAL,
    
    /**
     * 协同模式:成员相互协商讨论
     */
    COLLABORATIVE,
    
    /**
     * 竞争模式:选择最佳方案
     */
    COMPETITIVE
}
