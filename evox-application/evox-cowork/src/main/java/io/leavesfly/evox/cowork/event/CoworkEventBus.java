package io.leavesfly.evox.cowork.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class CoworkEventBus {
    
    private final List<SseEmitter> emitters;
    private final List<CoworkEvent> eventHistory;
    private final ExecutorService broadcastExecutor;
    private final int maxHistorySize;

    public CoworkEventBus() {
        this(100);
    }

    public CoworkEventBus(int maxHistorySize) {
        this.emitters = new CopyOnWriteArrayList<>();
        this.eventHistory = Collections.synchronizedList(new ArrayList<>());
        this.broadcastExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "sse-broadcast");
            thread.setDaemon(true);
            return thread;
        });
        this.maxHistorySize = maxHistorySize;
    }
    
    public record CoworkEvent(
        String eventId,
        String eventType,
        String sessionId,
        Object data,
        long timestamp
    ) {}
    
    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        
        emitter.onCompletion(() -> {
            log.info("SSE connection completed");
            emitters.remove(emitter);
        });
        
        emitter.onTimeout(() -> {
            log.warn("SSE connection timed out");
            emitters.remove(emitter);
        });
        
        emitter.onError((ex) -> {
            log.error("SSE connection error", ex);
            emitters.remove(emitter);
        });
        
        emitters.add(emitter);
        
        CoworkEvent initEvent = new CoworkEvent(
            UUID.randomUUID().toString(),
            "connection_established",
            "system",
            Map.of("message", "SSE connection established successfully"),
            System.currentTimeMillis()
        );
        
        try {
            emitter.send(SseEmitter.event()
                .id(initEvent.eventId())
                .name(initEvent.eventType())
                .data(initEvent));
        } catch (IOException e) {
            log.error("Failed to send initial event", e);
            emitters.remove(emitter);
        }
        
        return emitter;
    }
    
    public void emit(CoworkEvent event) {
        synchronized (eventHistory) {
            eventHistory.add(event);
            if (eventHistory.size() > maxHistorySize) {
                eventHistory.remove(0);
            }
        }

        broadcastExecutor.submit(() -> {
            List<SseEmitter> deadEmitters = new ArrayList<>();

            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event()
                            .id(event.eventId())
                            .name(event.eventType())
                            .data(event));
                } catch (IOException e) {
                    log.debug("Failed to send event to emitter, removing", e);
                    deadEmitters.add(emitter);
                }
            }

            if (!deadEmitters.isEmpty()) {
                emitters.removeAll(deadEmitters);
            }
        });
    }
    
    public void emitStream(String sessionId, String content) {
        CoworkEvent event = new CoworkEvent(
            UUID.randomUUID().toString(),
            "stream",
            sessionId,
            content,
            System.currentTimeMillis()
        );
        emit(event);
    }
    
    public void emitToolExecution(String sessionId, String toolName, String status, Object result) {
        CoworkEvent event = new CoworkEvent(
            UUID.randomUUID().toString(),
            "tool_execution",
            sessionId,
            Map.of("toolName", toolName, "status", status, "result", result),
            System.currentTimeMillis()
        );
        emit(event);
    }
    
    public void emitPermissionRequest(String sessionId, String requestId, String toolName, Map<String, Object> parameters) {
        CoworkEvent event = new CoworkEvent(
            UUID.randomUUID().toString(),
            "permission_request",
            sessionId,
            Map.of("requestId", requestId, "toolName", toolName, "parameters", parameters),
            System.currentTimeMillis()
        );
        emit(event);
    }
    
    public void emitSessionUpdate(String sessionId, String updateType, Object data) {
        CoworkEvent event = new CoworkEvent(
            UUID.randomUUID().toString(),
            "session_update",
            sessionId,
            Map.of("updateType", updateType, "data", data),
            System.currentTimeMillis()
        );
        emit(event);
    }
    
    public void emitProgress(String sessionId, String description, int completedSteps, int totalSteps) {
        CoworkEvent event = new CoworkEvent(
            UUID.randomUUID().toString(),
            "progress",
            sessionId,
            Map.of("description", description, "completedSteps", completedSteps, "totalSteps", totalSteps),
            System.currentTimeMillis()
        );
        emit(event);
    }
    
    public List<CoworkEvent> getRecentEvents(int count) {
        synchronized (eventHistory) {
            int size = eventHistory.size();
            if (count >= size) {
                return new ArrayList<>(eventHistory);
            }
            return new ArrayList<>(eventHistory.subList(size - count, size));
        }
    }
    
    public int getActiveConnectionCount() {
        return emitters.size();
    }
}
