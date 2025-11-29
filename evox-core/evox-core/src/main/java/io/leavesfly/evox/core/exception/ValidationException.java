package io.leavesfly.evox.core.exception;

/**
 * 参数验证异常
 * 用于参数校验失败的场景
 *
 * @author EvoX Team
 */
public class ValidationException extends EvoXException {

    public ValidationException(String message) {
        super("VALIDATION_ERROR", message);
    }

    public ValidationException(String message, Object context) {
        super("VALIDATION_ERROR", message, context);
    }

    public ValidationException(String message, Throwable cause) {
        super("VALIDATION_ERROR", message, cause);
    }
}
