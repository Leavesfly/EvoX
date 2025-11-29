package io.leavesfly.evox.core.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EvoX 配置属性测试
 * 
 * @author EvoX Team
 */
@SpringBootTest(classes = {EvoXProperties.class})
@EnableConfigurationProperties(EvoXProperties.class)
@TestPropertySource(properties = {
    "evox.llm.provider=openai",
    "evox.llm.temperature=0.7",
    "evox.llm.max-tokens=2000",
    "evox.llm.timeout=30000",
    "evox.agents.default-timeout=60000",
    "evox.agents.max-concurrent=10",
    "evox.memory.short-term.capacity=100",
    "evox.memory.short-term.window-size=10"
})
class EvoXPropertiesTest {

    private EvoXProperties properties;

    @BeforeEach
    void setUp() {
        properties = new EvoXProperties();
        // 手动设置属性用于测试
        properties.getLlm().setProvider("openai");
        properties.getLlm().setTemperature(0.7);
        properties.getLlm().setMaxTokens(2000);
        properties.getLlm().setTimeout(30000L);
        
        properties.getAgents().setDefaultTimeout(60000L);
        properties.getAgents().setMaxConcurrent(10);
        
        properties.getMemory().getShortTerm().setCapacity(100);
        properties.getMemory().getShortTerm().setWindowSize(10);
    }

    @Test
    void testLlmConfiguration() {
        assertNotNull(properties.getLlm(), "LLM 配置不应为 null");
        assertEquals("openai", properties.getLlm().getProvider(), "LLM 提供商应为 openai");
        assertEquals(0.7, properties.getLlm().getTemperature(), "Temperature 应为 0.7");
        assertEquals(2000, properties.getLlm().getMaxTokens(), "MaxTokens 应为 2000");
        assertEquals(30000L, properties.getLlm().getTimeout(), "Timeout 应为 30000");
    }

    @Test
    void testLlmRetryConfiguration() {
        EvoXProperties.RetryConfig retry = properties.getLlm().getRetry();
        assertNotNull(retry, "重试配置不应为 null");
        assertEquals(3, retry.getMaxAttempts(), "默认最大重试次数应为 3");
        assertEquals(1000L, retry.getInitialDelay(), "默认初始延迟应为 1000ms");
        assertEquals(10000L, retry.getMaxDelay(), "默认最大延迟应为 10000ms");
    }

    @Test
    void testAgentsConfiguration() {
        assertNotNull(properties.getAgents(), "Agents 配置不应为 null");
        assertEquals(60000L, properties.getAgents().getDefaultTimeout(), "默认超时应为 60000ms");
        assertEquals(10, properties.getAgents().getMaxConcurrent(), "最大并发应为 10");
    }

    @Test
    void testMemoryConfiguration() {
        assertNotNull(properties.getMemory(), "Memory 配置不应为 null");
        
        // 测试短期记忆配置
        EvoXProperties.ShortTermConfig shortTerm = properties.getMemory().getShortTerm();
        assertNotNull(shortTerm, "短期记忆配置不应为 null");
        assertEquals(100, shortTerm.getCapacity(), "容量应为 100");
        assertEquals(10, shortTerm.getWindowSize(), "窗口大小应为 10");
        
        // 测试长期记忆配置
        EvoXProperties.LongTermConfig longTerm = properties.getMemory().getLongTerm();
        assertNotNull(longTerm, "长期记忆配置不应为 null");
        assertTrue(longTerm.getEnabled(), "长期记忆应默认启用");
        assertEquals("in-memory", longTerm.getStorageType(), "默认存储类型应为 in-memory");
    }

    @Test
    void testStorageConfiguration() {
        assertNotNull(properties.getStorage(), "Storage 配置不应为 null");
        assertEquals("in-memory", properties.getStorage().getType(), "默认存储类型应为 in-memory");
        
        // 测试向量存储配置
        EvoXProperties.VectorConfig vector = properties.getStorage().getVector();
        assertNotNull(vector, "向量存储配置不应为 null");
        assertFalse(vector.getEnabled(), "向量存储应默认禁用");
        assertEquals("in-memory", vector.getProvider(), "默认向量存储提供商应为 in-memory");
        assertEquals(1536, vector.getDimension(), "默认向量维度应为 1536");
    }

