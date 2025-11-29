package io.leavesfly.evox.workflow;

import io.leavesfly.evox.agents.manager.AgentManager;
import io.leavesfly.evox.workflow.base.Workflow;
import io.leavesfly.evox.workflow.base.WorkflowNode;
import io.leavesfly.evox.workflow.graph.WorkflowGraph;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 并行节点测试
 */
@Slf4j
class ParallelNodeTest {

    private AgentManager agentManager;
    private Workflow workflow;
    private WorkflowGraph graph;

    @BeforeEach
    void setUp() {
        agentManager = new AgentManager();
        graph = new WorkflowGraph("Parallel test workflow");
    }

    @Test
    void testParallelAllStrategy() {
        // 创建并行节点
        WorkflowNode parallelNode = new WorkflowNode();
        parallelNode.setName("parallel_all");
        parallelNode.setDescription("Parallel execution with ALL strategy");
        parallelNode.setNodeType(WorkflowNode.NodeType.PARALLEL);
        parallelNode.setParallelStrategy(WorkflowNode.ParallelStrategy.ALL);
        parallelNode.initModule();

        // 创建3个子节点（使用 DECISION 节点作为简单任务）
        WorkflowNode child1 = createDecisionNode("child1", "Child task 1");
        WorkflowNode child2 = createDecisionNode("child2", "Child task 2");
        WorkflowNode child3 = createDecisionNode("child3", "Child task 3");

        // 配置并行节点的子节点
        parallelNode.setParallelNodes(List.of(child1.getNodeId(), child2.getNodeId(), child3.getNodeId()));

        // 构建图
        graph.addNode(parallelNode);
        graph.addNode(child1);
        graph.addNode(child2);
        graph.addNode(child3);

        graph.addEdge(parallelNode.getNodeId(), child1.getNodeId());
        graph.addEdge(parallelNode.getNodeId(), child2.getNodeId());
        graph.addEdge(parallelNode.getNodeId(), child3.getNodeId());

        // 执行工作流
        workflow = new Workflow();
        workflow.setGraph(graph);
        workflow.setAgentManager(agentManager);
        workflow.initModule();

        String result = workflow.execute(new HashMap<>());

        assertNotNull(result, "Result should not be null");
        log.info("Parallel ALL result: {}", result);

        // 验证所有子节点都完成了
        assertEquals(WorkflowNode.NodeState.COMPLETED, child1.getState());
        assertEquals(WorkflowNode.NodeState.COMPLETED, child2.getState());
        assertEquals(WorkflowNode.NodeState.COMPLETED, child3.getState());
    }

    @Test
    void testParallelAnyStrategy() {
        // 创建并行节点（ANY 策略）
        WorkflowNode parallelNode = new WorkflowNode();
        parallelNode.setName("parallel_any");
        parallelNode.setDescription("Parallel execution with ANY strategy");
        parallelNode.setNodeType(WorkflowNode.NodeType.PARALLEL);
        parallelNode.setParallelStrategy(WorkflowNode.ParallelStrategy.ANY);
        parallelNode.initModule();

        // 创建子节点
        WorkflowNode child1 = createDecisionNode("child1", "Child task 1");
        WorkflowNode child2 = createDecisionNode("child2", "Child task 2");

        parallelNode.setParallelNodes(List.of(child1.getNodeId(), child2.getNodeId()));

        // 构建图
        graph.addNode(parallelNode);
        graph.addNode(child1);
        graph.addNode(child2);

        graph.addEdge(parallelNode.getNodeId(), child1.getNodeId());
        graph.addEdge(parallelNode.getNodeId(), child2.getNodeId());

        // 执行工作流
        workflow = new Workflow();
        workflow.setGraph(graph);
        workflow.setAgentManager(agentManager);
        workflow.initModule();

        String result = workflow.execute(new HashMap<>());

        assertNotNull(result, "Result should not be null");
        log.info("Parallel ANY result: {}", result);

        // ANY 策略至少有一个节点完成
        assertTrue(child1.getState() == WorkflowNode.NodeState.COMPLETED || 
                   child2.getState() == WorkflowNode.NodeState.COMPLETED,
                   "At least one child should be completed");
    }

    @Test
    void testParallelWithDefaultSuccessors() {
        // 创建并行节点（不显式配置 parallelNodes，使用 successors）
        WorkflowNode parallelNode = new WorkflowNode();
        parallelNode.setName("parallel_default");
        parallelNode.setDescription("Parallel with default successors");
        parallelNode.setNodeType(WorkflowNode.NodeType.PARALLEL);
        parallelNode.initModule();

        // 创建子节点
        WorkflowNode child1 = createDecisionNode("child1", "Child 1");
        WorkflowNode child2 = createDecisionNode("child2", "Child 2");

        // 不设置 parallelNodes，依赖 successors

        // 构建图
        graph.addNode(parallelNode);
        graph.addNode(child1);
        graph.addNode(child2);

        graph.addEdge(parallelNode.getNodeId(), child1.getNodeId());
        graph.addEdge(parallelNode.getNodeId(), child2.getNodeId());

        // 执行工作流
        workflow = new Workflow();
        workflow.setGraph(graph);
        workflow.setAgentManager(agentManager);
        workflow.initModule();

        String result = workflow.execute(new HashMap<>());

        assertNotNull(result, "Result should not be null");
        log.info("Parallel default result: {}", result);
    }

