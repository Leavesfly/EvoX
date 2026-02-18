package io.leavesfly.evox.optimizers.base;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 优化上下文 - 管理迭代优化过程中的状态
 *
 * 封装优化过程中的迭代状态、历史记录和收敛检测逻辑，
 * 为三层优化器提供统一的状态管理能力。
 *
 * @author EvoX Team
 */
@Data
@Builder
public class OptimizationContext {

    /**
     * 最大优化步骤数
     */
    private int maxSteps;

    /**
     * 每 N 步进行一次评估
     */
    @Builder.Default
    private int evalEveryNSteps = 1;

    /**
     * 收敛阈值（连续 N 步无改善则停止）
     */
    @Builder.Default
    private int convergenceThreshold = 5;

    /**
     * 当前优化步骤
     */
    @Builder.Default
    private int currentStep = 0;

    /**
     * 优化过程中获得的最佳分数
     */
    @Builder.Default
    private double bestScore = Double.NEGATIVE_INFINITY;

    /**
     * 连续无改善的步骤数
     */
    @Builder.Default
    private int stepsWithoutImprovement = 0;

    /**
     * 优化历史记录（每步的评估反馈）
     */
    @Builder.Default
    private List<EvaluationFeedback> feedbackHistory = new ArrayList<>();

    /**
     * 额外参数
     */
    @Builder.Default
    private Map<String, Object> parameters = new HashMap<>();

    /**
     * 检查优化是否已收敛
     *
     * @param currentScore 当前分数
     * @return 如果已收敛返回 true
     */
    public boolean checkConvergence(double currentScore) {
        if (currentScore > bestScore) {
            bestScore = currentScore;
            stepsWithoutImprovement = 0;
            return false;
        } else {
            stepsWithoutImprovement++;
            return stepsWithoutImprovement >= convergenceThreshold;
        }
    }

    /**
     * 推进到下一步
     */
    public void advanceStep() {
        currentStep++;
    }

    /**
     * 是否需要在当前步骤进行评估
     */
    public boolean shouldEvaluate() {
        return evalEveryNSteps > 0 && (currentStep + 1) % evalEveryNSteps == 0;
    }

    /**
     * 是否已达到最大步骤数
     */
    public boolean isMaxStepsReached() {
        return currentStep >= maxSteps;
    }

    /**
     * 记录评估反馈
     */
    public void recordFeedback(EvaluationFeedback feedback) {
        feedbackHistory.add(feedback);
    }

    /**
     * 重置上下文状态
     */
    public void reset() {
        currentStep = 0;
        bestScore = Double.NEGATIVE_INFINITY;
        stepsWithoutImprovement = 0;
        feedbackHistory.clear();
    }
}
