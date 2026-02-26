package io.leavesfly.evox.workflow.generation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.leavesfly.evox.core.agent.IAgent;
import io.leavesfly.evox.core.llm.ILLM;
import io.leavesfly.evox.workflow.base.Workflow;
import io.leavesfly.evox.workflow.builder.WorkflowBuilder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 工作流生成器
 * 基于任务描述自动生成工作流
 *
 * @author EvoX Team
 */
@Slf4j
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowGenerator {

    /**
     * LLM用于生成工作流
     */
    private ILLM llm;

    /**
     * 可用的Agent列表
     */
    private List<IAgent> availableAgents;

    /**
     * 生成提示词模板
     */
    private String generationPromptTemplate;

    public WorkflowGenerator(ILLM llm) {
        this.llm = llm;
        this.availableAgents = new ArrayList<>();
        this.generationPromptTemplate = """
                根据以下任务描述生成工作流：
                
                任务：{task}
                
                可用的Agent类型：
                {agents}
                
                请返回JSON格式的工作流定义：
                {
                  "name": "工作流名称",
                  "description": "工作流描述",
                  "steps": [
                    {
                      "name": "步骤名称",
                      "agent": "使用的Agent",
                      "description": "步骤描述"
                    }
                  ]
                }
                """;
    }

    /**
     * 根据任务描述生成工作流
     *
     * @param taskDescription 任务描述
     * @return 生成的工作流
     */
    public Workflow generateWorkflow(String taskDescription) {
        return generateWorkflow(taskDescription, null);
    }

    /**
     * 根据任务描述生成工作流
     *
     * @param taskDescription 任务描述
     * @param options 生成选项
     * @return 生成的工作流
     */
    public Workflow generateWorkflow(String taskDescription, GenerationOptions options) {
        if (llm == null) {
            throw new IllegalStateException("LLM未配置");
        }

        try {
            log.info("开始生成工作流,任务: {}", taskDescription);

            // 1. 构建提示词
            String prompt = buildGenerationPrompt(taskDescription);

            // 2. 调用LLM生成工作流定义
            String response = llm.generate(prompt);

            // 3. 解析LLM响应
            WorkflowDefinition definition = parseWorkflowDefinition(response);

            // 4. 构建工作流
            Workflow workflow = buildWorkflowFromDefinition(definition, options);

            log.info("工作流生成成功: {}", definition.getName());
            return workflow;

        } catch (Exception e) {
            log.error("工作流生成失败", e);
            throw new RuntimeException("工作流生成失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构建生成提示词
     */
    private String buildGenerationPrompt(String taskDescription) {
        StringBuilder agentsInfo = new StringBuilder();
        for (int i = 0; i < availableAgents.size(); i++) {
            IAgent agent = availableAgents.get(i);
            agentsInfo.append(String.format("%d. %s: %s\n",
                    i + 1,
                    agent.getName(),
                    agent.getDescription() != null ? agent.getDescription() : "Agent"));
        }

        return generationPromptTemplate
                .replace("{task}", taskDescription)
                .replace("{agents}", agentsInfo.toString());
    }

    /**
     * 解析工作流定义
     * 从LLM返回的JSON字符串中提取工作流定义，支持裸JSON和Markdown代码块两种格式
     */
    private WorkflowDefinition parseWorkflowDefinition(String response) {
        try {
            String json = extractJson(response);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);

            WorkflowDefinition definition = new WorkflowDefinition();
            definition.setName(root.path("name").asText("生成的工作流"));
            definition.setDescription(root.path("description").asText("自动生成"));

            List<WorkflowStep> steps = new ArrayList<>();
            JsonNode stepsNode = root.path("steps");
            if (stepsNode.isArray()) {
                for (JsonNode stepNode : stepsNode) {
                    WorkflowStep step = new WorkflowStep();
                    step.setName(stepNode.path("name").asText("步骤"));
                    step.setAgent(stepNode.path("agent").asText(""));
                    step.setDescription(stepNode.path("description").asText(""));
                    steps.add(step);
                }
            }

            if (steps.isEmpty()) {
                log.warn("LLM返回的工作流定义中没有步骤，将使用默认步骤");
                WorkflowStep defaultStep = new WorkflowStep();
                defaultStep.setName("步骤1");
                defaultStep.setAgent(availableAgents.isEmpty() ? "DefaultAgent" : availableAgents.get(0).getName());
                defaultStep.setDescription("执行任务");
                steps.add(defaultStep);
            }

            definition.setSteps(steps);
            return definition;

        } catch (Exception e) {
            log.error("解析LLM响应失败，使用默认工作流定义。原始响应: {}", response, e);
            WorkflowDefinition fallback = new WorkflowDefinition();
            fallback.setName("生成的工作流");
            fallback.setDescription("自动生成");
            List<WorkflowStep> steps = new ArrayList<>();
            WorkflowStep defaultStep = new WorkflowStep();
            defaultStep.setName("步骤1");
            defaultStep.setAgent(availableAgents.isEmpty() ? "DefaultAgent" : availableAgents.get(0).getName());
            defaultStep.setDescription("执行任务");
            steps.add(defaultStep);
            fallback.setSteps(steps);
            return fallback;
        }
    }

    /**
     * 从LLM响应中提取JSON字符串
     * 兼容裸JSON和Markdown代码块（```json ... ``` 或 ``` ... ```）两种格式
     */
    private String extractJson(String response) {
        if (response == null || response.isBlank()) {
            throw new IllegalArgumentException("LLM响应为空");
        }
        String trimmed = response.trim();
        // 处理 ```json ... ``` 或 ``` ... ``` 格式
        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf('\n');
            int end = trimmed.lastIndexOf("```");
            if (start != -1 && end > start) {
                return trimmed.substring(start + 1, end).trim();
            }
        }
        return trimmed;
    }

    /**
     * 从定义构建工作流
     */
    private Workflow buildWorkflowFromDefinition(WorkflowDefinition definition, GenerationOptions options) {
        WorkflowBuilder builder = WorkflowBuilder.sequential()
                .name(definition.getName())
                .goal(definition.getDescription());

        // 添加步骤
        for (WorkflowStep step : definition.getSteps()) {
            IAgent agent = findAgentByType(step.getAgent());
            if (agent != null) {
                builder.step(step.getName(), agent);
            }
        }

        // 应用选项
        if (options != null) {
            if (options.getMaxSteps() > 0) {
                builder.maxSteps(options.getMaxSteps());
            }
        }

        return builder.build();
    }

    /**
     * 根据类型查找Agent
     */
    private IAgent findAgentByType(String agentType) {
        return availableAgents.stream()
                .filter(agent -> agent.getName().equals(agentType))
                .findFirst()
                .orElse(availableAgents.isEmpty() ? null : availableAgents.get(0));
    }

    /**
     * 工作流定义
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkflowDefinition {
        private String name;
        private String description;
        private List<WorkflowStep> steps;
    }

    /**
     * 工作流步骤
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkflowStep {
        private String name;
        private String agent;
        private String description;
    }

    /**
     * 生成选项
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenerationOptions {
        /**
         * 最大步骤数
         */
        private int maxSteps = 100;

        /**
         * 是否启用并行
         */
        private boolean enableParallel = false;

        /**
         * 额外配置
         */
        private Map<String, Object> additionalConfig;
    }
}
