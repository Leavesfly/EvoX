package io.leavesfly.evox.core.evaluation;

import reactor.core.publisher.Mono;

/**
 * 评估器核心接口
 * 定义评估的基本能力，具体实现在 evox-evaluation 模块中。
 * 优化器模块（evox-optimizers）应依赖此接口而非具体的评估器实现。
 *
 * @author EvoX Team
 */
public interface IEvaluator {

    /**
     * 评估单个样本
     *
     * @param prediction 预测结果
     * @param label 真实标签
     * @return 评估结果
     */
    EvaluationResult evaluate(Object prediction, Object label);

    /**
     * 异步评估单个样本
     *
     * @param prediction 预测结果
     * @param label 真实标签
     * @return 评估结果(Mono)
     */
    default Mono<EvaluationResult> evaluateAsync(Object prediction, Object label) {
        return Mono.fromCallable(() -> evaluate(prediction, label));
    }

    /**
     * 批量评估
     *
     * @param predictions 预测结果数组
     * @param labels 真实标签数组
     * @return 聚合的评估结果
     */
    EvaluationResult evaluateBatch(Object[] predictions, Object[] labels);
}
