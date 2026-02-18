package io.leavesfly.evox.evaluation.metrics;

import lombok.extern.slf4j.Slf4j;

/**
 * 精确匹配指标
 * <p>
 * 评估预测结果与真实标签是否完全一致。
 * 通过比较两个字符串的规范化形式来判断是否匹配。
 * </p>
 * <p>
 * 计算逻辑：
 * <ul>
 *   <li>将prediction和label都转为字符串</li>
 *   <li>去除首尾空格并转为小写</li>
 *   <li>比较两个字符串是否相等</li>
 *   <li>如果相等返回1.0，否则返回0.0</li>
 * </ul>
 * </p>
 *
 * @author EvoX Team
 * @version 1.0
 */
@Slf4j
public class ExactMatchMetric implements EvaluationMetric {

    @Override
    public double compute(Object prediction, Object label) {
        if (prediction == null || label == null) {
            return 0.0;
        }

        String predictionStr = prediction.toString().trim().toLowerCase();
        String labelStr = label.toString().trim().toLowerCase();

        if (predictionStr.equals(labelStr)) {
            return 1.0;
        }

        return 0.0;
    }

    @Override
    public String getName() {
        return "exact_match";
    }

    @Override
    public String getDescription() {
        return "Exact match metric, checks if prediction exactly matches the label after normalization";
    }
}
