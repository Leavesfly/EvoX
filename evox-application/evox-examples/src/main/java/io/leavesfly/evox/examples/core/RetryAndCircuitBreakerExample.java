package io.leavesfly.evox.examples.core;

import io.leavesfly.evox.resilience.CircuitBreaker;
import io.leavesfly.evox.resilience.RetryExecutor;
import io.leavesfly.evox.resilience.RetryPolicy;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 核心能力示例：重试与熔断。
 */
@Slf4j
public class RetryAndCircuitBreakerExample {

    public static void main(String[] args) throws InterruptedException {
        log.info("=== EvoX Core Resilience Demo ===");
        // 先演示重试，再演示熔断
        demoRetry();
        demoCircuitBreaker();
    }

    private static void demoRetry() {
        log.info("--- Retry demo ---");

        // 配置重试策略：最多 4 次，固定退避
        RetryPolicy policy = RetryPolicy.builder()
                .maxAttempts(4)
                .initialDelay(Duration.ofMillis(50))
                .maxDelay(Duration.ofMillis(200))
                .backoffMultiplier(1.5)
                .useJitter(false)
                .build();

        RetryExecutor executor = new RetryExecutor(policy);
        AtomicInteger attempts = new AtomicInteger(0);

        // 前两次失败，第三次成功
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

        // 构建熔断器：2 次失败即打开
        CircuitBreaker breaker = new CircuitBreaker(
                "demo-breaker",
                2,
                Duration.ofSeconds(1),
                Duration.ofSeconds(1)
        );

        AtomicInteger counter = new AtomicInteger(0);

        // 连续失败触发熔断
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

        // 等待进入半开状态
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
