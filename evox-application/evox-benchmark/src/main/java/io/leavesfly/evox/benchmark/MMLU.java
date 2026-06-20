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
import java.util.stream.Collectors;

/**
 * MMLU基准测试
 * Massive Multitask Language Understanding - 大规模多任务语言理解
 * <p>
 * 涵盖57个学科（STEM、人文、社科、其他），共约15,000+多选题。
 * 被广泛认为是衡量LLM综合知识与推理能力的核心基准之一。
 * </p>
 * <p>
 * 数据格式(JSONL/CSV)：
 * - question: 问题文本
 * - choices: 选项列表 [A, B, C, D]
 * - answer: 正确答案索引(0-3)或字母(A-D)
 * - subject: 学科名称
 * </p>
 * <p>
 * 评估指标：
 * - accuracy: 整体准确率
 * - subject_accuracy: 按学科分组的准确率
 * </p>
 *
 * @author EvoX Team
 * @see <a href="https://arxiv.org/abs/2009.03300">Measuring Massive Multitask Language Understanding</a>
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class MMLU extends Benchmark<MMLU.MMLUExample, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 有效选项字母
     */
    private static final List<String> VALID_CHOICES = Arrays.asList("A", "B", "C", "D");

    public MMLU(String path) {
        super("MMLU", path);
    }

    @Override
    protected void loadData() {
        log.info("Loading MMLU dataset from: {}", path);

        try {
            File dataFile = new File(path);
            if (!dataFile.exists()) {
                log.warn("MMLU data file not found: {}", path);
                this.trainData = new ArrayList<>();
                this.testData = new ArrayList<>();
                this.devData = new ArrayList<>();
                return;
            }

            List<String> lines = Files.readAllLines(Paths.get(path));
            List<MMLUExample> allExamples = new ArrayList<>();

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) continue;

                try {
                    MMLUExample example = parseLine(line, i);
                    if (example != null) {
                        allExamples.add(example);
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse line {}: {}", i, e.getMessage());
                }
            }

            // 按 7:1:2 分割 train/dev/test
            int trainEnd = (int) (allExamples.size() * 0.7);
            int devEnd = (int) (allExamples.size() * 0.8);
            this.trainData = new ArrayList<>(allExamples.subList(0, trainEnd));
            this.devData = new ArrayList<>(allExamples.subList(trainEnd, devEnd));
            this.testData = new ArrayList<>(allExamples.subList(devEnd, allExamples.size()));

            log.info("Loaded MMLU: {} train, {} dev, {} test examples across {} subjects",
                    trainData.size(), devData.size(), testData.size(), getSubjects().size());

        } catch (IOException e) {
            log.error("Error loading MMLU data", e);
            this.trainData = new ArrayList<>();
            this.testData = new ArrayList<>();
            this.devData = new ArrayList<>();
        }
    }

    /**
     * 解析单行数据，支持JSONL和CSV两种格式
     */
    private MMLUExample parseLine(String line, int index) throws Exception {
        MMLUExample example = new MMLUExample();

        if (line.startsWith("{")) {
            // JSONL格式
            JsonNode node = objectMapper.readTree(line);
            example.id = node.has("id") ? node.get("id").asText() : String.valueOf(index);
            example.question = node.get("question").asText();
            example.subject = node.has("subject") ? node.get("subject").asText() : "unknown";

            // 解析选项
            List<String> choices = new ArrayList<>();
            if (node.has("choices")) {
                JsonNode choicesNode = node.get("choices");
                if (choicesNode.isArray()) {
                    for (JsonNode choice : choicesNode) {
                        choices.add(choice.asText());
                    }
                }
            }
            example.choices = choices;

            // 解析答案 - 支持索引(0-3)和字母(A-D)两种格式
            String answerRaw = node.get("answer").asText();
            example.answer = normalizeAnswer(answerRaw);
        } else {
            // CSV格式: question,A,B,C,D,answer[,subject]
            String[] parts = splitCsvLine(line);
            if (parts.length < 6) {
                return null;
            }
            example.id = String.valueOf(index);
            example.question = parts[0];
            example.choices = Arrays.asList(parts[1], parts[2], parts[3], parts[4]);
            example.answer = normalizeAnswer(parts[5].trim());
            example.subject = parts.length > 6 ? parts[6].trim() : "unknown";
        }

        return example;
    }

    /**
     * 将答案标准化为A/B/C/D格式
     */
    private String normalizeAnswer(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        String upper = raw.trim().toUpperCase();
        // 已经是字母格式
        if (VALID_CHOICES.contains(upper)) {
            return upper;
        }
        // 数字索引格式 (0->A, 1->B, 2->C, 3->D)
        try {
            int index = Integer.parseInt(raw.trim());
            if (index >= 0 && index < VALID_CHOICES.size()) {
                return VALID_CHOICES.get(index);
            }
        } catch (NumberFormatException ignored) {
        }
        return upper;
    }

    /**
     * 简单CSV行分割（处理引号内的逗号）
     */
    private String[] splitCsvLine(String line) {
        List<String> parts = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();

        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                parts.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        parts.add(current.toString());

        return parts.toArray(new String[0]);
    }

    @Override
    public String getId(MMLUExample example) {
        return example.id;
    }

    @Override
    public String getLabel(MMLUExample example) {
        return example.answer;
    }

    @Override
    public Map<String, Double> evaluate(Object prediction, String label) {
        Map<String, Double> metrics = new HashMap<>();

        String predAnswer = extractChoice(prediction.toString());
        String correctAnswer = label.trim().toUpperCase();

        double accuracy = predAnswer.equals(correctAnswer) ? 1.0 : 0.0;
        metrics.put("accuracy", accuracy);

        return metrics;
    }

    /**
     * 按学科分组评估
     *
     * @param predictions 预测结果列表（与testData对应）
     * @return 每个学科的准确率
     */
    public Map<String, Double> evaluateBySubject(List<Object> predictions) {
        Map<String, List<Double>> subjectScores = new HashMap<>();

        List<MMLUExample> data = getTestData();
        for (int i = 0; i < Math.min(predictions.size(), data.size()); i++) {
            MMLUExample example = data.get(i);
            Map<String, Double> result = evaluate(predictions.get(i), example.answer);
            double score = result.getOrDefault("accuracy", 0.0);

            subjectScores.computeIfAbsent(example.subject, k -> new ArrayList<>()).add(score);
        }

        // 计算每个学科的平均准确率
        Map<String, Double> subjectAccuracy = new LinkedHashMap<>();
        subjectScores.forEach((subject, scores) -> {
            double avg = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            subjectAccuracy.put(subject, avg);
        });

        return subjectAccuracy;
    }

    /**
     * 获取所有学科列表
     */
    public Set<String> getSubjects() {
        Set<String> subjects = new HashSet<>();
        if (trainData != null) trainData.forEach(e -> subjects.add(e.subject));
        if (devData != null) devData.forEach(e -> subjects.add(e.subject));
        if (testData != null) testData.forEach(e -> subjects.add(e.subject));
        return subjects;
    }

    /**
     * 从LLM响应中提取选项字母
     * 支持多种格式：直接字母、"Answer: A"、"(A)"、"选A"等
     */
    private String extractChoice(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        String trimmed = text.trim().toUpperCase();

        // 直接是单个字母
        if (trimmed.length() == 1 && VALID_CHOICES.contains(trimmed)) {
            return trimmed;
        }

        // 匹配 "Answer: X" 或 "The answer is X" 格式
        for (String pattern : Arrays.asList(
                "ANSWER:\\s*([A-D])",
                "ANSWER IS\\s*([A-D])",
                "\\(([A-D])\\)",
                "选([A-D])",
                "^([A-D])[.\\)\\s]")) {
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(pattern).matcher(trimmed);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }

        // 兜底：找到第一个出现的A/B/C/D
        for (char c : trimmed.toCharArray()) {
            String s = String.valueOf(c);
            if (VALID_CHOICES.contains(s)) {
                return s;
            }
        }

        return trimmed.length() > 0 ? trimmed.substring(0, 1) : "";
    }

    /**
     * 格式化为few-shot prompt
     *
     * @param example    待回答的样本
     * @param fewShots   few-shot示例
     * @return 格式化的prompt
     */
    public String formatPrompt(MMLUExample example, List<MMLUExample> fewShots) {
        StringBuilder sb = new StringBuilder();
        sb.append("The following are multiple choice questions about ").append(example.subject).append(".\n\n");

        // Few-shot 示例
        if (fewShots != null) {
            for (MMLUExample shot : fewShots) {
                sb.append(formatQuestion(shot));
                sb.append("Answer: ").append(shot.answer).append("\n\n");
            }
        }

        // 目标问题
        sb.append(formatQuestion(example));
        sb.append("Answer:");

        return sb.toString();
    }

    /**
     * 格式化单个问题
     */
    private String formatQuestion(MMLUExample example) {
        StringBuilder sb = new StringBuilder();
        sb.append(example.question).append("\n");
        for (int i = 0; i < example.choices.size(); i++) {
            sb.append(VALID_CHOICES.get(i)).append(". ").append(example.choices.get(i)).append("\n");
        }
        return sb.toString();
    }

    /**
     * MMLU样本数据结构
     */
    @Data
    public static class MMLUExample {
        private String id;
        private String question;
        private List<String> choices;
        private String answer;
        private String subject;
    }
}