    @Test
    void testNestedParallel() {
        // 创建外层并行节点
        WorkflowNode outerParallel = new WorkflowNode();
        outerParallel.setName("outer_parallel");
        outerParallel.setDescription("Outer parallel node");
        outerParallel.setNodeType(WorkflowNode.NodeType.PARALLEL);
        outerParallel.initModule();

        // 创建内层并行节点
        WorkflowNode innerParallel = new WorkflowNode();
        innerParallel.setName("inner_parallel");
        innerParallel.setDescription("Inner parallel node");
        innerParallel.setNodeType(WorkflowNode.NodeType.PARALLEL);
        innerParallel.initModule();

        // 创建叶子节点
        WorkflowNode leaf1 = createDecisionNode("leaf1", "Leaf 1");
        WorkflowNode leaf2 = createDecisionNode("leaf2", "Leaf 2");
        WorkflowNode leaf3 = createDecisionNode("leaf3", "Leaf 3");

        // 配置嵌套结构
        innerParallel.setParallelNodes(List.of(leaf1.getNodeId(), leaf2.getNodeId()));
        outerParallel.setParallelNodes(List.of(innerParallel.getNodeId(), leaf3.getNodeId()));

        // 构建图
        graph.addNode(outerParallel);
        graph.addNode(innerParallel);
        graph.addNode(leaf1);
        graph.addNode(leaf2);
        graph.addNode(leaf3);

        graph.addEdge(outerParallel.getNodeId(), innerParallel.getNodeId());
        graph.addEdge(outerParallel.getNodeId(), leaf3.getNodeId());
        graph.addEdge(innerParallel.getNodeId(), leaf1.getNodeId());
        graph.addEdge(innerParallel.getNodeId(), leaf2.getNodeId());

        // 执行工作流
        workflow = new Workflow();
        workflow.setGraph(graph);
        workflow.setAgentManager(agentManager);
        workflow.setMaxExecutionSteps(20);
        workflow.initModule();

        String result = workflow.execute(new HashMap<>());

        assertNotNull(result, "Result should not be null");
        log.info("Nested parallel result: {}", result);
    }

    @Test
    void testParallelMixedWithDecision() {
        // 创建决策节点
        WorkflowNode decision = new WorkflowNode();
        decision.setName("decision");
        decision.setDescription("Initial decision");
        decision.setNodeType(WorkflowNode.NodeType.DECISION);
        decision.setCondition("true");
        decision.initModule();

        // 创建并行节点
        WorkflowNode parallel = new WorkflowNode();
        parallel.setName("parallel");
        parallel.setDescription("Parallel tasks");
        parallel.setNodeType(WorkflowNode.NodeType.PARALLEL);
        parallel.initModule();

        // 创建子节点
        WorkflowNode task1 = createDecisionNode("task1", "Task 1");
        WorkflowNode task2 = createDecisionNode("task2", "Task 2");

        // 配置决策分支指向并行节点
        Map<String, String> branches = new HashMap<>();
        branches.put("true", parallel.getNodeId());
        decision.setBranches(branches);

        parallel.setParallelNodes(List.of(task1.getNodeId(), task2.getNodeId()));

        // 构建图
        graph.addNode(decision);
        graph.addNode(parallel);
        graph.addNode(task1);
        graph.addNode(task2);

        graph.addEdge(decision.getNodeId(), parallel.getNodeId());
        graph.addEdge(parallel.getNodeId(), task1.getNodeId());
        graph.addEdge(parallel.getNodeId(), task2.getNodeId());

        // 执行工作流
        workflow = new Workflow();
        workflow.setGraph(graph);
        workflow.setAgentManager(agentManager);
        workflow.setMaxExecutionSteps(10);
        workflow.initModule();

        String result = workflow.execute(new HashMap<>());

        assertNotNull(result, "Result should not be null");
        log.info("Mixed decision-parallel result: {}", result);

        // 验证执行路径
        assertEquals(WorkflowNode.NodeState.COMPLETED, decision.getState());
        assertEquals(WorkflowNode.NodeState.COMPLETED, parallel.getState());
        assertEquals(WorkflowNode.NodeState.COMPLETED, task1.getState());
        assertEquals(WorkflowNode.NodeState.COMPLETED, task2.getState());
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
