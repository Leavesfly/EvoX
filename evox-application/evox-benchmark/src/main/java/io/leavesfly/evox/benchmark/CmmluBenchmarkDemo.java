package io.leavesfly.evox.benchmark;

import io.leavesfly.evox.benchmark.CMMLU;
import io.leavesfly.evox.benchmark.CMMLU.CMMUExample;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * CMMLU 中文多任务语言理解评测集 Demo
 * <p>
 * 使用真实的 CMMLU 数据集和 Ollama 本地 LLM 进行评测。
 * 自动从 HuggingFace 下载 CMMLU 官方 zip 数据包并解压，通过 Ollama 生成答案并评测。
 * </p>
 * <p>
 * 前置条件：
 * 1. 本地运行 Ollama 服务 (http://localhost:11434)
 * 2. 已拉取模型，如: ollama pull qwen3:4b-instruct-2507-q8_0
 * 3. 网络可访问 HuggingFace（下载数据集 zip 包）
 * </p>
 *
 * @author EvoX Team
 */
public class CmmluBenchmarkDemo {

    private static final Logger log = LoggerFactory.getLogger(CmmluBenchmarkDemo.class);

    /**
     * CMMLU 官方数据集 zip 下载 URL（HuggingFace）
     */
    private static final String CMMLU_ZIP_URL =
            "https://huggingface.co/datasets/haonan-li/cmmlu/resolve/main/cmmlu_v1_0_1.zip";

    /**
     * 需要加载的主题（对应zip中 test/ 目录下的文件名，不含.csv后缀）
     * 文件名列表来自官方仓库 cmmlu.py 中的 task_list
     */
    private static final Map<String, String> SUBJECT_FILES = new LinkedHashMap<>();

    static {
        // 中国特色
        SUBJECT_FILES.put("中医", "traditional_chinese_medicine");
        SUBJECT_FILES.put("中国食文化", "chinese_food_culture");
        SUBJECT_FILES.put("驾照考试", "chinese_driving_rule");
        SUBJECT_FILES.put("公务员考试", "chinese_civil_service_exam");
        // 人文学科
        SUBJECT_FILES.put("中国历史", "chinese_history");
        SUBJECT_FILES.put("中国文学", "chinese_literature");
        SUBJECT_FILES.put("古代汉语", "ancient_chinese");
        // STEM
        SUBJECT_FILES.put("初等数学", "elementary_mathematics");
        SUBJECT_FILES.put("高中物理", "high_school_physics");
        SUBJECT_FILES.put("计算机科学", "computer_science");
        // 社会科学
        SUBJECT_FILES.put("经济学", "economics");
        SUBJECT_FILES.put("法学", "college_law");
    }

    private static final String DATA_DIR = "data/cmmlu";

    public static void main(String[] args) {
        CmmluBenchmarkDemo demo = new CmmluBenchmarkDemo();
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
        config.setMaxTokens(100);
        OllamaLLM llm = new OllamaLLM(config);
        log.info("模型: {}", config.getModel());
        log.info("地址: {}", config.getEffectiveBaseUrl());

        // Step 2: 下载数据集
        log.info("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("【Step 2】下载 CMMLU 真实数据集（HuggingFace 官方 zip 包）");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        Path dataFile = downloadAndPrepareData();
        if (dataFile == null) {
            log.error("数据集准备失败。请手动下载:");
            log.error("  wget {}", CMMLU_ZIP_URL);
            log.error("  解压到 {} 目录", DATA_DIR);
            return;
        }

        // Step 3: 加载评测集
        log.info("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("【Step 3】加载 CMMLU 评测集");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        CMMLU cmmlu = new CMMLU(dataFile.toString());
        log.info("数据集名称: {}", cmmlu.getName());
        log.info("涵盖主题: {}", cmmlu.getSubjects());

        List<CMMUExample> allExamples = new ArrayList<>();
        allExamples.addAll(cmmlu.getTrainData());
        allExamples.addAll(cmmlu.getDevData());
        allExamples.addAll(cmmlu.getTestData());
        log.info("总样本数: {}", allExamples.size());

        Collections.shuffle(allExamples, new Random(42));
        int sampleSize = Math.min(20, allExamples.size());
        List<CMMUExample> testExamples = allExamples.subList(0, sampleSize);
        log.info("抽取评测样本: {} 道题", sampleSize);

        // Step 4: LLM 评测
        log.info("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("【Step 4】使用 Ollama LLM 逐题评测（共 {} 题）", sampleSize);
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        int correctCount = 0;
        Map<String, List<Boolean>> subjectResults = new LinkedHashMap<>();
        Map<String, List<Boolean>> categoryResults = new LinkedHashMap<>();

        for (int i = 0; i < testExamples.size(); i++) {
            CMMUExample example = testExamples.get(i);
            String category = example.getCategory() != null ? example.getCategory() : "Other";

            String prompt = cmmlu.formatPrompt(example, null);

            log.info("┌──────────────────────────────────────────────────────────────────────");
            log.info("│ 📝 第 {}/{} 题  |  主题: {}  |  大类: {}  |  正确答案: {}", i + 1, sampleSize, example.getSubject(), category, example.getAnswer());
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

            Map<String, Double> metrics = cmmlu.evaluate(llmResponse, example.getAnswer());
            boolean isCorrect = metrics.get("accuracy") == 1.0;
            if (isCorrect) correctCount++;

            subjectResults.computeIfAbsent(example.getSubject(), k -> new ArrayList<>()).add(isCorrect);
            categoryResults.computeIfAbsent(category, k -> new ArrayList<>()).add(isCorrect);

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
        log.info("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("【Step 5】CMMLU 评测结果汇总");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        double overallAccuracy = (double) correctCount / sampleSize;
        log.info("\n📊 总体结果:");
        log.info("   模型: {}", config.getModel());
        log.info("   总题数: {}", sampleSize);
        log.info("   正确数: {}", correctCount);
        log.info("   错误数: {}", sampleSize - correctCount);
        log.info("   准确率: {}", String.format("%.1f%%", overallAccuracy * 100));

        log.info("\n🏷️  按大类分组:");
        log.info("   {}", String.format("%-20s %-6s %-6s %s", "大类", "正确", "总数", "准确率"));
        log.info("   ──────────────────────────────────────────────");
        categoryResults.forEach((cat, results) -> {
            long correct = results.stream().filter(b -> b).count();
            double acc = (double) correct / results.size();
            log.info("   {} {}", String.format("%-20s %-6d %-6d %.1f%%", cat, correct, results.size(), acc * 100), generateBar(acc));
        });

        log.info("\n📚 按主题分组:");
        log.info("   {}", String.format("%-18s %-6s %-6s %s", "主题", "正确", "总数", "准确率"));
        log.info("   ──────────────────────────────────────────────");
        subjectResults.forEach((subject, results) -> {
            long correct = results.stream().filter(b -> b).count();
            double acc = (double) correct / results.size();
            log.info("   {}", String.format("%-18s %-6d %-6d %.1f%%", subject, correct, results.size(), acc * 100));
        });

        log.info("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("CMMLU 评测完成！");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    /**
     * 下载并准备 CMMLU 数据集
     * 从 HuggingFace 下载官方 zip 包，解压后提取需要的主题 CSV，合并为 JSONL
     */
    private Path downloadAndPrepareData() {
        try {
            Path dataDir = Paths.get(DATA_DIR);
            Files.createDirectories(dataDir);

            Path mergedFile = dataDir.resolve("cmmlu_merged.jsonl");

            // 如果已有缓存，直接使用
            if (Files.exists(mergedFile) && Files.size(mergedFile) > 100) {
                log.info("使用缓存数据: {}", mergedFile);
                return mergedFile;
            }

            // 下载 zip
            Path zipFile = dataDir.resolve("cmmlu_v1_0_1.zip");
            if (!Files.exists(zipFile) || Files.size(zipFile) < 1000) {
                log.info("正在从 HuggingFace 下载 CMMLU 数据集...");
                log.info("URL: {}", CMMLU_ZIP_URL);
                log.info("(首次下载约 3MB，请耐心等待)");

                boolean downloaded = downloadToFile(CMMLU_ZIP_URL, zipFile);
                if (!downloaded) {
                    log.error("zip 下载失败，请检查网络连接");
                    log.info("你也可以手动下载后放到: {}", zipFile.toAbsolutePath());
                    return null;
                }
                log.info("下载完成: {} ({}KB)", zipFile, Files.size(zipFile) / 1024);
            } else {
                log.info("使用已缓存的 zip: {}", zipFile);
            }

            // 从 zip 中提取所需主题的 CSV 数据
            log.info("正在解析 zip 包中的 CSV 数据...");
            List<String> allLines = extractFromZip(zipFile);

            if (allLines.isEmpty()) {
                log.error("从 zip 中未提取到有效数据");
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
     * 从 zip 文件中提取指定主题的 CSV 数据并转为 JSONL
     * CMMLU CSV 由 pandas 导出，格式固定为: index,Question,A,B,C,D,Answer（7列）
     */
    private List<String> extractFromZip(Path zipFile) {
        List<String> allLines = new ArrayList<>();
        Set<String> targetFiles = new HashSet<>();
        Map<String, String> fileToSubject = new HashMap<>();

        for (Map.Entry<String, String> entry : SUBJECT_FILES.entrySet()) {
            String fileName = "test/" + entry.getValue() + ".csv";
            targetFiles.add(fileName);
            fileToSubject.put(fileName, entry.getKey());
        }

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile.toFile()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();

                // 匹配目标文件（可能有前缀目录）
                String matchedFile = null;
                for (String target : targetFiles) {
                    if (entryName.endsWith(target)) {
                        matchedFile = target;
                        break;
                    }
                }

                if (matchedFile != null) {
                    String subject = fileToSubject.get(matchedFile);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(zis, "UTF-8"));
                    String line;
                    boolean isHeader = true;
                    int count = 0;

                    while ((line = reader.readLine()) != null) {
                        if (isHeader) {
                            isHeader = false;
                            continue; // 跳过CSV header
                        }
                        line = line.trim();
                        if (line.isEmpty()) continue;

                        String[] parts = splitCsvLine(line);
                        // CMMLU CSV格式固定: index(0), Question(1), A(2), B(3), C(4), D(5), Answer(6)
                        // pandas 导出总是有 index 列，所以强制 offset=1
                        if (parts.length >= 7) {
                            String question = parts[1];
                            String choiceA = parts[2];
                            String choiceB = parts[3];
                            String choiceC = parts[4];
                            String choiceD = parts[5];
                            String answer = parts[6].trim();

                            String jsonLine = String.format(
                                    "{\"id\":\"%d\",\"question\":\"%s\",\"choices\":[\"%s\",\"%s\",\"%s\",\"%s\"],\"answer\":\"%s\",\"subject\":\"%s\"}",
                                    allLines.size() + 1,
                                    escapeJson(question),
                                    escapeJson(choiceA),
                                    escapeJson(choiceB),
                                    escapeJson(choiceC),
                                    escapeJson(choiceD),
                                    escapeJson(answer),
                                    escapeJson(subject)
                            );
                            allLines.add(jsonLine);
                            count++;
                        } else {
                            log.debug("  跳过格式异常行（列数={}）: {}", parts.length, truncate(line, 40));
                        }
                    }
                    log.info("  提取 [{}]: {} 条", subject, count);
                }
                zis.closeEntry();
            }
        } catch (IOException e) {
            log.error("解压 zip 失败: {}", e.getMessage());
        }

        return allLines;
    }

    /**
     * 下载文件到本地
     */
    private boolean downloadToFile(String urlStr, Path target) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(120000);
            conn.setRequestProperty("User-Agent", "EvoX-Benchmark/1.0");
            conn.setInstanceFollowRedirects(true);

            int code = conn.getResponseCode();
            // 处理重定向
            if (code == 302 || code == 301) {
                String redirectUrl = conn.getHeaderField("Location");
                conn = (HttpURLConnection) new URL(redirectUrl).openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(30000);
                conn.setReadTimeout(120000);
                code = conn.getResponseCode();
            }

            if (code != 200) {
                log.warn("HTTP 响应码: {}", code);
                return false;
            }

            try (InputStream is = conn.getInputStream();
                 FileOutputStream fos = new FileOutputStream(target.toFile())) {
                byte[] buf = new byte[8192];
                int len;
                long total = 0;
                while ((len = is.read(buf)) != -1) {
                    fos.write(buf, 0, len);
                    total += len;
                    if (total % (512 * 1024) == 0) {
                        log.info("  已下载 {}KB...", total / 1024);
                    }
                }
            }
            return true;
        } catch (Exception e) {
            log.error("下载失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * RFC 4180 兼容的 CSV 行分割
     * 正确处理：引号字段、字段内逗号、双引号转义（"" → "）
     */
    private String[] splitCsvLine(String line) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        int i = 0;

        while (i < line.length()) {
            char c = line.charAt(i);

            if (inQuotes) {
                if (c == '"') {
                    // 检查是否是转义的双引号 ""
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i += 2; // 跳过两个引号
                    } else {
                        // 引号字段结束
                        inQuotes = false;
                        i++;
                    }
                } else {
                    current.append(c);
                    i++;
                }
            } else {
                if (c == '"') {
                    // 引号字段开始
                    inQuotes = true;
                    i++;
                } else if (c == ',') {
                    parts.add(current.toString());
                    current = new StringBuilder();
                    i++;
                } else {
                    current.append(c);
                    i++;
                }
            }
        }
        parts.add(current.toString());
        return parts.toArray(new String[0]);
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

    private String generateBar(double ratio) {
        int filled = (int) (ratio * 10);
        StringBuilder sb = new StringBuilder("▐");
        for (int i = 0; i < 10; i++) sb.append(i < filled ? "█" : "░");
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
        log.info("║          CMMLU 中文多任务语言理解 Demo (真实LLM版)                 ║");
        log.info("║   使用 Ollama 本地大模型 + HuggingFace 官方数据集                   ║");
        log.info("║                                                                    ║");
        log.info("║   67个主题 | 侧重中国文化特有知识 | 按大类汇总                     ║");
        log.info("║   随机抽取20题，使用LLM推理并评测准确率                            ║");
        log.info("╚════════════════════════════════════════════════════════════════════╝");
        log.info("");
    }
}
