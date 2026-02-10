package io.leavesfly.evox.evaluation.llm;

import io.leavesfly.evox.core.evaluation.EvaluationResult;
import io.leavesfly.evox.evaluation.Evaluator;
import io.leavesfly.evox.models.base.LLMProvider;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * LLM评估器
 * 使用LLM作为评判者评估输出质量
 *
 * @author EvoX Team
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class LLMJudgeEvaluator extends Evaluator {

    /**
     * 评判用的LLM模型
     */
    private LLMProvider judgeLLM;

    /**
     * 评判提示词模板
     */
    private String judgePromptTemplate;

    /**
     * 评分范围最大值
     */
    private int maxScore;

    public LLMJudgeEvaluator() {
        super();
        this.maxScore = 10;
        this.judgePromptTemplate = """
                请评估以下回答的质量：
                
                问题或任务：{task}
                
                回答：{answer}
                
                期望答案（参考）：{expected}
                
                请从以下维度评分（0-10分）：
                1. 准确性：回答是否正确、准确
                2. 完整性：是否充分回答了问题
                3. 相关性：回答是否切题
                4. 清晰度：表达是否清晰易懂
                
                请返回JSON格式：
                {
                  "accuracy": 分数,
                  "completeness": 分数,
                  "relevance": 分数,
                  "clarity": 分数,
                  "overall": 总体分数,
                  "reasoning": "评分理由"
                }
                """;
    }

    @Override
    public EvaluationResult evaluate(Object prediction, Object label) {
        if (judgeLLM == null) {
            return EvaluationResult.failure("未配置评判LLM");
        }

        try {
            String answer = prediction.toString();
            String expected = label != null ? label.toString() : "无参考答案";

            // 构建评判提示词
            String prompt = judgePromptTemplate
                    .replace("{task}", "评估任务")
                    .replace("{answer}", answer)
                    .replace("{expected}", expected);

            // 调用LLM进行评判
            String judgeResponse = judgeLLM.generate(prompt);

            // 解析评判结果
            Map<String, Double> metrics = parseJudgeResponse(judgeResponse);

            return EvaluationResult.success(metrics);
        } catch (Exception e) {
            return EvaluationResult.failure("LLM评判失败: " + e.getMessage());
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
        if (finalSuccessCount > 0) {
            aggregatedMetrics.replaceAll((k, v) -> v / finalSuccessCount);
        }

        return EvaluationResult.success(aggregatedMetrics);
    }

    /**
     * 解析LLM评判响应
     */
    private Map<String, Double> parseJudgeResponse(String response) {
        Map<String, Double> metrics = new HashMap<>();

        try {
            // 简单的JSON解析（实际应使用Jackson）
            if (response.contains("\"accuracy\"")) {
                double accuracy = extractScore(response, "accuracy");
                metrics.put("accuracy", accuracy / maxScore);
            }

            if (response.contains("\"completeness\"")) {
                double completeness = extractScore(response, "completeness");
                metrics.put("completeness", completeness / maxScore);
            }

            if (response.contains("\"relevance\"")) {
                double relevance = extractScore(response, "relevance");
                metrics.put("relevance", relevance / maxScore);
            }

            if (response.contains("\"clarity\"")) {
                double clarity = extractScore(response, "clarity");
                metrics.put("clarity", clarity / maxScore);
            }

            if (response.contains("\"overall\"")) {
                double overall = extractScore(response, "overall");
                metrics.put("overall_score", overall / maxScore);
            }

        } catch (Exception e) {
            // 如果解析失败，返回默认分数
            metrics.put("parse_error", 1.0);
        }

        return metrics;
    }

    /**
     * 从响应中提取分数
     */
    private double extractScore(String response, String key) {
        try {
            String pattern = "\"" + key + "\"\\s*:\\s*(\\d+\\.?\\d*)";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(response);

            if (m.find()) {
                return Double.parseDouble(m.group(1));
            }
        } catch (Exception e) {
            // 忽略解析错误
        }

        return 0.0;
    }
}
