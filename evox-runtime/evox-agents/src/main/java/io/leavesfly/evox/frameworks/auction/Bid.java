package io.leavesfly.evox.frameworks.auction;

import lombok.Data;

/**
 * 出价记录
 *
 * @param <T> 拍卖物品类型
 * @author EvoX Team
 */
@Data
public class Bid<T> {

    /**
     * 竞价者
     */
    private Bidder<T> bidder;

    /**
     * 出价金额
     */
    private double amount;

    /**
     * 出价类型
     */
    private BidType type;

    /**
     * 轮次
     */
    private int round;

    /**
     * 时间戳
     */
    private long timestamp;

    public Bid(Bidder<T> bidder, double amount, BidType type, int round) {
        this.bidder = bidder;
        this.amount = amount;
        this.type = type;
        this.round = round;
        this.timestamp = System.currentTimeMillis();
    }
}
