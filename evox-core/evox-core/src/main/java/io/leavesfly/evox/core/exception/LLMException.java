package io.leavesfly.evox.core.exception;

/**
 * LLM调用异常
 * 用于大语言模型调用失败的场景
 *
 * @author EvoX Team
 */
public class LLMException extends EvoXException {

    public LLMException(String message) {
        super("LLM_ERROR", message);
    }

    public LLMException(String message, Throwable cause) {
        super("LLM_ERROR", message, cause);
    }

    public LLMException(String message, Object context) {
        super("LLM_ERROR", message, context);
    }

    public LLMException(String message, Throwable cause, Object context) {
        super("LLM_ERROR", message, cause, context);
    }
}
