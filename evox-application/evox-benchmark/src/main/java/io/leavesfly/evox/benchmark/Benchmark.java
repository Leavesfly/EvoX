package io.leavesfly.evox.benchmark;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 基准测试抽象基类
 * 提供数据加载、评估等通用功能
 *
 * @author EvoX Team
 */
@Slf4j
@Data
public abstract class Benchmark<T, R> {

    /**
     * 基准测试名称
     */
    protected String name;

    /**
     * 数据路径
     */
    protected String path;

    /**
     * 训练数据
     */
    protected List<T> trainData;

    /**
     * 验证数据
     */
    protected List<T> devData;

    /**
     * 测试数据
     */
    protected List<T> testData;

    /**
     * 构造函数
     */
    public Benchmark(String name, String path) {
        this.name = name;
        this.path = path;
        loadData();
    }

    /**
     * 加载数据
     */
    protected abstract void loadData();

    /**
     * 获取样本ID
     */
    public abstract String getId(T example);

    /**
     * 获取标签
     */
    public abstract R getLabel(T example);

    /**
     * 评估单个预测
     * @return 评估指标Map (如 accuracy, f1_score等)
     */
    public abstract Map<String, Double> evaluate(Object prediction, R label);

    /**
     * 评估单个预测的便捷方法
     * @return 评估指标Map
     */
    public Map<String, Double> evaluateSingle(Object prediction, R label) {
        return evaluate(prediction, label);
    }

    /**
     * 获取训练数据
     */
    public List<T> getTrainData() {
        if (trainData == null) {
            log.warn("Train data for {} is not loaded", name);
            return Collections.emptyList();
        }
        return trainData;
    }

    /**
     * 获取验证数据
     */
    public List<T> getDevData() {
        if (devData == null) {
            log.warn("Dev data for {} is not loaded", name);
            return Collections.emptyList();
        }
        return devData;
    }

    /**
     * 获取测试数据
     */
    public List<T> getTestData() {
        if (testData == null) {
            log.warn("Test data for {} is not loaded", name);
            return Collections.emptyList();
        }
        return testData;
    }

    /**
     * 根据索引获取样本
     */
    public T getExampleByIndex(int index, String mode) {
        List<T> data = getDataByMode(mode);
        if (index < 0 || index >= data.size()) {
            return null;
        }
        return data.get(index);
    }

    /**
     * 根据ID获取样本
     */
    public T getExampleById(String exampleId, String mode) {
        List<T> data = getDataByMode(mode);
        return data.stream()
                .filter(example -> getId(example).equals(exampleId))
                .findFirst()
                .orElse(null);
    }

    /**
     * 根据模式获取数据
     */
    protected List<T> getDataByMode(String mode) {
        switch (mode.toLowerCase()) {
            case "train":
                return getTrainData();
            case "dev":
                return getDevData();
            case "test":
                return getTestData();
            default:
                throw new IllegalArgumentException("Invalid mode: " + mode);
        }
    }

    /**
     * 随机采样
     */
    public List<T> sampleData(List<T> data, int sampleSize, Long seed) {
        if (sampleSize >= data.size()) {
            return new ArrayList<>(data);
        }

        Random random = seed != null ? new Random(seed) : new Random();
        List<T> shuffled = new ArrayList<>(data);
        Collections.shuffle(shuffled, random);
        return shuffled.subList(0, sampleSize);
    }
}
