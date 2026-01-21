package io.leavesfly.evox.frameworks.auction;

import io.leavesfly.evox.agents.base.Agent;
import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.core.message.MessageType;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 默认竞价者实现
 * 继承自基础 Agent 类，实现了 Bidder 接口
 *
 * @param <T> 拍卖物品类型
 * @author EvoX Team
 */
@Slf4j
@SuperBuilder
@Data
@EqualsAndHashCode(callSuper = true)
public class DefaultBidder<T> extends Agent implements Bidder<T> {

    /**
     * 竞价者对物品的估值
     */
    @Builder.Default
    private double valuation = 0.0;

    /**
     * 竞价者的预算限制
     */
    @Builder.Default
    private double budget = Double.MAX_VALUE;

    @Override
    public String getBidderId() {
        return getAgentId();
    }

    @Override
    public String getBidderName() {
        return getName();
    }

    @Override
    public double bid(T item, double currentPrice, List<BidRecord<T>> bidHistory) {
        StringBuilder sb = new StringBuilder();
        appendCommonContext(sb, item);
        
        sb.append("### 拍卖类型：英式拍卖（公开递增出价）\n")
          .append(String.format("当前价格：%.2f\n", currentPrice))
          .append(String.format("你的估值：%.2f\n", valuation))
          .append(String.format("你的预算：%.2f\n\n", budget));

        if (bidHistory != null && !bidHistory.isEmpty()) {
            sb.append("### 出价历史：\n");
            for (BidRecord<T> record : bidHistory) {
                sb.append(String.format("轮次 %d | %s 出价：%.2f\n", 
                    record.getRound(), record.getBid().getBidder().getBidderName(), record.getBid().getAmount()));
            }
        }

        sb.append("\n请决定你的出价。如果你想加价，请输入一个新的价格（必须大于当前价格且在预算内）。")
          .append("如果你想放弃或不再加价，请输入 0。")
          .append("\n请仅输出数字结果：");

        String response = getLlm().generate(sb.toString());
        return parseDouble(response, 0.0);
    }

    @Override
    public double sealedBid(T item) {
        StringBuilder sb = new StringBuilder();
        appendCommonContext(sb, item);

        sb.append("### 拍卖类型：密封价格拍卖（一次性出价）\n")
          .append(String.format("你的估值：%.2f\n", valuation))
          .append(String.format("你的预算：%.2f\n\n", budget))
          .append("这是一个密封拍卖，你只有一次出价机会。请根据你的估值和策略给出一个出价。")
          .append("\n请仅输出数字结果：");

        String response = getLlm().generate(sb.toString());
        return parseDouble(response, 0.0);
    }

    @Override
    public boolean acceptPrice(T item, double currentPrice, List<BidRecord<T>> bidHistory) {
        StringBuilder sb = new StringBuilder();
        appendCommonContext(sb, item);

        sb.append("### 拍卖类型：荷兰式拍卖（价格递减）\n")
          .append(String.format("当前价格：%.2f\n", currentPrice))
          .append(String.format("你的估值：%.2f\n", valuation))
          .append(String.format("你的预算：%.2f\n\n", budget))
          .append("如果此时你接受该价格，你将赢得物品并支付该价格。")
          .append("\n你是否接受当前价格？请回答 'YES' 或 'NO'。")
          .append("\n请仅输出 YES 或 NO：");

        String response = getLlm().generate(sb.toString());
        return response.trim().equalsIgnoreCase("YES");
    }

    private void appendCommonContext(StringBuilder sb, T item) {
        if (getSystemPrompt() != null && !getSystemPrompt().isEmpty()) {
            sb.append("### 角色设定\n").append(getSystemPrompt()).append("\n\n");
        }
        sb.append("### 拍卖物品\n").append(item.toString()).append("\n\n");
    }

    /**
     * 从字符串中解析数字
     */
    private double parseDouble(String input, double defaultValue) {
        if (input == null || input.isEmpty()) {
            return defaultValue;
        }
        try {
            // 尝试直接解析
            return Double.parseDouble(input.trim());
        } catch (NumberFormatException e) {
            // 尝试使用正则提取第一个数字
            Pattern pattern = Pattern.compile("(\\d+(\\.\\d+)?)");
            Matcher matcher = pattern.matcher(input);
            if (matcher.find()) {
                return Double.parseDouble(matcher.group(1));
            }
        }
        return defaultValue;
    }

    @Override
    public Message execute(String actionName, List<Message> messages) {
        // 基础 Agent 接口实现，在拍卖框架中主要使用 Bidder 接口
        return Message.builder()
                .messageType(MessageType.RESPONSE)
                .content("Bidder agent is active.")
                .build();
    }
}
