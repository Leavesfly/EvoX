package io.leavesfly.evox.evaluation.dataset;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 简单的内存评估数据集实现。
 * <p>
 * 该类将数据集存储在内存中，适用于小规模数据集或测试场景。
 * 使用 Lombok 注解简化代码，支持 Builder 模式构建对象。
 * </p>
 *
 * @author EvoX Team
 * @since 1.0.0
 */
@Data
@Builder
@Slf4j
public class SimpleEvaluationDataset implements EvaluationDataset {

    private String name;
    private List<Map<String, Object>> inputs;
    private List<Object> labels;

    @Override
    public int size() {
        return inputs.size();
    }

    @Override
    public Map<String, Object> getInput(int index) {
        if (index < 0 || index >= inputs.size()) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for size " + inputs.size());
        }
        return inputs.get(index);
    }

    @Override
    public Object getLabel(int index) {
        if (index < 0 || index >= labels.size()) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for size " + labels.size());
        }
        return labels.get(index);
    }

    /**
     * 使用输入列表和标签列表创建简单评估数据集。
     * <p>
     * 该方法会验证 inputs 和 labels 的大小是否一致，如果不一致将抛出 IllegalArgumentException。
     * </p>
     *
     * @param name   数据集名称
     * @param inputs 输入列表
     * @param labels 标签列表
     * @return 简单评估数据集实例
     * @throws IllegalArgumentException 如果 inputs 和 labels 大小不一致
     */
    public static SimpleEvaluationDataset of(String name, List<Map<String, Object>> inputs, List<Object> labels) {
        if (inputs == null || labels == null) {
            throw new IllegalArgumentException("Inputs and labels cannot be null");
        }
        if (inputs.size() != labels.size()) {
            throw new IllegalArgumentException(
                "Inputs and labels must have the same size. Inputs: " + inputs.size() + ", Labels: " + labels.size()
            );
        }
        log.debug("Creating SimpleEvaluationDataset '{}' with {} samples", name, inputs.size());
        return SimpleEvaluationDataset.builder()
            .name(name)
            .inputs(new ArrayList<>(inputs))
            .labels(new ArrayList<>(labels))
            .build();
    }

    /**
     * 使用输入标签对列表创建简单评估数据集。
     * <p>
     * 该方法从 Map.Entry 列表中提取输入和标签，构建数据集。
     * </p>
     *
     * @param name  数据集名称
     * @param pairs 输入标签对列表
     * @return 简单评估数据集实例
     * @throws IllegalArgumentException 如果 pairs 为 null
     */
    public static SimpleEvaluationDataset fromPairs(String name, List<Map.Entry<Map<String, Object>, Object>> pairs) {
        if (pairs == null) {
            throw new IllegalArgumentException("Pairs cannot be null");
        }
        List<Map<String, Object>> inputs = new ArrayList<>(pairs.size());
        List<Object> labels = new ArrayList<>(pairs.size());
        
        for (Map.Entry<Map<String, Object>, Object> pair : pairs) {
            inputs.add(pair.getKey());
            labels.add(pair.getValue());
        }
        
        log.debug("Creating SimpleEvaluationDataset '{}' from {} pairs", name, pairs.size());
        return SimpleEvaluationDataset.builder()
            .name(name)
            .inputs(inputs)
            .labels(labels)
            .build();
    }
}
