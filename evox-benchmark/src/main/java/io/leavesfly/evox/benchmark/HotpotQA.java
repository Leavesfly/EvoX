package io.leavesfly.evox.benchmark;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

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

    public HotpotQA(String path) {
        super("HotpotQA", path);
    }

    @Override
    protected void loadData() {
        log.info("Loading HotpotQA dataset from: {}", path);
        // TODO: 实现数据加载逻辑
        this.trainData = new ArrayList<>();
        this.devData = new ArrayList<>();
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
