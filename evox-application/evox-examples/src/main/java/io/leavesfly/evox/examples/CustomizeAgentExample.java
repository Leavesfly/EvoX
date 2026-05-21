package io.leavesfly.evox.examples;

import io.leavesfly.evox.agents.customize.CustomizeAgent;
import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.models.config.LLMFactory;
import io.leavesfly.evox.models.provider.ollama.OllamaLLMConfig;
import io.leavesfly.evox.models.spi.LLMProvider;
import io.leavesfly.evox.tools.file.FileSystemTool;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 自定义智能体示例
 * 展示如何创建和使用CustomizeAgent
 *
 * @author EvoX Team
 */
public class CustomizeAgentExample {
    private static final Logger log = LoggerFactory.getLogger(CustomizeAgentExample.class);

    public static void main(String[] args) {
        CustomizeAgentExample example = new CustomizeAgentExample();
        
        // 示例1: 最简单的智能体
        example.simpleAgent();

        // 示例2: 带输入的智能体
        example.agentWithInputs();

        // 示例3: 带输入和输出的智能体
        example.agentWithInputsAndOutputs();

        // 示例4: 带工具的智能体
         example.agentWithTools();
    }

    /**
     * 示例1: 创建最简单的智能体
     */
    public void simpleAgent() {
        log.info("=== 示例1: 最简单的智能体 ===");

        // 配置LLM
        OllamaLLMConfig config = new OllamaLLMConfig();
        LLMProvider llm = LLMFactory.create(config);

        // 创建智能体
        CustomizeAgent agent = new CustomizeAgent();
        agent.setName("HelloAgent");
        agent.setDescription("A simple agent that prints hello world");
        agent.setPromptTemplate("Print 'hello world'");
        agent.setLlmConfig(config);
        agent.setLlm(llm);
        agent.initModule();

        // 执行智能体
        Map<String, Object> emptyInput = new HashMap<>();
        Message response = agent.call(emptyInput);

        log.info("Response from {}: {}", agent.getName(), response.getContent());
    }

    /**
     * 示例2: 带输入的智能体
     */
    public void agentWithInputs() {
        log.info("=== 示例2: 带输入的智能体 ===");

        OllamaLLMConfig config = new OllamaLLMConfig();
        LLMProvider llm = LLMFactory.create(config);

        // 创建智能体
        CustomizeAgent agent = new CustomizeAgent();
        agent.setName("QuestionAnswerAgent");
        agent.setDescription("An agent that answers questions");
        agent.setPromptTemplate("Answer the following question: {question}");
        agent.setLlmConfig(config);
        agent.setLlm(llm);
        
        // 定义输入参数
        List<CustomizeAgent.InputSpec> inputSpecs = new ArrayList<>();
        inputSpecs.add(new CustomizeAgent.InputSpec("question", "string", "The question to answer"));
        agent.setInputs(inputSpecs);
        
        agent.initModule();

        // 准备输入
        Map<String, Object> inputValues = new HashMap<>();
        inputValues.put("question", "What is a language model?");

        // 执行智能体
        Message response = agent.call(inputValues);

        log.info("Response: {}", response.getContent());
    }

    /**
     * 示例3: 带输入和输出的智能体
     */
    public void agentWithInputsAndOutputs() {
        log.info("=== 示例3: 带输入和输出的智能体 ===");

        OllamaLLMConfig config = new OllamaLLMConfig();
        LLMProvider llm = LLMFactory.create(config);

        // 创建代码编写智能体
        CustomizeAgent codeWriter = new CustomizeAgent();
        codeWriter.setName("CodeWriter");
        codeWriter.setDescription("Writes code based on requirements");
        codeWriter.setPromptTemplate("Write Java code that implements the following requirement: {requirement}");
        codeWriter.setLlmConfig(config);
        codeWriter.setLlm(llm);
        
        // 定义输入
        List<CustomizeAgent.InputSpec> inputSpecs = new ArrayList<>();
        inputSpecs.add(new CustomizeAgent.InputSpec("requirement", "string", "The coding requirement"));
        codeWriter.setInputs(inputSpecs);
        
        // 定义输出
        List<CustomizeAgent.OutputSpec> outputSpecs = new ArrayList<>();
        outputSpecs.add(new CustomizeAgent.OutputSpec("code", "string", "The generated code"));
        codeWriter.setOutputs(outputSpecs);
        
        codeWriter.initModule();

        // 执行
        Map<String, Object> inputValues = new HashMap<>();
        inputValues.put("requirement", "Write a method that returns the sum of two numbers");

        Message response = codeWriter.call(inputValues);

        log.info("Generated Code:\n{}", response.getContent());
    }

    /**
     * 示例4: 带工具的智能体
     */
    public void agentWithTools() {
        log.info("=== 示例4: 带工具的智能体 ===");

        OllamaLLMConfig config = new OllamaLLMConfig();
        LLMProvider llm = LLMFactory.create(config);

        CustomizeAgent agent = CustomizeAgent.builder()
                .name("TaskAgent")
                .description("An agent that can use tools to complete tasks")
                .promptTemplate("Complete the following task: {task}")
                .llmConfig(config)
                .llm(llm)
                .inputs(CustomizeAgent.InputSpec.of("task", "string", "The task to complete"))
                .build();

        Map<String, Object> inputValues = new HashMap<>();
        inputValues.put("task", "Search for 'Java best practices' and save the results to a file");

        Message response = agent.call(inputValues);
        log.info("Response: {}", response.getContent());

        log.info("Tools feature coming soon...");
    }
}
