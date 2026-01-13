package io.leavesfly.evox.frameworks.hierarchical;

import lombok.Builder;
import lombok.Data;

/**
 * 分层决策配置
 *
 * @author EvoX Team
 */
@Data
@Builder
public class HierarchicalConfig {

    /**
     * 最大层级深度
     */
    @Builder.Default
    private int maxDepth = 10;

    /**
     * 是否允许跨层委派
     */
    @Builder.Default
    private boolean allowCrossLevelDelegation = false;

    /**
     * 是否启用并行执行
     */
    @Builder.Default
    private boolean enableParallelExecution = false;

    /**
     * 执行超时时间(毫秒)
     */
    @Builder.Default
    private long executionTimeout = 300000; // 5分钟

    /**
     * 聚合策略
     */
    private HierarchicalFramework.AggregationStrategy<?> aggregationStrategy;

    /**
     * 是否记录详细历史
     */
    @Builder.Default
    private boolean recordDetailedHistory = true;
}
