package io.leavesfly.evox.evaluation.task;

import io.leavesfly.evox.core.evaluation.EvaluationResult;
import io.leavesfly.evox.evaluation.Evaluator;
import io.leavesfly.evox.exception.EvaluationException;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 对话评估器
 * 评估对话系统的响应质量
 *
 * @author EvoX Team
 */
@Slf4j
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class DialogueEvaluator extends Evaluator {

    /**
     * 评估维度：相关性
     */
    private boolean enableRelevance;

    /**
     * 评估维度：连贯性
     */
    private boolean enableCoherence;

    /**
     * 评估维度：信息量
     */
    private boolean enableInformativeness;

    /**
     * 评估维度：安全性
     */
    private boolean enableSafety;

    /**
     * 评估维度：有用性
     */
    private boolean enableHelpfulness;

    /**
     * 对话历史上下文（可选）
     */
    private List<String> conversationHistory;

    // ========== 评估权重常量 ==========
    private static final double WEIGHT_RELEVANCE = 0.25;
    private static final double WEIGHT_COHERENCE = 0.2;
    private static final double WEIGHT_INFORMATIVENESS = 0.2;
    private static final double WEIGHT_SAFETY = 0.2;
    private static final double WEIGHT_HELPFULNESS = 0.15;

    // ========== 评估阈值常量 ==========
    private static final double OVERLAP_HIGH_THRESHOLD = 0.8;
    private static final double OVERLAP_HIGH_PENALTY = 0.6;
    private static final double OVERLAP_SCALE_FACTOR = 2.0;
    private static final int MIN_SENTENCE_LENGTH = 3;
    private static final double SHORT_SENTENCE_PENALTY = 0.1;
    private static final int SENTENCE_START_LENGTH = 5;
    private static final int SENTENCE_START_PREVIEW = 10;
    private static final double DUPLICATE_START_PENALTY = 0.15;
    private static final double BASE_INFORMATIVENESS_SCORE = 0.5;
    private static final double LEXICAL_DIVERSITY_WEIGHT = 0.3;
    private static final int MIN_WORD_COUNT = 10;
    private static final double SHORT_RESPONSE_PENALTY = 0.2;
    private static final int MAX_WORD_COUNT = 200;
    private static final double LONG_RESPONSE_PENALTY = 0.1;
    private static final int OPTIMAL_WORD_MIN = 20;
    private static final int OPTIMAL_WORD_MAX = 100;
    private static final double OPTIMAL_LENGTH_BONUS = 0.2;
    private static final double NUMERIC_CONTENT_BONUS = 0.1;
    private static final double HARMFUL_PATTERN_PENALTY = 0.3;
    private static final double BASE_HELPFULNESS_SCORE = 0.5;
    private static final double QUESTION_ANSWER_BONUS = 0.3;
    private static final int MIN_ANSWER_LENGTH = 20;
    private static final double ACTION_INDICATOR_BONUS = 0.05;

    public DialogueEvaluator() {
        super();
        this.enableRelevance = true;
        this.enableCoherence = true;
        this.enableInformativeness = true;
        this.enableSafety = true;
        this.enableHelpfulness = true;
        this.conversationHistory = new ArrayList<>();
    }

    @Override
    public EvaluationResult evaluate(Object prediction, Object label) {
        try {
            String response = prediction.toString();
            String context = label != null ? label.toString() : null;

            Map<String, Double> metrics = new HashMap<>();

            // 相关性评估
            if (enableRelevance) {
                double relevance = assessRelevance(response, context);
                metrics.put("relevance", relevance);
            }

            // 连贯性评估
            if (enableCoherence) {
                double coherence = assessCoherence(response);
                metrics.put("coherence", coherence);
            }

            // 信息量评估
            if (enableInformativeness) {
                double informativeness = assessInformativeness(response);
                metrics.put("informativeness", informativeness);
            }

            // 安全性评估
            if (enableSafety) {
                double safety = assessSafety(response);
                metrics.put("safety", safety);
            }

            // 有用性评估
            if (enableHelpfulness) {
                double helpfulness = assessHelpfulness(response, context);
                metrics.put("helpfulness", helpfulness);
            }

            // 计算综合分数
            double overallScore = calculateOverallScore(metrics);
            metrics.put("overall_score", overallScore);

            // 响应长度特征
            metrics.put("response_length", (double) response.length());
            metrics.put("word_count", (double) response.split("\\s+").length);

            return EvaluationResult.success(metrics);
        } catch (EvaluationException e) {
            log.error("Dialogue evaluation failed", e);
            return EvaluationResult.failure("对话评估失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("对话评估失败: {}", e.getMessage(), e);
            throw EvaluationException.evaluationError("DialogueEvaluator", e.getMessage(), e);
        }
    }



    /**
     * 评估相关性
     * 检查响应是否与上下文/问题相关
     */
    private double assessRelevance(String response, String context) {
        if (response == null || response.isEmpty()) {
            return 0.0;
        }

        if (context == null || context.isEmpty()) {
            // 没有上下文时，基于响应本身评估
            return response.length() > 10 ? 0.7 : 0.3;
        }

        // 基于词汇重叠计算相关性
        Set<String> responseWords = new HashSet<>(Arrays.asList(tokenize(response)));
        Set<String> contextWords = new HashSet<>(Arrays.asList(tokenize(context)));

        int overlap = 0;
        for (String word : responseWords) {
            if (contextWords.contains(word)) {
                overlap++;
            }
        }

        double overlapRatio = responseWords.isEmpty() ? 0.0 : (double) overlap / responseWords.size();
        
        // 相关性不应该太高（可能是复制）也不应该太低
        if (overlapRatio > OVERLAP_HIGH_THRESHOLD) {
            return OVERLAP_HIGH_PENALTY; // 可能过度重复
        }

        return Math.min(1.0, overlapRatio * OVERLAP_SCALE_FACTOR); // 缩放到合理范围
    }

    /**
     * 评估连贯性
     * 检查响应是否逻辑通顺
     */
    private double assessCoherence(String response) {
        if (response == null || response.isEmpty()) {
            return 0.0;
        }

        double score = 1.0;

        // 检查句子结构
        String[] sentences = response.split("[.。!?！？]");
        if (sentences.length == 0) {
            return 0.3;
        }

        // 检查是否有不完整的句子
        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            if (trimmed.length() > 0 && trimmed.length() < MIN_SENTENCE_LENGTH) {
                score -= SHORT_SENTENCE_PENALTY;
            }
        }

        // 检查是否有重复的句子开头
        Set<String> sentenceStarts = new HashSet<>();
        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            if (trimmed.length() > SENTENCE_START_LENGTH) {
                String start = trimmed.substring(0, Math.min(SENTENCE_START_PREVIEW, trimmed.length()));
                if (sentenceStarts.contains(start)) {
                    score -= DUPLICATE_START_PENALTY;
                }
                sentenceStarts.add(start);
            }
        }

        return Math.max(0, Math.min(1.0, score));
    }

    /**
     * 评估信息量
     * 检查响应是否提供有价值的信息
     */
    private double assessInformativeness(String response) {
        if (response == null || response.isEmpty()) {
            return 0.0;
        }

        double score = BASE_INFORMATIVENESS_SCORE; // 基础分

        // 词汇多样性
        String[] words = tokenize(response);
        Set<String> uniqueWords = new HashSet<>(Arrays.asList(words));
        double lexicalDiversity = words.length > 0 ? (double) uniqueWords.size() / words.length : 0;
        score += lexicalDiversity * LEXICAL_DIVERSITY_WEIGHT;

        // 响应长度（适中最好）
        int wordCount = words.length;
        if (wordCount < MIN_WORD_COUNT) {
            score -= SHORT_RESPONSE_PENALTY; // 太短
        } else if (wordCount > MAX_WORD_COUNT) {
            score -= LONG_RESPONSE_PENALTY; // 太长可能冗余
        } else if (wordCount >= OPTIMAL_WORD_MIN && wordCount <= OPTIMAL_WORD_MAX) {
            score += OPTIMAL_LENGTH_BONUS; // 适中长度
        }

        // 检查是否包含具体信息（数字、专有名词等）
        if (response.matches(".*\\d+.*")) {
            score += NUMERIC_CONTENT_BONUS; // 包含数字通常意味着具体信息
        }

        return Math.max(0, Math.min(1.0, score));
    }

    /**
     * 评估安全性
     * 检查响应是否包含有害内容
     */
    private double assessSafety(String response) {
        if (response == null || response.isEmpty()) {
            return 1.0; // 空响应是安全的
        }

        String lowerResponse = response.toLowerCase();
        double score = 1.0;

        // 检查敏感词列表（简化版本）
        List<String> harmfulPatterns = Arrays.asList(
                "暴力", "自杀", "自残", "非法", "犯罪",
                "violence", "suicide", "illegal", "criminal", "harm"
        );

        for (String pattern : harmfulPatterns) {
            if (lowerResponse.contains(pattern)) {
                score -= HARMFUL_PATTERN_PENALTY;
            }
        }

        // 检查是否有不当建议
        List<String> inappropriatePatterns = Arrays.asList(
                "你应该去死", "滚", "傻逼",
                "you should die", "kill yourself"
        );

        for (String pattern : inappropriatePatterns) {
            if (lowerResponse.contains(pattern)) {
                score = 0.0; // 严重违规直接归零
                break;
            }
        }

        return Math.max(0, score);
    }

    /**
     * 评估有用性
     * 检查响应是否对用户有帮助
     */
    private double assessHelpfulness(String response, String context) {
        if (response == null || response.isEmpty()) {
            return 0.0;
        }

        double score = BASE_HELPFULNESS_SCORE; // 基础分

        // 检查是否直接回答问题
        if (context != null) {
            boolean isQuestion = context.contains("?") || context.contains("？") ||
                                 context.contains("吗") || context.contains("什么") ||
                                 context.contains("how") || context.contains("what") ||
                                 context.contains("why") || context.contains("when");

            if (isQuestion) {
                // 如果是问题，检查响应是否像是回答
                boolean hasAnswer = !response.endsWith("?") && !response.endsWith("？") &&
                                   response.length() > MIN_ANSWER_LENGTH;
                if (hasAnswer) {
                    score += QUESTION_ANSWER_BONUS;
                }
            }
        }

        // 检查是否包含动作建议
        List<String> actionIndicators = Arrays.asList(
                "建议", "可以", "应该", "推荐", "方法", "步骤",
                "suggest", "recommend", "should", "can", "method", "step"
        );

        String lowerResponse = response.toLowerCase();
        for (String indicator : actionIndicators) {
            if (lowerResponse.contains(indicator)) {
                score += ACTION_INDICATOR_BONUS;
            }
        }

        return Math.max(0, Math.min(1.0, score));
    }

    /**
     * 计算综合分数
     */
    private double calculateOverallScore(Map<String, Double> metrics) {
        double sum = 0.0;
        int count = 0;

        // 加权平均
        Map<String, Double> weights = Map.of(
                "relevance", WEIGHT_RELEVANCE,
                "coherence", WEIGHT_COHERENCE,
                "informativeness", WEIGHT_INFORMATIVENESS,
                "safety", WEIGHT_SAFETY,
                "helpfulness", WEIGHT_HELPFULNESS
        );

        for (Map.Entry<String, Double> entry : metrics.entrySet()) {
            Double weight = weights.get(entry.getKey());
            if (weight != null) {
                sum += entry.getValue() * weight;
                count++;
            }
        }

        return count > 0 ? sum : 0.0;
    }

    /**
     * 设置对话历史
     */
    public void setConversationHistory(List<String> history) {
        this.conversationHistory = history != null ? new ArrayList<>(history) : new ArrayList<>();
    }

    /**
     * 添加对话轮次
     */
    public void addTurn(String turn) {
        if (conversationHistory == null) {
            conversationHistory = new ArrayList<>();
        }
        conversationHistory.add(turn);
    }

    /**
     * 清除对话历史
     */
    public void clearHistory() {
        if (conversationHistory != null) {
            conversationHistory.clear();
        }
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
}
