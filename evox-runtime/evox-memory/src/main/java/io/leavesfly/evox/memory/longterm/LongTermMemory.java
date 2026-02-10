package io.leavesfly.evox.memory.longterm;

import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.memory.base.Memory;
import io.leavesfly.evox.storage.vector.VectorStore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 长期记忆实现
 * 使用向量存储持久化保存历史消息
 * 
 * @author EvoX Team
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class LongTermMemory extends Memory {

    /**
     * 向量存储
     */
    private VectorStore vectorStore;

    /**
     * 嵌入服务（用于将消息转换为向量）
     */
    private EmbeddingService embeddingService;

    /**
     * 是否已初始化
     */
    private boolean initialized = false;

    public LongTermMemory(VectorStore vectorStore, EmbeddingService embeddingService) {
        this.vectorStore = vectorStore;
        this.embeddingService = embeddingService;
    }

    @Override
    public void initModule() {
        super.initModule();
        if (vectorStore != null && !vectorStore.isInitialized()) {
            vectorStore.initialize();
        }
        initialized = true;
    }

    /**
     * 存储消息到长期记忆
     */
    public void storeMessage(Message message) {
        if (!initialized) {
            initModule();
        }

        try {
            // 将消息内容转换为向量
            String content = message.getContent() != null ? 
                           message.getContent().toString() : "";
            float[] embedding = embeddingService.embed(content);

            // 构建元数据
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("message_id", message.getMessageId());
            metadata.put("agent", message.getAgent());
            metadata.put("action", message.getAction());
            metadata.put("timestamp", message.getTimestamp().toString());
            metadata.put("message_type", message.getMessageType().toString());
            
            // 存储到向量数据库
            vectorStore.addVector(message.getMessageId(), embedding, metadata);
            
            log.debug("Stored message to long-term memory: {}", message.getMessageId());
        } catch (Exception e) {
            log.error("Failed to store message to long-term memory", e);
        }
    }

    /**
     * 批量存储消息
     */
    public void storeMessages(List<Message> messages) {
        for (Message message : messages) {
            storeMessage(message);
        }
    }

    /**
     * 搜索相似消息
     */
    public List<Message> searchSimilar(String query, int topK) {
        if (!initialized) {
            initModule();
        }

        try {
            // 将查询转换为向量
            float[] queryEmbedding = embeddingService.embed(query);

            // 搜索相似向量
            List<VectorStore.SearchResult> results = vectorStore.search(queryEmbedding, topK);

            // 转换为消息对象
            List<Message> messages = new ArrayList<>();
            for (VectorStore.SearchResult result : results) {
                Message message = reconstructMessage(result);
                if (message != null) {
                    messages.add(message);
                }
            }

            return messages;
        } catch (Exception e) {
            log.error("Failed to search similar messages", e);
            return Collections.emptyList();
        }
    }

    /**
     * 从搜索结果重构消息
     */
    private Message reconstructMessage(VectorStore.SearchResult result) {
        Map<String, Object> metadata = result.getMetadata();
        
        Message.MessageBuilder builder = Message.builder();
        builder.messageId((String) metadata.get("message_id"));
        builder.agent((String) metadata.get("agent"));
        builder.action((String) metadata.get("action"));
        
        return builder.build();
    }

    @Override
    public void clear() {
        if (vectorStore != null) {
            vectorStore.clear();
        }
        log.info("Long-term memory cleared");
    }

    /**
     * 嵌入服务接口
     */
    public interface EmbeddingService {
        /**
         * 将文本转换为向量
         */
        float[] embed(String text);
    }
}
