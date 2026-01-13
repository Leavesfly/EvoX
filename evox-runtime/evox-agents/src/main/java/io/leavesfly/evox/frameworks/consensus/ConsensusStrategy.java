package io.leavesfly.evox.frameworks.consensus;

import java.util.List;

/**
 * 共识策略接口
 * 定义了如何评估一组提议并判断是否达成共识
 *
 * @param <T> 提议的类型
 * @author EvoX Team
 */
public interface ConsensusStrategy<T> {

    /**
     * 评估一组提议
     *
     * @param proposals 所有智能体的提议
     * @param agents 智能体列表
     * @return 共识评估结果
     */
    ConsensusEvaluation<T> evaluate(List<T> proposals, List<ConsensusFramework.ConsensusAgent<T>> agents);

    /**
     * 在未达成共识时的回退策略
     *
     * @param history 历史记录
     * @param agents 智能体列表
     * @return 最终评估结果
     */
    ConsensusEvaluation<T> fallback(List<ConsensusRecord<T>> history, List<ConsensusFramework.ConsensusAgent<T>> agents);

    /**
     * 获取策略名称
     */
    String getStrategyName();
}
