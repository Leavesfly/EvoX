package io.leavesfly.evox.frameworks.auction;

import io.leavesfly.evox.workflow.base.WorkflowNode;
import io.leavesfly.evox.workflow.execution.WorkflowContext;
import io.leavesfly.evox.workflow.node.NodeHandler;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 拍卖节点处理器
 * 
 * 提供两个核心处理器，基于 Workflow DAG 引擎执行完整的拍卖流程：
 * - AuctionRoundHandler：执行单轮拍卖操作，包括收集出价、评估出价、更新拍卖状态
 * - AuctionResultHandler：在拍卖循环结束后确定最终赢家并构建拍卖结果
 * 
 * 支持多种拍卖机制：英式拍卖、荷兰式拍卖、第一价格密封拍卖、第二价格密封拍卖、Vickrey 拍卖、全付拍卖
 *
 * @param <T> 拍卖物品类型
 * @author EvoX Team
 * @see AuctionMechanism
 * @see Bidder
 * @see AuctionConfig
 */
@Slf4j
public class AuctionNodeHandler {

    // ==================== WorkflowContext 上下文键常量 ====================
    
    /** 拍卖物品上下文键：存储待拍卖的物品对象 */
    public static final String CONTEXT_KEY_ITEM = "item";
    
    /** 拍卖机制上下文键：存储拍卖类型（英式、荷兰式等） */
    public static final String CONTEXT_KEY_MECHANISM = "mechanism";
    
    /** 竞价者列表上下文键：存储所有参与竞拍的竞价者 */
    public static final String CONTEXT_KEY_BIDDERS = "bidders";
    
    /** 拍卖配置上下文键：存储拍卖规则配置（起拍价、保留价、最大轮次等） */
    public static final String CONTEXT_KEY_CONFIG = "config";
    
    /** 拍卖结果上下文键：存储最终的拍卖结果 */
    public static final String CONTEXT_KEY_RESULT = "result";
    
    /** 当前轮次上下文键：存储当前是第几轮拍卖 */
    public static final String CONTEXT_KEY_CURRENT_ROUND = "current_round";
    
    /** 当前价格上下文键：存储当前的竞拍价格 */
    public static final String CONTEXT_KEY_CURRENT_PRICE = "current_price";
    
    /** 当前领先者上下文键：存储当前最高出价者 */
    public static final String CONTEXT_KEY_CURRENT_WINNER = "current_winner";
    
    /** 出价历史上下文键：存储所有轮次的出价记录 */
    public static final String CONTEXT_KEY_BID_HISTORY = "bid_history";
    
    /** 拍卖是否完成上下文键：标识拍卖是否已结束 */
    public static final String CONTEXT_KEY_AUCTION_FINISHED = "auction_finished";
    
    /** 拍卖开始时间上下文键：存储拍卖启动的时间戳 */
    public static final String CONTEXT_KEY_START_TIME = "start_time";
    
    /** 拍卖状态上下文键：存储拍卖当前状态（运行中、已完成等） */
    public static final String CONTEXT_KEY_STATUS = "status";
    
    // ==================== 元数据键常量 ====================
    
    /** 元数据键：拍卖机制名称 */
    public static final String METADATA_KEY_MECHANISM = "mechanism";
    
    /** 元数据键：总出价次数 */
    public static final String METADATA_KEY_TOTAL_BIDS = "total_bids";
    
    /** 元数据键：参与竞拍的人数 */
    public static final String METADATA_KEY_PARTICIPANT_COUNT = "participant_count";
    
    /** 元数据键：全付拍卖中所有竞拍者的支付金额列表 */
    public static final String METADATA_KEY_ALL_PAYMENTS = "all_payments";

    // ==================== 拍卖轮次处理器 ====================

