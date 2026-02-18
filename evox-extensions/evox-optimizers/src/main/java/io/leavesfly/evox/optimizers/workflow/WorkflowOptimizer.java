package io.leavesfly.evox.optimizers.workflow;

import io.leavesfly.evox.models.spi.LLMProvider;
import io.leavesfly.evox.optimizers.Optimizer;
import io.leavesfly.evox.optimizers.base.EvaluationFeedback;
import io.leavesfly.evox.optimizers.base.OptimizationType;
import io.leavesfly.evox.workflow.base.Workflow;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

/**
 * Workflow 级优化器抽象基类 - Evolving Layer Workflow Optimizer
 *
 * 对应论文公式 (4):
 *   W(t+1) = O_workflow(W(t), E)
 *
 * Workflow 优化器专注于改进任务分解和执行流程，通过调整工作流图
 * W = (V, E) 的结构来实现。SEW 和 AFlow 优化器用于工作流优化，
 * 通过重排节点、修改依赖关系和探索替代执行策略来迭代重构工作流图。
 *
 * @author EvoX Team
 */
@Slf4j
@Data
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public abstract class WorkflowOptimizer extends Optimizer {

    /**
     * 用于优化的 LLM
     */
    protected LLMProvider optimizerLLM;

    /**
     * 要优化的工作流 W = (V, E)
     */
    protected Workflow workflow;

    /**
     * 优化过程中找到的最佳工作流
     */
    protected Workflow bestWorkflow;

    @Override
    public final OptimizationType getOptimizationType() {
        return OptimizationType.WORKFLOW;
    }

    /**
     * 执行 Workflow 级优化操作
     * 子类实现具体的工作流图结构优化算法
     *
     * @param currentWorkflow 当前工作流 W(t)
     * @param feedback 评估反馈 E
     * @return 优化后的工作流 W(t+1)
     */
    public abstract Workflow optimizeWorkflow(Workflow currentWorkflow, EvaluationFeedback feedback);

    /**
     * 恢复优化过程中找到的最佳工作流
     */
    public void restoreBestWorkflow() {
        if (bestWorkflow != null) {
            this.workflow = bestWorkflow;
            log.info("Restored best workflow with score: {}", bestScore);
        } else {
            log.warn("No best workflow available to restore");
        }
    }

    /**
     * 更新最佳工作流（当发现更好的工作流时调用）
     *
     * @param workflow 新的最佳工作流
     * @param score 对应的评估分数
     */
    protected void updateBestWorkflow(Workflow workflow, double score) {
        this.bestWorkflow = workflow;
        this.bestScore = score;
        log.info("Updated best workflow with score: {}", score);
    }
}
