package io.leavesfly.evox.models.factory;

import io.leavesfly.evox.models.aliyun.AliyunLLM;
import io.leavesfly.evox.models.base.LLMProvider;
import io.leavesfly.evox.models.config.*;
import io.leavesfly.evox.models.litellm.LiteLLM;
import io.leavesfly.evox.models.ollama.OllamaLLM;
import io.leavesfly.evox.models.openai.OpenAILLM;
import io.leavesfly.evox.models.siliconflow.SiliconFlowLLM;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * LLM 工厂
 * 根据 LLMConfig 自动创建对应的 LLMProvider 实例
 *
 * <p>核心作用：消除手动 new XxxLLM(config) 的样板代码，
 * 让用户只需关注配置，不需要知道具体的 LLM 实现类</p>
 *
 * <p>使用方式：</p>
 * <pre>{@code
 * // 方式1：从配置自动创建
 * LLMConfig config = LLMConfigs.openAI("sk-xxx", "gpt-4o");
 * LLMProvider llm = LLMFactory.create(config);
 *
 * // 方式2：快捷方法
 * LLMProvider llm = LLMFactory.openai("sk-xxx");
 * LLMProvider llm = LLMFactory.aliyun("sk-xxx", "qwen-max");
 * LLMProvider llm = LLMFactory.ollama("llama3");
 * }</pre>
 *
 * @author EvoX Team
 */
@Slf4j
public class LLMFactory {

    /**
     * 已注册的 provider -> 创建函数映射
     */
    private static final Map<String, Function<LLMConfig, LLMProvider>> REGISTRY = new ConcurrentHashMap<>();

    static {
        // 注册内置 provider
        register("openai", config -> new OpenAILLM((OpenAILLMConfig) config));
        register("ollama", config -> new OllamaLLM((OllamaLLMConfig) config));
        register("litellm", config -> new LiteLLM((LiteLLMConfig) config));
        register("aliyun", config -> new AliyunLLM((AliyunLLMConfig) config));
        register("siliconflow", config -> new SiliconFlowLLM((SiliconFlowConfig) config));
    }

    private LLMFactory() {
        // 工具类不允许实例化
    }

    // ===================================================================
    // 核心工厂方法
    // ===================================================================

    /**
     * 根据 LLMConfig 自动创建对应的 LLMProvider 实例
     *
     * @param config LLM 配置
     * @return LLMProvider 实例
     * @throws IllegalArgumentException 如果配置为null或provider未注册
     */
    public static LLMProvider create(LLMConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("LLMConfig cannot be null");
        }

        String provider = config.getProvider();
        if (provider == null || provider.isEmpty()) {
            // 根据配置类型推断 provider
            provider = inferProvider(config);
        }

        Function<LLMConfig, LLMProvider> creator = REGISTRY.get(provider.toLowerCase());
        if (creator == null) {
            throw new IllegalArgumentException(
                    "Unknown LLM provider: '" + provider + "'. " +
                    "Supported providers: " + REGISTRY.keySet() + ". " +
                    "Use LLMFactory.register() to add custom providers.");
        }

