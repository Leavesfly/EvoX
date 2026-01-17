package io.leavesfly.evox.evaluation;

import io.leavesfly.evox.core.module.BaseModule;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 评估器基类
 * 支持任务特定评估和LLM评估
 *
 * @author EvoX Team
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public abstract class Evaluator extends BaseModule {

    /**
     * 无参构造函数
     */
    public Evaluator() {
        super();
    }

    /**
     * 评估单个样本
     *
     * @param prediction 预测结果
     * @param label 真实标签
     * @return 评估结果
     */
    public abstract EvaluationResult evaluate(Object prediction, Object label);

    /**
     * 异步评估单个样本
     *
     * @param prediction 预测结果
     * @param label 真实标签
     * @return 评估结果(Mono)
     */
    public Mono<EvaluationResult> evaluateAsync(Object prediction, Object label) {
        return Mono.fromCallable(() -> evaluate(prediction, label));
    }

    /**
     * 批量评估
     *
     * @param predictions 预测结果列表
     * @param labels 真实标签列表
     * @return 聚合的评估结果
     */
    public abstract EvaluationResult evaluateBatch(Object[] predictions, Object[] labels);

    /**
     * 评估结果类
     */
    @Data
    @SuperBuilder
    public static class EvaluationResult {
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
         * @return 指标值,如果不存在返回0.0
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
}
