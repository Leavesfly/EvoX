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
 * 循环节点测试
 */
@Slf4j
class LoopNodeTest {

    private AgentManager agentManager;
    private Workflow workflow;
    private WorkflowGraph graph;

    @BeforeEach
    void setUp() {
        agentManager = new AgentManager();
        graph = new WorkflowGraph("Loop test workflow");
    }

    @Test
    void testSimpleLoop() {
        // 创建循环节点
        WorkflowNode loopNode = new WorkflowNode();
        loopNode.setName("simple_loop");
        loopNode.setDescription("Simple loop with max iterations");
        loopNode.setNodeType(WorkflowNode.NodeType.LOOP);
        loopNode.setMaxIterations(5);
        loopNode.initModule();

        // 创建循环体（使用 DECISION 节点作为简单任务）
        WorkflowNode loopBody = createDecisionNode("loop_body", "Loop body task");
        loopNode.setLoopBodyNodeId(loopBody.getNodeId());

        // 构建图
        graph.addNode(loopNode);
        graph.addNode(loopBody);
        graph.addEdge(loopNode.getNodeId(), loopBody.getNodeId());

        // 执行工作流
        workflow = new Workflow();
        workflow.setGraph(graph);
        workflow.setAgentManager(agentManager);
        workflow.initModule();

        String result = workflow.execute(new HashMap<>());

        assertNotNull(result, "Result should not be null");
        log.info("Simple loop result: {}", result);

        // 验证循环执行了5次
        assertEquals(5, loopNode.getCurrentIteration());
    }

    @Test
    void testLoopWithCondition() {
        // 创建循环节点（带条件）
        WorkflowNode loopNode = new WorkflowNode();
        loopNode.setName("conditional_loop");
        loopNode.setDescription("Loop with exit condition");
        loopNode.setNodeType(WorkflowNode.NodeType.LOOP);
        loopNode.setLoopCondition("loop_iteration < 3"); // 循环3次
        loopNode.setMaxIterations(10); // 最大10次，但条件会让它提前退出
        loopNode.initModule();

        // 创建循环体
        WorkflowNode loopBody = createDecisionNode("body", "Task");
        loopNode.setLoopBodyNodeId(loopBody.getNodeId());

        // 构建图
        graph.addNode(loopNode);
        graph.addNode(loopBody);
        graph.addEdge(loopNode.getNodeId(), loopBody.getNodeId());

        // 执行工作流
        workflow = new Workflow();
        workflow.setGraph(graph);
        workflow.setAgentManager(agentManager);
        workflow.initModule();

        String result = workflow.execute(new HashMap<>());

        assertNotNull(result, "Result should not be null");
        log.info("Conditional loop result: {}", result);

        // 验证循环因条件退出（应该是3次）
        assertTrue(loopNode.getCurrentIteration() <= 3, 
                   "Loop should exit by condition before max iterations");
    }

    @Test
    void testLoopWithDefaultBody() {
        // 创建循环节点（不显式设置 loopBodyNodeId，使用第一个 successor）
        WorkflowNode loopNode = new WorkflowNode();
        loopNode.setName("default_body_loop");
        loopNode.setDescription("Loop with default body");
        loopNode.setNodeType(WorkflowNode.NodeType.LOOP);
        loopNode.setMaxIterations(3);
        loopNode.initModule();

        // 创建循环体
        WorkflowNode loopBody = createDecisionNode("body", "Default body");

        // 构建图
        graph.addNode(loopNode);
        graph.addNode(loopBody);
        graph.addEdge(loopNode.getNodeId(), loopBody.getNodeId());

        // 执行工作流
        workflow = new Workflow();
        workflow.setGraph(graph);
        workflow.setAgentManager(agentManager);
        workflow.initModule();

        String result = workflow.execute(new HashMap<>());

        assertNotNull(result, "Result should not be null");
        log.info("Default body loop result: {}", result);

        assertEquals(3, loopNode.getCurrentIteration());
    }

