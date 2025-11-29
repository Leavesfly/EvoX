package io.leavesfly.evox.frameworks.hierarchical;

import java.util.List;

/**
 * 决策层级接口
 *
 * @param <T> 决策结果类型
 * @author EvoX Team
 */
public interface DecisionLayer<T> {

    /**
     * 获取层级ID
     */
    String getLayerId();

    /**
     * 获取层级名称
     */
    String getLayerName();

    /**
     * 获取层级级别(0为最高层)
     */
    int getLevel();

    /**
     * 做出决策
     *
     * @param task 任务描述
     * @param parentDecision 父层决策(如果有)
     * @return 层级决策
     */
    LayerDecision<T> decide(String task, LayerDecision<T> parentDecision);

    /**
     * 判断是否可以处理该任务
     *
     * @param task 任务描述
     * @return 是否可以处理
     */
    default boolean canHandle(String task) {
        return true;
    }

    /**
     * 获取该层级的智能体列表
     */
    default List<? extends Object> getAgents() {
        return List.of();
    }
}
