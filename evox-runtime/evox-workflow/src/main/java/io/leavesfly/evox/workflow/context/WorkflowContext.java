package io.leavesfly.evox.workflow.context;

import io.leavesfly.evox.core.message.Message;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作流上下文
 * 承载工作流编排相关的语义信息，与 Message 的通信职责解耦。
 * 工作流引擎在执行过程中使用此类管理目标、任务、状态等编排信息。
 *
 * @author EvoX Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowContext {

    /**
     * 工作流目标
     */
    private String workflowGoal;

    /**
     * 当前工作流任务
     */
    private String workflowTask;

    /**
     * 任务描述
     */
    private String workflowTaskDesc;

    /**
     * 下一步动作列表
     */
    @Builder.Default
    private List<String> nextActions = new ArrayList<>();

    /**
     * 工作流执行过程中的变量
     */
    @Builder.Default
    private Map<String, Object> variables = new HashMap<>();

    /**
     * 从 Message 的 metadata 中提取工作流上下文
     *
     * @param message 消息
     * @return 工作流上下文
     */
    public static WorkflowContext fromMessage(Message message) {
        if (message == null) {
            return WorkflowContext.builder().build();
        }

        return WorkflowContext.builder()
                .workflowGoal(message.getWorkflowGoal())
                .workflowTask(message.getWorkflowTask())
                .workflowTaskDesc(message.getWorkflowTaskDesc())
                .nextActions(message.getNextActions() != null ? message.getNextActions() : new ArrayList<>())
                .build();
    }

    /**
     * 将工作流上下文写入 Message 的 metadata
     *
     * @param message 目标消息
     */
    public void applyToMessage(Message message) {
        if (message == null) {
            return;
        }

        if (workflowGoal != null) {
            message.setWorkflowGoal(workflowGoal);
        }
        if (workflowTask != null) {
            message.setWorkflowTask(workflowTask);
        }
        if (workflowTaskDesc != null) {
            message.setWorkflowTaskDesc(workflowTaskDesc);
        }
        if (nextActions != null && !nextActions.isEmpty()) {
            message.setNextActions(nextActions);
        }
    }
}
