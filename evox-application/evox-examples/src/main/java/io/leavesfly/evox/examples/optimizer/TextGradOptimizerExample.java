package io.leavesfly.evox.examples.optimizer;

import io.leavesfly.evox.models.provider.ollama.OllamaLLM;
import io.leavesfly.evox.optimizers.Optimizer;
import io.leavesfly.evox.optimizers.TextGradOptimizer;
import io.leavesfly.evox.optimizers.base.EvaluationFeedback;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * TextGrad 优化器示例 — 基于文本梯度的 Prompt 优化
 *
 * <p>TextGrad 属于 Evolving Layer 的 Agent Optimizer（Layer 1），通过模拟梯度下降的
 * 方式，利用 LLM 对 Prompt 文本生成「文本梯度」，并据此迭代更新 Prompt，从而提升
 * Agent 在特定任务上的表现。</p>
 *
 * <h3>核心参数</h3>
 * <ul>
 *   <li>{@code optimizeMode} — 优化目标（prompt / config / all）</li>
 *   <li>{@code batchSize} — 每步评估的样本批量大小</li>
 *   <li>{@code learningRate} — 文本梯度应用强度</li>
 *   <li>{@code maxSteps} — 最大优化步数</li>
 *   <li>{@code convergenceThreshold} — 连续无提升步数触发收敛</li>
 * </ul>
 */
@Slf4j
public class TextGradOptimizerExample {

    public static void main(String[] args) {
        OptimizerExampleUtils.printBanner("TextGrad 优化器示例", "基于文本梯度的 Prompt 优化");
        OllamaLLM llm = OptimizerExampleUtils.createLLM();
        Object dataset = OptimizerExampleUtils.createMockDataset();
        run(dataset, llm);
    }

    /**
     * 运行 TextGrad 优化器演示
     *
     * @param dataset 评估数据集
     * @param llm     使用的语言模型
     */
    public static void run(Object dataset, OllamaLLM llm) {
        OptimizerExampleUtils.printSection("1.1", "TextGrad 优化器", "基于文本梯度的 prompt 优化");

        TextGradOptimizer optimizer = TextGradOptimizer.builder()
                .optimizerLLM(llm)
                .executorLLM(llm)
                .currentPrompt("Analyze the given text and provide insights")
                .optimizeMode("all")
                .batchSize(3)
                .learningRate(0.1)
                .maxSteps(5)
                .evalEveryNSteps(1)
                .convergenceThreshold(3)
                .build();

        System.out.println("  优化类型: " + optimizer.getOptimizationType());
        System.out.println("  优化模式: all | 批量大小: 3 | 学习率: 0.1 | 最大步数: 5");

        // 执行优化
        Optimizer.OptimizationResult result = optimizer.optimize(dataset, Map.of());
        OptimizerExampleUtils.printOptimizationResult(result);

        // 演示 optimizePrompt 和 optimizeConfig（Agent 层特有 API）
        EvaluationFeedback feedback = EvaluationFeedback.builder()
                .primaryScore(0.75)
                .textualGradient("Be more specific about data patterns")
                .evalMode("validation")
                .sampleCount(50)
                .build();

        String optimizedPrompt = optimizer.optimizePrompt(
                "Analyze the given text", Map.of(), feedback);
        System.out.println("  optimizePrompt 结果: " + optimizedPrompt);

        Map<String, Object> optimizedConfig = optimizer.optimizeConfig(
                Map.of("batchSize", 3), feedback);
        System.out.println("  optimizeConfig 结果: " + optimizedConfig);

        // 查看优化历史
        List<Optimizer.StepResult> history = optimizer.getHistory();
        System.out.println("  优化历史 (" + history.size() + " 步):");
        for (int i = 0; i < Math.min(3, history.size()); i++) {
            Optimizer.StepResult step = history.get(i);
            System.out.println("    步骤 " + (step.getStep() + 1) + ": " + step.getModification());
        }

        // 恢复最佳 prompt
        optimizer.restoreBest();
        System.out.println("  ✅ 已恢复最佳 prompt 配置\n");
    }
}
