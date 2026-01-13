package io.leavesfly.evox.frameworks.auction;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 拍卖框架
 * 支持多种拍卖机制的通用框架
 *
 * @param <T> 拍卖物品类型
 * @author EvoX Team
 */
@Slf4j
@Data
public class AuctionFramework<T> {

    /**
     * 拍卖物品
     */
    private T item;

    /**
     * 拍卖机制
     */
    private AuctionMechanism auctionMechanism;

    /**
     * 参与竞价的智能体
     */
    private List<Bidder<T>> bidders;

    /**
     * 拍卖配置
     */
    private AuctionConfig config;

    /**
     * 拍卖历史
     */
    private List<BidRecord<T>> bidHistory;

    /**
     * 拍卖状态
     */
    private AuctionStatus status;

    /**
     * 当前轮次
     */
    private int currentRound;

    public AuctionFramework(T item, AuctionMechanism mechanism, List<Bidder<T>> bidders, AuctionConfig config) {
        this.item = item;
        this.auctionMechanism = mechanism;
        this.bidders = bidders;
        this.config = config;
        this.bidHistory = new ArrayList<>();
        this.status = AuctionStatus.PENDING;
        this.currentRound = 0;
    }

    public AuctionFramework(T item, AuctionMechanism mechanism, List<Bidder<T>> bidders) {
        this(item, mechanism, bidders, AuctionConfig.builder().build());
    }

    /**
     * 开始拍卖
     *
     * @return 拍卖结果
     */
    public AuctionResult<T> startAuction() {
        log.info("Starting auction for item: {} with mechanism: {}", item, auctionMechanism);
        
        long startTime = System.currentTimeMillis();
        status = AuctionStatus.RUNNING;
        
        try {
            // 根据拍卖机制执行不同的拍卖流程
            AuctionResult<T> result = switch (auctionMechanism) {
                case ENGLISH -> executeEnglishAuction();
                case DUTCH -> executeDutchAuction();
                case FIRST_PRICE_SEALED -> executeFirstPriceSealedAuction();
                case SECOND_PRICE_SEALED -> executeSecondPriceSealedAuction();
                case VICKREY -> executeVickreyAuction();
                case ALL_PAY -> executeAllPayAuction();
            };
            
            status = AuctionStatus.COMPLETED;
            result.setDuration(System.currentTimeMillis() - startTime);
            result.setTotalRounds(currentRound);
            
            log.info("Auction completed. Winner: {}, Price: {}", 
                result.getWinner() != null ? result.getWinner().getBidderId() : "None", 
                result.getFinalPrice());
            
            return result;
            
        } catch (Exception e) {
            status = AuctionStatus.FAILED;
            log.error("Auction failed: {}", e.getMessage(), e);
            
            return AuctionResult.<T>builder()
                .success(false)
                .error(e.getMessage())
                .duration(System.currentTimeMillis() - startTime)
                .build();
        }
    }

    /**
     * 英式拍卖(递增价格)
     */
    private AuctionResult<T> executeEnglishAuction() {
        double currentPrice = config.getStartingPrice();
        Bidder<T> currentWinner = null;
        
        while (currentRound < config.getMaxRounds()) {
            currentRound++;
            log.debug("English auction round {}, current price: {}", currentRound, currentPrice);
            
            // 收集本轮出价
            List<Bid<T>> bids = collectBids(currentPrice);
            
            if (bids.isEmpty()) {
                // 无人出价,拍卖结束
                break;
            }
            
            // 找出最高出价
            Bid<T> highestBid = bids.stream()
                .max(Comparator.comparingDouble(Bid::getAmount))
                .orElse(null);
            
            if (highestBid != null && highestBid.getAmount() > currentPrice) {
                currentPrice = highestBid.getAmount();
                currentWinner = highestBid.getBidder();
                recordBid(highestBid);
            } else {
                // 无人提高出价,拍卖结束
                break;
            }
            
            // 检查是否达到保留价
            if (config.getReservePrice() > 0 && currentPrice >= config.getReservePrice()) {
                break;
            }
        }
        
        return buildResult(currentWinner, currentPrice, currentWinner != null);
    }

    /**
     * 荷兰式拍卖(递减价格)
     */
    private AuctionResult<T> executeDutchAuction() {
        double currentPrice = config.getStartingPrice();
        double priceDecrement = config.getPriceIncrement(); // 用作递减值
        
        while (currentRound < config.getMaxRounds() && currentPrice > config.getReservePrice()) {
            currentRound++;
            log.debug("Dutch auction round {}, current price: {}", currentRound, currentPrice);
            
            // 询问竞价者是否接受当前价格
            for (Bidder<T> bidder : bidders) {
                if (bidder.acceptPrice(item, currentPrice, bidHistory)) {
                    Bid<T> bid = new Bid<>(bidder, currentPrice, BidType.ACCEPT, currentRound);
                    recordBid(bid);
                    return buildResult(bidder, currentPrice, true);
                }
            }
            
            // 降价
            currentPrice -= priceDecrement;
        }
        
        return buildResult(null, 0, false);
    }

