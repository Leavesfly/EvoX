package io.leavesfly.evox.models.anthropic;

import io.leavesfly.evox.models.config.LLMConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

/**
 * Anthropic (Claude) LLM 配置类
 *
 * @author EvoX Team
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class AnthropicLLMConfig extends LLMConfig {

    /**
     * Anthropic API 版本
     */
    private String anthropicVersion = "2023-06-01";

    public AnthropicLLMConfig() {
        setProvider("anthropic");
        setModel("claude-sonnet-4-20250514");
        setBaseUrl("https://api.anthropic.com");
        setTemperature(0.7f);
        setMaxTokens(4096);
        setAnthropicVersion("2023-06-01");
    }
}
