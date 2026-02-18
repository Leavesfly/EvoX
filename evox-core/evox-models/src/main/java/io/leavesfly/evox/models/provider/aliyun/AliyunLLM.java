package io.leavesfly.evox.models.provider.aliyun;

import io.leavesfly.evox.models.support.OpenAiCompatibleLLM;
import lombok.extern.slf4j.Slf4j;

/**
 * 阿里云通义千问 LLM 实现
 * 通过 DashScope OpenAI 兼容接口调用阿里云大模型服务
 *
 * @author EvoX Team
 */
@Slf4j
public class AliyunLLM extends OpenAiCompatibleLLM {

    private static final String DEFAULT_BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1";
    private final AliyunLLMConfig aliyunConfig;

    public AliyunLLM(AliyunLLMConfig config) {
        super(syncApiKey(config), DEFAULT_BASE_URL);
        if (!config.isValid()) {
            throw new IllegalArgumentException("Invalid AliyunLLMConfig: aliyunApiKey is required");
        }
        this.aliyunConfig = config;
        log.info("Initialized Aliyun LLM with model: {} via OpenAI-compatible API", config.getEffectiveModelName());
    }

    /**
     * 将 aliyunApiKey 同步到基类的 apiKey
     * 这样基类 OpenAiCompatibleLLM 就能正确获取 API key
     */
    private static AliyunLLMConfig syncApiKey(AliyunLLMConfig config) {
        if (config.getApiKey() == null && config.getAliyunApiKey() != null) {
            config.setApiKey(config.getAliyunApiKey());
        }
        return config;
    }

    @Override
    protected String getEffectiveModel() {
        return aliyunConfig.getEffectiveModelName();
    }
}
