package io.leavesfly.evox.optimizers.memory;

import io.leavesfly.evox.models.spi.LLMProvider;
import io.leavesfly.evox.optimizers.Optimizer;
import io.leavesfly.evox.optimizers.base.EvaluationFeedback;
import io.leavesfly.evox.optimizers.base.OptimizationType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

/**
 * Memory 级优化器抽象基类 - Evolving Layer Memory Optimizer
 *
 * 对应论文公式 (5):
 *   M(t+1) = O_memory(M(t), E)
 *
 * Memory 优化器旨在提供结构化、持久化的记忆模块，支持选择性保留、
 * 动态裁剪和基于优先级的检索。通过评估反馈来优化 agent 的记忆
 * 管理策略，提升上下文利用效率。
 *
 * @author EvoX Team
 */
@Slf4j
@Data
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public abstract class BaseMemoryOptimizer extends Optimizer {

    /**
     * 用于记忆分析和摘要的 LLM
     */
    protected LLMProvider llm;

    /**
     * 记忆压缩比例 (0-1)
     */
    protected double compressionRatio;

    /**
     * 是否启用智能摘要
     */
    protected boolean enableSmartSummary;

    @Override
    public final OptimizationType getOptimizationType() {
        return OptimizationType.MEMORY;
    }

    /**
     * 执行 Memory 级优化操作
     * 子类实现具体的记忆优化策略（选择性保留、动态裁剪、优先级检索）
     *
     * @param feedback 评估反馈 E
     * @return 优化是否产生了改进
     */
    public abstract boolean optimizeMemory(EvaluationFeedback feedback);

    /**
     * 分析当前记忆状态，返回记忆质量评估
     *
     * @return 记忆质量分数 (0-1)
     */
    public abstract double analyzeMemoryQuality();

    /**
     * 执行记忆压缩（选择性保留）
     *
     * @return 压缩是否成功
     */
    public abstract boolean compressMemory();

    /**
     * 执行记忆裁剪（动态裁剪低重要性记忆）
     *
     * @return 裁剪是否成功
     */
    public abstract boolean pruneMemory();
}
