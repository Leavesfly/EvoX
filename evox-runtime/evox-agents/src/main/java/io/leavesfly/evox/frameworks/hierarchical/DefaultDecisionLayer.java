package io.leavesfly.evox.frameworks.hierarchical;

import io.leavesfly.evox.agents.base.Agent;
import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.core.message.MessageType;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 默认决策层级实现
 * 继承自基础 Agent 类，实现了 DecisionLayer 接口
 *
 * @author EvoX Team
 */
@Slf4j
@SuperBuilder
@Data
@EqualsAndHashCode(callSuper = true)
public class DefaultDecisionLayer extends Agent implements DecisionLayer<String> {

    /**
     * 层级级别 (0 为最高层)
     */
    private int level;

    @Override
    public String getLayerId() {
        return getAgentId();
    }

    @Override
    public String getLayerName() {
        return getName();
    }

    @Override
    public LayerDecision<String> decide(String task, LayerDecision<String> parentDecision) {
        StringBuilder sb = new StringBuilder();

        // 1. 角色设定
        if (getSystemPrompt() != null && !getSystemPrompt().isEmpty()) {
            sb.append("### 你的角色设定\n")
              .append(getSystemPrompt())
              .append("\n\n");
        }
        sb.append(String.format("你当前处于分层决策结构的第 %d 层。\n\n", level));

        // 2. 任务描述
        sb.append("### 当前任务目标\n")
          .append(task)
          .append("\n\n");

        // 3. 上下文信息
        if (parentDecision != null) {
            sb.append("### 上层决策背景\n")
              .append(String.format("上层任务: %s\n", parentDecision.getTask()))
              .append(String.format("上层思路: %s\n\n", parentDecision.getReasoning()));
        }

        // 4. 指令引导
        sb.append("请分析该任务并决定处理方式。你可以选择：\n")
          .append("1. 直接完成任务并给出最终答案。\n")
          .append("2. 将任务分解为更细小的子任务，委派给下一层处理。\n\n")
          .append("请按以下格式输出：\n")
          .append("DELEGATE: [YES/NO]\n")
          .append("REASONING: [你的决策依据]\n")
          .append("RESULT: [如果你选择 NO，请在此给出最终答案；如果选择 YES，请填 N/A]\n")
          .append("SUBTASKS: [如果你选择 YES，请列出子任务，每行一个；如果选择 NO，请填 N/A]\n");

        String response = getLlm().generate(sb.toString());
        log.debug("Layer [{}] decision response: {}", getName(), response);

        return parseResponse(task, response);
    }

    private LayerDecision<String> parseResponse(String task, String response) {
        LayerDecision<String> decision = new LayerDecision<>(getLayerId(), task);
        
        // 解析是否需要委派
        boolean needDelegation = response.contains("DELEGATE: YES");
        decision.setNeedDelegation(needDelegation);

        // 解析决策依据
        String reasoning = extractField(response, "REASONING");
        decision.setReasoning(reasoning);

        if (needDelegation) {
            // 解析子任务
            String subtasksText = extractField(response, "SUBTASKS");
            if (subtasksText != null && !subtasksText.equalsIgnoreCase("N/A")) {
                String[] lines = subtasksText.split("\n");
                for (String line : lines) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        // 移除可能的序号 (如 "1. ", "- ")
                        line = line.replaceAll("^[\\d\\.\\-\\s]+", "");
                        if (!line.isEmpty()) {
                            decision.addSubTask(line);
                        }
                    }
                }
            }
        } else {
            // 解析结果
            String result = extractField(response, "RESULT");
            decision.setResult(result);
        }

        return decision;
    }

    private String extractField(String text, String fieldName) {
        Pattern pattern = Pattern.compile(fieldName + ":\\s*(.*?)(?=\\n[A-Z]+:|$)", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    @Override
    public Message execute(String actionName, List<Message> messages) {
        // 基础 Agent 接口实现
        return Message.builder()
                .messageType(MessageType.RESPONSE)
                .content("Decision layer is active.")
                .build();
    }
}
