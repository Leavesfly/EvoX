package io.leavesfly.evox.benchmark;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.leavesfly.evox.benchmark.GSM8K.GSM8KExample;
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
 * GSM8K 数学推理评测集 Demo
 * <p>
 * 使用真实的 GSM8K 数据集和 Ollama 本地 LLM 进行数学推理评测。
 * 通过 HuggingFace Datasets Server API 获取 openai/gsm8k 数据集，
 * 使用 LLM 进行链式推理（Chain-of-Thought），提取最终数值答案后评测。
 * </p>
 * <p>
 * GSM8K 特点：
 * - 8000+ 小学数学应用题
 * - 答案格式: 推理过程 + "#### 最终数值答案"
 * - 评估 LLM 的数学推理和计算能力
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
public class GSM8KBenchmarkDemo {

    private static final Logger log = LoggerFactory.getLogger(GSM8KBenchmarkDemo.class);

    /**
     * HuggingFace Datasets Server API
     * GSM8K 数据集: openai/gsm8k, config=main, split=test
     */
    private static final String DATASETS_API_URL =
            "https://datasets-server.huggingface.co/rows?dataset=openai/gsm8k&config=main&split=test&offset=0&length=100";

    private static final String DATA_DIR = "data/gsm8k";

    public static void main(String[] args) {
        GSM8KBenchmarkDemo demo = new GSM8KBenchmarkDemo();
        demo.run();
    }

