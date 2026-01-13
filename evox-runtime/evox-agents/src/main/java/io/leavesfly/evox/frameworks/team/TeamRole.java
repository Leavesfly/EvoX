package io.leavesfly.evox.frameworks.team;

/**
 * 团队角色枚举
 *
 * @author EvoX Team
 */
public enum TeamRole {
    /**
     * 领导者
     */
    LEADER(1),
    
    /**
     * 管理者
     */
    MANAGER(2),
    
    /**
     * 专家
     */
    EXPERT(3),
    
    /**
     * 执行者
     */
    EXECUTOR(4),
    
    /**
     * 协调者
     */
    COORDINATOR(3),
    
    /**
     * 审核者
     */
    REVIEWER(3),
    
    /**
     * 普通成员
     */
    MEMBER(5);

    private final int priority;

    TeamRole(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }
}
