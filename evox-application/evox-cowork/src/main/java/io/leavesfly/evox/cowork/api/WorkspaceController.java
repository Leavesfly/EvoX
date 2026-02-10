package io.leavesfly.evox.cowork.api;

import io.leavesfly.evox.cowork.api.exception.InvalidRequestException;
import io.leavesfly.evox.cowork.api.exception.ResourceNotFoundException;
import io.leavesfly.evox.cowork.workspace.Workspace;
import io.leavesfly.evox.cowork.workspace.WorkspaceManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/cowork/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceManager workspaceManager;

    @GetMapping
    public ResponseEntity<List<Workspace>> getWorkspaces() {
        return ResponseEntity.ok(workspaceManager.getAllWorkspaces());
    }

    @PostMapping
    public ResponseEntity<Workspace> addWorkspace(@RequestBody Map<String, String> request) {
        String name = request.get("name");
        String directory = request.get("directory");
        if (name == null || directory == null) {
            throw new InvalidRequestException("Both name and directory are required");
        }
        Workspace workspace = workspaceManager.addWorkspace(name, directory);
        return ResponseEntity.ok(workspace);
    }

    @PostMapping("/{workspaceId}/switch")
    public ResponseEntity<Workspace> switchWorkspace(@PathVariable String workspaceId) {
        Workspace workspace = workspaceManager.switchWorkspace(workspaceId);
        if (workspace == null) {
            throw new ResourceNotFoundException("Workspace", workspaceId);
        }
        return ResponseEntity.ok(workspace);
    }

    @PostMapping("/{workspaceId}/pin")
    public ResponseEntity<Map<String, String>> pinWorkspace(@PathVariable String workspaceId) {
        workspaceManager.pinWorkspace(workspaceId);
        return ResponseEntity.ok(Map.of("message", "Workspace pinned: " + workspaceId));
    }

    @PostMapping("/{workspaceId}/unpin")
    public ResponseEntity<Map<String, String>> unpinWorkspace(@PathVariable String workspaceId) {
        workspaceManager.unpinWorkspace(workspaceId);
        return ResponseEntity.ok(Map.of("message", "Workspace unpinned: " + workspaceId));
    }

    @DeleteMapping("/{workspaceId}")
    public ResponseEntity<Map<String, String>> removeWorkspace(@PathVariable String workspaceId) {
        workspaceManager.removeWorkspace(workspaceId);
        return ResponseEntity.ok(Map.of("message", "Workspace removed: " + workspaceId));
    }
}
