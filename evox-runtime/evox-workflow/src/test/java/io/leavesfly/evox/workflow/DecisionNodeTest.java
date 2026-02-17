package io.leavesfly.evox.workflow;

import io.leavesfly.evox.core.agent.IAgent;
import io.leavesfly.evox.core.agent.IAgentManager;
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
 * 决策节点测试
 */
@Slf4j
class DecisionNodeTest {

    private IAgentManager agentManager;
    private Workflow workflow;
    private WorkflowGraph graph;

    @BeforeEach
    void setUp() {
        agentManager = createEmptyAgentManager();
        graph = new WorkflowGraph("Decision test workflow");
    }

    @Test
    void testSimpleBooleanDecision() {
        // 创建决策节点
        WorkflowNode decisionNode = new WorkflowNode();
        decisionNode.setName("decision");
        decisionNode.setDescription("Boolean decision");
        decisionNode.setNodeType(WorkflowNode.NodeType.DECISION);
        decisionNode.setCondition("true");
        decisionNode.initModule();

        // 创建两个分支节点（使用 DECISION 节点作为终止节点）
        WorkflowNode trueBranch = new WorkflowNode();
        trueBranch.setName("true_branch");
        trueBranch.setDescription("True path");
        trueBranch.setNodeType(WorkflowNode.NodeType.DECISION);
        trueBranch.setCondition("true");
        trueBranch.initModule();
        
        WorkflowNode falseBranch = new WorkflowNode();
        falseBranch.setName("false_branch");
        falseBranch.setDescription("False path");
        falseBranch.setNodeType(WorkflowNode.NodeType.DECISION);
        falseBranch.setCondition("true");
        falseBranch.initModule();

        // 配置决策分支
        Map<String, String> branches = new HashMap<>();
        branches.put("true", trueBranch.getNodeId());
        branches.put("false", falseBranch.getNodeId());
        decisionNode.setBranches(branches);

        // 构建工作流图
        graph.addNode(decisionNode);
        graph.addNode(trueBranch);
        graph.addNode(falseBranch);

        graph.addEdge(decisionNode.getNodeId(), trueBranch.getNodeId());
        graph.addEdge(decisionNode.getNodeId(), falseBranch.getNodeId());

        // 创建并执行工作流
        workflow = new Workflow();
        workflow.setGraph(graph);
        workflow.setAgentManager(agentManager);
        workflow.setMaxExecutionSteps(10);
        workflow.initModule();

        Map<String, Object> inputs = new HashMap<>();
        String result = workflow.execute(inputs);

        System.out.println("=== Test Result: " + result);
        System.out.println("=== True branch state: " + trueBranch.getState());
        System.out.println("=== False branch state: " + falseBranch.getState());
        
        assertNotNull(result, "Result should not be null");
        log.info("Boolean decision result: {}", result);
        
        // 验证 true 分支被执行，false 分支被跳过
        assertEquals(WorkflowNode.NodeState.COMPLETED, trueBranch.getState());
        assertEquals(WorkflowNode.NodeState.SKIPPED, falseBranch.getState());
    }

    @Test
    void testConditionWithContextData() {
        // 创建决策节点（基于上下文数据）
        WorkflowNode decisionNode = new WorkflowNode();
        decisionNode.setName("check_status");
        decisionNode.setDescription("Check status from context");
        decisionNode.setNodeType(WorkflowNode.NodeType.DECISION);
        decisionNode.setCondition("status == success");
        decisionNode.initModule();

        // 创建分支节点
        WorkflowNode successBranch = new WorkflowNode();
        successBranch.setName("success_handler");
        successBranch.setDescription("Success handler");
        successBranch.setNodeType(WorkflowNode.NodeType.DECISION);
        successBranch.setCondition("true");
        successBranch.initModule();
        
        WorkflowNode failureBranch = new WorkflowNode();
        failureBranch.setName("failure_handler");
        failureBranch.setDescription("Failure handler");
        failureBranch.setNodeType(WorkflowNode.NodeType.DECISION);
        failureBranch.setCondition("true");
        failureBranch.initModule();

        // 配置分支
        Map<String, String> branches = new HashMap<>();
        branches.put("true", successBranch.getNodeId());
        branches.put("false", failureBranch.getNodeId());
        decisionNode.setBranches(branches);

        // 构建图
        graph.addNode(decisionNode);
        graph.addNode(successBranch);
        graph.addNode(failureBranch);

        graph.addEdge(decisionNode.getNodeId(), successBranch.getNodeId());
        graph.addEdge(decisionNode.getNodeId(), failureBranch.getNodeId());

        // 执行工作流
        workflow = new Workflow();
        workflow.setGraph(graph);
        workflow.setAgentManager(agentManager);
        workflow.initModule();

        Map<String, Object> inputs = new HashMap<>();
        inputs.put("status", "success");
        
        String result = workflow.execute(inputs);
        
        assertNotNull(result);
        log.info("Context-based decision result: {}", result);
    }

