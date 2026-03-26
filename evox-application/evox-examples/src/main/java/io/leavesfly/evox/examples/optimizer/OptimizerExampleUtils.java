package io.leavesfly.evox.examples.optimizer;

import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.memory.shortterm.ShortTermMemory;
import io.leavesfly.evox.models.provider.ollama.OllamaLLM;
import io.leavesfly.evox.models.provider.ollama.OllamaLLMConfig;
import io.leavesfly.evox.optimizers.Optimizer;
import io.leavesfly.evox.workflow.base.Workflow;
import io.leavesfly.evox.workflow.graph.WorkflowGraph;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 优化器示例公共工具类
 *
 * <p>为各优化器示例提供共享的辅助方法：创建 LLM、工作流、数据集、记忆，
 * 以及格式化控制台输出。</p>
 */
public final class OptimizerExampleUtils {

    private OptimizerExampleUtils() {
        // 工具类，禁止实例化
    }

    // ==================== 对象创建 ====================

    /**
     * 创建 Ollama LLM 实例
     */
    public static OllamaLLM createLLM() {
        OllamaLLMConfig config = new OllamaLLMConfig();
        return new OllamaLLM(config);
    }

    /**
     * 创建测试用工作流
     */
    public static Workflow createTestWorkflow() {
        WorkflowGraph graph = new WorkflowGraph();
        Workflow workflow = new Workflow();
        workflow.setName("test-optimization-workflow");
        workflow.setGraph(graph);
        return workflow;
    }

    /**
     * 创建模拟评估数据集
     *
     * <p>模拟一组问答对，用于优化器在评估阶段衡量工作流/prompt 的表现。</p>
     */
    public static List<Map<String, String>> createMockDataset() {
        List<Map<String, String>> dataset = new ArrayList<>();
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

    /**
     * 创建测试用短期记忆
     */
    public static ShortTermMemory createTestMemory() {
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

    // ==================== 格式化输出 ====================

    /**
     * 打印标题横幅
     */
    public static void printBanner(String title, String subtitle) {
        System.out.println("\n╔══════════════════════════════════════════════════════╗");
        System.out.println("║  " + title);
        System.out.println("║  " + subtitle);
        System.out.println("╚══════════════════════════════════════════════════════╝\n");
    }

    /**
     * 打印层级标题
     */
    public static void printLayerHeader(String layer, String name, String formula) {
        System.out.println("┌──────────────────────────────────────────────────────┐");
        System.out.println("│ " + layer + ": " + name);
        if (!formula.isEmpty()) {
            System.out.println("│ " + formula);
        }
        System.out.println("└──────────────────────────────────────────────────────┘");
    }

    /**
     * 打印示例小节标题
     */
    public static void printSection(String number, String name, String description) {
        System.out.println("【示例 " + number + "】" + name + " — " + description);
        System.out.println("  ----------------------------------------");
    }

    /**
     * 打印优化结果摘要
     */
    public static void printOptimizationResult(Optimizer.OptimizationResult result) {
        System.out.println("  优化结果:");
        System.out.println("    成功: " + result.isSuccess()
                + " | 最终得分: " + String.format("%.4f", result.getFinalScore())
                + " | 总步数: " + result.getTotalSteps());
        System.out.println("    消息: " + result.getMessage());
        if (result.getMetadata() != null && !result.getMetadata().isEmpty()) {
            System.out.println("    元数据: " + result.getMetadata());
        }
    }
}
