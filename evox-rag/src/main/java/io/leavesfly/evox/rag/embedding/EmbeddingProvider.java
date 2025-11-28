package io.leavesfly.evox.rag.embedding;

/**
 * 嵌入服务提供者枚举
 *
 * @author EvoX Team
 */
public enum EmbeddingProvider {
    
    /**
     * OpenAI嵌入服务
     */
    OPENAI("openai"),
    
    /**
     * Azure OpenAI嵌入服务
     */
    AZURE_OPENAI("azure_openai"),
    
    /**
     * HuggingFace本地嵌入模型
     */
    HUGGINGFACE("huggingface"),
    
    /**
     * Ollama本地嵌入服务
     */
    OLLAMA("ollama"),
    
    /**
     * Voyage AI嵌入服务
     */
    VOYAGE("voyage");

    private final String value;

    EmbeddingProvider(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * 根据字符串值获取Provider
     */
    public static EmbeddingProvider fromValue(String value) {
        for (EmbeddingProvider provider : values()) {
            if (provider.value.equalsIgnoreCase(value)) {
                return provider;
            }
        }
        throw new IllegalArgumentException("Unknown embedding provider: " + value);
    }
}
