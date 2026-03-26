package io.leavesfly.evox.examples.optimizer;

import io.leavesfly.evox.memory.shortterm.ShortTermMemory;
import io.leavesfly.evox.models.provider.ollama.OllamaLLM;
import io.leavesfly.evox.optimizers.MemoryOptimizer;
import io.leavesfly.evox.optimizers.Optimizer;
import io.leavesfly.evox.optimizers.base.EvaluationFeedback;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * MemoryOptimizer 示例 — 记忆压缩、裁剪与智能摘要
 *
 * <p>MemoryOptimizer 属于 Evolving Layer 的 Memory Optimizer（Layer 3），负责对
 * Agent 的短期/长期记忆进行动态管理，通过压缩、裁剪和摘要三种策略降低记忆冗余，
 * 提升 Agent 在长对话场景下的推理质量。</p>
 *
 * <h3>核心参数</h3>
 * <ul>
 *   <li>{@code compressionRatio} — 压缩目标比例（0~1），值越小压缩力度越大</li>
 *   <li>{@code enableSmartSummary} — 是否启用 LLM 智能摘要</li>
 * </ul>
 *
 * <h3>Memory 层特有 API</h3>
 * <ul>
 *   <li>{@code analyzeMemoryQuality()} — 量化评估当前记忆质量</li>
 *   <li>{@code compressMemory()} — 按压缩比例精简记忆</li>
 *   <li>{@code pruneMemory()} — 删除低价值记忆条目</li>
 *   <li>{@code optimizeMemory(feedback)} — 根据反馈分数自动选择策略</li>
 * </ul>
 */
@Slf4j
public class MemoryOptimizerExample {

    public static void main(String[] args) {
        OptimizerExampleUtils.printBanner("MemoryOptimizer 示例", "记忆压缩、裁剪与智能摘要");
        OllamaLLM llm = OptimizerExampleUtils.createLLM();
        Object dataset = OptimizerExampleUtils.createMockDataset();
        run(dataset, llm);
    }

    /**
     * 运行 MemoryOptimizer 演示
     *
     * @param dataset 评估数据集
     * @param llm     使用的语言模型
     */
    public static void run(Object dataset, OllamaLLM llm) {
        OptimizerExampleUtils.printSection("3.1", "Memory 优化器", "记忆压缩、裁剪、智能摘要");

        // 创建测试记忆
        ShortTermMemory memory = OptimizerExampleUtils.createTestMemory();
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
        OptimizerExampleUtils.printOptimizationResult(result);

        // Memory 层特有 API
        double memoryQuality = optimizer.analyzeMemoryQuality();
        System.out.println("  记忆质量分析: " + String.format("%.4f", memoryQuality));

        boolean compressResult = optimizer.compressMemory();
        System.out.println("  记忆压缩: " + (compressResult ? "成功" : "无需压缩"));

        boolean pruneResult = optimizer.pruneMemory();
        System.out.println("  记忆裁剪: " + (pruneResult ? "成功" : "无需裁剪"));

        // 演示 optimizeMemory（低分时触发压缩+裁剪）
        EvaluationFeedback feedback = EvaluationFeedback.builder()
                .primaryScore(0.4)
                .evalMode("validation")
                .sampleCount(50)
                .build();

        boolean optimizeResult = optimizer.optimizeMemory(feedback);
        System.out.println("  optimizeMemory (低分数触发压缩+裁剪): " + (optimizeResult ? "成功" : "失败"));

        System.out.println("  ✅ Memory 优化完成\n");
    }
}
