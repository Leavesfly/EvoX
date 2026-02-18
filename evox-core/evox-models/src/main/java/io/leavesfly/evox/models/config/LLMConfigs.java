package io.leavesfly.evox.models.config;

import io.leavesfly.evox.models.provider.aliyun.AliyunLLMConfig;
import io.leavesfly.evox.models.provider.deepseek.DeepSeekLLMConfig;
import io.leavesfly.evox.models.provider.ollama.OllamaLLMConfig;
import io.leavesfly.evox.models.provider.openai.OpenAILLMConfig;
import io.leavesfly.evox.models.provider.siliconflow.SiliconFlowLLMConfig;

/**
 * LLM 配置快捷创建工具类
 *
 * <p>用法示例：
 * <pre>{@code
 * LLMConfig config = LLMConfigs.openAI("sk-xxx", "gpt-4o");
 * LLMConfig config = LLMConfigs.aliyun("sk-xxx", "qwen-turbo");
 * LLMConfig config = LLMConfigs.ollama("llama3");
 * }</pre>
 *
 * @author EvoX Team
 */
public final class LLMConfigs {

    private LLMConfigs() {
    }

    /**
     * 创建 OpenAI 配置
     *
     * @param apiKey API 密钥
     * @param model  模型名称（如 "gpt-4o-mini", "gpt-4o", "gpt-3.5-turbo"）
     * @return OpenAILLMConfig
     */
    public static OpenAILLMConfig openAI(String apiKey, String model) {
        return OpenAILLMConfig.builder().apiKey(apiKey).model(model).build();
    }

    /**
     * 创建 OpenAI 配置（默认模型 gpt-4o-mini）
     */
    public static OpenAILLMConfig openAI(String apiKey) {
        return openAI(apiKey, "gpt-4o-mini");
    }

    /**
     * 创建阿里云通义千问配置
     *
     * @param apiKey DashScope API 密钥
     * @param model  模型名称（如 "qwen-turbo", "qwen-max", "qwen-plus"）
     * @return AliyunLLMConfig
     */
    public static AliyunLLMConfig aliyun(String apiKey, String model) {
        return AliyunLLMConfig.builder().aliyunApiKey(apiKey).model(model).build();
    }

    /**
     * 创建阿里云通义千问配置（默认模型 qwen-turbo）
     */
    public static AliyunLLMConfig aliyun(String apiKey) {
        return aliyun(apiKey, "qwen-turbo");
    }

    /**
     * 创建 DeepSeek 配置
     *
     * @param apiKey API 密钥
     * @param model  模型名称（如 "deepseek-chat"）
     * @return DeepSeekLLMConfig
     */
    public static DeepSeekLLMConfig deepSeek(String apiKey, String model) {
        return DeepSeekLLMConfig.builder().apiKey(apiKey).model(model).build();
    }

    /**
     * 创建 DeepSeek 配置（默认模型 deepseek-chat）
     */
    public static DeepSeekLLMConfig deepSeek(String apiKey) {
        return deepSeek(apiKey, "deepseek-chat");
    }

    /**
     * 创建 Ollama 配置（本地模型）
     *
     * @param model 模型名称（如 "llama3", "mistral", "codellama"）
     * @return OllamaLLMConfig
     */
    public static OllamaLLMConfig ollama(String model) {
        return OllamaLLMConfig.builder().model(model).build();
    }

    /**
     * 创建 Ollama 配置（自定义地址）
     */
    public static OllamaLLMConfig ollama(String model, String baseUrl) {
        return OllamaLLMConfig.builder().model(model).baseUrl(baseUrl).build();
    }

    /**
     * 创建 SiliconFlow 配置
     *
     * @param apiKey API 密钥
     * @param model  模型名称（如 "Qwen/Qwen2.5-7B-Instruct"）
     * @return SiliconFlowLLMConfig
     */
    public static SiliconFlowLLMConfig siliconFlow(String apiKey, String model) {
        return SiliconFlowLLMConfig.builder().siliconflowKey(apiKey).model(model).build();
    }

    /**
     * 创建 SiliconFlow 配置（默认模型）
     */
    public static SiliconFlowLLMConfig siliconFlow(String apiKey) {
        return siliconFlow(apiKey, "Qwen/Qwen2.5-7B-Instruct");
    }

}
