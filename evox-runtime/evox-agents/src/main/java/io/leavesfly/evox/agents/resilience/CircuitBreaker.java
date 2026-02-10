package io.leavesfly.evox.agents.resilience;

import io.leavesfly.evox.core.exception.ExecutionException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 简单的熔断器实现
 * 提供失败保护和自动恢复机制
 *
 * @author EvoX Team
 */
@Slf4j
@Data
public class CircuitBreaker {

    /**
     * 熔断器状态
     */
    public enum State {
        /** 关闭状态 - 正常运行 */
        CLOSED,
        /** 打开状态 - 快速失败 */
        OPEN,
        /** 半开状态 - 尝试恢复 */
        HALF_OPEN
    }

    private final String name;
    private final int failureThreshold;
    private final Duration timeout;
    private final Duration resetTimeout;

    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicLong lastFailureTime = new AtomicLong(0);

    public CircuitBreaker(String name, int failureThreshold, Duration timeout, Duration resetTimeout) {
        this.name = name;
        this.failureThreshold = failureThreshold;
        this.timeout = timeout;
        this.resetTimeout = resetTimeout;
    }

    /**
     * 创建默认熔断器
     */
    public static CircuitBreaker defaultBreaker(String name) {
        return new CircuitBreaker(name, 5, Duration.ofSeconds(30), Duration.ofMinutes(1));
    }

    /**
     * 执行操作
     *
     * @param callable 操作
     * @param <T> 返回类型
     * @return 执行结果
     * @throws ExecutionException 如果熔断器打开或执行失败
     */
    public <T> T execute(Callable<T> callable) throws ExecutionException {
        if (!allowRequest()) {
            log.warn("Circuit breaker [{}] is OPEN, rejecting request", name);
            throw new ExecutionException("Circuit breaker is OPEN for: " + name);
        }

        try {
            T result = callable.call();
            onSuccess();
            return result;
        } catch (Exception e) {
            onFailure();
            throw new ExecutionException("Execution failed in circuit breaker: " + name, e);
        }
    }

    private boolean allowRequest() {
        State currentState = state.get();

        if (currentState == State.CLOSED) {
            return true;
        }

        if (currentState == State.OPEN) {
            long lastFailure = lastFailureTime.get();
            if (System.currentTimeMillis() - lastFailure > resetTimeout.toMillis()) {
                log.info("Circuit breaker [{}] transitioning from OPEN to HALF_OPEN", name);
                state.compareAndSet(State.OPEN, State.HALF_OPEN);
                return true;
            }
            return false;
        }

        return true;
    }

    private void onSuccess() {
        State currentState = state.get();

        if (currentState == State.HALF_OPEN) {
            int successes = successCount.incrementAndGet();
            log.debug("Circuit breaker [{}] success count: {}", name, successes);

            if (successes >= 3) {
                log.info("Circuit breaker [{}] transitioning from HALF_OPEN to CLOSED", name);
                state.set(State.CLOSED);
                failureCount.set(0);
                successCount.set(0);
            }
        } else if (currentState == State.CLOSED) {
            failureCount.set(0);
        }
    }

    private void onFailure() {
        lastFailureTime.set(System.currentTimeMillis());
        int failures = failureCount.incrementAndGet();
        State currentState = state.get();

        log.warn("Circuit breaker [{}] failure count: {}/{}", name, failures, failureThreshold);

        if (currentState == State.HALF_OPEN) {
            log.warn("Circuit breaker [{}] transitioning from HALF_OPEN to OPEN", name);
            state.set(State.OPEN);
            successCount.set(0);
        } else if (currentState == State.CLOSED && failures >= failureThreshold) {
            log.warn("Circuit breaker [{}] transitioning from CLOSED to OPEN", name);
            state.set(State.OPEN);
        }
    }

    /**
     * 重置熔断器
     */
    public void reset() {
        log.info("Resetting circuit breaker [{}]", name);
        state.set(State.CLOSED);
        failureCount.set(0);
        successCount.set(0);
        lastFailureTime.set(0);
    }

    /**
     * 获取当前状态
     */
    public State getState() {
        return state.get();
    }

    /**
     * 获取失败次数
     */
    public int getFailureCount() {
        return failureCount.get();
    }
}