    /**
     * 拍卖轮次处理器
     * 
     * 执行单轮拍卖的完整流程：
     * 1. 从 WorkflowContext 读取当前拍卖状态（轮次、价格、领先者等）
     * 2. 根据拍卖机制收集本轮出价（公开出价或密封出价）
     * 3. 评估出价并更新拍卖状态（更新价格、领先者、历史记录）
     * 4. 判断拍卖是否完成（达到保留价、无人出价、达到最大轮次等）
     * 5. 将更新后的状态写入 WorkflowContext
     * 
     * 支持的拍卖机制：
     * - ENGLISH：英式拍卖，公开加价，价高者得
     * - DUTCH：荷兰式拍卖，价格递减，首个接受者获胜
     * - FIRST_PRICE_SEALED：第一价格密封拍卖，最高出价者获胜并支付其出价
     * - SECOND_PRICE_SEALED：第二价格密封拍卖，最高出价者获胜但支付第二高出价
     * - VICKREY：Vickrey 拍卖，等同于第二价格密封拍卖
     * - ALL_PAY：全付拍卖，所有竞拍者都需支付其出价，最高者获得物品
     *
     * @param <T> 拍卖物品类型
     */
    @Data
    public static class AuctionRoundHandler<T> implements NodeHandler {
        
        /** 结果回调函数，用于在拍卖完成后通知调用方 */
        private Consumer<AuctionResult<T>> resultCallback;
        
        /**
         * 获取处理器名称
         * 
         * @return 处理器标识符 "auction_round"
         */
        @Override
        public String getHandlerName() {
            return "auction_round";
        }
        
        /**
         * 处理拍卖轮次节点
         * 
         * 执行完整的单轮拍卖流程：
         * - 读取拍卖配置和状态数据
         * - 根据拍卖机制执行相应的出价逻辑
         * - 评估出价并更新状态
         * - 判断拍卖是否结束
         * - 将结果写入上下文
         * 
         * @param context 工作流上下文，包含所有执行数据
         * @param node 当前工作流节点
         * @return 处理结果描述字符串
         */
        @Override
        public Mono<Object> handle(WorkflowContext context, WorkflowNode node) {
            return Mono.fromCallable(() -> {
                try {
                    // ==================== 读取拍卖配置 ====================
                            
                    T item = (T) context.getExecutionData(CONTEXT_KEY_ITEM);
                    AuctionMechanism mechanism = (AuctionMechanism) context.getExecutionData(CONTEXT_KEY_MECHANISM);
                    List<Bidder<T>> bidders = (List<Bidder<T>>) context.getExecutionData(CONTEXT_KEY_BIDDERS);
                    AuctionConfig config = (AuctionConfig) context.getExecutionData(CONTEXT_KEY_CONFIG);
                            
                    // 如果未配置则使用默认配置
                    if (config == null) {
                        config = AuctionConfig.builder().build();
                    }
                            
                    // ==================== 读取当前状态 ====================
                            
                    // 当前轮次（从 0 开始）
                    int currentRound = getOrDefault(context, CONTEXT_KEY_CURRENT_ROUND, 0);
                            
                    // 当前价格（默认为起拍价）
                    double currentPrice = getOrDefault(context, CONTEXT_KEY_CURRENT_PRICE, config.getStartingPrice());
                            
                    // 当前领先者
                    Bidder<T> currentWinner = (Bidder<T>) context.getExecutionData(CONTEXT_KEY_CURRENT_WINNER);
                            
                    // 出价历史记录
                    List<BidRecord<T>> bidHistory = getOrDefault(context, CONTEXT_KEY_BID_HISTORY, new ArrayList<>());
                            
                    // ==================== 执行拍卖流程 ====================
                            
                    // 更新状态为运行中
                    context.updateExecutionData(CONTEXT_KEY_STATUS, AuctionFramework.AuctionStatus.RUNNING);
                            
                    // 执行本轮拍卖
                    RoundResult<T> roundResult = executeRound(
                        mechanism, item, bidders, config, 
                        currentRound, currentPrice, currentWinner, bidHistory
                    );
                            
                    // ==================== 更新上下文状态 ====================
                            
                    context.updateExecutionData(CONTEXT_KEY_CURRENT_ROUND, roundResult.currentRound);
                    context.updateExecutionData(CONTEXT_KEY_CURRENT_PRICE, roundResult.currentPrice);
                    context.updateExecutionData(CONTEXT_KEY_CURRENT_WINNER, roundResult.currentWinner);
                    context.updateExecutionData(CONTEXT_KEY_BID_HISTORY, roundResult.bidHistory);
                    context.updateExecutionData(CONTEXT_KEY_AUCTION_FINISHED, roundResult.finished);
                            
                    log.debug("[AuctionRoundHandler] 第 {} 轮完成，拍卖完成：{}", 
                        roundResult.currentRound, roundResult.finished);
                            
                    return "Round " + roundResult.currentRound + " completed";
                            
                } catch (Exception e) {
                    log.error("[AuctionRoundHandler] 拍卖轮次执行失败", e);
                    throw e;
                }
            });
        }
        
