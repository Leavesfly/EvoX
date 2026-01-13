package io.leavesfly.evox.frameworks.auction;

/**
 * 出价类型
 *
 * @author EvoX Team
 */
public enum BidType {
    /**
     * 加价
     */
    RAISE,
    
    /**
     * 接受价格
     */
    ACCEPT,
    
    /**
     * 密封出价
     */
    SEALED,
    
    /**
     * 放弃
     */
    PASS
}
