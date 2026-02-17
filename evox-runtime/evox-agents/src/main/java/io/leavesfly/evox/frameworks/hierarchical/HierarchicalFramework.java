package io.leavesfly.evox.frameworks.hierarchical;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.leavesfly.evox.frameworks.base.MultiAgentFramework;
import io.leavesfly.evox.workflow.base.Workflow;
import io.leavesfly.evox.workflow.base.WorkflowNode;
import io.leavesfly.evox.workflow.graph.WorkflowGraph;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 分层决策框架
 * 支持多层级的决策结构,管理者-执行者模式
 * 基于 evox-workflow DAG 引擎实现
 *
 * @param <T> 决策结果类型
 * @author EvoX Team
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class HierarchicalFramework<T> extends MultiAgentFramework {


    private static final String LAYER_DECISION_HANDLER_NAME = "LayerDecisionHandler";
    private static final String LAYER_DECISION_NODE_NAME = "layer_decision_node";
    private static final String HIERARCHICAL_RESULT_HANDLER_NAME = "HierarchicalResultHandler";
    private static final String HIERARCHICAL_RESULT_NODE_NAME = "hierarchical_result_node";
    private static final String LOOP_NODE_NAME = "hierarchical_loop_node";
    
    private final ObjectMapper objectMapper = new ObjectMapper();

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
        this.frameworkName = "HierarchicalFramework";
        
        // 构建层级映射
        for (DecisionLayer<T> layer : layers) {
            layerMap.put(layer.getLayerId(), layer);
        }
    }

    public HierarchicalFramework(List<DecisionLayer<T>> layers) {
        this(layers, HierarchicalConfig.builder().build());
    }

    /**
     * 构建工作流 DAG 图
     * 创建多节点 DAG：LOOP + COLLECT(layer_decision) + COLLECT(hierarchical_result)
     * LOOP 节点迭代处理所有层级，直到 all_layers_processed == true
     *
     * @param task 任务描述
     * @return 工作流图
     */
    @Override
    protected WorkflowGraph buildWorkflowGraph(String task) {
        WorkflowGraph graph = new WorkflowGraph(task);
        
        // 创建 LOOP 节点
        WorkflowNode loopNode = createLoopNode(
            LOOP_NODE_NAME,
            layers.size(),
            "all_layers_processed == false"
        );
        
        // 创建 layer_decision COLLECT 节点
        Map<String, Object> layerDecisionConfig = new HashMap<>();
        layerDecisionConfig.put("task", task);
        
        WorkflowNode layerDecisionNode = createCollectNode(
            LAYER_DECISION_NODE_NAME,
            LAYER_DECISION_HANDLER_NAME,
            layerDecisionConfig
        );
        
        // 创建 hierarchical_result COLLECT 节点
        Map<String, Object> resultConfig = new HashMap<>();
        resultConfig.put("task", task);
        
        WorkflowNode resultNode = createCollectNode(
            HIERARCHICAL_RESULT_NODE_NAME,
            HIERARCHICAL_RESULT_HANDLER_NAME,
            resultConfig
        );
        
        // 设置 LOOP 节点的循环体为 layer_decision 节点
        loopNode.setLoopBodyNodeId(layerDecisionNode.getNodeId());
        
        // 添加所有节点到图中
        graph.addNode(loopNode);
        graph.addNode(layerDecisionNode);
        graph.addNode(resultNode);
        
        // 添加边：LOOP -> hierarchical_result
        // 当 LOOP 退出时（all_layers_processed == true），执行结果聚合
        graph.addEdge(loopNode.getNodeId(), resultNode.getNodeId());
        
        return graph;
    }

    /**
     * 注册节点处理器
     * 注册层级决策处理器到 Workflow
     *
     * @param workflow 工作流实例
     */
    @Override
    protected void registerNodeHandlers(Workflow workflow) {
        // 注册层级决策节点处理器
        LayerDecisionHandler<T> layerDecisionHandler = new LayerDecisionHandler<>();
        workflow.registerHandler(LAYER_DECISION_HANDLER_NAME, layerDecisionHandler);
        
        // 注册层级结果节点处理器
        HierarchicalResultHandler<T> resultHandler = new HierarchicalResultHandler<>();
        workflow.registerHandler(HIERARCHICAL_RESULT_HANDLER_NAME, resultHandler);
    }

    /**
     * 执行前的初始化钩子
     *
     * @param task 任务描述
     */
    @Override
    protected void beforeExecute(String task) {
        this.rootTask = task;
        log.info("Initializing hierarchical framework for task: {}", task);
    }

    /**
     * 执行分层决策
     * 内部调用 workflow 执行引擎
     *
     * @param task 任务描述
     * @return 决策结果
     */
    public HierarchicalResult<T> executeHierarchical(String task) {
        // 准备输入参数
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("task", task);
        inputs.put("hierarchical_layers", layers);
        inputs.put("hierarchical_config", config);
        inputs.put("hierarchical_history", new ArrayList<>());
        inputs.put("framework", this);
        inputs.put("all_layers_processed", false);
        inputs.put("current_layer_index", 0);
        inputs.put("start_time", System.currentTimeMillis());
        
        // 通过 workflow 执行，返回 JSON 字符串
        String rawResult = executeWorkflow(task, inputs);
        
        // 反序列化结果
        if (rawResult != null && !rawResult.isEmpty()) {
            try {
                // 尝试反序列化为 HierarchicalResult
                HierarchicalResult<T> result = objectMapper.readValue(
                    rawResult, 
                    new TypeReference<HierarchicalResult<T>>() {}
                );
                log.info("Hierarchical workflow execution completed successfully");
                return result;
            } catch (Exception e) {
                log.error("Failed to deserialize hierarchical result: {}", e.getMessage(), e);
                // 降级处理：返回基于字符串的结果
                return HierarchicalResult.<T>builder()
                    .success(true)
                    .result((T) rawResult)
                    .build();
            }
        }
        
        // 降级处理：如果无法获取结果，返回失败结果
        log.warn("Failed to retrieve result from workflow");
        return HierarchicalResult.<T>builder()
            .success(false)
            .error("Failed to retrieve result from workflow")
            .build();
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