        /**
         * 从上下文获取数据，如果不存在则返回默认值
         * 
         * @param context 工作流上下文
         * @param key 数据键
         * @param defaultValue 默认值
         * @param <V> 数据类型
         * @return 上下文中的数据或默认值
         */
        @SuppressWarnings("unchecked")
        private <V> V getOrDefault(WorkflowContext context, String key, V defaultValue) {
            Object value = context.getExecutionData(key);
            return value != null ? (V) value : defaultValue;
        }
        
        /**
         * 执行单轮拍卖
         * 
         * 根据指定的拍卖机制分发到相应的处理方法。
         * 
         * @param mechanism 拍卖机制类型
         * @param item 拍卖物品
         * @param bidders 竞价者列表
         * @param config 拍卖配置
         * @param currentRound 当前轮次
         * @param currentPrice 当前价格
         * @param currentWinner 当前领先者
         * @param bidHistory 出价历史
         * @return 本轮拍卖结果
         */
        private RoundResult<T> executeRound(
                AuctionMechanism mechanism,
                T item,
                List<Bidder<T>> bidders,
                AuctionConfig config,
                int currentRound,
                double currentPrice,
                Bidder<T> currentWinner,
                List<BidRecord<T>> bidHistory) {
            
            // 计算新一轮次序号
            int newRound = currentRound + 1;
            
            // 根据拍卖机制分发到相应的处理方法
            return switch (mechanism) {
                case ENGLISH -> executeEnglishRound(
                    item, bidders, config, newRound, currentPrice, currentWinner, bidHistory
                );
                case DUTCH -> executeDutchRound(
                    item, bidders, config, newRound, currentPrice, bidHistory
                );
                case FIRST_PRICE_SEALED -> executeFirstPriceSealedRound(
                    item, bidders, config, newRound, bidHistory
                );
                case SECOND_PRICE_SEALED -> executeSecondPriceSealedRound(
                    item, bidders, config, newRound, bidHistory
                );
                case VICKREY -> executeVickreyRound(
                    item, bidders, config, newRound, bidHistory
                );
                case ALL_PAY -> executeAllPayRound(
                    item, bidders, config, newRound, bidHistory
                );
            };
        }
        
        /**
         * 执行英式拍卖单轮
         */
        private RoundResult<T> executeEnglishRound(
                T item,
                List<Bidder<T>> bidders,
                AuctionConfig config,
                int round,
                double currentPrice,
                Bidder<T> currentWinner,
                List<BidRecord<T>> bidHistory) {
            
            log.debug("[AuctionRoundHandler] 英式拍卖第 {} 轮，当前价格: {}", round, currentPrice);
            
            // 收集本轮出价
            List<Bid<T>> bids = collectBids(item, bidders, currentPrice, bidHistory, round);
            
            if (bids.isEmpty()) {
                log.debug("[AuctionRoundHandler] 无人出价，拍卖结束");
                return new RoundResult<>(round, currentPrice, currentWinner, bidHistory, true);
            }
            
            // 找出最高出价
            Bid<T> highestBid = findHighestBid(bids);
            
            if (highestBid != null && highestBid.getAmount() > currentPrice) {
                currentPrice = highestBid.getAmount();
                currentWinner = highestBid.getBidder();
                BidRecord<T> record = new BidRecord<>(highestBid, round, System.currentTimeMillis());
                bidHistory.add(record);
                
                // 检查是否达到保留价
                if (config.getReservePrice() > 0 && currentPrice >= config.getReservePrice()) {
                    log.debug("[AuctionRoundHandler] 达到保留价，拍卖结束");
                    return new RoundResult<>(round, currentPrice, currentWinner, bidHistory, true);
                }
                
                // 检查是否达到最大轮次
                if (round >= config.getMaxRounds()) {
                    log.debug("[AuctionRoundHandler] 达到最大轮次，拍卖结束");
                    return new RoundResult<>(round, currentPrice, currentWinner, bidHistory, true);
                }
                
                return new RoundResult<>(round, currentPrice, currentWinner, bidHistory, false);
            } else {
                log.debug("[AuctionRoundHandler] 无人提高出价，拍卖结束");
                return new RoundResult<>(round, currentPrice, currentWinner, bidHistory, true);
            }
        }
        
