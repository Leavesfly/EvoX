package io.leavesfly.evox.examples;

import io.leavesfly.evox.actions.base.Action;
import io.leavesfly.evox.actions.base.ActionInput;
import io.leavesfly.evox.actions.base.ActionOutput;
import io.leavesfly.evox.actions.base.SimpleActionOutput;
import io.leavesfly.evox.agents.base.Agent;
import io.leavesfly.evox.agents.manager.AgentManager;
import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.core.message.MessageType;
import io.leavesfly.evox.workflow.base.Workflow;
import io.leavesfly.evox.workflow.base.WorkflowNode;
import io.leavesfly.evox.workflow.graph.WorkflowGraph;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * WorkflowDemo 示例应用
 * 
 * 展示如何使用 EvoX 框架构建和执行工作流：
 * - 创建多个 Agent 和 Action
 * - 构建工作流图（包含决策、并行、循环节点）
 * - 执行工作流并查看结果
 */
@Slf4j
public class WorkflowDemo {

    public static void main(String[] args) {
        log.info("=== WorkflowDemo 示例应用启动 ===");
        
        // 运行不同的工作流示例
        runSequentialWorkflow();
        runDecisionWorkflow();
        runParallelWorkflow();
        runLoopWorkflow();
    }

    /**
     * 示例 1: 顺序工作流
     */
    private static void runSequentialWorkflow() {
        log.info("\n=== 示例 1: 顺序工作流 ===");
        
        // 创建 AgentManager
        AgentManager agentManager = new AgentManager();
        
        // 创建数据处理 Agent
        DataProcessorAgent agent = new DataProcessorAgent();
        agent.setName("DataProcessor");
        agent.setDescription("数据处理智能体");
        agent.initModule();
        agentManager.addAgent(agent);
        
        // 创建工作流图
        WorkflowGraph graph = new WorkflowGraph("顺序数据处理工作流");
        
        // 节点 1: 数据验证
        WorkflowNode validateNode = new WorkflowNode();
        validateNode.setName("DataProcessor.validate");
        validateNode.setDescription("验证输入数据");
        validateNode.setNodeType(WorkflowNode.NodeType.ACTION);
        validateNode.initModule();
        
        // 节点 2: 数据转换
        WorkflowNode transformNode = new WorkflowNode();
        transformNode.setName("DataProcessor.transform");
        transformNode.setDescription("转换数据格式");
        transformNode.setNodeType(WorkflowNode.NodeType.ACTION);
        transformNode.initModule();
        
        // 节点 3: 数据汇总
        WorkflowNode summarizeNode = new WorkflowNode();
        summarizeNode.setName("DataProcessor.summarize");
        summarizeNode.setDescription("汇总处理结果");
        summarizeNode.setNodeType(WorkflowNode.NodeType.ACTION);
        summarizeNode.initModule();
        
        // 构建图
        graph.addNode(validateNode);
        graph.addNode(transformNode);
        graph.addNode(summarizeNode);
        graph.addEdge(validateNode.getNodeId(), transformNode.getNodeId());
        graph.addEdge(transformNode.getNodeId(), summarizeNode.getNodeId());
        
        // 创建工作流
        Workflow workflow = new Workflow();
        workflow.setName("顺序工作流");
        workflow.setGraph(graph);
        workflow.setAgentManager(agentManager);
        workflow.setMaxExecutionSteps(10);
        workflow.initModule();
        
        // 准备输入
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("data", Arrays.asList(1, 2, 3, 4, 5));
        
        // 执行工作流
        String result = workflow.execute(inputs);
        
        log.info("工作流执行完成");
        log.info("进度: {}%", workflow.getProgress());
        log.info("结果: {}", result);
    }

