package io.leavesfly.evox.core.evaluation;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 评估结果
 * 核心层的评估结果数据类，供优化器和评估框架共同使用。
 *
 * @author EvoX Team
 */
@Data
@Builder
public class EvaluationResult {

    /**
     * 评估指标映射
     * 例如: {"accuracy": 0.95, "f1_score": 0.92}
     */
    private Map<String, Double> metrics;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 错误信息（如果失败）
     */
    private String error;

    /**
     * 元数据
     */
    private Map<String, Object> metadata;

    /**
     * 获取指定指标的值
     *
     * @param metricName 指标名称
     * @return 指标值，如果不存在返回 0.0
     */
    public double getMetric(String metricName) {
        return metrics != null ? metrics.getOrDefault(metricName, 0.0) : 0.0;
    }

    /**
     * 创建成功的评估结果
     *
     * @param metrics 评估指标
     * @return 评估结果
     */
    public static EvaluationResult success(Map<String, Double> metrics) {
        return EvaluationResult.builder()
                .success(true)
                .metrics(metrics)
                .build();
    }

    /**
     * 创建失败的评估结果
     *
     * @param error 错误信息
     * @return 评估结果
     */
    public static EvaluationResult failure(String error) {
        return EvaluationResult.builder()
                .success(false)
                .error(error)
                .build();
    }
}
