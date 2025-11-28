package io.leavesfly.evox.rag.embedding;

import java.util.List;

/**
 * 嵌入服务接口
 *
 * @author EvoX Team
 */
public interface EmbeddingService {

    /**
     * 生成文本嵌入向量
     *
     * @param text 输入文本
     * @return 嵌入向量
     */
    List<Float> embed(String text);

    /**
     * 批量生成嵌入向量
     *
     * @param texts 文本列表
     * @return 嵌入向量列表
     */
    List<List<Float>> embedBatch(List<String> texts);

    /**
     * 获取嵌入维度
     *
     * @return 维度数
     */
    int getDimensions();
}
