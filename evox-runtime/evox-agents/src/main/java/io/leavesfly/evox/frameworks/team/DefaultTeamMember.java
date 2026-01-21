package io.leavesfly.evox.frameworks.team;

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
 * 默认团队成员实现
 * 继承自基础 Agent 类，实现了 TeamMember 接口
 *
 * @author EvoX Team
 */
@Slf4j
@SuperBuilder
@Data
@EqualsAndHashCode(callSuper = true)
public class DefaultTeamMember extends Agent implements TeamMember<String> {

    /**
     * 成员角色
     */
    private TeamRole role;

    /**
     * 成员技能列表
     */
    @Builder.Default
    private List<String> skills = List.of();

    @Override
    public String getMemberId() {
        return getAgentId();
    }

    @Override
    public String getMemberName() {
        return getName();
    }

    @Override
    public String execute(String task, String previousResult, List<TaskExecution<String>> executionHistory) {
        StringBuilder sb = new StringBuilder();

        // 1. 角色设定
        if (getSystemPrompt() != null && !getSystemPrompt().isEmpty()) {
            sb.append("### 你的角色设定\n")
              .append(getSystemPrompt())
              .append("\n\n");
        }
        sb.append(String.format("你的当前团队职责是：%s\n", role.name()));
        if (!skills.isEmpty()) {
            sb.append(String.format("你的专业技能包括：%s\n", String.join(", ", skills)));
        }
        sb.append("\n");

        // 2. 任务描述
        sb.append("### 当前任务目标\n")
          .append(task)
          .append("\n\n");

        // 3. 上下文信息
        if (previousResult != null && !previousResult.isEmpty()) {
            sb.append("### 前序环节产出\n")
              .append(previousResult)
              .append("\n\n");
        }

        // 4. 执行历史
        if (executionHistory != null && !executionHistory.isEmpty()) {
            sb.append("### 团队执行历史\n");
            for (TaskExecution<String> execution : executionHistory) {
                sb.append(String.format("- 成员 [%s]: %s\n", 
                    execution.getMemberId(), execution.getResult()));
            }
            sb.append("\n");
        }

        // 5. 输出引导
        sb.append("请根据上述信息执行你的任务。");
        if (role == TeamRole.REVIEWER) {
            sb.append("作为审核者，请重点检查前序工作的质量并给出改进建议或批准结论。");
        } else if (role == TeamRole.COORDINATOR) {
            sb.append("作为协调者，请重点整合各方观点并给出协调方案。");
        }
        sb.append("\n请直接输出你的执行结果：");

        String prompt = sb.toString();
        log.debug("Team member [{}] is executing task...", getName());

        // 使用关联的 LLM 生成回答
        return getLlm().generate(prompt);
    }

    @Override
    public Message execute(String actionName, List<Message> messages) {
        // 基础 Agent 接口实现，在团队框架中主要使用 execute(String task, ...)
        String response = execute("Action: " + actionName, null, null);
        return Message.builder()
                .messageType(MessageType.RESPONSE)
                .content(response)
                .build();
    }
}