    /**
     * 示例 2: 决策工作流
     */
    private static void runDecisionWorkflow() {
        log.info("\n=== 示例 2: 决策工作流 ===");
        
        AgentManager agentManager = new AgentManager();
        
        DataProcessorAgent agent = new DataProcessorAgent();
        agent.setName("DataProcessor");
        agent.setDescription("数据处理智能体");
        agent.initModule();
        agentManager.addAgent(agent);
        
        WorkflowGraph graph = new WorkflowGraph("决策工作流");
        
        // 节点 1: 数据验证
        WorkflowNode validateNode = new WorkflowNode();
        validateNode.setName("DataProcessor.validate");
        validateNode.setDescription("验证输入数据");
        validateNode.setNodeType(WorkflowNode.NodeType.ACTION);
        validateNode.initModule();
        
        // 节点 2: 决策节点
        WorkflowNode decisionNode = new WorkflowNode();
        decisionNode.setName("decision");
        decisionNode.setDescription("根据数据量选择处理方式");
        decisionNode.setNodeType(WorkflowNode.NodeType.DECISION);
        decisionNode.setCondition("context.dataSize > 3");
        decisionNode.initModule();
        
        // 节点 3a: 大数据集处理
        WorkflowNode transformNode = new WorkflowNode();
        transformNode.setName("DataProcessor.transform");
        transformNode.setDescription("转换大数据集");
        transformNode.setNodeType(WorkflowNode.NodeType.ACTION);
        transformNode.initModule();
        
        // 节点 3b: 小数据集处理
        WorkflowNode summarizeNode = new WorkflowNode();
        summarizeNode.setName("DataProcessor.summarize");
        summarizeNode.setDescription("直接汇总小数据集");
        summarizeNode.setNodeType(WorkflowNode.NodeType.ACTION);
        summarizeNode.initModule();
        
        // 构建图
        graph.addNode(validateNode);
        graph.addNode(decisionNode);
        graph.addNode(transformNode);
        graph.addNode(summarizeNode);
        
        graph.addEdge(validateNode.getNodeId(), decisionNode.getNodeId());
        
        // 决策分支映射
        Map<String, String> branches = new HashMap<>();
        branches.put("true", transformNode.getNodeId());
        branches.put("false", summarizeNode.getNodeId());
        decisionNode.setBranchMapping(branches);
        
        // 创建工作流
        Workflow workflow = new Workflow();
        workflow.setName("决策工作流");
        workflow.setGraph(graph);
        workflow.setAgentManager(agentManager);
        workflow.setMaxExecutionSteps(10);
        workflow.initModule();
        
        // 测试不同大小的数据集
        for (int size : new int[]{2, 5}) {
            log.info("\n--- 测试数据集大小: {} ---", size);
            
            List<Integer> data = new ArrayList<>();
            for (int i = 1; i <= size; i++) {
                data.add(i);
            }
            
            Map<String, Object> inputs = new HashMap<>();
            inputs.put("data", data);
            
            String result = workflow.execute(inputs);
            log.info("执行结果: {}", result);
            
            // 重置工作流以便下次执行
            workflow.reset();
        }
    }

    /**
     * 示例 3: 并行工作流
     */
    private static void runParallelWorkflow() {
        log.info("\n=== 示例 3: 并行工作流 ===");
        
        AgentManager agentManager = new AgentManager();
        
        DataProcessorAgent agent = new DataProcessorAgent();
        agent.setName("DataProcessor");
        agent.initModule();
        agentManager.addAgent(agent);
        
        WorkflowGraph graph = new WorkflowGraph("并行工作流");
        
        // 节点 1: 数据准备
        WorkflowNode prepareNode = new WorkflowNode();
        prepareNode.setName("DataProcessor.validate");
        prepareNode.setDescription("准备数据");
        prepareNode.setNodeType(WorkflowNode.NodeType.ACTION);
        prepareNode.initModule();
        
        // 节点 2: 并行节点
        WorkflowNode parallelNode = new WorkflowNode();
        parallelNode.setName("parallel");
        parallelNode.setDescription("并行处理数据");
        parallelNode.setNodeType(WorkflowNode.NodeType.PARALLEL);
        parallelNode.setParallelStrategy(WorkflowNode.ParallelStrategy.ALL);
        parallelNode.initModule();
        
        // 并行子节点 1: 数据转换
        WorkflowNode transformNode = new WorkflowNode();
        transformNode.setName("DataProcessor.transform");
        transformNode.setDescription("数据转换");
        transformNode.setNodeType(WorkflowNode.NodeType.ACTION);
        transformNode.initModule();
        
        // 并行子节点 2: 数据汇总
        WorkflowNode summarizeNode = new WorkflowNode();
        summarizeNode.setName("DataProcessor.summarize");
        summarizeNode.setDescription("数据汇总");
        summarizeNode.setNodeType(WorkflowNode.NodeType.ACTION);
        summarizeNode.initModule();
        
        // 构建图
        graph.addNode(prepareNode);
        graph.addNode(parallelNode);
        graph.addNode(transformNode);
        graph.addNode(summarizeNode);
        
        graph.addEdge(prepareNode.getNodeId(), parallelNode.getNodeId());
        graph.addEdge(parallelNode.getNodeId(), transformNode.getNodeId());
        graph.addEdge(parallelNode.getNodeId(), summarizeNode.getNodeId());
        
        // 创建工作流
        Workflow workflow = new Workflow();
        workflow.setName("并行工作流");
        workflow.setGraph(graph);
        workflow.setAgentManager(agentManager);
        workflow.setMaxExecutionSteps(10);
        workflow.initModule();
        
        // 执行
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("data", Arrays.asList(1, 2, 3, 4, 5));
        
        String result = workflow.execute(inputs);
        log.info("并行工作流执行完成: {}", result);
    }

