package io.leavesfly.evox.evaluation.metrics;

import lombok.extern.slf4j.Slf4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 求解率指标
 * <p>
 * 适用于MATH等数学推理任务，评估模型是否正确解决了数学问题。
 * 通过提取预测和标签中的最终数值答案进行比较来判断是否正确。
 * </p>
 * <p>
 * 计算逻辑：
 * <ul>
 *   <li>从prediction中提取最终数值答案，优先匹配\\boxed{...}格式，其次匹配末尾的数字</li>
 *   <li>从label中提取最终数值答案，使用相同的提取逻辑</li>
 *   <li>比较两个答案是否相等（忽略前后空白）</li>
 *   <li>如果相等返回1.0，否则返回0.0</li>
 * </ul>
 * </p>
 *
 * @author EvoX Team
 * @version 1.0
 */
@Slf4j
public class SolveRateMetric implements EvaluationMetric {

    private static final Pattern BOXED_PATTERN = Pattern.compile("\\\\boxed\\{(.+?)\\}");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("-?\\d+\\.?\\d*");

    @Override
    public double compute(Object prediction, Object label) {
        if (prediction == null || label == null) {
            return 0.0;
        }

        String predictionAnswer = extractAnswer(prediction.toString());
        String labelAnswer = extractAnswer(label.toString());

        if (predictionAnswer == null || labelAnswer == null) {
            return 0.0;
        }

        if (predictionAnswer.trim().equals(labelAnswer.trim())) {
            return 1.0;
        }

        return 0.0;
    }

    /**
     * 从文本中提取最终答案
     * <p>
     * 优先匹配\\boxed{...}格式，其次匹配末尾的数字
     * </p>
     *
     * @param text 输入文本
     * @return 提取的答案，如果未找到则返回null
     */
    private String extractAnswer(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }

        String trimmedText = text.trim();

        Matcher boxedMatcher = BOXED_PATTERN.matcher(trimmedText);
        if (boxedMatcher.find()) {
            return boxedMatcher.group(1).trim();
        }

        Matcher numberMatcher = NUMBER_PATTERN.matcher(trimmedText);
        String lastNumber = null;
        while (numberMatcher.find()) {
            lastNumber = numberMatcher.group();
        }

        return lastNumber;
    }

    @Override
    public String getName() {
        return "solve_rate";
    }

    @Override
    public String getDescription() {
        return "Solve rate metric for math reasoning tasks, extracts and compares final numerical answers";
    }
}
