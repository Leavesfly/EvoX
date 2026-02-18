package io.leavesfly.evox.models.provider.deepseek;

import io.leavesfly.evox.models.support.OpenAiCompatibleLLM;
import lombok.extern.slf4j.Slf4j;

/**
 * DeepSeek LLM 实现
 * 通过 OpenAI 兼容 HTTP 客户端调用 DeepSeek API
 *
 * @author EvoX Team
 */
@Slf4j
public class DeepSeekLLM extends OpenAiCompatibleLLM {

    private static final String DEFAULT_BASE_URL = "https://api.deepseek.com/v1";
    private final DeepSeekLLMConfig deepSeekConfig;

    public DeepSeekLLM(DeepSeekLLMConfig config) {
        super(config, DEFAULT_BASE_URL);
        this.deepSeekConfig = config;
        log.info("Initialized DeepSeek LLM with model: {}", config.getModel());
    }

    @Override
    protected Float getEffectiveFrequencyPenalty() {
        return deepSeekConfig.getFrequencyPenalty() != null ? deepSeekConfig.getFrequencyPenalty() : 0.0f;
    }

    @Override
    protected Float getEffectivePresencePenalty() {
        return deepSeekConfig.getPresencePenalty() != null ? deepSeekConfig.getPresencePenalty() : 0.0f;
    }
}
