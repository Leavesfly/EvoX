package io.leavesfly.evox.frameworks.hierarchical;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 分层决策结果
 *
 * @param <T> 结果类型
 * @author EvoX Team
 */
@Data
@Builder
public class HierarchicalResult<T> {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 最终结果
     */
    private T result;

    /**
     * 执行的层级数
     */
    private int layers;

    /**
     * 耗时(毫秒)
     */
    private long duration;

    /**
     * 执行历史
     */
    private List<ExecutionRecord<T>> history;

    /**
     * 错误信息(如果失败)
     */
    private String error;

    /**
     * 元数据
     */
    private Map<String, Object> metadata;
}
