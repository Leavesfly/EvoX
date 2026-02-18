package io.leavesfly.evox.models.provider.ollama;

import io.leavesfly.evox.models.support.OpenAiCompatibleLLM;
import lombok.extern.slf4j.Slf4j;

/**
 * Ollama LLM 实现
 * 通过 OpenAI 兼容 HTTP 客户端调用 Ollama 服务
 *
 * @author EvoX Team
 */
@Slf4j
public class OllamaLLM extends OpenAiCompatibleLLM {

    public OllamaLLM(OllamaLLMConfig config) {
        super(config, config.getEffectiveBaseUrl());
        log.info("Initialized Ollama LLM with model: {} at {}", config.getModel(), config.getEffectiveBaseUrl());
    }

    @Override
    public boolean supportsToolUse() {
        return false;
    }
}
