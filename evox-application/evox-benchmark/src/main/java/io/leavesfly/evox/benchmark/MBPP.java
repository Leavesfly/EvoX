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
 * MBPP基准测试
 * Mostly Basic Python Programming - Python编程基础测试
 *
 * @author EvoX Team
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class MBPP extends Benchmark<MBPP.MBPPExample, String> {

    private ObjectMapper objectMapper = new ObjectMapper();
    
    public MBPP(String path) {
        super("MBPP", path);
    }

    @Override
    protected void loadData() {
        log.info("Loading MBPP dataset from: {}", path);
        
        try {
            File dataFile = new File(path);
            if (!dataFile.exists()) {
                log.warn("MBPP data file not found: {}", path);
                this.trainData = new ArrayList<>();
                this.testData = new ArrayList<>();
                return;
            }
            
            // 读取JSONL格式数据
            List<String> lines = Files.readAllLines(Paths.get(path));
            List<MBPPExample> examples = new ArrayList<>();
            
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) continue;
                
                try {
                    JsonNode node = objectMapper.readTree(line);
                    MBPPExample example = new MBPPExample();
                    example.taskId = node.get("task_id").asInt();
                    example.text = node.get("text").asText();
                    example.code = node.get("code").asText();
                    
                    // 解析测试用例
                    List<String> testList = new ArrayList<>();
                    if (node.has("test_list")) {
                        JsonNode testListNode = node.get("test_list");
                        if (testListNode.isArray()) {
                            for (JsonNode testNode : testListNode) {
                                testList.add(testNode.asText());
                            }
                        }
                    }
                    example.testList = testList;
                    
                    if (node.has("test_setup_code")) {
                        example.testSetup = node.get("test_setup_code").asText();
                    }
                    if (node.has("challenge_test_list")) {
                        example.challenge = node.get("challenge_test_list").asText();
                    }
                    
                    examples.add(example);
                } catch (Exception e) {
                    log.warn("Failed to parse line {}: {}", i, e.getMessage());
                }
            }
            
            // 简单分割为训练集和测试集
            int splitIndex = (int) (examples.size() * 0.8);
            this.trainData = examples.subList(0, splitIndex);
            this.testData = examples.subList(splitIndex, examples.size());
            
            log.info("Loaded {} train and {} test MBPP examples", 
                    trainData.size(), testData.size());
            
        } catch (IOException e) {
            log.error("Error loading MBPP data", e);
            this.trainData = new ArrayList<>();
            this.testData = new ArrayList<>();
        }
    }

    @Override
    public String getId(MBPPExample example) {
        return String.valueOf(example.taskId);
    }

    @Override
    public String getLabel(MBPPExample example) {
        return example.code;
    }

    @Override
    public Map<String, Double> evaluate(Object prediction, String label) {
        Map<String, Double> metrics = new HashMap<>();
        
        // 简化的代码评估
        String predCode = extractCode(prediction.toString());
        String labelCode = extractCode(label);
        
        // 计算相似度
        double similarity = calculateSimilarity(predCode, labelCode);
        double passRate = similarity > 0.7 ? 1.0 : 0.0;
        
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
     * MBPP样本数据结构
     */
    @Data
    public static class MBPPExample {
        private int taskId;
        private String text;
        private String code;
        private List<String> testList;
        private String testSetup;
        private String challenge;
    }
}
