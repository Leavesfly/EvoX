package io.leavesfly.evox.examples.optimizer;

import io.leavesfly.evox.models.provider.ollama.OllamaLLM;
import io.leavesfly.evox.optimizers.MIPROOptimizer;
import io.leavesfly.evox.optimizers.Optimizer;
import io.leavesfly.evox.optimizers.base.EvaluationFeedback;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * MIPRO 优化器示例 — 贝叶斯优化 + 指令生成 + 示例引导
 *
 * <p>MIPRO（Multi-prompt Instruction PRoposal Optimizer）属于 Evolving Layer 的
 * Agent Optimizer（Layer 1）。它结合贝叶斯优化搜索策略与 LLM 生成的候选指令，
 * 通过引导示例与标注示例共同评分，找到最优 Prompt 配置。</p>
 *
 * <h3>核心参数</h3>
 * <ul>
 *   <li>{@code autoMode} — 自动模式（light / medium / heavy）</li>
 *   <li>{@code maxBootstrappedDemos} — 最多自举演示样例数</li>
 *   <li>{@code maxLabeledDemos} — 最多标注演示样例数</li>
 *   <li>{@code numCandidates} — 候选指令数量</li>
 *   <li>{@code metricThreshold} — 接受新配置的最低分数阈值</li>
 * </ul>
 */
@Slf4j
public class MIPROOptimizerExample {

    public static void main(String[] args) {
        OptimizerExampleUtils.printBanner("MIPRO 优化器示例", "贝叶斯优化 + 指令生成 + 示例引导");
        OllamaLLM llm = OptimizerExampleUtils.createLLM();
        Object dataset = OptimizerExampleUtils.createMockDataset();
        run(dataset, llm);
    }

    /**
     * 运行 MIPRO 优化器演示
     *
     * @param dataset 评估数据集
     * @param llm     使用的语言模型
     */
    public static void run(Object dataset, OllamaLLM llm) {
        OptimizerExampleUtils.printSection("1.2", "MIPRO 优化器", "贝叶斯优化 + 指令生成 + 示例引导");

        MIPROOptimizer optimizer = MIPROOptimizer.builder()
                .optimizerLLM(llm)
                .autoMode("medium")
                .maxBootstrappedDemos(4)
                .maxLabeledDemos(4)
                .numCandidates(12)
                .metricThreshold(0.7)
                .maxSteps(8)
                .evalEveryNSteps(2)
                .convergenceThreshold(3)
                .build();

        System.out.println("  优化类型: " + optimizer.getOptimizationType());
        System.out.println("  自动模式: medium | 引导示例: 4 | 标注示例: 4 | 候选数: 12");

        Optimizer.OptimizationResult result = optimizer.optimize(dataset, Map.of());
        OptimizerExampleUtils.printOptimizationResult(result);

        // MIPRO 特有: 最佳配置
        Map<String, Object> bestConfig = optimizer.getBestConfiguration();
        System.out.println("  最佳配置: " + bestConfig);

        // 演示 optimizePrompt（从候选指令中选择）
        EvaluationFeedback feedback = EvaluationFeedback.builder()
                .primaryScore(0.82)
                .evalMode("validation")
                .sampleCount(100)
                .build();

        String optimizedPrompt = optimizer.optimizePrompt(
                "Solve the problem step by step", Map.of(), feedback);
        System.out.println("  optimizePrompt 结果: " + optimizedPrompt);

        optimizer.restoreBest();
        System.out.println("  ✅ MIPRO 优化完成\n");
    }
}
