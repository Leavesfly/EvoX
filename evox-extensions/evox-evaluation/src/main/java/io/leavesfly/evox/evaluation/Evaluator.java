package io.leavesfly.evox.evaluation;

import io.leavesfly.evox.core.evaluation.IEvaluator;
import io.leavesfly.evox.core.evaluation.EvaluationResult;
import io.leavesfly.evox.core.module.BaseModule;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import reactor.core.publisher.Mono;

/**
 * 评估器基类
 * 实现核心层的 {@link IEvaluator} 接口，并继承 {@link BaseModule} 获得模块管理能力。
 * 支持任务特定评估和 LLM 评估。
 *
 * @author EvoX Team
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public abstract class Evaluator extends BaseModule implements IEvaluator {

    /**
     * 无参构造函数
     */
    public Evaluator() {
        super();
    }

    @Override
    public abstract EvaluationResult evaluate(Object prediction, Object label);

    @Override
    public Mono<EvaluationResult> evaluateAsync(Object prediction, Object label) {
        return Mono.fromCallable(() -> evaluate(prediction, label));
    }

    @Override
    public abstract EvaluationResult evaluateBatch(Object[] predictions, Object[] labels);
}
