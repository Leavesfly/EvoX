package io.leavesfly.evox.optimizers.base;

import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 评估反馈 - Evolving Layer 统一评估信号
 *
 * 对应论文中的 E（evaluation feedback），为所有三层优化器
 * （Agent/Workflow/Memory）提供统一的评估反馈机制。
 * 优化器通过评估反馈来指导迭代优化方向。
 *
 * @author EvoX Team
 */
@Data
@Builder
public class EvaluationFeedback {

    /**
     * 评估指标映射
     * 例如: {"accuracy": 0.95, "f1_score": 0.92, "latency_ms": 120.0}
     */
    @Builder.Default
    private Map<String, Double> metrics = new HashMap<>();

    /**
     * 主评估分数（用于收敛判断和最优选择）
     */
    private double primaryScore;

    /**
     * 文本形式的梯度/反馈（用于 TextGrad 等基于文本梯度的优化器）
     */
    private String textualGradient;

    /**
     * 评估模式: "train", "validation", "test"
     */
    @Builder.Default
    private String evalMode = "validation";

    /**
     * 评估的样本数量
     */
    private int sampleCount;

    /**
     * 评估是否成功
     */
    @Builder.Default
    private boolean success = true;

    /**
     * 错误信息（如果评估失败）
     */
    private String errorMessage;

    /**
     * 额外元数据
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * 获取指定指标值
     */
    public double getMetric(String name) {
        return metrics.getOrDefault(name, 0.0);
    }

    /**
     * 设置指标值
     */
    public void putMetric(String name, double value) {
        metrics.put(name, value);
    }

    /**
     * 创建失败的评估反馈
     */
    public static EvaluationFeedback failure(String errorMessage) {
        return EvaluationFeedback.builder()
                .success(false)
                .errorMessage(errorMessage)
                .primaryScore(0.0)
                .build();
    }
}
