package io.leavesfly.evox.frameworks.consensus;

import io.leavesfly.evox.agents.base.Agent;
import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.core.message.MessageType;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 默认共识智能体实现
 * 继承自基础 Agent 类，实现了 ConsensusAgent 接口
 *
 * @param <T> 提议结果类型 (在默认实现中通常为 String)
 * @author EvoX Team
 */
@Slf4j
@SuperBuilder
@Data
@EqualsAndHashCode(callSuper = true)
public class DefaultConsensusAgent<T> extends Agent implements ConsensusFramework.ConsensusAgent<T> {

    /**
     * 智能体权重
     */
    @Builder.Default
    private double weight = 1.0;

    @Override
    public T propose(String question, List<ConsensusRecord<T>> history) {
        StringBuilder sb = new StringBuilder();

        // 1. 角色设定
        if (getSystemPrompt() != null && !getSystemPrompt().isEmpty()) {
            sb.append("### 你的角色设定\n")
              .append(getSystemPrompt())
              .append("\n\n");
        }

        // 2. 问题描述
        sb.append("### 待决策问题\n")
          .append(question)
          .append("\n\n");

        // 3. 共识历史
        if (history != null && !history.isEmpty()) {
            sb.append("### 共识讨论历史\n");
            for (ConsensusRecord<T> record : history) {
                sb.append(String.format("轮次 %d | 提议汇总: %s\n", 
                    record.getRound(), record.getProposals().toString()));
                if (record.getEvaluation() != null) {
                    sb.append(String.format("上轮评估: %s (置信度: %.2f)\n", 
                        record.getEvaluation().getConsensusValue(), 
                        record.getEvaluation().getConfidence()));
                }
            }
            sb.append("\n请参考上述历史，给出你认为最合适的方案。你可以坚持原观点，也可以根据他人建议进行修正。");
        } else {
            sb.append("你是第一轮提出方案，请根据你的专业背景给出初始建议。");
        }

        // 4. 输出引导
        sb.append("\n\n请直接输出你的提议内容：");

        String response = getLlm().generate(sb.toString());
        log.debug("Consensus agent [{}] proposed: {}", getName(), response);

        // 强制转换类型 (通常 T 为 String)
        return (T) response;
    }

    @Override
    public void onEvaluation(int round, ConsensusEvaluation<T> evaluation) {
        log.debug("Agent [{}] received evaluation for round {}: confidence={}", 
            getName(), round, evaluation.getConfidence());
    }

    @Override
    public double getWeight() {
        return weight;
    }

    @Override
    public Message execute(String actionName, List<Message> messages) {
        return Message.builder()
                .messageType(MessageType.RESPONSE)
                .content("Consensus agent is active.")
                .build();
    }
}
