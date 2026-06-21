package io.leavesfly.evox.benchmark;

import io.leavesfly.evox.benchmark.CEval.CEvalExample;
import io.leavesfly.evox.models.provider.ollama.OllamaLLM;
import io.leavesfly.evox.models.provider.ollama.OllamaLLMConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * C-Eval 中文评测集 Demo
 * <p>
 * 使用真实的 C-Eval 数据集和 Ollama 本地 LLM 进行评测。
 * 通过 HuggingFace Datasets Server API 直接获取 JSON 格式数据，通过 Ollama 生成答案并评测。
 * </p>
 * <p>
 * 前置条件：
 * 1. 本地运行 Ollama 服务 (http://localhost:11434)
 * 2. 已拉取模型，如: ollama pull qwen3:4b-instruct-2507-q8_0
 * 3. 网络可访问 HuggingFace Datasets API
 * </p>
 *
 * @author EvoX Team
 */
public class CEvalBenchmarkDemo {

    private static final Logger log = LoggerFactory.getLogger(CEvalBenchmarkDemo.class);

    /**
     * HuggingFace Datasets Server API 基址
     * C-Eval 已迁移到 parquet 格式，无法再通过zip下载，改用 Datasets API 获取 JSON 数据
     */
    private static final String DATASETS_API_BASE =
            "https://datasets-server.huggingface.co/rows?dataset=ceval/ceval-exam&split=val&offset=0&length=100&config=";

    /**
     * 需要加载的学科（对应 HuggingFace 上的 config 名称）
     */
    private static final Map<String, String> SUBJECT_FILES = new LinkedHashMap<>();

    static {
        // STEM
        SUBJECT_FILES.put("高等数学", "advanced_mathematics");
        SUBJECT_FILES.put("离散数学", "discrete_mathematics");
        SUBJECT_FILES.put("计算机网络", "computer_network");
        SUBJECT_FILES.put("操作系统", "operating_system");
        // 人文社科
        SUBJECT_FILES.put("中国语言文学", "chinese_language_and_literature");
        SUBJECT_FILES.put("马克思主义", "marxism");
        SUBJECT_FILES.put("教育学", "education_science");
        SUBJECT_FILES.put("法律", "law");
        // 专业
        SUBJECT_FILES.put("税务师", "tax_accountant");
        SUBJECT_FILES.put("医师资格", "physician");
    }

    private static final String DATA_DIR = "data/ceval";

    public static void main(String[] args) {
        CEvalBenchmarkDemo demo = new CEvalBenchmarkDemo();
        demo.run();
    }

    public void run() {
        printBanner();

        // Step 1: 初始化 LLM
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("【Step 1】初始化 Ollama LLM");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        OllamaLLMConfig config = new OllamaLLMConfig();
        config.setTemperature(0.1f);
        config.setMaxTokens(100);
        OllamaLLM llm = new OllamaLLM(config);
        log.info("模型: {}", config.getModel());
        log.info("地址: {}", config.getEffectiveBaseUrl());

        // Step 2: 下载数据集
        log.info("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("【Step 2】通过 HuggingFace Datasets API 获取 C-Eval 数据");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        Path dataFile = downloadAndPrepareData();
        if (dataFile == null) {
            log.error("数据集准备失败。请检查网络连接是否可访问 HuggingFace");
            return;
        }

        // Step 3: 加载评测集
        log.info("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("【Step 3】加载 C-Eval 评测集");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        CEval ceval = new CEval(dataFile.toString());
        log.info("数据集名称: {}", ceval.getName());
        log.info("涵盖学科: {}", ceval.getSubjects());

        List<CEvalExample> allExamples = new ArrayList<>();
        allExamples.addAll(ceval.getTrainData());
        allExamples.addAll(ceval.getDevData());
        allExamples.addAll(ceval.getTestData());
        log.info("总样本数: {}", allExamples.size());

        Collections.shuffle(allExamples, new Random(42));
        int sampleSize = Math.min(20, allExamples.size());
        List<CEvalExample> testExamples = allExamples.subList(0, sampleSize);
        log.info("抽取评测样本: {} 道题", sampleSize);

        // Step 4: LLM 评测
        log.info("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("【Step 4】使用 Ollama LLM 逐题评测（共 {} 题）", sampleSize);
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        int correctCount = 0;
        Map<String, List<Boolean>> subjectResults = new LinkedHashMap<>();

        for (int i = 0; i < testExamples.size(); i++) {
            CEvalExample example = testExamples.get(i);

            String prompt = ceval.formatPrompt(example, null);

            log.info("┌──────────────────────────────────────────────────────────────────────");
            log.info("│ 📝 第 {}/{} 题  |  学科: {}  |  正确答案: {}", i + 1, sampleSize, example.getSubject(), example.getAnswer());
            log.info("├──────────────────────────────────────────────────────────────────────");
            log.info("│ 【题目】");
            log.info("│ {}", example.getQuestion());
            List<String> choiceLabels = Arrays.asList("A", "B", "C", "D");
            for (int j = 0; j < example.getChoices().size(); j++) {
                String marker = choiceLabels.get(j).equals(example.getAnswer()) ? " ✓" : "";
                log.info("│   {}. {}{}", choiceLabels.get(j), example.getChoices().get(j), marker);
            }
            log.info("├──────────────────────────────────────────────────────────────────────");

            String llmResponse;
            try {
                llmResponse = llm.generate(prompt);
            } catch (Exception e) {
                log.warn("│ LLM调用失败: {}", e.getMessage());
                llmResponse = "";
            }

            Map<String, Double> metrics = ceval.evaluate(llmResponse, example.getAnswer());
            boolean isCorrect = metrics.get("accuracy") == 1.0;
            if (isCorrect) correctCount++;

            subjectResults.computeIfAbsent(example.getSubject(), k -> new ArrayList<>()).add(isCorrect);

            // 完整显示 LLM 回答
            log.info("│ 【LLM回答】");
            String[] responseLines = llmResponse.split("\n");
            for (String rLine : responseLines) {
                if (!rLine.trim().isEmpty()) {
                    log.info("│   {}", rLine.trim());
                }
            }

            log.info("├──────────────────────────────────────────────────────────────────────");
            log.info("│ → 提取答案: {}  |  正确答案: {}  |  {}",
                    extractAnswer(llmResponse), example.getAnswer(),
                    isCorrect ? "✅ 正确" : "❌ 错误");
            log.info("└──────────────────────────────────────────────────────────────────────");
        }

        // Step 5: 汇总
        log.info("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("【Step 5】C-Eval 评测结果汇总");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        double overallAccuracy = (double) correctCount / sampleSize;
        log.info("\n📊 总体结果:");
        log.info("   模型: {}", config.getModel());
        log.info("   总题数: {}", sampleSize);
        log.info("   正确数: {}", correctCount);
        log.info("   错误数: {}", sampleSize - correctCount);
        log.info("   准确率: {}", String.format("%.1f%%", overallAccuracy * 100));

        log.info("\n📚 按学科分组:");
        log.info("   {}", String.format("%-18s %-6s %-6s %s", "学科", "正确", "总数", "准确率"));
        log.info("   ────────────────────────────────────────");
        subjectResults.forEach((subject, results) -> {
            long correct = results.stream().filter(b -> b).count();
            double acc = (double) correct / results.size();
            log.info("   {}", String.format("%-18s %-6d %-6d %.1f%%", subject, correct, results.size(), acc * 100));
        });

        log.info("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("C-Eval 评测完成！");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    /**
     * 下载并准备 C-Eval 数据集
     * 使用 HuggingFace Datasets Server API 直接获取 JSON 数据（无需下载 zip）
     */
    private Path downloadAndPrepareData() {
        try {
            Path dataDir = Paths.get(DATA_DIR);
            Files.createDirectories(dataDir);

            Path mergedFile = dataDir.resolve("ceval_merged.jsonl");

            if (Files.exists(mergedFile) && Files.size(mergedFile) > 100) {
                log.info("使用缓存数据: {}", mergedFile);
                return mergedFile;
            }

            List<String> allLines = new ArrayList<>();

            for (Map.Entry<String, String> entry : SUBJECT_FILES.entrySet()) {
                String subjectCn = entry.getKey();
                String subjectEn = entry.getValue();
                String apiUrl = DATASETS_API_BASE + subjectEn;
                log.info("获取学科 [{}]: {}", subjectCn, truncate(apiUrl, 70));

                try {
                    String json = downloadString(apiUrl);
                    if (json == null || json.isEmpty()) {
                        log.warn("  跳过（请求失败）");
                        continue;
                    }

                    // 解析 HuggingFace Datasets API 响应
                    // 格式: {"rows":[{"row":{"id":0,"question":"...","A":"...","B":"...","C":"...","D":"...","answer":"C"}}]}
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(json);
                    com.fasterxml.jackson.databind.JsonNode rows = root.get("rows");

                    if (rows == null || !rows.isArray()) {
                        log.warn("  跳过（无有效数据）");
                        continue;
                    }

                    int count = 0;
                    for (com.fasterxml.jackson.databind.JsonNode rowWrapper : rows) {
                        com.fasterxml.jackson.databind.JsonNode row = rowWrapper.get("row");
                        if (row == null) continue;

                        String jsonLine = String.format(
                                "{\"id\":\"%s\",\"question\":\"%s\",\"A\":\"%s\",\"B\":\"%s\",\"C\":\"%s\",\"D\":\"%s\",\"answer\":\"%s\",\"subject\":\"%s\"}",
                                escapeJson(row.has("id") ? row.get("id").asText() : String.valueOf(count)),
                                escapeJson(row.get("question").asText()),
                                escapeJson(row.get("A").asText()),
                                escapeJson(row.get("B").asText()),
                                escapeJson(row.get("C").asText()),
                                escapeJson(row.get("D").asText()),
                                escapeJson(row.get("answer").asText()),
                                escapeJson(subjectCn)
                        );
                        allLines.add(jsonLine);
                        count++;
                    }
                    log.info("  已加载 {} 条", count);
                } catch (Exception e) {
                    log.warn("  获取失败: {}", e.getMessage());
                }
            }

            if (allLines.isEmpty()) {
                log.error("所有学科数据获取失败");
                return null;
            }

            Files.write(mergedFile, allLines);
            log.info("数据已保存到: {} (共 {} 条)", mergedFile, allLines.size());
            return mergedFile;

        } catch (IOException e) {
            log.error("准备数据失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 下载 URL 内容为字符串
     */
    private String downloadString(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(60000);
            conn.setRequestProperty("User-Agent", "EvoX-Benchmark/1.0");
            conn.setInstanceFollowRedirects(true);

            if (conn.getResponseCode() != 200) {
                log.debug("HTTP {}", conn.getResponseCode());
                return null;
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                return sb.toString();
            }
        } catch (Exception e) {
            log.debug("Download error: {}", e.getMessage());
            return null;
        }
    }


    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }

    private String extractAnswer(String response) {
        if (response == null || response.isEmpty()) return "?";
        for (char c : response.toUpperCase().toCharArray()) {
            if (c >= 'A' && c <= 'D') return String.valueOf(c);
        }
        return "?";
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }

    private void printBanner() {
        log.info("");
        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║           C-Eval 中文评测集 Demo (真实LLM版)                 ║");
        log.info("║   使用 Ollama 本地大模型 + HuggingFace 官方数据集             ║");
        log.info("║                                                              ║");
        log.info("║   52学科/4难度级别 | 随机抽取20题 | LLM推理评测               ║");
        log.info("╚══════════════════════════════════════════════════════════════╝");
        log.info("");
    }
}
