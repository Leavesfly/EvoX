package io.leavesfly.evox.optimizers.evaluators.metrics;

import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * F1分数指标
 * 计算精确率(Precision)和召回率(Recall)的调和平均值
 * 适用于分类任务
 *
 * @author EvoX Team
 */
@Slf4j
public class F1ScoreMetric implements EvaluationMetric {

    /**
     * 正类标签（用于二分类）
     */
    private final String positiveLabel;

    /**
     * 默认构造函数（正类标签为"1"或"true"）
     */
    public F1ScoreMetric() {
        this.positiveLabel = "1";
    }

    /**
     * 指定正类标签的构造函数
     *
     * @param positiveLabel 正类标签
     */
    public F1ScoreMetric(String positiveLabel) {
        this.positiveLabel = positiveLabel;
    }

    @Override
    public double compute(Object prediction, Object label) {
        if (prediction == null || label == null) {
            log.warn("Prediction or label is null, returning 0.0");
            return 0.0;
        }

        try {
            // 对于二分类
            String predStr = normalizeString(prediction.toString());
            String labelStr = normalizeString(label.toString());

            boolean predIsPositive = isPositive(predStr);
            boolean labelIsPositive = isPositive(labelStr);

            // 计算TP, FP, FN
            int truePositive = (predIsPositive && labelIsPositive) ? 1 : 0;
            int falsePositive = (predIsPositive && !labelIsPositive) ? 1 : 0;
            int falseNegative = (!predIsPositive && labelIsPositive) ? 1 : 0;

            return computeF1(truePositive, falsePositive, falseNegative);

        } catch (Exception e) {
            log.error("Error computing F1 score", e);
            return 0.0;
        }
    }

    @Override
    public String getName() {
        return "f1_score";
    }

    /**
     * 计算F1分数
     *
     * @param tp 真阳性
     * @param fp 假阳性
     * @param fn 假阴性
     * @return F1分数
     */
    private double computeF1(int tp, int fp, int fn) {
        if (tp == 0) {
            return 0.0;
        }

        double precision = (double) tp / (tp + fp);
        double recall = (double) tp / (tp + fn);

        if (precision + recall == 0) {
            return 0.0;
        }

        return 2 * (precision * recall) / (precision + recall);
    }

    /**
     * 判断是否为正类
     */
    private boolean isPositive(String value) {
        return value.equals(normalizeString(positiveLabel)) ||
               value.equals("true") ||
               value.equals("yes") ||
               value.equals("1");
    }

    /**
     * 规范化字符串
     */
    private String normalizeString(String str) {
        if (str == null) {
            return "";
        }
        return str.trim().toLowerCase();
    }

    /**
     * 批量计算F1分数（用于多样本）
     *
     * @param predictions 预测列表
     * @param labels 标签列表
     * @return F1分数
     */
    public double computeBatch(List<Object> predictions, List<Object> labels) {
        if (predictions == null || labels == null || predictions.size() != labels.size()) {
            log.warn("Invalid input for batch F1 computation");
            return 0.0;
        }

        int tp = 0, fp = 0, fn = 0;

        for (int i = 0; i < predictions.size(); i++) {
            String predStr = normalizeString(predictions.get(i).toString());
            String labelStr = normalizeString(labels.get(i).toString());

            boolean predIsPositive = isPositive(predStr);
            boolean labelIsPositive = isPositive(labelStr);

            if (predIsPositive && labelIsPositive) {
                tp++;
            } else if (predIsPositive && !labelIsPositive) {
                fp++;
            } else if (!predIsPositive && labelIsPositive) {
                fn++;
            }
        }

        return computeF1(tp, fp, fn);
    }
}
