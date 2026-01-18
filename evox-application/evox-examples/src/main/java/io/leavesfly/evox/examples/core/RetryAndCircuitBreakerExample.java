package io.leavesfly.evox.examples.core;

import io.leavesfly.evox.core.circuitbreaker.CircuitBreaker;
import io.leavesfly.evox.core.retry.RetryExecutor;
import io.leavesfly.evox.core.retry.RetryPolicy;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Core resilience demo showcasing retry and circuit breaker.
 */
@Slf4j
public class RetryAndCircuitBreakerExample {

    public static void main(String[] args) throws InterruptedException {
        log.info("=== EvoX Core Resilience Demo ===");
        demoRetry();
        demoCircuitBreaker();
    }

    private static void demoRetry() {
        log.info("--- Retry demo ---");

        RetryPolicy policy = RetryPolicy.builder()
                .maxAttempts(4)
                .initialDelay(Duration.ofMillis(50))
                .maxDelay(Duration.ofMillis(200))
                .backoffMultiplier(1.5)
                .useJitter(false)
                .build();

        RetryExecutor executor = new RetryExecutor(policy);
        AtomicInteger attempts = new AtomicInteger(0);

        String result = executor.execute(() -> {
            int attempt = attempts.incrementAndGet();
            if (attempt < 3) {
                throw new IllegalStateException("Simulated failure on attempt " + attempt);
            }
            return "Success on attempt " + attempt;
        });

        log.info("Retry result: {}", result);
    }

    private static void demoCircuitBreaker() throws InterruptedException {
        log.info("--- Circuit breaker demo ---");

        CircuitBreaker breaker = new CircuitBreaker(
                "demo-breaker",
                2,
                Duration.ofSeconds(1),
                Duration.ofSeconds(1)
        );

        AtomicInteger counter = new AtomicInteger(0);

        for (int i = 0; i < 3; i++) {
            try {
                breaker.execute(() -> {
                    int attempt = counter.incrementAndGet();
                    throw new IllegalStateException("Failing request " + attempt);
                });
            } catch (Exception ex) {
                log.info("Call {} failed, breaker state: {}", i + 1, breaker.getState());
            }
        }

        log.info("Breaker open, waiting to reset...");
        Thread.sleep(1100);

        try {
            String recovered = breaker.execute(() -> "Recovered after reset");
            log.info("Breaker recovered: {}, state: {}", recovered, breaker.getState());
        } catch (Exception ex) {
            log.warn("Breaker still open: {}", breaker.getState());
        }
    }
}
