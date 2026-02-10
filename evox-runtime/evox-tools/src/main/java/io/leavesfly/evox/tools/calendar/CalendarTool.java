package io.leavesfly.evox.tools.calendar;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.leavesfly.evox.tools.base.BaseTool;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class CalendarTool extends BaseTool {

    private final String calendarFilePath;
    private final ObjectMapper objectMapper;

    public CalendarTool() {
        this(System.getProperty("user.dir") + "/calendar.json");
    }

    public CalendarTool(String calendarFilePath) {
        super();
        this.name = "calendar_manager";
        this.description = "Manage calendar events stored in a local JSON file. "
                + "Create, list, search, and delete events with conflict detection.";
        this.calendarFilePath = calendarFilePath;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        this.inputs = new HashMap<>();
        this.required = new ArrayList<>();

        Map<String, String> operationParam = new HashMap<>();
        operationParam.put("type", "string");
        operationParam.put("description", "Operation: 'create', 'list', 'today', 'search', 'delete'");
        this.inputs.put("operation", operationParam);
        this.required.add("operation");

        Map<String, String> titleParam = new HashMap<>();
        titleParam.put("type", "string");
        titleParam.put("description", "Event title (required for create)");
        this.inputs.put("title", titleParam);

        Map<String, String> startTimeParam = new HashMap<>();
        startTimeParam.put("type", "string");
        startTimeParam.put("description", "Event start time in ISO 8601 format (required for create)");
        this.inputs.put("startTime", startTimeParam);

        Map<String, String> endTimeParam = new HashMap<>();
        endTimeParam.put("type", "string");
        endTimeParam.put("description", "Event end time in ISO 8601 format (optional)");
        this.inputs.put("endTime", endTimeParam);

        Map<String, String> descriptionParam = new HashMap<>();
        descriptionParam.put("type", "string");
        descriptionParam.put("description", "Event description (optional)");
        this.inputs.put("description", descriptionParam);

        Map<String, String> queryParam = new HashMap<>();
        queryParam.put("type", "string");
        queryParam.put("description", "Search query (for search operation)");
        this.inputs.put("query", queryParam);

        Map<String, String> eventIdParam = new HashMap<>();
        eventIdParam.put("type", "string");
        eventIdParam.put("description", "Event ID (for delete operation)");
        this.inputs.put("eventId", eventIdParam);
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        validateParameters(parameters);
        String operation = getParameter(parameters, "operation", "");

        return switch (operation) {
            case "create" -> createEvent(parameters);
            case "list" -> listEvents();
            case "today" -> todayEvents();
            case "search" -> searchEvents(getParameter(parameters, "query", ""));
            case "delete" -> deleteEvent(getParameter(parameters, "eventId", ""));
            default -> ToolResult.failure("Unknown operation: " + operation + ". Use: create, list, today, search, delete");
        };
    }

    private ToolResult createEvent(Map<String, Object> parameters) {
        String title = getParameter(parameters, "title", "");
        String startTime = getParameter(parameters, "startTime", "");
        String endTime = getParameter(parameters, "endTime", "");
        String description = getParameter(parameters, "description", "");

        if (title.isEmpty() || startTime.isEmpty()) {
            return ToolResult.failure("'title' and 'startTime' are required for create operation");
        }

        try {
            List<Map<String, Object>> events = loadEvents();

            Map<String, Object> newEvent = new LinkedHashMap<>();
            newEvent.put("id", UUID.randomUUID().toString().substring(0, 8));
            newEvent.put("title", title);
            newEvent.put("startTime", startTime);
            if (!endTime.isEmpty()) newEvent.put("endTime", endTime);
            if (!description.isEmpty()) newEvent.put("description", description);
            newEvent.put("createdAt", Instant.now().toString());

            List<String> conflicts = findConflicts(events, startTime, endTime);

            events.add(newEvent);
            saveEvents(events);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("created", true);
            result.put("event", newEvent);
            if (!conflicts.isEmpty()) {
                result.put("conflicts", conflicts);
                result.put("warning", "Time conflicts detected with existing events");
            }
            return ToolResult.success(result);
        } catch (Exception e) {
            log.error("Error creating event", e);
            return ToolResult.failure("Failed to create event: " + e.getMessage());
        }
    }

    private ToolResult listEvents() {
        try {
            List<Map<String, Object>> events = loadEvents();
            events.sort(Comparator.comparing(e -> e.getOrDefault("startTime", "").toString()));

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("totalEvents", events.size());
            result.put("events", events);
            return ToolResult.success(result);
        } catch (Exception e) {
            return ToolResult.failure("Failed to list events: " + e.getMessage());
        }
    }

    private ToolResult todayEvents() {
        try {
            List<Map<String, Object>> events = loadEvents();
            String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

            List<Map<String, Object>> todayList = events.stream()
                    .filter(e -> e.getOrDefault("startTime", "").toString().startsWith(today))
                    .sorted(Comparator.comparing(e -> e.getOrDefault("startTime", "").toString()))
                    .collect(Collectors.toList());

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("date", today);
            result.put("eventCount", todayList.size());
            result.put("events", todayList);
            return ToolResult.success(result);
        } catch (Exception e) {
            return ToolResult.failure("Failed to get today events: " + e.getMessage());
        }
    }

    private ToolResult searchEvents(String query) {
        if (query == null || query.isBlank()) {
            return ToolResult.failure("Search query is required");
        }
        try {
            List<Map<String, Object>> events = loadEvents();
            String lowerQuery = query.toLowerCase();

            List<Map<String, Object>> matched = events.stream()
                    .filter(e -> e.getOrDefault("title", "").toString().toLowerCase().contains(lowerQuery)
                            || e.getOrDefault("description", "").toString().toLowerCase().contains(lowerQuery))
                    .collect(Collectors.toList());

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("query", query);
            result.put("matchCount", matched.size());
            result.put("events", matched);
            return ToolResult.success(result);
        } catch (Exception e) {
            return ToolResult.failure("Failed to search events: " + e.getMessage());
        }
    }

    private ToolResult deleteEvent(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            return ToolResult.failure("Event ID is required for delete operation");
        }
        try {
            List<Map<String, Object>> events = loadEvents();
            boolean removed = events.removeIf(e -> eventId.equals(e.get("id")));

            if (removed) {
                saveEvents(events);
                return ToolResult.success(Map.of("deleted", true, "eventId", eventId));
            } else {
                return ToolResult.failure("Event not found: " + eventId);
            }
        } catch (Exception e) {
            return ToolResult.failure("Failed to delete event: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> loadEvents() throws IOException {
        File file = new File(calendarFilePath);
        if (!file.exists()) {
            return new ArrayList<>();
        }
        Map<String, Object> data = objectMapper.readValue(file, new TypeReference<>() {});
        Object events = data.get("events");
        if (events instanceof List) {
            return new ArrayList<>((List<Map<String, Object>>) events);
        }
        return new ArrayList<>();
    }

    private void saveEvents(List<Map<String, Object>> events) throws IOException {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("events", events);
        data.put("lastModified", Instant.now().toString());
        objectMapper.writeValue(new File(calendarFilePath), data);
    }

    private List<String> findConflicts(List<Map<String, Object>> events, String startTime, String endTime) {
        List<String> conflicts = new ArrayList<>();
        for (Map<String, Object> event : events) {
            String existingStart = event.getOrDefault("startTime", "").toString();
            String existingEnd = event.getOrDefault("endTime", "").toString();

            if (existingStart.isEmpty()) continue;

            boolean overlaps = false;
            if (!endTime.isEmpty() && !existingEnd.isEmpty()) {
                overlaps = startTime.compareTo(existingEnd) < 0 && endTime.compareTo(existingStart) > 0;
            } else {
                overlaps = startTime.equals(existingStart);
            }

            if (overlaps) {
                conflicts.add(event.getOrDefault("title", "Untitled").toString()
                        + " (" + existingStart + ")");
            }
        }
        return conflicts;
    }
}
