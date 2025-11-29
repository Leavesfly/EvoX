package io.leavesfly.evox.core.exception;

/**
 * 执行异常
 * 用于Agent、Action、Workflow执行过程中的异常
 *
 * @author EvoX Team
 */
public class ExecutionException extends EvoXException {

    public ExecutionException(String message) {
        super("EXECUTION_ERROR", message);
    }

    public ExecutionException(String message, Throwable cause) {
        super("EXECUTION_ERROR", message, cause);
    }

    public ExecutionException(String message, Object context) {
        super("EXECUTION_ERROR", message, context);
    }

    public ExecutionException(String message, Throwable cause, Object context) {
        super("EXECUTION_ERROR", message, cause, context);
    }
}
