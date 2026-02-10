package io.leavesfly.evox.models.config;

/**
 * LLM 配置快捷创建工具类
 * 将工厂方法从 {@link LLMConfig} 中外移，让 LLMConfig 回归纯配置职责。
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
        // 工具类禁止实例化
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
     * @return SiliconFlowConfig
     */
    public static SiliconFlowConfig siliconFlow(String apiKey, String model) {
        return SiliconFlowConfig.builder().siliconflowKey(apiKey).model(model).build();
    }

    /**
     * 创建 SiliconFlow 配置（默认模型）
     */
    public static SiliconFlowConfig siliconFlow(String apiKey) {
        return siliconFlow(apiKey, "Qwen/Qwen2.5-7B-Instruct");
    }

    /**
     * 创建 LiteLLM 配置（多模型代理）
     *
     * @param model   模型名称
     * @param baseUrl LiteLLM 代理地址
     * @return LiteLLMConfig
     */
    public static LiteLLMConfig liteLLM(String model, String baseUrl) {
        return LiteLLMConfig.builder().model(model).litellmBaseUrl(baseUrl).build();
    }

    /**
     * 创建 LiteLLM 配置（本地代理 localhost:4000）
     */
    public static LiteLLMConfig liteLLM(String model) {
        return liteLLM(model, "http://localhost:4000");
    }
}
