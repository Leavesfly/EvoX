package io.leavesfly.evox.agents;

import io.leavesfly.evox.agents.customize.CustomizeAgent;
import io.leavesfly.evox.agents.plan.PlanAgent;
import io.leavesfly.evox.agents.react.ReActAgent;

import io.leavesfly.evox.models.provider.openai.OpenAILLMConfig;
import io.leavesfly.evox.models.provider.openai.OpenAILLM;
import io.leavesfly.evox.tools.base.BaseTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 专用智能体测试
 *
 * @author EvoX Team
 */
class SpecializedAgentsTest {

    private OpenAILLMConfig llmConfig;

    @BeforeEach
    void setUp() {
        llmConfig = OpenAILLMConfig.builder()
                .apiKey("test-key")
                .model("gpt-4o-mini")
                .temperature(0.7f)
                .maxTokens(1000)
                .build();
    }

    @Test
    void testCustomizeAgentConfiguration() {
        // 创建 CustomizeAgent
        CustomizeAgent agent = new CustomizeAgent();
        agent.setName("SummaryAgent");
        agent.setDescription("Summarize text content");
        agent.setPromptTemplate("Please summarize the following text:\n\n{text}\n\nProvide a concise summary.");
        
        // 添加输入规格
        agent.getInputs().add(new CustomizeAgent.InputSpec("text", "string", "The text to summarize"));
        
        // 添加输出规格
        agent.getOutputs().add(new CustomizeAgent.OutputSpec("summary", "string", "The summary"));
        
        // 验证配置
        assertNotNull(agent.getName());
        assertEquals("SummaryAgent", agent.getName());
        assertEquals(1, agent.getInputs().size());
        assertEquals(1, agent.getOutputs().size());
        assertEquals("text", agent.getInputs().get(0).getName());
        assertEquals("summary", agent.getOutputs().get(0).getName());
    }

    @Test
    void testCustomizeAgentParseMode() {
        CustomizeAgent agent = new CustomizeAgent();
        agent.setParseMode(CustomizeAgent.ParseMode.JSON);
        assertEquals(CustomizeAgent.ParseMode.JSON, agent.getParseMode());
        
        agent.setParseMode(CustomizeAgent.ParseMode.STRING);
        assertEquals(CustomizeAgent.ParseMode.STRING, agent.getParseMode());
    }

    @Test
    void testReActAgentConfiguration() {
        ReActAgent agent = new ReActAgent();
        agent.setName("ReActSolver");
        agent.setDescription("Solve problems using ReAct approach");
        agent.setMaxIterations(5);
        
        // 添加模拟工具
        BaseTool searchTool = new BaseTool() {
            @Override
            public String getName() {
                return "search";
            }

            @Override
            public String getDescription() {
                return "Search for information";
            }

            @Override
            public ToolResult execute(Map<String, Object> parameters) {
                return ToolResult.success("Search result for: " + parameters.get("input"));
            }
        };
        
        agent.getTools().add(searchTool);
        
        // 验证配置
        assertEquals(5, agent.getMaxIterations());
        assertEquals(1, agent.getTools().size());
        assertEquals("search", agent.getTools().get(0).getName());
    }

    @Test
    void testPlanAgentConfiguration() {
        PlanAgent agent = new PlanAgent();
        agent.setName("TaskPlanner");
        agent.setDescription("Plan and decompose tasks");
        
        // 验证配置
        assertNotNull(agent.getName());
        assertEquals("TaskPlanner", agent.getName());
        assertNotNull(agent.getPlanningPrompt());
        assertTrue(agent.getPlanningPrompt().contains("{goal}"));
    }

    @Test
    void testPlanAgentTaskStructure() {
        PlanAgent.Task task = new PlanAgent.Task();
        task.setId(1);
        task.setDescription("Analyze requirements");
        task.setStatus("pending");
        
        assertNotNull(task);
        assertEquals(1, task.getId());
        assertEquals("Analyze requirements", task.getDescription());
        assertEquals("pending", task.getStatus());
        assertNotNull(task.getDependencies());
        assertNotNull(task.getMetadata());
    }

    @Test
    void testInputOutputSpecValidation() {
        CustomizeAgent.InputSpec input = new CustomizeAgent.InputSpec("query", "string", "Search query");
        assertEquals("query", input.getName());
        assertEquals("string", input.getType());
        assertTrue(input.isRequired());
        
        CustomizeAgent.OutputSpec output = new CustomizeAgent.OutputSpec("result", "string", "Search result");
        assertEquals("result", output.getName());
        assertEquals("string", output.getType());
        assertTrue(output.isRequired());
    }

    @Test
    @Disabled("Requires valid API key")
    void testCustomizeAgentWithLLM() {
        CustomizeAgent agent = new CustomizeAgent();
        agent.setName("TranslatorAgent");
        agent.setDescription("Translate text");
        agent.setPromptTemplate("Translate the following text to {target_language}:\n\n{text}");
        agent.setLlm(new OpenAILLM(llmConfig));
        
        agent.getInputs().add(new CustomizeAgent.InputSpec("text", "string", "Text to translate"));
        agent.getInputs().add(new CustomizeAgent.InputSpec("target_language", "string", "Target language"));
        agent.getOutputs().add(new CustomizeAgent.OutputSpec("translation", "string", "Translated text"));
        
        agent.initModule();
        
        // 执行需要有效的 API key
        Map<String, Object> input = new HashMap<>();
        input.put("text", "Hello, world!");
        input.put("target_language", "French");
        
        // Message result = agent.call(input);
        // assertNotNull(result);
    }

    @Test
    void testAgentTypeIdentification() {
        CustomizeAgent customizeAgent = new CustomizeAgent();
        customizeAgent.setName("CustomAgent");
        assertTrue(customizeAgent instanceof CustomizeAgent);

        ReActAgent reactAgent = new ReActAgent();
        reactAgent.setName("ReActAgent");
        assertTrue(reactAgent instanceof ReActAgent);

        PlanAgent planAgent = new PlanAgent();
        planAgent.setName("PlanAgent");
        assertTrue(planAgent instanceof PlanAgent);
    }
}
