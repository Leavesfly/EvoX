package io.leavesfly.evox.models.provider.openai;

import io.leavesfly.evox.models.support.OpenAiCompatibleLLM;
import lombok.extern.slf4j.Slf4j;

/**
 * OpenAI LLM 实现
 * 通过 OpenAI 兼容 HTTP 客户端调用 OpenAI API
 *
 * @author EvoX Team
 */
@Slf4j
public class OpenAILLM extends OpenAiCompatibleLLM {

    private static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";
    private final OpenAILLMConfig openAIConfig;

    public OpenAILLM(OpenAILLMConfig config) {
        super(config, DEFAULT_BASE_URL);
        this.openAIConfig = config;
        log.info("Initialized OpenAI LLM with model: {}", config.getModel());
    }

    @Override
    protected Float getEffectiveFrequencyPenalty() {
        return openAIConfig.getFrequencyPenalty() != null ? openAIConfig.getFrequencyPenalty() : 0.0f;
    }

    @Override
    protected Float getEffectivePresencePenalty() {
        return openAIConfig.getPresencePenalty() != null ? openAIConfig.getPresencePenalty() : 0.0f;
    }
}
