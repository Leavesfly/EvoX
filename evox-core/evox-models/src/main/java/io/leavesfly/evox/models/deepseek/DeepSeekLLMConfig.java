package io.leavesfly.evox.models.deepseek;

import io.leavesfly.evox.models.config.LLMConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

/**
 * DeepSeek LLM 配置类
 *
 * @author EvoX Team
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class DeepSeekLLMConfig extends LLMConfig {

    /**
     * 频率惩罚
     */
    private Float frequencyPenalty = 0.0f;

    /**
     * 存在惩罚
     */
    private Float presencePenalty = 0.0f;

    public DeepSeekLLMConfig() {
        setProvider("deepseek");
        setModel("deepseek-chat");
        setBaseUrl("https://api.deepseek.com/v1");
        setTemperature(0.7f);
        setMaxTokens(4096);
        setFrequencyPenalty(0.0f);
        setPresencePenalty(0.0f);
    }
}
