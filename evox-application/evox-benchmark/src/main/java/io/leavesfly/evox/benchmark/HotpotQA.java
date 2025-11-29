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
 * HotpotQA基准测试
 * 多跳问答数据集
 *
 * @author EvoX Team
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class HotpotQA extends Benchmark<HotpotQA.HotpotQAExample, String> {

    private ObjectMapper objectMapper = new ObjectMapper();
    
    public HotpotQA(String path) {
        super("HotpotQA", path);
    }

    @Override
    protected void loadData() {
        log.info("Loading HotpotQA dataset from: {}", path);
        
        try {
            File dataFile = new File(path);
            if (!dataFile.exists()) {
                log.warn("HotpotQA data file not found: {}", path);
                this.trainData = new ArrayList<>();
                this.devData = new ArrayList<>();
                return;
            }
            
            // 读取JSON格式数据
            List<String> lines = Files.readAllLines(Paths.get(path));
            List<HotpotQAExample> examples = new ArrayList<>();
            
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) continue;
                
                try {
                    JsonNode node = objectMapper.readTree(line);
                    HotpotQAExample example = new HotpotQAExample();
                    example.id = node.get("_id").asText();
                    example.question = node.get("question").asText();
                    example.answer = node.get("answer").asText();
                    example.type = node.has("type") ? node.get("type").asText() : "unknown";
                    
                    // 解析supporting facts
                    List<String> facts = new ArrayList<>();
                    if (node.has("supporting_facts")) {
                        JsonNode factsNode = node.get("supporting_facts");
                        if (factsNode.isArray()) {
                            for (JsonNode factNode : factsNode) {
                                if (factNode.isArray() && factNode.size() >= 2) {
                                    facts.add(factNode.get(0).asText() + ": " + factNode.get(1).asText());
                                }
                            }
                        }
                    }
                    example.supportingFacts = facts;
                    
                    examples.add(example);
                } catch (Exception e) {
                    log.warn("Failed to parse line {}: {}", i, e.getMessage());
                }
            }
            
            // 简单分割为训练集和开发集
            int splitIndex = (int) (examples.size() * 0.9);
            this.trainData = examples.subList(0, splitIndex);
            this.devData = examples.subList(splitIndex, examples.size());
            
            log.info("Loaded {} train and {} dev HotpotQA examples", 
                    trainData.size(), devData.size());
            
        } catch (IOException e) {
            log.error("Error loading HotpotQA data", e);
            this.trainData = new ArrayList<>();
            this.devData = new ArrayList<>();
        }
    }

    @Override
    public String getId(HotpotQAExample example) {
        return example.id;
    }

    @Override
    public String getLabel(HotpotQAExample example) {
        return example.answer;
    }

    @Override
    public Map<String, Double> evaluate(Object prediction, String label) {
        Map<String, Double> metrics = new HashMap<>();
        
        String pred = normalize(prediction.toString());
        String truth = normalize(label);
        
        // Exact Match
        double em = pred.equals(truth) ? 1.0 : 0.0;
        
        // F1 Score (基于token overlap)
        double f1 = calculateF1(pred, truth);
        
        metrics.put("exact_match", em);
        metrics.put("f1_score", f1);
        
        return metrics;
    }

    private String normalize(String text) {
        return text.toLowerCase().trim();
    }

    private double calculateF1(String prediction, String label) {
        Set<String> predTokens = new HashSet<>(Arrays.asList(prediction.split("\\s+")));
        Set<String> labelTokens = new HashSet<>(Arrays.asList(label.split("\\s+")));
        
        Set<String> common = new HashSet<>(predTokens);
        common.retainAll(labelTokens);
        
        if (common.isEmpty()) {
            return 0.0;
        }
        
        double precision = (double) common.size() / predTokens.size();
        double recall = (double) common.size() / labelTokens.size();
        
        return 2 * precision * recall / (precision + recall);
    }

    /**
     * HotpotQA样本数据结构
     */
    @Data
    public static class HotpotQAExample {
        private String id;
        private String question;
        private String answer;
        private String type;
        private List<String> supportingFacts;
    }
}
