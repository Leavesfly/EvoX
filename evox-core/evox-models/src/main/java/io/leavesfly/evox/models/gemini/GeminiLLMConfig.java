package io.leavesfly.evox.models.gemini;

import io.leavesfly.evox.core.llm.LLMConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.time.Duration;

/**
 * Gemini LLM 配置
 * Google Generative AI API 配置
 *
 * @author EvoX Team
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class GeminiLLMConfig extends LLMConfig {

    public GeminiLLMConfig() {
        super();
        setProvider("gemini");
        setModel("gemini-2.0-flash");
        setBaseUrl("https://generativelanguage.googleapis.com");
        setTemperature(0.7f);
        setMaxTokens(8192);
        setTimeout(Duration.ofSeconds(60));
    }
}
