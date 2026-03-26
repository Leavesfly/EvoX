package io.leavesfly.evox.examples.optimizer;

import io.leavesfly.evox.models.provider.ollama.OllamaLLM;
import io.leavesfly.evox.optimizers.Optimizer;
import io.leavesfly.evox.optimizers.SEWOptimizer;
import io.leavesfly.evox.optimizers.base.EvaluationFeedback;
import io.leavesfly.evox.workflow.base.Workflow;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * SEW 优化器示例 — 顺序工作流进化（Sequential Evolutionary Workflow）
 *
 * <p>SEW 属于 Evolving Layer 的 Workflow Optimizer（Layer 2），通过遗传算法对工作流
 * 进行顺序进化，支持多种表示方案（YAML / Python / JSON / DSL），以便 LLM 更好地
 * 理解和修改工作流结构。</p>
 *
 * <h3>核心参数</h3>
 * <ul>
 *   <li>{@code scheme} — 工作流表示方案（YAML / PYTHON / JSON / DSL）</li>
 *   <li>{@code maxIterations} — 进化最大代数</li>
 *   <li>{@code populationSize} — 候选工作流种群大小</li>
 *   <li>{@code mutationRate} — 变异概率</li>
 *   <li>{@code eliteRatio} — 精英保留比例</li>
 * </ul>
 */
@Slf4j
public class SEWOptimizerExample {

    public static void main(String[] args) {
        OptimizerExampleUtils.printBanner("SEW 优化器示例", "顺序工作流进化（YAML/Python/JSON/DSL 表示方案）");
        OllamaLLM llm = OptimizerExampleUtils.createLLM();
        Workflow workflow = OptimizerExampleUtils.createTestWorkflow();
        Object dataset = OptimizerExampleUtils.createMockDataset();
        run(workflow, dataset, llm);
    }

    /**
     * 运行 SEW 优化器演示
     *
     * @param workflow 待优化的工作流
     * @param dataset  评估数据集
     * @param llm      使用的语言模型
     */
    public static void run(Workflow workflow, Object dataset, OllamaLLM llm) {
        OptimizerExampleUtils.printSection("2.2", "SEW 优化器", "顺序工作流进化（支持 YAML/Python/JSON/DSL 表示方案）");

        SEWOptimizer optimizer = SEWOptimizer.builder()
                .workflow(workflow)
                .optimizerLLM(llm)
                .evaluatorLLM(llm)
                .scheme(SEWOptimizer.Scheme.YAML)
                .maxIterations(5)
                .populationSize(4)
                .mutationRate(0.3)
                .eliteRatio(0.25)
                .maxSteps(5)
                .evalEveryNSteps(1)
                .convergenceThreshold(3)
                .build();

        System.out.println("  优化类型: " + optimizer.getOptimizationType());
        System.out.println("  表示方案: YAML | 最大迭代: 5 | 种群大小: 4 | 变异率: 0.3 | 精英比例: 0.25");

        Optimizer.OptimizationResult result = optimizer.optimize(dataset, Map.of());
        OptimizerExampleUtils.printOptimizationResult(result);

        // SEW 特有: 工作流表示方案转换
        System.out.println("  工作流表示方案转换:");
        for (SEWOptimizer.Scheme scheme : SEWOptimizer.Scheme.values()) {
            optimizer.setScheme(scheme);
            String representation = optimizer.convertToScheme(workflow);
            String firstLine = representation.split("\n")[0];
            System.out.println("    " + scheme.name() + ": " + firstLine);
        }
        optimizer.setScheme(SEWOptimizer.Scheme.YAML);

        // SEW 特有: 变异类型
        System.out.println("  支持的变异类型:");
        for (SEWOptimizer.MutationType mutationType : SEWOptimizer.MutationType.values()) {
            System.out.println("    - " + mutationType.name());
        }

        // SEW 特有: 进化历史
        List<SEWOptimizer.EvolutionRecord> evolutionHistory = optimizer.getEvolutionHistory();
        System.out.println("  进化历史 (" + evolutionHistory.size() + " 代):");
        for (SEWOptimizer.EvolutionRecord record : evolutionHistory) {
            System.out.println("    第 " + (record.getGeneration() + 1) + " 代: 最佳适应度="
                    + String.format("%.4f", record.getBestFitness())
                    + " 平均适应度=" + String.format("%.4f", record.getAvgFitness()));
        }

        // SEW 特有: 最佳候选
        SEWOptimizer.WorkflowCandidate bestCandidate = optimizer.getBestCandidate();
        if (bestCandidate != null) {
            System.out.println("  最佳候选: id=" + bestCandidate.getId()
                    + " 适应度=" + String.format("%.4f", bestCandidate.getFitness())
                    + " 代数=" + bestCandidate.getGeneration());
        }

        // 演示 optimizeWorkflow
        EvaluationFeedback feedback = EvaluationFeedback.builder()
                .primaryScore(0.65)
                .evalMode("validation")
                .sampleCount(80)
                .build();

        Workflow optimizedWorkflow = optimizer.optimizeWorkflow(workflow, feedback);
        System.out.println("  optimizeWorkflow 返回: " + (optimizedWorkflow != null ? "成功" : "失败"));

        optimizer.restoreBestWorkflow();
        System.out.println("  ✅ SEW 优化完成\n");
    }
}
