package io.leavesfly.evox.evaluation.dataset;

/**
 * 数据集分割类型枚举。
 * <p>
 * 该枚举定义了评估数据集的常见分割方式，包括训练集、开发集和测试集。
 * 每个枚举值都有一个对应的标签字符串，用于标识数据集的用途。
 * </p>
 *
 * @author EvoX Team
 * @since 1.0.0
 */
public enum DatasetSplit {

    /**
     * 训练集，用于模型训练。
     */
    TRAIN("train"),

    /**
     * 开发集（验证集），用于模型调优和超参数选择。
     */
    DEV("dev"),

    /**
     * 测试集，用于最终模型评估。
     */
    TEST("test");

    private final String label;

    /**
     * 构造数据集分割枚举值。
     *
     * @param label 数据集分割的标签标识
     */
    DatasetSplit(String label) {
        this.label = label;
    }

    /**
     * 获取数据集分割的标签标识。
     *
     * @return 标签字符串
     */
    public String getLabel() {
        return label;
    }
}
