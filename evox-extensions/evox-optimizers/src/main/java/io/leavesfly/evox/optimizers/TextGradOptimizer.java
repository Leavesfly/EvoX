package io.leavesfly.evox.optimizers;

import io.leavesfly.evox.models.spi.LLMProvider;
import io.leavesfly.evox.optimizers.agent.AgentOptimizer;
import io.leavesfly.evox.optimizers.base.EvaluationFeedback;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TextGrad优化器,使用基于梯度的方法进行提示词优化。
 * 基于TextGrad论文: https://arxiv.org/abs/2406.07496
 * 
 * 此优化器将提示词视为可微分变量,并使用
 * 梯度下降来基于任务性能优化它们。
 */
@Slf4j
@Data
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class TextGradOptimizer extends AgentOptimizer {

    /**
     * 用于执行的LLM(运行工作流)
     */
    private LLMProvider executorLLM;

    /**
     * 优化模式: "all", "system_prompt", "instruction"
     */
    private String optimizeMode;

    /**
     * 优化的批次大小
     */
    private int batchSize;

    /**
     * 学习率(不直接使用,仅供参考)
     */
    private double learningRate;

    /**
     * 优化步骤的历史记录
     */
    private List<StepResult> history;

    @Override
    public String optimizePrompt(String currentPrompt, Map<String, Object> agentConfig, EvaluationFeedback feedback) {
        // 使用 TextGrad 的梯度下降方法优化 prompt
        // 在真实实现中，这里会使用 optimizerLLM 生成文本梯度并更新 prompt
        String gradient = feedback.getTextualGradient();
        if (gradient != null && !gradient.isEmpty()) {
            // 根据梯度更新 prompt
            return currentPrompt + " " + gradient;
        }
        return currentPrompt;
    }

    @Override
    public Map<String, Object> optimizeConfig(Map<String, Object> agentConfig, EvaluationFeedback feedback) {
        // TextGrad 主要优化 prompt，配置优化相对简单
        // 可以根据反馈调整批次大小、学习率等参数
        Map<String, Object> optimizedConfig = new HashMap<>(agentConfig);
        
        double score = feedback.getPrimaryScore();
        if (score < 0.5) {
            // 性能较差时，增加批次大小
            optimizedConfig.put("batchSize", Math.max(1, (Integer) optimizedConfig.getOrDefault("batchSize", batchSize) + 1));
        }
        
        return optimizedConfig;
    }

    @Override
    public OptimizationResult optimize(Object dataset, Map<String, Object> kwargs) {
        log.info("Starting TextGrad optimization");
        log.info("Parameters: maxSteps={}, batchSize={}, optimizeMode={}", 
                maxSteps, batchSize, optimizeMode);

        reset();
        history = new ArrayList<>();
        bestPrompt = currentPrompt; // 在真实实现中克隆或深拷贝

        for (int step = 0; step < maxSteps; step++) {
            currentStep = step;
            log.info("Optimization step {}/{}", step + 1, maxSteps);

            // 执行优化步骤
            Map<String, Object> stepKwargs = new HashMap<>(kwargs);
            stepKwargs.put("dataset", dataset);
            stepKwargs.put("step", step);

            StepResult stepResult = step(stepKwargs);
            history.add(stepResult);

            // 如果需要则进行评估
            if ((step + 1) % evalEveryNSteps == 0) {
                EvaluationMetrics metrics = evaluate(dataset, "validation", kwargs);
                double currentScore = metrics.getScore();
                log.info("Step {} evaluation score: {}", step + 1, currentScore);

                // 检查收敛
                if (checkConvergence(currentScore)) {
                    log.info("Optimization converged at step {}", step + 1);
                    break;
                }

                // 如果有改善则更新最佳 prompt
                if (currentScore > bestScore - 0.001) { // 小的epsilon用于浮点数比较
                    bestPrompt = currentPrompt; // 克隆或深拷贝
                    log.info("Updated best prompt at step {}", step + 1);
                }
            }
        }

        log.info("Optimization completed. Best score: {}", bestScore);

        return OptimizationResult.builder()
                .success(true)
                .finalScore(bestScore)
                .totalSteps(currentStep + 1)
                .message("TextGrad optimization completed")
                .metadata(Map.of(
                        "optimizeMode", optimizeMode,
                        "batchSize", batchSize,
                        "historySize", history.size()
                ))
                .build();
    }

    @Override
    public StepResult step(Map<String, Object> kwargs) {
        // 简化的步骤实现
        // 在真实实现中,这将会:
        // 1. 从数据集中采样批次
        // 2. 在批次上执行工作流
        // 3. 计算损失/梯度
        // 4. 使用梯度下降更新提示词

        log.debug("执行优化步骤 {}", currentStep);

        // 模拟提示词更新
        String modification = String.format("在步骤 %d 使用梯度下降更新了提示词", currentStep);

        return StepResult.builder()
                .step(currentStep)
                .score(0.0) // 占位符
                .modification(modification)
                .improved(false) // 占位符
                .details(Map.of(
                        "optimizeMode", optimizeMode,
                        "batchSize", batchSize
                ))
                .build();
    }

    @Override
    public EvaluationMetrics evaluate(Object dataset, String evalMode, Map<String, Object> kwargs) {
        log.info("在 {} 集上评估工作流", evalMode);

        // 简化的评估
        // 在真实实现中,这将会:
        // 1. 在评估数据集上运行工作流
        // 2. 计算指标(准确率、F1等)
        // 3. 返回评估结果

        int totalSamples = 100; // 占位符
        int correctSamples = 70; // 占位符
        double accuracy = (double) correctSamples / totalSamples;

        return EvaluationMetrics.builder()
                .accuracy(accuracy)
                .f1Score(accuracy) // 简化
                .totalSamples(totalSamples)
                .correctSamples(correctSamples)
                .additionalMetrics(Map.of(
                        "evalMode", evalMode,
                        "step", currentStep
                ))
                .build();
    }

    /**
     * 恢复优化过程中找到的最佳工作流。
     * @deprecated 使用父类的 restoreBest() 方法
     */
    @Deprecated
    public void restoreBestWorkflow() {
        restoreBest();
    }

    /**
     * 获取优化历史。
     */
    public List<StepResult> getHistory() {
        return new ArrayList<>(history);
    }
}