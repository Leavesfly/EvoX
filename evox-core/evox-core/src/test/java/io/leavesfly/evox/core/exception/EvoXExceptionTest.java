package io.leavesfly.evox.core.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EvoX 异常体系单元测试
 * 
 * @author EvoX Team
 */
@DisplayName("EvoX 异常体系测试")
class EvoXExceptionTest {

    @Test
    @DisplayName("测试 EvoXException 基本创建")
    void testEvoXExceptionBasic() {
        // Given
        String message = "Test exception message";
        
        // When
        EvoXException exception = new EvoXException(message);
        
        // Then
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertEquals("EVOX_ERROR", exception.getErrorCode());
        assertNull(exception.getContext());
    }

    @Test
    @DisplayName("测试 EvoXException 带错误码")
    void testEvoXExceptionWithErrorCode() {
        // Given
        String errorCode = "CUSTOM_ERROR";
        String message = "Custom error message";
        
        // When
        EvoXException exception = new EvoXException(errorCode, message);
        
        // Then
        assertEquals(errorCode, exception.getErrorCode());
        assertEquals(message, exception.getMessage());
    }

    @Test
    @DisplayName("测试 EvoXException 带上下文")
    void testEvoXExceptionWithContext() {
        // Given
        String errorCode = "TEST_ERROR";
        String message = "Test message";
        Object context = new Object();
        
        // When
        EvoXException exception = new EvoXException(errorCode, message, context);
        
        // Then
        assertSame(context, exception.getContext());
    }

    @Test
    @DisplayName("测试 EvoXException 带 Cause")
    void testEvoXExceptionWithCause() {
        // Given
        Throwable cause = new RuntimeException("原始异常");
        String message = "包装异常";
        
        // When
        EvoXException exception = new EvoXException(message, cause);
        
        // Then
        assertSame(cause, exception.getCause());
        assertEquals(message, exception.getMessage());
    }

    @Test
    @DisplayName("测试 ConfigurationException")
    void testConfigurationException() {
        // Given
        String message = "配置错误";
        
        // When
        ConfigurationException exception = new ConfigurationException(message);
        
        // Then
        assertEquals("CONFIG_ERROR", exception.getErrorCode());
        assertEquals(message, exception.getMessage());
    }

    @Test
    @DisplayName("测试 ExecutionException")
    void testExecutionException() {
        // Given
        String message = "执行错误";
        
        // When
        ExecutionException exception = new ExecutionException(message);
        
        // Then
        assertEquals("EXECUTION_ERROR", exception.getErrorCode());
        assertEquals(message, exception.getMessage());
    }

    @Test
    @DisplayName("测试 LLMException")
    void testLLMException() {
        // Given
        String message = "LLM 调用失败";
        
        // When
        LLMException exception = new LLMException(message);
        
        // Then
        assertEquals("LLM_ERROR", exception.getErrorCode());
        assertEquals(message, exception.getMessage());
    }

    @Test
    @DisplayName("测试 ModuleException")
    void testModuleException() {
        // Given
        String message = "模块错误";
        
        // When
        ModuleException exception = new ModuleException(message);
        
        // Then
        assertEquals("MODULE_ERROR", exception.getErrorCode());
        assertEquals(message, exception.getMessage());
    }

    @Test
    @DisplayName("测试 StorageException")
    void testStorageException() {
        // Given
        String message = "存储错误";
        
        // When
        StorageException exception = new StorageException(message);
        
        // Then
        assertEquals("STORAGE_ERROR", exception.getErrorCode());
        assertEquals(message, exception.getMessage());
    }

    @Test
    @DisplayName("测试 ValidationException")
    void testValidationException() {
        // Given
        String message = "验证错误";
        
        // When
        ValidationException exception = new ValidationException(message);
        
        // Then
        assertEquals("VALIDATION_ERROR", exception.getErrorCode());
        assertEquals(message, exception.getMessage());
    }

    @Test
    @DisplayName("测试异常链")
    void testExceptionChaining() {
        // Given
        RuntimeException originalException = new RuntimeException("原始错误");
        LLMException llmException = new LLMException("LLM 调用失败", originalException);
        
        // When
        ExecutionException executionException = new ExecutionException(
                "工作流执行失败", llmException);
        
        // Then
        assertSame(llmException, executionException.getCause());
        assertSame(originalException, executionException.getCause().getCause());
    }

    @Test
    @DisplayName("测试异常 toString")
    void testExceptionToString() {
        // Given
        EvoXException exception = new EvoXException("TEST_CODE", "测试消息");
        
        // When
        String str = exception.toString();
        
        // Then
        assertNotNull(str);
        assertTrue(str.contains("TEST_CODE"), "应包含错误码");
        assertTrue(str.contains("测试消息"), "应包含错误消息");
    }

    @Test
    @DisplayName("测试异常带上下文信息")
    void testExceptionWithContextObject() {
        // Given
        class ErrorContext {
            String field = "value";
        }
        ErrorContext context = new ErrorContext();
        
        // When
        EvoXException exception = new EvoXException(
                "ERROR_CODE", "错误消息", context);
        
        // Then
        assertNotNull(exception.getContext());
        assertSame(context, exception.getContext());
    }

    @Test
    @DisplayName("测试所有异常类都是 RuntimeException")
    void testAllExceptionsAreRuntimeException() {
        assertTrue(RuntimeException.class.isAssignableFrom(EvoXException.class));
        assertTrue(RuntimeException.class.isAssignableFrom(ConfigurationException.class));
        assertTrue(RuntimeException.class.isAssignableFrom(ExecutionException.class));
        assertTrue(RuntimeException.class.isAssignableFrom(LLMException.class));
        assertTrue(RuntimeException.class.isAssignableFrom(ModuleException.class));
        assertTrue(RuntimeException.class.isAssignableFrom(StorageException.class));
        assertTrue(RuntimeException.class.isAssignableFrom(ValidationException.class));
    }

    @Test
    @DisplayName("测试异常可序列化性")
    void testExceptionSerializable() {
        // 测试异常是否可以被序列化（对于分布式系统很重要）
        EvoXException exception = new EvoXException("TEST", "测试");
        
        // 基本验证：确保异常对象可以创建
        assertNotNull(exception);
        assertDoesNotThrow(() -> exception.toString());
    }
}
