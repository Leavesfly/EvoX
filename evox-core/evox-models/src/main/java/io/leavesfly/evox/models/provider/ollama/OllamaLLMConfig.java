package io.leavesfly.evox.models.provider.ollama;

import io.leavesfly.evox.core.llm.LLMConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

/**
 * Ollama LLM 配置
 * 用于连接本地或远程 Ollama 服务
 *
 * @author EvoX Team
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class OllamaLLMConfig extends LLMConfig {

    /**
     * 默认 Ollama 服务地址
     */
    public static final String DEFAULT_BASE_URL = "http://localhost:11434";

    public OllamaLLMConfig() {
        setProvider("ollama");
        setModel("qwen3:4b-instruct-2507-q8_0");
        setBaseUrl(DEFAULT_BASE_URL);
        setTemperature(0.7f);
        setTopP(1.0f);
        setMaxTokens(5000);
        // Ollama 本地服务无需 API Key
        setApiKey("ollama");
    }

    /**
     * 获取有效的 Base URL
     */
    public String getEffectiveBaseUrl() {
        return getBaseUrl() != null && !getBaseUrl().isEmpty()
                ? getBaseUrl()
                : DEFAULT_BASE_URL;
    }
}
