package io.leavesfly.evox.resilience;

import io.leavesfly.evox.core.exception.ExecutionException;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
     * 执行带超时和重试。
     * 使用独立线程 + Future.get(timeout) 实现真正的超时控制，
     * 能够在超时后中断正在执行的任务。
     *
     * @param callable 可调用对象
     * @param timeout 超时时间
     * @param <T> 返回类型
     * @return 执行结果
     */
    public <T> T executeWithTimeout(Callable<T> callable, Duration timeout) {
        return execute(() -> {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<T> future = executor.submit(callable);
            try {
                return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                throw new ExecutionException(
                        "Execution timeout after " + timeout.toMillis() + "ms", e);
            } catch (java.util.concurrent.ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof Exception) {
                    throw (Exception) cause;
                }
                throw new ExecutionException("Execution failed", cause);
            } finally {
                executor.shutdownNow();
            }
        });
    }

    /**
     * 获取策略
     */
    public RetryPolicy getPolicy() {
        return policy;
    }
}