    @Test
    void testNestedLoop() {
        // 创建外层循环
        WorkflowNode outerLoop = new WorkflowNode();
        outerLoop.setName("outer_loop");
        outerLoop.setDescription("Outer loop");
        outerLoop.setNodeType(WorkflowNode.NodeType.LOOP);
        outerLoop.setMaxIterations(2);
        outerLoop.initModule();

        // 创建内层循环
        WorkflowNode innerLoop = new WorkflowNode();
        innerLoop.setName("inner_loop");
        innerLoop.setDescription("Inner loop");
        innerLoop.setNodeType(WorkflowNode.NodeType.LOOP);
        innerLoop.setMaxIterations(2);
        innerLoop.initModule();

        // 创建最内层任务
        WorkflowNode task = createDecisionNode("task", "Inner task");

        // 配置嵌套结构
        outerLoop.setLoopBodyNodeId(innerLoop.getNodeId());
        innerLoop.setLoopBodyNodeId(task.getNodeId());

        // 构建图
        graph.addNode(outerLoop);
        graph.addNode(innerLoop);
        graph.addNode(task);

        graph.addEdge(outerLoop.getNodeId(), innerLoop.getNodeId());
        graph.addEdge(innerLoop.getNodeId(), task.getNodeId());

        // 执行工作流
        workflow = new Workflow();
        workflow.setGraph(graph);
        workflow.setAgentManager(agentManager);
        workflow.setMaxExecutionSteps(20);
        workflow.initModule();

        String result = workflow.execute(new HashMap<>());

        assertNotNull(result, "Result should not be null");
        log.info("Nested loop result: {}", result);

        // 外层循环应该执行2次
        assertEquals(2, outerLoop.getCurrentIteration());
    }

    @Test
    void testLoopMixedWithDecision() {
        // 创建决策节点
        WorkflowNode decision = new WorkflowNode();
        decision.setName("decision");
        decision.setDescription("Initial decision");
        decision.setNodeType(WorkflowNode.NodeType.DECISION);
        decision.setCondition("true");
        decision.initModule();

        // 创建循环节点
        WorkflowNode loop = new WorkflowNode();
        loop.setName("loop");
        loop.setDescription("Loop after decision");
        loop.setNodeType(WorkflowNode.NodeType.LOOP);
        loop.setMaxIterations(3);
        loop.initModule();

        // 创建循环体
        WorkflowNode loopBody = createDecisionNode("body", "Loop body");
        loop.setLoopBodyNodeId(loopBody.getNodeId());

        // 配置决策分支指向循环
        Map<String, String> branches = new HashMap<>();
        branches.put("true", loop.getNodeId());
        decision.setBranches(branches);

        // 构建图
        graph.addNode(decision);
        graph.addNode(loop);
        graph.addNode(loopBody);

        graph.addEdge(decision.getNodeId(), loop.getNodeId());
        graph.addEdge(loop.getNodeId(), loopBody.getNodeId());

        // 执行工作流
        workflow = new Workflow();
        workflow.setGraph(graph);
        workflow.setAgentManager(agentManager);
        workflow.setMaxExecutionSteps(15);
        workflow.initModule();

        String result = workflow.execute(new HashMap<>());

        assertNotNull(result, "Result should not be null");
        log.info("Decision-loop mixed result: {}", result);

        // 验证执行路径
        assertEquals(WorkflowNode.NodeState.COMPLETED, decision.getState());
        assertEquals(WorkflowNode.NodeState.COMPLETED, loop.getState());
        assertEquals(3, loop.getCurrentIteration());
    }

    /**
     * 创建决策节点辅助方法
     */
    private WorkflowNode createDecisionNode(String name, String description) {
        WorkflowNode node = new WorkflowNode();
        node.setName(name);
        node.setDescription(description);
        node.setNodeType(WorkflowNode.NodeType.DECISION);
        node.setCondition("true");
        node.initModule();
        return node;
    }
}
