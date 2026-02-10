package io.leavesfly.evox.cowork.api.exception;

public class CoworkException extends RuntimeException {
    private final String errorCode;

    public CoworkException(String message) {
        super(message);
        this.errorCode = "COWORK_ERROR";
    }

    public CoworkException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public CoworkException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
