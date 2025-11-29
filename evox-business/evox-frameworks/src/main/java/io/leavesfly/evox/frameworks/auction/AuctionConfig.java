package io.leavesfly.evox.frameworks.auction;

import lombok.Builder;
import lombok.Data;

/**
 * 拍卖配置
 *
 * @author EvoX Team
 */
@Data
@Builder
public class AuctionConfig {

    /**
     * 起拍价
     */
    @Builder.Default
    private double startingPrice = 0.0;

    /**
     * 保留价(最低成交价)
     */
    @Builder.Default
    private double reservePrice = 0.0;

    /**
     * 价格增量(英式拍卖)或递减量(荷兰式拍卖)
     */
    @Builder.Default
    private double priceIncrement = 10.0;

    /**
     * 最大轮数
     */
    @Builder.Default
    private int maxRounds = 100;

    /**
     * 竞价超时时间(毫秒)
     */
    @Builder.Default
    private long bidTimeout = 30000;

    /**
     * 是否允许撤回出价
     */
    @Builder.Default
    private boolean allowBidRetraction = false;

    /**
     * 是否公开竞价历史
     */
    @Builder.Default
    private boolean publicBidHistory = true;
}
