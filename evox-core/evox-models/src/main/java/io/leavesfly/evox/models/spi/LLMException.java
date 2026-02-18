package io.leavesfly.evox.models.spi;

import io.leavesfly.evox.core.exception.EvoXException;

public class LLMException extends EvoXException {
    public LLMException(String message) { super("LLM_ERROR", message); }
    public LLMException(String message, Throwable cause) { super("LLM_ERROR", message, cause); }
    public LLMException(String message, Object context) { super("LLM_ERROR", message, context); }
    public LLMException(String message, Throwable cause, Object context) { super("LLM_ERROR", message, cause, context); }
}
