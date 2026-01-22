package io.leavesfly.evox.frameworks.team;

import java.util.List;

/**
 * 团队成员接口
 *
 * @param <T> 任务结果类型
 * @author EvoX Team
 */
public interface TeamMember<T> {

    /**
     * 获取成员ID
     */
    String getMemberId();

    /**
     * 获取成员名称
     */
    String getMemberName();

    /**
     * 获取角色
     */
    TeamRole getRole();

    /**
     * 执行任务
     *
     * @param task 任务描述
     * @param previousResult 前一个成员的结果(如果有)
     * @param executionHistory 执行历史
     * @return 执行结果
     */
    T execute(String task, T previousResult, List<TaskExecution<T>> executionHistory);

    /**
     * 获取成员的能力/技能标签
     */
    default List<String> getSkills() {
        return List.of();
    }

    /**
     * 获取成员的工作负载
     */
    default double getWorkload() {
        return 0.0;
    }

    /**
     * 对议题进行投票
     *
     * @param topic 议题
     * @param options 选项列表
     * @return 选择的选项
     */
    default String vote(String topic, List<String> options) {
        // 默认选择第一个选项
        return options.isEmpty() ? null : options.get(0);
    }

    /**
     * 接收消息
     *
     * @param message 消息内容
     * @param fromMemberId 发送者ID
     */
    default void receiveMessage(String message, String fromMemberId) {
        // 默认不处理
    }

    /**
     * 检查成员是否可用
     */
    default boolean isAvailable() {
        return true;
    }
}
