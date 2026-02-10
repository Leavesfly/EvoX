package io.leavesfly.evox.agents.skill.builtin;

import io.leavesfly.evox.agents.skill.BaseSkill;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class ReminderSkill extends BaseSkill {

    public ReminderSkill() {
        setName("reminder");
        setDescription("Create, list, and manage reminders and to-do items. "
                + "Supports setting reminders with specific times, recurring schedules, and priority levels.");

        setSystemPrompt(buildReminderSystemPrompt());

        setRequiredTools(List.of("file_system"));

        Map<String, Map<String, String>> inputParams = new LinkedHashMap<>();

        Map<String, String> operationParam = new HashMap<>();
        operationParam.put("type", "string");
        operationParam.put("description", "Operation: 'create', 'list', 'complete', 'delete', or 'search'");
        inputParams.put("operation", operationParam);

        Map<String, String> titleParam = new HashMap<>();
        titleParam.put("type", "string");
        titleParam.put("description", "Reminder title or description");
        inputParams.put("title", titleParam);

        Map<String, String> timeParam = new HashMap<>();
        timeParam.put("type", "string");
        timeParam.put("description", "When to remind (e.g., '2024-12-25 09:00', 'tomorrow 3pm', 'in 2 hours')");
        inputParams.put("time", timeParam);

        Map<String, String> priorityParam = new HashMap<>();
        priorityParam.put("type", "string");
        priorityParam.put("description", "Priority level: 'high', 'medium', 'low' (default: 'medium')");
        inputParams.put("priority", priorityParam);

        Map<String, String> recurringParam = new HashMap<>();
        recurringParam.put("type", "string");
        recurringParam.put("description", "Recurring pattern: 'daily', 'weekly', 'monthly', or cron expression (optional)");
        inputParams.put("recurring", recurringParam);

        setInputParameters(inputParams);
        setRequiredInputs(List.of("operation"));
    }

    @Override
    public SkillResult execute(SkillContext context) {
        validateInputs(context.getParameters());

        String operation = context.getParameters().getOrDefault("operation", "list").toString();
        String title = context.getParameters().getOrDefault("title", "").toString();
        String time = context.getParameters().getOrDefault("time", "").toString();
        String priority = context.getParameters().getOrDefault("priority", "medium").toString();
        String recurring = context.getParameters().getOrDefault("recurring", "").toString();

        String prompt = buildPrompt(context.getInput(), context.getAdditionalContext());

        StringBuilder reminderPrompt = new StringBuilder(prompt);
        reminderPrompt.append("\n\nReminder Operation: ").append(operation);

        switch (operation) {
            case "create" -> {
                reminderPrompt.append("\nTitle: ").append(title);
                if (!time.isEmpty()) reminderPrompt.append("\nTime: ").append(time);
                reminderPrompt.append("\nPriority: ").append(priority);
                if (!recurring.isEmpty()) reminderPrompt.append("\nRecurring: ").append(recurring);
                reminderPrompt.append("\n\nCreate this reminder and save it to a local JSON file (reminders.json).");
                reminderPrompt.append("\nGenerate a unique ID for the reminder and confirm creation.");
            }
            case "list" -> {
                reminderPrompt.append("\n\nList all active reminders from the local reminders.json file.");
                reminderPrompt.append("\nSort by priority (high first) and then by time.");
                reminderPrompt.append("\nShow status, title, time, and priority for each reminder.");
            }
            case "complete" -> {
                reminderPrompt.append("\nReminder to complete: ").append(title);
                reminderPrompt.append("\n\nMark this reminder as completed in reminders.json.");
            }
            case "delete" -> {
                reminderPrompt.append("\nReminder to delete: ").append(title);
                reminderPrompt.append("\n\nDelete this reminder from reminders.json.");
            }
            case "search" -> {
                reminderPrompt.append("\nSearch query: ").append(title);
                reminderPrompt.append("\n\nSearch reminders matching the query in reminders.json.");
            }
            default -> reminderPrompt.append("\n\nUnknown operation. Please use: create, list, complete, delete, or search.");
        }

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("skillName", "reminder");
        metadata.put("operation", operation);
        if (!title.isEmpty()) metadata.put("title", title);

        return SkillResult.success(reminderPrompt.toString(), metadata);
    }

    private String buildReminderSystemPrompt() {
        return """
                You are a personal reminder and task management assistant.
                
                When managing reminders:
                1. Store reminders in a local JSON file (reminders.json) in the working directory
                2. Each reminder should have: id, title, description, time, priority, recurring, status, createdAt
                3. Support natural language time parsing (e.g., "tomorrow 3pm", "next Monday")
                4. Validate time inputs and convert to ISO 8601 format
                5. When listing, show reminders in a clear table format
                6. Highlight overdue reminders
                7. For recurring reminders, calculate the next occurrence
                
                Reminder JSON format:
                {
                  "reminders": [
                    {
                      "id": "uuid",
                      "title": "string",
                      "description": "string",
                      "time": "ISO 8601",
                      "priority": "high|medium|low",
                      "recurring": "daily|weekly|monthly|cron",
                      "status": "active|completed|overdue",
                      "createdAt": "ISO 8601"
                    }
                  ]
                }""";
    }
}
