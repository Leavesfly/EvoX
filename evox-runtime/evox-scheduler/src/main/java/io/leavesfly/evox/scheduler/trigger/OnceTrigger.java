package io.leavesfly.evox.scheduler.trigger;

import java.time.Instant;

public class OnceTrigger implements ITrigger {

    private final Instant fireTime;
    private boolean fired = false;

    public OnceTrigger(Instant fireTime) {
        this.fireTime = fireTime;
    }

    public static OnceTrigger immediate() {
        return new OnceTrigger(Instant.now());
    }

    public static OnceTrigger at(Instant time) {
        return new OnceTrigger(time);
    }

    @Override
    public String getType() {
        return "once";
    }

    @Override
    public Instant getNextFireTime() {
        return fired ? null : fireTime;
    }

    @Override
    public boolean shouldFire() {
        return !fired && !Instant.now().isBefore(fireTime);
    }

    @Override
    public void onFired() {
        this.fired = true;
    }

    @Override
    public boolean isRepeating() {
        return false;
    }
}
