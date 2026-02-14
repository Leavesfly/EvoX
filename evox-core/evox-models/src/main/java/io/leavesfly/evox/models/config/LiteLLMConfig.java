package io.leavesfly.evox.models.config;

import io.leavesfly.evox.core.llm.LLMConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * LiteLLM配置类
 * 支持通过LiteLLM统一接口调用多种大模型
 * 包括OpenAI、Anthropic、Google、Azure等
 *
 * @author EvoX Team
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class LiteLLMConfig extends LLMConfig {

    /**
     * LiteLLM API基础URL
     * 例如: http://localhost:4000 (本地部署的LiteLLM Proxy)
     */
    private String litellmBaseUrl;

    /**
     * 是否为本地模型 (例如Ollama)
     */
    @lombok.Builder.Default
    private Boolean isLocal = false;

    /**
     * OpenAI API Key
     */
    private String openaiKey;

    /**
     * Anthropic API Key
     */
    private String anthropicKey;

    /**
     * DeepSeek API Key
     */
    private String deepseekKey;

    /**
     * Google Gemini API Key
     */
    private String geminiKey;

    /**
     * Meta Llama API Key
     */
    private String metaLlamaKey;

    /**
     * OpenRouter API Key
     */
    private String openrouterKey;

    /**
     * OpenRouter Base URL
     */
    private String openrouterBase;

    /**
     * Perplexity API Key
     */
    private String perplexityKey;

    /**
     * Groq API Key
     */
    private String groqKey;

    /**
     * Azure OpenAI Endpoint
     */
    private String azureEndpoint;

    /**
     * Azure OpenAI API Key
     */
    private String azureKey;

    /**
     * Azure API Version
     */
    private String apiVersion;

    /**
     * 最大完成token数
     * 用于o1系列等模型
     */
    private Integer maxCompletionTokens;

    /**
     * 生成结果数量
     */
    private Integer n;

    /**
     * 流式输出选项
     */
    private Object streamOptions;

    /**
     * 工具列表 (Function Calling)
     */
    private List<Object> tools;

    /**
     * 工具选择策略
     */
    private String toolChoice;

    /**
     * 是否启用并行工具调用
     */
    private Boolean parallelToolCalls;

    /**
     * 是否返回token的对数概率
     */
    private Boolean logprobs;

    /**
     * 返回最可能的N个token及其对数概率
     */
    private Integer topLogprobs;

    /**
     * 响应格式
     * 支持JSON Schema等结构化输出
     */
    private Object responseFormat;

    /**
     * 默认构造函数
     */
    public LiteLLMConfig() {
        setProvider("litellm");
        setModel("gpt-4o-mini");
        if (this.isLocal == null) {
            this.isLocal = false;
        }
    }

    /**
     * 验证配置有效性
     *
     * @return true if configuration is valid
     */
    public boolean isValid() {
        if (isLocal != null && isLocal) {
            return litellmBaseUrl != null && !litellmBaseUrl.isEmpty();
        }
        return getApiKey() != null && !getApiKey().isEmpty();
    }

    /**
     * 获取有效的API Key
     * 根据模型类型自动选择对应的API Key
     *
     * @return API Key
     */
    public String getEffectiveApiKey() {
        String modelLower = getModel().toLowerCase();

        if (modelLower.startsWith("gpt") || modelLower.contains("openai")) {
            return openaiKey != null ? openaiKey : getApiKey();
        } else if (modelLower.contains("claude") || modelLower.contains("anthropic")) {
            return anthropicKey != null ? anthropicKey : getApiKey();
        } else if (modelLower.contains("deepseek")) {
            return deepseekKey != null ? deepseekKey : getApiKey();
        } else if (modelLower.contains("gemini")) {
            return geminiKey != null ? geminiKey : getApiKey();
        } else if (modelLower.contains("llama")) {
            return metaLlamaKey != null ? metaLlamaKey : getApiKey();
        } else if (modelLower.contains("openrouter")) {
            return openrouterKey != null ? openrouterKey : getApiKey();
        } else if (modelLower.contains("perplexity")) {
            return perplexityKey != null ? perplexityKey : getApiKey();
        } else if (modelLower.contains("groq")) {
            return groqKey != null ? groqKey : getApiKey();
        } else if (modelLower.contains("azure")) {
            return azureKey != null ? azureKey : getApiKey();
        }

        return getApiKey();
    }

    /**
     * 获取有效的Base URL
     *
     * @return Base URL
     */
    public String getEffectiveBaseUrl() {
        if (litellmBaseUrl != null) {
            return litellmBaseUrl;
        }
        if (openrouterBase != null && getModel().toLowerCase().contains("openrouter")) {
            return openrouterBase;
        }
        if (azureEndpoint != null && getModel().toLowerCase().contains("azure")) {
            return azureEndpoint;
        }
        return getBaseUrl();
    }
}