    /**
     * 示例 4: 循环工作流
     */
    private static void runLoopWorkflow() {
        log.info("\n=== 示例 4: 循环工作流 ===");
        
        AgentManager agentManager = new AgentManager();
        
        DataProcessorAgent agent = new DataProcessorAgent();
        agent.setName("DataProcessor");
        agent.initModule();
        agentManager.addAgent(agent);
        
        WorkflowGraph graph = new WorkflowGraph("循环工作流");
        
        // 节点 1: 初始化计数器
        WorkflowNode initNode = new WorkflowNode();
        initNode.setName("DataProcessor.validate");
        initNode.setDescription("初始化");
        initNode.setNodeType(WorkflowNode.NodeType.ACTION);
        initNode.initModule();
        
        // 节点 2: 循环节点
        WorkflowNode loopNode = new WorkflowNode();
        loopNode.setName("loop");
        loopNode.setDescription("循环处理数据");
        loopNode.setNodeType(WorkflowNode.NodeType.LOOP);
        loopNode.setMaxIterations(5);
        loopNode.initModule();
        
        // 循环体: 数据转换
        WorkflowNode transformNode = new WorkflowNode();
        transformNode.setName("DataProcessor.transform");
        transformNode.setDescription("转换数据");
        transformNode.setNodeType(WorkflowNode.NodeType.ACTION);
        transformNode.initModule();
        
        // 构建图
        graph.addNode(initNode);
        graph.addNode(loopNode);
        graph.addNode(transformNode);
        
        graph.addEdge(initNode.getNodeId(), loopNode.getNodeId());
        graph.addEdge(loopNode.getNodeId(), transformNode.getNodeId());
        
        // 创建工作流
        Workflow workflow = new Workflow();
        workflow.setName("循环工作流");
        workflow.setGraph(graph);
        workflow.setAgentManager(agentManager);
        workflow.setMaxExecutionSteps(20);
        workflow.initModule();
        
        // 执行
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("data", Arrays.asList(1, 2, 3));
        
        String result = workflow.execute(inputs);
        log.info("循环工作流执行完成: {}", result);
    }

    /**
     * 数据处理 Agent
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    static class DataProcessorAgent extends Agent {
        
        public DataProcessorAgent() {
            // 添加三个数据处理动作
            addAction(new ValidateAction());
            addAction(new TransformAction());
            addAction(new SummarizeAction());
        }
        
        @Override
        public Message execute(String actionName, List<Message> messages) {
            Action action = getAction(actionName);
            if (action == null) {
                return Message.builder()
                        .content("未找到动作: " + actionName)
                        .messageType(MessageType.ERROR)
                        .build();
            }
            
            try {
                // 准备输入
                Map<String, Object> inputs = new HashMap<>();
                if (!messages.isEmpty()) {
                    Message lastMsg = messages.get(messages.size() - 1);
                    if (lastMsg.getContent() instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> content = (Map<String, Object>) lastMsg.getContent();
                        inputs.putAll(content);
                    }
                }
                
                DataActionInput input = new DataActionInput(inputs);
                
                // 执行动作
                ActionOutput output = action.execute(input);
                
                if (output.isSuccess()) {
                    return Message.builder()
                            .content(output.getData())
                            .messageType(MessageType.RESPONSE)
                            .build();
                } else {
                    return Message.builder()
                            .content("错误: " + output.getError())
                            .messageType(MessageType.ERROR)
                            .build();
                }
            } catch (Exception e) {
                log.error("动作执行失败", e);
                return Message.builder()
                        .content("执行错误: " + e.getMessage())
                        .messageType(MessageType.ERROR)
                        .build();
            }
        }
    }

    /**
     * 数据验证动作
     */
    static class ValidateAction extends Action {
        
