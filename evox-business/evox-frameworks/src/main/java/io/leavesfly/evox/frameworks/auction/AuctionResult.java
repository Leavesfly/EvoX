package io.leavesfly.evox.frameworks.auction;

import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 拍卖结果
 *
 * @param <T> 拍卖物品类型
 * @author EvoX Team
 */
@Data
@Builder
public class AuctionResult<T> {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 拍卖物品
     */
    private T item;

    /**
     * 获胜者
     */
    private Bidder<T> winner;

    /**
     * 最终成交价
     */
    private double finalPrice;

    /**
     * 总轮数
     */
    private int totalRounds;

    /**
     * 耗时(毫秒)
     */
    private long duration;

    /**
     * 竞价历史
     */
    private List<BidRecord<T>> bidHistory;

    /**
     * 错误信息(如果失败)
     */
    private String error;

    /**
     * 元数据
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * 添加元数据
     */
    public void addMetadata(String key, Object value) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put(key, value);
    }
}
