package io.leavesfly.evox.evaluation.pipeline;

import io.leavesfly.evox.core.evaluation.EvaluationResult;
import io.leavesfly.evox.evaluation.Evaluator;
import io.leavesfly.evox.evaluation.dataset.EvaluationDataset;
import io.leavesfly.evox.evaluation.workflow.WorkflowEvaluator;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.Function;

/**
 * 评估流水线
 * <p>
 * 编排完整的评估流程，对应论文中的 T(·) 函数：P = T(W, D)。
 * 将工作流执行映射到聚合性能指标，支持 WorkFlowGraph 和 ActionGraph 两种抽象级别的评估。
 * </p>
 * <p>
 * 支持的评估模式：
 * <ul>
 *   <li><b>WorkFlowGraph 级别</b>：评估整个工作流的端到端性能</li>
 *   <li><b>ActionGraph 级别</b>：评估工作流中每个 Action 节点的独立性能</li>
 *   <li><b>组合评估</b>：同时使用多个评估器（task-specific + LLM-based）进行综合评估</li>
 * </ul>
 * </p>
 *
 * @author EvoX Team
 */
@Slf4j
public class EvaluationPipeline {

    /**
     * 流水线名称
     */
    private final String pipelineName;

    /**
     * 已注册的评估器列表
     */
    private final List<Evaluator> evaluators;

    /**
     * 工作流评估器（用于工作流级别评估）
     */
    private WorkflowEvaluator workflowEvaluator;

    /**
     * 构造函数
     *
     * @param pipelineName 流水线名称
     */
    public EvaluationPipeline(String pipelineName) {
        this.pipelineName = pipelineName;
        this.evaluators = new ArrayList<>();
    }

    /**
     * 添加评估器到流水线
     *
     * @param evaluator 评估器
     * @return 当前流水线实例（支持链式调用）
     */
    public EvaluationPipeline addEvaluator(Evaluator evaluator) {
        if (evaluator == null) {
            throw new IllegalArgumentException("Evaluator cannot be null");
        }
        this.evaluators.add(evaluator);
        log.debug("Added evaluator '{}' to pipeline '{}'",
                evaluator.getClass().getSimpleName(), pipelineName);
        return this;
    }

    /**
     * 设置工作流评估器
     *
     * @param workflowEvaluator 工作流评估器
     * @return 当前流水线实例
     */
    public EvaluationPipeline withWorkflowEvaluator(WorkflowEvaluator workflowEvaluator) {
        this.workflowEvaluator = workflowEvaluator;
        return this;
    }

    /**
     * WorkFlowGraph 级别评估：P = T(W, D)
     * <p>
     * 将工作流执行函数应用于整个数据集，使用所有注册的评估器进行综合评估。
     * </p>
     *
     * @param workflowExecutor 工作流执行函数
     * @param dataset          评估数据集
     * @return 综合评估结果
     */
    public EvaluationResult evaluateWorkflowGraph(
            Function<Map<String, Object>, String> workflowExecutor,
            EvaluationDataset dataset) {

        log.info("Starting WorkFlowGraph evaluation on pipeline '{}', dataset '{}' ({} samples)",
                pipelineName, dataset.getName(), dataset.size());

        long startTime = System.currentTimeMillis();

        Object[] predictions = executeWorkflow(workflowExecutor, dataset);
        Object[] labels = extractLabels(dataset);

        Map<String, Double> combinedMetrics = new LinkedHashMap<>();
        Map<String, Object> combinedMetadata = new LinkedHashMap<>();
        combinedMetadata.put("pipeline_name", pipelineName);
        combinedMetadata.put("evaluation_level", "WorkFlowGraph");
        combinedMetadata.put("dataset_name", dataset.getName());
        combinedMetadata.put("dataset_size", dataset.size());

        for (Evaluator evaluator : evaluators) {
            String evaluatorName = evaluator.getClass().getSimpleName();
            try {
                EvaluationResult result = evaluator.evaluateBatch(predictions, labels);
                if (result.isSuccess() && result.getMetrics() != null) {
                    result.getMetrics().forEach((key, value) ->
                            combinedMetrics.put(evaluatorName + "." + key, value));
                }
            } catch (Exception e) {
                log.warn("Evaluator '{}' failed: {}", evaluatorName, e.getMessage());
                combinedMetrics.put(evaluatorName + ".error", 1.0);
            }
        }

        if (workflowEvaluator != null) {
            EvaluationResult workflowResult = workflowEvaluator.evaluateBatch(predictions, labels);
            if (workflowResult.isSuccess() && workflowResult.getMetrics() != null) {
                workflowResult.getMetrics().forEach((key, value) ->
                        combinedMetrics.put("workflow." + key, value));
            }
        }

        long elapsedMs = System.currentTimeMillis() - startTime;
        combinedMetadata.put("evaluation_time_ms", elapsedMs);
        combinedMetadata.put("evaluator_count", evaluators.size());

        log.info("WorkFlowGraph evaluation completed on '{}': {} metrics in {}ms",
                pipelineName, combinedMetrics.size(), elapsedMs);

        return EvaluationResult.builder()
                .success(true)
                .metrics(combinedMetrics)
                .metadata(combinedMetadata)
                .build();
    }

