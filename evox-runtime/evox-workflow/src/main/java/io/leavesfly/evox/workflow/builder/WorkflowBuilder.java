package io.leavesfly.evox.workflow.builder;

import io.leavesfly.evox.core.agent.IAgent;
import io.leavesfly.evox.core.agent.IAgentManager;
import io.leavesfly.evox.workflow.base.Workflow;
import io.leavesfly.evox.workflow.base.WorkflowNode;
import io.leavesfly.evox.workflow.graph.WorkflowGraph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Workflow 流式构建器
 * 
 * <p>提供链式调用方式快速构建工作流</p>
 * 
 * <h3>使用示例:</h3>
 * <pre>{@code
 * // 创建顺序工作流
 * Workflow workflow = WorkflowBuilder.sequential()
 *     .name("数据处理流程")
 *     .step("validate", validateAgent, "验证数据")
 *     .step("transform", transformAgent, "转换数据")
 *     .step("save", saveAgent, "保存数据")
 *     .build();
 * 
 * // 创建条件工作流
 * Workflow workflow = WorkflowBuilder.conditional()
 *     .name("条件处理流程")
 *     .step("check", checkAgent)
 *     .when("check").equals("pass")
 *         .then("processA", processAAgent)
 *     .otherwise()
 *         .then("processB", processBAgent)
 *     .build();
 * }</pre>
 * 
 * @author EvoX Team
 * @since 1.0.0
 */
public class WorkflowBuilder {
    
    private String name;
    private String goal;
    private WorkflowGraph graph;
    private IAgentManager agentManager;
    private List<StepConfig> steps = new ArrayList<>();
    private int maxExecutionSteps = 100;
    private boolean isSequential = true;
    
    /**
     * 私有构造函数
     */
    private WorkflowBuilder() {
    }

    /**
     * 设置AgentManager
     */
    public WorkflowBuilder agentManager(IAgentManager agentManager) {
        this.agentManager = agentManager;
        return this;
    }
    
    /**
     * 创建顺序工作流构建器
     */
    public static WorkflowBuilder sequential() {
        WorkflowBuilder builder = new WorkflowBuilder();
        builder.isSequential = true;
        return builder;
    }
    
    /**
     * 创建条件工作流构建器
     */
    public static WorkflowBuilder conditional() {
        WorkflowBuilder builder = new WorkflowBuilder();
        builder.isSequential = false;
        return builder;
    }
    
    /**
     * 设置工作流名称
     */
    public WorkflowBuilder name(String name) {
        this.name = name;
        return this;
    }
    
    /**
     * 设置工作流目标
     */
    public WorkflowBuilder goal(String goal) {
        this.goal = goal;
        return this;
    }
    
    /**
     * 设置最大执行步数
     */
    public WorkflowBuilder maxSteps(int maxSteps) {
        this.maxExecutionSteps = maxSteps;
        return this;
    }
    
    /**
     * 添加步骤（仅 Agent 名称）
     */
    public WorkflowBuilder step(String stepName, IAgent agent) {
        return step(stepName, agent, null);
    }
    
    /**
     * 添加步骤（带描述）
     */
    public WorkflowBuilder step(String stepName, IAgent agent, String description) {
        if (agentManager != null) {
            agentManager.addAgent(agent);
        }
        steps.add(new StepConfig(stepName, agent.getName(), description));
        return this;
    }
    
    /**
     * 构建工作流
     */
    public Workflow build() {
        // 创建工作流图
        graph = new WorkflowGraph(goal != null ? goal : "Complete workflow");
        
        // 创建节点
        WorkflowNode prevNode = null;
        for (StepConfig step : steps) {
            WorkflowNode node = new WorkflowNode();
            node.setName(step.agentName);
            node.setDescription(step.description != null ? step.description : step.stepName);
            node.setNodeType(WorkflowNode.NodeType.ACTION);
            node.initModule();
            
            graph.addNode(node);
            
            // 如果是顺序工作流，连接前后节点
            if (isSequential && prevNode != null) {
                graph.addEdge(prevNode.getNodeId(), node.getNodeId());
            }
            
            prevNode = node;
        }
        
        // 创建工作流
        Workflow workflow = new Workflow();
        workflow.setName(name != null ? name : "Workflow-" + System.currentTimeMillis());
        workflow.setGraph(graph);
        workflow.setAgentManager(agentManager);
        workflow.setMaxExecutionSteps(maxExecutionSteps);
        workflow.initModule();
        
        return workflow;
    }
    
    /**
     * 步骤配置
     */
    private static class StepConfig {
        String stepName;
        String agentName;
        String description;
        
        StepConfig(String stepName, String agentName, String description) {
            this.stepName = stepName;
            this.agentName = agentName;
            this.description = description;
        }
    }
}
