package io.leavesfly.evox.examples.optimizer;

import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.memory.shortterm.ShortTermMemory;
import io.leavesfly.evox.models.provider.ollama.OllamaLLM;
import io.leavesfly.evox.models.provider.ollama.OllamaLLMConfig;
import io.leavesfly.evox.optimizers.*;
import io.leavesfly.evox.optimizers.base.EvaluationFeedback;
import io.leavesfly.evox.optimizers.base.OptimizationContext;
import io.leavesfly.evox.optimizers.base.OptimizationType;
import io.leavesfly.evox.workflow.base.Workflow;
import io.leavesfly.evox.workflow.graph.WorkflowGraph;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 优化器示例：完整演示 EvoX Evolving Layer 三层六种优化器
 *
 * <p>基于 EvoAgentX 论文的 Evolving Layer 架构，本示例展示如何使用
 * EvoX 框架的优化器模块来优化 Agent、Workflow 和 Memory。</p>
 *
 * <h3>三层优化器架构</h3>
 * <pre>
 * Layer 1 - Agent Optimizer:    (Prompt(t+1), θ(t+1)) = O_agent(Prompt(t), θ(t), E)
 * Layer 2 - Workflow Optimizer:  W(t+1) = O_workflow(W(t), E)
 * Layer 3 - Memory Optimizer:    M(t+1) = O_memory(M(t), E)
 * </pre>
 *
 * <h3>包含的优化器</h3>
 * <ul>
 *   <li><b>Agent 层</b>: TextGrad（文本梯度）、MIPRO（贝叶斯优化）、EvoPrompt（进化算法）</li>
 *   <li><b>Workflow 层</b>: AFlow（蒙特卡洛树搜索）、SEW（顺序工作流进化）</li>
 *   <li><b>Memory 层</b>: MemoryOptimizer（记忆压缩/裁剪/摘要）</li>
 * </ul>
 *
 * <h3>统一机制</h3>
 * <ul>
 *   <li>EvaluationFeedback: 统一评估反馈信号</li>
 *   <li>OptimizationContext: 优化上下文管理</li>
 *   <li>OptimizationType: 优化类型枚举（AGENT/WORKFLOW/MEMORY）</li>
 * </ul>
 */
@Slf4j
public class SimpleOptimizerExample {

    public static void main(String[] args) {
        printBanner("EvoX Evolving Layer 优化器完整示例", "三层六种优化器 + 统一评估反馈");

        // 准备共享资源
        Workflow testWorkflow = createTestWorkflow();
        Object testDataset = createMockDataset();
        OllamaLLM llm = createLLM();

        // ===== Layer 1: Agent Optimizer =====
        printLayerHeader("Layer 1", "Agent Optimizer",
                "(Prompt(t+1), θ(t+1)) = O_agent(Prompt(t), θ(t), E)");

        demonstrateTextGradOptimizer(testDataset, llm);
        demonstrateMIPROOptimizer(testDataset, llm);
        demonstrateEvoPromptOptimizer(testDataset, llm);

        // ===== Layer 2: Workflow Optimizer =====
        printLayerHeader("Layer 2", "Workflow Optimizer",
                "W(t+1) = O_workflow(W(t), E)");

        demonstrateAFlowOptimizer(testWorkflow, testDataset, llm);
        demonstrateSEWOptimizer(testWorkflow, testDataset, llm);

        // ===== Layer 3: Memory Optimizer =====
        printLayerHeader("Layer 3", "Memory Optimizer",
                "M(t+1) = O_memory(M(t), E)");

        demonstrateMemoryOptimizer(testDataset, llm);

        // ===== 统一机制演示 =====
        printLayerHeader("统一机制", "EvaluationFeedback & OptimizationContext", "");

        demonstrateEvaluationFeedback(llm, testDataset);
        demonstrateOptimizationContext();

        printBanner("所有优化器演示完成", "6 种优化器 × 3 层架构 + 统一评估反馈");
    }

    // ==================== Layer 1: Agent Optimizers ====================

