package io.leavesfly.evox.cowork.api.exception;

public class InvalidRequestException extends CoworkException {
    public InvalidRequestException(String message) {
        super("INVALID_REQUEST", message);
    }
}
