package io.leavesfly.evox.core.retry;

import io.leavesfly.evox.core.exception.ExecutionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 重试机制单元测试
 * 
 * @author EvoX Team
 */
@DisplayName("重试机制测试")
class RetryTest {

    @Test
    @DisplayName("测试 RetryPolicy 默认配置")
    void testRetryPolicyDefaultConfiguration() {
        // When
        RetryPolicy policy = new RetryPolicy();
        
        // Then
        assertEquals(3, policy.getMaxAttempts(), "默认最大重试次数应为 3");
        assertEquals(Duration.ofSeconds(1), policy.getInitialDelay(), 
                "默认初始延迟应为 1 秒");
        assertEquals(2.0, policy.getBackoffMultiplier(), 
                "默认退避倍数应为 2.0");
        assertEquals(Duration.ofSeconds(30), policy.getMaxDelay(), 
                "默认最大延迟应为 30 秒");
    }

    @Test
    @DisplayName("测试 RetryPolicy 自定义配置")
    void testRetryPolicyCustomConfiguration() {
        // When
        RetryPolicy policy = RetryPolicy.builder()
                .maxAttempts(5)
                .initialDelay(Duration.ofMillis(500))
                .backoffMultiplier(1.5)
                .maxDelay(Duration.ofSeconds(60))
                .build();
        
        // Then
        assertEquals(5, policy.getMaxAttempts());
        assertEquals(Duration.ofMillis(500), policy.getInitialDelay());
        assertEquals(1.5, policy.getBackoffMultiplier());
        assertEquals(Duration.ofSeconds(60), policy.getMaxDelay());
    }

    @Test
    @DisplayName("测试指数退避延迟计算")
    void testExponentialBackoffCalculation() {
        // Given
        RetryPolicy policy = RetryPolicy.builder()
                .initialDelay(Duration.ofMillis(100))
                .backoffMultiplier(2.0)
                .maxDelay(Duration.ofSeconds(10))
                .build();
        
        // When & Then
        assertEquals(100, policy.calculateDelay(1).toMillis(), 
                "第1次重试延迟应为 100ms");
        assertEquals(200, policy.calculateDelay(2).toMillis(), 
                "第2次重试延迟应为 200ms");
        assertEquals(400, policy.calculateDelay(3).toMillis(), 
                "第3次重试延迟应为 400ms");
        assertEquals(800, policy.calculateDelay(4).toMillis(), 
                "第4次重试延迟应为 800ms");
    }

    @Test
    @DisplayName("测试延迟上限限制")
    void testDelayMaxLimit() {
        // Given
        RetryPolicy policy = RetryPolicy.builder()
                .initialDelay(Duration.ofMillis(1000))
                .backoffMultiplier(10.0)
                .maxDelay(Duration.ofSeconds(5))
                .build();
        
        // When
        Duration delay = policy.calculateDelay(5);
        
        // Then
        assertTrue(delay.toMillis() <= 5000, 
                "延迟应不超过最大延迟 5000ms");
    }

    @Test
    @DisplayName("测试是否可重试判断 - 默认所有异常可重试")
    void testIsRetryableDefault() {
        // Given
        RetryPolicy policy = new RetryPolicy();
        
        // Then
        assertTrue(policy.isRetryable(new RuntimeException()), 
                "默认 RuntimeException 应可重试");
        assertTrue(policy.isRetryable(new ExecutionException("test")), 
                "默认 ExecutionException 应可重试");
        assertTrue(policy.isRetryable(new Exception()), 
                "默认 Exception 应可重试");
    }

    @Test
    @DisplayName("测试 RetryExecutor 成功执行（无需重试）")
    void testRetryExecutorSuccessNoRetry() throws Exception {
        // Given
        RetryPolicy policy = new RetryPolicy();
        RetryExecutor executor = new RetryExecutor(policy);
        Callable<String> callable = () -> "success";
        
        // When
        String result = executor.execute(callable);
        
        // Then
        assertEquals("success", result);
    }

