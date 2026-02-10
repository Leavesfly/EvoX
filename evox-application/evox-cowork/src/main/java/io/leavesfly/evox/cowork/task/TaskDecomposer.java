package io.leavesfly.evox.cowork.task;

import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.models.base.LLMProvider;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class TaskDecomposer {
    private final LLMProvider llm;

    public TaskDecomposer(LLMProvider llm) {
        this.llm = llm;
    }

    // 使用 LLM 分解复杂任务
    public List<CoworkTask> decompose(String taskDescription) {
        String systemPrompt = "You are a task planning assistant. Break down the following task into independent, actionable subtasks. For each subtask, provide a description and a detailed prompt. Format your response as XML: <subtasks><subtask><description>...</description><prompt>...</prompt></subtask>...</subtasks>. If the task is simple and does not need decomposition, return a single subtask.";
        
        List<Message> messages = List.of(
            Message.systemMessage(systemPrompt),
            Message.inputMessage(taskDescription)
        );

        try {
            String response = llm.chat(messages);
            return parseSubtasks(response, taskDescription);
        } catch (Exception e) {
            log.warn("Failed to decompose task: {}, using original task", e.getMessage());
            return List.of(CoworkTask.of(taskDescription, taskDescription));
        }
    }

    // 解析 LLM 返回的任务分解结果
    private List<CoworkTask> parseSubtasks(String response, String originalDescription) {
        List<CoworkTask> subtasks = new ArrayList<>();
        Pattern pattern = Pattern.compile("<subtask>\\s*<description>(.*?)</description>\\s*<prompt>(.*?)</prompt>\\s*</subtask>", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(response);

        while (matcher.find()) {
            String description = matcher.group(1).trim();
            String prompt = matcher.group(2).trim();
            subtasks.add(CoworkTask.of(description, prompt));
        }

        if (subtasks.isEmpty()) {
            log.info("No subtasks parsed from response, using original task");
            return List.of(CoworkTask.of(originalDescription, originalDescription));
        }

        log.info("Decomposed task into {} subtasks", subtasks.size());
        return subtasks;
    }
}