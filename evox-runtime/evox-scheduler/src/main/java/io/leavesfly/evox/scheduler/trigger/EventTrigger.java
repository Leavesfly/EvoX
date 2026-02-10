package io.leavesfly.evox.scheduler.trigger;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

public class EventTrigger implements ITrigger {

    private final String eventName;
    private final AtomicBoolean eventFired = new AtomicBoolean(false);

    public EventTrigger(String eventName) {
        this.eventName = eventName;
    }

    @Override
    public String getType() {
        return "event";
    }

    @Override
    public Instant getNextFireTime() {
        return null;
    }

    @Override
    public boolean shouldFire() {
        return eventFired.compareAndSet(true, false);
    }

    @Override
    public void onFired() {
    }

    @Override
    public boolean isRepeating() {
        return true;
    }

    public void fireEvent() {
        eventFired.set(true);
    }

    public String getEventName() {
        return eventName;
    }
}
