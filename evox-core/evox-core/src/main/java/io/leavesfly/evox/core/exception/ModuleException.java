package io.leavesfly.evox.core.exception;

/**
 * 模块异常
 * 用于模块初始化、执行过程中的异常
 *
 * @author EvoX Team
 */
public class ModuleException extends EvoXException {

    public ModuleException(String message) {
        super("MODULE_ERROR", message);
    }

    public ModuleException(String message, Throwable cause) {
        super("MODULE_ERROR", message, cause);
    }

    public ModuleException(String message, Object context) {
        super("MODULE_ERROR", message, context);
    }

    public ModuleException(String message, Throwable cause, Object context) {
        super("MODULE_ERROR", message, cause, context);
    }
}
