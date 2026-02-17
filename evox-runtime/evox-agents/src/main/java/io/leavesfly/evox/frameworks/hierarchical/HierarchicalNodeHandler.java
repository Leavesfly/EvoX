package io.leavesfly.evox.frameworks.hierarchical;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.leavesfly.evox.workflow.base.WorkflowNode;
import io.leavesfly.evox.workflow.execution.WorkflowContext;
import io.leavesfly.evox.workflow.node.NodeHandler;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 层级决策节点处理器 - 单层决策处理器
 * 执行单个层级的决策逻辑，通过 LOOP 节点迭代处理所有层级
 *
 * @param <T> 决策结果类型
 * @author EvoX Team
 */
@Slf4j
class LayerDecisionHandler<T> implements NodeHandler {

    private static final String CONTEXT_KEY_LAYERS = "hierarchical_layers";
    private static final String CONTEXT_KEY_CONFIG = "hierarchical_config";
    private static final String CONTEXT_KEY_HISTORY = "hierarchical_history";
    private static final String CONTEXT_KEY_CURRENT_LAYER_INDEX = "current_layer_index";
    private static final String CONTEXT_KEY_CURRENT_DECISION = "current_decision";
    private static final String CONTEXT_KEY_ALL_LAYERS_PROCESSED = "all_layers_processed";
    private static final String CONTEXT_KEY_FRAMEWORK = "framework";
    private static final String CONTEXT_KEY_START_TIME = "start_time";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getHandlerName() {
        return "LayerDecisionHandler";
    }

    @Override
    public Mono<Object> handle(WorkflowContext context, WorkflowNode node) {
        return Mono.fromCallable(() -> {
            log.debug("LayerDecisionHandler started execution");
            
            @SuppressWarnings("unchecked")
            HierarchicalFramework<T> framework = (HierarchicalFramework<T>) context.getExecutionData(CONTEXT_KEY_FRAMEWORK);
            
            String task = (String) context.getExecutionData("task");
            
            LayerDecision<T> decision = executeSingleLayerDecision(framework, task, context);
            
            try {
                String jsonResult = objectMapper.writeValueAsString(decision);
                log.debug("LayerDecisionHandler completed, layer: {}, needDelegation: {}", 
                    decision.getLayerId(), decision.isNeedDelegation());
                return jsonResult;
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize layer decision", e);
                throw new RuntimeException("Failed to serialize layer decision: " + e.getMessage(), e);
            }
        });
    }

    /**
     * 执行单层决策逻辑
     */
    private LayerDecision<T> executeSingleLayerDecision(HierarchicalFramework<T> framework, 
                                                        String task, 
                                                        WorkflowContext context) {
        @SuppressWarnings("unchecked")
        List<DecisionLayer<T>> layers = (List<DecisionLayer<T>>) context.getExecutionData(CONTEXT_KEY_LAYERS);
        
        if (layers == null || layers.isEmpty()) {
            throw new IllegalStateException("No layers configured");
        }
        
        int currentLayerIndex = (Integer) context.getExecutionData(CONTEXT_KEY_CURRENT_LAYER_INDEX);
        
        if (currentLayerIndex >= layers.size()) {
            context.updateExecutionData(CONTEXT_KEY_ALL_LAYERS_PROCESSED, true);
            LayerDecision<T> finalDecision = new LayerDecision<>("FINAL", task);
            finalDecision.setNeedDelegation(false);
            return finalDecision;
        }
        
        DecisionLayer<T> currentLayer = layers.get(currentLayerIndex);
        
        @SuppressWarnings("unchecked")
        LayerDecision<T> parentDecision = (LayerDecision<T>) context.getExecutionData(CONTEXT_KEY_CURRENT_DECISION);
        
        LayerDecision<T> decision = executeLayer(currentLayer, task, parentDecision, context);
        
        context.updateExecutionData(CONTEXT_KEY_CURRENT_DECISION, decision);
        
        if (!decision.isNeedDelegation() || decision.getSubTasks() == null || decision.getSubTasks().isEmpty()) {
            currentLayerIndex++;
            context.updateExecutionData(CONTEXT_KEY_CURRENT_LAYER_INDEX, currentLayerIndex);
            
            if (currentLayerIndex >= layers.size()) {
                context.updateExecutionData(CONTEXT_KEY_ALL_LAYERS_PROCESSED, true);
            }
        } else {
            List<LayerDecision<T>> subDecisions = new ArrayList<>();
            
            if (currentLayerIndex + 1 < layers.size()) {
                DecisionLayer<T> nextLayer = layers.get(currentLayerIndex + 1);
                
                for (String subTask : decision.getSubTasks()) {
                    LayerDecision<T> subDecision = executeLayer(nextLayer, subTask, decision, context);
                    subDecisions.add(subDecision);
                }
            }
            
            @SuppressWarnings("unchecked")
            HierarchicalConfig config = (HierarchicalConfig) context.getExecutionData(CONTEXT_KEY_CONFIG);
            T aggregatedResult = aggregateResults(subDecisions, decision, config);
            decision.setResult(aggregatedResult);
            decision.setSubDecisions(subDecisions);
            
            currentLayerIndex++;
            context.updateExecutionData(CONTEXT_KEY_CURRENT_LAYER_INDEX, currentLayerIndex);
            
            if (currentLayerIndex >= layers.size()) {
                context.updateExecutionData(CONTEXT_KEY_ALL_LAYERS_PROCESSED, true);
            }
        }
        
        return decision;
    }

