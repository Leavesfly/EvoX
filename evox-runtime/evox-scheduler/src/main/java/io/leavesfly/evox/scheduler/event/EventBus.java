package io.leavesfly.evox.scheduler.event;

import io.leavesfly.evox.scheduler.trigger.EventTrigger;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
public class EventBus {

    private final Map<String, List<EventTrigger>> eventTriggers = new ConcurrentHashMap<>();
    private final Map<String, List<EventListener>> eventListeners = new ConcurrentHashMap<>();

    public void registerTrigger(String eventName, EventTrigger trigger) {
        eventTriggers.computeIfAbsent(eventName, k -> new CopyOnWriteArrayList<>()).add(trigger);
        log.debug("EventTrigger registered for event: {}", eventName);
    }

    public void unregisterTrigger(String eventName, EventTrigger trigger) {
        List<EventTrigger> triggers = eventTriggers.get(eventName);
        if (triggers != null) {
            triggers.remove(trigger);
        }
    }

    public void addEventListener(String eventName, EventListener listener) {
        eventListeners.computeIfAbsent(eventName, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    public void removeEventListener(String eventName, EventListener listener) {
        List<EventListener> listeners = eventListeners.get(eventName);
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    public void publishEvent(String eventName, Map<String, Object> eventData) {
        log.debug("Event published: {}", eventName);

        List<EventTrigger> triggers = eventTriggers.get(eventName);
        if (triggers != null) {
            for (EventTrigger trigger : triggers) {
                trigger.fireEvent();
            }
        }

        List<EventListener> listeners = eventListeners.get(eventName);
        if (listeners != null) {
            for (EventListener listener : listeners) {
                try {
                    listener.onEvent(eventName, eventData);
                } catch (Exception e) {
                    log.error("Error in event listener for event: {}", eventName, e);
                }
            }
        }
    }

    @FunctionalInterface
    public interface EventListener {
        void onEvent(String eventName, Map<String, Object> eventData);
    }
}
