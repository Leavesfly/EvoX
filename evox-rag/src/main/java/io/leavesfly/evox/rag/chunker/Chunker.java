package io.leavesfly.evox.rag.chunker;

import io.leavesfly.evox.rag.schema.Chunk;
import io.leavesfly.evox.rag.schema.Document;

import java.util.List;

/**
 * 分块器接口
 *
 * @author EvoX Team
 */
public interface Chunker {

    /**
     * 对文档进行分块
     *
     * @param document 输入文档
     * @return 分块列表
     */
    List<Chunk> chunk(Document document);

    /**
     * 批量对文档进行分块
     *
     * @param documents 文档列表
     * @return 分块列表
     */
    default List<Chunk> chunkBatch(List<Document> documents) {
        return documents.stream()
                .flatMap(doc -> chunk(doc).stream())
                .toList();
    }
}
