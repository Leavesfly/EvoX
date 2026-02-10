package io.leavesfly.evox.agents.resilience;

import io.leavesfly.evox.core.exception.ExecutionException;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.Callable;

/**
 * 重试执行器
 * 提供同步的重试机制
 *
 * @author EvoX Team
 */
@Slf4j
public class RetryExecutor {

    private final RetryPolicy policy;

    public RetryExecutor(RetryPolicy policy) {
        this.policy = policy;
    }

    /**
     * 使用默认策略创建
     */
    public static RetryExecutor withDefaultPolicy() {
        return new RetryExecutor(RetryPolicy.defaultPolicy());
    }

    /**
     * 同步执行带重试
     *
     * @param callable 可调用对象
     * @param <T> 返回类型
     * @return 执行结果
     * @throws ExecutionException 如果所有重试都失败
     */
    public <T> T execute(Callable<T> callable) {
        int attempt = 0;
        Throwable lastException = null;

        while (attempt < policy.getMaxAttempts()) {
            attempt++;
            try {
                log.debug("Executing attempt {}/{}", attempt, policy.getMaxAttempts());
                T result = callable.call();
                if (attempt > 1) {
                    log.info("Execution succeeded on attempt {}", attempt);
                }
                return result;
            } catch (Throwable e) {
                lastException = e;

                if (!policy.isRetryable(e)) {
                    log.warn("Exception is not retryable: {}", e.getMessage());
                    throw new ExecutionException("Execution failed with non-retryable exception", e);
                }

                if (attempt < policy.getMaxAttempts()) {
                    Duration delay = policy.calculateDelay(attempt);
                    log.warn("Attempt {} failed, retrying in {}ms: {}",
                            attempt, delay.toMillis(), e.getMessage());

                    try {
                        Thread.sleep(delay.toMillis());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new ExecutionException("Retry interrupted", ie);
                    }
                } else {
                    log.error("All {} attempts failed", policy.getMaxAttempts());
                }
            }
        }

        throw new ExecutionException(
                String.format("Execution failed after %d attempts", policy.getMaxAttempts()),
                lastException
        );
    }

    /**
     * 执行带超时和重试
     *
     * @param callable 可调用对象
     * @param timeout 超时时间
     * @param <T> 返回类型
     * @return 执行结果
     */
    public <T> T executeWithTimeout(Callable<T> callable, Duration timeout) {
        return execute(() -> {
            long startTime = System.currentTimeMillis();
            T result = callable.call();
            long elapsed = System.currentTimeMillis() - startTime;

            if (elapsed > timeout.toMillis()) {
                throw new ExecutionException("Execution timeout after " + elapsed + "ms");
            }

            return result;
        });
    }

    /**
     * 获取策略
     */
    public RetryPolicy getPolicy() {
        return policy;
    }
}
