package io.leavesfly.evox.evaluation.metrics;

/**
 * 评估指标接口
 * <p>
 * 定义了评估指标的基本契约，用于计算模型预测结果与真实标签之间的匹配度。
 * 所有具体的评估指标实现都需要实现此接口。
 * </p>
 *
 * @author EvoX Team
 * @version 1.0
 */
public interface EvaluationMetric {

    /**
     * 计算评估指标得分
     *
     * @param prediction 模型预测结果，可以是任意类型的对象
     * @param label 真实标签，可以是任意类型的对象
     * @return 评估指标得分，范围通常在 [0, 1] 之间，具体取决于指标实现
     */
    double compute(Object prediction, Object label);

    /**
     * 获取指标名称
     *
     * @return 指标的唯一标识名称
     */
    String getName();

    /**
     * 获取指标描述
     *
     * @return 指标的详细描述信息，默认返回空字符串
     */
    default String getDescription() {
        return "";
    }
}
