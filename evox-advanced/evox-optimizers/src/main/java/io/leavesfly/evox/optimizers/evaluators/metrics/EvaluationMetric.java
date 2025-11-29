package io.leavesfly.evox.optimizers.evaluators.metrics;

/**
 * 评估指标接口
 * 定义评估指标的计算方法
 *
 * @author EvoX Team
 */
public interface EvaluationMetric {

    /**
     * 计算指标值
     *
     * @param prediction 预测值
     * @param label 真实标签
     * @return 指标值
     */
    double compute(Object prediction, Object label);

    /**
     * 获取指标名称
     *
     * @return 指标名称
     */
    String getName();
}