        /**
         * 执行荷兰式拍卖单轮
         */
        private RoundResult<T> executeDutchRound(
                T item,
                List<Bidder<T>> bidders,
                AuctionConfig config,
                int round,
                double currentPrice,
                List<BidRecord<T>> bidHistory) {
            
            log.debug("[AuctionRoundHandler] 荷兰式拍卖第 {} 轮，当前价格: {}", round, currentPrice);
            
            // 检查是否降至保留价以下
            if (currentPrice <= config.getReservePrice()) {
                log.debug("[AuctionRoundHandler] 价格降至保留价以下，拍卖流拍");
                return new RoundResult<>(round, currentPrice, null, bidHistory, true);
            }
            
            // 询问竞价者是否接受当前价格
            for (Bidder<T> bidder : bidders) {
                if (bidder.acceptPrice(item, currentPrice, bidHistory)) {
                    Bid<T> bid = new Bid<>(bidder, currentPrice, BidType.ACCEPT, round);
                    BidRecord<T> record = new BidRecord<>(bid, round, System.currentTimeMillis());
                    bidHistory.add(record);
                    log.debug("[AuctionRoundHandler] 竞价者 {} 接受价格 {}", 
                        bidder.getBidderId(), currentPrice);
                    return new RoundResult<>(round, currentPrice, bidder, bidHistory, true);
                }
            }
            
            // 降价
            double nextPrice = currentPrice - config.getPriceIncrement();
            
            // 检查是否达到最大轮次
            if (round >= config.getMaxRounds()) {
                log.debug("[AuctionRoundHandler] 达到最大轮次，拍卖结束");
                return new RoundResult<>(round, currentPrice, null, bidHistory, true);
            }
            
            return new RoundResult<>(round, nextPrice, null, bidHistory, false);
        }
        
        /**
         * 执行第一价格密封拍卖单轮
         */
        private RoundResult<T> executeFirstPriceSealedRound(
                T item,
                List<Bidder<T>> bidders,
                AuctionConfig config,
                int round,
                List<BidRecord<T>> bidHistory) {
            
            log.debug("[AuctionRoundHandler] 执行第一价格密封拍卖");
            
            // 收集所有密封出价
            List<Bid<T>> sealedBids = collectSealedBids(item, bidders, config.getReservePrice(), round);
            
            if (sealedBids.isEmpty()) {
                log.debug("[AuctionRoundHandler] 无有效出价，拍卖流拍");
                return new RoundResult<>(round, 0, null, bidHistory, true);
            }
            
            // 找出最高出价
            Bid<T> winningBid = findHighestBid(sealedBids);
            
            if (winningBid != null && winningBid.getAmount() >= config.getReservePrice()) {
                BidRecord<T> record = new BidRecord<>(winningBid, round, System.currentTimeMillis());
                bidHistory.add(record);
                log.debug("[AuctionRoundHandler] 赢家: {}, 成交价: {}", 
                    winningBid.getBidder().getBidderId(), winningBid.getAmount());
                return new RoundResult<>(round, winningBid.getAmount(), winningBid.getBidder(), bidHistory, true);
            }
            
            log.debug("[AuctionRoundHandler] 最高出价未达到保留价，拍卖流拍");
            return new RoundResult<>(round, 0, null, bidHistory, true);
        }
        
