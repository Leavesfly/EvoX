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
 * GAOKAO-Bench 基准测试
 * 基于中国高考真题评估大语言模型能力
 * <p>
 * GAOKAO-Bench 收集了2010-2022年全国高考卷的客观题，
 * 涵盖语文、数学、英语、物理、化学、生物、政治、历史、地理等科目。
 * 共1781道客观题，是评估LLM中文理解与逻辑推理能力的重要基准。
 * </p>
 * <p>
 * 科目涵盖:
 * - 理科: 数学(理)、物理、化学、生物
 * - 文科: 数学(文)、政治、历史、地理
 * - 语言: 语文、英语
 * </p>
 * <p>
 * 数据格式(JSONL):
 * - question: 问题文本（含选项）
 * - choices: 选项列表 [A, B, C, D]
 * - answer: 正确答案 (A/B/C/D)
 * - subject: 科目名称
 * - year: 出题年份
 * - category: 试卷类别（如全国甲卷、新课标等）
 * - score: 分值
 * - analysis: 解题分析
 * </p>
 * <p>
 * 评估指标:
 * - accuracy: 整体准确率
 * - subject_accuracy: 按科目分组的准确率
 * </p>
 *
 * @author EvoX Team
 * @see <a href="https://github.com/OpenLMLab/GAOKAO-Bench">GAOKAO-Bench GitHub</a>
 * @see <a href="https://arxiv.org/abs/2305.12474">Evaluating the Performance of Large Language Models on GAOKAO Benchmark</a>
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class GaokaoBench extends Benchmark<GaokaoBench.GaokaoExample, String> {

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
            Pattern.compile("故选[：:]*\\s*([A-Da-d])"),
            Pattern.compile("ANSWER[:\\s]*([A-Da-d])", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\(([A-Da-d])\\)"),
            Pattern.compile("【([A-Da-d])】"),
            Pattern.compile("^\\s*([A-Da-d])\\s*[.、．）)\\s]"),
            Pattern.compile("([A-Da-d])\\s*[.、．]?\\s*$")
    );

    /**
     * 从题目文本中提取选项的正则模式
     * 匹配格式: A．xxx 或 A. xxx 或 A、xxx
     */
    private static final Pattern CHOICE_PATTERN = Pattern.compile(
            "([A-D])[．.、]\\s*(.+?)(?=\\s*[A-D][．.、]|\\s*$)", Pattern.DOTALL);

    public GaokaoBench(String path) {
        super("GAOKAO-Bench", path);
    }

    @Override
    protected void loadData() {
        log.info("Loading GAOKAO-Bench dataset from: {}", path);

        try {
            File dataFile = new File(path);
            if (!dataFile.exists()) {
                log.warn("GAOKAO-Bench data file not found: {}", path);
                this.trainData = new ArrayList<>();
                this.testData = new ArrayList<>();
                this.devData = new ArrayList<>();
                return;
            }

            List<String> lines = Files.readAllLines(Paths.get(path));
            List<GaokaoExample> allExamples = new ArrayList<>();

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) continue;

                try {
                    GaokaoExample example = parseLine(line, i);
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

            log.info("Loaded GAOKAO-Bench: {} train, {} dev, {} test examples across {} subjects",
                    trainData.size(), devData.size(), testData.size(), getSubjects().size());

        } catch (IOException e) {
            log.error("Error loading GAOKAO-Bench data", e);
            this.trainData = new ArrayList<>();
            this.testData = new ArrayList<>();
            this.devData = new ArrayList<>();
        }
    }

    /**
     * 解析单行JSONL数据
     */
    private GaokaoExample parseLine(String line, int index) throws Exception {
        GaokaoExample example = new GaokaoExample();

        JsonNode node = objectMapper.readTree(line);
        example.id = node.has("id") ? node.get("id").asText() : String.valueOf(index);
        example.year = node.has("year") ? node.get("year").asText() : "unknown";
        example.category = node.has("category") ? node.get("category").asText() : "unknown";
        example.subject = node.has("subject") ? node.get("subject").asText() : "unknown";
        example.score = node.has("score") ? node.get("score").asInt() : 0;
        example.analysis = node.has("analysis") ? node.get("analysis").asText() : "";

        // 解析题目和选项
        String rawQuestion = node.get("question").asText();
        parseQuestionAndChoices(example, rawQuestion, node);

        // 解析答案
        if (node.has("answer")) {
            JsonNode answerNode = node.get("answer");
            if (answerNode.isArray() && answerNode.size() > 0) {
                example.answer = answerNode.get(0).asText().trim().toUpperCase();
            } else {
                example.answer = answerNode.asText().trim().toUpperCase();
            }
        }

        return example;
    }

    /**
     * 从原始题目文本中解析出题目和选项
     * GAOKAO-Bench 的题目文本中选项嵌入在 question 字段里
     */
    private void parseQuestionAndChoices(GaokaoExample example, String rawQuestion, JsonNode node) {
        // 如果已有独立的 choices 字段
        if (node.has("choices")) {
            JsonNode choicesNode = node.get("choices");
            if (choicesNode.isArray() && choicesNode.size() > 0) {
                List<String> choices = new ArrayList<>();
                for (JsonNode choice : choicesNode) {
                    choices.add(choice.asText());
                }
                example.choices = choices;
                example.question = rawQuestion;
                return;
            }
        }

        // 从题目文本中提取选项
        // 先清理题目编号前缀，如 "1．（6分）"
        String cleaned = rawQuestion.replaceAll("^\\d+[．.]\\s*（\\s*\\d+分\\s*）\\s*", "");

        // 尝试按 \nA．... \nB．... 模式分割
        List<String> choices = new ArrayList<>();
        String questionText = cleaned;

        // 寻找选项开始位置
        Pattern optionStart = Pattern.compile("\\n\\s*A[．.、]");
        Matcher startMatcher = optionStart.matcher(cleaned);
        if (startMatcher.find()) {
            questionText = cleaned.substring(0, startMatcher.start()).trim();
            String optionsText = cleaned.substring(startMatcher.start());

            // 解析各选项
            Pattern optPat = Pattern.compile("([A-D])[．.、]\\s*(.*?)(?=\\s*\\n\\s*[A-D][．.、]|$)", Pattern.DOTALL);
            Matcher optMatcher = optPat.matcher(optionsText);
            while (optMatcher.find()) {
                choices.add(optMatcher.group(2).trim());
            }
        }

        // 如果没有提取到4个选项，尝试用空格分割
        if (choices.size() < 4) {
            choices.clear();
            Pattern altPat = Pattern.compile("([A-D])[．.、]([^A-D]*?)(?=\\s+[A-D][．.、]|\\s*$)");
            Matcher altMatcher = altPat.matcher(cleaned);
            String lastEnd = null;
            while (altMatcher.find()) {
                if (choices.isEmpty()) {
                    questionText = cleaned.substring(0, altMatcher.start()).trim();
                }
                choices.add(altMatcher.group(2).trim());
            }
        }

        // 如果仍然无法解析，保留原始文本
        if (choices.size() < 2) {
            choices = Arrays.asList("A", "B", "C", "D"); // 占位
            questionText = cleaned;
        }

        example.question = questionText;
        example.choices = choices;
    }

    @Override
    public String getId(GaokaoExample example) {
        return example.id;
    }

    @Override
    public String getLabel(GaokaoExample example) {
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
     * 按科目分组评估
     *
     * @param predictions 预测结果列表（与testData对应）
     * @return 每个科目的准确率
     */
    public Map<String, Double> evaluateBySubject(List<Object> predictions) {
        Map<String, List<Double>> subjectScores = new HashMap<>();

        List<GaokaoExample> data = getTestData();
        for (int i = 0; i < Math.min(predictions.size(), data.size()); i++) {
            GaokaoExample example = data.get(i);
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
     * 按年份分组评估
     *
     * @param predictions 预测结果列表（与testData对应）
     * @return 每个年份的准确率
     */
    public Map<String, Double> evaluateByYear(List<Object> predictions) {
        Map<String, List<Double>> yearScores = new HashMap<>();

        List<GaokaoExample> data = getTestData();
        for (int i = 0; i < Math.min(predictions.size(), data.size()); i++) {
            GaokaoExample example = data.get(i);
            Map<String, Double> result = evaluate(predictions.get(i), example.answer);
            double score = result.getOrDefault("accuracy", 0.0);
            yearScores.computeIfAbsent(example.year, k -> new ArrayList<>()).add(score);
        }

        Map<String, Double> yearAccuracy = new LinkedHashMap<>();
        yearScores.forEach((year, scores) -> {
            double avg = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            yearAccuracy.put(year, avg);
        });

        return yearAccuracy;
    }

    /**
     * 获取所有科目列表
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
     * 针对中文LLM输出优化，支持多种中英文格式
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
    public String formatPrompt(GaokaoExample example, List<GaokaoExample> fewShots) {
        StringBuilder sb = new StringBuilder();
        sb.append("以下是一道高考").append(example.subject).append("选择题，请直接给出正确答案的选项。\n\n");

        // Few-shot 示例
        if (fewShots != null) {
            for (GaokaoExample shot : fewShots) {
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
    private String formatQuestion(GaokaoExample example) {
        StringBuilder sb = new StringBuilder();
        sb.append("题目：").append(example.question).append("\n");
        for (int i = 0; i < example.choices.size() && i < VALID_CHOICES.size(); i++) {
            sb.append(VALID_CHOICES.get(i)).append(". ").append(example.choices.get(i)).append("\n");
        }
        return sb.toString();
    }

    /**
     * GAOKAO-Bench 样本数据结构
     */
    @Data
    public static class GaokaoExample {
        private String id;
        private String question;
        private List<String> choices;
        private String answer;
        private String subject;
        private String year;
        private String category;
        private int score;
        private String analysis;
    }
}
