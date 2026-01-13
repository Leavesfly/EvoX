package io.leavesfly.evox.frameworks.team;

import lombok.Data;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 角色管理器
 *
 * @author EvoX Team
 */
@Data
public class RoleManager {

    /**
     * 成员ID到角色的映射
     */
    private Map<String, TeamRole> roleAssignments;

    public RoleManager() {
        this.roleAssignments = new ConcurrentHashMap<>();
    }

    /**
     * 分配角色
     */
    public void assignRole(String memberId, TeamRole role) {
        roleAssignments.put(memberId, role);
    }

    /**
     * 获取角色
     */
    public TeamRole getRole(String memberId) {
        return roleAssignments.get(memberId);
    }

    /**
     * 移除角色
     */
    public void removeRole(String memberId) {
        roleAssignments.remove(memberId);
    }

    /**
     * 获取角色优先级
     */
    public int getRolePriority(TeamRole role) {
        return role != null ? role.getPriority() : Integer.MAX_VALUE;
    }

    /**
     * 检查是否有特定角色
     */
    public boolean hasRole(TeamRole role) {
        return roleAssignments.containsValue(role);
    }
}
