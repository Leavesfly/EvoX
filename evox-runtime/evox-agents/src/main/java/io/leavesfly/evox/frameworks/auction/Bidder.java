package io.leavesfly.evox.frameworks.auction;

import java.util.List;

/**
 * 竞价者接口
 *
 * @param <T> 拍卖物品类型
 * @author EvoX Team
 */
public interface Bidder<T> {

    /**
     * 获取竞价者ID
     */
    String getBidderId();

    /**
     * 获取竞价者名称
     */
    String getBidderName();

    /**
     * 在公开拍卖中出价
     *
     * @param item 拍卖物品
     * @param currentPrice 当前价格
     * @param bidHistory 历史出价记录
     * @return 出价金额(如果不出价返回0或当前价格)
     */
    double bid(T item, double currentPrice, List<BidRecord<T>> bidHistory);

    /**
     * 在密封拍卖中提交出价
     *
     * @param item 拍卖物品
     * @return 出价金额
     */
    double sealedBid(T item);

    /**
     * 在荷兰式拍卖中决定是否接受当前价格
     *
     * @param item 拍卖物品
     * @param currentPrice 当前价格
     * @param bidHistory 历史记录
     * @return 是否接受
     */
    default boolean acceptPrice(T item, double currentPrice, List<BidRecord<T>> bidHistory) {
        return false;
    }

    /**
     * 获取竞价者的估值
     *
     * @param item 拍卖物品
     * @return 估值
     */
    default double getValuation(T item) {
        return 0.0;
    }

    /**
     * 获取竞价者的预算
     */
    default double getBudget() {
        return Double.MAX_VALUE;
    }
}
