package io.leavesfly.evox.rag.reranker;

import io.leavesfly.evox.rag.schema.Chunk;
import io.leavesfly.evox.rag.schema.Query;

import java.util.List;

/**
 * 重排序器接口 - 对检索结果进行二次排序
 *
 * @author EvoX Team
 */
public interface Reranker {

    /**
     * 对检索到的分块列表进行重排序
     *
     * @param query 查询对象
     * @param chunks 待重排序的分块列表
     * @return 重排序后的分块列表
     */
    List<Chunk> rerank(Query query, List<Chunk> chunks);

    /**
     * 对检索到的分块列表进行重排序，并返回Top K
     *
     * @param query 查询对象
     * @param chunks 待重排序的分块列表
     * @param topK 返回的结果数量
     * @return 重排序后的Top K分块列表
     */
    default List<Chunk> rerankTopK(Query query, List<Chunk> chunks, int topK) {
        List<Chunk> reranked = rerank(query, chunks);
        if (reranked.size() <= topK) {
            return reranked;
        }
        return reranked.subList(0, topK);
    }

    /**
     * 获取重排序器名称
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }
}
