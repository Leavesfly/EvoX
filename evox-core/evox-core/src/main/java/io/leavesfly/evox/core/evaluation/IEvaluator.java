package io.leavesfly.evox.core.evaluation;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 评估器核心接口
 * 定义评估的基本能力，具体实现在 evox-evaluation 模块中。
 * 优化器模块（evox-optimizers）应依赖此接口而非具体的评估器实现。
 *
 * <p>支持三个层次的评估：
 * <ul>
 *   <li>样本级别：evaluate / evaluateAsync</li>
 *   <li>批量级别：evaluateBatch</li>
 *   <li>工作流级别：evaluateWorkflow — 对应论文公式 P = T(W, D)</li>
 * </ul>
 *
 * @author EvoX Team
 */
public interface IEvaluator {

    /**
     * 评估单个样本
     *
     * @param prediction 预测结果
     * @param label      真实标签
     * @return 评估结果
     */
    EvaluationResult evaluate(Object prediction, Object label);

    /**
     * 异步评估单个样本
     *
     * @param prediction 预测结果
     * @param label      真实标签
     * @return 评估结果(Mono)
     */
    default Mono<EvaluationResult> evaluateAsync(Object prediction, Object label) {
        return Mono.fromCallable(() -> evaluate(prediction, label));
    }

    /**
     * 批量评估
     *
     * @param predictions 预测结果数组
     * @param labels      真实标签数组
     * @return 聚合的评估结果
     */
    EvaluationResult evaluateBatch(Object[] predictions, Object[] labels);

    /**
     * 工作流级别评估：P = T(W, D)
     * 将工作流执行函数应用于数据集中的每个样本，收集预测结果后进行聚合评估。
     *
     * @param workflowExecutor 工作流执行函数，接收输入 Map 返回预测结果字符串
     * @param dataset          数据集，每个元素为 (input, label) 的键值对列表
     * @return 聚合的评估结果
     */
    default EvaluationResult evaluateWorkflow(
            Function<Map<String, Object>, String> workflowExecutor,
            List<Map.Entry<Map<String, Object>, Object>> dataset) {

        Object[] predictions = new Object[dataset.size()];
        Object[] labels = new Object[dataset.size()];

        for (int i = 0; i < dataset.size(); i++) {
            Map.Entry<Map<String, Object>, Object> entry = dataset.get(i);
            predictions[i] = workflowExecutor.apply(entry.getKey());
            labels[i] = entry.getValue();
        }

        return evaluateBatch(predictions, labels);
    }

    /**
     * 异步工作流级别评估
     *
     * @param workflowExecutor 工作流执行函数
     * @param dataset          数据集
     * @return 聚合的评估结果(Mono)
     */
    default Mono<EvaluationResult> evaluateWorkflowAsync(
            Function<Map<String, Object>, String> workflowExecutor,
            List<Map.Entry<Map<String, Object>, Object>> dataset) {
        return Mono.fromCallable(() -> evaluateWorkflow(workflowExecutor, dataset));
    }
}
