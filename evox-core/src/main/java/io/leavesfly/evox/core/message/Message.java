package io.leavesfly.evox.core.message;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 消息类
 * 用于在智能体、工作流和动作之间传递信息
 *
 * @author EvoX Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Message {

    /**
     * 消息唯一标识
     */
    @Builder.Default
    private String messageId = UUID.randomUUID().toString();

    /**
     * 消息内容(支持多种类型)
     */
    private Object content;

    /**
     * 消息类型
     */
    @Builder.Default
    private MessageType messageType = MessageType.UNKNOWN;

    /**
     * 发送智能体名称
     */
    private String agent;

    /**
     * 执行的动作名称
     */
    private String action;

    /**
     * 使用的提示词
     */
    private String prompt;

    /**
     * 时间戳
     */
    @Builder.Default
    private Instant timestamp = Instant.now();

    /**
     * 工作流目标
     */
    private String workflowGoal;

    /**
     * 工作流任务
     */
    private String workflowTask;

    /**
     * 任务描述
     */
    private String workflowTaskDesc;

    /**
     * 下一步动作列表
     */
    private List<String> nextActions;

    /**
     * 创建输入消息
     */
    public static Message inputMessage(Object content) {
        return Message.builder()
                .content(content)
                .messageType(MessageType.INPUT)
                .build();
    }

    /**
     * 创建输出消息
     */
    public static Message outputMessage(Object content) {
        return Message.builder()
                .content(content)
                .messageType(MessageType.OUTPUT)
                .build();
    }

    /**
     * 创建响应消息
     */
    public static Message responseMessage(Object content, String agent, String action) {
        return Message.builder()
                .content(content)
                .agent(agent)
                .action(action)
                .messageType(MessageType.RESPONSE)
                .build();
    }

    /**
     * 创建错误消息
     */
    public static Message errorMessage(String errorMsg) {
        return Message.builder()
                .content(errorMsg)
                .messageType(MessageType.ERROR)
                .build();
    }

    /**
     * 创建系统消息
     */
    public static Message systemMessage(String content) {
        return Message.builder()
                .content(content)
                .messageType(MessageType.SYSTEM)
                .build();
    }
}
