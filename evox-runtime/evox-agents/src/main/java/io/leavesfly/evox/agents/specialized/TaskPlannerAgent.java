package io.leavesfly.evox.agents.specialized;

import io.leavesfly.evox.agents.base.Agent;
import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.core.message.MessageType;
import io.leavesfly.evox.core.llm.ILLM;
import io.leavesfly.evox.core.llm.LLMConfig;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * TaskPlannerAgent - 任务规划代理
 * 负责将高层目标分解为可执行的子任务序列
 * 
 * @author EvoX Team
 */
@Data
@Slf4j
@EqualsAndHashCode(callSuper = true)
public class TaskPlannerAgent extends Agent {

    /**
     * 默认系统提示词
     */
    private static final String DEFAULT_SYSTEM_PROMPT =
        "你是任务规划专家。你的职责是分析复杂目标并拆解为结构化、可执行的子任务序列。"
        + "对每个任务请给出：\n"
        + "1. 任务描述 - 需要做什么\n"
        + "2. 所需输入 - 需要哪些信息或资源\n"
        + "3. 预期输出 - 应该产出什么\n"
        + "4. 依赖关系 - 必须先完成哪些任务\n\n"
        + "请使用编号列表输出任务，并保证描述清晰。";

    /**
     * 最大子任务数量
     */
    private int maxSubTasks = 10;

    /**
     * 是否包含依赖关系
     */
    private boolean includeDependencies = true;

    /**
     * 构建器构造函数
     */
    @Builder
    public TaskPlannerAgent(
            String name,
            String description,
            String systemPrompt,
            LLMConfig llmConfig,
            ILLM llm,
            Integer maxSubTasks,
            Boolean includeDependencies
    ) {
        this.setName(name != null ? name : "TaskPlanner");
        this.setDescription(description != null ? description : 
            "An agent responsible for planning and decomposing high-level tasks into smaller sub-tasks");
        this.setSystemPrompt(systemPrompt != null ? systemPrompt : DEFAULT_SYSTEM_PROMPT);
        this.setLlmConfig(llmConfig);
        this.setLlm(llm);
        this.maxSubTasks = maxSubTasks != null ? maxSubTasks : 10;
        this.includeDependencies = includeDependencies != null ? includeDependencies : true;
        this.setHuman(false);
    }

    @Override
    public Message execute(String actionName, List<Message> messages) {
        log.debug("TaskPlannerAgent {} executing planning", getName());

        try {
            // 提取目标描述
            String goal = extractGoal(messages);
            
            if (goal == null || goal.trim().isEmpty()) {
                throw new IllegalArgumentException("No goal provided for task planning");
            }

            // 构造规划提示
            String planningPrompt = buildPlanningPrompt(goal);

            // 调用LLM生成计划
            ILLM llmInstance = getLlm();
            if (llmInstance == null) {
                throw new IllegalStateException("LLM not initialized for TaskPlannerAgent");
            }

            // 构造LLM消息
            List<Message> llmMessages = new ArrayList<>();
            llmMessages.add(Message.builder()
                    .messageType(MessageType.SYSTEM)
                    .content(getSystemPrompt())
                    .build());
            llmMessages.add(Message.builder()
                    .messageType(MessageType.INPUT)
                    .content(planningPrompt)
                    .build());

            // 获取LLM响应
            String planResponse = llmInstance.chat(llmMessages);

            // 解析任务列表（用于日志/调试）
            List<TaskItem> tasks = parseTaskPlan(planResponse);
            log.debug("Parsed {} tasks from plan", tasks.size());

            // 构造响应（使用content存储计划文本）
            return Message.builder()
                    .messageType(MessageType.RESPONSE)
                    .content(planResponse)
                    .build();

        } catch (Exception e) {
            log.error("Error in TaskPlannerAgent execution: {}", e.getMessage(), e);
            return Message.builder()
                    .messageType(MessageType.RESPONSE)
                    .content("Error during task planning: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 从消息中提取目标
     */
    private String extractGoal(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return null;
        }

        // 使用最后一条消息的内容作为目标
        Message lastMessage = messages.get(messages.size() - 1);
        Object content = lastMessage.getContent();
        
        return content != null ? String.valueOf(content) : null;
    }

    /**
     * 构造规划提示
     */
    private String buildPlanningPrompt(String goal) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("目标：").append(goal).append("\n\n");
        prompt.append("请为该目标制定详细的任务计划。\n");
        prompt.append("子任务数量上限：").append(maxSubTasks).append("\n");
        
        if (includeDependencies) {
            prompt.append("请包含任务之间的依赖关系。\n");
        }
        
        prompt.append("\n请以结构化格式输出计划（编号列表）。");
        
        return prompt.toString();
    }

    /**
     * 解析任务计划
     */
    private List<TaskItem> parseTaskPlan(String planText) {
        List<TaskItem> tasks = new ArrayList<>();
        
        if (planText == null || planText.trim().isEmpty()) {
            return tasks;
        }

        // 简单解析：按行分割，查找编号的任务
        String[] lines = planText.split("\n");
        int taskIndex = 0;
        
        for (String line : lines) {
            String trimmed = line.trim();
            
            // 匹配 "1. ", "1) ", "Task 1:", etc.
            if (trimmed.matches("^\\d+[.):].*")) {
                taskIndex++;
                // 移除编号
                String description = trimmed.replaceFirst("^\\d+[.):] *", "");
                
                tasks.add(TaskItem.builder()
                        .taskId("task_" + taskIndex)
                        .description(description)
                        .order(taskIndex)
                        .build());
                
                if (tasks.size() >= maxSubTasks) {
                    break;
                }
            }
        }

        return tasks;
    }

    /**
     * 任务项
     */
    @Data
    @Builder
    public static class TaskItem {
        /**
         * 任务ID
         */
        private String taskId;

        /**
         * 任务描述
         */
        private String description;

        /**
         * 执行顺序
         */
        private int order;

        /**
         * 依赖的任务ID列表
         */
        @Builder.Default
        private List<String> dependencies = new ArrayList<>();

        /**
         * 所需输入
         */
        private List<String> requiredInputs;

        /**
         * 预期输出
         */
        private List<String> expectedOutputs;
    }
}