    public void run() {
        printBanner();

        // Step 1: 初始化 LLM
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("【Step 1】初始化 Ollama LLM");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        OllamaLLMConfig config = new OllamaLLMConfig();
        config.setTemperature(0.1f);
        config.setMaxTokens(512); // 数学推理需要较多 token 生成推理步骤
        OllamaLLM llm = new OllamaLLM(config);
        log.info("模型: {}", config.getModel());
        log.info("地址: {}", config.getEffectiveBaseUrl());

        // Step 2: 获取数据集
        log.info("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("【Step 2】通过 HuggingFace Datasets API 获取 GSM8K 数据");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        Path dataFile = downloadAndPrepareData();
        if (dataFile == null) {
            log.error("数据集准备失败。请检查网络连接是否可访问 HuggingFace");
            return;
        }

        // Step 3: 加载评测集
        log.info("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("【Step 3】加载 GSM8K 评测集");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        GSM8K gsm8k = new GSM8K(dataFile.toString());
        log.info("数据集名称: {}", gsm8k.getName());

        List<GSM8KExample> allExamples = new ArrayList<>();
        allExamples.addAll(gsm8k.getTrainData());
        allExamples.addAll(gsm8k.getTestData());
        log.info("总样本数: {}", allExamples.size());

        // 随机抽取20道题
        Collections.shuffle(allExamples, new Random(42));
        int sampleSize = Math.min(20, allExamples.size());
        List<GSM8KExample> testExamples = allExamples.subList(0, sampleSize);
        log.info("抽取评测样本: {} 道题", sampleSize);

        // Step 4: LLM 逐题评测
        log.info("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("【Step 4】使用 Ollama LLM 逐题评测（共 {} 题）", sampleSize);
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        int correctCount = 0;

        for (int i = 0; i < testExamples.size(); i++) {
            GSM8KExample example = testExamples.get(i);

            // 构造 Chain-of-Thought prompt
            String prompt = buildPrompt(example);

            // 提取标准答案的最终数值
            String groundTruth = extractNumericAnswer(example.getAnswer());

            log.info("┌──────────────────────────────────────────────────────────────────────");
            log.info("│ 📝 第 {}/{} 题                                      正确答案: {}", i + 1, sampleSize, groundTruth);
            log.info("├──────────────────────────────────────────────────────────────────────");
            log.info("│ 【题目】");
            log.info("│ {}", example.getQuestion());
            log.info("├──────────────────────────────────────────────────────────────────────");

            // 调用 LLM
            String llmResponse;
            try {
                llmResponse = llm.generate(prompt);
            } catch (Exception e) {
                log.warn("│ LLM调用失败: {}", e.getMessage());
                llmResponse = "";
            }

            // 从 LLM 回答中提取数值答案
            String predictedAnswer = extractNumericAnswer(llmResponse);

            // 评估
            Map<String, Double> metrics = gsm8k.evaluate(llmResponse, example.getAnswer());
            boolean isCorrect = metrics.get("accuracy") == 1.0;
            if (isCorrect) correctCount++;

            // 完整显示 LLM 推理过程（多行）
            log.info("│ 【LLM推理】");
            String[] reasoningLines = llmResponse.split("\n");
            for (String rLine : reasoningLines) {
                if (!rLine.trim().isEmpty()) {
                    log.info("│   {}", rLine.trim());
                }
            }

            log.info("├──────────────────────────────────────────────────────────────────────");
            log.info("│ → 提取答案: {}  |  正确答案: {}  |  {}",
                    predictedAnswer.isEmpty() ? "?" : predictedAnswer,
                    groundTruth,
                    isCorrect ? "✅ 正确" : "❌ 错误");
            log.info("└──────────────────────────────────────────────────────────────────────");
        }

        // Step 5: 汇总结果
        log.info("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("【Step 5】GSM8K 评测结果汇总");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        double overallAccuracy = (double) correctCount / sampleSize;
        log.info("\n📊 总体结果:");
        log.info("   模型: {}", config.getModel());
        log.info("   总题数: {}", sampleSize);
        log.info("   正确数: {}", correctCount);
        log.info("   错误数: {}", sampleSize - correctCount);
        log.info("   准确率: {}", String.format("%.1f%%", overallAccuracy * 100));
        log.info("   评测类型: 数学推理 (Chain-of-Thought)");

        // 可视化进度条
        log.info("\n   准确率可视化: {} {}", generateBar(overallAccuracy),
                String.format("%.1f%%", overallAccuracy * 100));

        log.info("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("GSM8K 评测完成！");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    /**
     * 构造 GSM8K Chain-of-Thought Prompt
     * 引导模型先展示推理过程，最后用 #### 给出数值答案
     */
    private String buildPrompt(GSM8KExample example) {
        return "Solve the following math problem step by step. "
                + "Show your reasoning process, then give the final numerical answer after ####.\n\n"
                + "Question: " + example.getQuestion() + "\n\n"
                + "Let's solve this step by step:\n";
    }

    /**
     * 从文本中提取数值答案
     * GSM8K 标准格式: 推理过程\n#### 最终数值
     */
    private String extractNumericAnswer(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        // 优先提取 #### 之后的数值
        String[] parts = text.split("####");
        if (parts.length > 1) {
            String answer = parts[parts.length - 1].trim();
            return answer.replaceAll("[^0-9.-]", "");
        }

        // 兜底：尝试提取最后出现的数字
        String cleaned = text.replaceAll(",", "");
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("-?\\d+\\.?\\d*")
                .matcher(cleaned);
        String lastNumber = "";
        while (matcher.find()) {
            lastNumber = matcher.group();
        }
        return lastNumber;
    }

    /**
     * 下载并准备 GSM8K 数据集
     * 使用 HuggingFace Datasets Server API 直接获取 JSON 数据
     */
    private Path downloadAndPrepareData() {
        try {
            Path dataDir = Paths.get(DATA_DIR);
            Files.createDirectories(dataDir);

            Path mergedFile = dataDir.resolve("gsm8k_test.jsonl");

            // 使用缓存
            if (Files.exists(mergedFile) && Files.size(mergedFile) > 100) {
                log.info("使用缓存数据: {}", mergedFile);
                return mergedFile;
            }

            log.info("正在从 HuggingFace Datasets API 获取 GSM8K 数据...");
            log.info("API: {}", truncate(DATASETS_API_URL, 70));

            String json = downloadString(DATASETS_API_URL);
            if (json == null || json.isEmpty()) {
                log.error("API 请求失败");
                return null;
            }

            // 解析 API 响应
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);
            JsonNode rows = root.get("rows");

            if (rows == null || !rows.isArray()) {
                log.error("API 返回数据格式异常");
                return null;
            }

            List<String> allLines = new ArrayList<>();
            for (JsonNode rowWrapper : rows) {
                JsonNode row = rowWrapper.get("row");
                if (row == null) continue;

                String question = row.get("question").asText();
                String answer = row.get("answer").asText();

                String jsonLine = String.format(
                        "{\"question\":\"%s\",\"answer\":\"%s\"}",
                        escapeJson(question),
                        escapeJson(answer)
                );
                allLines.add(jsonLine);
            }

            if (allLines.isEmpty()) {
                log.error("未获取到有效数据");
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
                log.warn("HTTP 响应码: {}", conn.getResponseCode());
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
            log.error("下载失败: {}", e.getMessage());
            return null;
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }

    private String generateBar(double ratio) {
        int filled = (int) (ratio * 20);
        StringBuilder sb = new StringBuilder("▐");
        for (int i = 0; i < 20; i++) sb.append(i < filled ? "█" : "░");
        sb.append("▌");
        return sb.toString();
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }

    private void printBanner() {
        log.info("");
        log.info("╔════════════════════════════════════════════════════════════════════╗");
        log.info("║          GSM8K 数学推理评测 Demo (真实LLM版)                       ║");
        log.info("║   Grade School Math 8K - 小学数学应用题                            ║");
        log.info("║                                                                    ║");
        log.info("║   使用 Ollama 本地大模型 + HuggingFace 官方数据集                   ║");
        log.info("║   Chain-of-Thought 推理 | 随机抽取20题 | 提取数值答案评测           ║");
        log.info("╚════════════════════════════════════════════════════════════════════╝");
        log.info("");
    }
}
