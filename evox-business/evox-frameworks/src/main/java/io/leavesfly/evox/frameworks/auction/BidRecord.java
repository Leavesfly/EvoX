package io.leavesfly.evox.frameworks.auction;

import lombok.Data;

/**
 * 竞价历史记录
 *
 * @param <T> 拍卖物品类型
 * @author EvoX Team
 */
@Data
public class BidRecord<T> {

    /**
     * 出价
     */
    private Bid<T> bid;

    /**
     * 轮次
     */
    private int round;

    /**
     * 时间戳
     */
    private long timestamp;

    public BidRecord(Bid<T> bid, int round, long timestamp) {
        this.bid = bid;
        this.round = round;
        this.timestamp = timestamp;
    }
}
