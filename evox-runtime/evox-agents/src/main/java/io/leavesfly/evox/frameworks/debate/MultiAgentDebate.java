package io.leavesfly.evox.frameworks.debate;

import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.core.message.MessageType;
import io.leavesfly.evox.models.base.BaseLLM;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 多智能体辩论框架
 * 允许多个智能体通过辩论达成共识
 *
 * @author EvoX Team
 */
@Slf4j
@Data
@Builder
public class MultiAgentDebate {

    /**
     * 参与辩论的智能体列表
     */
    private List<DebateAgent> agents;

    /**
     * 最大辩论轮数
     */
    private int maxRounds;

    /**
     * 当前辩论轮数
     */
    private int currentRound;

    /**
     * 辩论历史
     */
    private List<DebateRecord> history;

    /**
     * 辩论主持人（可选，用于检查共识和生成最终答案）
     */
    private BaseLLM moderator;

    public MultiAgentDebate(List<DebateAgent> agents, int maxRounds) {
        this(agents, maxRounds, null);
    }

    public MultiAgentDebate(List<DebateAgent> agents, int maxRounds, BaseLLM moderator) {
        this.agents = agents;
        this.maxRounds = maxRounds;
        this.moderator = moderator;
        this.currentRound = 0;
        this.history = new ArrayList<>();
    }

    /**
     * 开始辩论
     */
    public String debate(String question) {
        log.info("Starting debate on question: {}", question);
        
        for (currentRound = 1; currentRound <= maxRounds; currentRound++) {
            log.info("Round {}/{}", currentRound, maxRounds);
            
            // 每个智能体发表观点
            for (DebateAgent agent : agents) {
                String response = agent.respond(question, history);
                history.add(new DebateRecord(currentRound, agent.getName(), response));
                log.debug("{}: {}", agent.getName(), response);
            }
            
            // 检查是否达成共识
            if (checkConsensus()) {
                log.info("Consensus reached in round {}", currentRound);
                break;
            }
        }
        
        // 生成最终答案
        return generateFinalAnswer();
    }

    /**
     * 检查是否达成共识
     */
    private boolean checkConsensus() {
        if (moderator == null || history.isEmpty()) {
            return false;
        }

        log.debug("Checking for consensus among agents...");

        // 构建共识检查提示词
        StringBuilder sb = new StringBuilder();
        sb.append("分析以下辩论历史，并判断所有参与者是否已达成明确的共识或一致意见。\n\n");
        sb.append("辩论历史：\n");
        for (DebateRecord record : history) {
            sb.append(String.format("[%d] %s: %s\n", record.getRound(), record.getAgentName(), record.getResponse()));
        }
        sb.append("\n是否已达成共识？请以 'YES' 或 'NO' 开头回答，并简要说明理由。");

        String response = moderator.generate(sb.toString());
        boolean reached = response.trim().toUpperCase().startsWith("YES");

        if (reached) {
            log.info("Consensus detected by moderator: {}", response);
        }

        return reached;
    }

    /**
     * 生成最终答案
     */
    private String generateFinalAnswer() {
        if (history.isEmpty()) {
            return "没有可用的辩论历史。";
        }

        if (moderator == null) {
            // 如果没有主持人，返回最后一条记录作为参考
            DebateRecord last = history.get(history.size() - 1);
            return String.format("辩论结束，未配置主持人。来自 %s 的最后观点：%s", 
                last.getAgentName(), last.getResponse());
        }

        log.info("Generating final answer from debate history...");

        // 构建总结提示词
        StringBuilder sb = new StringBuilder();
        sb.append("基于以下多智能体辩论历史，请提供一个全面的最终答案或总结，综合关键观点及达成的任何共识。\n\n");
        sb.append("辩论历史：\n");
        for (DebateRecord record : history) {
            sb.append(String.format("[%d] %s: %s\n", record.getRound(), record.getAgentName(), record.getResponse()));
        }
        sb.append("\n最终全面回答：");

        return moderator.generate(sb.toString());
    }

    /**
     * 辩论智能体接口
     */
    public interface DebateAgent {
        String getName();
        String respond(String question, List<DebateRecord> history);
    }

    /**
     * 辩论记录
     */
    @Data
    public static class DebateRecord {
        private int round;
        private String agentName;
        private String response;
        private long timestamp;

        public DebateRecord(int round, String agentName, String response) {
            this.round = round;
            this.agentName = agentName;
            this.response = response;
            this.timestamp = System.currentTimeMillis();
        }
    }
}
