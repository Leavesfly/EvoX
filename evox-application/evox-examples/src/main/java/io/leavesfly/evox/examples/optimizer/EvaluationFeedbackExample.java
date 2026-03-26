package io.leavesfly.evox.examples.optimizer;

import io.leavesfly.evox.models.provider.ollama.OllamaLLM;
import io.leavesfly.evox.optimizers.TextGradOptimizer;
import io.leavesfly.evox.optimizers.base.EvaluationFeedback;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * EvaluationFeedback 示例 — 统一评估反馈机制
 *
 * <p>{@link EvaluationFeedback} 是 EvoX Evolving Layer 的统一反馈载体，承载每轮
 * 评估的核心信号，包括主评分、多维度指标、文本梯度及错误信息等，供各类优化器
 * 决策下一步优化方向。</p>
 *
 * <h3>主要字段</h3>
 * <ul>
 *   <li>{@code primaryScore} — 主评估分数（0~1）</li>
 *   <li>{@code evalMode} — 评估模式（train / validation / test）</li>
 *   <li>{@code sampleCount} — 参与评估的样本数量</li>
 *   <li>{@code textualGradient} — 文本梯度描述（TextGrad 专用）</li>
 *   <li>{@code metrics} — 自定义多维度指标 Map</li>
 *   <li>{@code errorMessage} — 失败时的错误原因</li>
 * </ul>
 */
@Slf4j
public class EvaluationFeedbackExample {

    public static void main(String[] args) {
        OptimizerExampleUtils.printBanner("EvaluationFeedback 示例", "统一评估反馈机制");
        OllamaLLM llm = OptimizerExampleUtils.createLLM();
        Object dataset = OptimizerExampleUtils.createMockDataset();
        run(llm, dataset);
    }

    /**
     * 运行 EvaluationFeedback 演示
     *
     * @param llm     使用的语言模型
     * @param dataset 评估数据集
     */
    public static void run(OllamaLLM llm, Object dataset) {
        OptimizerExampleUtils.printSection("4.1", "EvaluationFeedback", "统一评估反馈机制");

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

        // 通过优化器自动生成评估反馈
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
}