        /**
         * 执行第二价格密封拍卖单轮
         */
        private RoundResult<T> executeSecondPriceSealedRound(
                T item,
                List<Bidder<T>> bidders,
                AuctionConfig config,
                int round,
                List<BidRecord<T>> bidHistory) {
            
            log.debug("[AuctionRoundHandler] 执行第二价格密封拍卖");
            
            // 收集所有密封出价
            List<Bid<T>> sealedBids = collectSealedBids(item, bidders, config.getReservePrice(), round);
            
            if (sealedBids.size() < 2) {
                log.debug("[AuctionRoundHandler] 出价不足两个，拍卖流拍");
                return new RoundResult<>(round, 0, null, bidHistory, true);
            }
            
            // 排序找出最高和第二高出价
            List<Bid<T>> sortedBids = sealedBids.stream()
                .sorted((b1, b2) -> Double.compare(b2.getAmount(), b1.getAmount()))
                .collect(Collectors.toList());
            
            Bid<T> winningBid = sortedBids.get(0);
            double secondPrice = sortedBids.get(1).getAmount();
            
            if (winningBid.getAmount() >= config.getReservePrice()) {
                BidRecord<T> record = new BidRecord<>(winningBid, round, System.currentTimeMillis());
                bidHistory.add(record);
                log.debug("[AuctionRoundHandler] 赢家: {}, 最高出价: {}, 实际支付: {}", 
                    winningBid.getBidder().getBidderId(), winningBid.getAmount(), secondPrice);
                return new RoundResult<>(round, secondPrice, winningBid.getBidder(), bidHistory, true);
            }
            
            log.debug("[AuctionRoundHandler] 最高出价未达到保留价，拍卖流拍");
            return new RoundResult<>(round, 0, null, bidHistory, true);
        }
        
        /**
         * 执行 Vickrey 拍卖单轮
         */
        private RoundResult<T> executeVickreyRound(
                T item,
                List<Bidder<T>> bidders,
                AuctionConfig config,
                int round,
                List<BidRecord<T>> bidHistory) {
            
            log.debug("[AuctionRoundHandler] 执行 Vickrey 拍卖 (等同于第二价格密封拍卖)");
            return executeSecondPriceSealedRound(item, bidders, config, round, bidHistory);
        }
        
        /**
         * 执行全付拍卖单轮
         */
        private RoundResult<T> executeAllPayRound(
                T item,
                List<Bidder<T>> bidders,
                AuctionConfig config,
                int round,
                List<BidRecord<T>> bidHistory) {
            
            log.debug("[AuctionRoundHandler] 执行全付拍卖");
            
            List<Bid<T>> sealedBids = collectSealedBids(item, bidders, config.getReservePrice(), round);
            
            if (sealedBids.isEmpty()) {
                log.debug("[AuctionRoundHandler] 无有效出价，拍卖流拍");
                return new RoundResult<>(round, 0, null, bidHistory, true);
            }
            
            // 记录所有出价
            for (Bid<T> bid : sealedBids) {
                BidRecord<T> record = new BidRecord<>(bid, round, System.currentTimeMillis());
                bidHistory.add(record);
            }
            
            // 找出最高出价者
            Bid<T> winningBid = findHighestBid(sealedBids);
            
            if (winningBid != null && winningBid.getAmount() >= config.getReservePrice()) {
                log.debug("[AuctionRoundHandler] 赢家: {}, {} 位竞价者均需支付", 
                    winningBid.getBidder().getBidderId(), sealedBids.size());
                return new RoundResult<>(round, winningBid.getAmount(), winningBid.getBidder(), bidHistory, true);
            }
            
            log.debug("[AuctionRoundHandler] 最高出价未达到保留价，拍卖流拍");
            return new RoundResult<>(round, 0, null, bidHistory, true);
        }
        
        // ==================== 辅助方法 ====================
        
        private List<Bid<T>> collectBids(T item, List<Bidder<T>> bidders, double currentPrice, 
                                         List<BidRecord<T>> bidHistory, int round) {
            List<Bid<T>> bids = new ArrayList<>();
            for (Bidder<T> bidder : bidders) {
                double bidAmount = bidder.bid(item, currentPrice, bidHistory);
                if (bidAmount > currentPrice) {
                    bids.add(new Bid<>(bidder, bidAmount, BidType.RAISE, round));
                }
            }
            return bids;
        }
        
        private List<Bid<T>> collectSealedBids(T item, List<Bidder<T>> bidders, double reservePrice, int round) {
            List<Bid<T>> bids = new ArrayList<>();
            for (Bidder<T> bidder : bidders) {
                double bidAmount = bidder.sealedBid(item);
                if (bidAmount >= reservePrice) {
                    bids.add(new Bid<>(bidder, bidAmount, BidType.SEALED, round));
                }
            }
            return bids;
        }
        
        private Bid<T> findHighestBid(List<Bid<T>> bids) {
            return bids.stream()
                .max((b1, b2) -> Double.compare(b1.getAmount(), b2.getAmount()))
                .orElse(null);
        }
        
