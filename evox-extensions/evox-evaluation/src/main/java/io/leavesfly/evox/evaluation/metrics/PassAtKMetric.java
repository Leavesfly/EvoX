package io.leavesfly.evox.evaluation.metrics;

import lombok.extern.slf4j.Slf4j;

/**
 * Pass@K指标
 * <p>
 * 适用于MBPP等代码生成任务，评估生成的代码是否通过了测试用例。
 * 通过检查预测代码中是否包含标签中的所有关键内容来判断是否通过。
 * </p>
 * <p>
 * 计算逻辑：
 * <ul>
 *   <li>如果prediction和label都不为null，将两者转为字符串并trim</li>
 *   <li>将label按换行符分割成多行，检查每一行是否都能在prediction中找到</li>
 *   <li>如果所有行都能在prediction中找到，返回1.0，否则返回0.0</li>
 * </ul>
 * </p>
 *
 * @author EvoX Team
 * @version 1.0
 */
@Slf4j
public class PassAtKMetric implements EvaluationMetric {

    private final int k;

    /**
     * 构造函数，使用默认K值1
     */
    public PassAtKMetric() {
        this.k = 1;
    }

    /**
     * 构造函数，指定K值
     *
     * @param k K值，表示在K次尝试中至少成功一次
     */
    public PassAtKMetric(int k) {
        this.k = k;
    }

    @Override
    public double compute(Object prediction, Object label) {
        if (prediction == null || label == null) {
            return 0.0;
        }

        String predictionStr = prediction.toString().trim();
        String labelStr = label.toString().trim();

        if (predictionStr.isEmpty() || labelStr.isEmpty()) {
            return 0.0;
        }

        String[] labelLines = labelStr.split("\\r?\\n");
        
        for (String line : labelLines) {
            String trimmedLine = line.trim();
            if (!trimmedLine.isEmpty() && !predictionStr.contains(trimmedLine)) {
                return 0.0;
            }
        }

        return 1.0;
    }

    @Override
    public String getName() {
        return "pass_at_" + k;
    }

    @Override
    public String getDescription() {
        return "Pass@K metric for code generation tasks, checks if all key content from label is present in prediction";
    }

    /**
     * 获取K值
     *
     * @return K值
     */
    public int getK() {
        return k;
    }
}
