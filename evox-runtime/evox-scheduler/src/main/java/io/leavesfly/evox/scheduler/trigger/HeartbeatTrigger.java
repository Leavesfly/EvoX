package io.leavesfly.evox.scheduler.trigger;

import java.time.Instant;
import java.util.function.Supplier;

public class HeartbeatTrigger implements ITrigger {
    
    private final long intervalMs;
    private Instant lastFireTime;
    private final Supplier<Boolean> checkCondition;
    
    public HeartbeatTrigger(long intervalMs) {
        this(intervalMs, null);
    }
    
    public HeartbeatTrigger(long intervalMs, Supplier<Boolean> checkCondition) {
        if (intervalMs <= 0) {
            throw new IllegalArgumentException("Interval must be positive");
        }
        this.intervalMs = intervalMs;
        this.checkCondition = checkCondition;
        this.lastFireTime = Instant.now();
    }
    
    @Override
    public String getType() {
        return "heartbeat";
    }
    
    @Override
    public Instant getNextFireTime() {
        return lastFireTime.plusMillis(intervalMs);
    }
    
    @Override
    public boolean shouldFire() {
        Instant now = Instant.now();
        if (now.isBefore(getNextFireTime())) {
            return false;
        }
        
        if (checkCondition != null) {
            return checkCondition.get();
        }
        
        return true;
    }
    
    @Override
    public void onFired() {
        this.lastFireTime = Instant.now();
    }
    
    @Override
    public boolean isRepeating() {
        return true;
    }
    
    public long getIntervalMs() {
        return intervalMs;
    }
    
    public Instant getLastFireTime() {
        return lastFireTime;
    }
    
    public Supplier<Boolean> getCheckCondition() {
        return checkCondition;
    }
}
