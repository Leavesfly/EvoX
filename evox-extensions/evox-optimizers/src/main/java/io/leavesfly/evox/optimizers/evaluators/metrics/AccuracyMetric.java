package io.leavesfly.evox.optimizers.evaluators.metrics;

import lombok.extern.slf4j.Slf4j;

/**
 * 准确率指标
 * 计算预测值与真实标签的匹配程度
 *
 * @author EvoX Team
 */
@Slf4j
public class AccuracyMetric implements EvaluationMetric {

    @Override
    public double compute(Object prediction, Object label) {
        if (prediction == null || label == null) {
            log.warn("Prediction or label is null, returning 0.0");
            return 0.0;
        }

        // 字符串比较（忽略大小写和前后空白）
        String predStr = normalizeString(prediction.toString());
        String labelStr = normalizeString(label.toString());

        return predStr.equals(labelStr) ? 1.0 : 0.0;
    }

    @Override
    public String getName() {
        return "accuracy";
    }

    /**
     * 规范化字符串（去除空白、转小写）
     */
    private String normalizeString(String str) {
        if (str == null) {
            return "";
        }
        return str.trim().toLowerCase();
    }
}
