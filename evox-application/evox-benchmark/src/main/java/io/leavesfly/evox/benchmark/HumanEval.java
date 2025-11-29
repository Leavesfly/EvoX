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

/**
 * HumanEval基准测试
 * 代码生成评估数据集
 *
 * @author EvoX Team
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class HumanEval extends Benchmark<HumanEval.HumanEvalExample, String> {

    public HumanEval(String path) {
        super("HumanEval", path);
    }

    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void loadData() {
        log.info("Loading HumanEval dataset from: {}", path);
        
        try {
            File dataFile = new File(path);
            if (!dataFile.exists()) {
                log.warn("HumanEval data file not found: {}", path);
                this.testData = new ArrayList<>();
                return;
            }

            // 读取JSONL格式数据
            List<String> lines = Files.readAllLines(Paths.get(path));
            List<HumanEvalExample> examples = new ArrayList<>();
            
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) continue;
                
                try {
                    JsonNode node = objectMapper.readTree(line);
                    HumanEvalExample example = new HumanEvalExample();
                    example.taskId = node.get("task_id").asText();
                    example.prompt = node.get("prompt").asText();
                    example.canonicalSolution = node.get("canonical_solution").asText();
                    example.testCases = node.get("test").asText();
                    example.entryPoint = node.get("entry_point").asText();
                    examples.add(example);
                } catch (Exception e) {
                    log.warn("Failed to parse line {}: {}", i, e.getMessage());
                }
            }
            
            this.testData = examples;
            log.info("Loaded {} HumanEval test examples", testData.size());
            
        } catch (IOException e) {
            log.error("Error loading HumanEval data", e);
            this.testData = new ArrayList<>();
        }
    }

    @Override
    public String getId(HumanEvalExample example) {
        return example.taskId;
    }

    @Override
    public String getLabel(HumanEvalExample example) {
        return example.canonicalSolution;
    }

    @Override
    public Map<String, Double> evaluate(Object prediction, String label) {
        Map<String, Double> metrics = new HashMap<>();
        
        // 代码评估需要执行测试用例
        // TODO: 实现代码执行和测试逻辑
        // 简化版本: 比较代码相似度
        String predCode = extractCode(prediction.toString());
        String labelCode = extractCode(label);
        
        // 简单的相似度计算
        double similarity = calculateSimilarity(predCode, labelCode);
        double passRate = similarity > 0.8 ? 1.0 : 0.0;
        
        metrics.put("pass_rate", passRate);
        metrics.put("code_similarity", similarity);
        
        return metrics;
    }

    /**
     * 从响应中提取代码
     */
    private String extractCode(String text) {
        if (text == null) return "";
        
        // 移除markdown代码块标记
        text = text.replaceAll("```python\\n", "");
        text = text.replaceAll("```\\n", "");
        text = text.replaceAll("```", "");
        
        return text.trim();
    }

    /**
     * 计算代码相似度
     */
    private double calculateSimilarity(String code1, String code2) {
        if (code1.equals(code2)) return 1.0;
        
        // 使用简单的编辑距离
        int distance = levenshteinDistance(code1, code2);
        int maxLen = Math.max(code1.length(), code2.length());
        
        return maxLen == 0 ? 1.0 : 1.0 - ((double) distance / maxLen);
    }

    /**
     * 计算编辑距离
     */
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(dp[i - 1][j - 1], 
                                           Math.min(dp[i - 1][j], dp[i][j - 1]));
                }
            }
        }
        
        return dp[s1.length()][s2.length()];
    }

    /**
     * HumanEval样本数据结构
     */
    @Data
    public static class HumanEvalExample {
        private String taskId;
        private String prompt;
        private String canonicalSolution;
        private String testCases;
        private String entryPoint;
    }
}
