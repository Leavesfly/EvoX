package io.leavesfly.evox.optimizers;

import io.leavesfly.evox.core.module.BaseModule;
import io.leavesfly.evox.optimizers.base.EvaluationFeedback;
import io.leavesfly.evox.optimizers.base.OptimizationContext;
import io.leavesfly.evox.optimizers.base.OptimizationType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 优化器基类 - Evolving Layer 统一抽象
 *
 * 为 Evolving Layer 的三层优化器（Agent/Workflow/Memory）提供通用的
 * 迭代优化框架。每个优化器遵循统一的优化范式：
 *   Target(t+1) = O(Target(t), E)
 * 其中 O 为优化算子，E 为评估反馈。
 *
 * 子类应继承对应的分层抽象类：
 * - {@link io.leavesfly.evox.optimizers.agent.AgentOptimizer} 用于 Agent 级优化
 * - {@link io.leavesfly.evox.optimizers.workflow.WorkflowOptimizer} 用于 Workflow 级优化
 * - {@link io.leavesfly.evox.optimizers.memory.BaseMemoryOptimizer} 用于 Memory 级优化
 *
 * @author EvoX Team
 */
@Slf4j
@Data
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public abstract class Optimizer extends BaseModule {

    /**
     * 最大优化步骤数
     */
    protected int maxSteps;

    /**
     * 每 N 步进行一次评估
     */
    protected int evalEveryNSteps;

    /**
     * 评估轮数
     */
    protected int evalRounds;

    /**
     * 收敛阈值(如果N步没有改善则停止)
     */
    protected int convergenceThreshold;

    /**
     * 优化过程中获得的最佳分数
     */
    protected double bestScore;

    /**
     * 当前优化步骤
     */
    protected int currentStep;

    /**
     * 没有改善的步骤数
     */
    protected int stepsWithoutImprovement;

    /**
     * 获取优化器类型
     *
     * @return 优化器类型（AGENT / WORKFLOW / MEMORY）
     */
    public abstract OptimizationType getOptimizationType();

    /**
     * 使用给定的数据集执行优化。
     *
     * @param dataset 评估数据集
     * @param kwargs 额外参数
     * @return 优化结果
     */
    public abstract OptimizationResult optimize(Object dataset, Map<String, Object> kwargs);

    /**
     * 执行单次优化步骤。
     *
     * @param kwargs 额外参数
     * @return 步骤结果
     */
    public abstract StepResult step(Map<String, Object> kwargs);

    /**
     * 在给定的数据集上进行评估。
     *
     * @param dataset 评估数据集
     * @param evalMode 评估模式(例如:"train", "validation", "test")
     * @param kwargs 额外参数
     * @return 评估指标
     */
    public abstract EvaluationMetrics evaluate(Object dataset, String evalMode, Map<String, Object> kwargs);

    /**
     * 在给定的数据集上进行评估，返回统一的评估反馈。
     *
     * @param dataset 评估数据集
     * @param evalMode 评估模式
     * @param kwargs 额外参数
     * @return 评估反馈
     */
    public EvaluationFeedback evaluateWithFeedback(Object dataset, String evalMode, Map<String, Object> kwargs) {
        EvaluationMetrics metrics = evaluate(dataset, evalMode, kwargs);
        return EvaluationFeedback.builder()
                .primaryScore(metrics.getScore())
                .evalMode(evalMode)
                .sampleCount(metrics.getTotalSamples())
                .metrics(Map.of(
                        "accuracy", metrics.getAccuracy(),
                        "f1_score", metrics.getF1Score()
                ))
                .metadata(metrics.getAdditionalMetrics() != null ? metrics.getAdditionalMetrics() : Map.of())
                .build();
    }

    /**
     * 创建优化上下文
     *
     * @return 初始化的优化上下文
     */
    public OptimizationContext createContext() {
        return OptimizationContext.builder()
                .maxSteps(maxSteps)
                .evalEveryNSteps(evalEveryNSteps)
                .convergenceThreshold(convergenceThreshold)
                .build();
    }

    /**
     * 检查优化是否已收敛。
     *
     * @param currentScore 当前分数
     * @return 如果已收敛返回true,否则返回false
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
     * 重置优化器状态
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
            "{\"type\":\"%s\",\"optimizationType\":\"%s\",\"maxSteps\":%d,\"currentStep\":%d,\"bestScore\":%.4f}",
            getClass().getSimpleName(), getOptimizationType(), maxSteps, currentStep, bestScore
        );
    }

    /**
     * 从 JSON 字符串恢复 Optimizer 状态
     * 仅恢复可序列化的基本字段
     * 
     * @param json JSON字符串
     */
    public void fromJson(String json) {
        try {
            if (json == null || json.trim().isEmpty()) {
                log.warn("Empty JSON string provided for deserialization");
                return;
            }
            
            String content = json.replaceAll("[{}\"]", "");
            String[] pairs = content.split(",");
            
            for (String pair : pairs) {
                String[] keyValue = pair.split(":");
                if (keyValue.length != 2) {
                    continue;
                }
                
                String key = keyValue[0].trim();
                String value = keyValue[1].trim();
                
                switch (key) {
                    case "maxSteps":
                        this.maxSteps = Integer.parseInt(value);
                        break;
                    case "currentStep":
                        this.currentStep = Integer.parseInt(value);
                        break;
                    case "bestScore":
                        this.bestScore = Double.parseDouble(value);
                        break;
                    case "type":
                        log.debug("Deserializing optimizer of type: {}", value);
                        break;
                    case "optimizationType":
                        log.debug("Optimizer optimization type: {}", value);
                        break;
                    default:
                        log.debug("Unknown field in JSON: {}", key);
                }
            }
            
            log.info("Successfully deserialized Optimizer state");
        } catch (Exception e) {
            log.error("Failed to deserialize JSON: {}", json, e);
            throw new RuntimeException("Failed to deserialize Optimizer from JSON", e);
        }
    }

    /**
     * 优化结果容器
     */
    @Data
    @SuperBuilder
    public static class OptimizationResult {
        private boolean success;
        private double finalScore;
        private int totalSteps;
        private String message;
        private OptimizationType optimizationType;
        private Map<String, Object> metadata;
    }

    /**
     * 单步结果容器
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
     * 评估指标容器
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
