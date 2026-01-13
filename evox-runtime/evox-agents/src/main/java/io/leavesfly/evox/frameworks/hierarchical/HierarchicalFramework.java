package io.leavesfly.evox.frameworks.hierarchical;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 分层决策框架
 * 支持多层级的决策结构,管理者-执行者模式
 *
 * @param <T> 决策结果类型
 * @author EvoX Team
 */
@Slf4j
@Data
public class HierarchicalFramework<T> {

    /**
     * 决策层级列表(从高到低)
     */
    private List<DecisionLayer<T>> layers;

    /**
     * 根任务
     */
    private String rootTask;

    /**
     * 配置
     */
    private HierarchicalConfig config;

    /**
     * 执行历史
     */
    private List<ExecutionRecord<T>> history;

    /**
     * 层级映射(层级ID -> 层级对象)
     */
    private Map<String, DecisionLayer<T>> layerMap;

    public HierarchicalFramework(List<DecisionLayer<T>> layers, HierarchicalConfig config) {
        this.layers = layers;
        this.config = config;
        this.history = new ArrayList<>();
        this.layerMap = new ConcurrentHashMap<>();
        
        // 构建层级映射
        for (DecisionLayer<T> layer : layers) {
            layerMap.put(layer.getLayerId(), layer);
        }
    }

    public HierarchicalFramework(List<DecisionLayer<T>> layers) {
        this(layers, HierarchicalConfig.builder().build());
    }

    /**
     * 执行分层决策
     *
     * @param task 任务描述
     * @return 决策结果
     */
    public HierarchicalResult<T> executeHierarchical(String task) {
        this.rootTask = task;
        long startTime = System.currentTimeMillis();
        
        log.info("Starting hierarchical decision for task: {}", task);
        
        try {
            // 从最高层开始执行
            DecisionLayer<T> topLayer = layers.get(0);
            LayerDecision<T> topDecision = executeLayer(topLayer, task, null);
            
            // 递归执行下层
            LayerDecision<T> finalDecision = executeRecursive(topDecision, 0);
            
            long duration = System.currentTimeMillis() - startTime;
            
            return HierarchicalResult.<T>builder()
                .success(true)
                .result(finalDecision.getResult())
                .layers(history.size())
                .duration(duration)
                .history(new ArrayList<>(history))
                .metadata(buildMetadata())
                .build();
                
        } catch (Exception e) {
            log.error("Hierarchical decision failed: {}", e.getMessage(), e);
            return HierarchicalResult.<T>builder()
                .success(false)
                .error(e.getMessage())
                .duration(System.currentTimeMillis() - startTime)
                .build();
        }
    }

    /**
     * 递归执行层级决策
     */
    private LayerDecision<T> executeRecursive(LayerDecision<T> currentDecision, int currentLayerIndex) {
        // 如果当前层已经是最底层,或者不需要继续分解,直接返回
        if (currentLayerIndex >= layers.size() - 1 || !currentDecision.isNeedDelegation()) {
            return currentDecision;
        }

        // 如果需要委派,则委派给下一层
        List<String> subTasks = currentDecision.getSubTasks();
        if (subTasks == null || subTasks.isEmpty()) {
            return currentDecision;
        }

        DecisionLayer<T> nextLayer = layers.get(currentLayerIndex + 1);
        List<LayerDecision<T>> subDecisions = new ArrayList<>();

        // 执行所有子任务
        for (String subTask : subTasks) {
            LayerDecision<T> subDecision = executeLayer(nextLayer, subTask, currentDecision);
            LayerDecision<T> finalSubDecision = executeRecursive(subDecision, currentLayerIndex + 1);
            subDecisions.add(finalSubDecision);
        }

        // 聚合子任务结果
        T aggregatedResult = aggregateResults(subDecisions, currentDecision);
        currentDecision.setResult(aggregatedResult);
        currentDecision.setSubDecisions(subDecisions);

        return currentDecision;
    }

    /**
     * 执行单个层级的决策
     */
    private LayerDecision<T> executeLayer(DecisionLayer<T> layer, String task, LayerDecision<T> parentDecision) {
        log.debug("Executing layer {} for task: {}", layer.getLayerId(), task);
        
        long startTime = System.currentTimeMillis();
        
        // 执行层级决策
        LayerDecision<T> decision = layer.decide(task, parentDecision);
        
        long duration = System.currentTimeMillis() - startTime;
        
        // 记录执行历史
        ExecutionRecord<T> record = new ExecutionRecord<>(
            layer.getLayerId(),
            task,
            decision,
            duration,
            System.currentTimeMillis()
        );
        history.add(record);
        
        log.debug("Layer {} decision completed in {}ms", layer.getLayerId(), duration);
        
        return decision;
    }

    /**
     * 聚合子结果
     */
    @SuppressWarnings("unchecked")
    private T aggregateResults(List<LayerDecision<T>> subDecisions, LayerDecision<T> parentDecision) {
        if (config.getAggregationStrategy() != null) {
            return ((HierarchicalFramework.AggregationStrategy<T>) config.getAggregationStrategy()).aggregate(subDecisions, parentDecision);
        }
        
        // 默认聚合策略: 返回第一个非空结果
        return subDecisions.stream()
            .map(LayerDecision::getResult)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }

    /**
     * 构建元数据
     */
    private Map<String, Object> buildMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("totalLayers", layers.size());
        metadata.put("executedLayers", history.stream()
            .map(ExecutionRecord::getLayerId)
            .distinct()
            .count());
        metadata.put("totalExecutions", history.size());
        metadata.put("avgLayerDuration", history.stream()
            .mapToLong(ExecutionRecord::getDuration)
            .average()
            .orElse(0.0));
        
        return metadata;
    }

    /**
     * 添加新层级
     */
    public void addLayer(DecisionLayer<T> layer) {
        layers.add(layer);
        layerMap.put(layer.getLayerId(), layer);
    }

    /**
     * 获取层级
     */
    public DecisionLayer<T> getLayer(String layerId) {
        return layerMap.get(layerId);
    }

    /**
     * 移除层级
     */
    public void removeLayer(String layerId) {
        DecisionLayer<T> layer = layerMap.remove(layerId);
        if (layer != null) {
            layers.remove(layer);
        }
    }

    /**
     * 清空历史
     */
    public void clearHistory() {
        history.clear();
    }

    /**
     * 聚合策略接口
     */
    public interface AggregationStrategy<T> {
        /**
         * 聚合子决策结果
         *
         * @param subDecisions 子决策列表
         * @param parentDecision 父决策
         * @return 聚合后的结果
         */
        T aggregate(List<LayerDecision<T>> subDecisions, LayerDecision<T> parentDecision);
    }
}