    @Test
    void testNumericComparison() {
        // 创建决策节点（数值比较）
        WorkflowNode decisionNode = new WorkflowNode();
        decisionNode.setName("check_count");
        decisionNode.setDescription("Check if count > 5");
        decisionNode.setNodeType(WorkflowNode.NodeType.DECISION);
        decisionNode.setCondition("count > 5");
        decisionNode.initModule();

        // 创建分支
        WorkflowNode highCountBranch = new WorkflowNode();
        highCountBranch.setName("high_count");
        highCountBranch.setDescription("High count handler");
        highCountBranch.setNodeType(WorkflowNode.NodeType.DECISION);
        highCountBranch.setCondition("true");
        highCountBranch.initModule();
        
        WorkflowNode lowCountBranch = new WorkflowNode();
        lowCountBranch.setName("low_count");
        lowCountBranch.setDescription("Low count handler");
        lowCountBranch.setNodeType(WorkflowNode.NodeType.DECISION);
        lowCountBranch.setCondition("true");
        lowCountBranch.initModule();

        Map<String, String> branches = new HashMap<>();
        branches.put("true", highCountBranch.getNodeId());
        branches.put("false", lowCountBranch.getNodeId());
        decisionNode.setBranches(branches);

        // 构建图
        graph.addNode(decisionNode);
        graph.addNode(highCountBranch);
        graph.addNode(lowCountBranch);

        graph.addEdge(decisionNode.getNodeId(), highCountBranch.getNodeId());
        graph.addEdge(decisionNode.getNodeId(), lowCountBranch.getNodeId());

        // 执行工作流
        workflow = new Workflow();
        workflow.setGraph(graph);
        workflow.setAgentManager(agentManager);
        workflow.initModule();

        Map<String, Object> inputs = new HashMap<>();
        inputs.put("count", 10);
        
        String result = workflow.execute(inputs);
        
        assertNotNull(result);
        log.info("Numeric comparison result: {}", result);
        
        // count=10 > 5, 应该执行 high_count 分支
        assertEquals(WorkflowNode.NodeState.COMPLETED, highCountBranch.getState());
        assertEquals(WorkflowNode.NodeState.SKIPPED, lowCountBranch.getState());
    }

    @Test
    void testDefaultBranch() {
        // 创建决策节点（无条件，使用默认分支）
        WorkflowNode decisionNode = new WorkflowNode();
        decisionNode.setName("default_decision");
        decisionNode.setDescription("Decision with default branch");
        decisionNode.setNodeType(WorkflowNode.NodeType.DECISION);
        decisionNode.initModule();

        // 创建分支
        WorkflowNode defaultBranch = new WorkflowNode();
        defaultBranch.setName("default");
        defaultBranch.setDescription("Default handler");
        defaultBranch.setNodeType(WorkflowNode.NodeType.DECISION);
        defaultBranch.setCondition("true");
        defaultBranch.initModule();
        
        WorkflowNode otherBranch = new WorkflowNode();
        otherBranch.setName("other");
        otherBranch.setDescription("Other handler");
        otherBranch.setNodeType(WorkflowNode.NodeType.DECISION);
        otherBranch.setCondition("true");
        otherBranch.initModule();

        // 构建图
        graph.addNode(decisionNode);
        graph.addNode(defaultBranch);
        graph.addNode(otherBranch);

        graph.addEdge(decisionNode.getNodeId(), defaultBranch.getNodeId());
        graph.addEdge(decisionNode.getNodeId(), otherBranch.getNodeId());

        // 执行工作流
        workflow = new Workflow();
        workflow.setGraph(graph);
        workflow.setAgentManager(agentManager);
        workflow.initModule();

        String result = workflow.execute(new HashMap<>());
        
        assertNotNull(result);
        log.info("Default branch result: {}", result);
        
        // 第一个后继节点应该被执行
        assertTrue(defaultBranch.getState() == WorkflowNode.NodeState.COMPLETED);
    }

