package io.leavesfly.evox.cowork.ui;

import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * UI event bus for decoupling UI components.
 * Allows SidebarPanel and ChatPanel to communicate without direct references,
 * eliminating circular dependencies.
 */
@Slf4j
public class UIEventBus {

    private final Map<String, List<Consumer<Object>>> listeners = new ConcurrentHashMap<>();

    public void on(String eventName, Consumer<Object> listener) {
        listeners.computeIfAbsent(eventName, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    public void emit(String eventName, Object data) {
        List<Consumer<Object>> eventListeners = listeners.get(eventName);
        if (eventListeners != null) {
            for (Consumer<Object> listener : eventListeners) {
                Platform.runLater(() -> {
                    try {
                        listener.accept(data);
                    } catch (Exception e) {
                        log.error("Error handling UI event '{}': {}", eventName, e.getMessage(), e);
                    }
                });
            }
        }
    }

    // Event name constants
    public static final String SESSION_SELECTED = "session.selected";
    public static final String SESSION_LIST_REFRESH = "session.list.refresh";
    public static final String INPUT_TEXT_SET = "input.text.set";
}
