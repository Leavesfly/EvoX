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
 * GSM8K基准测试
 * Grade School Math 8K - 小学数学问题数据集
 *
 * @author EvoX Team
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class GSM8K extends Benchmark<GSM8K.GSM8KExample, String> {

    public GSM8K(String path) {
        super("GSM8K", path);
    }

    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void loadData() {
        log.info("Loading GSM8K dataset from: {}", path);
        
        try {
            File dataFile = new File(path);
            if (!dataFile.exists()) {
                log.warn("GSM8K data file not found: {}", path);
                this.trainData = new ArrayList<>();
                this.testData = new ArrayList<>();
                return;
            }

            // 读取JSONL格式数据
            List<String> lines = Files.readAllLines(Paths.get(path));
            List<GSM8KExample> allExamples = new ArrayList<>();
            
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) continue;
                
                try {
                    JsonNode node = objectMapper.readTree(line);
                    GSM8KExample example = new GSM8KExample();
                    example.id = String.valueOf(i);
                    example.question = node.get("question").asText();
                    example.answer = node.get("answer").asText();
                    allExamples.add(example);
                } catch (Exception e) {
                    log.warn("Failed to parse line {}: {}", i, e.getMessage());
                }
            }
            
            // 按8:2分割训练集和测试集
            int splitIndex = (int) (allExamples.size() * 0.8);
            this.trainData = new ArrayList<>(allExamples.subList(0, splitIndex));
            this.testData = new ArrayList<>(allExamples.subList(splitIndex, allExamples.size()));
            
            log.info("Loaded {} training examples and {} test examples", 
                    trainData.size(), testData.size());
            
        } catch (IOException e) {
            log.error("Error loading GSM8K data", e);
            this.trainData = new ArrayList<>();
            this.testData = new ArrayList<>();
        }
    }

    @Override
    public String getId(GSM8KExample example) {
        return example.id;
    }

    @Override
    public String getLabel(GSM8KExample example) {
        return example.answer;
    }

    @Override
    public Map<String, Double> evaluate(Object prediction, String label) {
        Map<String, Double> metrics = new HashMap<>();
        
        // 提取数值答案进行比较
        String predAnswer = extractAnswer(prediction.toString());
        String labelAnswer = extractAnswer(label);
        
        double accuracy = predAnswer.equals(labelAnswer) ? 1.0 : 0.0;
        metrics.put("accuracy", accuracy);
        
        return metrics;
    }

    /**
     * 从答案文本中提取数值
     * GSM8K答案格式: "推理过程\n#### 最终答案"
     */
    private String extractAnswer(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        // GSM8K格式: 答案在 #### 之后
        String[] parts = text.split("####");
        if (parts.length > 1) {
            String answer = parts[parts.length - 1].trim();
            // 移除逗号和空格,只保留数字和小数点
            return answer.replaceAll("[^0-9.-]", "");
        }
        
        // 如果没有####标记,尝试提取文本中的数字
        String cleaned = text.replaceAll("[^0-9.-]", "");
        return cleaned.isEmpty() ? text.trim() : cleaned;
    }

    /**
     * GSM8K样本数据结构
     */
    @Data
    public static class GSM8KExample {
        private String id;
        private String question;
        private String answer;
    }
}
