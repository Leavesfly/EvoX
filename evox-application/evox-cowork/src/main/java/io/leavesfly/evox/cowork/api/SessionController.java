package io.leavesfly.evox.cowork.api;

import io.leavesfly.evox.cowork.api.exception.InvalidRequestException;
import io.leavesfly.evox.cowork.api.exception.ResourceNotFoundException;
import io.leavesfly.evox.cowork.permission.InteractivePermissionManager;
import io.leavesfly.evox.cowork.session.CoworkSession;
import io.leavesfly.evox.cowork.session.SessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/cowork/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionManager sessionManager;
    private final InteractivePermissionManager permissionManager;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createSession(@RequestBody(required = false) Map<String, String> request) {
        String workingDirectory = request != null ? request.get("workingDirectory") : null;
        CoworkSession session = sessionManager.createSession(workingDirectory);
        permissionManager.setCurrentSessionId(session.getSessionId());
        return ResponseEntity.ok(session.toSummary());
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listSessions() {
        List<Map<String, Object>> sessionSummaries = sessionManager.listSessions().stream()
                .map(CoworkSession::toSummary)
                .collect(Collectors.toList());
        return ResponseEntity.ok(sessionSummaries);
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<Map<String, Object>> getSession(@PathVariable String sessionId) {
        CoworkSession session = sessionManager.getSession(sessionId);
        if (session == null) {
            throw new ResourceNotFoundException("Session", sessionId);
        }
        return ResponseEntity.ok(session.toSummary());
    }

    @GetMapping("/{sessionId}/messages")
    public ResponseEntity<List<CoworkSession.SessionMessage>> getSessionMessages(@PathVariable String sessionId) {
        CoworkSession session = sessionManager.getSession(sessionId);
        if (session == null) {
            throw new ResourceNotFoundException("Session", sessionId);
        }
        return ResponseEntity.ok(sessionManager.getMessages(sessionId));
    }

    @PostMapping("/{sessionId}/prompt")
    public ResponseEntity<Map<String, Object>> prompt(
            @PathVariable String sessionId,
            @RequestBody Map<String, String> request) {
        String message = request.get("message");
        if (message == null || message.trim().isEmpty()) {
            throw new InvalidRequestException("Message is required");
        }

        CoworkSession session = sessionManager.getSession(sessionId);
        if (session == null) {
            throw new ResourceNotFoundException("Session", sessionId);
        }

        permissionManager.setCurrentSessionId(sessionId);
        String response = sessionManager.prompt(sessionId, message);
        Map<String, Object> result = new HashMap<>();
        result.put("response", response);
        result.put("sessionId", sessionId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{sessionId}/abort")
    public ResponseEntity<Map<String, String>> abortSession(@PathVariable String sessionId) {
        CoworkSession session = sessionManager.getSession(sessionId);
        if (session == null) {
            throw new ResourceNotFoundException("Session", sessionId);
        }
        sessionManager.abortSession(sessionId);
        return ResponseEntity.ok(Map.of("message", "Session aborted: " + sessionId));
    }

    @GetMapping("/{sessionId}/summarize")
    public ResponseEntity<Map<String, String>> summarizeSession(@PathVariable String sessionId) {
        CoworkSession session = sessionManager.getSession(sessionId);
        if (session == null) {
            throw new ResourceNotFoundException("Session", sessionId);
        }
        String summary = sessionManager.summarizeSession(sessionId);
        return ResponseEntity.ok(Map.of("summary", summary));
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Map<String, String>> deleteSession(@PathVariable String sessionId) {
        CoworkSession session = sessionManager.getSession(sessionId);
        if (session == null) {
            throw new ResourceNotFoundException("Session", sessionId);
        }
        sessionManager.deleteSession(sessionId);
        return ResponseEntity.ok(Map.of("message", "Session deleted: " + sessionId));
    }

    @PostMapping("/{sessionId}/switch")
    public ResponseEntity<Map<String, String>> switchSession(@PathVariable String sessionId) {
        CoworkSession session = sessionManager.getSession(sessionId);
        if (session == null) {
            throw new ResourceNotFoundException("Session", sessionId);
        }
        sessionManager.switchSession(sessionId);
        permissionManager.setCurrentSessionId(sessionId);
        return ResponseEntity.ok(Map.of("message", "Switched to session: " + sessionId));
    }
}
