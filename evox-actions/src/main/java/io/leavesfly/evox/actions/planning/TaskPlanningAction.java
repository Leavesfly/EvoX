package io.leavesfly.evox.actions.planning;

import io.leavesfly.evox.actions.base.Action;
import io.leavesfly.evox.actions.base.ActionInput;
import io.leavesfly.evox.actions.base.ActionOutput;
import io.leavesfly.evox.models.base.BaseLLM;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TaskPlanning动作 - 任务规划
 * 将高层目标分解为可执行的子任务序列
 * 
 * @author EvoX Team
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class TaskPlanningAction extends Action {

    /**
     * 提示词模板
     */
    private static final String DEFAULT_PROMPT = 
        "Your Task: Given a user's goal, break it down into clear, manageable sub-tasks.\n\n" +
        "### Instructions:\n" +
        "1. **Understand the Goal**: Identify the core objective.\n" +
        "2. **Review the History**: Assess any previously generated task plan.\n" +
        "3. **Consider Suggestions**: Use suggestions to improve the workflow.\n" +
        "4. **Define Sub-Tasks**: Break the task into logical, actionable sub-tasks.\n\n" +
        "### Principles:\n" +
        "- **Simplicity**: Each sub-task has a specific, clear objective\n" +
        "- **Modularity**: Sub-tasks are self-contained and reusable\n" +
        "- **Consistency**: Sub-tasks logically support the goal\n" +
        "- **Avoid Redundancy**: No overlapping or unnecessary sub-tasks\n\n" +
        "### Sub-Task Format:\n" +
        "```json\n" +
        "{\n" +
        "  \"name\": \"subtask_name\",\n" +
        "  \"description\": \"Clear explanation of the sub-task goal\",\n" +
        "  \"reason\": \"Why this sub-task is necessary\",\n" +
        "  \"inputs\": [{\"name\": \"input_name\", \"type\": \"string\", \"required\": true, \"description\": \"...\"}],\n" +
        "  \"outputs\": [{\"name\": \"output_name\", \"type\": \"string\", \"required\": true, \"description\": \"...\"}]\n" +
        "}\n" +
        "```\n\n" +
        "### Output Format:\n" +
        "## Thought\n" +
        "Your reasoning for the task structure.\n\n" +
        "## Goal\n" +
        "Restate the user's goal.\n\n" +
        "## Plan\n" +
        "```json\n" +
        "{\n" +
        "  \"sub_tasks\": [\n" +
        "    {\"name\": \"...\", \"description\": \"...\", ...},\n" +
        "    ...\n" +
        "  ]\n" +
        "}\n" +
        "```\n\n" +
        "---\n\n" +
        "### History: {history}\n" +
        "### Suggestions: {suggestion}\n" +
        "### User's Goal: {goal}\n\n" +
        "Output:";

    private String prompt;

    public TaskPlanningAction() {
        this.setName("TaskPlanning");
        this.setDescription("Analyze a goal and break it into manageable sub-tasks in optimal order");
        this.prompt = DEFAULT_PROMPT;
    }

    public TaskPlanningAction(String customPrompt) {
        this();
        this.prompt = customPrompt;
    }

    @Override
    public ActionOutput execute(ActionInput input) {
        if (!(input instanceof TaskPlanningInput)) {
            throw new IllegalArgumentException("Input must be TaskPlanningInput");
        }

        TaskPlanningInput planInput = (TaskPlanningInput) input;
        
        // 验证必需字段
        if (planInput.getGoal() == null || planInput.getGoal().trim().isEmpty()) {
            throw new IllegalArgumentException("Goal is required for task planning");
        }

        try {
            // 填充提示词参数
            String filledPrompt = prompt
                    .replace("{goal}", planInput.getGoal())
                    .replace("{history}", planInput.getHistory() != null ? planInput.getHistory() : "No previous plan")
                    .replace("{suggestion}", planInput.getSuggestion() != null ? planInput.getSuggestion() : "No suggestions");

            // 调用LLM生成计划
            if (getLlm() == null) {
                throw new IllegalStateException("LLM not set for TaskPlanningAction");
            }

            String response = getLlm().generate(filledPrompt);

            // 解析响应
            TaskPlanningOutput output = parseResponse(response, planInput.getGoal());
            return output;

        } catch (Exception e) {
            log.error("Error executing TaskPlanningAction", e);
            TaskPlanningOutput errorOutput = new TaskPlanningOutput();
            errorOutput.setSubTasks(new ArrayList<>());
            errorOutput.setSuccess(false);
            errorOutput.setError(e.getMessage());
            return errorOutput;
        }
    }

    /**
     * 解析LLM响应
     */
    private TaskPlanningOutput parseResponse(String response, String originalGoal) {
        TaskPlanningOutput output = new TaskPlanningOutput();
        List<SubTask> subTasks = new ArrayList<>();

        try {
            // 提取JSON部分
            String jsonPart = extractJsonFromResponse(response);
            
            if (jsonPart != null) {
                // 简化处理：手动解析或使用JSON库
                // 这里简化为提取任务描述
                subTasks = parseSubTasksFromJson(jsonPart);
            } else {
                // 回退：从文本中提取任务
                subTasks = parseSubTasksFromText(response);
            }

            output.setSubTasks(subTasks);
            output.setSuccess(true);
            output.setRawResponse(response);

        } catch (Exception e) {
            log.warn("Failed to parse task planning response, using fallback", e);
            // 创建单个任务作为回退
            SubTask fallbackTask = SubTask.builder()
                    .name("complete_goal")
                    .description(originalGoal)
                    .reason("Single task to achieve the goal")
                    .build();
            subTasks.add(fallbackTask);
            output.setSubTasks(subTasks);
            output.setSuccess(true);
        }

        return output;
    }

    /**
     * 从响应中提取JSON
     */
    private String extractJsonFromResponse(String response) {
        int startIdx = response.indexOf("```json");
        if (startIdx == -1) {
            startIdx = response.indexOf("{");
        } else {
            startIdx += 7; // 跳过 ```json
        }

        int endIdx = response.lastIndexOf("```");
        if (endIdx == -1 || endIdx <= startIdx) {
            endIdx = response.lastIndexOf("}");
            if (endIdx != -1) {
                endIdx += 1;
            }
        }

        if (startIdx != -1 && endIdx != -1 && startIdx < endIdx) {
            return response.substring(startIdx, endIdx).trim();
        }

        return null;
    }

    /**
     * 从JSON字符串解析子任务
     */
    private List<SubTask> parseSubTasksFromJson(String jsonStr) {
        List<SubTask> tasks = new ArrayList<>();
        
        // 简化实现：提取name和description字段
        // 实际应用中应使用Jackson或Gson
        String[] lines = jsonStr.split("\n");
        SubTask.SubTaskBuilder currentTask = null;
        
        for (String line : lines) {
            String trimmed = line.trim();
            
            if (trimmed.contains("\"name\"")) {
                if (currentTask != null) {
                    tasks.add(currentTask.build());
                }
                currentTask = SubTask.builder();
                String name = extractValue(trimmed);
                currentTask.name(name);
            } else if (trimmed.contains("\"description\"") && currentTask != null) {
                String desc = extractValue(trimmed);
                currentTask.description(desc);
            } else if (trimmed.contains("\"reason\"") && currentTask != null) {
                String reason = extractValue(trimmed);
                currentTask.reason(reason);
            }
        }
        
        if (currentTask != null) {
            tasks.add(currentTask.build());
        }
        
        return tasks;
    }

    /**
     * 从文本中解析子任务
     */
    private List<SubTask> parseSubTasksFromText(String text) {
        List<SubTask> tasks = new ArrayList<>();
        String[] lines = text.split("\n");
        
        for (String line : lines) {
            String trimmed = line.trim();
            // 匹配 "1. ", "1) ", "Task 1:" 等格式
            if (trimmed.matches("^\\d+[.):] .*")) {
                String description = trimmed.replaceFirst("^\\d+[.):] *", "");
                SubTask task = SubTask.builder()
                        .name("task_" + (tasks.size() + 1))
                        .description(description)
                        .build();
                tasks.add(task);
            }
        }
        
        return tasks;
    }

    /**
     * 提取JSON值
     */
    private String extractValue(String line) {
        int colonIdx = line.indexOf(":");
        if (colonIdx == -1) {
            return "";
        }
        
        String value = line.substring(colonIdx + 1).trim();
        // 移除引号和逗号
        value = value.replaceAll("^\"|\"$|\",$", "").trim();
        return value;
    }

    @Override
    public String[] getInputFields() {
        return new String[]{"goal", "history", "suggestion"};
    }

    @Override
    public String[] getOutputFields() {
        return new String[]{"sub_tasks"};
    }

    /**
     * 任务规划输入
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskPlanningInput extends ActionInput {
        /**
         * 用户目标描述
         */
        private String goal;

        /**
         * 历史计划（可选）
         */
        private String history;

        /**
         * 优化建议（可选）
         */
        private String suggestion;
    }

    /**
     * 任务规划输出
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    public static class TaskPlanningOutput extends ActionOutput {
        /**
         * 子任务列表
         */
        private List<SubTask> subTasks;

        /**
         * 原始响应
         */
        private String rawResponse;
    }

    /**
     * 子任务定义
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubTask {
        /**
         * 任务名称
         */
        private String name;

        /**
         * 任务描述
         */
        private String description;

        /**
         * 任务理由
         */
        private String reason;

        /**
         * 输入参数
         */
        private List<Map<String, Object>> inputs;

        /**
         * 输出参数
         */
        private List<Map<String, Object>> outputs;
    }
}
