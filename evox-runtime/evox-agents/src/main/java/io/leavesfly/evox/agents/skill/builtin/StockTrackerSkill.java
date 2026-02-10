package io.leavesfly.evox.agents.skill.builtin;

import io.leavesfly.evox.agents.skill.BaseSkill;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class StockTrackerSkill extends BaseSkill {

    public StockTrackerSkill() {
        setName("stock_tracker");
        setDescription("Track stock prices, market data, and financial information. "
                + "Query real-time prices, historical data, and set price alerts.");

        setSystemPrompt(buildStockSystemPrompt());

        setRequiredTools(List.of("http"));

        Map<String, Map<String, String>> inputParams = new LinkedHashMap<>();

        Map<String, String> operationParam = new HashMap<>();
        operationParam.put("type", "string");
        operationParam.put("description", "Operation: 'quote', 'history', 'watchlist', 'alert', 'market_summary'");
        inputParams.put("operation", operationParam);

        Map<String, String> symbolParam = new HashMap<>();
        symbolParam.put("type", "string");
        symbolParam.put("description", "Stock symbol (e.g., 'AAPL', 'GOOGL', 'TSLA')");
        inputParams.put("symbol", symbolParam);

        Map<String, String> periodParam = new HashMap<>();
        periodParam.put("type", "string");
        periodParam.put("description", "Time period for history: '1d', '5d', '1m', '3m', '6m', '1y' (default: '1m')");
        inputParams.put("period", periodParam);

        Map<String, String> thresholdParam = new HashMap<>();
        thresholdParam.put("type", "number");
        thresholdParam.put("description", "Price threshold for alerts (optional)");
        inputParams.put("threshold", thresholdParam);

        Map<String, String> directionParam = new HashMap<>();
        directionParam.put("type", "string");
        directionParam.put("description", "Alert direction: 'above' or 'below' (for alert operation)");
        inputParams.put("direction", directionParam);

        setInputParameters(inputParams);
        setRequiredInputs(List.of("operation"));
    }

    @Override
    public SkillResult execute(SkillContext context) {
        validateInputs(context.getParameters());

        String operation = context.getParameters().getOrDefault("operation", "quote").toString();
        String symbol = context.getParameters().getOrDefault("symbol", "").toString();
        String period = context.getParameters().getOrDefault("period", "1m").toString();
        String threshold = context.getParameters().getOrDefault("threshold", "").toString();
        String direction = context.getParameters().getOrDefault("direction", "below").toString();

        String prompt = buildPrompt(context.getInput(), context.getAdditionalContext());

        StringBuilder stockPrompt = new StringBuilder(prompt);
        stockPrompt.append("\n\nStock Operation: ").append(operation);

        switch (operation) {
            case "quote" -> {
                stockPrompt.append("\nSymbol: ").append(symbol);
                stockPrompt.append("\n\nGet real-time stock quote.");
                stockPrompt.append("\nUse a free API to fetch current price data.");
                stockPrompt.append("\nShow: current price, change, change%, volume, market cap, 52-week high/low.");
            }
            case "history" -> {
                stockPrompt.append("\nSymbol: ").append(symbol);
                stockPrompt.append("\nPeriod: ").append(period);
                stockPrompt.append("\n\nGet historical price data.");
                stockPrompt.append("\nShow price trend, high/low range, and average volume.");
                stockPrompt.append("\nProvide a brief technical analysis summary.");
            }
            case "watchlist" -> {
                stockPrompt.append("\n\nManage stock watchlist stored in watchlist.json.");
                if (!symbol.isEmpty()) {
                    stockPrompt.append("\nAdd/remove symbol: ").append(symbol);
                } else {
                    stockPrompt.append("\nList all watched stocks with current prices.");
                }
            }
            case "alert" -> {
                stockPrompt.append("\nSymbol: ").append(symbol);
                stockPrompt.append("\nThreshold: ").append(threshold);
                stockPrompt.append("\nDirection: ").append(direction);
                stockPrompt.append("\n\nSet a price alert. Store in alerts.json.");
                stockPrompt.append("\nAlert when price goes ").append(direction).append(" ").append(threshold).append(".");
            }
            case "market_summary" -> {
                stockPrompt.append("\n\nProvide a market summary.");
                stockPrompt.append("\nInclude major indices (S&P 500, NASDAQ, Dow Jones).");
                stockPrompt.append("\nShow market sentiment and notable movers.");
            }
            default -> stockPrompt.append("\n\nUnknown operation. Available: quote, history, watchlist, alert, market_summary.");
        }

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("skillName", "stock_tracker");
        metadata.put("operation", operation);
        if (!symbol.isEmpty()) metadata.put("symbol", symbol);

        return SkillResult.success(stockPrompt.toString(), metadata);
    }

    private String buildStockSystemPrompt() {
        return """
                You are a financial data and stock market assistant.
                
                When tracking stocks:
                1. Use free financial APIs for data (e.g., Yahoo Finance via curl, Alpha Vantage)
                2. Present financial data in clear, tabular format
                3. Include relevant market context with price data
                4. For alerts, store configurations in a local JSON file
                5. Handle market hours awareness (pre-market, regular, after-hours)
                6. Provide brief analysis but always include disclaimers
                
                Important:
                - Always include a disclaimer that this is not financial advice
                - Show data timestamps to indicate freshness
                - Handle API errors gracefully
                - Use appropriate currency formatting""";
    }
}
