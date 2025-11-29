package io.leavesfly.evox.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * EvoX 框架配置属性
 * 
 * <p>统一管理 EvoX 框架的所有配置项</p>
 * 
 * @author EvoX Team
 * @since 1.0.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "evox")
public class EvoXProperties {

    /**
     * LLM 配置
     */
    private LlmConfig llm = new LlmConfig();

    /**
     * Agent 配置
     */
    private AgentsConfig agents = new AgentsConfig();

    /**
     * Memory 配置
     */
    private MemoryConfig memory = new MemoryConfig();

    /**
     * Storage 配置
     */
    private StorageConfig storage = new StorageConfig();

    /**
     * Workflow 配置
     */
    private WorkflowConfig workflow = new WorkflowConfig();

    /**
     * Tools 配置
     */
    private ToolsConfig tools = new ToolsConfig();

    /**
     * Benchmark 配置
     */
    private BenchmarkConfig benchmark = new BenchmarkConfig();

    /**
     * LLM 配置
     */
    @Data
    public static class LlmConfig {
        /**
         * LLM 提供商: openai, dashscope, litellm
         */
        private String provider = "openai";

        /**
         * 温度参数 (0.0-2.0)
         */
        private Double temperature = 0.7;

        /**
         * 最大 token 数
         */
        private Integer maxTokens = 2000;

        /**
         * 超时时间（毫秒）
         */
        private Long timeout = 30000L;

        /**
         * 重试配置
         */
        private RetryConfig retry = new RetryConfig();
    }

    /**
     * 重试配置
     */
    @Data
    public static class RetryConfig {
        /**
         * 最大重试次数
         */
        private Integer maxAttempts = 3;

        /**
         * 初始延迟（毫秒）
         */
        private Long initialDelay = 1000L;

        /**
         * 最大延迟（毫秒）
         */
        private Long maxDelay = 10000L;
    }

    /**
     * Agent 配置
     */
    @Data
    public static class AgentsConfig {
        /**
         * 默认超时时间（毫秒）
         */
        private Long defaultTimeout = 60000L;

        /**
         * 最大并发数
         */
        private Integer maxConcurrent = 10;
    }

    /**
     * Memory 配置
     */
    @Data
    public static class MemoryConfig {
        /**
         * 短期记忆配置
         */
        private ShortTermConfig shortTerm = new ShortTermConfig();

        /**
         * 长期记忆配置
         */
        private LongTermConfig longTerm = new LongTermConfig();
    }

    /**
     * 短期记忆配置
     */
    @Data
    public static class ShortTermConfig {
        /**
         * 容量
         */
        private Integer capacity = 100;

        /**
         * 窗口大小
         */
        private Integer windowSize = 10;
    }

    /**
     * 长期记忆配置
     */
    @Data
    public static class LongTermConfig {
        /**
         * 是否启用
         */
        private Boolean enabled = true;

        /**
         * 存储类型: in-memory, redis, database
         */
        private String storageType = "in-memory";
    }

    /**
     * Storage 配置
     */
    @Data
    public static class StorageConfig {
        /**
         * 存储类型: in-memory, h2, postgresql
         */
        private String type = "in-memory";

        /**
         * 向量存储配置
         */
        private VectorConfig vector = new VectorConfig();
    }

    /**
     * 向量存储配置
     */
    @Data
    public static class VectorConfig {
        /**
         * 是否启用
         */
        private Boolean enabled = false;

        /**
         * 向量存储提供商: in-memory, qdrant, milvus
         */
        private String provider = "in-memory";

        /**
         * 向量维度
         */
        private Integer dimension = 1536;
    }

    /**
     * Workflow 配置
     */
    @Data
    public static class WorkflowConfig {
        /**
         * 最大深度
         */
        private Integer maxDepth = 10;

        /**
         * 超时时间（毫秒）
         */
        private Long timeout = 300000L;

        /**
         * 是否启用并行
         */
        private Boolean enableParallel = true;
    }

    /**
     * Tools 配置
     */
    @Data
    public static class ToolsConfig {
        /**
         * 是否启用
         */
        private Boolean enabled = true;

        /**
         * 超时时间（毫秒）
         */
        private Long timeout = 30000L;

        /**
         * 最大重试次数
         */
        private Integer maxRetries = 3;
    }

    /**
     * Benchmark 配置
     */
    @Data
    public static class BenchmarkConfig {
        /**
         * 预热迭代次数
         */
        private Integer warmupIterations = 3;

        /**
         * 测量迭代次数
         */
        private Integer measurementIterations = 10;

        /**
         * 超时时间（毫秒）
         */
        private Long timeout = 300000L;

        /**
         * 并行线程数
         */
        private Integer parallelThreads = 1;

        /**
         * 输出目录
         */
        private String outputDirectory = "./benchmark-results";
    }

    /**
     * 获取 LLM 超时 Duration
     */
    public Duration getLlmTimeoutDuration() {
        return Duration.ofMillis(llm.getTimeout());
    }

    /**
     * 获取 Agent 超时 Duration
     */
    public Duration getAgentTimeoutDuration() {
        return Duration.ofMillis(agents.getDefaultTimeout());
    }

    /**
     * 获取 Workflow 超时 Duration
     */
    public Duration getWorkflowTimeoutDuration() {
        return Duration.ofMillis(workflow.getTimeout());
    }

    /**
     * 获取 Tools 超时 Duration
     */
    public Duration getToolsTimeoutDuration() {
        return Duration.ofMillis(tools.getTimeout());
    }
}
