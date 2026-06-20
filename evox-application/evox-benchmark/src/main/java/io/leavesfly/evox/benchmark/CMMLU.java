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
 * CMMLU 基准测试
 * Chinese Massive Multitask Language Understanding - 中文大规模多任务语言理解
 * <p>
 * CMMLU 是专门针对中文语境设计的综合性评估基准，涵盖67个主题，
 * 从基础学科到高级专业水平全面评估LLM在中文环境下的知识和推理能力。
 * </p>
 * <p>
 * 与 C-Eval 的区别：
 * - CMMLU 更加侧重中国文化特有知识（中医、古文、书法、饮食文化等）
 * - 包含更多需要计算和推理的自然科学题目
 * - 涵盖中国特色社会知识（中国法律、驾照考试等）
 * </p>
 * <p>
 * 主题分类:
 * - 自然科学: 数学、物理、化学、生物
 * - 社会科学: 经济学、管理学、心理学、教育学
 * - 人文学科: 中国历史、世界历史、中国文学、哲学
 * - 中国特色: 中国传统文化、中国地理、中医基础、食品科学
 * - 专业考试: 注册会计师、法律职业资格、教师资格、驾照考试
 * </p>
 * <p>
 * 数据格式(JSONL/CSV):
 * - question: 问题文本（中文）
 * - choices / A,B,C,D: 四个选项
 * - answer: 正确答案 (A/B/C/D)
 * - subject: 主题/学科名称
 * - subcategory: 子分类（可选）
 * </p>
 *
 * @author EvoX Team
 * @see <a href="https://github.com/haonan-li/CMMLU">CMMLU GitHub</a>
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class CMMLU extends Benchmark<CMMLU.CMMUExample, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 有效选项字母
     */
    private static final List<String> VALID_CHOICES = Arrays.asList("A", "B", "C", "D");

    /**
     * 中文答案提取正则模式（与CEval共享类似逻辑但独立定义以便各自扩展）
     */
    private static final List<Pattern> ANSWER_PATTERNS = Arrays.asList(
            Pattern.compile("答案[是为：:]*\\s*([A-Da-d])"),
            Pattern.compile("选[择项]?[是为：:]*\\s*([A-Da-d])"),
            Pattern.compile("正确[的答案选项]*[是为：:]*\\s*([A-Da-d])"),
            Pattern.compile("ANSWER[:\\s]*([A-Da-d])", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\(([A-Da-d])\\)"),
            Pattern.compile("【([A-Da-d])】"),
            Pattern.compile("^\\s*([A-Da-d])\\s*[.、）)\\s]"),
            Pattern.compile("([A-Da-d])\\s*[.、]?\\s*$")
    );

    /**
     * CMMLU 主题大类
     */
    public static final String CATEGORY_STEM = "STEM";
    public static final String CATEGORY_SOCIAL_SCIENCE = "Social Science";
    public static final String CATEGORY_HUMANITIES = "Humanities";
    public static final String CATEGORY_CHINA_SPECIFIC = "China Specific";
    public static final String CATEGORY_OTHER = "Other";

    /**
     * 主题到大类的映射
     */
    private Map<String, String> subjectCategoryMap;

    public CMMLU(String path) {
        super("CMMLU", path);
        initSubjectCategoryMap();
    }

    /**
     * 初始化主题到大类的映射
     */
    private void initSubjectCategoryMap() {
        subjectCategoryMap = new HashMap<>();

        // STEM
        for (String s : Arrays.asList("数学", "物理", "化学", "生物", "计算机科学",
                "统计学", "天文学", "电气工程", "机械工程", "mathematics",
                "physics", "chemistry", "biology", "computer_science")) {
            subjectCategoryMap.put(s, CATEGORY_STEM);
        }

        // 社会科学
        for (String s : Arrays.asList("经济学", "管理学", "心理学", "教育学", "社会学",
                "政治学", "法学", "economics", "management", "psychology",
                "education", "sociology", "law")) {
            subjectCategoryMap.put(s, CATEGORY_SOCIAL_SCIENCE);
        }

        // 人文学科
        for (String s : Arrays.asList("中国历史", "世界历史", "中国文学", "外国文学",
                "哲学", "逻辑学", "艺术", "music", "philosophy", "history",
                "chinese_literature", "foreign_literature")) {
            subjectCategoryMap.put(s, CATEGORY_HUMANITIES);
        }

        // 中国特色
        for (String s : Arrays.asList("中国传统文化", "中国地理", "中医基础", "中国食文化",
                "中国法律", "驾照考试", "公务员考试", "中国古代文学",
                "chinese_traditional_culture", "chinese_geography",
                "traditional_chinese_medicine", "chinese_food_culture",
                "chinese_driving_rule", "chinese_civil_service_exam")) {
            subjectCategoryMap.put(s, CATEGORY_CHINA_SPECIFIC);
        }
    }

    @Override
    protected void loadData() {
        log.info("Loading CMMLU dataset from: {}", path);

        try {
            File dataFile = new File(path);
            if (!dataFile.exists()) {
                log.warn("CMMLU data file not found: {}", path);
                this.trainData = new ArrayList<>();
                this.testData = new ArrayList<>();
                this.devData = new ArrayList<>();
                return;
            }

            List<String> lines = Files.readAllLines(Paths.get(path));
            List<CMMUExample> allExamples = new ArrayList<>();

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) continue;

                try {
                    CMMUExample example = parseLine(line, i);
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

            log.info("Loaded CMMLU: {} train, {} dev, {} test examples across {} subjects",
                    trainData.size(), devData.size(), testData.size(), getSubjects().size());

        } catch (IOException e) {
            log.error("Error loading CMMLU data", e);
            this.trainData = new ArrayList<>();
            this.testData = new ArrayList<>();
            this.devData = new ArrayList<>();
        }
    }

    /**
     * 解析单行数据，支持JSONL和CSV格式
     */
    private CMMUExample parseLine(String line, int index) throws Exception {
        CMMUExample example = new CMMUExample();

        if (line.startsWith("{")) {
            // JSONL格式
            JsonNode node = objectMapper.readTree(line);
            example.id = node.has("id") ? node.get("id").asText() : String.valueOf(index);
            example.question = node.get("question").asText();
            example.subject = node.has("subject") ? node.get("subject").asText() : "unknown";
            example.subcategory = node.has("subcategory") ? node.get("subcategory").asText() : null;

            // 解析选项
            List<String> choices = new ArrayList<>();
            if (node.has("choices")) {
                JsonNode choicesNode = node.get("choices");
                if (choicesNode.isArray()) {
                    for (JsonNode choice : choicesNode) {
                        choices.add(choice.asText());
                    }
                }
            } else {
                // CMMLU 常见格式：独立的 A/B/C/D 字段
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

        } else {
            // CSV格式: question,A,B,C,D,answer[,subject[,subcategory]]
            String[] parts = splitCsvLine(line);
            if (parts.length < 6) {
                return null;
            }
            example.id = String.valueOf(index);
            example.question = parts[0];
            example.choices = Arrays.asList(parts[1], parts[2], parts[3], parts[4]);
            example.answer = normalizeAnswer(parts[5].trim());
            example.subject = parts.length > 6 ? parts[6].trim() : "unknown";
            example.subcategory = parts.length > 7 ? parts[7].trim() : null;
        }

        // 自动推导大类
        example.category = getCategory(example.subject);

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
     * 简单CSV行分割
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

    /**
     * 获取主题对应的大类
     */
    public String getCategory(String subject) {
        if (subject == null) return CATEGORY_OTHER;
        if (subjectCategoryMap == null) {
            initSubjectCategoryMap();
        }
        return subjectCategoryMap.getOrDefault(subject, CATEGORY_OTHER);
    }

    @Override
    public String getId(CMMUExample example) {
        return example.id;
    }

    @Override
    public String getLabel(CMMUExample example) {
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
     * 按主题分组评估
     *
     * @param predictions 预测结果列表（与testData对应）
     * @return 每个主题的准确率
     */
    public Map<String, Double> evaluateBySubject(List<Object> predictions) {
        Map<String, List<Double>> subjectScores = new HashMap<>();

        List<CMMUExample> data = getTestData();
        for (int i = 0; i < Math.min(predictions.size(), data.size()); i++) {
            CMMUExample example = data.get(i);
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
     * 按大类分组评估（STEM/社科/人文/中国特色/其他）
     *
     * @param predictions 预测结果列表（与testData对应）
     * @return 每个大类的准确率
     */
    public Map<String, Double> evaluateByCategory(List<Object> predictions) {
        Map<String, List<Double>> categoryScores = new HashMap<>();

        List<CMMUExample> data = getTestData();
        for (int i = 0; i < Math.min(predictions.size(), data.size()); i++) {
            CMMUExample example = data.get(i);
            Map<String, Double> result = evaluate(predictions.get(i), example.answer);
            double score = result.getOrDefault("accuracy", 0.0);

            String category = example.category != null ? example.category : CATEGORY_OTHER;
            categoryScores.computeIfAbsent(category, k -> new ArrayList<>()).add(score);
        }

        Map<String, Double> categoryAccuracy = new LinkedHashMap<>();
        categoryScores.forEach((category, scores) -> {
            double avg = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            categoryAccuracy.put(category, avg);
        });

        return categoryAccuracy;
    }

    /**
     * 获取所有主题列表
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
     * 针对中文LLM输出优化
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

        // 兜底
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
    public String formatPrompt(CMMUExample example, List<CMMUExample> fewShots) {
        StringBuilder sb = new StringBuilder();
        sb.append("以下是关于").append(example.subject).append("的单项选择题，请直接给出正确答案的选项。\n\n");

        // Few-shot 示例
        if (fewShots != null) {
            for (CMMUExample shot : fewShots) {
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
    private String formatQuestion(CMMUExample example) {
        StringBuilder sb = new StringBuilder();
        sb.append("题目：").append(example.question).append("\n");
        for (int i = 0; i < example.choices.size(); i++) {
            sb.append(VALID_CHOICES.get(i)).append(". ").append(example.choices.get(i)).append("\n");
        }
        return sb.toString();
    }

    /**
     * CMMLU 样本数据结构
     */
    @Data
    public static class CMMUExample {
        private String id;
        private String question;
        private List<String> choices;
        private String answer;
        private String subject;
        private String subcategory;
        private String category;
    }
}
