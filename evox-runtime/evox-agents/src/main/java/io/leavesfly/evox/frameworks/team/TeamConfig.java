package io.leavesfly.evox.frameworks.team;

import lombok.Builder;
import lombok.Data;

/**
 * 团队配置
 *
 * @author EvoX Team
 */
@Data
@Builder
public class TeamConfig {

    /**
     * 是否启用线程池
     */
    @Builder.Default
    private boolean enableThreadPool = false;

    /**
     * 最大线程数
     */
    @Builder.Default
    private int maxThreads = 10;

    /**
     * 任务超时时间(毫秒)
     */
    @Builder.Default
    private long taskTimeout = 300000; // 5分钟

    /**
     * 是否启用负载均衡
     */
    @Builder.Default
    private boolean enableLoadBalancing = false;

    /**
     * 聚合策略
     */
    private TeamFramework.AggregationStrategy<?> aggregationStrategy;

    /**
     * 选择策略
     */
    private TeamFramework.SelectionStrategy<?> selectionStrategy;

    /**
     * 是否记录详细历史
     */
    @Builder.Default
    private boolean recordDetailedHistory = true;
}
