package io.leavesfly.evox.evaluation.workflow;

import io.leavesfly.evox.core.evaluation.EvaluationResult;
import io.leavesfly.evox.evaluation.Evaluator;
import io.leavesfly.evox.evaluation.dataset.EvaluationDataset;
import io.leavesfly.evox.evaluation.metrics.EvaluationMetric;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.AbstractMap;

/**
 * 工作流评估器
 * <p>
 * 实现论文中定义的评估过程 P = T(W, D)，将工作流执行函数 W 应用于数据集 D，
 * 收集预测结果后通过注册的评估指标计算聚合性能指标 P。
 * </p>
 * <p>
 * 支持 WorkFlowGraph 和 ActionGraph 两种抽象级别的评估，
 * 通过传入不同的 workflowExecutor 函数来适配不同的执行结构。
 * </p>
 *
 * @author EvoX Team
 */
@Slf4j
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class WorkflowEvaluator extends Evaluator {

    /**
     * 最大并发评估任务数
     */
    private int maxConcurrentTasks;

    /**
     * 是否在评估失败时继续执行
     */
    private boolean continueOnFailure;

    /**
     * 无参构造函数
     */
    public WorkflowEvaluator() {
        super();
        this.maxConcurrentTasks = 10;
        this.continueOnFailure = true;
    }

    /**
     * 构造函数
     *
     * @param maxConcurrentTasks 最大并发任务数
     * @param continueOnFailure  评估失败时是否继续
     */
    public WorkflowEvaluator(int maxConcurrentTasks, boolean continueOnFailure) {
        super();
        this.maxConcurrentTasks = maxConcurrentTasks;
        this.continueOnFailure = continueOnFailure;
    }

    @Override
    public EvaluationResult evaluate(Object prediction, Object label) {
        if (prediction == null || label == null) {
            return EvaluationResult.failure("Prediction and label cannot be null");
        }

        Map<String, Double> metrics = new LinkedHashMap<>();

        Map<String, Double> registeredScores = computeRegisteredMetrics(prediction, label);
        metrics.putAll(registeredScores);

        if (metrics.isEmpty()) {
            String predStr = prediction.toString().trim().toLowerCase();
            String labelStr = label.toString().trim().toLowerCase();
            metrics.put("exact_match", predStr.equals(labelStr) ? 1.0 : 0.0);
        }

        return EvaluationResult.success(metrics);
    }

    /**
     * 执行工作流级别评估：P = T(W, D)
     * <p>
     * 将工作流执行函数应用于数据集中的每个样本，收集预测结果后通过注册的指标计算聚合性能。
     * </p>
     *
     * @param workflowExecutor 工作流执行函数，接收输入 Map 返回预测结果字符串
     * @param dataset          评估数据集
     * @return 聚合的评估结果
     */
    public EvaluationResult evaluateOnDataset(
            Function<Map<String, Object>, String> workflowExecutor,
            EvaluationDataset dataset) {

        log.info("Starting workflow evaluation on dataset '{}' with {} samples",
                dataset.getName(), dataset.size());

        long startTime = System.currentTimeMillis();

        Object[] predictions = new Object[dataset.size()];
        Object[] labels = new Object[dataset.size()];
        int executionFailures = 0;

        for (int i = 0; i < dataset.size(); i++) {
            try {
                Map<String, Object> input = dataset.getInput(i);
                predictions[i] = workflowExecutor.apply(input);
                labels[i] = dataset.getLabel(i);
            } catch (Exception e) {
                log.warn("Workflow execution failed for sample {}: {}", i, e.getMessage());
                executionFailures++;
                if (!continueOnFailure) {
                    return EvaluationResult.failure(
                            "Workflow execution failed at sample " + i + ": " + e.getMessage());
                }
                predictions[i] = "";
                labels[i] = dataset.getLabel(i);
            }
        }

        EvaluationResult batchResult = evaluateBatch(predictions, labels);

        long elapsedMs = System.currentTimeMillis() - startTime;

        Map<String, Object> metadata = new LinkedHashMap<>();
        if (batchResult.getMetadata() != null) {
            metadata.putAll(batchResult.getMetadata());
        }
        metadata.put("dataset_name", dataset.getName());
        metadata.put("dataset_size", dataset.size());
        metadata.put("execution_failures", executionFailures);
        metadata.put("evaluation_time_ms", elapsedMs);

        log.info("Workflow evaluation completed on '{}': {} samples in {}ms, {} execution failures",
                dataset.getName(), dataset.size(), elapsedMs, executionFailures);

        return EvaluationResult.builder()
                .success(batchResult.isSuccess())
                .metrics(batchResult.getMetrics())
                .metadata(metadata)
                .error(batchResult.getError())
                .build();
    }

    /**
     * 异步执行工作流级别评估（带并发控制）
     *
     * @param workflowExecutor 工作流执行函数
     * @param dataset          评估数据集
     * @return 聚合的评估结果(Mono)
     */
    public Mono<EvaluationResult> evaluateOnDatasetAsync(
            Function<Map<String, Object>, String> workflowExecutor,
            EvaluationDataset dataset) {

        log.info("Starting async workflow evaluation on dataset '{}' with {} samples",
                dataset.getName(), dataset.size());

        long startTime = System.currentTimeMillis();
        AtomicInteger executionFailures = new AtomicInteger(0);

        return Flux.range(0, dataset.size())
                .flatMap(index -> {
                    Map<String, Object> input = dataset.getInput(index);
                    Object label = dataset.getLabel(index);

                    return Mono.<Map.Entry<Object, Object>>fromCallable(() -> {
                                String prediction = workflowExecutor.apply(input);
                                return new AbstractMap.SimpleEntry<>(prediction, label);
                            })
                            .onErrorResume(e -> {
                                log.warn("Async workflow execution failed for sample {}: {}",
                                        index, e.getMessage());
                                executionFailures.incrementAndGet();
                                if (!continueOnFailure) {
                                    return Mono.error(e);
                                }
                                return Mono.just(new AbstractMap.SimpleEntry<>("", label));
                            });
                }, maxConcurrentTasks)
                .collectList()
                .map(results -> {
                    Object[] predictions = new Object[results.size()];
                    Object[] labels = new Object[results.size()];

                    for (int i = 0; i < results.size(); i++) {
                        predictions[i] = results.get(i).getKey();
                        labels[i] = results.get(i).getValue();
                    }

                    EvaluationResult batchResult = evaluateBatch(predictions, labels);
                    long elapsedMs = System.currentTimeMillis() - startTime;

                    Map<String, Object> metadata = new LinkedHashMap<>();
                    if (batchResult.getMetadata() != null) {
                        metadata.putAll(batchResult.getMetadata());
                    }
                    metadata.put("dataset_name", dataset.getName());
                    metadata.put("dataset_size", dataset.size());
                    metadata.put("execution_failures", executionFailures.get());
                    metadata.put("evaluation_time_ms", elapsedMs);
                    metadata.put("async", true);

                    log.info("Async workflow evaluation completed on '{}': {} samples in {}ms",
                            dataset.getName(), dataset.size(), elapsedMs);

                    return EvaluationResult.builder()
                            .success(batchResult.isSuccess())
                            .metrics(batchResult.getMetrics())
                            .metadata(metadata)
                            .error(batchResult.getError())
                            .build();
                });
    }

    /**
     * 对多个数据集执行工作流评估并汇总结果
     *
     * @param workflowExecutor 工作流执行函数
     * @param datasets         多个评估数据集
     * @return 每个数据集名称到评估结果的映射
     */
    public Map<String, EvaluationResult> evaluateOnMultipleDatasets(
            Function<Map<String, Object>, String> workflowExecutor,
            List<EvaluationDataset> datasets) {

        Map<String, EvaluationResult> results = new LinkedHashMap<>();
        for (EvaluationDataset dataset : datasets) {
            EvaluationResult result = evaluateOnDataset(workflowExecutor, dataset);
            results.put(dataset.getName(), result);
        }
        return results;
    }
}
