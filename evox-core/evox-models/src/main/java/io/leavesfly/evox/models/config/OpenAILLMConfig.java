package io.leavesfly.evox.models.config;

import io.leavesfly.evox.core.llm.LLMConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

/**
 * OpenAI LLM配置
 *
 * @author EvoX Team
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class OpenAILLMConfig extends LLMConfig {

    /**
     * 组织ID
     */
    private String organization;

    /**
     * 频率惩罚参数
     */
    private Float frequencyPenalty = 0.0f;

    /**
     * 存在惩罚参数
     */
    private Float presencePenalty = 0.0f;

    public OpenAILLMConfig() {
        setProvider("openai");
        setModel("gpt-4o-mini");
        setTemperature(0.7f);  // 设置默认温度
        setTopP(1.0f);        // 设置默认top-p
        setMaxTokens(1000);   // 设置默认最大token数
    }
}
