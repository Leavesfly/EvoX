package io.leavesfly.evox.evaluation.task;

import io.leavesfly.evox.core.evaluation.EvaluationResult;
import io.leavesfly.evox.evaluation.Evaluator;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * 代码生成评估器
 * 评估生成代码的正确性、可执行性和质量
 *
 * @author EvoX Team
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class CodeEvaluator extends Evaluator {

    /**
     * 是否执行语法检查
     */
    private boolean enableSyntaxCheck;

    /**
     * 是否执行测试用例
     */
    private boolean enableTestExecution;

    public CodeEvaluator() {
        super();
        this.enableSyntaxCheck = true;
        this.enableTestExecution = false;
    }

    @Override
    public EvaluationResult evaluate(Object prediction, Object label) {
        try {
            String generatedCode = prediction.toString();
            String expectedCode = label != null ? label.toString() : null;

            Map<String, Double> metrics = new HashMap<>();

            // 语法正确性检查
            if (enableSyntaxCheck) {
                double syntaxScore = checkSyntax(generatedCode);
                metrics.put("syntax_score", syntaxScore);
            }

            // 代码相似度（如果提供了期望代码）
            if (expectedCode != null) {
                double similarity = calculateSimilarity(generatedCode, expectedCode);
                metrics.put("similarity", similarity);
            }

            // 代码质量评分
            double qualityScore = assessCodeQuality(generatedCode);
            metrics.put("quality_score", qualityScore);

            // 如果启用测试执行
            if (enableTestExecution) {
                double testPassRate = executeTests(generatedCode);
                metrics.put("test_pass_rate", testPassRate);
            }

            return EvaluationResult.success(metrics);
        } catch (Exception e) {
            return EvaluationResult.failure("代码评估失败: " + e.getMessage());
        }
    }

    @Override
    public EvaluationResult evaluateBatch(Object[] predictions, Object[] labels) {
        Map<String, Double> aggregatedMetrics = new HashMap<>();
        int successCount = 0;

        for (int i = 0; i < predictions.length; i++) {
            Object label = (labels != null && i < labels.length) ? labels[i] : null;
            EvaluationResult result = evaluate(predictions[i], label);

            if (result.isSuccess()) {
                successCount++;
                result.getMetrics().forEach((key, value) ->
                        aggregatedMetrics.merge(key, value, Double::sum));
            }
        }

        // 计算平均值
        int finalSuccessCount = successCount;
        aggregatedMetrics.replaceAll((k, v) -> v / finalSuccessCount);

        return EvaluationResult.success(aggregatedMetrics);
    }

    /**
     * 检查代码语法
     */
    private double checkSyntax(String code) {
        // 简单的语法检查逻辑
        if (code == null || code.trim().isEmpty()) {
            return 0.0;
        }

        int score = 100;

        // 检查基本语法要素
        if (!code.contains("{") || !code.contains("}")) {
            score -= 30;
        }

        // 检查括号匹配
        if (!isBalancedBrackets(code)) {
            score -= 40;
        }

        return Math.max(0, score) / 100.0;
    }

    /**
     * 计算代码相似度
     */
    private double calculateSimilarity(String code1, String code2) {
        if (code1 == null || code2 == null) {
            return 0.0;
        }

        // 简单的编辑距离算法
        int distance = levenshteinDistance(code1, code2);
        int maxLen = Math.max(code1.length(), code2.length());

        return maxLen > 0 ? 1.0 - ((double) distance / maxLen) : 1.0;
    }

    /**
     * 评估代码质量
     */
    private double assessCodeQuality(String code) {
        if (code == null || code.trim().isEmpty()) {
            return 0.0;
        }

        double score = 1.0;

        // 检查代码长度合理性
        int lines = code.split("\n").length;
        if (lines < 5) {
            score -= 0.2;
        }

        // 检查是否包含注释
        if (!code.contains("//") && !code.contains("/*")) {
            score -= 0.1;
        }

        return Math.max(0, score);
    }

    /**
     * 执行测试用例
     */
    private double executeTests(String code) {
        // 占位实现，实际应该编译并执行代码
        return 0.8;
    }

    /**
     * 检查括号是否匹配
     */
    private boolean isBalancedBrackets(String code) {
        int count = 0;
        for (char c : code.toCharArray()) {
            if (c == '{') count++;
            else if (c == '}') count--;
            if (count < 0) return false;
        }
        return count == 0;
    }

    /**
     * 计算编辑距离
     */
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = Math.min(Math.min(dp[i - 1][j], dp[i][j - 1]), dp[i - 1][j - 1]) + 1;
                }
            }
        }

        return dp[s1.length()][s2.length()];
    }
}
