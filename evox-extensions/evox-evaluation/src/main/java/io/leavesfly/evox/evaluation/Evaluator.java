package io.leavesfly.evox.evaluation;

import io.leavesfly.evox.core.evaluation.IEvaluator;
import io.leavesfly.evox.core.evaluation.EvaluationResult;
import io.leavesfly.evox.core.module.BaseModule;
import io.leavesfly.evox.evaluation.metrics.EvaluationMetric;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.*;

/**
 * 评估器基类
 * <p>
 * 实现核心层的 {@link IEvaluator} 接口，并继承 {@link BaseModule} 获得模块管理能力。
 * 支持任务特定评估和 LLM 评估两种互补的评估方式。
 * </p>
 * <p>
 * 提供指标注册机制和通用的批量评估聚合逻辑，子类只需关注单样本评估的实现。
 * </p>
 *
 * @author EvoX Team
 */
@Slf4j
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public abstract class Evaluator extends BaseModule implements IEvaluator {

    /**
     * 已注册的评估指标列表
     */
    private transient List<EvaluationMetric> registeredMetrics;

    /**
     * 无参构造函数
     */
    public Evaluator() {
        super();
        this.registeredMetrics = new ArrayList<>();
    }

    /**
     * 注册评估指标
     *
     * @param metric 要注册的评估指标
     */
    public void registerMetric(EvaluationMetric metric) {
        if (metric == null) {
            throw new IllegalArgumentException("Metric cannot be null");
        }
        this.registeredMetrics.add(metric);
        log.debug("Registered metric: {}", metric.getName());
    }

    /**
     * 注册多个评估指标
     *
     * @param metrics 要注册的评估指标列表
     */
    public void registerMetrics(List<EvaluationMetric> metrics) {
        if (metrics != null) {
            metrics.forEach(this::registerMetric);
        }
    }

    /**
     * 获取已注册的指标列表（不可变视图）
     *
     * @return 已注册的指标列表
     */
    public List<EvaluationMetric> getRegisteredMetrics() {
        return Collections.unmodifiableList(registeredMetrics);
    }

    /**
     * 使用已注册的指标计算额外的评估分数
     *
     * @param prediction 预测结果
     * @param label      真实标签
     * @return 指标名称到分数的映射
     */
    protected Map<String, Double> computeRegisteredMetrics(Object prediction, Object label) {
        Map<String, Double> metricScores = new LinkedHashMap<>();
        for (EvaluationMetric metric : registeredMetrics) {
            try {
                double score = metric.compute(prediction, label);
                metricScores.put(metric.getName(), score);
            } catch (Exception e) {
                log.warn("Failed to compute metric '{}': {}", metric.getName(), e.getMessage());
                metricScores.put(metric.getName(), 0.0);
            }
        }
        return metricScores;
    }

    @Override
    public abstract EvaluationResult evaluate(Object prediction, Object label);

    @Override
    public Mono<EvaluationResult> evaluateAsync(Object prediction, Object label) {
        return Mono.fromCallable(() -> evaluate(prediction, label));
    }

    @Override
    public EvaluationResult evaluateBatch(Object[] predictions, Object[] labels) {
        if (predictions == null || labels == null) {
            return EvaluationResult.failure("Predictions and labels cannot be null");
        }
        if (predictions.length != labels.length) {
            return EvaluationResult.failure("Predictions and labels must have the same length");
        }
        if (predictions.length == 0) {
            return EvaluationResult.failure("Empty predictions or labels");
        }

        Map<String, Double> aggregatedMetrics = new LinkedHashMap<>();
        int successCount = 0;

        for (int i = 0; i < predictions.length; i++) {
            EvaluationResult result = evaluate(predictions[i], labels[i]);
            if (result.isSuccess() && result.getMetrics() != null) {
                successCount++;
                result.getMetrics().forEach((key, value) ->
                        aggregatedMetrics.merge(key, value, Double::sum));
            }
        }

        if (successCount == 0) {
            return EvaluationResult.failure("All evaluations failed");
        }

        int finalSuccessCount = successCount;
        aggregatedMetrics.replaceAll((key, value) -> value / finalSuccessCount);

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("total_samples", predictions.length);
        metadata.put("success_count", finalSuccessCount);
        metadata.put("failure_count", predictions.length - finalSuccessCount);

        return EvaluationResult.builder()
                .success(true)
                .metrics(aggregatedMetrics)
                .metadata(metadata)
                .build();
    }
}
