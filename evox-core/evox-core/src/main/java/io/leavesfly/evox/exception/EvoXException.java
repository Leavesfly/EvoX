package io.leavesfly.evox.exception;

/**
 * EvoX 框架基础异常类
 * 所有框架特定异常的基类，包含统一的错误码机制
 */
public class EvoXException extends RuntimeException {

    private static final int DEFAULT_ERROR_CODE = -1;

    /**
     * 错误码
     */
    private final int errorCode;

    public EvoXException(String message) {
        super(message);
        this.errorCode = DEFAULT_ERROR_CODE;
    }

    public EvoXException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = DEFAULT_ERROR_CODE;
    }

    public EvoXException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public EvoXException(int errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }

    @Override
    public String toString() {
        return String.format("%s[errorCode=%d, message=%s]", 
            this.getClass().getSimpleName(), errorCode, getMessage());
    }
}
