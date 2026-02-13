package io.leavesfly.evox.scheduler.heartbeat;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 系统事件队列 — 线程安全的事件缓冲区。
 * 各模块（Cron 任务、Hook、手动触发等）将 SystemEvent 入队，
 * HeartbeatRunner 在每次心跳时批量取出并交给 Agent 处理。
 */
@Slf4j
public class SystemEventQueue {

    private final ConcurrentLinkedQueue<SystemEvent> queue = new ConcurrentLinkedQueue<>();

    /**
     * 入队一个系统事件
     */
    public void enqueue(SystemEvent event) {
        if (event == null) {
            return;
        }
        queue.offer(event);
        log.debug("System event enqueued: source={}, message={}", event.getSource(), event.getMessage());
    }

    /**
     * 取出所有待处理事件（清空队列）
     */
    public List<SystemEvent> drainAll() {
        List<SystemEvent> events = new ArrayList<>();
        SystemEvent event;
        while ((event = queue.poll()) != null) {
            events.add(event);
        }
        if (!events.isEmpty()) {
            log.debug("Drained {} system events from queue", events.size());
        }
        return events;
    }

    /**
     * 查看队列中是否有待处理事件
     */
    public boolean hasPendingEvents() {
        return !queue.isEmpty();
    }

    /**
     * 获取队列中待处理事件数量
     */
    public int size() {
        return queue.size();
    }

    /**
     * 清空队列
     */
    public void clear() {
        queue.clear();
    }
}
