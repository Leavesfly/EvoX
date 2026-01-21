package io.leavesfly.evox.frameworks.debate;

import io.leavesfly.evox.agents.base.Agent;
import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.core.message.MessageType;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 默认辩论智能体实现
 * 继承自基础 Agent 类，实现了 MultiAgentDebate.DebateAgent 接口
 *
 * @author EvoX Team
 */
@Slf4j
@SuperBuilder
public class DefaultDebateAgent extends Agent implements MultiAgentDebate.DebateAgent {

    /**
     * 响应辩论请求
     *
     * @param question 辩论的主题或问题
     * @param history  之前的辩论记录
     * @return 智能体的观点
     */
    @Override
    public String respond(String question, List<MultiAgentDebate.DebateRecord> history) {
        StringBuilder sb = new StringBuilder();

        // 1. 角色设定
        if (getSystemPrompt() != null && !getSystemPrompt().isEmpty()) {
            sb.append("### 你的角色设定\n")
              .append(getSystemPrompt())
              .append("\n\n");
        }

        // 2. 核心问题
        sb.append("### 当前辩论主题\n")
          .append(question)
          .append("\n\n");

        // 3. 辩论历史
        if (history != null && !history.isEmpty()) {
            sb.append("### 辩论历史记录\n");
            for (MultiAgentDebate.DebateRecord record : history) {
                sb.append(String.format("[%d] %s: %s\n", 
                    record.getRound(), record.getAgentName(), record.getResponse()));
            }
            sb.append("\n请仔细分析上述历史观点，提出你的看法。你可以支持、反驳或补充他人的论点。");
        } else {
            sb.append("你是第一个发言者。请针对该主题提出你的初始观点。");
        }

        // 4. 输出引导
        sb.append("\n\n请直接输出你的辩论观点：");

        String prompt = sb.toString();
        log.debug("Debate agent [{}] is generating response...", getName());

        // 使用关联的 LLM 生成回答
        return getLlm().generate(prompt);
    }

    @Override
    public Message execute(String actionName, List<Message> messages) {
        // 为了兼容 Agent 基类，提供一个默认的 execute 实现
        // 在辩论框架中，主要通过 respond 接口进行交互
        String response = respond("Executing action: " + actionName, null);
        return Message.builder()
                .messageType(MessageType.RESPONSE)
                .content(response)
                .build();
    }
}
