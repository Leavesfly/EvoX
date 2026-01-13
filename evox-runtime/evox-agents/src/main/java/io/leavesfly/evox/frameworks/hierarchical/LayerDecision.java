package io.leavesfly.evox.frameworks.hierarchical;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 层级决策结果
 *
 * @param <T> 决策结果类型
 * @author EvoX Team
 */
@Data
public class LayerDecision<T> {

    /**
     * 层级ID
     */
    private String layerId;

    /**
     * 任务描述
     */
    private String task;

    /**
     * 决策结果
     */
    private T result;

    /**
     * 是否需要委派给下层
     */
    private boolean needDelegation;

    /**
     * 子任务列表(如果需要委派)
     */
    private List<String> subTasks;

    /**
     * 子决策列表
     */
    private List<LayerDecision<T>> subDecisions;

    /**
     * 置信度
     */
    private double confidence;

    /**
     * 决策依据/理由
     */
    private String reasoning;

    /**
     * 元数据
     */
    private Map<String, Object> metadata;

    public LayerDecision() {
        this.subTasks = new ArrayList<>();
        this.subDecisions = new ArrayList<>();
        this.metadata = new HashMap<>();
        this.confidence = 1.0;
    }

    public LayerDecision(String layerId, String task) {
        this();
        this.layerId = layerId;
        this.task = task;
    }

    /**
     * 添加子任务
     */
    public void addSubTask(String subTask) {
        if (subTasks == null) {
            subTasks = new ArrayList<>();
        }
        subTasks.add(subTask);
        this.needDelegation = true;
    }

    /**
     * 添加多个子任务
     */
    public void addSubTasks(List<String> tasks) {
        if (subTasks == null) {
            subTasks = new ArrayList<>();
        }
        subTasks.addAll(tasks);
        this.needDelegation = true;
    }

    /**
     * 添加元数据
     */
    public void addMetadata(String key, Object value) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put(key, value);
    }

    /**
     * 获取元数据
     */
    public Object getMetadata(String key) {
        return metadata != null ? metadata.get(key) : null;
    }
}
