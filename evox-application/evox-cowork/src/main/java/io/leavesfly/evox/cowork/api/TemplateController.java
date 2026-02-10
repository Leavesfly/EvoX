package io.leavesfly.evox.cowork.api;

import io.leavesfly.evox.cowork.api.exception.ResourceNotFoundException;
import io.leavesfly.evox.cowork.template.TemplateManager;
import io.leavesfly.evox.cowork.template.WorkflowTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/cowork/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateManager templateManager;

    @GetMapping
    public ResponseEntity<List<WorkflowTemplate>> getTemplates() {
        return ResponseEntity.ok(templateManager.getAllTemplates());
    }

    @GetMapping("/{templateId}")
    public ResponseEntity<WorkflowTemplate> getTemplate(@PathVariable String templateId) {
        WorkflowTemplate template = templateManager.getTemplate(templateId);
        if (template == null) {
            throw new ResourceNotFoundException("Template", templateId);
        }
        return ResponseEntity.ok(template);
    }

    @PostMapping
    public ResponseEntity<WorkflowTemplate> saveTemplate(@RequestBody WorkflowTemplate template) {
        templateManager.saveTemplate(template);
        return ResponseEntity.ok(template);
    }

    @DeleteMapping("/{templateId}")
    public ResponseEntity<Map<String, String>> deleteTemplate(@PathVariable String templateId) {
        templateManager.deleteTemplate(templateId);
        return ResponseEntity.ok(Map.of("message", "Template deleted: " + templateId));
    }

    @PostMapping("/{templateId}/render")
    public ResponseEntity<Map<String, String>> renderTemplate(
            @PathVariable String templateId,
            @RequestBody Map<String, String> variables) {
        String rendered = templateManager.renderTemplate(templateId, variables);
        return ResponseEntity.ok(Map.of("rendered", rendered));
    }

    @GetMapping("/search")
    public ResponseEntity<List<WorkflowTemplate>> searchTemplates(@RequestParam String keyword) {
        return ResponseEntity.ok(templateManager.searchTemplates(keyword));
    }
}