    @Test
    @DisplayName("测试 RetryExecutor 失败后重试成功")
    void testRetryExecutorRetrySuccess() throws Exception {
        // Given
        RetryPolicy policy = RetryPolicy.builder()
                .maxAttempts(3)
                .initialDelay(Duration.ofMillis(10))
                .build();
        RetryExecutor executor = new RetryExecutor(policy);
        
        AtomicInteger attempts = new AtomicInteger(0);
        Callable<String> callable = () -> {
            int attempt = attempts.incrementAndGet();
            if (attempt < 3) {
                throw new RuntimeException("模拟失败 " + attempt);
            }
            return "成功";
        };
        
        // When
        String result = executor.execute(callable);
        
        // Then
        assertEquals("成功", result);
        assertEquals(3, attempts.get(), "应尝试 3 次");
    }

    @Test
    @DisplayName("测试 RetryExecutor 所有重试都失败")
    void testRetryExecutorAllAttemptsFail() {
        // Given
        RetryPolicy policy = RetryPolicy.builder()
                .maxAttempts(3)
                .initialDelay(Duration.ofMillis(10))
                .build();
        RetryExecutor executor = new RetryExecutor(policy);
        
        Callable<String> callable = () -> {
            throw new RuntimeException("总是失败");
        };
        
        // When & Then
        ExecutionException exception = assertThrows(ExecutionException.class, 
                () -> executor.execute(callable));
        
        assertTrue(exception.getMessage().contains("3 attempts"), 
                "异常消息应包含重试次数");
    }

    @Test
    @DisplayName("测试 RetryExecutor 非可重试异常")
    void testRetryExecutorNonRetryableException() {
        // Given
        RetryPolicy policy = RetryPolicy.builder()
                .maxAttempts(3)
                .retryableExceptions(ExecutionException.class)
                .build();
        RetryExecutor executor = new RetryExecutor(policy);
        
        Callable<String> callable = () -> {
            throw new IllegalArgumentException("不可重试的异常");
        };
        
        // When & Then
        ExecutionException exception = assertThrows(ExecutionException.class, 
                () -> executor.execute(callable));
        
        assertTrue(exception.getMessage().contains("non-retryable"), 
                "应提示异常不可重试");
    }

    @Test
    @DisplayName("测试 RetryExecutor 重试间隔")
    void testRetryExecutorDelayBetweenRetries() {
        // Given
        RetryPolicy policy = RetryPolicy.builder()
                .maxAttempts(3)
                .initialDelay(Duration.ofMillis(100))
                .build();
        RetryExecutor executor = new RetryExecutor(policy);
        
        AtomicInteger attempts = new AtomicInteger(0);
        long[] timestamps = new long[3];
        
        Callable<String> callable = () -> {
            int attempt = attempts.getAndIncrement();
            timestamps[attempt] = System.currentTimeMillis();
            if (attempt < 2) {
                throw new RuntimeException("失败");
            }
            return "成功";
        };
        
        // When
        assertDoesNotThrow(() -> executor.execute(callable));
        
        // Then
        assertTrue(timestamps[1] - timestamps[0] >= 90, 
                "第一次重试应有至少 90ms 延迟");
        assertTrue(timestamps[2] - timestamps[1] >= 90, 
                "第二次重试应有至少 90ms 延迟");
    }

    @Test
    @DisplayName("测试 RetryPolicy Builder 模式")
    void testRetryPolicyBuilder() {
        // When
        RetryPolicy policy = RetryPolicy.builder()
                .maxAttempts(5)
                .initialDelay(Duration.ofMillis(200))
                .backoffMultiplier(1.5)
                .maxDelay(Duration.ofSeconds(10))
                .retryableExceptions(RuntimeException.class, ExecutionException.class)
                .build();
        
        // Then
        assertNotNull(policy);
        assertEquals(5, policy.getMaxAttempts());
        assertEquals(200, policy.getInitialDelay().toMillis());
    }

    @Test
    @DisplayName("测试获取 RetryPolicy")
    void testGetRetryPolicy() {
        // Given
        RetryPolicy policy = new RetryPolicy();
        RetryExecutor executor = new RetryExecutor(policy);
        
        // When
        RetryPolicy retrievedPolicy = executor.getPolicy();
        
        // Then
        assertSame(policy, retrievedPolicy);
    }
}
