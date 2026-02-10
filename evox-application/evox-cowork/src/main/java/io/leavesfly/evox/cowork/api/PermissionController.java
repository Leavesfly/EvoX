package io.leavesfly.evox.cowork.api;

import io.leavesfly.evox.cowork.api.exception.InvalidRequestException;
import io.leavesfly.evox.cowork.api.exception.ResourceNotFoundException;
import io.leavesfly.evox.cowork.permission.InteractivePermissionManager;
import io.leavesfly.evox.cowork.permission.PermissionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/cowork/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final InteractivePermissionManager permissionManager;

    @GetMapping("/pending")
    public ResponseEntity<List<PermissionRequest>> getPendingPermissions() {
        return ResponseEntity.ok(permissionManager.getPendingRequests());
    }

    @PostMapping("/{requestId}/reply")
    public ResponseEntity<Map<String, Object>> replyPermission(
            @PathVariable String requestId,
            @RequestBody Map<String, String> request) {
        String replyStr = request.get("reply");
        if (replyStr == null) {
            throw new InvalidRequestException("Reply is required (once/always/reject)");
        }

        PermissionRequest.PermissionReply reply;
        try {
            reply = PermissionRequest.PermissionReply.valueOf(replyStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("Invalid reply. Must be: once, always, or reject");
        }

        boolean success = permissionManager.replyPermission(requestId, reply);
        if (!success) {
            throw new ResourceNotFoundException("PermissionRequest", requestId);
        }

        return ResponseEntity.ok(Map.of(
                "message", "Permission replied",
                "requestId", requestId,
                "reply", reply.name()
        ));
    }
}
