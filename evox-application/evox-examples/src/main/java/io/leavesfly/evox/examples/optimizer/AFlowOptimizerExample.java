package io.leavesfly.evox.examples.optimizer;

import io.leavesfly.evox.models.provider.ollama.OllamaLLM;
import io.leavesfly.evox.optimizers.AFlowOptimizer;
import io.leavesfly.evox.optimizers.Optimizer;
import io.leavesfly.evox.optimizers.base.EvaluationFeedback;
import io.leavesfly.evox.workflow.base.Workflow;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * AFlow 优化器示例 — 基于蒙特卡洛树搜索（MCTS）的工作流结构优化
 *
 * <p>AFlow 属于 Evolving Layer 的 Workflow Optimizer（Layer 2），通过 MCTS 搜索策略
 * 探索工作流拓扑空间，利用 LLM 生成结构修改提案，并以评估分数作为反馈信号，
 * 迭代找到最优工作流结构。</p>
 *
 * <h3>核心参数</h3>
 * <ul>
 *   <li>{@code maxIterations} — MCTS 最大迭代次数</li>
 *   <li>{@code populationSize} — 工作流候选种群大小</li>
 *   <li>{@code convergenceWindow} — 判断收敛的滑动窗口大小</li>
 * </ul>
 */
@Slf4j
public class AFlowOptimizerExample {

    public static void main(String[] args) {
        OptimizerExampleUtils.printBanner("AFlow 优化器示例", "蒙特卡洛树搜索 (MCTS) 工作流结构优化");
        OllamaLLM llm = OptimizerExampleUtils.createLLM();
        Workflow workflow = OptimizerExampleUtils.createTestWorkflow();
        Object dataset = OptimizerExampleUtils.createMockDataset();
        run(workflow, dataset, llm);
    }

    /**
     * 运行 AFlow 优化器演示
     *
     * @param workflow 待优化的工作流
     * @param dataset  评估数据集
     * @param llm      使用的语言模型
     */
    public static void run(Workflow workflow, Object dataset, OllamaLLM llm) {
        OptimizerExampleUtils.printSection("2.1", "AFlow 优化器", "蒙特卡洛树搜索 (MCTS) 工作流结构优化");

        AFlowOptimizer optimizer = AFlowOptimizer.builder()
                .workflow(workflow)
                .optimizerLLM(llm)
                .maxIterations(10)
                .populationSize(5)
                .convergenceWindow(3)
                .maxSteps(10)
                .evalEveryNSteps(2)
                .convergenceThreshold(3)
                .build();

        System.out.println("  优化类型: " + optimizer.getOptimizationType());
        System.out.println("  最大迭代: 10 | 种群大小: 5 | 收敛窗口: 3 | 最大步数: 10");

        Optimizer.OptimizationResult result = optimizer.optimize(dataset, Map.of());
        OptimizerExampleUtils.printOptimizationResult(result);

        // AFlow 特有: 经验缓冲区
        List<AFlowOptimizer.ExperienceEntry> experience = optimizer.getExperienceBuffer();
        System.out.println("  经验缓冲区 (" + experience.size() + " 条):");
        for (int i = Math.max(0, experience.size() - 3); i < experience.size(); i++) {
            AFlowOptimizer.ExperienceEntry entry = experience.get(i);
            System.out.println("    步骤 " + (entry.getStep() + 1) + ": "
                    + entry.getModification() + " (分数: " + String.format("%.4f", entry.getScore()) + ")");
        }

        // 演示 optimizeWorkflow（Workflow 层特有 API）
        EvaluationFeedback feedback = EvaluationFeedback.builder()
                .primaryScore(0.78)
                .evalMode("validation")
                .sampleCount(100)
                .build();

        Workflow optimizedWorkflow = optimizer.optimizeWorkflow(workflow, feedback);
        System.out.println("  optimizeWorkflow 返回: " + (optimizedWorkflow != null ? "成功" : "失败"));

        optimizer.restoreBestWorkflow();
        System.out.println("  ✅ 已恢复最佳工作流\n");
    }
}
