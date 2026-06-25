package io.leavesfly.evox.benchmark;

import io.leavesfly.evox.benchmark.GaokaoBench.GaokaoExample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GAOKAO-Bench 高考评测集 Demo
 * <p>
 * 使用真实的 GAOKAO-Bench 数据集和 Ollama 本地 LLM 进行评测。
 * 通过 Ollama 原生 API（/api/chat）调用，使用 think:false 关闭思考模式，直接输出答案。
 * 从 GitHub 直接下载 GAOKAO-Bench 原始 JSON 数据（客观选择题）。
 * </p>
 * <p>
 * 前置条件：
 * 1. 本地运行 Ollama 服务 (http://localhost:11434)
 * 2. 已拉取模型，如: ollama pull qwen3.5:4b
 * 3. 网络可访问 GitHub raw 内容
 * </p>
 *
 * @author EvoX Team
 */
public class GaokaoBenchmarkDemo {

    private static final Logger log = LoggerFactory.getLogger(GaokaoBenchmarkDemo.class);

    /**
     * GitHub 原始数据下载基址
     */
    private static final String GITHUB_RAW_BASE =
            "https://raw.githubusercontent.com/OpenLMLab/GAOKAO-Bench/main/Data/Objective_Questions/";

    /**
     * 需要加载的科目文件 (科目中文名 -> 文件名)
     */
    private static final Map<String, String> SUBJECT_FILES = new LinkedHashMap<>();

    static {
        SUBJECT_FILES.put("生物", "2010-2022_Biology_MCQs.json");
        SUBJECT_FILES.put("化学", "2010-2022_Chemistry_MCQs.json");
        SUBJECT_FILES.put("物理", "2010-2022_Physics_MCQs.json");
        SUBJECT_FILES.put("数学(理)", "2010-2022_Math_I_MCQs.json");
        SUBJECT_FILES.put("数学(文)", "2010-2022_Math_II_MCQs.json");
        SUBJECT_FILES.put("政治", "2010-2022_Political_Science_MCQs.json");
        SUBJECT_FILES.put("历史", "2010-2022_History_MCQs.json");
        SUBJECT_FILES.put("地理", "2010-2022_Geography_MCQs.json");
        SUBJECT_FILES.put("语文", "2010-2022_Chinese_Lang_and_Usage_MCQs.json");
    }

    private static final String DATA_DIR = "data/gaokao";

    /**
     * Ollama 服务配置
     */
    private static final String OLLAMA_BASE_URL = "http://localhost:11434";
    private static final String OLLAMA_MODEL = "qwen3.5:4b";

    public static void main(String[] args) {
        GaokaoBenchmarkDemo demo = new GaokaoBenchmarkDemo();
        demo.run();
    }

    public void run() {
        printBanner();

        // Step 1: 初始化 LLM
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("【Step 1】初始化 Ollama LLM（非思考模式）");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        log.info("模型: {}", OLLAMA_MODEL);
        log.info("地址: {}", OLLAMA_BASE_URL);
        log.info("模式: 非思考模式 (think=false，使用 Ollama 原生 API)");

        // Step 2: 下载数据集
        log.info("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("【Step 2】从 GitHub 下载 GAOKAO-Bench 客观题数据");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        Path dataFile = downloadAndPrepareData();
        if (dataFile == null) {
            log.error("数据集准备失败。请检查网络连接是否可访问 GitHub");
            return;
        }

        // Step 3: 加载评测集
        log.info("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("【Step 3】加载 GAOKAO-Bench 评测集");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        GaokaoBench gaokao = new GaokaoBench(dataFile.toString());
        log.info("数据集名称: {}", gaokao.getName());
        log.info("涵盖科目: {}", gaokao.getSubjects());

        List<GaokaoExample> allExamples = new ArrayList<>();
        allExamples.addAll(gaokao.getTrainData());
        allExamples.addAll(gaokao.getDevData());
        allExamples.addAll(gaokao.getTestData());
        log.info("总样本数: {}", allExamples.size());

        Collections.shuffle(allExamples, new Random(42));
        int sampleSize = Math.min(20, allExamples.size());
        List<GaokaoExample> testExamples = allExamples.subList(0, sampleSize);
        log.info("抽取评测样本: {} 道题", sampleSize);

        // Step 4: LLM 评测
        log.info("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("【Step 4】使用 Ollama LLM 逐题评测（共 {} 题）", sampleSize);
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        int correctCount = 0;
        Map<String, List<Boolean>> subjectResults = new LinkedHashMap<>();

        for (int i = 0; i < testExamples.size(); i++) {
            GaokaoExample example = testExamples.get(i);

            String prompt = gaokao.formatPrompt(example, null);

            log.info("┌──────────────────────────────────────────────────────────────────────");
            log.info("│ 📝 第 {}/{} 题  |  科目: {}  |  年份: {}  |  正确答案: {}", i + 1, sampleSize, example.getSubject(), example.getYear(), example.getAnswer());
            log.info("├──────────────────────────────────────────────────────────────────────");
            log.info("│ 【题目】");
            log.info("│ {}", truncate(example.getQuestion(), 120));
            List<String> choiceLabels = Arrays.asList("A", "B", "C", "D");
            for (int j = 0; j < example.getChoices().size() && j < choiceLabels.size(); j++) {
                String marker = choiceLabels.get(j).equals(example.getAnswer()) ? " ✓" : "";
                log.info("│   {}. {}{}", choiceLabels.get(j), truncate(example.getChoices().get(j), 80), marker);
            }
            log.info("├──────────────────────────────────────────────────────────────────────");

            String llmResponse;
            try {
                llmResponse = callOllama(prompt);
            } catch (Exception e) {
                log.warn("│ LLM调用失败: {}", e.getMessage());
                llmResponse = "";
            }

            Map<String, Double> metrics = gaokao.evaluate(llmResponse, example.getAnswer());
            boolean isCorrect = metrics.get("accuracy") == 1.0;
            if (isCorrect) correctCount++;

            subjectResults.computeIfAbsent(example.getSubject(), k -> new ArrayList<>()).add(isCorrect);

            // 完整显示 LLM 回答
            log.info("│ 【LLM回答】");
            String[] responseLines = llmResponse.split("\n");
            for (String rLine : responseLines) {
                if (!rLine.trim().isEmpty()) {
                    log.info("│   {}", truncate(rLine.trim(), 100));
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
        log.info("【Step 5】GAOKAO-Bench 评测结果汇总");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        double overallAccuracy = (double) correctCount / sampleSize;
        log.info("\n📊 总体结果:");
        log.info("   模型: {}", OLLAMA_MODEL);
        log.info("   总题数: {}", sampleSize);
        log.info("   正确数: {}", correctCount);
        log.info("   错误数: {}", sampleSize - correctCount);
        log.info("   准确率: {}", String.format("%.1f%%", overallAccuracy * 100));

        log.info("\n📚 按科目分组:");
        log.info("   {}", String.format("%-12s %-6s %-6s %s", "科目", "正确", "总数", "准确率"));
        log.info("   ────────────────────────────────────────");
        subjectResults.forEach((subject, results) -> {
            long correct = results.stream().filter(b -> b).count();
            double acc = (double) correct / results.size();
            log.info("   {}", String.format("%-12s %-6d %-6d %.1f%%", subject, correct, results.size(), acc * 100));
        });

        log.info("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("GAOKAO-Bench 评测完成！");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    /**
     * 下载并准备 GAOKAO-Bench 数据集
     * 从 GitHub 下载各科目的 JSON 文件并合并为 JSONL 格式
     */
    private Path downloadAndPrepareData() {
        try {
            Path dataDir = Paths.get(DATA_DIR);
            Files.createDirectories(dataDir);

            Path mergedFile = dataDir.resolve("gaokao_merged.jsonl");

            if (Files.exists(mergedFile) && Files.size(mergedFile) > 100) {
                log.info("使用缓存数据: {}", mergedFile);
                return mergedFile;
            }

            List<String> allLines = new ArrayList<>();
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

            for (Map.Entry<String, String> entry : SUBJECT_FILES.entrySet()) {
                String subjectCn = entry.getKey();
                String fileName = entry.getValue();
                String fileUrl = GITHUB_RAW_BASE + fileName;
                log.info("获取科目 [{}]: {}", subjectCn, truncate(fileUrl, 70));

                try {
                    String json = downloadString(fileUrl);
                    if (json == null || json.isEmpty()) {
                        log.warn("  跳过（请求失败）");
                        continue;
                    }

                    // 解析 GAOKAO-Bench JSON 格式
                    // 格式: {"keywords":"...", "example":[{"year":"2010","category":"...",
                    //         "question":"...","answer":["D"],"analysis":"...","index":0,"score":6},...]}
                    com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(json);
                    com.fasterxml.jackson.databind.JsonNode examples = root.get("example");

                    if (examples == null || !examples.isArray()) {
                        log.warn("  跳过（无有效数据）");
                        continue;
                    }

                    int count = 0;
                    for (com.fasterxml.jackson.databind.JsonNode item : examples) {
                        // 获取答案（数组格式）
                        String answer = "";
                        com.fasterxml.jackson.databind.JsonNode answerNode = item.get("answer");
                        if (answerNode != null && answerNode.isArray() && answerNode.size() > 0) {
                            answer = answerNode.get(0).asText();
                        }

                        // 只保留单选题（答案为单个字母A-D）
                        if (answer.length() != 1 || !answer.matches("[A-Da-d]")) {
                            continue;
                        }

                        String question = item.has("question") ? item.get("question").asText() : "";
                        String year = item.has("year") ? item.get("year").asText() : "unknown";
                        String category = item.has("category") ? item.get("category").asText() : "unknown";
                        String analysis = item.has("analysis") ? item.get("analysis").asText() : "";
                        int score = item.has("score") ? item.get("score").asInt() : 0;
                        int index = item.has("index") ? item.get("index").asInt() : count;

                        String jsonLine = mapper.writeValueAsString(
                                mapper.createObjectNode()
                                        .put("id", subjectCn + "_" + index)
                                        .put("question", question)
                                        .put("answer", answer.toUpperCase())
                                        .put("subject", subjectCn)
                                        .put("year", year)
                                        .put("category", category)
                                        .put("score", score)
                                        .put("analysis", analysis)
                        );
                        allLines.add(jsonLine);
                        count++;
                    }
                    log.info("  已加载 {} 条单选题", count);
                } catch (Exception e) {
                    log.warn("  获取失败: {}", e.getMessage());
                }
            }

            if (allLines.isEmpty()) {
                log.error("所有科目数据获取失败");
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
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                return sb.toString();
            }
        } catch (Exception e) {
            log.debug("Download error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 通过 Ollama 原生 API (/api/chat) 调用 LLM
     * 使用 think:false 关闭思考模式，直接输出答案
     */
    private String callOllama(String prompt) {
        try {
            URL url = new URL(OLLAMA_BASE_URL + "/api/chat");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(300000);
            conn.setRequestProperty("Content-Type", "application/json");

            // 构造请求体：think=false 关闭思考模式，stream=false 同步返回
            String requestBody = String.format(
                    "{\"model\":\"%s\",\"messages\":[{\"role\":\"user\",\"content\":\"%s\"}],"
                            + "\"think\":false,\"stream\":false,\"options\":{\"temperature\":0.1,\"num_predict\":200}}",
                    OLLAMA_MODEL, escapeJson(prompt)
            );

            try (OutputStream os = conn.getOutputStream()) {
                os.write(requestBody.getBytes(StandardCharsets.UTF_8));
            }

            if (conn.getResponseCode() != 200) {
                log.warn("Ollama API HTTP {}", conn.getResponseCode());
                return "";
            }

            // 读取响应
            String responseJson;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                responseJson = sb.toString();
            }

            // 解析 Ollama 原生 API 响应：{"message":{"content":"..."}}
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(responseJson);
            com.fasterxml.jackson.databind.JsonNode message = root.get("message");
            if (message != null && message.has("content")) {
                return message.get("content").asText("");
            }
            return "";
        } catch (Exception e) {
            log.error("Ollama API 调用失败: {}", e.getMessage());
            return "";
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }

    private String extractAnswer(String response) {
        if (response == null || response.isEmpty()) return "?";
        // 优先匹配 "答案：X" 或 "故选：X" 格式
        Matcher matcher = Pattern.compile("(?:答案|故选)[：:]\\s*([A-Da-d])").matcher(response);
        if (matcher.find()) {
            return matcher.group(1).trim().toUpperCase();
        }
        // 回退：提取第一个出现的 A-D 字母
        for (char c : response.toUpperCase().toCharArray()) {
            if (c >= 'A' && c <= 'D') return String.valueOf(c);
        }
        return "?";
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        text = text.replace("\n", " ").replace("\r", "");
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }

    private void printBanner() {
        log.info("");
        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║        GAOKAO-Bench 高考评测集 Demo (真实LLM版)              ║");
        log.info("║   使用 Ollama 本地大模型 + GitHub 官方数据集                  ║");
        log.info("║                                                              ║");
        log.info("║   2010-2022高考真题 | 9大科目 | 随机抽取20题 | LLM推理评测    ║");
        log.info("╚══════════════════════════════════════════════════════════════╝");
        log.info("");
    }
}
