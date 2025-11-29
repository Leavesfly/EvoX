package io.leavesfly.evox.core.retry;

import lombok.Builder;
import lombok.Data;

import java.time.Duration;
import java.util.function.Predicate;

/**
 * 重试策略配置
 *
 * @author EvoX Team
 */
@Data
@Builder
public class RetryPolicy {

    /**
     * 最大重试次数
     */
    @Builder.Default
    private int maxAttempts = 3;

    /**
     * 初始延迟
     */
    @Builder.Default
    private Duration initialDelay = Duration.ofMillis(100);

    /**
     * 最大延迟
     */
    @Builder.Default
    private Duration maxDelay = Duration.ofSeconds(10);

    /**
     * 延迟倍数（指数退避）
     */
    @Builder.Default
    private double backoffMultiplier = 2.0;

    /**
     * 是否使用抖动
     */
    @Builder.Default
    private boolean useJitter = true;

    /**
     * 可重试的异常类型
     */
    @Builder.Default
    private Predicate<Throwable> retryableException = throwable -> true;

    /**
     * 默认重试策略
     */
    public static RetryPolicy defaultPolicy() {
        return RetryPolicy.builder().build();
    }

    /**
     * 快速重试策略（低延迟）
     */
    public static RetryPolicy fastRetry() {
        return RetryPolicy.builder()
                .maxAttempts(3)
                .initialDelay(Duration.ofMillis(50))
                .maxDelay(Duration.ofSeconds(1))
                .backoffMultiplier(1.5)
                .build();
    }

    /**
     * 稳健重试策略（高可靠性）
     */
    public static RetryPolicy robustRetry() {
        return RetryPolicy.builder()
                .maxAttempts(5)
                .initialDelay(Duration.ofMillis(500))
                .maxDelay(Duration.ofSeconds(30))
                .backoffMultiplier(2.0)
                .build();
    }

    /**
     * 计算下次重试延迟
     *
     * @param attempt 当前尝试次数（从1开始）
     * @return 延迟时间
     */
    public Duration calculateDelay(int attempt) {
        if (attempt <= 0) {
            return Duration.ZERO;
        }

        // 指数退避
        long delayMillis = (long) (initialDelay.toMillis() * Math.pow(backoffMultiplier, attempt - 1));
        
        // 限制最大延迟
        delayMillis = Math.min(delayMillis, maxDelay.toMillis());

        // 添加抖动
        if (useJitter) {
            delayMillis = (long) (delayMillis * (0.5 + Math.random() * 0.5));
        }

        return Duration.ofMillis(delayMillis);
    }

    /**
     * 判断异常是否可重试
     */
    public boolean isRetryable(Throwable throwable) {
        return retryableException.test(throwable);
    }
}
