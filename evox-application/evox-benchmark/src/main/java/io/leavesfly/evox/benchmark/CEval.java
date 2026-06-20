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

/**
 * C-Eval 基准测试
 * 全面的中文基础模型评估套件
 * <p>
 * C-Eval 由港科大和清华联合推出，包含13,948个多项选择题，
 * 涵盖52个不同学科和4个难度级别（初中、高中、大学、专业）。
 * 是中文LLM评测中最核心、引用最广的基准之一。
 * </p>
 * <p>
 * 学科涵盖:
 * - STEM: 数学、物理、化学、生物、计算机科学等
 * - 人文社科: 中国历史、地理、政治、法律等
 * - 中国特色: 公务员行测、税务师、注册会计师、教师资格等
 * </p>
 * <p>
 * 数据格式(JSONL/CSV):
 * - question: 问题文本（中文）
 * - choices / A,B,C,D: 四个选项
 * - answer: 正确答案 (A/B/C/D)
 * - subject: 学科名称
 * - difficulty: 难度级别 (middle_school/high_school/college/professional)
 * </p>
 * <p>
 * 评估指标:
 * - accuracy: 整体准确率
 * - subject_accuracy: 按学科分组的准确率
 * - difficulty_accuracy: 按难度级别分组的准确率
 * </p>
 *
 * @author EvoX Team
 * @see <a href="https://cevalbenchmark.com/">C-Eval Official Website</a>
 * @see <a href="https://arxiv.org/abs/2305.08322">C-Eval: A Multi-Level Multi-Discipline Chinese Evaluation Suite</a>
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class CEval extends Benchmark<CEval.CEvalExample, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 有效选项字母
     */
    private static final List<String> VALID_CHOICES = Arrays.asList("A", "B", "C", "D");

    /**
     * 中文答案提取正则模式
     */
    private static final List<Pattern> ANSWER_PATTERNS = Arrays.asList(
            Pattern.compile("答案[是为：:]*\\s*([A-Da-d])"),
            Pattern.compile("选[择项]?[是为：:]*\\s*([A-Da-d])"),
            Pattern.compile("ANSWER[:\\s]*([A-Da-d])", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\(([A-Da-d])\\)"),
            Pattern.compile("【([A-Da-d])】"),
            Pattern.compile("^\\s*([A-Da-d])\\s*[.、）)\\s]"),
            Pattern.compile("([A-Da-d])\\s*[.、]?\\s*$")
    );

    /**
     * 难度级别
     */
    public static final String DIFFICULTY_MIDDLE_SCHOOL = "middle_school";
    public static final String DIFFICULTY_HIGH_SCHOOL = "high_school";
    public static final String DIFFICULTY_COLLEGE = "college";
    public static final String DIFFICULTY_PROFESSIONAL = "professional";

    public CEval(String path) {
        super("C-Eval", path);
    }

    @Override
    protected void loadData() {
        log.info("Loading C-Eval dataset from: {}", path);

        try {
            File dataFile = new File(path);
            if (!dataFile.exists()) {
                log.warn("C-Eval data file not found: {}", path);
                this.trainData = new ArrayList<>();
                this.testData = new ArrayList<>();
                this.devData = new ArrayList<>();
                return;
            }

            List<String> lines = Files.readAllLines(Paths.get(path));
            List<CEvalExample> allExamples = new ArrayList<>();

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) continue;

                try {
                    CEvalExample example = parseLine(line, i);
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

            log.info("Loaded C-Eval: {} train, {} dev, {} test examples across {} subjects",
                    trainData.size(), devData.size(), testData.size(), getSubjects().size());

        } catch (IOException e) {
            log.error("Error loading C-Eval data", e);
            this.trainData = new ArrayList<>();
            this.testData = new ArrayList<>();
            this.devData = new ArrayList<>();
        }
    }

    /**
     * 解析单行数据，支持JSONL和CSV两种格式
     */
    private CEvalExample parseLine(String line, int index) throws Exception {
        CEvalExample example = new CEvalExample();

        if (line.startsWith("{")) {
            // JSONL格式
            JsonNode node = objectMapper.readTree(line);
            example.id = node.has("id") ? node.get("id").asText() : String.valueOf(index);
            example.question = node.get("question").asText();
            example.subject = node.has("subject") ? node.get("subject").asText() : "unknown";
            example.difficulty = node.has("difficulty") ? node.get("difficulty").asText() : "unknown";

            // 解析选项 - 支持 choices 数组和 A/B/C/D 字段两种格式
            List<String> choices = new ArrayList<>();
            if (node.has("choices")) {
                JsonNode choicesNode = node.get("choices");
                if (choicesNode.isArray()) {
                    for (JsonNode choice : choicesNode) {
                        choices.add(choice.asText());
                    }
                }
            } else {
                // C-Eval 常见格式：独立的 A/B/C/D 字段
                for (String key : VALID_CHOICES) {
                    if (node.has(key)) {
                        choices.add(node.get(key).asText());
                    }
                }
            }
            example.choices = choices;

            // 解析答案
            String answerRaw = node.get("answer").asText();
            example.answer = normalizeAnswer(answerRaw);

            // 解析解析（如果有）
            if (node.has("explanation")) {
                example.explanation = node.get("explanation").asText();
            }
        } else {
            // CSV格式: question,A,B,C,D,answer[,subject[,difficulty]]
            String[] parts = splitCsvLine(line);
            if (parts.length < 6) {
                return null;
            }
            example.id = String.valueOf(index);
            example.question = parts[0];
            example.choices = Arrays.asList(parts[1], parts[2], parts[3], parts[4]);
            example.answer = normalizeAnswer(parts[5].trim());
            example.subject = parts.length > 6 ? parts[6].trim() : "unknown";
            example.difficulty = parts.length > 7 ? parts[7].trim() : "unknown";
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
        if (VALID_CHOICES.contains(upper)) {
            return upper;
        }
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
    public String getId(CEvalExample example) {
        return example.id;
    }

    @Override
    public String getLabel(CEvalExample example) {
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

        List<CEvalExample> data = getTestData();
        for (int i = 0; i < Math.min(predictions.size(), data.size()); i++) {
            CEvalExample example = data.get(i);
            Map<String, Double> result = evaluate(predictions.get(i), example.answer);
            double score = result.getOrDefault("accuracy", 0.0);
            subjectScores.computeIfAbsent(example.subject, k -> new ArrayList<>()).add(score);
        }

        Map<String, Double> subjectAccuracy = new LinkedHashMap<>();
        subjectScores.forEach((subject, scores) -> {
            double avg = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            subjectAccuracy.put(subject, avg);
        });

        return subjectAccuracy;
    }

    /**
     * 按难度级别分组评估
     *
     * @param predictions 预测结果列表（与testData对应）
     * @return 每个难度级别的准确率
     */
    public Map<String, Double> evaluateByDifficulty(List<Object> predictions) {
        Map<String, List<Double>> difficultyScores = new HashMap<>();

        List<CEvalExample> data = getTestData();
        for (int i = 0; i < Math.min(predictions.size(), data.size()); i++) {
            CEvalExample example = data.get(i);
            Map<String, Double> result = evaluate(predictions.get(i), example.answer);
            double score = result.getOrDefault("accuracy", 0.0);
            difficultyScores.computeIfAbsent(example.difficulty, k -> new ArrayList<>()).add(score);
        }

        Map<String, Double> difficultyAccuracy = new LinkedHashMap<>();
        difficultyScores.forEach((difficulty, scores) -> {
            double avg = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            difficultyAccuracy.put(difficulty, avg);
        });

        return difficultyAccuracy;
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
     * 针对中文LLM输出优化，支持多种中英文格式：
     * "A"、"答案是A"、"选A"、"(A)"、"【A】"等
     */
    private String extractChoice(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        String trimmed = text.trim();

        // 直接是单个字母
        String upper = trimmed.toUpperCase();
        if (upper.length() == 1 && VALID_CHOICES.contains(upper)) {
            return upper;
        }

        // 按优先级尝试各种模式
        for (Pattern pattern : ANSWER_PATTERNS) {
            Matcher matcher = pattern.matcher(trimmed);
            if (matcher.find()) {
                return matcher.group(1).toUpperCase();
            }
        }

        // 兜底：找到第一个出现的A/B/C/D
        String upperText = trimmed.toUpperCase();
        for (char c : upperText.toCharArray()) {
            String s = String.valueOf(c);
            if (VALID_CHOICES.contains(s)) {
                return s;
            }
        }

        return "";
    }

    /**
     * 格式化为few-shot prompt（中文）
     *
     * @param example  待回答的样本
     * @param fewShots few-shot示例列表
     * @return 格式化的中文prompt
     */
    public String formatPrompt(CEvalExample example, List<CEvalExample> fewShots) {
        StringBuilder sb = new StringBuilder();
        sb.append("以下是关于").append(example.subject).append("的单项选择题，请直接给出正确答案的选项。\n\n");

        // Few-shot 示例
        if (fewShots != null) {
            for (CEvalExample shot : fewShots) {
                sb.append(formatQuestion(shot));
                sb.append("答案：").append(shot.answer).append("\n\n");
            }
        }

        // 目标问题
        sb.append(formatQuestion(example));
        sb.append("答案：");

        return sb.toString();
    }

    /**
     * 格式化单个问题
     */
    private String formatQuestion(CEvalExample example) {
        StringBuilder sb = new StringBuilder();
        sb.append("题目：").append(example.question).append("\n");
        for (int i = 0; i < example.choices.size(); i++) {
            sb.append(VALID_CHOICES.get(i)).append(". ").append(example.choices.get(i)).append("\n");
        }
        return sb.toString();
    }

    /**
     * C-Eval 样本数据结构
     */
    @Data
    public static class CEvalExample {
        private String id;
        private String question;
        private List<String> choices;
        private String answer;
        private String subject;
        private String difficulty;
        private String explanation;
    }
}
