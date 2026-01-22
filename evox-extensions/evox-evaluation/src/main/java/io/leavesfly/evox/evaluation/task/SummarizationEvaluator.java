package io.leavesfly.evox.evaluation.task;

import io.leavesfly.evox.evaluation.Evaluator;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 摘要评估器
 * 评估文本摘要的质量，包括ROUGE等指标
 *
 * @author EvoX Team
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class SummarizationEvaluator extends Evaluator {

    /**
     * 计算ROUGE-1
     */
    private boolean enableRouge1;

    /**
     * 计算ROUGE-2
     */
    private boolean enableRouge2;

    /**
     * 计算ROUGE-L
     */
    private boolean enableRougeL;

    /**
     * 计算压缩率
     */
    private boolean enableCompressionRatio;

    public SummarizationEvaluator() {
        super();
        this.enableRouge1 = true;
        this.enableRouge2 = true;
        this.enableRougeL = true;
        this.enableCompressionRatio = true;
    }

    @Override
    public EvaluationResult evaluate(Object prediction, Object label) {
        try {
            String summary = prediction.toString();
            String reference = label != null ? label.toString() : null;

            Map<String, Double> metrics = new HashMap<>();

            if (reference != null) {
                // ROUGE-1 (unigram)
                if (enableRouge1) {
                    RougeScore rouge1 = calculateRouge(summary, reference, 1);
                    metrics.put("rouge1_precision", rouge1.precision);
                    metrics.put("rouge1_recall", rouge1.recall);
                    metrics.put("rouge1_f1", rouge1.f1);
                }

                // ROUGE-2 (bigram)
                if (enableRouge2) {
                    RougeScore rouge2 = calculateRouge(summary, reference, 2);
                    metrics.put("rouge2_precision", rouge2.precision);
                    metrics.put("rouge2_recall", rouge2.recall);
                    metrics.put("rouge2_f1", rouge2.f1);
                }

                // ROUGE-L (最长公共子序列)
                if (enableRougeL) {
                    RougeScore rougeL = calculateRougeL(summary, reference);
                    metrics.put("rougeL_precision", rougeL.precision);
                    metrics.put("rougeL_recall", rougeL.recall);
                    metrics.put("rougeL_f1", rougeL.f1);
                }

                // 压缩率
                if (enableCompressionRatio) {
                    double ratio = (double) summary.length() / reference.length();
                    metrics.put("compression_ratio", ratio);
                }
            }

            // 摘要质量评估
            metrics.put("fluency", assessFluency(summary));
            metrics.put("coherence", assessCoherence(summary));

            return EvaluationResult.success(metrics);
        } catch (Exception e) {
            return EvaluationResult.failure("摘要评估失败: " + e.getMessage());
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

        int finalSuccessCount = successCount;
        if (finalSuccessCount > 0) {
            aggregatedMetrics.replaceAll((k, v) -> v / finalSuccessCount);
        }

        return EvaluationResult.success(aggregatedMetrics);
    }

    /**
     * 计算ROUGE-N分数
     */
    private RougeScore calculateRouge(String summary, String reference, int n) {
        List<String> summaryNgrams = getNgrams(summary, n);
        List<String> referenceNgrams = getNgrams(reference, n);

        if (summaryNgrams.isEmpty() || referenceNgrams.isEmpty()) {
            return new RougeScore(0.0, 0.0, 0.0);
        }

        Set<String> summarySet = new HashSet<>(summaryNgrams);
        Set<String> referenceSet = new HashSet<>(referenceNgrams);

        int overlap = 0;
        for (String ngram : summarySet) {
            if (referenceSet.contains(ngram)) {
                overlap++;
            }
        }

        double precision = (double) overlap / summarySet.size();
        double recall = (double) overlap / referenceSet.size();
        double f1 = (precision + recall) > 0 ? 2 * precision * recall / (precision + recall) : 0.0;

        return new RougeScore(precision, recall, f1);
    }

    /**
     * 计算ROUGE-L分数 (基于最长公共子序列)
     */
    private RougeScore calculateRougeL(String summary, String reference) {
        String[] summaryWords = tokenize(summary);
        String[] referenceWords = tokenize(reference);

        int lcsLength = lcsLength(summaryWords, referenceWords);

        if (summaryWords.length == 0 || referenceWords.length == 0) {
            return new RougeScore(0.0, 0.0, 0.0);
        }

        double precision = (double) lcsLength / summaryWords.length;
        double recall = (double) lcsLength / referenceWords.length;
        double f1 = (precision + recall) > 0 ? 2 * precision * recall / (precision + recall) : 0.0;

        return new RougeScore(precision, recall, f1);
    }

    /**
     * 获取n-gram列表
     */
    private List<String> getNgrams(String text, int n) {
        String[] words = tokenize(text);
        List<String> ngrams = new ArrayList<>();

        for (int i = 0; i <= words.length - n; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < n; j++) {
                if (j > 0) sb.append(" ");
                sb.append(words[i + j]);
            }
            ngrams.add(sb.toString());
        }

        return ngrams;
    }

    /**
     * 计算最长公共子序列长度
     */
    private int lcsLength(String[] a, String[] b) {
        int m = a.length;
        int n = b.length;
        int[][] dp = new int[m + 1][n + 1];

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (a[i - 1].equals(b[j - 1])) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }

        return dp[m][n];
    }

    /**
     * 评估流畅性
     */
    private double assessFluency(String text) {
        if (text == null || text.isEmpty()) {
            return 0.0;
        }

        double score = 1.0;

        // 检查句子完整性
        if (!text.endsWith(".") && !text.endsWith("。") && !text.endsWith("!") && !text.endsWith("?")) {
            score -= 0.2;
        }

        // 检查是否有重复词
        String[] words = tokenize(text);
        Set<String> uniqueWords = new HashSet<>(Arrays.asList(words));
        double repetitionRatio = 1.0 - ((double) uniqueWords.size() / words.length);
        if (repetitionRatio > 0.5) {
            score -= 0.3;
        }

        return Math.max(0, score);
    }

    /**
     * 评估连贯性
     */
    private double assessCoherence(String text) {
        if (text == null || text.isEmpty()) {
            return 0.0;
        }

        double score = 1.0;

        // 检查句子数量
        String[] sentences = text.split("[.。!?]");
        if (sentences.length < 2) {
            score -= 0.2;
        }

        // 检查是否有连接词
        boolean hasConnectives = text.contains("因此") || text.contains("所以") || 
                                  text.contains("然后") || text.contains("此外") ||
                                  text.contains("therefore") || text.contains("however") ||
                                  text.contains("moreover") || text.contains("then");
        if (!hasConnectives && sentences.length > 2) {
            score -= 0.2;
        }

        return Math.max(0, score);
    }

    /**
     * 分词
     */
    private String[] tokenize(String text) {
        return text.toLowerCase()
                .replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5\\s]", "")
                .trim()
                .split("\\s+");
    }

    /**
     * ROUGE分数容器
     */
    private static class RougeScore {
        final double precision;
        final double recall;
        final double f1;

        RougeScore(double precision, double recall, double f1) {
            this.precision = precision;
            this.recall = recall;
            this.f1 = f1;
        }
    }
}
