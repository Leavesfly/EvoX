package io.leavesfly.evox.scheduler.trigger;

import lombok.Getter;

import java.time.Duration;
import java.time.Instant;

public class IntervalTrigger implements ITrigger {

    @Getter
    private final Duration interval;
    private final Instant startTime;
    private Instant nextFireTime;

    public IntervalTrigger(Duration interval) {
        this(interval, Instant.now());
    }

    public IntervalTrigger(Duration interval, Instant startTime) {
        this.interval = interval;
        this.startTime = startTime;
        this.nextFireTime = startTime;
    }

    @Override
    public String getType() {
        return "interval";
    }

    @Override
    public Instant getNextFireTime() {
        return nextFireTime;
    }

    @Override
    public boolean shouldFire() {
        return !Instant.now().isBefore(nextFireTime);
    }

    @Override
    public void onFired() {
        this.nextFireTime = Instant.now().plus(interval);
    }

    @Override
    public boolean isRepeating() {
        return true;
    }
}
