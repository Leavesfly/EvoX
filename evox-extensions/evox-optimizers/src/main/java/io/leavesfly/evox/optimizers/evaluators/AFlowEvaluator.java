package io.leavesfly.evox.optimizers.evaluators;

import io.leavesfly.evox.core.evaluation.EvaluationResult;
import io.leavesfly.evox.core.evaluation.IEvaluator;
import io.leavesfly.evox.optimizers.evaluators.metrics.EvaluationMetric;
import io.leavesfly.evox.models.spi.LLMProvider;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AFlow 评估器
 * 专门用于 AFlow 工作流优化的评估器。
 * 实现核心层的 {@link IEvaluator} 接口，不再依赖 evox-evaluation 模块。
 * 支持异步批量评估和成本跟踪。
 *
 * @author EvoX Team
 */
@Slf4j
public class AFlowEvaluator implements IEvaluator {

    /**
     * 使用的LLM模型
     */
    private final LLMProvider llm;

    /**
     * 评估指标列表
     */
    private final List<EvaluationMetric> metrics;

    /**
     * 最大并发任务数
     */
    private final int maxConcurrentTasks;

    /**
     * 构造函数
     *
     * @param llm LLM模型
     */
    public AFlowEvaluator(LLMProvider llm) {
        this(llm, new ArrayList<>(), 20);
    }

    /**
     * 构造函数
     *
     * @param llm LLM模型
     * @param metrics 评估指标
     * @param maxConcurrentTasks 最大并发任务数
     */
    public AFlowEvaluator(LLMProvider llm, List<EvaluationMetric> metrics, int maxConcurrentTasks) {
        this.llm = llm;
        this.metrics = metrics;
        this.maxConcurrentTasks = maxConcurrentTasks;
    }

    @Override
    public EvaluationResult evaluate(Object prediction, Object label) {
        Map<String, Double> resultMetrics = new HashMap<>();

        if (metrics.isEmpty()) {
            double accuracy = prediction.toString().trim().equalsIgnoreCase(label.toString().trim()) ? 1.0 : 0.0;
            resultMetrics.put("accuracy", accuracy);
        } else {
            for (EvaluationMetric metric : metrics) {
                try {
                    double value = metric.compute(prediction, label);
                    resultMetrics.put(metric.getName(), value);
                } catch (Exception e) {
                    log.error("Error computing metric: {}", metric.getName(), e);
                    resultMetrics.put(metric.getName(), 0.0);
                }
            }
        }

        return EvaluationResult.success(resultMetrics);
    }

    @Override
    public EvaluationResult evaluateBatch(Object[] predictions, Object[] labels) {
        if (predictions.length != labels.length) {
            return EvaluationResult.failure("Predictions and labels must have the same length");
        }

        Map<String, Double> aggregatedMetrics = new HashMap<>();
        int totalSamples = predictions.length;

        if (totalSamples == 0) {
            return EvaluationResult.failure("Empty predictions or labels");
        }

        // 对每个样本进行评估并聚合结果
        for (int i = 0; i < totalSamples; i++) {
            EvaluationResult result = evaluate(predictions[i], labels[i]);
            if (result.isSuccess()) {
                for (Map.Entry<String, Double> entry : result.getMetrics().entrySet()) {
                    String metricName = entry.getKey();
                    double value = entry.getValue();
                    aggregatedMetrics.merge(metricName, value, Double::sum);
                }
            }
        }

        // 计算平均值
        Map<String, Double> avgMetrics = new HashMap<>();
        for (Map.Entry<String, Double> entry : aggregatedMetrics.entrySet()) {
            avgMetrics.put(entry.getKey(), entry.getValue() / totalSamples);
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("total_samples", totalSamples);
        metadata.put("metrics_computed", avgMetrics.keySet());

        return EvaluationResult.builder()
                .success(true)
                .metrics(avgMetrics)
                .metadata(metadata)
                .build();
    }

    /**
     * 异步批量评估（带并发控制）
     *
     * @param predictions 预测结果数组
     * @param labels 真实标签数组
     * @return 评估结果的Mono
     */
    public Mono<EvaluationResult> evaluateBatchAsync(Object[] predictions, Object[] labels) {
        if (predictions.length != labels.length) {
            return Mono.just(EvaluationResult.failure("Predictions and labels must have the same length"));
        }

        if (predictions.length == 0) {
            return Mono.just(EvaluationResult.failure("Empty predictions or labels"));
        }

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // 创建评估任务流
        return Flux.range(0, predictions.length)
                .flatMap(i -> evaluateAsync(predictions[i], labels[i])
                        .doOnNext(result -> {
                            if (result.isSuccess()) {
                                successCount.incrementAndGet();
                            } else {
                                failureCount.incrementAndGet();
                            }
                        })
                        .onErrorResume(e -> {
                            log.error("Error evaluating sample {}", i, e);
                            failureCount.incrementAndGet();
                            return Mono.just(EvaluationResult.failure(e.getMessage()));
                        }),
                        maxConcurrentTasks) // 限制并发数
                .collectList()
                .map(results -> {
                    // 聚合结果
                    Map<String, Double> aggregatedMetrics = new HashMap<>();
                    int validResults = 0;

                    for (EvaluationResult result : results) {
                        if (result.isSuccess()) {
                            validResults++;
                            for (Map.Entry<String, Double> entry : result.getMetrics().entrySet()) {
                                aggregatedMetrics.merge(entry.getKey(), entry.getValue(), Double::sum);
                            }
                        }
                    }

                    if (validResults == 0) {
                        return EvaluationResult.failure("All evaluations failed");
                    }

                    // 计算平均值
                    Map<String, Double> avgMetrics = new HashMap<>();
                    for (Map.Entry<String, Double> entry : aggregatedMetrics.entrySet()) {
                        avgMetrics.put(entry.getKey(), entry.getValue() / validResults);
                    }

                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("total_samples", predictions.length);
                    metadata.put("success_count", successCount.get());
                    metadata.put("failure_count", failureCount.get());
                    metadata.put("metrics_computed", avgMetrics.keySet());

                    return EvaluationResult.builder()
                            .success(true)
                            .metrics(avgMetrics)
                            .metadata(metadata)
                            .build();
                });
    }

    /**
     * 获取LLM模型
     *
     * @return LLM模型
     */
    public LLMProvider getLlm() {
        return llm;
    }

    /**
     * 添加评估指标
     *
     * @param metric 评估指标
     */
    public void addMetric(EvaluationMetric metric) {
        this.metrics.add(metric);
    }

    /**
     * 获取所有评估指标
     *
     * @return 评估指标列表
     */
    public List<EvaluationMetric> getMetrics() {
        return Collections.unmodifiableList(metrics);
    }
}
