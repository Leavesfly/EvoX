package io.leavesfly.evox.examples.frameworks;

import io.leavesfly.evox.frameworks.auction.*;
import io.leavesfly.evox.models.provider.ollama.OllamaLLM;
import io.leavesfly.evox.models.provider.ollama.OllamaLLMConfig;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * 拍卖框架示例
 * 
 * <p>演示智能体之间通过拍卖机制进行资源分配的场景。
 * 拍卖框架支持多种拍卖机制，适用于竞争性资源分配问题。
 * </p>
 * 
 * <p>支持的拍卖机制：
 * <ul>
 *   <li>ENGLISH: 英式拍卖（公开增价拍卖）</li>
 *   <li>DUTCH: 荷兰式拍卖（公开降价拍卖）</li>
 *   <li>SEALED_BID: 密封投标拍卖</li>
 *   <li>VICKREY: 维克里拍卖（二价密封拍卖）</li>
 * </ul>
 * </p>
 */
public class AuctionFrameworkExample {

    /**
     * 带打印功能的竞价者，在每次调用大模型前后打印 prompt 和响应
     */
    @SuperBuilder
    static class VerboseBidder<T> extends DefaultBidder<T> {

        @Override
        public double bid(T item, double currentPrice, List<BidRecord<T>> bidHistory) {
            System.out.println("\n  ┌─ [LLM调用] " + getBidderName() + " (英式拍卖出价, 当前价格: " + currentPrice + ")");
            double result = super.bid(item, currentPrice, bidHistory);
            System.out.println("  └─ [LLM响应] 出价: " + result);
            return result;
        }

        @Override
        public double sealedBid(T item) {
            System.out.println("\n  ┌─ [LLM调用] " + getBidderName() + " (密封拍卖出价)");
            double result = super.sealedBid(item);
            System.out.println("  └─ [LLM响应] 出价: " + result);
            return result;
        }

        @Override
        public boolean acceptPrice(T item, double currentPrice, List<BidRecord<T>> bidHistory) {
            System.out.println("\n  ┌─ [LLM调用] " + getBidderName() + " (荷兰式拍卖决策, 当前价格: " + currentPrice + ")");
            boolean result = super.acceptPrice(item, currentPrice, bidHistory);
            System.out.println("  └─ [LLM响应] 接受价格: " + result);
            return result;
        }
    }

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("拍卖框架示例 (Auction Framework)");
        System.out.println("========================================\n");

        // 创建LLM
        OllamaLLM llm = createLLM();

        // 执行拍卖框架演示
        demonstrateAuctionFramework(llm);

        System.out.println("\n========================================");
        System.out.println("拍卖框架示例演示完成!");
        System.out.println("========================================");
    }

    /**
     * 演示拍卖框架
     */
    private static void demonstrateAuctionFramework(OllamaLLM llm) {
        System.out.println("场景: 三个智能体竞拍\"一张稀有的复古海报\"");
        System.out.println();

        // 1. 创建竞价者
        List<Bidder<String>> bidders = new ArrayList<>();
        
        bidders.add(VerboseBidder.<String>builder()
                .agentId("collector-a-001")
                .name("收藏家A")
                .valuation(500.0)
                .budget(600.0)
                .systemPrompt("你是一个谨慎的收藏家，对海报估值为500，最多愿意出到600。")
                .llm(llm)
                .build());

        bidders.add(VerboseBidder.<String>builder()
                .agentId("collector-b-002")
                .name("收藏家B")
                .valuation(450.0)
                .budget(800.0)
                .systemPrompt("你是一个资金雄厚的收藏家，虽然估值450，但只要心情好，预算可以到800。")
                .llm(llm)
                .build());

        bidders.add(VerboseBidder.<String>builder()
                .agentId("collector-c-003")
                .name("收藏家C")
                .valuation(550.0)
                .budget(580.0)
                .systemPrompt("你是一个理性的收藏家，严格遵守估值，绝不超支。")
                .llm(llm)
                .build());

        // 2. 配置拍卖参数 (英式拍卖)
        AuctionConfig config = AuctionConfig.builder()
                .startingPrice(100.0)
                .priceIncrement(50.0)
                .maxRounds(10)
                .build();

        // 3. 创建并启动拍卖
        AuctionFramework<String> auction = new AuctionFramework<>(
                "复古海报",
                AuctionMechanism.ENGLISH,
                bidders,
                config
        );

        System.out.println("开始英式拍卖...");
        AuctionResult<String> result = auction.startAuction();

        // 4. 输出结果
        System.out.println("\n拍卖结束结果:");
        if (result.isSuccess()) {
            System.out.println("  获胜者: " + result.getWinner().getBidderName());
            System.out.println("  最终价格: " + result.getFinalPrice());
            System.out.println("  总轮次: " + result.getTotalRounds());
        } else {
            System.out.println("  拍卖失败: " + result.getError());
        }

        System.out.println("\n✅ 拍卖框架演示完成");
    }

    /**
     * 创建LLM实例
     */
    private static OllamaLLM createLLM() {
        OllamaLLMConfig config = new OllamaLLMConfig();
        return new OllamaLLM(config);
    }
}