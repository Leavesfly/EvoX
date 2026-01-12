package io.leavesfly.evox.examples.optimizer;

import io.leavesfly.evox.models.openai.OpenAILLM;
import io.leavesfly.evox.models.config.OpenAILLMConfig;
import io.leavesfly.evox.optimizers.*;
import io.leavesfly.evox.workflow.base.Workflow;
import io.leavesfly.evox.workflow.core.WorkflowGraph;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 优化器示例：演示TextGrad、MIPRO、AFlow三种优化器的基本使用
 * 
 * <p>本示例展示如何使用EvoX框架的优化器模块来优化工作流和提示词</p>
 * 
 * <p>包含的优化器：
 * <ul>
 *   <li>TextGrad: 基于梯度的提示词优化</li>
 *   <li>MIPRO: 多指令提示优化</li>
 *   <li>AFlow: 自动化工作流优化</li>
 * </ul>
 * </p>
 */
@Slf4j
public class SimpleOptimizerExample {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("优化器示例：三种优化器使用演示");
        System.out.println("========================================\n");

        // 创建测试工作流
        Workflow testWorkflow = createTestWorkflow();
        
        // 创建测试数据集
        Object testDataset = createMockDataset();
        
        // 创建LLM配置
        OpenAILLM llm = createLLM();

        // 演示1: TextGrad优化器
        demonstrateTextGradOptimizer(testWorkflow, testDataset, llm);

        // 演示2: MIPRO优化器
        demonstrateMIPROOptimizer(testWorkflow, testDataset, llm);

        // 演示3: AFlow优化器
        demonstrateAFlowOptimizer(testWorkflow, testDataset, llm);