    /**
     * ActionGraph 级别评估
     * <p>
     * 对工作流中的每个 Action 节点独立评估，每个 Action 有自己的执行函数和数据集。
     * </p>
     *
     * @param actionExecutors 每个 Action 名称到其执行函数的映射
     * @param actionDatasets  每个 Action 名称到其评估数据集的映射
     * @return 每个 Action 的评估结果映射
     */
    public Map<String, EvaluationResult> evaluateActionGraph(
            Map<String, Function<Map<String, Object>, String>> actionExecutors,
            Map<String, EvaluationDataset> actionDatasets) {

        log.info("Starting ActionGraph evaluation on pipeline '{}' with {} actions",
                pipelineName, actionExecutors.size());

        long startTime = System.currentTimeMillis();
        Map<String, EvaluationResult> actionResults = new LinkedHashMap<>();

        for (Map.Entry<String, Function<Map<String, Object>, String>> entry : actionExecutors.entrySet()) {
            String actionName = entry.getKey();
            Function<Map<String, Object>, String> executor = entry.getValue();
            EvaluationDataset dataset = actionDatasets.get(actionName);

            if (dataset == null) {
                log.warn("No dataset found for action '{}', skipping", actionName);
                actionResults.put(actionName,
                        EvaluationResult.failure("No dataset configured for action: " + actionName));
                continue;
            }

            log.info("Evaluating action '{}' on dataset '{}' ({} samples)",
                    actionName, dataset.getName(), dataset.size());

            Object[] predictions = executeWorkflow(executor, dataset);
            Object[] labels = extractLabels(dataset);

            Map<String, Double> actionMetrics = new LinkedHashMap<>();
            for (Evaluator evaluator : evaluators) {
                String evaluatorName = evaluator.getClass().getSimpleName();
                try {
                    EvaluationResult result = evaluator.evaluateBatch(predictions, labels);
                    if (result.isSuccess() && result.getMetrics() != null) {
                        result.getMetrics().forEach((key, value) ->
                                actionMetrics.put(evaluatorName + "." + key, value));
                    }
                } catch (Exception e) {
                    log.warn("Evaluator '{}' failed for action '{}': {}",
                            evaluatorName, actionName, e.getMessage());
                }
            }

            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("action_name", actionName);
            metadata.put("dataset_name", dataset.getName());
            metadata.put("dataset_size", dataset.size());

            actionResults.put(actionName, EvaluationResult.builder()
                    .success(true)
                    .metrics(actionMetrics)
                    .metadata(metadata)
                    .build());
        }

        long elapsedMs = System.currentTimeMillis() - startTime;
        log.info("ActionGraph evaluation completed on '{}': {} actions in {}ms",
                pipelineName, actionResults.size(), elapsedMs);

        return actionResults;
    }

