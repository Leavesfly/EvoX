package io.leavesfly.evox.workflow;

import io.leavesfly.evox.agents.manager.AgentManager;
import io.leavesfly.evox.workflow.base.Workflow;
import io.leavesfly.evox.workflow.base.WorkflowNode;
import io.leavesfly.evox.workflow.graph.WorkflowGraph;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 子工作流节点测试
 */
@Slf4j
class SubWorkflowNodeTest {

    private AgentManager agentManager;
    private Workflow parentWorkflow;
    private WorkflowGraph parentGraph;

    @BeforeEach
    void setUp() {
        agentManager = new AgentManager();
        parentGraph = new WorkflowGraph("Parent workflow");
    }

    @Test
    void testSimpleSubWorkflow() {
        // 创建子工作流
        Workflow subWorkflow = createSimpleSubWorkflow();

        // 创建子工作流节点
        WorkflowNode subWorkflowNode = new WorkflowNode();
        subWorkflowNode.setName("sub_workflow");
        subWorkflowNode.setDescription("Execute sub workflow");
        subWorkflowNode.setNodeType(WorkflowNode.NodeType.SUBWORKFLOW);
        subWorkflowNode.setSubWorkflow(subWorkflow);
        subWorkflowNode.initModule();

        // 构建父工作流
        parentGraph.addNode(subWorkflowNode);

        parentWorkflow = new Workflow();
        parentWorkflow.setName("Parent workflow");
        parentWorkflow.setGraph(parentGraph);
        parentWorkflow.setAgentManager(agentManager);
        parentWorkflow.initModule();

        // 执行父工作流
        Map<String, Object> input = new HashMap<>();
        input.put("test_key", "test_value");
        String result = parentWorkflow.execute(input);

        assertNotNull(result, "Result should not be null");
        log.info("Simple subworkflow result: {}", result);

        // 验证子工作流节点完成
        assertEquals(WorkflowNode.NodeState.COMPLETED, subWorkflowNode.getState());
    }

    @Test
    void testSubWorkflowWithInputMapping() {
        // 创建子工作流
        Workflow subWorkflow = createSimpleSubWorkflow();

        // 创建子工作流节点（配置输入映射）
        WorkflowNode subWorkflowNode = new WorkflowNode();
        subWorkflowNode.setName("sub_workflow_with_input");
        subWorkflowNode.setDescription("Sub workflow with input mapping");
        subWorkflowNode.setNodeType(WorkflowNode.NodeType.SUBWORKFLOW);
        subWorkflowNode.setSubWorkflow(subWorkflow);

        // 配置输入映射：父上下文的 parent_data -> 子工作流的 sub_input
        Map<String, String> inputMapping = new HashMap<>();
        inputMapping.put("sub_input", "parent_data");
        subWorkflowNode.setSubWorkflowInputMapping(inputMapping);

        subWorkflowNode.initModule();

        // 构建父工作流
        parentGraph.addNode(subWorkflowNode);

        parentWorkflow = new Workflow();
        parentWorkflow.setName("Parent workflow");
        parentWorkflow.setGraph(parentGraph);
        parentWorkflow.setAgentManager(agentManager);
        parentWorkflow.initModule();

        // 执行父工作流
        Map<String, Object> input = new HashMap<>();
        input.put("parent_data", "data_from_parent");
        String result = parentWorkflow.execute(input);

        assertNotNull(result, "Result should not be null");
        log.info("Subworkflow with input mapping result: {}", result);

        assertEquals(WorkflowNode.NodeState.COMPLETED, subWorkflowNode.getState());
    }

    @Test
    void testSubWorkflowWithOutputMapping() {
        // 创建子工作流
        Workflow subWorkflow = createSimpleSubWorkflow();

        // 创建子工作流节点（配置输出映射）
        WorkflowNode subWorkflowNode = new WorkflowNode();
        subWorkflowNode.setName("sub_workflow_with_output");
        subWorkflowNode.setDescription("Sub workflow with output mapping");
        subWorkflowNode.setNodeType(WorkflowNode.NodeType.SUBWORKFLOW);
        subWorkflowNode.setSubWorkflow(subWorkflow);

        // 配置输出映射：子工作流的 sub_result -> 父上下文的 parent_result
        Map<String, String> outputMapping = new HashMap<>();
        outputMapping.put("parent_result", "sub_result");
        subWorkflowNode.setSubWorkflowOutputMapping(outputMapping);

        subWorkflowNode.initModule();

        // 构建父工作流
        parentGraph.addNode(subWorkflowNode);

        parentWorkflow = new Workflow();
        parentWorkflow.setName("Parent workflow");
        parentWorkflow.setGraph(parentGraph);
        parentWorkflow.setAgentManager(agentManager);
        parentWorkflow.initModule();

        // 执行父工作流
        String result = parentWorkflow.execute(new HashMap<>());

        assertNotNull(result, "Result should not be null");
        log.info("Subworkflow with output mapping result: {}", result);

        assertEquals(WorkflowNode.NodeState.COMPLETED, subWorkflowNode.getState());
    }

