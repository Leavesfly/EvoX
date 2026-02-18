package io.leavesfly.evox.evaluation.metrics;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * F1分数指标
 * <p>
 * 计算预测结果与真实标签之间的F1分数，适用于HotPotQA等QA任务。
 * F1分数是精确率和召回率的调和平均值，综合评估预测结果的准确性。
 * </p>
 * <p>
 * 计算逻辑：
 * <ul>
 *   <li>将prediction和label都转换为字符串，转为小写并去除首尾空格</li>
 *   <li>按空格分词得到token集合</li>
 *   <li>计算token级别的precision、recall和f1</li>
 *   <li>如果prediction或label为null，返回0.0</li>
 * </ul>
 * </p>
 *
 * @author EvoX Team
 * @version 1.0
 */
@Slf4j
public class F1ScoreMetric implements EvaluationMetric {

    @Override
    public double compute(Object prediction, Object label) {
        if (prediction == null || label == null) {
            return 0.0;
        }

        String predictionStr = prediction.toString().toLowerCase().trim();
        String labelStr = label.toString().toLowerCase().trim();

        if (predictionStr.isEmpty() || labelStr.isEmpty()) {
            return 0.0;
        }

        Set<String> predictionTokens = new HashSet<>(Arrays.asList(predictionStr.split("\\s+")));
        Set<String> labelTokens = new HashSet<>(Arrays.asList(labelStr.split("\\s+")));

        if (labelTokens.isEmpty()) {
            return 0.0;
        }

        Set<String> intersection = new HashSet<>(predictionTokens);
        intersection.retainAll(labelTokens);

        int truePositives = intersection.size();
        int falsePositives = predictionTokens.size() - truePositives;
        int falseNegatives = labelTokens.size() - truePositives;

        if (truePositives == 0) {
            return 0.0;
        }

        double precision = (double) truePositives / (truePositives + falsePositives);
        double recall = (double) truePositives / (truePositives + falseNegatives);

        if (precision + recall == 0) {
            return 0.0;
        }

        return 2.0 * precision * recall / (precision + recall);
    }

    @Override
    public String getName() {
        return "f1_score";
    }

    @Override
    public String getDescription() {
        return "F1 Score metric for QA tasks, calculates the harmonic mean of precision and recall at token level";
    }
}
