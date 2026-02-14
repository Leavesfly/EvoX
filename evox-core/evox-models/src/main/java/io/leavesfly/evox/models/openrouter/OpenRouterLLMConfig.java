package io.leavesfly.evox.models.openrouter;

import io.leavesfly.evox.core.llm.LLMConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.time.Duration;

/**
 * OpenRouter LLM 配置
 * OpenRouter API 配置，支持自定义站点信息
 *
 * @author EvoX Team
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class OpenRouterLLMConfig extends LLMConfig {

    /**
     * 站点 URL（可选，用于 OpenRouter 的流量追踪）
     */
    private String siteUrl;

    /**
     * 站点名称（可选，用于 OpenRouter 的流量追踪）
     */
    private String siteName;

    public OpenRouterLLMConfig() {
        super();
        setProvider("openrouter");
        setModel("openai/gpt-4o-mini");
        setBaseUrl("https://openrouter.ai/api/v1");
        setTemperature(0.7f);
        setMaxTokens(4096);
        setTimeout(Duration.ofSeconds(60));
    }
}