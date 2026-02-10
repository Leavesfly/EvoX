package io.leavesfly.evox.cowork.api;

import io.leavesfly.evox.cowork.api.exception.InvalidRequestException;
import io.leavesfly.evox.cowork.api.exception.ResourceNotFoundException;
import io.leavesfly.evox.cowork.task.CoworkTask;
import io.leavesfly.evox.cowork.task.TaskManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/cowork/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskManager taskManager;

    @PostMapping
    public ResponseEntity<Map<String, Object>> submitTask(@RequestBody Map<String, String> request) {
        String description = request.get("description");
        String prompt = request.get("prompt");
        if (description == null || description.trim().isEmpty()) {
            throw new InvalidRequestException("Description is required");
        }
        CoworkTask task = taskManager.decomposeAndSubmit(description, prompt);
        return ResponseEntity.ok(Map.of("task", task));
    }

    @GetMapping
    public ResponseEntity<List<CoworkTask>> getTasks() {
        return ResponseEntity.ok(taskManager.getAllTasks());
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<CoworkTask> getTask(@PathVariable String taskId) {
        CoworkTask task = taskManager.getTask(taskId);
        if (task == null) {
            throw new ResourceNotFoundException("Task", taskId);
        }
        return ResponseEntity.ok(task);
    }

    @PostMapping("/{taskId}/cancel")
    public ResponseEntity<Map<String, String>> cancelTask(@PathVariable String taskId) {
        taskManager.cancelTask(taskId);
        return ResponseEntity.ok(Map.of("message", "Task cancelled: " + taskId));
    }
}
