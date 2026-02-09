package io.leavesfly.evox.models.config;

import io.leavesfly.evox.core.module.BaseModule;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.time.Duration;

/**
 * LLM配置基类
 * 所有LLM配置的基础类
 *
 * @author EvoX Team
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public abstract class LLMConfig extends BaseModule {

    /**
     * 提供商标识
     */
    private String provider;

    /**
     * 模型名称
     */
    private String model;

    /**
     * API密钥
     */
    private String apiKey;

    /**
     * API基础URL
     */
    private String baseUrl;

    /**
     * 温度参数(0.0-2.0)
     */
    @Builder.Default
    private Float temperature = 0.7f;

    /**
     * 最大token数
     */
    @Builder.Default
    private Integer maxTokens = 1000;

    /**
     * Top-p采样参数
     */
    @Builder.Default
    private Float topP = 1.0f;

    /**
     * 频率惩罚
     */
    @Builder.Default
    private Float frequencyPenalty = 0.0f;

    /**
     * 存在惩罚
     */
    @Builder.Default
    private Float presencePenalty = 0.0f;

    /**
     * 是否启用流式输出
     */
    private Boolean stream = false;

    /**
     * 请求超时时间
     */
    private Duration timeout = Duration.ofSeconds(60);

    /**
     * 是否输出响应到控制台
     */
    private Boolean outputResponse;

    public LLMConfig() {
        super();
    }

    // ===================================================================
    // 静态快捷创建方法 - 用一行代码创建任意 LLM 配置
    // ===================================================================

    /**
     * 创建 OpenAI 配置
     *
     * <pre>{@code
     * LLMConfig config = LLMConfig.ofOpenAI("sk-xxx", "gpt-4o");
     * }</pre>
     *
     * @param apiKey API密钥
     * @param model  模型名称（如 "gpt-4o-mini", "gpt-4o", "gpt-3.5-turbo"）
     * @return OpenAILLMConfig
     */
    public static OpenAILLMConfig ofOpenAI(String apiKey, String model) {
        return OpenAILLMConfig.builder().apiKey(apiKey).model(model).build();
    }

    /**
     * 创建 OpenAI 配置（默认模型 gpt-4o-mini）
     */
    public static OpenAILLMConfig ofOpenAI(String apiKey) {
        return ofOpenAI(apiKey, "gpt-4o-mini");
    }

    /**
     * 创建阿里云通义千问配置
     *
     * @param apiKey DashScope API密钥
     * @param model  模型名称（如 "qwen-turbo", "qwen-max", "qwen-plus"）
     * @return AliyunLLMConfig
     */
    public static AliyunLLMConfig ofAliyun(String apiKey, String model) {
        return AliyunLLMConfig.builder().aliyunApiKey(apiKey).model(model).build();
    }

    /**
     * 创建阿里云通义千问配置（默认模型 qwen-turbo）
     */
    public static AliyunLLMConfig ofAliyun(String apiKey) {
        return ofAliyun(apiKey, "qwen-turbo");
    }

    /**
     * 创建 Ollama 配置（本地模型）
     *
     * @param model 模型名称（如 "llama3", "mistral", "codellama"）
     * @return OllamaLLMConfig
     */
    public static OllamaLLMConfig ofOllama(String model) {
        return OllamaLLMConfig.builder().model(model).build();
    }

    /**
     * 创建 Ollama 配置（自定义地址）
     */
    public static OllamaLLMConfig ofOllama(String model, String baseUrl) {
        return OllamaLLMConfig.builder().model(model).baseUrl(baseUrl).build();
    }

    /**
     * 创建 SiliconFlow 配置
     *
     * @param apiKey API密钥
     * @param model  模型名称（如 "Qwen/Qwen2.5-7B-Instruct"）
     * @return SiliconFlowConfig
     */
    public static SiliconFlowConfig ofSiliconFlow(String apiKey, String model) {
        return SiliconFlowConfig.builder().siliconflowKey(apiKey).model(model).build();
    }

    /**
     * 创建 SiliconFlow 配置（默认模型）
     */
    public static SiliconFlowConfig ofSiliconFlow(String apiKey) {
        return ofSiliconFlow(apiKey, "Qwen/Qwen2.5-7B-Instruct");
    }

    /**
     * 创建 LiteLLM 配置（多模型代理）
     *
     * @param model   模型名称
     * @param baseUrl LiteLLM 代理地址
     * @return LiteLLMConfig
     */
    public static LiteLLMConfig ofLiteLLM(String model, String baseUrl) {
        return LiteLLMConfig.builder().model(model).litellmBaseUrl(baseUrl).build();
    }

    /**
     * 创建 LiteLLM 配置（本地代理 localhost:4000）
     */
    public static LiteLLMConfig ofLiteLLM(String model) {
        return ofLiteLLM(model, "http://localhost:4000");
    }
}
