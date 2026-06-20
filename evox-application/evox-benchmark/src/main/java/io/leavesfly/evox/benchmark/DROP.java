package io.leavesfly.evox.benchmark;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * DROP基准测试
 * Discrete Reasoning Over Paragraphs - 段落离散推理
 * <p>
 * 需要在阅读段落后进行数值推理（加减、计数、排序、比较等）的阅读理解任务。
 * 共约96,000+样本，是评估LLM数值推理和阅读理解综合能力的重要基准。
 * </p>
 * <p>
 * 数据格式(JSONL/JSON)：
 * - passage: 段落文本
 * - question: 问题
 * - answers_spans: 答案文本列表（可能有多个合法答案）
 * - answer_type: 答案类型（number/span/date）
 * </p>
 * <p>
 * 评估指标：
 * - exact_match: 精确匹配率
 * - f1_score: Token级别F1分数
 * - number_accuracy: 数值答案的准确率
 * </p>
 *
 * @author EvoX Team
 * @see <a href="https://arxiv.org/abs/1903.00161">DROP: A Reading Comprehension Benchmark Requiring Discrete Reasoning Over Paragraphs</a>
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class DROP extends Benchmark<DROP.DROPExample, List<String>> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 数值提取正则表达式
     */
    private static final Pattern NUMBER_PATTERN = Pattern.compile("-?\\d+\\.?\\d*");

    public DROP(String path) {
        super("DROP", path);
    }

    @Override
    protected void loadData() {
        log.info("Loading DROP dataset from: {}", path);

        try {
            File dataFile = new File(path);
            if (!dataFile.exists()) {
                log.warn("DROP data file not found: {}", path);
                this.trainData = new ArrayList<>();
                this.testData = new ArrayList<>();
                this.devData = new ArrayList<>();
                return;
            }

            String content = new String(Files.readAllBytes(Paths.get(path)));
            List<DROPExample> allExamples = new ArrayList<>();

            // 尝试按JSON对象解析（DROP官方格式是按passage分组的大JSON）
            if (content.trim().startsWith("{") && !content.trim().startsWith("{\"")) {
                allExamples = parseGroupedJson(content);
            } else {
                // JSONL 格式：每行一个样本
                List<String> lines = Files.readAllLines(Paths.get(path));
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i).trim();
                    if (line.isEmpty()) continue;

                    try {
                        DROPExample example = parseJsonLine(line, i);
                        if (example != null) {
                            allExamples.add(example);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to parse line {}: {}", i, e.getMessage());
                    }
                }
            }

            // 按 8:1:1 分割 train/dev/test
            int trainEnd = (int) (allExamples.size() * 0.8);
            int devEnd = (int) (allExamples.size() * 0.9);
            this.trainData = new ArrayList<>(allExamples.subList(0, trainEnd));
            this.devData = new ArrayList<>(allExamples.subList(trainEnd, devEnd));
            this.testData = new ArrayList<>(allExamples.subList(devEnd, allExamples.size()));

            log.info("Loaded DROP: {} train, {} dev, {} test examples",
                    trainData.size(), devData.size(), testData.size());

        } catch (IOException e) {
            log.error("Error loading DROP data", e);
            this.trainData = new ArrayList<>();
            this.testData = new ArrayList<>();
            this.devData = new ArrayList<>();
        }
    }

    /**
     * 解析DROP官方的分组JSON格式
     * 格式: { "passage_id": { "passage": "...", "qa_pairs": [...] } }
     */
    private List<DROPExample> parseGroupedJson(String content) throws IOException {
        List<DROPExample> examples = new ArrayList<>();
        JsonNode root = objectMapper.readTree(content);

        Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String passageId = entry.getKey();
            JsonNode passageNode = entry.getValue();

            String passage = passageNode.get("passage").asText();
            JsonNode qaPairs = passageNode.get("qa_pairs");

            if (qaPairs != null && qaPairs.isArray()) {
                for (JsonNode qa : qaPairs) {
                    DROPExample example = new DROPExample();
                    example.id = qa.has("query_id") ? qa.get("query_id").asText() : passageId;
                    example.passage = passage;
                    example.question = qa.get("question").asText();
                    example.answerType = qa.has("answer_type") ? qa.get("answer_type").asText() : "span";
                    example.answers = extractAnswers(qa.get("answer"));
                    examples.add(example);
                }
            }
        }

        return examples;
    }

    /**
     * 解析JSONL格式的单行
     */
    private DROPExample parseJsonLine(String line, int index) throws Exception {
        JsonNode node = objectMapper.readTree(line);
        DROPExample example = new DROPExample();

        example.id = node.has("query_id") ? node.get("query_id").asText() : String.valueOf(index);
        example.passage = node.get("passage").asText();
        example.question = node.get("question").asText();
        example.answerType = node.has("answer_type") ? node.get("answer_type").asText() : "span";

        // 解析答案
        if (node.has("answers_spans")) {
            JsonNode answersNode = node.get("answers_spans");
            List<String> answers = new ArrayList<>();
            if (answersNode.isArray()) {
                for (JsonNode ans : answersNode) {
                    answers.add(ans.asText());
                }
            }
            example.answers = answers;
        } else if (node.has("answer")) {
            example.answers = extractAnswers(node.get("answer"));
        } else {
            example.answers = Collections.emptyList();
        }

        return example;
    }

    /**
     * 从answer节点中提取所有合法答案
     * DROP的answer结构: {"number": "3", "date": {...}, "spans": ["text1", "text2"]}
     */
    private List<String> extractAnswers(JsonNode answerNode) {
        List<String> answers = new ArrayList<>();
        if (answerNode == null) {
            return answers;
        }

        // 数值答案
        if (answerNode.has("number") && !answerNode.get("number").asText().isEmpty()) {
            answers.add(answerNode.get("number").asText());
        }

        // 文本span答案
        if (answerNode.has("spans")) {
            JsonNode spans = answerNode.get("spans");
            if (spans.isArray()) {
                for (JsonNode span : spans) {
                    String text = span.asText().trim();
                    if (!text.isEmpty()) {
                        answers.add(text);
                    }
                }
            }
        }

        // 日期答案
        if (answerNode.has("date")) {
            JsonNode dateNode = answerNode.get("date");
            String day = dateNode.has("day") ? dateNode.get("day").asText() : "";
            String month = dateNode.has("month") ? dateNode.get("month").asText() : "";
            String year = dateNode.has("year") ? dateNode.get("year").asText() : "";
            String dateStr = String.join(" ", Arrays.asList(month, day, year)).trim();
            if (!dateStr.isEmpty()) {
                answers.add(dateStr);
            }
        }

        return answers;
    }

    @Override
    public String getId(DROPExample example) {
        return example.id;
    }

    @Override
    public List<String> getLabel(DROPExample example) {
        return example.answers;
    }

    @Override
    public Map<String, Double> evaluate(Object prediction, List<String> labels) {
        Map<String, Double> metrics = new HashMap<>();

        if (labels == null || labels.isEmpty()) {
            metrics.put("exact_match", 0.0);
            metrics.put("f1_score", 0.0);
            return metrics;
        }

        String predText = normalize(prediction.toString());

        // 对每个合法答案计算指标，取最高分
        double bestEM = 0.0;
        double bestF1 = 0.0;

        for (String label : labels) {
            String labelText = normalize(label);

            // Exact Match
            double em = predText.equals(labelText) ? 1.0 : 0.0;
            bestEM = Math.max(bestEM, em);

            // F1 Score
            double f1 = calculateF1(predText, labelText);
            bestF1 = Math.max(bestF1, f1);

            // 数值相等判定（容忍格式差异）
            if (em == 0.0 && isNumericMatch(predText, labelText)) {
                bestEM = 1.0;
            }
        }

        metrics.put("exact_match", bestEM);
        metrics.put("f1_score", bestF1);

        return metrics;
    }

    /**
     * 文本标准化
     */
    private String normalize(String text) {
        if (text == null) return "";
        // 转小写、去除冠词和标点
        String normalized = text.toLowerCase().trim();
        normalized = normalized.replaceAll("\\b(a|an|the)\\b", " ");
        normalized = normalized.replaceAll("[^a-z0-9\\s.-]", " ");
        normalized = normalized.replaceAll("\\s+", " ").trim();
        return normalized;
    }

    /**
     * 计算Token级别F1分数
     */
    private double calculateF1(String prediction, String label) {
        Set<String> predTokens = new HashSet<>(Arrays.asList(prediction.split("\\s+")));
        Set<String> labelTokens = new HashSet<>(Arrays.asList(label.split("\\s+")));

        // 移除空token
        predTokens.remove("");
        labelTokens.remove("");

        if (predTokens.isEmpty() || labelTokens.isEmpty()) {
            return predTokens.equals(labelTokens) ? 1.0 : 0.0;
        }

        Set<String> common = new HashSet<>(predTokens);
        common.retainAll(labelTokens);

        if (common.isEmpty()) {
            return 0.0;
        }

        double precision = (double) common.size() / predTokens.size();
        double recall = (double) common.size() / labelTokens.size();

        return 2 * precision * recall / (precision + recall);
    }

    /**
     * 数值匹配：判断两个字符串表示的数值是否相等
     * 处理 "3" vs "3.0"、"1,000" vs "1000" 等情况
     */
    private boolean isNumericMatch(String pred, String label) {
        try {
            String predNum = extractNumber(pred);
            String labelNum = extractNumber(label);

            if (predNum == null || labelNum == null) {
                return false;
            }

            double predValue = Double.parseDouble(predNum);
            double labelValue = Double.parseDouble(labelNum);

            // 允许极小的浮点误差
            return Math.abs(predValue - labelValue) < 1e-6;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 从文本中提取数值
     */
    private String extractNumber(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        // 移除逗号（千分位分隔符）
        String cleaned = text.replaceAll(",", "");
        Matcher matcher = NUMBER_PATTERN.matcher(cleaned);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    /**
     * 格式化为prompt
     *
     * @param example 待回答的样本
     * @return 格式化的prompt
     */
    public String formatPrompt(DROPExample example) {
        return String.format(
                "Read the following passage and answer the question.\n\n" +
                "Passage: %s\n\n" +
                "Question: %s\n\n" +
                "Answer:",
                example.passage, example.question
        );
    }

    /**
     * DROP样本数据结构
     */
    @Data
    public static class DROPExample {
        private String id;
        private String passage;
        private String question;
        private List<String> answers;
        private String answerType;
    }
}
