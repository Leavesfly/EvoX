package io.leavesfly.evox.models.groq;

import io.leavesfly.evox.models.config.LLMConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

/**
 * Groq LLM 配置
 * Groq 提供快速的 AI 推理服务，兼容 OpenAI 协议
 *
 * @author EvoX Team
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class GroqLLMConfig extends LLMConfig {

    /**
     * Whisper 语音转写模型
     * 默认使用 whisper-large-v3-turbo
     */
    @lombok.Builder.Default
    private String whisperModel = "whisper-large-v3-turbo";

    public GroqLLMConfig() {
        setProvider("groq");
        setModel("llama-3.3-70b-versatile");
        setBaseUrl("https://api.groq.com/openai/v1");
        setTemperature(0.7f);
        setMaxTokens(4096);
    }
}