        log.debug("Creating LLM instance for provider '{}', model '{}'", provider, config.getModel());
        return creator.apply(config);
    }

    /**
     * 注册自定义 LLM provider
     * 允许用户扩展支持新的 LLM provider
     *
     * @param provider provider 标识（如 "deepseek", "anthropic"）
     * @param creator  创建函数
     */
    public static void register(String provider, Function<LLMConfig, LLMProvider> creator) {
        REGISTRY.put(provider.toLowerCase(), creator);
        log.debug("Registered LLM provider: {}", provider);
    }

    /**
     * 检查 provider 是否已注册
     */
    public static boolean isSupported(String provider) {
        return provider != null && REGISTRY.containsKey(provider.toLowerCase());
    }

    // ===================================================================
    // 快捷创建方法 - OpenAI
    // ===================================================================

    /**
     * 创建 OpenAI LLM（使用环境变量 OPENAI_API_KEY）
     */
    public static LLMProvider openai() {
        return openai(getEnvOrThrow("OPENAI_API_KEY"), "gpt-4o-mini");
    }

    /**
     * 创建 OpenAI LLM
     */
    public static LLMProvider openai(String apiKey) {
        return openai(apiKey, "gpt-4o-mini");
    }

    /**
     * 创建 OpenAI LLM
     */
    public static LLMProvider openai(String apiKey, String model) {
        OpenAILLMConfig config = OpenAILLMConfig.builder()
                .apiKey(apiKey)
                .model(model)
                .build();
        return new OpenAILLM(config);
    }

    // ===================================================================
    // 快捷创建方法 - 阿里云通义千问
    // ===================================================================

    /**
     * 创建阿里云 LLM（使用环境变量 DASHSCOPE_API_KEY）
     */
    public static LLMProvider aliyun() {
        return aliyun(getEnvOrThrow("DASHSCOPE_API_KEY"), "qwen-turbo");
    }

    /**
     * 创建阿里云 LLM
     */
    public static LLMProvider aliyun(String apiKey) {
        return aliyun(apiKey, "qwen-turbo");
    }

    /**
     * 创建阿里云 LLM
     */
    public static LLMProvider aliyun(String apiKey, String model) {
        AliyunLLMConfig config = AliyunLLMConfig.builder()
                .aliyunApiKey(apiKey)
                .model(model)
                .build();
        return new AliyunLLM(config);
    }

    // ===================================================================
    // 快捷创建方法 - Ollama
    // ===================================================================

    /**
     * 创建 Ollama LLM（本地默认地址 localhost:11434）
     */
    public static LLMProvider ollama(String model) {
        return ollama(model, OllamaLLMConfig.DEFAULT_BASE_URL);
    }

    /**
     * 创建 Ollama LLM
     */
    public static LLMProvider ollama(String model, String baseUrl) {
        OllamaLLMConfig config = OllamaLLMConfig.builder()
                .model(model)
                .baseUrl(baseUrl)
                .build();
        return new OllamaLLM(config);
    }

    // ===================================================================
    // 快捷创建方法 - SiliconFlow
    // ===================================================================

    /**
     * 创建 SiliconFlow LLM（使用环境变量 SILICONFLOW_API_KEY）
     */
    public static LLMProvider siliconflow() {
        return siliconflow(getEnvOrThrow("SILICONFLOW_API_KEY"), "Qwen/Qwen2.5-7B-Instruct");
    }

    /**
     * 创建 SiliconFlow LLM
     */
    public static LLMProvider siliconflow(String apiKey) {
        return siliconflow(apiKey, "Qwen/Qwen2.5-7B-Instruct");
    }

    /**
     * 创建 SiliconFlow LLM
     */
    public static LLMProvider siliconflow(String apiKey, String model) {
        SiliconFlowConfig config = SiliconFlowConfig.builder()
                .siliconflowKey(apiKey)
                .model(model)
                .build();
        return new SiliconFlowLLM(config);
    }

    // ===================================================================
    // 快捷创建方法 - LiteLLM（多模型代理）
    // ===================================================================

    /**
     * 创建 LiteLLM（本地代理）
     */
    public static LLMProvider litellm(String model) {
        return litellm(model, "http://localhost:4000");
    }

    /**
     * 创建 LiteLLM
     */
    public static LLMProvider litellm(String model, String baseUrl) {
        LiteLLMConfig config = LiteLLMConfig.builder()
                .model(model)
                .litellmBaseUrl(baseUrl)
                .build();
        return new LiteLLM(config);
    }

    // ===================================================================
    // 内部辅助方法
    // ===================================================================

    /**
     * 根据配置类型推断 provider
     */
    private static String inferProvider(LLMConfig config) {
        if (config instanceof OpenAILLMConfig) return "openai";
        if (config instanceof OllamaLLMConfig) return "ollama";
        if (config instanceof LiteLLMConfig) return "litellm";
        if (config instanceof AliyunLLMConfig) return "aliyun";
        if (config instanceof SiliconFlowConfig) return "siliconflow";

        throw new IllegalArgumentException(
                "Cannot infer provider from config type: " + config.getClass().getSimpleName() +
                ". Please set the 'provider' field explicitly.");
    }

    /**
     * 从环境变量获取值，不存在则抛出友好异常
     */
    private static String getEnvOrThrow(String envKey) {
        String value = System.getenv(envKey);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException(
                    "Environment variable '" + envKey + "' is not set. " +
                    "Please set it or pass the API key directly.");
        }
        return value;
    }
}
