package io.leavesfly.evox.rag.vectorstore;

import io.leavesfly.evox.rag.schema.Chunk;
import io.leavesfly.evox.rag.schema.Query;
import io.leavesfly.evox.rag.schema.RetrievalResult;

import java.util.List;

/**
 * 向量存储接口
 *
 * @author EvoX Team
 */
public interface VectorStore {

    /**
     * 添加分块到向量存储
     *
     * @param chunk 分块
     */
    void add(Chunk chunk);

    /**
     * 批量添加分块
     *
     * @param chunks 分块列表
     */
    void addBatch(List<Chunk> chunks);

    /**
     * 执行向量检索
     *
     * @param query 查询对象
     * @return 检索结果
     */
    RetrievalResult search(Query query);

    /**
     * 删除分块
     *
     * @param chunkId 分块ID
     * @return 是否成功
     */
    boolean delete(String chunkId);

    /**
     * 清空向量存储
     */
    void clear();

    /**
     * 获取存储的分块数量
     *
     * @return 数量
     */
    int size();

    /**
     * 保存向量存储到磁盘
     *
     * @param path 保存路径
     */
    void save(String path);

    /**
     * 从磁盘加载向量存储
     *
     * @param path 加载路径
     */
    void load(String path);
}
