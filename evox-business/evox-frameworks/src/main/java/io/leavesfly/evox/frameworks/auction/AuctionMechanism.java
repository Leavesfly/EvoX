package io.leavesfly.evox.frameworks.auction;

/**
 * 拍卖机制枚举
 *
 * @author EvoX Team
 */
public enum AuctionMechanism {
    
    /**
     * 英式拍卖(递增价格,公开竞价)
     */
    ENGLISH,
    
    /**
     * 荷兰式拍卖(递减价格,首个接受者获胜)
     */
    DUTCH,
    
    /**
     * 第一价格密封拍卖(密封出价,最高价者获胜并支付其出价)
     */
    FIRST_PRICE_SEALED,
    
    /**
     * 第二价格密封拍卖(密封出价,最高价者获胜但支付第二高价)
     */
    SECOND_PRICE_SEALED,
    
    /**
     * Vickrey拍卖(第二价格密封拍卖的别名)
     */
    VICKREY,
    
    /**
     * 全付拍卖(所有人支付出价,只有最高者获得物品)
     */
    ALL_PAY
}