        public ValidateAction() {
            setName("validate");
            setDescription("验证数据是否有效");
        }
        
        @Override
        public ActionOutput execute(ActionInput input) {
            Object data = input.toMap().get("data");
            
            if (data == null) {
                return SimpleActionOutput.failure("数据为空");
            }
            
            if (data instanceof List) {
                List<?> list = (List<?>) data;
                int size = list.size();
                
                Map<String, Object> result = new HashMap<>();
                result.put("valid", true);
                result.put("dataSize", size);
                result.put("data", data);
                
                log.info("数据验证通过: {} 个元素", size);
                return SimpleActionOutput.success(result);
            }
            
            return SimpleActionOutput.failure("数据格式不正确");
        }
        
        @Override
        public String[] getInputFields() {
            return new String[]{"data"};
        }
        
        @Override
        public String[] getOutputFields() {
            return new String[]{"valid", "dataSize", "data"};
        }
    }

    /**
     * 数据转换动作
     */
    static class TransformAction extends Action {
        
        public TransformAction() {
            setName("transform");
            setDescription("转换数据格式");
        }
        
        @Override
        public ActionOutput execute(ActionInput input) {
            Object data = input.toMap().get("data");
            
            if (data instanceof List) {
                @SuppressWarnings("unchecked")
                List<Integer> list = (List<Integer>) data;
                
                // 转换: 每个元素乘以2
                List<Integer> transformed = list.stream()
                        .map(x -> x * 2)
                        .toList();
                
                Map<String, Object> result = new HashMap<>();
                result.put("data", transformed);
                result.put("transformedCount", transformed.size());
                
                log.info("数据转换完成: {} -> {}", list, transformed);
                return SimpleActionOutput.success(result);
            }
            
            return SimpleActionOutput.failure("无法转换数据");
        }
        
        @Override
        public String[] getInputFields() {
            return new String[]{"data"};
        }
        
        @Override
        public String[] getOutputFields() {
            return new String[]{"data", "transformedCount"};
        }
    }

    /**
     * 数据汇总动作
     */
    static class SummarizeAction extends Action {
        
        public SummarizeAction() {
            setName("summarize");
            setDescription("汇总数据");
        }
        
        @Override
        public ActionOutput execute(ActionInput input) {
            Object data = input.toMap().get("data");
            
            if (data instanceof List) {
                @SuppressWarnings("unchecked")
                List<Integer> list = (List<Integer>) data;
                
                int sum = list.stream().mapToInt(Integer::intValue).sum();
                double avg = list.stream().mapToInt(Integer::intValue).average().orElse(0.0);
                
                Map<String, Object> result = new HashMap<>();
                result.put("count", list.size());
                result.put("sum", sum);
                result.put("average", avg);
                result.put("data", list);
                
                log.info("数据汇总: 数量={}, 总和={}, 平均值={}", list.size(), sum, avg);
                return SimpleActionOutput.success(result);
            }
            
            return SimpleActionOutput.failure("无法汇总数据");
        }
        
        @Override
        public String[] getInputFields() {
            return new String[]{"data"};
        }
        
        @Override
        public String[] getOutputFields() {
            return new String[]{"count", "sum", "average", "data"};
        }
    }

    /**
     * 数据动作输入
     */
    @Data
    static class DataActionInput extends ActionInput {
        private Map<String, Object> inputs = new HashMap<>();
        
        public DataActionInput() {
            super();
        }
        
        public DataActionInput(Map<String, Object> inputs) {
            super(inputs);
            this.inputs = inputs;
        }
        
        public void setInputs(Map<String, Object> inputs) {
            this.inputs = inputs;
            super.setData(inputs);
        }
        
        public Map<String, Object> getInputs() {
            return inputs;
        }
    }
}
