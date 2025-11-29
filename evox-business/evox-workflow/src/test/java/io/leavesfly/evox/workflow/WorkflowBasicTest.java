package io.leavesfly.evox.workflow;

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
 * 工作流基础功能测试
 */
@Slf4j
class WorkflowBasicTest {

    private WorkflowGraph graph;
    private Workflow workflow;

    @BeforeEach
    void setUp() {
        // 创建工作流图
        graph = new WorkflowGraph("Test workflow goal");

        // 创建节点
        WorkflowNode node1 = new WorkflowNode();
        node1.setName("Node1");
        node1.setDescription("First node");
        node1.setNodeType(WorkflowNode.NodeType.ACTION);
        node1.initModule();

        WorkflowNode node2 = new WorkflowNode();
        node2.setName("Node2");
        node2.setDescription("Second node");
        node2.setNodeType(WorkflowNode.NodeType.ACTION);
        node2.initModule();

        // 添加节点到图
        graph.addNode(node1);
        graph.addNode(node2);

        // 添加边
        graph.addEdge(node1.getNodeId(), node2.getNodeId());

        // 创建工作流
        workflow = new Workflow();
        workflow.setGraph(graph);
        workflow.setMaxExecutionSteps(10);
        workflow.initModule();
    }

    @Test
    void testWorkflowGraphCreation() {
        assertNotNull(graph);
        assertEquals("Test workflow goal", graph.getGoal());
        assertEquals(2, graph.getNodeCount());
    }

    @Test
    void testNodeCreation() {
        WorkflowNode node = graph.getNodeByName("Node1");
        assertNotNull(node);
        assertEquals("Node1", node.getName());
        assertEquals("First node", node.getDescription());
        assertEquals(WorkflowNode.NodeType.ACTION, node.getNodeType());
        assertEquals(WorkflowNode.NodeState.PENDING, node.getState());
    }

    @Test
    void testGraphStructure() {
        WorkflowNode node1 = graph.getNodeByName("Node1");
        WorkflowNode node2 = graph.getNodeByName("Node2");

        assertNotNull(node1);
        assertNotNull(node2);

        // 验证边关系
        assertTrue(node1.getSuccessors().contains(node2.getNodeId()));
        assertTrue(node2.getPredecessors().contains(node1.getNodeId()));
    }

    @Test
    void testInitialAndTerminalNodes() {
        var initialNodes = graph.findInitialNodes();
        var terminalNodes = graph.findTerminalNodes();

        assertEquals(1, initialNodes.size());
        assertEquals(1, terminalNodes.size());

        assertEquals("Node1", initialNodes.get(0).getName());
        assertEquals("Node2", terminalNodes.get(0).getName());
    }

    @Test
    void testNodeStateTransitions() {
        WorkflowNode node = graph.getNodeByName("Node1");

        // 初始状态
        assertEquals(WorkflowNode.NodeState.PENDING, node.getState());

        // 标记为就绪
        node.markReady();
        assertEquals(WorkflowNode.NodeState.READY, node.getState());
        assertTrue(node.isReady());

        // 标记为运行
        node.markRunning();
        assertEquals(WorkflowNode.NodeState.RUNNING, node.getState());

        // 标记为完成
        node.markCompleted("test result");
        assertEquals(WorkflowNode.NodeState.COMPLETED, node.getState());
        assertTrue(node.isCompleted());
        assertEquals("test result", node.getResult());
    }

    @Test
    void testNodeFailure() {
        WorkflowNode node = graph.getNodeByName("Node1");

        node.markFailed("Test error");
        assertTrue(node.isFailed());
        assertEquals("Test error", node.getErrorMessage());
    }

    @Test
    void testGraphCompletion() {
        WorkflowNode node1 = graph.getNodeByName("Node1");
        WorkflowNode node2 = graph.getNodeByName("Node2");

        // 初始状态未完成
        assertFalse(graph.isComplete());

        // 完成节点1
        graph.completeNode(node1.getNodeId(), "result1");
        assertFalse(graph.isComplete());

        // 完成节点2
        graph.completeNode(node2.getNodeId(), "result2");
        assertTrue(graph.isComplete());
    }

    @Test
    void testGraphValidation() {
        // 应该通过验证
        assertDoesNotThrow(() -> graph.validate());

        // 创建孤立节点的图
        WorkflowGraph invalidGraph = new WorkflowGraph("Invalid");
        WorkflowNode isolated = new WorkflowNode();
        isolated.setName("Isolated");
        isolated.setNodeType(WorkflowNode.NodeType.ACTION);
        isolated.initModule();

        WorkflowNode another = new WorkflowNode();
        another.setName("Another");
        another.setNodeType(WorkflowNode.NodeType.ACTION);
        another.initModule();

        invalidGraph.addNode(isolated);
        invalidGraph.addNode(another);

        // 应该抛出异常
        assertThrows(IllegalStateException.class, invalidGraph::validate);
    }

    @Test
    void testWorkflowCreation() {
        assertNotNull(workflow);
        assertNotNull(workflow.getWorkflowId());
        assertEquals(graph, workflow.getGraph());
        assertEquals(10, workflow.getMaxExecutionSteps());
        assertEquals(Workflow.WorkflowStatus.PENDING, workflow.getStatus());
    }

    @Test
    void testWorkflowExecution() {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("input", "test data");

        // 注意：现在执行需要 AgentManager，这里仅测试不依赖 agent 的基础功能
        // 完整的执行测试将在集成测试中进行
        log.info("Workflow basic structure test - execution requires AgentManager");
        
        // 验证工作流基础状态
        assertEquals(Workflow.WorkflowStatus.PENDING, workflow.getStatus());
    }

    @Test
    void testWorkflowProgress() {
        assertEquals(0.0, workflow.getProgress());

        WorkflowNode node1 = graph.getNodeByName("Node1");
        graph.completeNode(node1.getNodeId(), "result1");

        assertEquals(50.0, workflow.getProgress());

        WorkflowNode node2 = graph.getNodeByName("Node2");
        graph.completeNode(node2.getNodeId(), "result2");

        assertEquals(100.0, workflow.getProgress());
    }

    @Test
    void testWorkflowReset() {
        WorkflowNode node1 = graph.getNodeByName("Node1");
        graph.completeNode(node1.getNodeId(), "result1");

        assertTrue(graph.getCompletedNodes().contains(node1.getNodeId()));

        // 重置工作流
        workflow.reset();

        assertTrue(graph.getCompletedNodes().isEmpty());
        assertEquals(WorkflowNode.NodeState.PENDING, node1.getState());
        assertNull(node1.getResult());
    }
}
