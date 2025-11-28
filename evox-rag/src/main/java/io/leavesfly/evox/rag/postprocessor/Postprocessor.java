package io.leavesfly.evox.rag.postprocessor;

import io.leavesfly.evox.rag.schema.RetrievalResult;
import java.util.List;

/**
 * 后处理器基类
 * 用于对检索结果进行后处理
 *
 * @author EvoX Team
 */
public interface Postprocessor {
    
    /**
     * 处理检索结果
     *
     * @param results 原始检索结果
     * @return 处理后的结果
     */
    List<RetrievalResult> process(List<RetrievalResult> results);
}
