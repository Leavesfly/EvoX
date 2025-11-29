package io.leavesfly.evox.agents.plan;

import io.leavesfly.evox.actions.base.Action;
import io.leavesfly.evox.actions.base.ActionInput;
import io.leavesfly.evox.actions.base.ActionOutput;
import io.leavesfly.evox.actions.base.SimpleActionOutput;
import io.leavesfly.evox.agents.base.Agent;
import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.core.message.MessageType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PlanAgent 实现任务规划功能
 * 将复杂任务分解为子任务序列
 *
 * @author EvoX Team
 */
@Data
@Slf4j
@EqualsAndHashCode(callSuper = true)
public class PlanAgent extends Agent {

    /**
     * 规划提示模板
     */
    private String planningPrompt = """
            You are a task planning expert. Break down the following goal into a sequence of concrete sub-tasks.
            
            Goal: {goal}
            
            Please provide a structured plan with the following format:
            Task 1: [Description]
            Task 2: [Description]
            ...
            
            Each task should be specific, actionable, and build upon previous tasks.
            """;

    @Override
    public void initModule() {
        super.initModule();
        // 创建规划动作
        PlanningAction action = new PlanningAction();
        action.setName("planning");
        action.setDescription("Task planning and decomposition");
        action.setLlm(getLlm());
        action.setPromptTemplate(planningPrompt);
        addAction(action);
    }

    @Override
    public Message execute(String actionName, List<Message> messages) {
        Action action = getAction(actionName);
        if (action == null) {
            return Message.builder()
                    .messageType(MessageType.ERROR)
                    .content("Action not found: " + actionName)
                    .build();
        }

        try {
            // 提取目标
            String goal = extractGoal(messages);
            
            // 创建输入
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("goal", goal);
            
            ActionInput input = new ActionInput() {
                @Override
                public Map<String, Object> toMap() {
                    return inputData;
                }

                @Override
                public boolean validate() {
                    return goal != null && !goal.isEmpty();
                }
            };

            // 执行动作
            ActionOutput output = action.execute(input);

            // 构建响应消息
            return Message.builder()
                    .messageType(output.isSuccess() ? MessageType.RESPONSE : MessageType.ERROR)
                    .content(output.getData())
                    .build();
        } catch (Exception e) {
            log.error("Failed to execute planning action", e);
            return Message.builder()
                    .messageType(MessageType.ERROR)
                    .content("Execution failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 从消息中提取目标
     */
    private String extractGoal(List<Message> messages) {
        for (Message msg : messages) {
            if (msg.getMessageType() == MessageType.INPUT) {
                Object content = msg.getContent();
                if (content instanceof String) {
                    return (String) content;
                } else if (content instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> contentMap = (Map<String, Object>) content;
                    Object goal = contentMap.get("goal");
                    if (goal != null) {
                        return goal.toString();
                    }
                }
            }
        }
        return "";
    }

    /**
     * PlanningAction 内部类
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    private static class PlanningAction extends Action {
        private String promptTemplate;

        @Override
        public ActionOutput execute(ActionInput input) {
            try {
                String goal = (String) input.toMap().get("goal");
                
                // 构建提示
                String prompt = promptTemplate.replace("{goal}", goal);
                
                // 获取 LLM 响应
                String response = getLlm().generate(prompt);
                log.debug("Planning response: {}", response);
                
                // 解析任务列表
                List<Task> tasks = parseTasks(response);
                
                // 构建结果
                Map<String, Object> result = new HashMap<>();
                result.put("tasks", tasks);
                result.put("total_tasks", tasks.size());
                result.put("plan", response);
                
                return SimpleActionOutput.success(result);
            } catch (Exception e) {
                log.error("PlanningAction execution failed", e);
                return SimpleActionOutput.failure("Execution failed: " + e.getMessage());
            }
        }

        /**
         * 解析任务列表
         */
        private List<Task> parseTasks(String response) {
            List<Task> tasks = new ArrayList<>();
            String[] lines = response.split("\n");
            
            int taskId = 1;
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                
                // 匹配 "Task X: Description" 或 "X. Description" 格式
                if (line.matches("^Task\\s+\\d+:.*") || line.matches("^\\d+\\..*")) {
                    String description;
                    if (line.contains(":")) {
                        description = line.substring(line.indexOf(":") + 1).trim();
                    } else if (line.contains(".")) {
                        description = line.substring(line.indexOf(".") + 1).trim();
                    } else {
                        description = line;
                    }
                    
                    Task task = new Task();
                    task.setId(taskId++);
                    task.setDescription(description);
                    task.setStatus("pending");
                    tasks.add(task);
                }
            }
            
            return tasks;
        }

        @Override
        public String[] getInputFields() {
            return new String[]{"goal"};
        }

        @Override
        public String[] getOutputFields() {
            return new String[]{"tasks", "total_tasks", "plan"};
        }
    }

    /**
     * 任务类
     */
    @Data
    public static class Task {
        private int id;
        private String description;
        private String status;
        private List<String> dependencies;
        private Map<String, Object> metadata;

        public Task() {
            this.dependencies = new ArrayList<>();
            this.metadata = new HashMap<>();
        }
    }
}