    @Test
    void testWorkflowConfiguration() {
        assertNotNull(properties.getWorkflow(), "Workflow 配置不应为 null");
        assertEquals(10, properties.getWorkflow().getMaxDepth(), "最大深度应为 10");
        assertEquals(300000L, properties.getWorkflow().getTimeout(), "超时应为 300000ms");
        assertTrue(properties.getWorkflow().getEnableParallel(), "并行应默认启用");
    }

    @Test
    void testToolsConfiguration() {
        assertNotNull(properties.getTools(), "Tools 配置不应为 null");
        assertTrue(properties.getTools().getEnabled(), "工具应默认启用");
        assertEquals(30000L, properties.getTools().getTimeout(), "超时应为 30000ms");
        assertEquals(3, properties.getTools().getMaxRetries(), "最大重试次数应为 3");
    }

    @Test
    void testBenchmarkConfiguration() {
        assertNotNull(properties.getBenchmark(), "Benchmark 配置不应为 null");
        assertEquals(3, properties.getBenchmark().getWarmupIterations(), "预热迭代次数应为 3");
        assertEquals(10, properties.getBenchmark().getMeasurementIterations(), "测量迭代次数应为 10");
        assertEquals(300000L, properties.getBenchmark().getTimeout(), "超时应为 300000ms");
        assertEquals(1, properties.getBenchmark().getParallelThreads(), "并行线程数应为 1");
        assertEquals("./benchmark-results", properties.getBenchmark().getOutputDirectory(), 
                     "输出目录应为 ./benchmark-results");
    }

    @Test
    void testDurationConversion() {
        assertEquals(30000L, properties.getLlmTimeoutDuration().toMillis(), 
                     "LLM 超时 Duration 应为 30000ms");
        assertEquals(60000L, properties.getAgentTimeoutDuration().toMillis(), 
                     "Agent 超时 Duration 应为 60000ms");
        assertEquals(300000L, properties.getWorkflowTimeoutDuration().toMillis(), 
                     "Workflow 超时 Duration 应为 300000ms");
        assertEquals(30000L, properties.getToolsTimeoutDuration().toMillis(), 
                     "Tools 超时 Duration 应为 30000ms");
    }

    @Test
    void testDefaultValues() {
        // 创建新的配置对象，测试默认值
        EvoXProperties defaultProps = new EvoXProperties();
        
        // LLM 默认值
        assertEquals("openai", defaultProps.getLlm().getProvider());
        assertEquals(0.7, defaultProps.getLlm().getTemperature());
        assertEquals(2000, defaultProps.getLlm().getMaxTokens());
        assertEquals(30000L, defaultProps.getLlm().getTimeout());
        
        // Agents 默认值
        assertEquals(60000L, defaultProps.getAgents().getDefaultTimeout());
        assertEquals(10, defaultProps.getAgents().getMaxConcurrent());
        
        // Memory 默认值
        assertEquals(100, defaultProps.getMemory().getShortTerm().getCapacity());
        assertEquals(10, defaultProps.getMemory().getShortTerm().getWindowSize());
        assertTrue(defaultProps.getMemory().getLongTerm().getEnabled());
        
        // Storage 默认值
        assertEquals("in-memory", defaultProps.getStorage().getType());
        assertFalse(defaultProps.getStorage().getVector().getEnabled());
        
        // Workflow 默认值
        assertEquals(10, defaultProps.getWorkflow().getMaxDepth());
        assertTrue(defaultProps.getWorkflow().getEnableParallel());
        
        // Tools 默认值
        assertTrue(defaultProps.getTools().getEnabled());
        assertEquals(3, defaultProps.getTools().getMaxRetries());
    }
}
