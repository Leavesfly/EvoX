package io.leavesfly.evox.core.circuitbreaker;

import io.leavesfly.evox.core.exception.ExecutionException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
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
        /**
         * 关闭状态 - 正常运行
         */
        CLOSED,
        
        /**
         * 打开状态 - 快速失败
         */
        OPEN,
        
        /**
         * 半开状态 - 尝试恢复
         */
        HALF_OPEN
    }

    private final String name;
    private final int failureThreshold;          // 失败阈值
    private final Duration timeout;               // 超时时间
    private final Duration resetTimeout;          // 重置超时
    
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
        return new CircuitBreaker(
                name,
                5,                           // 5次失败后熔断
                Duration.ofSeconds(30),      // 30秒超时
                Duration.ofMinutes(1)        // 1分钟后尝试恢复
        );
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
        // 检查熔断器状态
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

    /**
     * 是否允许请求
     */
    private boolean allowRequest() {
        State currentState = state.get();
        
        if (currentState == State.CLOSED) {
            return true;
        }
        
        if (currentState == State.OPEN) {
            // 检查是否可以进入半开状态
            long lastFailure = lastFailureTime.get();
            if (System.currentTimeMillis() - lastFailure > resetTimeout.toMillis()) {
                log.info("Circuit breaker [{}] transitioning from OPEN to HALF_OPEN", name);
                state.compareAndSet(State.OPEN, State.HALF_OPEN);
                return true;
            }
            return false;
        }
        
        // HALF_OPEN 状态允许少量请求通过
        return true;
    }

    /**
     * 成功回调
     */
    private void onSuccess() {
        State currentState = state.get();
        
        if (currentState == State.HALF_OPEN) {
            int successes = successCount.incrementAndGet();
            log.debug("Circuit breaker [{}] success count: {}", name, successes);
            
            // 连续成功则关闭熔断器
            if (successes >= 3) {
                log.info("Circuit breaker [{}] transitioning from HALF_OPEN to CLOSED", name);
                state.set(State.CLOSED);
                failureCount.set(0);
                successCount.set(0);
            }
        } else if (currentState == State.CLOSED) {
            // 重置失败计数
            failureCount.set(0);
        }
    }

    /**
     * 失败回调
     */
    private void onFailure() {
        lastFailureTime.set(System.currentTimeMillis());
        int failures = failureCount.incrementAndGet();
        State currentState = state.get();
        
        log.warn("Circuit breaker [{}] failure count: {}/{}", name, failures, failureThreshold);
        
        if (currentState == State.HALF_OPEN) {
            // 半开状态下失败则立即打开
            log.warn("Circuit breaker [{}] transitioning from HALF_OPEN to OPEN", name);
            state.set(State.OPEN);
            successCount.set(0);
        } else if (currentState == State.CLOSED && failures >= failureThreshold) {
            // 关闭状态下达到阈值则打开
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
