package io.leavesfly.evox.memory.session;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.leavesfly.evox.core.message.Message;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 会话数据类，包含完整的会话信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SessionData {

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 项目目录
     */
    private String projectDirectory;

    /**
     * 消息历史
     */
    private List<Message> messages;

    /**
     * 创建时间
     */
    private Instant createdAt;

    /**
     * 更新时间
     */
    private Instant updatedAt;

    /**
     * 扩展元数据
     */
    private Map<String, Object> metadata;
}
