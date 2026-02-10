package io.leavesfly.evox.cowork.api.exception;

public class PermissionDeniedException extends CoworkException {
    public PermissionDeniedException(String toolName) {
        super("PERMISSION_DENIED", "Permission denied for tool: " + toolName);
    }
}