    /**
     * 第一价格密封拍卖
     */
    private AuctionResult<T> executeFirstPriceSealedAuction() {
        currentRound = 1;
        
        // 所有竞价者同时提交密封出价
        List<Bid<T>> sealedBids = collectSealedBids();
        
        if (sealedBids.isEmpty()) {
            return buildResult(null, 0, false);
        }
        
        // 找出最高出价
        Bid<T> winningBid = sealedBids.stream()
            .max(Comparator.comparingDouble(Bid::getAmount))
            .orElse(null);
        
        if (winningBid != null && winningBid.getAmount() >= config.getReservePrice()) {
            recordBid(winningBid);
            return buildResult(winningBid.getBidder(), winningBid.getAmount(), true);
        }
        
        return buildResult(null, 0, false);
    }

    /**
     * 第二价格密封拍卖
     */
    private AuctionResult<T> executeSecondPriceSealedAuction() {
        currentRound = 1;
        
        // 所有竞价者同时提交密封出价
        List<Bid<T>> sealedBids = collectSealedBids();
        
        if (sealedBids.size() < 2) {
            return buildResult(null, 0, false);
        }
        
        // 排序找出最高和第二高出价
        List<Bid<T>> sortedBids = sealedBids.stream()
            .sorted(Comparator.comparingDouble(Bid<T>::getAmount).reversed())
            .collect(Collectors.toList());
        
        Bid<T> winningBid = sortedBids.get(0);
        double secondPrice = sortedBids.get(1).getAmount();
        
        if (winningBid.getAmount() >= config.getReservePrice()) {
            recordBid(winningBid);
            // 第二价格拍卖:赢家支付第二高价
            return buildResult(winningBid.getBidder(), secondPrice, true);
        }
        
        return buildResult(null, 0, false);
    }

    /**
     * Vickrey拍卖(第二价格密封拍卖的另一种实现)
     */
    private AuctionResult<T> executeVickreyAuction() {
        return executeSecondPriceSealedAuction();
    }

    /**
     * 全付拍卖(所有人都支付出价,只有最高者获得物品)
     */
    private AuctionResult<T> executeAllPayAuction() {
        currentRound = 1;
        
        List<Bid<T>> sealedBids = collectSealedBids();
        
        if (sealedBids.isEmpty()) {
            return buildResult(null, 0, false);
        }
        
        // 记录所有出价
        sealedBids.forEach(this::recordBid);
        
        // 找出最高出价者
        Bid<T> winningBid = sealedBids.stream()
            .max(Comparator.comparingDouble(Bid::getAmount))
            .orElse(null);
        
        if (winningBid != null && winningBid.getAmount() >= config.getReservePrice()) {
            AuctionResult<T> result = buildResult(winningBid.getBidder(), winningBid.getAmount(), true);
            // 添加所有支付信息
            Map<String, Double> allPayments = sealedBids.stream()
                .collect(Collectors.toMap(
                    bid -> bid.getBidder().getBidderId(),
                    Bid::getAmount
                ));
            result.addMetadata("allPayments", allPayments);
            return result;
        }
        
        return buildResult(null, 0, false);
    }

    /**
     * 收集本轮出价
     */
    private List<Bid<T>> collectBids(double currentPrice) {
        List<Bid<T>> bids = new ArrayList<>();
        
        for (Bidder<T> bidder : bidders) {
            double bidAmount = bidder.bid(item, currentPrice, bidHistory);
            if (bidAmount > currentPrice) {
                bids.add(new Bid<>(bidder, bidAmount, BidType.RAISE, currentRound));
            }
        }
        
        return bids;
    }

    /**
     * 收集密封出价
     */
    private List<Bid<T>> collectSealedBids() {
        List<Bid<T>> bids = new ArrayList<>();
        
        for (Bidder<T> bidder : bidders) {
            double bidAmount = bidder.sealedBid(item);
            if (bidAmount >= config.getReservePrice()) {
                bids.add(new Bid<>(bidder, bidAmount, BidType.SEALED, currentRound));
            }
        }
        
        return bids;
    }

    /**
     * 记录出价
     */
    private void recordBid(Bid<T> bid) {
        BidRecord<T> record = new BidRecord<>(
            bid,
            currentRound,
            System.currentTimeMillis()
        );
        bidHistory.add(record);
    }

    /**
     * 构建拍卖结果
     */
    private AuctionResult<T> buildResult(Bidder<T> winner, double finalPrice, boolean success) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("mechanism", auctionMechanism.name());
        metadata.put("totalBids", bidHistory.size());
        metadata.put("participantCount", bidders.size());
        
        return AuctionResult.<T>builder()
            .success(success)
            .item(item)
            .winner(winner)
            .finalPrice(finalPrice)
            .bidHistory(new ArrayList<>(bidHistory))
            .metadata(metadata)
            .build();
    }

    /**
     * 拍卖状态枚举
     */
    public enum AuctionStatus {
        PENDING,    // 等待开始
        RUNNING,    // 进行中
        COMPLETED,  // 已完成
        FAILED      // 失败
    }
}
