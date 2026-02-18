package io.leavesfly.evox.models.config;

import io.leavesfly.evox.core.llm.LLMConfig;
import io.leavesfly.evox.models.provider.aliyun.AliyunLLM;
import io.leavesfly.evox.models.provider.aliyun.AliyunLLMConfig;
import io.leavesfly.evox.models.provider.deepseek.DeepSeekLLM;
import io.leavesfly.evox.models.provider.deepseek.DeepSeekLLMConfig;
import io.leavesfly.evox.models.provider.ollama.OllamaLLM;
import io.leavesfly.evox.models.provider.ollama.OllamaLLMConfig;
import io.leavesfly.evox.models.provider.openai.OpenAILLM;
import io.leavesfly.evox.models.provider.openai.OpenAILLMConfig;
import io.leavesfly.evox.models.provider.siliconflow.SiliconFlowLLM;
import io.leavesfly.evox.models.provider.siliconflow.SiliconFlowLLMConfig;
import io.leavesfly.evox.models.spi.LLMProvider;
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

    private static final Map<String, Function<LLMConfig, LLMProvider>> REGISTRY = new ConcurrentHashMap<>();

    static {
        register("openai", config -> new OpenAILLM((OpenAILLMConfig) config));
        register("ollama", config -> new OllamaLLM((OllamaLLMConfig) config));
        register("aliyun", config -> new AliyunLLM((AliyunLLMConfig) config));
        register("deepseek", config -> new DeepSeekLLM((DeepSeekLLMConfig) config));
        register("siliconflow", config -> new SiliconFlowLLM((SiliconFlowLLMConfig) config));
    }

    private LLMFactory() {
    }

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
     *
     * @param provider provider 标识
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

    public static LLMProvider openai() {
        return openai(getEnvOrThrow("OPENAI_API_KEY"), "gpt-4o-mini");
    }

    public static LLMProvider openai(String apiKey) {
        return openai(apiKey, "gpt-4o-mini");
    }

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

    public static LLMProvider aliyun() {
        return aliyun(getEnvOrThrow("DASHSCOPE_API_KEY"), "qwen-turbo");
    }

    public static LLMProvider aliyun(String apiKey) {
        return aliyun(apiKey, "qwen-turbo");
    }

    public static LLMProvider aliyun(String apiKey, String model) {
        AliyunLLMConfig config = AliyunLLMConfig.builder()
                .aliyunApiKey(apiKey)
                .model(model)
                .build();
        return new AliyunLLM(config);
    }

    // ===================================================================
    // 快捷创建方法 - DeepSeek
    // ===================================================================

    public static LLMProvider deepseek() {
        return deepseek(getEnvOrThrow("DEEPSEEK_API_KEY"), "deepseek-chat");
    }

    public static LLMProvider deepseek(String apiKey) {
        return deepseek(apiKey, "deepseek-chat");
    }

    public static LLMProvider deepseek(String apiKey, String model) {
        DeepSeekLLMConfig config = DeepSeekLLMConfig.builder()
                .apiKey(apiKey)
                .model(model)
                .build();
        return new DeepSeekLLM(config);
    }

    // ===================================================================
    // 快捷创建方法 - Ollama
    // ===================================================================

    public static LLMProvider ollama(String model) {
        return ollama(model, OllamaLLMConfig.DEFAULT_BASE_URL);
    }

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

    public static LLMProvider siliconflow() {
        return siliconflow(getEnvOrThrow("SILICONFLOW_API_KEY"), "Qwen/Qwen2.5-7B-Instruct");
    }

    public static LLMProvider siliconflow(String apiKey) {
        return siliconflow(apiKey, "Qwen/Qwen2.5-7B-Instruct");
    }

    public static LLMProvider siliconflow(String apiKey, String model) {
        SiliconFlowLLMConfig config = SiliconFlowLLMConfig.builder()
                .siliconflowKey(apiKey)
                .model(model)
                .build();
        return new SiliconFlowLLM(config);
    }

    // ===================================================================
    // 内部辅助方法
    // ===================================================================

    private static String inferProvider(LLMConfig config) {
        if (config instanceof OpenAILLMConfig) return "openai";
        if (config instanceof OllamaLLMConfig) return "ollama";
        if (config instanceof AliyunLLMConfig) return "aliyun";
        if (config instanceof DeepSeekLLMConfig) return "deepseek";
        if (config instanceof SiliconFlowLLMConfig) return "siliconflow";

        throw new IllegalArgumentException(
                "Cannot infer provider from config type: " + config.getClass().getSimpleName() +
                ". Please set the 'provider' field explicitly.");
    }

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
