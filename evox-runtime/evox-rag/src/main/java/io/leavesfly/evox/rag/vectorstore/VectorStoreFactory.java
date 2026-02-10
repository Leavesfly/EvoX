package io.leavesfly.evox.rag.vectorstore;

import lombok.extern.slf4j.Slf4j;

/**
 * 向量存储工厂
 *
 * @author EvoX Team
 */
@Slf4j
public class VectorStoreFactory {

    /**
     * 向量存储类型
     */
    public enum VectorStoreType {
        IN_MEMORY,
        PERSISTENT,
        FAISS,
        QDRANT
    }

    /**
     * 创建向量存储实例
     *
     * @param type 存储类型
     * @param dimensions 向量维度
     * @return DocumentVectorStore 实例
     */
    public static DocumentVectorStore create(VectorStoreType type, int dimensions) {
        return switch (type) {
            case IN_MEMORY -> {
                log.info("Creating InMemoryVectorStore");
                yield new InMemoryVectorStore();
            }
            case PERSISTENT -> {
                log.info("Creating PersistentVectorStore with dimensions: {}", dimensions);
                yield new PersistentVectorStore(dimensions);
            }
            case FAISS -> throw new UnsupportedOperationException(
                    "FAISS vector store not yet implemented. Use PERSISTENT for now.");
            case QDRANT -> throw new UnsupportedOperationException(
                    "Qdrant vector store not yet implemented. Use PERSISTENT for now.");
        };
    }

    /**
     * 创建默认向量存储(InMemory)
     */
    public static DocumentVectorStore createDefault() {
        return create(VectorStoreType.IN_MEMORY, 1536);
    }

    /**
     * 创建持久化向量存储
     */
    public static DocumentVectorStore createPersistent(int dimensions) {
        return create(VectorStoreType.PERSISTENT, dimensions);
    }
}
