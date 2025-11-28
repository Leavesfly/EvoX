package io.leavesfly.evox.optimizers;

import io.leavesfly.evox.core.module.BaseModule;
import io.leavesfly.evox.workflow.base.Workflow;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Base Optimizer class for workflow optimization.
 * Provides common functionality for different optimization strategies like TextGrad, MIPRO, AFlow.
 */
@Slf4j
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public abstract class Optimizer extends BaseModule {

    /**
     * The workflow to optimize
     */
    protected Workflow workflow;

    /**
     * Maximum number of optimization steps
     */
    protected int maxSteps;

    /**
     * Evaluate every N steps
     */
    protected int evalEveryNSteps;

    /**
     * Number of evaluation rounds
     */
    protected int evalRounds;

    /**
     * Convergence threshold (stop if no improvement for N steps)
     */
    protected int convergenceThreshold;

    /**
     * Best score achieved during optimization
     */
    protected double bestScore;

    /**
     * Current optimization step
     */
    protected int currentStep;

    /**
     * Steps without improvement
     */
    protected int stepsWithoutImprovement;

    /**
     * Optimize the workflow using the given dataset.
     *
     * @param dataset Evaluation dataset
     * @param kwargs Additional parameters
     * @return Optimization results
     */
    public abstract OptimizationResult optimize(Object dataset, Map<String, Object> kwargs);

    /**
     * Perform a single optimization step.
     *
     * @param kwargs Additional parameters
     * @return Step result
     */
    public abstract StepResult step(Map<String, Object> kwargs);

    /**
     * Evaluate the workflow on the given dataset.
     *
     * @param dataset Evaluation dataset
     * @param evalMode Evaluation mode (e.g., "train", "validation", "test")
     * @param kwargs Additional parameters
     * @return Evaluation metrics
     */
    public abstract EvaluationMetrics evaluate(Object dataset, String evalMode, Map<String, Object> kwargs);

    /**
     * Check if optimization has converged.
     *
     * @param currentScore Current score
     * @return True if converged, false otherwise
     */
    public boolean checkConvergence(double currentScore) {
        if (currentScore > bestScore) {
            bestScore = currentScore;
            stepsWithoutImprovement = 0;
            log.info("New best score: {}", bestScore);
            return false;
        } else {
            stepsWithoutImprovement++;
            log.info("No improvement. Steps without improvement: {}/{}", 
                    stepsWithoutImprovement, convergenceThreshold);
            return stepsWithoutImprovement >= convergenceThreshold;
        }
    }

    /**
     * Reset optimizer state.
     */
    public void reset() {
        currentStep = 0;
        bestScore = Double.NEGATIVE_INFINITY;
        stepsWithoutImprovement = 0;
        log.info("Optimizer state reset");
    }

    @Override
    public String toJson() {
        return String.format(
            "{\"type\":\"%s\",\"maxSteps\":%d,\"currentStep\":%d,\"bestScore\":%.4f}",
            getClass().getSimpleName(), maxSteps, currentStep, bestScore
        );
    }

    public void fromJson(String json) {
        // TODO: Implement JSON deserialization if needed
        log.warn("fromJson not implemented for Optimizer");
    }

    /**
     * Optimization result container.
     */
    @Data
    @SuperBuilder
    public static class OptimizationResult {
        private boolean success;
        private double finalScore;
        private int totalSteps;
        private String message;
        private Map<String, Object> metadata;
    }

    /**
     * Single step result container.
     */
    @Data
    @SuperBuilder
    public static class StepResult {
        private int step;
        private double score;
        private String modification;
        private boolean improved;
        private Map<String, Object> details;
    }

    /**
     * Evaluation metrics container.
     */
    @Data
    @SuperBuilder
    public static class EvaluationMetrics {
        private double accuracy;
        private double f1Score;
        private int totalSamples;
        private int correctSamples;
        private Map<String, Object> additionalMetrics;

        public double getScore() {
            return f1Score > 0 ? f1Score : accuracy;
        }
    }
}
