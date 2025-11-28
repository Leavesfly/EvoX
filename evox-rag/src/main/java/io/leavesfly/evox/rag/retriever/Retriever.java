package io.leavesfly.evox.rag.retriever;

import io.leavesfly.evox.rag.schema.Query;
import io.leavesfly.evox.rag.schema.RetrievalResult;

/**
 * 检索器接口
 *
 * @author EvoX Team
 */
public interface Retriever {

    /**
     * 执行检索
     *
     * @param query 查询对象
     * @return 检索结果
     */
    RetrievalResult retrieve(Query query);

    /**
     * 获取检索器类型
     *
     * @return 类型名称
     */
    String getType();
}