    /**
     * 执行单个层级的决策
     */
    private LayerDecision<T> executeLayer(DecisionLayer<T> layer, 
                                          String task, 
                                          LayerDecision<T> parentDecision,
                                          WorkflowContext context) {
        log.debug("Executing layer {} for task: {}", layer.getLayerId(), task);
        
        long startTime = System.currentTimeMillis();
        
        LayerDecision<T> decision = layer.decide(task, parentDecision);
        
        long duration = System.currentTimeMillis() - startTime;
        
        ExecutionRecord<T> record = new ExecutionRecord<>(
            layer.getLayerId(),
            task,
            decision,
            duration,
            System.currentTimeMillis()
        );
        
        @SuppressWarnings("unchecked")
        List<ExecutionRecord<T>> history = (List<ExecutionRecord<T>>) context.getExecutionData(CONTEXT_KEY_HISTORY);
        history.add(record);
        context.updateExecutionData(CONTEXT_KEY_HISTORY, history);
        
        log.debug("Layer {} decision completed in {}ms", layer.getLayerId(), duration);
        
        return decision;
    }

    /**
     * 聚合子结果
     */
    @SuppressWarnings("unchecked")
    private T aggregateResults(List<LayerDecision<T>> subDecisions, 
                              LayerDecision<T> parentDecision,
                              HierarchicalConfig config) {
        if (config != null && config.getAggregationStrategy() != null) {
            return ((HierarchicalFramework.AggregationStrategy<T>) config.getAggregationStrategy())
                .aggregate(subDecisions, parentDecision);
        }
        
        return subDecisions.stream()
            .map(LayerDecision::getResult)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }
}

/**
 * 层级决策结果处理器
 * 聚合所有层级的决策结果，构建最终的 HierarchicalResult
 *
 * @param <T> 决策结果类型
 * @author EvoX Team
 */
@Slf4j
class HierarchicalResultHandler<T> implements NodeHandler {

    private static final String CONTEXT_KEY_HISTORY = "hierarchical_history";
    private static final String CONTEXT_KEY_CURRENT_DECISION = "current_decision";
    private static final String CONTEXT_KEY_CONFIG = "hierarchical_config";
    private static final String CONTEXT_KEY_FRAMEWORK = "framework";
    private static final String CONTEXT_KEY_START_TIME = "start_time";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getHandlerName() {
        return "HierarchicalResultHandler";
    }

    @Override
    public Mono<Object> handle(WorkflowContext context, WorkflowNode node) {
        return Mono.fromCallable(() -> {
            log.debug("HierarchicalResultHandler started execution");
            
            @SuppressWarnings("unchecked")
            HierarchicalFramework<T> framework = (HierarchicalFramework<T>) context.getExecutionData(CONTEXT_KEY_FRAMEWORK);
            
            @SuppressWarnings("unchecked")
            LayerDecision<T> finalDecision = (LayerDecision<T>) context.getExecutionData(CONTEXT_KEY_CURRENT_DECISION);
            
            @SuppressWarnings("unchecked")
            List<ExecutionRecord<T>> history = (List<ExecutionRecord<T>>) context.getExecutionData(CONTEXT_KEY_HISTORY);
            
            Long startTime = (Long) context.getExecutionData(CONTEXT_KEY_START_TIME);
            long duration = startTime != null ? System.currentTimeMillis() - startTime : 0;
            
            HierarchicalResult<T> result = buildHierarchicalResult(framework, finalDecision, history, duration);
            
            try {
                String jsonResult = objectMapper.writeValueAsString(result);
                log.debug("HierarchicalResultHandler completed, success: {}, duration: {}ms", 
                    result.isSuccess(), result.getDuration());
                return jsonResult;
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize hierarchical result", e);
                throw new RuntimeException("Failed to serialize hierarchical result: " + e.getMessage(), e);
            }
        });
    }

    /**
     * 构建层级决策结果
     */
    private HierarchicalResult<T> buildHierarchicalResult(HierarchicalFramework<T> framework, 
                                                          LayerDecision<T> finalDecision,
                                                          List<ExecutionRecord<T>> history,
                                                          long duration) {
        if (finalDecision != null) {
            return HierarchicalResult.<T>builder()
                .success(true)
                .result(finalDecision.getResult())
                .layers(history != null ? history.size() : 0)
                .duration(duration)
                .history(new ArrayList<>(history != null ? history : List.of()))
                .metadata(buildMetadata(framework, history))
                .build();
        }
        
        return HierarchicalResult.<T>builder()
            .success(false)
            .error("No decision produced")
            .duration(duration)
            .build();
    }

    /**
     * 构建元数据
     */
    private Map<String, Object> buildMetadata(HierarchicalFramework<T> framework, 
                                              List<ExecutionRecord<T>> history) {
        Map<String, Object> metadata = new java.util.HashMap<>();
        
        if (framework.getLayers() != null) {
            metadata.put("totalLayers", framework.getLayers().size());
        }
        
        if (history != null && !history.isEmpty()) {
            metadata.put("executedLayers", history.stream()
                .map(ExecutionRecord::getLayerId)
                .distinct()
                .count());
            metadata.put("totalExecutions", history.size());
            metadata.put("avgLayerDuration", history.stream()
                .mapToLong(ExecutionRecord::getDuration)
                .average()
                .orElse(0.0));
        }
        
        return metadata;
    }
}