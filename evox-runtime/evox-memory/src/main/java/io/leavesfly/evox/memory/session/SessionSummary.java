package io.leavesfly.evox.memory.session;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 会话摘要类，用于会话列表展示
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionSummary {

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 项目目录
     */
    private String projectDirectory;

    /**
     * 消息数量
     */
    private int messageCount;

    /**
     * 创建时间
     */
    private Instant createdAt;

    /**
     * 更新时间
     */
    private Instant updatedAt;

    /**
     * 最后一条用户消息（用于显示）
     */
    private String lastUserMessage;
}