        /**
         * 轮次结果
         */
        @Data
        private static class RoundResult<T> {
            private final int currentRound;
            private final double currentPrice;
            private final Bidder<T> currentWinner;
            private final List<BidRecord<T>> bidHistory;
            private final boolean finished;
        }
    }

    // ==================== AuctionResultHandler ====================
    
    /**
     * 拍卖结果处理器
     * 
     * <p>在 LOOP 结束后执行，从 WorkflowContext 读取拍卖状态，
     * 构建最终的 AuctionResult 并通过 resultCallback 回调传递结果。</p>
     *
     * @param <T> 拍卖物品类型
     */
    @Data
    public static class AuctionResultHandler<T> implements NodeHandler {
        
        private Consumer<AuctionResult<T>> resultCallback;
        
        @Override
        public String getHandlerName() {
            return "auction_result";
        }
        
        @Override
        public Mono<Object> handle(WorkflowContext context, WorkflowNode node) {
            return Mono.fromCallable(() -> {
                try {
                    // 从上下文读取拍卖状态
                    T item = (T) context.getExecutionData(CONTEXT_KEY_ITEM);
                    AuctionMechanism mechanism = (AuctionMechanism) context.getExecutionData(CONTEXT_KEY_MECHANISM);
                    List<Bidder<T>> bidders = (List<Bidder<T>>) context.getExecutionData(CONTEXT_KEY_BIDDERS);
                    Bidder<T> currentWinner = (Bidder<T>) context.getExecutionData(CONTEXT_KEY_CURRENT_WINNER);
                    double currentPrice = context.getExecutionData(CONTEXT_KEY_CURRENT_PRICE) != null 
                        ? (double) context.getExecutionData(CONTEXT_KEY_CURRENT_PRICE) 
                        : 0;
                    List<BidRecord<T>> bidHistory = context.getExecutionData(CONTEXT_KEY_BID_HISTORY) != null 
                        ? (List<BidRecord<T>>) context.getExecutionData(CONTEXT_KEY_BID_HISTORY) 
                        : new ArrayList<>();
                    Long startTime = (Long) context.getExecutionData(CONTEXT_KEY_START_TIME);
                    
                    // 构建拍卖结果
                    AuctionResult<T> result = buildAuctionResult(
                        item, mechanism, currentWinner, currentPrice, 
                        bidHistory, bidders, startTime
                    );
                    
                    // 保存结果到上下文
                    context.updateExecutionData(CONTEXT_KEY_RESULT, result);
                    context.updateExecutionData(CONTEXT_KEY_STATUS, AuctionFramework.AuctionStatus.COMPLETED);
                    
                    // 触发回调
                    if (resultCallback != null) {
                        try {
                            resultCallback.accept(result);
                        } catch (Exception e) {
                            log.warn("[AuctionResultHandler] 结果回调执行失败", e);
                        }
                    }
                    
                    log.debug("[AuctionResultHandler] 拍卖完成，赢家: {}, 成交价: {}", 
                        currentWinner != null ? currentWinner.getBidderId() : "None", currentPrice);
                    
                    return result;
                    
                } catch (Exception e) {
                    log.error("[AuctionResultHandler] 拍卖结果构建失败", e);
                    throw e;
                }
            });
        }
        
        /**
         * 构建拍卖结果
         */
        private AuctionResult<T> buildAuctionResult(
                T item,
                AuctionMechanism mechanism,
                Bidder<T> winner,
                double finalPrice,
                List<BidRecord<T>> bidHistory,
                List<Bidder<T>> bidders,
                Long startTime) {
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put(METADATA_KEY_MECHANISM, mechanism.name());
            metadata.put(METADATA_KEY_TOTAL_BIDS, bidHistory.size());
            metadata.put(METADATA_KEY_PARTICIPANT_COUNT, bidders.size());
            
            // 计算持续时间
            long duration = 0;
            if (startTime != null) {
                duration = System.currentTimeMillis() - startTime;
            }
            
            return AuctionResult.<T>builder()
                .success(winner != null)
                .item(item)
                .winner(winner)
                .finalPrice(finalPrice)
                .bidHistory(new ArrayList<>(bidHistory))
                .metadata(metadata)
                .duration(duration)
                .totalRounds(bidHistory.isEmpty() ? 0 : bidHistory.get(bidHistory.size() - 1).getRound())
                .build();
        }
    }
}
