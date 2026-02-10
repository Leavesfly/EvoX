package io.leavesfly.evox.scheduler.trigger;

import java.time.Instant;

public interface ITrigger {

    String getType();

    Instant getNextFireTime();

    boolean shouldFire();

    void onFired();

    boolean isRepeating();
}
