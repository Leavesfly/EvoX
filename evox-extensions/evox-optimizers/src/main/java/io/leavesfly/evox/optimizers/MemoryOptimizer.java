package io.leavesfly.evox.optimizers;

import io.leavesfly.evox.memory.shortterm.ShortTermMemory;
import io.leavesfly.evox.models.base.LLMProvider;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * 记忆优化器
 * 优化Agent的记忆管理策略,提升上下文利用效率
 *
 * @author EvoX Team
 */
@Slf4j
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class MemoryOptimizer extends Optimizer {

    /**
     * 要优化的记忆系统
     */
    private ShortTermMemory memory;

    /**
     * LLM用于分析记忆重要性
     */
    private LLMProvider llm;

    /**
     * 记忆压缩比例 (0-1)
     */
    private double compressionRatio;

    /**
     * 是否启用智能摘要
     */
    private boolean enableSmartSummary;

    // 无参构造函数供 SuperBuilder 使用

    @Override
    public OptimizationResult optimize(Object dataset, Map<String, Object> kwargs) {
        log.info("开始记忆优化,压缩比例: {}", compressionRatio);
        reset();

        try {
            for (int step = 0; step < maxSteps; step++) {
                currentStep = step;

                // 执行优化步骤
                StepResult stepResult = step(kwargs);

                log.info("步骤 {}: 分数={}, 改进={}", 
                        step, stepResult.getScore(), stepResult.isImproved());

                // 检查收敛
                if (checkConvergence(stepResult.getScore())) {
                    log.info("优化已收敛,提前停止");
                    break;
                }
            }

            return OptimizationResult.builder()
                    .success(true)
                    .finalScore(bestScore)
                    .totalSteps(currentStep + 1)
                    .message("记忆优化完成")
                    .metadata(Map.of(
                            "compression_ratio", compressionRatio,
                            "smart_summary_enabled", enableSmartSummary
                    ))
                    .build();

        } catch (Exception e) {
            log.error("记忆优化失败", e);
            return OptimizationResult.builder()
                    .success(false)
                    .finalScore(bestScore)
                    .totalSteps(currentStep + 1)
                    .message("优化失败: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public StepResult step(Map<String, Object> kwargs) {
        log.debug("执行记忆优化步骤: {}", currentStep);

        try {
            // 1. 分析当前记忆状态
            MemoryAnalysis analysis = analyzeMemory();

            // 2. 执行优化策略
            boolean improved = false;
            String modification = "";

            if (analysis.redundancyScore > 0.3) {
                // 压缩冗余信息
                improved = compressRedundantMemories();
                modification = "压缩冗余记忆";
            } else if (analysis.importanceVariance > 0.5) {
                // 移除低重要性记忆
                improved = pruneUnimportantMemories();
                modification = "裁剪低重要性记忆";
            } else if (enableSmartSummary && analysis.summaryPotential > 0.7) {
                // 生成智能摘要
                improved = generateSmartSummary();
                modification = "生成智能摘要";
            }

            // 3. 评估优化效果
            double score = evaluateMemoryQuality();

            return StepResult.builder()
                    .step(currentStep)
                    .score(score)
                    .modification(modification)
                    .improved(improved)
                    .details(Map.of(
                            "redundancy", analysis.redundancyScore,
                            "importance_variance", analysis.importanceVariance
                    ))
                    .build();

        } catch (Exception e) {
            log.error("优化步骤失败", e);
            return StepResult.builder()
                    .step(currentStep)
                    .score(0.0)
                    .modification("步骤失败")
                    .improved(false)
                    .build();
        }
    }

    @Override
    public EvaluationMetrics evaluate(Object dataset, String evalMode, Map<String, Object> kwargs) {
        log.info("评估记忆质量,模式: {}", evalMode);

        try {
            double qualityScore = evaluateMemoryQuality();
            int memoryCount = memory != null ? memory.getMessages().size() : 0;

            return EvaluationMetrics.builder()
                    .accuracy(qualityScore)
                    .f1Score(qualityScore)
                    .totalSamples(memoryCount)
                    .correctSamples((int) (memoryCount * qualityScore))
                    .additionalMetrics(Map.of(
                            "memory_size", memoryCount,
                            "compression_effective", compressionRatio
                    ))
                    .build();

        } catch (Exception e) {
            log.error("记忆评估失败", e);
            return EvaluationMetrics.builder()
                    .accuracy(0.0)
                    .f1Score(0.0)
                    .totalSamples(0)
                    .correctSamples(0)
                    .build();
        }
    }

    /**
     * 分析记忆状态
     */
    private MemoryAnalysis analyzeMemory() {
        if (memory == null) {
            return new MemoryAnalysis(0.0, 0.0, 0.0);
        }

        int messageCount = memory.getMessages().size();
        if (messageCount == 0) {
            return new MemoryAnalysis(0.0, 0.0, 0.0);
        }

        // 计算冗余度
        double redundancy = calculateRedundancy();

        // 计算重要性方差
        double importanceVariance = calculateImportanceVariance();

        // 计算摘要潜力
        double summaryPotential = messageCount > 10 ? 0.8 : 0.3;

        return new MemoryAnalysis(redundancy, importanceVariance, summaryPotential);
    }

    /**
     * 压缩冗余记忆
     */
    private boolean compressRedundantMemories() {
        if (memory == null) {
            return false;
        }

        int originalSize = memory.getMessages().size();
        int targetSize = (int) (originalSize * compressionRatio);

        if (targetSize >= originalSize) {
            return false;
        }

        log.info("压缩记忆: {} -> {}", originalSize, targetSize);
        // 实际压缩逻辑：保留最近的targetSize条消息
        // 这里简化实现
        return true;
    }

    /**
     * 裁剪低重要性记忆
     */
    private boolean pruneUnimportantMemories() {
        if (memory == null) {
            return false;
        }

        log.info("裁剪低重要性记忆");
        // 实际应该基于重要性评分进行裁剪
        return true;
    }

    /**
     * 生成智能摘要
     */
    private boolean generateSmartSummary() {
        if (memory == null || llm == null) {
            return false;
        }

        log.info("生成智能摘要");

        try {
            // 使用LLM生成对话摘要
            String prompt = "请总结以下对话的关键信息:\n" +
                    memory.getMessages().stream()
                            .map(msg -> msg.getAgent() + ": " + msg.getContent())
                            .reduce("", (a, b) -> a + "\n" + b);

            String summary = llm.generate(prompt);
            log.debug("生成摘要: {}", summary);

            return true;
        } catch (Exception e) {
            log.error("摘要生成失败", e);
            return false;
        }
    }

    /**
     * 评估记忆质量
     */
    private double evaluateMemoryQuality() {
        if (memory == null) {
            return 0.0;
        }

        int messageCount = memory.getMessages().size();
        if (messageCount == 0) {
            return 0.0;
        }

        // 基于多个维度评估质量
        double sizeScore = Math.min(1.0, messageCount / 50.0); // 适中的大小
        double redundancyScore = 1.0 - calculateRedundancy(); // 低冗余度
        double diversityScore = calculateDiversity(); // 多样性

        return (sizeScore + redundancyScore + diversityScore) / 3.0;
    }

    /**
     * 计算冗余度
     */
    private double calculateRedundancy() {
        // 简化实现：基于消息相似度
        return 0.2; // 占位值
    }

    /**
     * 计算重要性方差
     */
    private double calculateImportanceVariance() {
        // 简化实现
        return 0.5; // 占位值
    }

    /**
     * 计算多样性
     */
    private double calculateDiversity() {
        // 简化实现
        return 0.7; // 占位值
    }

    /**
     * 记忆分析结果
     */
    private static class MemoryAnalysis {
        final double redundancyScore;
        final double importanceVariance;
        final double summaryPotential;

        MemoryAnalysis(double redundancyScore, double importanceVariance, double summaryPotential) {
            this.redundancyScore = redundancyScore;
            this.importanceVariance = importanceVariance;
            this.summaryPotential = summaryPotential;
        }
    }
}
