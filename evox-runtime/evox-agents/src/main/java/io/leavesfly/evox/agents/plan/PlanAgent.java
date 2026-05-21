package io.leavesfly.evox.agents.plan;

import io.leavesfly.evox.agents.base.Agent;
import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.core.message.MessageType;
import io.leavesfly.evox.exception.AgentException;
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
            你是任务规划专家。请将以下目标拆解为一系列具体的子任务。
            
            目标：{goal}
            
            请按以下格式输出结构化计划：
            Task 1: [描述]
            Task 2: [描述]
            ...
            
            每个任务应具体、可执行，并与前序任务形成递进关系。
            """;

    @Override
    public void initModule() {
        super.initModule();
        // Planning logic is now inline in execute() method
    }

    @Override
    public Message execute(List<Message> messages) {
        try {
            // 提取目标
            String goal = extractGoal(messages);
            
            if (goal == null || goal.isEmpty()) {
                return Message.builder()
                        .messageType(MessageType.ERROR)
                        .content("No goal found in messages")
                        .build();
            }
            
            // 构建提示
            String prompt = planningPrompt.replace("{goal}", goal);
            
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
            
            // 返回响应消息
            return Message.builder()
                    .messageType(MessageType.RESPONSE)
                    .content(result)
                    .build();
        } catch (Exception e) {
            log.error("Failed to execute planning", e);
            throw AgentException.executionError(getName(), e.getMessage(), e);
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
