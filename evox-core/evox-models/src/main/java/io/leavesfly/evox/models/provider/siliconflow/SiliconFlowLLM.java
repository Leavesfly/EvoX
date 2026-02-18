package io.leavesfly.evox.models.provider.siliconflow;

import io.leavesfly.evox.models.support.OpenAiCompatibleLLM;
import lombok.extern.slf4j.Slf4j;

/**
 * 硅基流动 LLM 实现
 * 通过 OpenAI 兼容 HTTP 客户端调用硅基流动服务
 *
 * @author EvoX Team
 */
@Slf4j
public class SiliconFlowLLM extends OpenAiCompatibleLLM {

    private final SiliconFlowLLMConfig siliconFlowConfig;

    public SiliconFlowLLM(SiliconFlowLLMConfig config) {
        super(syncApiKey(config), config.getBaseUrl());
        if (!config.isValid()) {
            throw new IllegalArgumentException("Invalid SiliconFlowLLMConfig: API key is required");
        }
        this.siliconFlowConfig = config;
        log.info("Initialized SiliconFlow LLM with model: {}", config.getModel());
    }

    private static SiliconFlowLLMConfig syncApiKey(SiliconFlowLLMConfig config) {
        if (config.getApiKey() == null && config.getSiliconflowKey() != null) {
            config.setApiKey(config.getSiliconflowKey());
        }
        return config;
    }

    @Override
    public boolean supportsToolUse() {
        return false;
    }
}
