package io.leavesfly.evox.scheduler.heartbeat;

import io.leavesfly.evox.core.agent.IAgent;
import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.core.message.MessageType;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Heartbeat 心跳执行器 — 主动唤醒机制的核心。
 *
 * <p>定期唤醒 Agent，收集并处理积压的系统事件。
 * 支持两种唤醒模式：
 * <ul>
 *   <li>定时心跳：按固定间隔自动执行</li>
 *   <li>立即唤醒：通过 {@link #wakeNow()} 触发即时执行</li>
 * </ul>
 *
 * <p>与 OpenClaw 的 Heartbeat 机制对标，实现 Agent 的主动感知与自动化任务处理。
 */
@Slf4j
public class HeartbeatRunner {

    private final IAgent agent;
    private final SystemEventQueue eventQueue;
    private final HeartbeatConfig config;
    private ScheduledExecutorService scheduler;
    private volatile boolean running = false;

    /** 心跳执行统计 */
    private volatile long totalHeartbeats = 0;
    private volatile long totalEventsProcessed = 0;
    private volatile Instant lastHeartbeatTime;

    public HeartbeatRunner(IAgent agent, SystemEventQueue eventQueue, HeartbeatConfig config) {
        this.agent = agent;
        this.eventQueue = eventQueue;
        this.config = config;
    }

    /**
     * 启动心跳
     */
    public void start() {
        if (running) {
            log.warn("HeartbeatRunner is already running");
            return;
        }
        if (!config.isEnabled()) {
            log.info("HeartbeatRunner is disabled by configuration");
            return;
        }

        running = true;
        scheduler = Executors.newSingleThreadScheduledExecutor(
                runnable -> {
                    Thread thread = new Thread(runnable, "evox-heartbeat");
                    thread.setDaemon(true);
                    return thread;
                });

        scheduler.scheduleWithFixedDelay(
                this::runHeartbeatSafely,
                config.getInitialDelayMs(),
                config.getIntervalMs(),
                TimeUnit.MILLISECONDS);

        log.info("HeartbeatRunner started: interval={}ms, initialDelay={}ms",
                config.getIntervalMs(), config.getInitialDelayMs());
    }

    /**
     * 停止心跳
     */
    public void shutdown() {
        running = false;
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        log.info("HeartbeatRunner shutdown. Total heartbeats: {}, events processed: {}",
                totalHeartbeats, totalEventsProcessed);
    }

    /**
     * 立即唤醒（wakeMode = NOW）
     * 在定时心跳之外，立即触发一次心跳执行
     */
    public void wakeNow() {
        if (!running || scheduler == null) {
            log.warn("Cannot wake: HeartbeatRunner is not running");
            return;
        }
        log.info("Immediate wake triggered");
        scheduler.submit(this::runHeartbeatSafely);
    }

    /**
     * 入队系统事件，并根据唤醒模式决定是否立即唤醒
     */
    public void enqueueEvent(SystemEvent event) {
        eventQueue.enqueue(event);
        if (event.getWakeMode() == SystemEvent.WakeMode.NOW) {
            wakeNow();
        }
    }

    /**
     * 安全执行心跳（捕获所有异常，防止调度器中断）
     */
    private void runHeartbeatSafely() {
        try {
            runHeartbeat();
        } catch (Exception e) {
            log.error("Heartbeat execution failed", e);
        }
    }

    /**
     * 执行一次心跳
     */
    private void runHeartbeat() {
        totalHeartbeats++;
        lastHeartbeatTime = Instant.now();

        List<SystemEvent> pendingEvents = eventQueue.drainAll();
        String heartbeatPrompt = buildHeartbeatPrompt(pendingEvents);

        log.debug("Heartbeat #{}: {} pending events", totalHeartbeats, pendingEvents.size());

        Message inputMessage = Message.builder()
                .content(heartbeatPrompt)
                .messageType(MessageType.SYSTEM)
                .build();
        inputMessage.putMetadata("heartbeatId", totalHeartbeats);
        inputMessage.putMetadata("pendingEventCount", pendingEvents.size());

        Message result = agent.execute("heartbeat", List.of(inputMessage));

        totalEventsProcessed += pendingEvents.size();

        if (result != null) {
            log.debug("Heartbeat #{} completed. Agent response length: {}",
                    totalHeartbeats,
                    result.getContent() != null ? result.getContent().toString().length() : 0);
        }
    }

    /**
     * 构建心跳提示词
     */
    private String buildHeartbeatPrompt(List<SystemEvent> pendingEvents) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("## Heartbeat #").append(totalHeartbeats).append("\n\n");
        prompt.append("This is a periodic heartbeat check. ");
        prompt.append("Review any pending system events and take appropriate actions.\n\n");

        if (pendingEvents.isEmpty()) {
            prompt.append("No pending system events. Perform routine checks if needed.\n");
        } else {
            prompt.append("### Pending System Events (").append(pendingEvents.size()).append(")\n\n");
            for (int i = 0; i < pendingEvents.size(); i++) {
                SystemEvent event = pendingEvents.get(i);
                prompt.append(i + 1).append(". **[").append(event.getSource()).append("]** ");
                prompt.append(event.getMessage()).append("\n");
                prompt.append("   _Created: ").append(event.getCreatedAt()).append("_\n\n");
            }
            prompt.append("Please process these events and respond with any actions taken.\n");
        }

        return prompt.toString();
    }

    // ---- 状态查询 ----

    public boolean isRunning() {
        return running;
    }

    public long getTotalHeartbeats() {
        return totalHeartbeats;
    }

    public long getTotalEventsProcessed() {
        return totalEventsProcessed;
    }

    public Instant getLastHeartbeatTime() {
        return lastHeartbeatTime;
    }

    public int getPendingEventCount() {
        return eventQueue.size();
    }

    /**
     * 心跳配置
     */
    @Data
    @Builder
    public static class HeartbeatConfig {
        /** 是否启用心跳 */
        @Builder.Default
        private boolean enabled = true;

        /** 心跳间隔（毫秒），默认 5 分钟 */
        @Builder.Default
        private long intervalMs = 300_000;

        /** 启动延迟（毫秒），默认 10 秒 */
        @Builder.Default
        private long initialDelayMs = 10_000;
    }
}
