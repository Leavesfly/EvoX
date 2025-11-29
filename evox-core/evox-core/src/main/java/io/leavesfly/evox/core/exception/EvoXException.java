package io.leavesfly.evox.core.exception;

/**
 * EvoX 异常基类
 * 所有业务异常的基础类
 *
 * @author EvoX Team
 */
public class EvoXException extends RuntimeException {

    /**
     * 错误码
     */
    private final String errorCode;

    /**
     * 错误上下文
     */
    private final transient Object context;

    public EvoXException(String message) {
        super(message);
        this.errorCode = "EVOX_ERROR";
        this.context = null;
    }

    public EvoXException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "EVOX_ERROR";
        this.context = null;
    }

    public EvoXException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.context = null;
    }

    public EvoXException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.context = null;
    }

    public EvoXException(String errorCode, String message, Object context) {
        super(message);
        this.errorCode = errorCode;
        this.context = context;
    }

    public EvoXException(String errorCode, String message, Throwable cause, Object context) {
        super(message, cause);
        this.errorCode = errorCode;
        this.context = context;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Object getContext() {
        return context;
    }

    @Override
    public String toString() {
        return String.format("EvoXException[code=%s, message=%s]", errorCode, getMessage());
    }
}
