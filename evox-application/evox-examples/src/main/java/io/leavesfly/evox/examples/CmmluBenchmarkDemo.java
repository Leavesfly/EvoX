package io.leavesfly.evox.examples;

import io.leavesfly.evox.benchmark.CMMLU;
import io.leavesfly.evox.benchmark.CMMLU.CMMUExample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * CMMLU 中文多任务语言理解评测集 Demo
 * <p>
 * 演示如何使用 CMMLU 基准测试评估模型在中文语境下的知识和推理能力。
 * 本示例内置了20个覆盖中国特色文化知识的选择题样本，
 * 模拟LLM生成答案后进行评测，打印完整的评测过程和结果。
 * </p>
 * <p>
 * CMMLU 特色：侧重中国文化特有知识（中医、古文、传统文化、驾照考试等），
 * 结果按大类（STEM/社科/人文/中国特色）汇总，体现模型对中国文化的理解程度。
 * </p>
 *
 * @author EvoX Team
 */
public class CmmluBenchmarkDemo {

    private static final Logger log = LoggerFactory.getLogger(CmmluBenchmarkDemo.class);

    public static void main(String[] args) {
        CmmluBenchmarkDemo demo = new CmmluBenchmarkDemo();
        demo.run();
    }

    public void run() {
        printBanner();

        // Step 1: 准备评测数据
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("【Step 1】准备 CMMLU 评测数据（20道中国特色知识多选题）");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        Path tempDataFile = createSampleData();
        if (tempDataFile == null) {
            log.error("创建样本数据失败，退出");
            return;
        }

        // Step 2: 加载 CMMLU 评测集
        log.info("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("【Step 2】加载 CMMLU 评测集");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        CMMLU cmmlu = new CMMLU(tempDataFile.toString());
        log.info("数据集名称: {}", cmmlu.getName());
        log.info("训练集大小: {}", cmmlu.getTrainData().size());
        log.info("验证集大小: {}", cmmlu.getDevData().size());
        log.info("测试集大小: {}", cmmlu.getTestData().size());
        log.info("涵盖主题: {}", cmmlu.getSubjects());

        // 合并所有数据用于演示（数据量小）
        List<CMMUExample> allExamples = new ArrayList<>();
        allExamples.addAll(cmmlu.getTrainData());
        allExamples.addAll(cmmlu.getDevData());
        allExamples.addAll(cmmlu.getTestData());

        int sampleSize = Math.min(20, allExamples.size());
        allExamples = allExamples.subList(0, sampleSize);

        // Step 3: 逐题评测
        log.info("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("【Step 3】模拟 LLM 推理 & 逐题评测（共 {} 题）", sampleSize);
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        List<Object> predictions = new ArrayList<>();
        int correctCount = 0;
        Map<String, List<Boolean>> subjectResults = new LinkedHashMap<>();
        Map<String, List<Boolean>> categoryResults = new LinkedHashMap<>();

        for (int i = 0; i < allExamples.size(); i++) {
            CMMUExample example = allExamples.get(i);

            // 生成 prompt
            String prompt = cmmlu.formatPrompt(example, null);

            // 模拟 LLM 回答
            String llmResponse = simulateLLMResponse(example);

            // 评估
            Map<String, Double> metrics = cmmlu.evaluate(llmResponse, example.getAnswer());
            boolean isCorrect = metrics.get("accuracy") == 1.0;
            if (isCorrect) correctCount++;

            predictions.add(llmResponse);

            // 记录主题和大类维度
            subjectResults.computeIfAbsent(example.getSubject(), k -> new ArrayList<>()).add(isCorrect);
            String category = example.getCategory() != null ? example.getCategory() : "Other";
            categoryResults.computeIfAbsent(category, k -> new ArrayList<>()).add(isCorrect);

            // 打印每题详情
            log.info("\n┌─ 第 {}/{} 题 ─────────────────────────────────────────────────", i + 1, sampleSize);
            log.info("│ 主题: {}  |  大类: {}", example.getSubject(), category);
            log.info("│ 题目: {}", truncate(example.getQuestion(), 55));
            log.info("│ 选项:");
            List<String> choiceLabels = Arrays.asList("A", "B", "C", "D");
            for (int j = 0; j < example.getChoices().size(); j++) {
                String marker = choiceLabels.get(j).equals(example.getAnswer()) ? " ✓" : "";
                log.info("│   {}. {}{}", choiceLabels.get(j), truncate(example.getChoices().get(j), 36), marker);
            }
            log.info("│ LLM回答: {}  |  正确答案: {}  |  结果: {}",
                    extractAnswer(llmResponse), example.getAnswer(),
                    isCorrect ? "✅ 正确" : "❌ 错误");
            log.info("└────────────────────────────────────────────────────────────────");
        }

        // Step 4: 汇总结果
        log.info("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("【Step 4】CMMLU 评测结果汇总");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        double overallAccuracy = (double) correctCount / sampleSize;
        log.info("\n📊 总体结果:");
        log.info("   总题数: {}", sampleSize);
        log.info("   正确数: {}", correctCount);
        log.info("   错误数: {}", sampleSize - correctCount);
        log.info("   准确率: {}", String.format("%.1f%%", overallAccuracy * 100));

        // 按大类统计（CMMLU特色）
        log.info("\n🏷️  按大类分组（CMMLU 核心维度）:");
        log.info("   {}", String.format("%-20s %-6s %-6s %s", "大类", "正确", "总数", "准确率"));
        log.info("   ──────────────────────────────────────────────");
        categoryResults.forEach((cat, results) -> {
            long correct = results.stream().filter(b -> b).count();
            double acc = (double) correct / results.size();
            String bar = generateBar(acc);
            log.info("   {} {}", String.format("%-20s %-6d %-6d %.1f%%", cat, correct, results.size(), acc * 100), bar);
        });

        // 按主题统计
        log.info("\n📚 按主题分组:");
        log.info("   {}", String.format("%-18s %-6s %-6s %s", "主题", "正确", "总数", "准确率"));
        log.info("   ──────────────────────────────────────────────");
        subjectResults.forEach((subject, results) -> {
            long correct = results.stream().filter(b -> b).count();
            double acc = (double) correct / results.size();
            log.info("   {}", String.format("%-18s %-6d %-6d %.1f%%", subject, correct, results.size(), acc * 100));
        });

        // 打印评测总结
        log.info("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("📝 评测总结:");
        log.info("   - CMMLU 重点考察模型对中国文化/知识的理解深度");
        log.info("   - '中国特色'大类涵盖中医、传统文化、驾照考试等独特领域");
        log.info("   - 可据此判断模型的中文本地化能力和知识广度");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("CMMLU 评测完成！\n");

        // 清理临时文件
        try {
            Files.deleteIfExists(tempDataFile);
        } catch (IOException ignored) {
        }
    }

    /**
     * 模拟 LLM 回答
     * 实际使用时替换为真实的 LLM 调用:
     *   String prompt = cmmlu.formatPrompt(example, fewShots);
     *   String response = llmProvider.generate(prompt);
     */
    private String simulateLLMResponse(CMMUExample example) {
        int hash = Math.abs(example.getQuestion().hashCode());
        boolean shouldBeCorrect = (hash % 10) < 6; // 约60%概率答对（CMMLU偏难）

        if (shouldBeCorrect) {
            String[] formats = {
                    "答案是%s",
                    "%s",
                    "正确答案为%s",
                    "经过分析，选%s。",
                    "【%s】"
            };
            String format = formats[hash % formats.length];
            return String.format(format, example.getAnswer());
        } else {
            List<String> wrongChoices = new ArrayList<>(Arrays.asList("A", "B", "C", "D"));
            wrongChoices.remove(example.getAnswer());
            String wrongAnswer = wrongChoices.get(hash % wrongChoices.size());
            return "答案是" + wrongAnswer;
        }
    }

    /**
     * 生成进度条
     */
    private String generateBar(double ratio) {
        int filled = (int) (ratio * 10);
        StringBuilder sb = new StringBuilder("▐");
        for (int i = 0; i < 10; i++) {
            sb.append(i < filled ? "█" : "░");
        }
        sb.append("▌");
        return sb.toString();
    }

    /**
     * 从响应中提取答案字母
     */
    private String extractAnswer(String response) {
        if (response == null || response.isEmpty()) return "?";
        for (char c : response.toUpperCase().toCharArray()) {
            if (c >= 'A' && c <= 'D') return String.valueOf(c);
        }
        return "?";
    }

    /**
     * 截断字符串
     */
    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }

    /**
     * 创建样本数据（20道中国特色知识题目）
     * 涵盖CMMLU五大分类：STEM、社会科学、人文学科、中国特色、其他
     */
    private Path createSampleData() {
        List<String> lines = Arrays.asList(
                // ===== 中国特色 (China Specific) =====
                // 中医基础
                "{\"id\":\"1\",\"question\":\"中医'望闻问切'四诊中，'切'指的是\",\"choices\":[\"观察面色\",\"听声音\",\"询问病史\",\"把脉\"],\"answer\":\"D\",\"subject\":\"中医基础\"}",
                // 中国传统文化
                "{\"id\":\"2\",\"question\":\"'岁寒三友'指的是\",\"choices\":[\"梅兰竹\",\"松竹梅\",\"兰竹菊\",\"松梅菊\"],\"answer\":\"B\",\"subject\":\"中国传统文化\"}",
                // 中国食文化
                "{\"id\":\"3\",\"question\":\"以下哪道菜属于川菜系？\",\"choices\":[\"北京烤鸭\",\"麻婆豆腐\",\"东坡肉\",\"佛跳墙\"],\"answer\":\"B\",\"subject\":\"中国食文化\"}",
                // 驾照考试
                "{\"id\":\"4\",\"question\":\"驾驶机动车在高速公路上行驶，遇有雾天能见度小于200米时，最高车速不得超过\",\"choices\":[\"90公里/时\",\"80公里/时\",\"60公里/时\",\"40公里/时\"],\"answer\":\"C\",\"subject\":\"驾照考试\"}",
                // 中国地理
                "{\"id\":\"5\",\"question\":\"'天府之国'指的是我国哪个地区？\",\"choices\":[\"关中平原\",\"江汉平原\",\"成都平原\",\"华北平原\"],\"answer\":\"C\",\"subject\":\"中国地理\"}",
                // 公务员考试
                "{\"id\":\"6\",\"question\":\"我国行政处罚法规定，限制人身自由的行政处罚只能由哪个机关设定？\",\"choices\":[\"国务院\",\"法律\",\"行政法规\",\"地方性法规\"],\"answer\":\"B\",\"subject\":\"公务员考试\"}",
                // 中国传统文化
                "{\"id\":\"7\",\"question\":\"中国传统节日中，重阳节的习俗是\",\"choices\":[\"吃粽子\",\"赏月\",\"登高\",\"放鞭炮\"],\"answer\":\"C\",\"subject\":\"中国传统文化\"}",

                // ===== 人文学科 (Humanities) =====
                // 中国历史
                "{\"id\":\"8\",\"question\":\"'丝绸之路'最早开辟于哪个朝代？\",\"choices\":[\"秦朝\",\"汉朝\",\"唐朝\",\"宋朝\"],\"answer\":\"B\",\"subject\":\"中国历史\"}",
                // 中国文学
                "{\"id\":\"9\",\"question\":\"《红楼梦》的作者是\",\"choices\":[\"施耐庵\",\"罗贯中\",\"曹雪芹\",\"吴承恩\"],\"answer\":\"C\",\"subject\":\"中国文学\"}",
                // 哲学
                "{\"id\":\"10\",\"question\":\"'知行合一'是哪位思想家提出的？\",\"choices\":[\"朱熹\",\"王阳明\",\"孔子\",\"老子\"],\"answer\":\"B\",\"subject\":\"哲学\"}",
                // 中国历史
                "{\"id\":\"11\",\"question\":\"科举制度正式确立于哪个朝代？\",\"choices\":[\"汉朝\",\"隋朝\",\"唐朝\",\"宋朝\"],\"answer\":\"B\",\"subject\":\"中国历史\"}",

                // ===== STEM =====
                // 数学
                "{\"id\":\"12\",\"question\":\"等差数列{an}中，a1=2，d=3，则a10等于\",\"choices\":[\"27\",\"29\",\"30\",\"32\"],\"answer\":\"B\",\"subject\":\"数学\"}",
                // 物理
                "{\"id\":\"13\",\"question\":\"以下哪种现象说明光具有粒子性？\",\"choices\":[\"光的干涉\",\"光的衍射\",\"光电效应\",\"光的偏振\"],\"answer\":\"C\",\"subject\":\"物理\"}",
                // 化学
                "{\"id\":\"14\",\"question\":\"下列气体中，能使品红溶液褪色的是\",\"choices\":[\"CO2\",\"SO2\",\"N2\",\"O2\"],\"answer\":\"B\",\"subject\":\"化学\"}",
                // 生物
                "{\"id\":\"15\",\"question\":\"人体最大的消化腺是\",\"choices\":[\"胃\",\"肝脏\",\"胰腺\",\"唾液腺\"],\"answer\":\"B\",\"subject\":\"生物\"}",

                // ===== 社会科学 (Social Science) =====
                // 经济学
                "{\"id\":\"16\",\"question\":\"GDP是衡量一个国家经济规模的重要指标，GDP的中文全称是\",\"choices\":[\"国民生产总值\",\"国内生产总值\",\"国民收入\",\"财政收入\"],\"answer\":\"B\",\"subject\":\"经济学\"}",
                // 法学
                "{\"id\":\"17\",\"question\":\"我国宪法规定，中华人民共和国的一切权力属于\",\"choices\":[\"全国人大\",\"人民\",\"中国共产党\",\"国务院\"],\"answer\":\"B\",\"subject\":\"法学\"}",
                // 心理学
                "{\"id\":\"18\",\"question\":\"马斯洛需求层次理论中，最高层次的需求是\",\"choices\":[\"安全需求\",\"社交需求\",\"尊重需求\",\"自我实现需求\"],\"answer\":\"D\",\"subject\":\"心理学\"}",
                // 教育学
                "{\"id\":\"19\",\"question\":\"'因材施教'这一教育原则最早由谁提出？\",\"choices\":[\"孟子\",\"孔子\",\"荀子\",\"韩非子\"],\"answer\":\"B\",\"subject\":\"教育学\"}",
                // 管理学
                "{\"id\":\"20\",\"question\":\"PDCA循环中的C代表\",\"choices\":[\"计划(Plan)\",\"执行(Do)\",\"检查(Check)\",\"处理(Act)\"],\"answer\":\"C\",\"subject\":\"管理学\"}"
        );

        try {
            Path tempFile = Files.createTempFile("cmmlu_sample_", ".jsonl");
            Files.write(tempFile, lines);
            log.info("已创建样本数据文件: {}", tempFile);
            log.info("样本数量: {} 道题", lines.size());
            log.info("覆盖大类:");
            log.info("   - 中国特色 (China Specific): 中医基础、中国传统文化、中国食文化、驾照考试、中国地理、公务员考试");
            log.info("   - 人文学科 (Humanities): 中国历史、中国文学、哲学");
            log.info("   - STEM: 数学、物理、化学、生物");
            log.info("   - 社会科学 (Social Science): 经济学、法学、心理学、教育学、管理学");
            return tempFile;
        } catch (IOException e) {
            log.error("创建临时文件失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 打印Banner
     */
    private void printBanner() {
        log.info("");
        log.info("╔════════════════════════════════════════════════════════════════════╗");
        log.info("║                CMMLU 中文多任务语言理解 Demo                       ║");
        log.info("║  Chinese Massive Multitask Language Understanding                  ║");
        log.info("║  67个主题 | 侧重中国文化特有知识                                   ║");
        log.info("║                                                                    ║");
        log.info("║  本Demo抽取20道题，覆盖中医、传统文化、驾照、历史、STEM等          ║");
        log.info("║  演示评测流程：数据加载 → Prompt生成 → LLM推理 → 分类评估         ║");
        log.info("╚════════════════════════════════════════════════════════════════════╝");
        log.info("");
    }
}