    @Test
    void testMultipleDecisions() {
        // 创建第一个决策
        WorkflowNode decision1 = new WorkflowNode();
        decision1.setName("decision1");
        decision1.setDescription("First decision");
        decision1.setNodeType(WorkflowNode.NodeType.DECISION);
        decision1.setCondition("true");
        decision1.initModule();

        // 创建中间节点
        WorkflowNode middleNode = new WorkflowNode();
        middleNode.setName("middle");
        middleNode.setDescription("Middle processing");
        middleNode.setNodeType(WorkflowNode.NodeType.DECISION);
        middleNode.setCondition("true");
        middleNode.initModule();

        // 创建第二个决策
        WorkflowNode decision2 = new WorkflowNode();
        decision2.setName("decision2");
        decision2.setDescription("Second decision");
        decision2.setNodeType(WorkflowNode.NodeType.DECISION);
        decision2.setCondition("false");
        decision2.initModule();

        // 创建最终分支
        WorkflowNode finalBranch1 = new WorkflowNode();
        finalBranch1.setName("final1");
        finalBranch1.setDescription("Final path 1");
        finalBranch1.setNodeType(WorkflowNode.NodeType.DECISION);
        finalBranch1.setCondition("true");
        finalBranch1.initModule();
        
        WorkflowNode finalBranch2 = new WorkflowNode();
        finalBranch2.setName("final2");
        finalBranch2.setDescription("Final path 2");
        finalBranch2.setNodeType(WorkflowNode.NodeType.DECISION);
        finalBranch2.setCondition("true");
        finalBranch2.initModule();

        // 配置第一个决策
        Map<String, String> branches1 = new HashMap<>();
        branches1.put("true", middleNode.getNodeId());
        decision1.setBranches(branches1);

        // 配置第二个决策
        Map<String, String> branches2 = new HashMap<>();
        branches2.put("true", finalBranch1.getNodeId());
        branches2.put("false", finalBranch2.getNodeId());
        decision2.setBranches(branches2);

        // 构建图
        graph.addNode(decision1);
        graph.addNode(middleNode);
        graph.addNode(decision2);
        graph.addNode(finalBranch1);
        graph.addNode(finalBranch2);

        graph.addEdge(decision1.getNodeId(), middleNode.getNodeId());
        graph.addEdge(middleNode.getNodeId(), decision2.getNodeId());
        graph.addEdge(decision2.getNodeId(), finalBranch1.getNodeId());
        graph.addEdge(decision2.getNodeId(), finalBranch2.getNodeId());

        // 执行工作流
        workflow = new Workflow();
        workflow.setGraph(graph);
        workflow.setAgentManager(agentManager);
        workflow.setMaxExecutionSteps(10);
        workflow.initModule();

        String result = workflow.execute(new HashMap<>());
        
        assertNotNull(result);
        log.info("Multiple decisions result: {}", result);
        
        // 验证执行路径
        assertEquals(WorkflowNode.NodeState.COMPLETED, decision1.getState());
        assertEquals(WorkflowNode.NodeState.COMPLETED, middleNode.getState());
        assertEquals(WorkflowNode.NodeState.COMPLETED, decision2.getState());
        assertEquals(WorkflowNode.NodeState.SKIPPED, finalBranch1.getState());
        assertEquals(WorkflowNode.NodeState.COMPLETED, finalBranch2.getState());
    }

    /**
     * 创建动作节点辅助方法
     */
    private WorkflowNode createActionNode(String name, String description) {
        WorkflowNode node = new WorkflowNode();
        node.setName(name);
        node.setDescription(description);
        node.setNodeType(WorkflowNode.NodeType.ACTION);
        node.initModule();
        return node;
    }

    /**
     * 创建空的 IAgentManager 实现（测试用，不依赖 evox-agents）
     */
    private IAgentManager createEmptyAgentManager() {
        return new IAgentManager() {
            private final Map<String, IAgent> agents = new HashMap<>();

            @Override
            public IAgent getAgent(String name) { return agents.get(name); }

            @Override
            public IAgent getAgentById(String agentId) { return null; }

            @Override
            public void addAgent(IAgent agent) { }

            @Override
            public IAgent removeAgent(String name) { return agents.remove(name); }

            @Override
            public boolean hasAgent(String name) { return agents.containsKey(name); }

            @Override
            public Map<String, IAgent> getAllAgents() { return agents; }

            @Override
            public int getAgentCount() { return agents.size(); }

            @Override
            public void clear() { agents.clear(); }
        };
    }
}
