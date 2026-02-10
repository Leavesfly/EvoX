package io.leavesfly.evox.rag.chunker;

/**
 * 分块策略枚举
 *
 * @author EvoX Team
 */
public enum ChunkingStrategy {
    
    /**
     * 固定大小分块
     */
    FIXED_SIZE("fixed_size"),
    
    /**
     * 语义分块(基于句子边界)
     */
    SEMANTIC("semantic"),
    
    /**
     * 层次分块
     */
    HIERARCHICAL("hierarchical");

    private final String value;

    ChunkingStrategy(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * 根据字符串值获取策略
     */
    public static ChunkingStrategy fromValue(String value) {
        for (ChunkingStrategy strategy : values()) {
            if (strategy.value.equalsIgnoreCase(value)) {
                return strategy;
            }
        }
        throw new IllegalArgumentException("Unknown chunking strategy: " + value);
    }
}