    /**
     * 示例 1: TextGrad 优化器 — 基于文本梯度的 prompt 优化
     */
    private static void demonstrateTextGradOptimizer(Object dataset, OllamaLLM llm) {
        printSection("1.1", "TextGrad 优化器", "基于文本梯度的 prompt 优化");

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
        printOptimizationResult(result);

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

    /**
     * 示例 1.2: MIPRO 优化器 — 贝叶斯优化 + 指令生成 + 示例引导
     */
    private static void demonstrateMIPROOptimizer(Object dataset, OllamaLLM llm) {
        printSection("1.2", "MIPRO 优化器", "贝叶斯优化 + 指令生成 + 示例引导");

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
        printOptimizationResult(result);

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

    /**
     * 示例 1.3: EvoPrompt 优化器 — 进化算法驱动的 prompt 优化
     */
    private static void demonstrateEvoPromptOptimizer(Object dataset, OllamaLLM llm) {
        printSection("1.3", "EvoPrompt 优化器", "进化算法驱动的 prompt 优化（选择/交叉/变异）");

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
        printOptimizationResult(result);

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

        // 演示 optimizePrompt 和 optimizeConfig
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

    // ==================== Layer 2: Workflow Optimizers ====================

    /**
     * 示例 2.1: AFlow 优化器 — 基于 MCTS 的工作流结构优化
     */
    private static void demonstrateAFlowOptimizer(Workflow workflow, Object dataset, OllamaLLM llm) {
        printSection("2.1", "AFlow 优化器", "蒙特卡洛树搜索 (MCTS) 工作流结构优化");

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
        printOptimizationResult(result);

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

    /**
     * 示例 2.2: SEW 优化器 — 顺序工作流进化
     */
    private static void demonstrateSEWOptimizer(Workflow workflow, Object dataset, OllamaLLM llm) {
        printSection("2.2", "SEW 优化器", "顺序工作流进化（支持 YAML/Python/JSON/DSL 表示方案）");

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
        printOptimizationResult(result);

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

    // ==================== Layer 3: Memory Optimizer ====================

    /**
     * 示例 3: MemoryOptimizer — 记忆压缩/裁剪/智能摘要
     */
    private static void demonstrateMemoryOptimizer(Object dataset, OllamaLLM llm) {
        printSection("3.1", "Memory 优化器", "记忆压缩、裁剪、智能摘要");

        // 创建测试记忆
        ShortTermMemory memory = createTestMemory();
        System.out.println("  初始记忆大小: " + memory.size() + " 条消息");

        MemoryOptimizer optimizer = MemoryOptimizer.builder()
                .llm(llm)
                .memory(memory)
                .compressionRatio(0.7)
                .enableSmartSummary(true)
                .maxSteps(5)
                .evalEveryNSteps(1)
                .convergenceThreshold(3)
                .build();

        System.out.println("  优化类型: " + optimizer.getOptimizationType());
        System.out.println("  压缩比例: 0.7 | 智能摘要: 启用 | 最大步数: 5");

        Optimizer.OptimizationResult result = optimizer.optimize(dataset, Map.of());
        printOptimizationResult(result);

        // Memory 层特有 API
        double memoryQuality = optimizer.analyzeMemoryQuality();
        System.out.println("  记忆质量分析: " + String.format("%.4f", memoryQuality));

        boolean compressResult = optimizer.compressMemory();
        System.out.println("  记忆压缩: " + (compressResult ? "成功" : "无需压缩"));

        boolean pruneResult = optimizer.pruneMemory();
        System.out.println("  记忆裁剪: " + (pruneResult ? "成功" : "无需裁剪"));

        // 演示 optimizeMemory（Memory 层特有 API）
        EvaluationFeedback feedback = EvaluationFeedback.builder()
                .primaryScore(0.4)
                .evalMode("validation")
                .sampleCount(50)
                .build();

        boolean optimizeResult = optimizer.optimizeMemory(feedback);
        System.out.println("  optimizeMemory (低分数触发压缩+裁剪): " + (optimizeResult ? "成功" : "失败"));

        System.out.println("  ✅ Memory 优化完成\n");
    }

    // ==================== 统一机制演示 ====================

    /**
     * 演示统一评估反馈 EvaluationFeedback
     */
    private static void demonstrateEvaluationFeedback(OllamaLLM llm, Object dataset) {
        printSection("4.1", "EvaluationFeedback", "统一评估反馈机制");

        // 手动构建评估反馈
        EvaluationFeedback feedback = EvaluationFeedback.builder()
                .primaryScore(0.85)
                .evalMode("validation")
                .sampleCount(100)
                .textualGradient("Be more specific in instructions")
                .build();

        feedback.putMetric("accuracy", 0.9);
        feedback.putMetric("f1_score", 0.88);
        feedback.putMetric("latency_ms", 120.0);

        System.out.println("  主评估分数: " + feedback.getPrimaryScore());
        System.out.println("  评估模式: " + feedback.getEvalMode());
        System.out.println("  样本数量: " + feedback.getSampleCount());
        System.out.println("  文本梯度: " + feedback.getTextualGradient());
        System.out.println("  accuracy: " + feedback.getMetric("accuracy"));
        System.out.println("  f1_score: " + feedback.getMetric("f1_score"));
        System.out.println("  latency_ms: " + feedback.getMetric("latency_ms"));

        // 通过优化器生成评估反馈
        TextGradOptimizer optimizer = TextGradOptimizer.builder()
                .optimizerLLM(llm)
                .executorLLM(llm)
                .optimizeMode("all")
                .batchSize(3)
                .maxSteps(3)
                .evalEveryNSteps(1)
                .convergenceThreshold(3)
                .build();

        EvaluationFeedback autoFeedback = optimizer.evaluateWithFeedback(dataset, "validation", Map.of());
        System.out.println("  自动生成反馈 - 主分数: " + String.format("%.4f", autoFeedback.getPrimaryScore()));
        System.out.println("  自动生成反馈 - 评估模式: " + autoFeedback.getEvalMode());

        // 失败反馈
        EvaluationFeedback failureFeedback = EvaluationFeedback.failure("Dataset loading error");
        System.out.println("  失败反馈 - 成功: " + failureFeedback.isSuccess()
                + " 错误: " + failureFeedback.getErrorMessage());

        System.out.println("  ✅ EvaluationFeedback 演示完成\n");
    }

    /**
     * 演示优化上下文 OptimizationContext
     */
    private static void demonstrateOptimizationContext() {
        printSection("4.2", "OptimizationContext", "优化上下文管理");

        OptimizationContext context = OptimizationContext.builder()
                .maxSteps(20)
                .evalEveryNSteps(2)
                .convergenceThreshold(5)
                .build();

        System.out.println("  最大步数: " + context.getMaxSteps());
        System.out.println("  评估频率: 每 " + context.getEvalEveryNSteps() + " 步");
        System.out.println("  收敛阈值: " + context.getConvergenceThreshold());

        // 模拟优化循环
        double[] simulatedScores = {0.5, 0.6, 0.65, 0.65, 0.65, 0.65, 0.65};
        System.out.println("  模拟优化循环:");
        for (double score : simulatedScores) {
            context.advanceStep();
            boolean converged = context.checkConvergence(score);
            boolean shouldEval = context.shouldEvaluate();
            System.out.println("    步骤 " + context.getCurrentStep()
                    + ": 分数=" + score
                    + " 需评估=" + shouldEval
                    + " 已收敛=" + converged
                    + " 最佳=" + String.format("%.2f", context.getBestScore()));
            if (converged) {
                System.out.println("    ⚡ 检测到收敛，停止优化");
                break;
            }
        }

        // 记录反馈
        EvaluationFeedback feedback = EvaluationFeedback.builder()
                .primaryScore(0.65)
                .evalMode("validation")
                .build();
        context.recordFeedback(feedback);
        System.out.println("  反馈历史大小: " + context.getFeedbackHistory().size());

        // 重置
        context.reset();
        System.out.println("  重置后 - 当前步骤: " + context.getCurrentStep()
                + " 最佳分数: " + context.getBestScore());

        System.out.println("  ✅ OptimizationContext 演示完成\n");
    }

    // ==================== 辅助方法 ====================

    private static Workflow createTestWorkflow() {
        WorkflowGraph graph = new WorkflowGraph();
        Workflow workflow = new Workflow();
        workflow.setName("test-optimization-workflow");
        workflow.setGraph(graph);
        return workflow;
    }

    /**
     * 创建模拟评估数据集
     * 模拟一组问答对，用于优化器在评估阶段衡量工作流/prompt 的表现
     */
    private static List<Map<String, String>> createMockDataset() {
        List<Map<String, String>> dataset = new java.util.ArrayList<>();
        dataset.add(Map.of(
                "input", "What is the capital of France?",
                "expected", "Paris",
                "category", "geography"));
        dataset.add(Map.of(
                "input", "Explain the concept of recursion in programming.",
                "expected", "A function that calls itself to solve smaller subproblems",
                "category", "computer_science"));
        dataset.add(Map.of(
                "input", "What are the benefits of renewable energy?",
                "expected", "Reduced emissions, sustainability, lower long-term costs",
                "category", "environment"));
        dataset.add(Map.of(
                "input", "Summarize the key principles of object-oriented programming.",
                "expected", "Encapsulation, inheritance, polymorphism, abstraction",
                "category", "computer_science"));
        dataset.add(Map.of(
                "input", "How does photosynthesis work?",
                "expected", "Plants convert sunlight, water and CO2 into glucose and oxygen",
                "category", "biology"));
        return dataset;
    }

    private static ShortTermMemory createTestMemory() {
        ShortTermMemory memory = new ShortTermMemory(50);
        memory.addMessage(Message.builder()
                .agent("user").content("请帮我分析这段数据").build());
        memory.addMessage(Message.builder()
                .agent("assistant").content("好的，我来分析这段数据的特征和趋势").build());
        memory.addMessage(Message.builder()
                .agent("user").content("重点关注异常值").build());
        memory.addMessage(Message.builder()
                .agent("assistant").content("发现了3个异常数据点，分别位于...").build());
        memory.addMessage(Message.builder()
                .agent("user").content("请给出优化建议").build());
        memory.addMessage(Message.builder()
                .agent("assistant").content("基于分析结果，建议采取以下措施...").build());
        return memory;
    }

    private static OllamaLLM createLLM() {
        OllamaLLMConfig config = new OllamaLLMConfig();
        return new OllamaLLM(config);
    }

    private static void printOptimizationResult(Optimizer.OptimizationResult result) {
        System.out.println("  优化结果:");
        System.out.println("    成功: " + result.isSuccess()
                + " | 最终得分: " + String.format("%.4f", result.getFinalScore())
                + " | 总步数: " + result.getTotalSteps());
        System.out.println("    消息: " + result.getMessage());
        if (result.getMetadata() != null && !result.getMetadata().isEmpty()) {
            System.out.println("    元数据: " + result.getMetadata());
        }
    }

    private static void printBanner(String title, String subtitle) {
        System.out.println("\n╔══════════════════════════════════════════════════════╗");
        System.out.println("║  " + title);
        System.out.println("║  " + subtitle);
        System.out.println("╚══════════════════════════════════════════════════════╝\n");
    }

    private static void printLayerHeader(String layer, String name, String formula) {
        System.out.println("┌──────────────────────────────────────────────────────┐");
        System.out.println("│ " + layer + ": " + name);
        if (!formula.isEmpty()) {
            System.out.println("│ " + formula);
        }
        System.out.println("└──────────────────────────────────────────────────────┘");
    }

    private static void printSection(String number, String name, String description) {
        System.out.println("【示例 " + number + "】" + name + " — " + description);
        System.out.println("  ----------------------------------------");
    }
}
