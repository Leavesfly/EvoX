package io.leavesfly.evox.core.message;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 消息类
 * 用于在智能体和动作之间传递信息。
 * 工作流编排相关的语义（如 workflowGoal、workflowTask）通过 metadata 扩展字段承载，
 * 保持 Message 本身作为通信载体的职责单一性。
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
     * 扩展元数据，用于承载工作流编排、自定义标签等非核心通信字段。
     * 工作流相关的 key 约定：
     * - "workflowGoal"     工作流目标
     * - "workflowTask"     工作流任务
     * - "workflowTaskDesc" 任务描述
     * - "nextActions"      下一步动作列表 (List&lt;String&gt;)
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    // ---- 工作流字段的向后兼容访问器 ----

    /**
     * 获取工作流目标（向后兼容）
     */
    public String getWorkflowGoal() {
        Object value = metadata != null ? metadata.get("workflowGoal") : null;
        return value instanceof String ? (String) value : null;
    }

    /**
     * 设置工作流目标（向后兼容）
     */
    public void setWorkflowGoal(String workflowGoal) {
        ensureMetadata();
        if (workflowGoal != null) {
            metadata.put("workflowGoal", workflowGoal);
        }
    }

    /**
     * 获取工作流任务（向后兼容）
     */
    public String getWorkflowTask() {
        Object value = metadata != null ? metadata.get("workflowTask") : null;
        return value instanceof String ? (String) value : null;
    }

    /**
     * 设置工作流任务（向后兼容）
     */
    public void setWorkflowTask(String workflowTask) {
        ensureMetadata();
        if (workflowTask != null) {
            metadata.put("workflowTask", workflowTask);
        }
    }

    /**
     * 获取任务描述（向后兼容）
     */
    public String getWorkflowTaskDesc() {
        Object value = metadata != null ? metadata.get("workflowTaskDesc") : null;
        return value instanceof String ? (String) value : null;
    }

    /**
     * 设置任务描述（向后兼容）
     */
    public void setWorkflowTaskDesc(String workflowTaskDesc) {
        ensureMetadata();
        if (workflowTaskDesc != null) {
            metadata.put("workflowTaskDesc", workflowTaskDesc);
        }
    }

    /**
     * 获取下一步动作列表（向后兼容）
     */
    @SuppressWarnings("unchecked")
    public List<String> getNextActions() {
        Object value = metadata != null ? metadata.get("nextActions") : null;
        return value instanceof List ? (List<String>) value : null;
    }

    /**
     * 设置下一步动作列表（向后兼容）
     */
    public void setNextActions(List<String> nextActions) {
        ensureMetadata();
        if (nextActions != null) {
            metadata.put("nextActions", nextActions);
        }
    }

    private void ensureMetadata() {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
    }

    /**
     * 获取元数据值
     */
    public Object getMetadataValue(String key) {
        return metadata != null ? metadata.get(key) : null;
    }

    /**
     * 设置元数据值
     */
    public void putMetadata(String key, Object value) {
        ensureMetadata();
        metadata.put(key, value);
    }

    /**
     * 自定义 Builder 扩展，为工作流字段提供向后兼容的 Builder 方法
     */
    public static class MessageBuilder {

        /**
         * 设置工作流目标（向后兼容 Builder 方法）
         */
        public MessageBuilder workflowGoal(String workflowGoal) {
            if (workflowGoal != null) {
                if (this.metadata$value == null) {
                    this.metadata$value = new HashMap<>();
                    this.metadata$set = true;
                }
                this.metadata$value.put("workflowGoal", workflowGoal);
            }
            return this;
        }

        /**
         * 设置工作流任务（向后兼容 Builder 方法）
         */
        public MessageBuilder workflowTask(String workflowTask) {
            if (workflowTask != null) {
                if (this.metadata$value == null) {
                    this.metadata$value = new HashMap<>();
                    this.metadata$set = true;
                }
                this.metadata$value.put("workflowTask", workflowTask);
            }
            return this;
        }

        /**
         * 设置任务描述（向后兼容 Builder 方法）
         */
        public MessageBuilder workflowTaskDesc(String workflowTaskDesc) {
            if (workflowTaskDesc != null) {
                if (this.metadata$value == null) {
                    this.metadata$value = new HashMap<>();
                    this.metadata$set = true;
                }
                this.metadata$value.put("workflowTaskDesc", workflowTaskDesc);
            }
            return this;
        }

        /**
         * 设置下一步动作列表（向后兼容 Builder 方法）
         */
        public MessageBuilder nextActions(List<String> nextActions) {
            if (nextActions != null) {
                if (this.metadata$value == null) {
                    this.metadata$value = new HashMap<>();
                    this.metadata$set = true;
                }
                this.metadata$value.put("nextActions", nextActions);
            }
            return this;
        }
    }

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