        System.out.println("\n========================================");
        System.out.println("所有优化器演示完成!");
        System.out.println("========================================");
    }

    /**
     * 演示TextGrad优化器
     */
    private static void demonstrateTextGradOptimizer(Workflow workflow, Object dataset, OpenAILLM llm) {
        System.out.println("【示例 1】TextGrad 优化器");
        System.out.println("----------------------------------------");
        
        // 创建TextGrad优化器
        TextGradOptimizer optimizer = TextGradOptimizer.builder()
                .workflow(workflow)
                .optimizerLLM(llm)
                .executorLLM(llm)
                .optimizeMode("all")  // 优化所有提示词
                .batchSize(3)
                .learningRate(0.1)
                .maxSteps(5)
                .evalEveryNSteps(1)
                .convergenceThreshold(3)
                .build();

        System.out.println("配置:");
        System.out.println("  - 优化模式: all");
        System.out.println("  - 批量大小: 3");
        System.out.println("  - 最大步数: 5");
        System.out.println("  - 学习率: 0.1");
        
        System.out.println("\n开始优化...");
        
        // 执行优化
        Map<String, Object> kwargs = new HashMap<>();
        Optimizer.OptimizationResult result = optimizer.optimize(dataset, kwargs);
        
        // 输出结果
        System.out.println("\n优化结果:");
        System.out.println("  - 成功: " + result.isSuccess());
        System.out.println("  - 最终得分: " + String.format("%.4f", result.getFinalScore()));
        System.out.println("  - 总步数: " + result.getTotalSteps());
        System.out.println("  - 消息: " + result.getMessage());
        
        // 查看优化历史
        List<Optimizer.StepResult> history = optimizer.getHistory();
        System.out.println("\n优化历史:");
        for (int i = 0; i < Math.min(3, history.size()); i++) {
            Optimizer.StepResult step = history.get(i);
            System.out.println("  步骤 " + (step.getStep() + 1) + ": " + step.getModification());
        }
        
        // 恢复最佳工作流
        optimizer.restoreBestWorkflow();
        System.out.println("\n✅ 已恢复最佳工作流配置\n");
    }

    /**
     * 演示MIPRO优化器
     */
    private static void demonstrateMIPROOptimizer(Workflow workflow, Object dataset, OpenAILLM llm) {
        System.out.println("【示例 2】MIPRO 优化器");
        System.out.println("----------------------------------------");
        
        // 创建MIPRO优化器
        MIPROOptimizer optimizer = MIPROOptimizer.builder()
                .workflow(workflow)
                .optimizerLLM(llm)
                .autoMode("medium")  // light/medium/heavy
                .maxBootstrappedDemos(4)
                .maxLabeledDemos(4)
                .numCandidates(12)
                .metricThreshold(0.7)
                .maxSteps(8)
                .evalEveryNSteps(2)
                .convergenceThreshold(3)
                .build();

        System.out.println("配置:");
        System.out.println("  - 自动模式: medium");
        System.out.println("  - 最大引导示例: 4");
        System.out.println("  - 最大标注示例: 4");
        System.out.println("  - 候选数量: 12");
        
        System.out.println("\n开始优化...");
        
        // 执行优化
        Map<String, Object> kwargs = new HashMap<>();
        Optimizer.OptimizationResult result = optimizer.optimize(dataset, kwargs);
        
        // 输出结果
        System.out.println("\n优化结果:");
        System.out.println("  - 成功: " + result.isSuccess());
        System.out.println("  - 最终得分: " + String.format("%.4f", result.getFinalScore()));
        System.out.println("  - 总步数: " + result.getTotalSteps());
        
        Map<String, Object> metadata = result.getMetadata();
        System.out.println("\n元数据:");
        System.out.println("  - 指令候选数: " + metadata.get("instructionCandidates"));
        System.out.println("  - 示例池大小: " + metadata.get("demonstrationPool"));
        
        // 获取最佳配置
        Map<String, Object> bestConfig = optimizer.getBestConfiguration();
        System.out.println("\n最佳配置:");
        System.out.println("  - 步骤: " + bestConfig.get("step"));
        System.out.println("  - 得分: " + bestConfig.get("score"));
        
        System.out.println("\n✅ MIPRO优化完成\n");
    }

    /**
     * 演示AFlow优化器
     */
    private static void demonstrateAFlowOptimizer(Workflow workflow, Object dataset, OpenAILLM llm) {
        System.out.println("【示例 3】AFlow 优化器");
        System.out.println("----------------------------------------");
        
        // 创建AFlow优化器
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

        System.out.println("配置:");
        System.out.println("  - 最大迭代次数: 10");
        System.out.println("  - 种群大小: 5");
        System.out.println("  - 收敛窗口: 3");
        System.out.println("  - 最大步数: 10");
        
        System.out.println("\n开始优化...");
        
        // 执行优化
        Map<String, Object> kwargs = new HashMap<>();
        Optimizer.OptimizationResult result = optimizer.optimize(dataset, kwargs);
        
        // 输出结果
        System.out.println("\n优化结果:");
        System.out.println("  - 成功: " + result.isSuccess());
        System.out.println("  - 最终得分: " + String.format("%.4f", result.getFinalScore()));
        System.out.println("  - 总步数: " + result.getTotalSteps());
        
        Map<String, Object> metadata = result.getMetadata();
        System.out.println("\n元数据:");
        System.out.println("  - 种群大小: " + metadata.get("populationSize"));
        System.out.println("  - 经验缓冲区: " + metadata.get("experienceBufferSize"));
        System.out.println("  - 收敛窗口: " + metadata.get("convergenceWindow"));
        
        // 查看经验缓冲区
        List<AFlowOptimizer.ExperienceEntry> experience = optimizer.getExperienceBuffer();
        System.out.println("\n经验缓冲区 (最近3条):");
        for (int i = Math.max(0, experience.size() - 3); i < experience.size(); i++) {
            AFlowOptimizer.ExperienceEntry entry = experience.get(i);
            System.out.println("  步骤 " + (entry.getStep() + 1) + ": " + entry.getModification());
        }
        
        // 恢复最佳工作流
        optimizer.restoreBestWorkflow();
        System.out.println("\n✅ 已恢复最佳工作流配置\n");
    }

    /**
     * 创建测试工作流
     */
    private static Workflow createTestWorkflow() {
        WorkflowGraph graph = new WorkflowGraph();
        Workflow workflow = new Workflow();
        workflow.setName("test-optimization-workflow");
        workflow.setGraph(graph);
        return workflow;
    }

    /**
     * 创建模拟数据集
     */
    private static Object createMockDataset() {
        // 在实际使用中，这里应该是真实的数据集
        return new Object();
    }

    /**
     * 创建LLM实例
     */
    private static OpenAILLM createLLM() {
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = "your-api-key-here";
            log.warn("未设置OPENAI_API_KEY环境变量，使用占位符");
        }

        OpenAILLMConfig config = OpenAILLMConfig.builder()
                .model("gpt-4o-mini")
                .apiKey(apiKey)
                .temperature(0.7)
                .maxTokens(1000)
                .build();

        return new OpenAILLM(config);
    }
}
