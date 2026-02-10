package io.leavesfly.evox.cowork.api.exception;

public class ResourceNotFoundException extends CoworkException {
    public ResourceNotFoundException(String resourceType, String resourceId) {
        super("NOT_FOUND", resourceType + " not found: " + resourceId);
    }
}
