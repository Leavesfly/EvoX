package io.leavesfly.evox.evaluation.llm;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.leavesfly.evox.core.evaluation.EvaluationResult;
import io.leavesfly.evox.evaluation.Evaluator;
import io.leavesfly.evox.exception.EvaluationException;
import io.leavesfly.evox.models.spi.LLMProvider;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LLM 评估器
 * <p>
 * 使用大语言模型作为评判者，提供灵活的、上下文感知的评估能力。
 * 支持以下评估模式：
 * <ul>
 *   <li><b>质量评估</b>：从准确性、完整性、相关性、清晰度等维度评估输出质量</li>
 *   <li><b>一致性检查</b>：检查输出与参考答案之间的语义一致性</li>
 *   <li><b>动态标准评估</b>：支持用户自定义评估标准，适应静态指标无法捕捉的场景</li>
 * </ul>
 * </p>
 *
 * @author EvoX Team
 */
@Slf4j
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class LLMJudgeEvaluator extends Evaluator {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Pattern SCORE_PATTERN = Pattern.compile("\"(\\w+)\"\\s*:\\s*(\\d+\\.?\\d*)");
    private static final Pattern JSON_BLOCK_PATTERN = Pattern.compile("\\{[^{}]*\\}");

    /**
     * 评判用的 LLM 模型
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

    /**
     * 是否启用一致性检查
     */
    private boolean enableConsistencyCheck;

    /**
     * 用户自定义的动态评估标准列表
     */
    private List<String> dynamicCriteria;

    /**
     * 一致性检查提示词模板
     */
    private String consistencyPromptTemplate;

    /**
     * 动态标准评估提示词模板
     */
    private String dynamicCriteriaPromptTemplate;

    public LLMJudgeEvaluator() {
        super();
        this.maxScore = 10;
        this.enableConsistencyCheck = false;
        this.dynamicCriteria = new ArrayList<>();

        this.judgePromptTemplate = """
                请评估以下回答的质量：
                
                任务/问题：{task}
                
                回答：{answer}
                
                期望答案（参考）：{expected}
                
                请从以下维度进行评分（0-{max_score}）：
                1. accuracy（准确性）：回答是否正确、准确？
                2. completeness（完整性）：是否完整地回答了问题？
                3. relevance（相关性）：回答是否切题？
                4. clarity（清晰度）：表达是否清晰易懂？
                
                {dynamic_criteria_section}
                
                请仅返回一个 JSON 对象（不要 markdown 标记，不要额外解释）：
                {
                  "accuracy": 分数,
                  "completeness": 分数,
                  "relevance": 分数,
                  "clarity": 分数,
                  "overall": 综合分数,
                  "reasoning": "简要评价说明"
                }
                """;

        this.consistencyPromptTemplate = """
                请检查以下两段文本之间的语义一致性：
                
                文本 A（预测输出）：{prediction}
                
                文本 B（参考答案）：{reference}
                
                请从以下方面进行评估：
                1. factual_consistency（事实一致性）：两段文本是否传达了相同的事实信息？（0-{max_score}）
                2. semantic_similarity（语义相似度）：两段文本的语义相似程度如何？（0-{max_score}）
                3. contradiction_score（无矛盾度）：是否存在矛盾？（0 = 存在大量矛盾，{max_score} = 无矛盾）
                
                请仅返回一个 JSON 对象（不要 markdown 标记，不要额外解释）：
                {
                  "factual_consistency": 分数,
                  "semantic_similarity": 分数,
                  "contradiction_score": 分数,
                  "consistency_overall": 综合分数,
                  "reasoning": "简要评价说明"
                }
                """;

        this.dynamicCriteriaPromptTemplate = """
                请根据指定的评估标准对以下输出进行评价：
                
                输出内容：{answer}
                
                参考答案（如有）：{expected}
                
                评估标准：
                {criteria_list}
                
                请为每个标准给出 0 到 {max_score} 的分数。
                
                请仅返回一个 JSON 对象（不要 markdown 标记，不要额外解释），以每个标准名称为 key、分数为 value。
                同时包含 "reasoning" 字段，提供简要的评价说明。
                """;
    }

    @Override
    public EvaluationResult evaluate(Object prediction, Object label) {
        if (judgeLLM == null) {
            return EvaluationResult.failure("Judge LLM is not configured");
        }

        try {
            String answer = prediction.toString();
            String expected = label != null ? label.toString() : "No reference answer available";

            Map<String, Double> allMetrics = new LinkedHashMap<>();

            Map<String, Double> qualityMetrics = evaluateQuality(answer, expected);
            allMetrics.putAll(qualityMetrics);

            if (enableConsistencyCheck && label != null) {
                Map<String, Double> consistencyMetrics = evaluateConsistency(answer, expected);
                allMetrics.putAll(consistencyMetrics);
            }

            if (!dynamicCriteria.isEmpty()) {
                Map<String, Double> dynamicMetrics = evaluateDynamicCriteria(answer, expected);
                allMetrics.putAll(dynamicMetrics);
            }

            Map<String, Double> registeredScores = computeRegisteredMetrics(prediction, label);
            allMetrics.putAll(registeredScores);

            return EvaluationResult.success(allMetrics);
        } catch (EvaluationException e) {
            log.error("LLM judge evaluation failed: {}", e.getMessage(), e);
            return EvaluationResult.failure("LLM judge evaluation failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("LLM judge evaluation failed: {}", e.getMessage(), e);
            throw EvaluationException.evaluationError("LLMJudgeEvaluator", e.getMessage(), e);
        }
    }

    /**
     * 质量评估：从准确性、完整性、相关性、清晰度等维度评估
     *
     * @param answer   模型输出
     * @param expected 期望答案
     * @return 质量评估指标
     */
    private Map<String, Double> evaluateQuality(String answer, String expected) {
        String dynamicSection = "";
        if (!dynamicCriteria.isEmpty()) {
            StringBuilder criteriaBuilder = new StringBuilder("Additional criteria to evaluate:\n");
            for (int i = 0; i < dynamicCriteria.size(); i++) {
                criteriaBuilder.append(String.format("%d. %s (0-%d)\n",
                        i + 5, dynamicCriteria.get(i), maxScore));
            }
            dynamicSection = criteriaBuilder.toString();
        }

        String prompt = judgePromptTemplate
                .replace("{task}", "Evaluate the answer quality")
                .replace("{answer}", answer)
                .replace("{expected}", expected)
                .replace("{max_score}", String.valueOf(maxScore))
                .replace("{dynamic_criteria_section}", dynamicSection);

        String response = judgeLLM.generate(prompt);
        return parseAndNormalizeScores(response, "quality_");
    }

    /**
     * 一致性检查：检查输出与参考答案之间的语义一致性
     *
     * @param prediction 预测输出
     * @param reference  参考答案
     * @return 一致性评估指标
     */
    private Map<String, Double> evaluateConsistency(String prediction, String reference) {
        String prompt = consistencyPromptTemplate
                .replace("{prediction}", prediction)
                .replace("{reference}", reference)
                .replace("{max_score}", String.valueOf(maxScore));

        String response = judgeLLM.generate(prompt);
        return parseAndNormalizeScores(response, "consistency_");
    }

    /**
     * 动态标准评估：基于用户自定义标准进行评估
     *
     * @param answer   模型输出
     * @param expected 期望答案
     * @return 动态标准评估指标
     */
    private Map<String, Double> evaluateDynamicCriteria(String answer, String expected) {
        StringBuilder criteriaList = new StringBuilder();
        for (int i = 0; i < dynamicCriteria.size(); i++) {
            criteriaList.append(String.format("%d. %s\n", i + 1, dynamicCriteria.get(i)));
        }

        String prompt = dynamicCriteriaPromptTemplate
                .replace("{answer}", answer)
                .replace("{expected}", expected)
                .replace("{criteria_list}", criteriaList.toString())
                .replace("{max_score}", String.valueOf(maxScore));

        String response = judgeLLM.generate(prompt);
        return parseAndNormalizeScores(response, "dynamic_");
    }

    /**
     * 解析 LLM 响应并归一化分数到 [0, 1] 范围
     *
     * @param response LLM 响应文本
     * @param prefix   指标名称前缀
     * @return 归一化后的指标映射
     */
    private Map<String, Double> parseAndNormalizeScores(String response, String prefix) {
        Map<String, Double> metrics = new LinkedHashMap<>();

        try {
            String jsonContent = extractJsonFromResponse(response);
            Map<String, Object> parsed = OBJECT_MAPPER.readValue(
                    jsonContent, new TypeReference<Map<String, Object>>() {});

            for (Map.Entry<String, Object> entry : parsed.entrySet()) {
                String key = entry.getKey();
                if ("reasoning".equals(key)) {
                    continue;
                }
                try {
                    double value = Double.parseDouble(entry.getValue().toString());
                    double normalizedValue = Math.min(1.0, Math.max(0.0, value / maxScore));
                    metrics.put(prefix + key, normalizedValue);
                } catch (NumberFormatException ignored) {
                    log.debug("Skipping non-numeric field '{}' in LLM response", key);
                }
            }
        } catch (EvaluationException e) {
            log.warn("Failed to parse LLM response as JSON, falling back to regex: {}", e.getMessage());
            metrics.putAll(parseWithRegex(response, prefix));
        } catch (Exception e) {
            log.warn("Failed to parse LLM response as JSON, falling back to regex: {}", e.getMessage());
            metrics.putAll(parseWithRegex(response, prefix));
        }

        return metrics;
    }

    /**
     * 从 LLM 响应中提取 JSON 内容
     *
     * @param response LLM 响应文本
     * @return 提取的 JSON 字符串
     */
    private String extractJsonFromResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return "{}";
        }

        String trimmed = response.trim();

        if (trimmed.startsWith("{")) {
            return trimmed;
        }

        Matcher jsonMatcher = JSON_BLOCK_PATTERN.matcher(trimmed);
        if (jsonMatcher.find()) {
            return jsonMatcher.group();
        }

        return trimmed;
    }

    /**
     * 使用正则表达式从响应中提取分数（JSON 解析失败时的回退方案）
     *
     * @param response LLM 响应文本
     * @param prefix   指标名称前缀
     * @return 提取的指标映射
     */
    private Map<String, Double> parseWithRegex(String response, String prefix) {
        Map<String, Double> metrics = new LinkedHashMap<>();

        Matcher matcher = SCORE_PATTERN.matcher(response);
        while (matcher.find()) {
            String key = matcher.group(1);
            if ("reasoning".equals(key)) {
                continue;
            }
            try {
                double value = Double.parseDouble(matcher.group(2));
                double normalizedValue = Math.min(1.0, Math.max(0.0, value / maxScore));
                metrics.put(prefix + key, normalizedValue);
            } catch (NumberFormatException ignored) {
                log.debug("Skipping non-numeric regex match for key '{}'", key);
            }
        }

        if (metrics.isEmpty()) {
            log.warn("Could not extract any scores from LLM response");
            metrics.put(prefix + "parse_error", 1.0);
        }

        return metrics;
    }

    /**
     * 添加动态评估标准
     *
     * @param criterion 评估标准描述
     */
    public void addDynamicCriterion(String criterion) {
        if (criterion != null && !criterion.trim().isEmpty()) {
            this.dynamicCriteria.add(criterion.trim());
            log.debug("Added dynamic criterion: {}", criterion);
        }
    }

    /**
     * 设置多个动态评估标准
     *
     * @param criteria 评估标准列表
     */
    public void setDynamicCriteria(List<String> criteria) {
        this.dynamicCriteria = criteria != null ? new ArrayList<>(criteria) : new ArrayList<>();
    }

    /**
     * 清除所有动态评估标准
     */
    public void clearDynamicCriteria() {
        this.dynamicCriteria.clear();
    }
}
