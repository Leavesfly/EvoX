package io.leavesfly.evox.frameworks.consensus;

import lombok.Builder;
import lombok.Data;

/**
 * 共识配置
 *
 * @author EvoX Team
 */
@Data
@Builder
public class ConsensusConfig {

    /**
     * 最大轮数
     */
    @Builder.Default
    private int maxRounds = 10;

    /**
     * 是否忽略失败的提议
     */
    @Builder.Default
    private boolean ignoreFailedProposals = true;

    /**
     * 是否启用智能体反馈
     */
    @Builder.Default
    private boolean enableAgentFeedback = true;

    /**
     * 是否启用早停
     */
    @Builder.Default
    private boolean enableEarlyStopping = true;

    /**
     * 早停耐心值(连续多少轮没有改进则停止)
     */
    @Builder.Default
    private int earlyStoppingPatience = 3;

    /**
     * 早停阈值(置信度改进小于此值视为没有改进)
     */
    @Builder.Default
    private double earlyStoppingThreshold = 0.01;

    /**
     * 共识阈值(置信度高于此值视为达成共识)
     */
    @Builder.Default
    private double consensusThreshold = 0.8;

    /**
     * 最小支持率(支持该结果的智能体比例)
     */
    @Builder.Default
    private double minSupportRate = 0.5;
}
