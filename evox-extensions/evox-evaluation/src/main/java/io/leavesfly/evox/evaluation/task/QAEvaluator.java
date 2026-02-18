package io.leavesfly.evox.evaluation.task;

import io.leavesfly.evox.core.evaluation.EvaluationResult;
import io.leavesfly.evox.evaluation.Evaluator;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * 问答(QA)评估器
 * 评估问答系统的准确性和相关性
 *
 * @author EvoX Team
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class QAEvaluator extends Evaluator {

    /**
     * 是否启用语义相似度计算
     */
    private boolean enableSemanticSimilarity;

    /**
     * 是否启用事实性检查
     */
    private boolean enableFactualityCheck;

    public QAEvaluator() {
        super();
        this.enableSemanticSimilarity = true;
        this.enableFactualityCheck = false;
    }

    @Override
    public EvaluationResult evaluate(Object prediction, Object label) {
        try {
            String answer = prediction.toString();
            String expectedAnswer = label != null ? label.toString() : null;

            Map<String, Double> metrics = new HashMap<>();

            // 精确匹配分数
            double exactMatchScore = calculateExactMatch(answer, expectedAnswer);
            metrics.put("exact_match", exactMatchScore);

            // 语义相似度
            if (enableSemanticSimilarity && expectedAnswer != null) {
                double semanticScore = calculateSemanticSimilarity(answer, expectedAnswer);
                metrics.put("semantic_similarity", semanticScore);
            }

            // 答案完整性
            double completenessScore = assessCompleteness(answer);
            metrics.put("completeness", completenessScore);

            // 答案相关性
            double relevanceScore = assessRelevance(answer);
            metrics.put("relevance", relevanceScore);

            return EvaluationResult.success(metrics);
        } catch (Exception e) {
            return EvaluationResult.failure("问答评估失败: " + e.getMessage());
        }
    }



    /**
     * 计算精确匹配分数
     */
    private double calculateExactMatch(String answer, String expected) {
        if (expected == null || answer == null) {
            return 0.0;
        }

        String normalizedAnswer = normalize(answer);
        String normalizedExpected = normalize(expected);

        return normalizedAnswer.equals(normalizedExpected) ? 1.0 : 0.0;
    }

    /**
     * 计算语义相似度
     */
    private double calculateSemanticSimilarity(String answer, String expected) {
        if (answer == null || expected == null) {
            return 0.0;
        }

        // 简化的相似度计算（基于词汇重叠）
        String[] answerTokens = tokenize(answer);
        String[] expectedTokens = tokenize(expected);

        int overlap = 0;
        for (String token : answerTokens) {
            for (String expToken : expectedTokens) {
                if (token.equals(expToken)) {
                    overlap++;
                    break;
                }
            }
        }

        int maxLen = Math.max(answerTokens.length, expectedTokens.length);
        return maxLen > 0 ? (double) overlap / maxLen : 0.0;
    }

    /**
     * 评估答案完整性
     */
    private double assessCompleteness(String answer) {
        if (answer == null || answer.trim().isEmpty()) {
            return 0.0;
        }

        // 基于答案长度和结构的完整性评估
        int wordCount = answer.split("\\s+").length;

        if (wordCount < 5) {
            return 0.3;
        } else if (wordCount < 20) {
            return 0.6;
        } else if (wordCount < 100) {
            return 0.9;
        } else {
            return 1.0;
        }
    }

    /**
     * 评估答案相关性
     */
    private double assessRelevance(String answer) {
        if (answer == null || answer.trim().isEmpty()) {
            return 0.0;
        }

        // 简单的相关性评估
        // 检查答案是否包含关键信息结构
        double score = 1.0;

        if (answer.length() < 10) {
            score -= 0.4;
        }

        if (!answer.contains(".") && !answer.contains("。")) {
            score -= 0.2;
        }

        return Math.max(0, score);
    }

    /**
     * 标准化文本
     */
    private String normalize(String text) {
        return text.toLowerCase().trim().replaceAll("\\s+", " ");
    }

    /**
     * 分词
     */
    private String[] tokenize(String text) {
        return normalize(text).split("\\s+");
    }
}
