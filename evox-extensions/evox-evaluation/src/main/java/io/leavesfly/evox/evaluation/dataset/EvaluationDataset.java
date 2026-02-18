package io.leavesfly.evox.evaluation.dataset;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;

/**
 * 评估数据集接口，定义了评估数据集的基本操作。
 * <p>
 * 该接口用于统一管理评估数据集，提供了获取数据集名称、大小、样本输入和标签等方法。
 * 实现该接口的类可以支持不同类型的数据集，如内存数据集、文件数据集等。
 * </p>
 *
 * @author EvoX Team
 * @since 1.0.0
 */
public interface EvaluationDataset {

    /**
     * 获取数据集名称。
     *
     * @return 数据集名称
     */
    String getName();

    /**
     * 获取数据集大小（样本数量）。
     *
     * @return 数据集大小
     */
    int size();

    /**
     * 获取指定索引位置的样本输入。
     *
     * @param index 样本索引，从 0 开始
     * @return 样本输入，以键值对形式表示
     * @throws IndexOutOfBoundsException 如果索引超出范围
     */
    Map<String, Object> getInput(int index);

    /**
     * 获取指定索引位置的样本标签。
     *
     * @param index 样本索引，从 0 开始
     * @return 样本标签
     * @throws IndexOutOfBoundsException 如果索引超出范围
     */
    Object getLabel(int index);

    /**
     * 将数据集转换为 (input, label) 的条目列表。
     * <p>
     * 该方法会遍历数据集的所有样本，将每个样本的输入和标签封装为 Map.Entry 对象。
     * </p>
     *
     * @return 包含所有样本的条目列表
     */
    default List<Map.Entry<Map<String, Object>, Object>> toEntryList() {
        List<Map.Entry<Map<String, Object>, Object>> entries = new java.util.ArrayList<>(size());
        for (int i = 0; i < size(); i++) {
            entries.add(new AbstractMap.SimpleEntry<>(getInput(i), getLabel(i)));
        }
        return entries;
    }

    /**
     * 获取数据集的子集。
     * <p>
     * 该方法返回从 fromIndex（包含）到 toIndex（不包含）范围内的样本子集。
     * </p>
     *
     * @param fromIndex 起始索引（包含）
     * @param toIndex   结束索引（不包含）
     * @return 子集数据集
     * @throws IndexOutOfBoundsException 如果索引超出范围
     * @throws IllegalArgumentException  如果 fromIndex > toIndex
     */
    default EvaluationDataset subset(int fromIndex, int toIndex) {
        List<Map.Entry<Map<String, Object>, Object>> entries = toEntryList();
        List<Map.Entry<Map<String, Object>, Object>> subEntries = entries.subList(fromIndex, toIndex);
        return SimpleEvaluationDataset.fromPairs(getName() + "_subset", subEntries);
    }
}
