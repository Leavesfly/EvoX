package io.leavesfly.evox.examples.optimizer;

import io.leavesfly.evox.optimizers.base.EvaluationFeedback;
import io.leavesfly.evox.optimizers.base.OptimizationContext;
import lombok.extern.slf4j.Slf4j;

/**
 * OptimizationContext 示例 — 优化上下文管理
 *
 * <p>{@link OptimizationContext} 是所有优化器共享的执行状态容器，追踪当前步数、
 * 最优分数、收敛状态及历史反馈，为优化循环提供统一的控制流支撑。</p>
 *
 * <h3>主要能力</h3>
 * <ul>
 *   <li>步进控制：{@code advanceStep()} / {@code getCurrentStep()}</li>
 *   <li>评估节拍：{@code shouldEvaluate()} 按 evalEveryNSteps 节奏触发评估</li>
 *   <li>收敛检测：{@code checkConvergence(score)} 当最佳分数连续 N 步不提升时返回 true</li>
 *   <li>反馈归档：{@code recordFeedback(feedback)} 保存历史评估记录</li>
 *   <li>状态重置：{@code reset()} 清空所有状态，支持复用</li>
 * </ul>
 */
@Slf4j
public class OptimizationContextExample {

    public static void main(String[] args) {
        OptimizerExampleUtils.printBanner("OptimizationContext 示例", "优化上下文管理");
        run();
    }

    /**
     * 运行 OptimizationContext 演示
     */
    public static void run() {
        OptimizerExampleUtils.printSection("4.2", "OptimizationContext", "优化上下文管理");

        OptimizationContext context = OptimizationContext.builder()
                .maxSteps(20)
                .evalEveryNSteps(2)
                .convergenceThreshold(5)
                .build();

        System.out.println("  最大步数: " + context.getMaxSteps());
        System.out.println("  评估频率: 每 " + context.getEvalEveryNSteps() + " 步");
        System.out.println("  收敛阈值: " + context.getConvergenceThreshold());

        // 模拟优化循环：分数先上升后停滞，触发收敛
        double[] simulatedScores = {0.5, 0.6, 0.65, 0.65, 0.65, 0.65, 0.65};
        System.out.println("  模拟优化循环:");
        for (double score : simulatedScores) {
            context.advanceStep();
            boolean converged = context.checkConvergence(score);
            boolean shouldEval = context.shouldEvaluate();
            System.out.println("    步骤 " + context.getCurrentStep()
                    + ": 分数=" + score
                    + " 需评估=" + shouldEval
                    + " 已收敛=" + converged
                    + " 最佳=" + String.format("%.2f", context.getBestScore()));
            if (converged) {
                System.out.println("    ⚡ 检测到收敛，停止优化");
                break;
            }
        }

        // 记录反馈
        EvaluationFeedback feedback = EvaluationFeedback.builder()
                .primaryScore(0.65)
                .evalMode("validation")
                .build();
        context.recordFeedback(feedback);
        System.out.println("  反馈历史大小: " + context.getFeedbackHistory().size());

        // 重置
        context.reset();
        System.out.println("  重置后 - 当前步骤: " + context.getCurrentStep()
                + " 最佳分数: " + context.getBestScore());

        System.out.println("  ✅ OptimizationContext 演示完成\n");
    }
}