    @Test
    void testNestedSubWorkflow() {
        // 创建最内层子工作流
        Workflow innerSubWorkflow = createSimpleSubWorkflow();
        innerSubWorkflow.setName("Inner sub workflow");

        // 创建中间层子工作流
        WorkflowGraph middleGraph = new WorkflowGraph("Middle sub workflow");
        WorkflowNode innerSubNode = new WorkflowNode();
        innerSubNode.setName("inner_sub_node");
        innerSubNode.setDescription("Inner sub workflow node");
        innerSubNode.setNodeType(WorkflowNode.NodeType.SUBWORKFLOW);
        innerSubNode.setSubWorkflow(innerSubWorkflow);
        innerSubNode.initModule();
        middleGraph.addNode(innerSubNode);

        Workflow middleSubWorkflow = new Workflow();
        middleSubWorkflow.setName("Middle sub workflow");
        middleSubWorkflow.setGraph(middleGraph);
        middleSubWorkflow.setAgentManager(agentManager);
        middleSubWorkflow.initModule();

        // 创建外层子工作流节点
        WorkflowNode middleSubNode = new WorkflowNode();
        middleSubNode.setName("middle_sub_node");
        middleSubNode.setDescription("Middle sub workflow node");
        middleSubNode.setNodeType(WorkflowNode.NodeType.SUBWORKFLOW);
        middleSubNode.setSubWorkflow(middleSubWorkflow);
        middleSubNode.initModule();

        // 构建父工作流
        parentGraph.addNode(middleSubNode);

        parentWorkflow = new Workflow();
        parentWorkflow.setName("Parent workflow");
        parentWorkflow.setGraph(parentGraph);
        parentWorkflow.setAgentManager(agentManager);
        parentWorkflow.setMaxExecutionSteps(20);
        parentWorkflow.initModule();

        // 执行父工作流
        String result = parentWorkflow.execute(new HashMap<>());

        assertNotNull(result, "Result should not be null");
        log.info("Nested subworkflow result: {}", result);

        // 验证执行完成
        assertEquals(WorkflowNode.NodeState.COMPLETED, middleSubNode.getState());
    }

    @Test
    void testSubWorkflowMixedWithDecision() {
        // 创建决策节点
        WorkflowNode decision = new WorkflowNode();
        decision.setName("decision");
        decision.setDescription("Decision before subworkflow");
        decision.setNodeType(WorkflowNode.NodeType.DECISION);
        decision.setCondition("true");
        decision.initModule();

        // 创建子工作流
        Workflow subWorkflow = createSimpleSubWorkflow();

        // 创建子工作流节点
        WorkflowNode subWorkflowNode = new WorkflowNode();
        subWorkflowNode.setName("sub_workflow");
        subWorkflowNode.setDescription("Sub workflow after decision");
        subWorkflowNode.setNodeType(WorkflowNode.NodeType.SUBWORKFLOW);
        subWorkflowNode.setSubWorkflow(subWorkflow);
        subWorkflowNode.initModule();

        // 配置决策分支指向子工作流
        Map<String, String> branches = new HashMap<>();
        branches.put("true", subWorkflowNode.getNodeId());
        decision.setBranches(branches);

        // 构建父工作流
        parentGraph.addNode(decision);
        parentGraph.addNode(subWorkflowNode);
        parentGraph.addEdge(decision.getNodeId(), subWorkflowNode.getNodeId());

        parentWorkflow = new Workflow();
        parentWorkflow.setName("Parent workflow");
        parentWorkflow.setGraph(parentGraph);
        parentWorkflow.setAgentManager(agentManager);
        parentWorkflow.setMaxExecutionSteps(10);
        parentWorkflow.initModule();

        // 执行父工作流
        String result = parentWorkflow.execute(new HashMap<>());

        assertNotNull(result, "Result should not be null");
        log.info("Decision-subworkflow mixed result: {}", result);

        // 验证执行路径
        assertEquals(WorkflowNode.NodeState.COMPLETED, decision.getState());
        assertEquals(WorkflowNode.NodeState.COMPLETED, subWorkflowNode.getState());
    }

    /**
     * 创建简单的子工作流
     */
    private Workflow createSimpleSubWorkflow() {
        WorkflowGraph subGraph = new WorkflowGraph("Sub workflow");

        // 创建简单的决策节点作为子工作流的内容
        WorkflowNode taskNode = new WorkflowNode();
        taskNode.setName("sub_task");
        taskNode.setDescription("Sub workflow task");
        taskNode.setNodeType(WorkflowNode.NodeType.DECISION);
        taskNode.setCondition("true");
        taskNode.initModule();

        subGraph.addNode(taskNode);

        Workflow subWorkflow = new Workflow();
        subWorkflow.setName("Sub workflow");
        subWorkflow.setGraph(subGraph);
        subWorkflow.setAgentManager(agentManager);
        subWorkflow.initModule();

        return subWorkflow;
    }
}
