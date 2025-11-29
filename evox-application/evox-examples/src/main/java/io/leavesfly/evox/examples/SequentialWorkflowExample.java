package io.leavesfly.evox.examples;

import io.leavesfly.evox.agents.customize.CustomizeAgent;
import io.leavesfly.evox.agents.manager.AgentManager;
import io.leavesfly.evox.models.config.OpenAILLMConfig;
import io.leavesfly.evox.models.openai.OpenAILLM;
import io.leavesfly.evox.workflow.base.Workflow;
import io.leavesfly.evox.workflow.graph.WorkflowGraph;
import io.leavesfly.evox.workflow.base.WorkflowNode;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 顺序工作流示例
 * 展示如何创建和执行顺序工作流
 *
 * @author EvoX Team
 */
public class SequentialWorkflowExample {
    private static final Logger log = LoggerFactory.getLogger(SequentialWorkflowExample.class);

    public static void main(String[] args) {
        // 示例1: 简单的两步工作流
        simpleTwoStepWorkflow();
    }

    /**
     * 示例1: 创建简单的两步工作流
     */
    public static void simpleTwoStepWorkflow() {
        log.info("=== 示例1: 简单的两步工作流 ===");

        // 配置LLM
        OpenAILLMConfig config = OpenAILLMConfig.builder()
                .model("gpt-4o-mini")
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .temperature(0.7f)
                .build();

        OpenAILLM llm = new OpenAILLM(config);

        // 创建AgentManager
        AgentManager agentManager = new AgentManager();

        // 定义输入输出
        List<CustomizeAgent.InputSpec> analyzeInputs = new ArrayList<>();
        analyzeInputs.add(new CustomizeAgent.InputSpec("question", "string", "The question to analyze"));

        List<CustomizeAgent.OutputSpec> analyzeOutputs = new ArrayList<>();
        analyzeOutputs.add(new CustomizeAgent.OutputSpec("analysis", "string", "Analysis result"));

        // 创建第一个智能体 - 分析
        CustomizeAgent analyzeAgent = new CustomizeAgent();
        analyzeAgent.setName("AnalyzeAgent");
        analyzeAgent.setDescription("Analyze the question");
        analyzeAgent.setPromptTemplate("Analyze the following question: {question}");
        // 设置LLM配置的代码需要根据实际API调整
        // analyzeAgent.setLlmConfig(config);
        analyzeAgent.setInputs(analyzeInputs);
        analyzeAgent.setOutputs(analyzeOutputs);

        // 定义第二个智能体的输入输出
        List<CustomizeAgent.InputSpec> answerInputs = new ArrayList<>();
        answerInputs.add(new CustomizeAgent.InputSpec("question", "string", "The question to answer"));
        answerInputs.add(new CustomizeAgent.InputSpec("analysis", "string", "Analysis from previous step"));

        List<CustomizeAgent.OutputSpec> answerOutputs = new ArrayList<>();
        answerOutputs.add(new CustomizeAgent.OutputSpec("answer", "string", "The final answer"));

        // 创建第二个智能体 - 回答
        CustomizeAgent answerAgent = new CustomizeAgent();
        answerAgent.setName("AnswerAgent");
        answerAgent.setDescription("Generate answer based on analysis");
        answerAgent.setPromptTemplate("Based on this analysis: {analysis}\n\nGenerate a comprehensive answer to: {question}");
        // 设置LLM配置的代码需要根据实际API调整
        // answerAgent.setLlmConfig(config);
        answerAgent.setInputs(answerInputs);
        answerAgent.setOutputs(answerOutputs);

        // 添加智能体到管理器
        agentManager.addAgent(analyzeAgent);
        agentManager.addAgent(answerAgent);

        // 创建工作流节点
        WorkflowNode node1 = new WorkflowNode();
        node1.setNodeId("analyze");
        node1.setName("Analyze");
        node1.setDescription("Analyze the question");
        node1.initModule();

        WorkflowNode node2 = new WorkflowNode();
        node2.setNodeId("answer");
        node2.setName("Answer");
        node2.setDescription("Generate answer");
        node2.initModule();

        // 创建工作流图
        WorkflowGraph graph = new WorkflowGraph();
        graph.setGoal("Answer questions with analysis");
        graph.addNode(node1);
        graph.addNode(node2);
        graph.addEdge("analyze", "answer");

        // 创建工作流
        Workflow workflow = new Workflow();
        workflow.setGraph(graph);
        workflow.setAgentManager(agentManager);
        workflow.setLlm(llm);
        workflow.initModule();

        // 执行工作流
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("question", "What is the capital of France?");

        try {
            String result = workflow.execute(inputs);
            log.info("Workflow result: {}", result);
        } catch (Exception e) {
            log.error("Workflow execution failed", e);
        }
    }
}