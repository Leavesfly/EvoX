package io.leavesfly.evox.gateway.ratelimit;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class RateLimiter {

    private final int maxRequestsPerMinute;
    private final Map<String, WindowCounter> userCounters = new ConcurrentHashMap<>();

    public RateLimiter(int maxRequestsPerMinute) {
        this.maxRequestsPerMinute = maxRequestsPerMinute;
    }

    public boolean tryAcquire(String userId) {
        WindowCounter counter = userCounters.computeIfAbsent(userId,
                k -> new WindowCounter());
        return counter.tryIncrement(maxRequestsPerMinute);
    }

    public int getRemainingRequests(String userId) {
        WindowCounter counter = userCounters.get(userId);
        if (counter == null) {
            return maxRequestsPerMinute;
        }
        return Math.max(0, maxRequestsPerMinute - counter.getCurrentCount());
    }

    private static class WindowCounter {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile long windowStart = System.currentTimeMillis();

        boolean tryIncrement(int maxCount) {
            long now = System.currentTimeMillis();
            if (now - windowStart >= 60_000) {
                synchronized (this) {
                    if (now - windowStart >= 60_000) {
                        count.set(0);
                        windowStart = now;
                    }
                }
            }
            return count.incrementAndGet() <= maxCount;
        }

        int getCurrentCount() {
            long now = System.currentTimeMillis();
            if (now - windowStart >= 60_000) {
                return 0;
            }
            return count.get();
        }
    }
}
