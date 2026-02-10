package io.leavesfly.evox.gateway.audit;

import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;

@Slf4j
public class AuditLogger {

    private final ConcurrentLinkedDeque<AuditEvent> events = new ConcurrentLinkedDeque<>();
    private final int maxEvents;

    public AuditLogger() {
        this(10000);
    }

    public AuditLogger(int maxEvents) {
        this.maxEvents = maxEvents;
    }

    public void logEvent(String userId, String channelId, String action,
                         String input, String output, boolean success, long durationMs) {
        AuditEvent event = AuditEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .userId(userId)
                .channelId(channelId)
                .action(action)
                .input(truncate(input, 500))
                .output(truncate(output, 500))
                .success(success)
                .durationMs(durationMs)
                .timestamp(Instant.now())
                .build();

        events.addLast(event);
        while (events.size() > maxEvents) {
            events.pollFirst();
        }

        log.info("AUDIT | user={} | channel={} | action={} | success={} | duration={}ms",
                userId, channelId, action, success, durationMs);
    }

    public List<AuditEvent> getRecentEvents(int count) {
        List<AuditEvent> recent = new ArrayList<>();
        var iterator = events.descendingIterator();
        while (iterator.hasNext() && recent.size() < count) {
            recent.add(iterator.next());
        }
        return recent;
    }

    public List<AuditEvent> getEventsByUser(String userId, int count) {
        List<AuditEvent> userEvents = new ArrayList<>();
        var iterator = events.descendingIterator();
        while (iterator.hasNext() && userEvents.size() < count) {
            AuditEvent event = iterator.next();
            if (userId.equals(event.getUserId())) {
                userEvents.add(event);
            }
        }
        return userEvents;
    }

    public int getTotalEventCount() {
        return events.size();
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}