    /**
     * 综合评估：同时进行 WorkFlowGraph 和 ActionGraph 级别的评估
     *
     * @param workflowExecutor 工作流执行函数
     * @param workflowDataset  工作流级别数据集
     * @param actionExecutors  Action 执行函数映射
     * @param actionDatasets   Action 数据集映射
     * @return 综合评估结果
     */
    public CombinedEvaluationResult evaluateCombined(
            Function<Map<String, Object>, String> workflowExecutor,
            EvaluationDataset workflowDataset,
            Map<String, Function<Map<String, Object>, String>> actionExecutors,
            Map<String, EvaluationDataset> actionDatasets) {

        log.info("Starting combined evaluation on pipeline '{}'", pipelineName);

        EvaluationResult workflowResult = evaluateWorkflowGraph(workflowExecutor, workflowDataset);
        Map<String, EvaluationResult> actionResults = evaluateActionGraph(actionExecutors, actionDatasets);

        return new CombinedEvaluationResult(workflowResult, actionResults);
    }

    /**
     * 执行工作流并收集预测结果
     *
     * @param executor 执行函数
     * @param dataset  数据集
     * @return 预测结果数组
     */
    private Object[] executeWorkflow(
            Function<Map<String, Object>, String> executor,
            EvaluationDataset dataset) {

        Object[] predictions = new Object[dataset.size()];
        for (int i = 0; i < dataset.size(); i++) {
            try {
                predictions[i] = executor.apply(dataset.getInput(i));
            } catch (Exception e) {
                log.warn("Workflow execution failed for sample {}: {}", i, e.getMessage());
                predictions[i] = "";
            }
        }
        return predictions;
    }

    /**
     * 从数据集中提取所有标签
     *
     * @param dataset 数据集
     * @return 标签数组
     */
    private Object[] extractLabels(EvaluationDataset dataset) {
        Object[] labels = new Object[dataset.size()];
        for (int i = 0; i < dataset.size(); i++) {
            labels[i] = dataset.getLabel(i);
        }
        return labels;
    }

    /**
     * 获取流水线名称
     *
     * @return 流水线名称
     */
    public String getPipelineName() {
        return pipelineName;
    }

    /**
     * 获取已注册的评估器列表（不可变视图）
     *
     * @return 评估器列表
     */
    public List<Evaluator> getEvaluators() {
        return Collections.unmodifiableList(evaluators);
    }

    /**
     * 综合评估结果，包含 WorkFlowGraph 和 ActionGraph 两个层级的结果
     */
    public static class CombinedEvaluationResult {

        private final EvaluationResult workflowGraphResult;
        private final Map<String, EvaluationResult> actionGraphResults;

        public CombinedEvaluationResult(
                EvaluationResult workflowGraphResult,
                Map<String, EvaluationResult> actionGraphResults) {
            this.workflowGraphResult = workflowGraphResult;
            this.actionGraphResults = actionGraphResults;
        }

        /**
         * 获取 WorkFlowGraph 级别的评估结果
         *
         * @return 工作流级别评估结果
         */
        public EvaluationResult getWorkflowGraphResult() {
            return workflowGraphResult;
        }

        /**
         * 获取 ActionGraph 级别的评估结果
         *
         * @return 每个 Action 的评估结果映射
         */
        public Map<String, EvaluationResult> getActionGraphResults() {
            return Collections.unmodifiableMap(actionGraphResults);
        }

        /**
         * 获取所有评估指标的汇总
         *
         * @return 汇总的指标映射
         */
        public Map<String, Double> getAllMetrics() {
            Map<String, Double> allMetrics = new LinkedHashMap<>();

            if (workflowGraphResult != null && workflowGraphResult.getMetrics() != null) {
                workflowGraphResult.getMetrics().forEach((key, value) ->
                        allMetrics.put("workflow_graph." + key, value));
            }

            if (actionGraphResults != null) {
                actionGraphResults.forEach((actionName, result) -> {
                    if (result != null && result.getMetrics() != null) {
                        result.getMetrics().forEach((key, value) ->
                                allMetrics.put("action_graph." + actionName + "." + key, value));
                    }
                });
            }

            return allMetrics;
        }
    }
}
