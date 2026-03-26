package io.leavesfly.evox.examples.optimizer;

import io.leavesfly.evox.models.provider.ollama.OllamaLLM;
import io.leavesfly.evox.optimizers.EvoPromptOptimizer;
import io.leavesfly.evox.optimizers.Optimizer;
import io.leavesfly.evox.optimizers.base.EvaluationFeedback;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * EvoPrompt 优化器示例 — 进化算法驱动的 Prompt 优化
 *
 * <p>EvoPrompt 属于 Evolving Layer 的 Agent Optimizer（Layer 1），基于遗传算法思想，
 * 对 Prompt 种群执行选择、交叉、变异操作，通过多代进化找到高适应度的 Prompt。
 * 支持多节点并发优化，适用于多步骤 Agent 的节点级 Prompt 调优。</p>
 *
 * <h3>核心参数</h3>
 * <ul>
 *   <li>{@code populationSize} — 种群大小</li>
 *   <li>{@code iterations} — 进化迭代次数</li>
 *   <li>{@code mutationRate} — 变异概率</li>
 *   <li>{@code crossoverRate} — 交叉概率</li>
 *   <li>{@code eliteSize} — 每代保留的精英个体数</li>
 *   <li>{@code concurrencyLimit} — 并发评估任务数上限</li>
 * </ul>
 */
@Slf4j
public class EvoPromptOptimizerExample {

    public static void main(String[] args) {
        OptimizerExampleUtils.printBanner("EvoPrompt 优化器示例", "进化算法驱动的 Prompt 优化");
        OllamaLLM llm = OptimizerExampleUtils.createLLM();
        run(llm);
    }

    /**
     * 运行 EvoPrompt 优化器演示
     *
     * @param llm 使用的语言模型
     */
    public static void run(OllamaLLM llm) {
        OptimizerExampleUtils.printSection("1.3", "EvoPrompt 优化器", "进化算法驱动的 prompt 优化（选择/交叉/变异）");

        EvoPromptOptimizer optimizer = EvoPromptOptimizer.builder()
                .optimizerLLM(llm)
                .populationSize(6)
                .iterations(3)
                .mutationRate(0.3)
                .crossoverRate(0.5)
                .eliteSize(2)
                .concurrencyLimit(4)
                .nodePopulations(new HashMap<>())
                .nodeScores(new HashMap<>())
                .bestIndividuals(new HashMap<>())
                .bestScores(new HashMap<>())
                .maxSteps(10)
                .evalEveryNSteps(1)
                .convergenceThreshold(3)
                .build();

        System.out.println("  优化类型: " + optimizer.getOptimizationType());
        System.out.println("  种群大小: 6 | 迭代次数: 3 | 变异率: 0.3 | 交叉率: 0.5 | 精英数: 2");

        // EvoPrompt 使用 Map<节点名, 初始提示词> 作为数据集
        Map<String, String> nodePrompts = Map.of(
                "analyzer", "Analyze the input data carefully",
                "summarizer", "Summarize the key findings concisely"
        );

        Optimizer.OptimizationResult result = optimizer.optimize(nodePrompts, Map.of());
        OptimizerExampleUtils.printOptimizationResult(result);

        // EvoPrompt 特有: 查看各节点最佳个体
        Map<String, String> bestIndividuals = optimizer.getBestIndividuals();
        Map<String, Double> bestScores = optimizer.getBestScores();
        System.out.println("  各节点最佳个体:");
        for (Map.Entry<String, String> entry : bestIndividuals.entrySet()) {
            String truncated = entry.getValue().length() > 60
                    ? entry.getValue().substring(0, 60) + "..."
                    : entry.getValue();
            System.out.println("    " + entry.getKey() + ": \"" + truncated
                    + "\" (分数: " + String.format("%.4f", bestScores.get(entry.getKey())) + ")");
        }

        // 演示 optimizeConfig（低分时增大探索力度）
        EvaluationFeedback feedback = EvaluationFeedback.builder()
                .primaryScore(0.25)
                .evalMode("train")
                .sampleCount(200)
                .build();

        Map<String, Object> optimizedConfig = optimizer.optimizeConfig(
                Map.of("mutationRate", 0.3), feedback);
        System.out.println("  低分数时 optimizeConfig (增加探索): " + optimizedConfig);

        System.out.println("  ✅ EvoPrompt 优化完成\n");
    }
}
