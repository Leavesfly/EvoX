package io.leavesfly.evox.agents.skill.builtin;

import io.leavesfly.evox.agents.skill.BaseSkill;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class CalendarSkill extends BaseSkill {

    public CalendarSkill() {
        setName("calendar");
        setDescription("Manage calendar events and schedules. "
                + "Create, list, update, and delete events. Supports recurring events and conflict detection.");

        setSystemPrompt(buildCalendarSystemPrompt());

        setRequiredTools(List.of("file_system"));

        Map<String, Map<String, String>> inputParams = new LinkedHashMap<>();

        Map<String, String> operationParam = new HashMap<>();
        operationParam.put("type", "string");
        operationParam.put("description", "Operation: 'create', 'list', 'update', 'delete', 'today', 'week'");
        inputParams.put("operation", operationParam);

        Map<String, String> titleParam = new HashMap<>();
        titleParam.put("type", "string");
        titleParam.put("description", "Event title");
        inputParams.put("title", titleParam);

        Map<String, String> startTimeParam = new HashMap<>();
        startTimeParam.put("type", "string");
        startTimeParam.put("description", "Event start time (e.g., '2024-12-25 09:00', 'tomorrow 2pm')");
        inputParams.put("startTime", startTimeParam);

        Map<String, String> endTimeParam = new HashMap<>();
        endTimeParam.put("type", "string");
        endTimeParam.put("description", "Event end time (e.g., '2024-12-25 10:00', 'tomorrow 3pm')");
        inputParams.put("endTime", endTimeParam);

        Map<String, String> locationParam = new HashMap<>();
        locationParam.put("type", "string");
        locationParam.put("description", "Event location (optional)");
        inputParams.put("location", locationParam);

        Map<String, String> recurringParam = new HashMap<>();
        recurringParam.put("type", "string");
        recurringParam.put("description", "Recurring pattern: 'daily', 'weekly', 'monthly', 'yearly' (optional)");
        inputParams.put("recurring", recurringParam);

        setInputParameters(inputParams);
        setRequiredInputs(List.of("operation"));
    }

    @Override
    public SkillResult execute(SkillContext context) {
        validateInputs(context.getParameters());

        String operation = context.getParameters().getOrDefault("operation", "today").toString();
        String title = context.getParameters().getOrDefault("title", "").toString();
        String startTime = context.getParameters().getOrDefault("startTime", "").toString();
        String endTime = context.getParameters().getOrDefault("endTime", "").toString();
        String location = context.getParameters().getOrDefault("location", "").toString();
        String recurring = context.getParameters().getOrDefault("recurring", "").toString();

        String prompt = buildPrompt(context.getInput(), context.getAdditionalContext());

        StringBuilder calendarPrompt = new StringBuilder(prompt);
        calendarPrompt.append("\n\nCalendar Operation: ").append(operation);

        switch (operation) {
            case "create" -> {
                calendarPrompt.append("\nTitle: ").append(title);
                calendarPrompt.append("\nStart: ").append(startTime);
                if (!endTime.isEmpty()) calendarPrompt.append("\nEnd: ").append(endTime);
                if (!location.isEmpty()) calendarPrompt.append("\nLocation: ").append(location);
                if (!recurring.isEmpty()) calendarPrompt.append("\nRecurring: ").append(recurring);
                calendarPrompt.append("\n\nCreate this event in the local calendar file (calendar.json).");
                calendarPrompt.append("\nCheck for time conflicts with existing events before creating.");
                calendarPrompt.append("\nGenerate a unique ID and confirm creation.");
            }
            case "list" -> {
                calendarPrompt.append("\n\nList all upcoming events from calendar.json.");
                calendarPrompt.append("\nSort by start time and show title, time, location, and recurring status.");
            }
            case "today" -> {
                calendarPrompt.append("\n\nShow today's schedule from calendar.json.");
                calendarPrompt.append("\nInclude time blocks, gaps, and any conflicts.");
                calendarPrompt.append("\nPresent as a timeline view.");
            }
            case "week" -> {
                calendarPrompt.append("\n\nShow this week's schedule from calendar.json.");
                calendarPrompt.append("\nGroup events by day and show a weekly overview.");
            }
            case "update" -> {
                calendarPrompt.append("\nEvent to update: ").append(title);
                calendarPrompt.append("\nUpdate details from input: ").append(context.getInput());
                calendarPrompt.append("\n\nUpdate the event in calendar.json.");
            }
            case "delete" -> {
                calendarPrompt.append("\nEvent to delete: ").append(title);
                calendarPrompt.append("\n\nDelete this event from calendar.json.");
            }
            default -> calendarPrompt.append("\n\nUnknown operation. Available: create, list, today, week, update, delete.");
        }

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("skillName", "calendar");
        metadata.put("operation", operation);
        if (!title.isEmpty()) metadata.put("title", title);

        return SkillResult.success(calendarPrompt.toString(), metadata);
    }

    private String buildCalendarSystemPrompt() {
        return """
                You are a calendar and schedule management assistant.
                
                When managing calendar events:
                1. Store events in a local JSON file (calendar.json) in the working directory
                2. Each event should have: id, title, startTime, endTime, location, description, recurring, createdAt
                3. Support natural language time parsing
                4. Detect and warn about time conflicts
                5. For recurring events, generate occurrences for the next 30 days
                6. Present schedules in a clear timeline format
                7. Use ISO 8601 format for all timestamps
                
                Calendar JSON format:
                {
                  "events": [
                    {
                      "id": "uuid",
                      "title": "string",
                      "startTime": "ISO 8601",
                      "endTime": "ISO 8601",
                      "location": "string",
                      "description": "string",
                      "recurring": "daily|weekly|monthly|yearly|null",
                      "createdAt": "ISO 8601"
                    }
                  ]
                }""";
    }
}
