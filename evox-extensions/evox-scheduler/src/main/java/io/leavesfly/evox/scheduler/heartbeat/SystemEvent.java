package io.leavesfly.evox.scheduler.heartbeat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * 系统事件 — 用于 Heartbeat 心跳机制中的事件传递。
 * 当定时任务、外部 Hook 或其他模块需要通知 Agent 处理某件事时，
 * 将事件入队到 SystemEventQueue，由 HeartbeatRunner 在下次心跳时统一处理。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemEvent {

    /** 事件唯一标识 */
    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    /** 事件来源（如 "cron", "hook", "manual"） */
    private String source;

    /** 事件消息内容（将作为 Agent 的输入提示） */
    private String message;

    /** 唤醒模式："now" 立即唤醒 / "next-heartbeat" 等待下次心跳 */
    @Builder.Default
    private WakeMode wakeMode = WakeMode.NEXT_HEARTBEAT;

    /** 事件创建时间 */
    @Builder.Default
    private Instant createdAt = Instant.now();

    /** 额外元数据 */
    private Map<String, Object> metadata;

    /**
     * 唤醒模式枚举
     */
    public enum WakeMode {
        /** 立即唤醒 HeartbeatRunner 执行 */
        NOW,
        /** 等待下次定时心跳时处理 */
        NEXT_HEARTBEAT
    }

    /**
     * 快速创建一个系统事件
     */
    public static SystemEvent of(String source, String message) {
        return SystemEvent.builder()
                .source(source)
                .message(message)
                .build();
    }

    /**
     * 快速创建一个立即唤醒的系统事件
     */
    public static SystemEvent immediate(String source, String message) {
        return SystemEvent.builder()
                .source(source)
                .message(message)
                .wakeMode(WakeMode.NOW)
                .build();
    }
}
