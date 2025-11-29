package io.leavesfly.evox.core.exception;

/**
 * 配置异常
 * 用于配置错误或缺失的场景
 *
 * @author EvoX Team
 */
public class ConfigurationException extends EvoXException {

    public ConfigurationException(String message) {
        super("CONFIG_ERROR", message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super("CONFIG_ERROR", message, cause);
    }

    public ConfigurationException(String message, Object context) {
        super("CONFIG_ERROR", message, context);
    }
}
