package io.leavesfly.evox.workflow.base;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.leavesfly.evox.core.module.BaseModule;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 工作流节点 - 代表工作流中的一个任务或步骤
 * 对应 Python 版本的 WorkFlowNode
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class WorkflowNode extends BaseModule {

    /**
     * 节点唯一标识符
     */
    private String nodeId;

    /**
     * 节点名称
     */
    private String name;

    /**
     * 节点描述
     */
    private String description;

    /**
     * 节点类型（例如：ACTION, DECISION, PARALLEL 等）
     */
    private NodeType nodeType;

    /**
     * 前置节点 ID 列表
     */
    private List<String> predecessors;

    /**
     * 后继节点 ID 列表
     */
    private List<String> successors;

    /**
     * 输入参数定义
     */
    private List<NodeParameter> inputs;

    /**
     * 输出参数定义
     */
    private List<NodeParameter> outputs;

    /**
     * 节点状态
     */
    private NodeState state;

    /**
     * 执行结果
     */
    private Object result;

    /**
     * 错误信息（如果执行失败）
     */
    private String errorMessage;

    /**
     * 决策条件（用于 DECISION 节点）
     * 格式：简单的条件表达式，如 "result.success == true"
     */
    private String condition;

    /**
     * 条件分支映射（用于 DECISION 节点）
     * key: 条件结果（true/false 或其他值）
     * value: 目标节点 ID
     */
    private Map<String, String> branches;

    /**
     * 并行执行的子节点列表（用于 PARALLEL 节点）
     */
    private List<String> parallelNodes;

    /**
     * 并行执行策略（用于 PARALLEL 节点）
     * ALL: 等待所有节点完成
     * ANY: 任意一个节点完成即可
     * FIRST: 第一个完成的节点
     */
    private ParallelStrategy parallelStrategy;

    /**
     * 循环条件（用于 LOOP 节点）
     * 例如："count < 5", "status != completed"
     */
    private String loopCondition;

    /**
     * 最大循环次数（用于 LOOP 节点）
     * 防止无限循环
     */
    private int maxIterations;

    /**
     * 当前迭代次数（用于 LOOP 节点）
     */
    private int currentIteration;

    /**
     * 循环体节点ID（用于 LOOP 节点）
     */
    private String loopBodyNodeId;

    /**
     * 子工作流（用于 SUBWORKFLOW 节点）
     */
    private Workflow subWorkflow;

    /**
     * 子工作流输入映射（用于 SUBWORKFLOW 节点）
     * key: 子工作流的输入参数名
     * value: 从父工作流上下文中获取的字段名
     */
    private Map<String, String> subWorkflowInputMapping;

    /**
     * 子工作流输出映射（用于 SUBWORKFLOW 节点）
     * key: 父工作流上下文中的字段名
     * value: 从子工作流结果中获取的字段名
     */
    private Map<String, String> subWorkflowOutputMapping;

    @Override
    public void initModule() {
        if (nodeId == null) {
            nodeId = UUID.randomUUID().toString();
        }
        if (predecessors == null) {
            predecessors = new ArrayList<>();
        }
        if (successors == null) {
            successors = new ArrayList<>();
        }
        if (inputs == null) {
            inputs = new ArrayList<>();
        }
        if (outputs == null) {
            outputs = new ArrayList<>();
        }
        if (state == null) {
            state = NodeState.PENDING;
        }
        if (branches == null) {
            branches = new java.util.HashMap<>();
        }
        if (parallelNodes == null) {
            parallelNodes = new ArrayList<>();
        }
        if (parallelStrategy == null) {
            parallelStrategy = ParallelStrategy.ALL;
        }
        if (maxIterations == 0 && nodeType == NodeType.LOOP) {
            maxIterations = 100; // 默认最大100次迭代
        }
        currentIteration = 0;
    }

    /**
     * 检查节点是否已完成
     */
    @JsonIgnore
    public boolean isCompleted() {
        return state == NodeState.COMPLETED;
    }

    /**
     * 检查节点是否失败
     */
    @JsonIgnore
    public boolean isFailed() {
        return state == NodeState.FAILED;
    }

    /**
     * 检查节点是否可以执行
     */
    @JsonIgnore
    public boolean isReady() {
        return state == NodeState.PENDING || state == NodeState.READY;
    }

    /**
     * 标记节点为就绪状态
     */
    public void markReady() {
        this.state = NodeState.READY;
    }

    /**
     * 标记节点为运行状态
     */
    public void markRunning() {
        this.state = NodeState.RUNNING;
    }

    /**
     * 标记节点为完成状态
     */
    public void markCompleted(Object result) {
        this.state = NodeState.COMPLETED;
        this.result = result;
    }

    /**
     * 标记节点为失败状态
     */
    public void markFailed(String errorMessage) {
        this.state = NodeState.FAILED;
        this.errorMessage = errorMessage;
    }

    /**
     * 添加前置节点
     */
    public void addPredecessor(String nodeId) {
        if (!predecessors.contains(nodeId)) {
            predecessors.add(nodeId);
        }
    }

    /**
     * 添加后继节点
     */
    public void addSuccessor(String nodeId) {
        if (!successors.contains(nodeId)) {
            successors.add(nodeId);
        }
    }

    /**
     * 节点类型枚举
     */
    public enum NodeType {
        /** 动作节点 */
        ACTION,
        /** 决策节点 */
        DECISION,
        /** 并行节点 */
        PARALLEL,
        /** 循环节点 */
        LOOP,
        /** 子工作流节点 */
        SUBWORKFLOW
    }

    /**
     * 节点状态枚举
     */
    public enum NodeState {
        /** 待处理 */
        PENDING,
        /** 就绪（所有前置节点已完成） */
        READY,
        /** 运行中 */
        RUNNING,
        /** 已完成 */
        COMPLETED,
        /** 失败 */
        FAILED,
        /** 已跳过 */
        SKIPPED
    }

    /**
     * 并行执行策略
     */
    public enum ParallelStrategy {
        /** 等待所有节点完成 */
        ALL,
        /** 任意一个节点完成即可 */
        ANY,
        /** 第一个完成的节点 */
        FIRST
    }

    /**
     * 节点参数定义
     */
    @Data
    @NoArgsConstructor
    public static class NodeParameter {
        /** 参数名 */
        private String name;
        /** 参数类型 */
        private String type;
        /** 是否必需 */
        private boolean required;
        /** 默认值 */
        private Object defaultValue;
        /** 参数描述 */
        private String description;
    }
}
