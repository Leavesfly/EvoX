package io.leavesfly.evox.starter;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * EvoX 框架配置属性
 * 
 * <p>通过 application.yml 配置 EvoX 框架行为</p>
 * 
 * <h3>配置示例:</h3>
 * <pre>
 * evox:
 *   enabled: true
 *   llm:
 *     provider: openai
 *     api-key: ${OPENAI_API_KEY}
 *     model: gpt-4o-mini
 *     temperature: 0.7
 *   agents:
 *     default-timeout: 60000
 *   memory:
 *     short-term:
 *       capacity: 100
 *       window-size: 10
 * </pre>
 * 
 * @author EvoX Team
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "evox")
public class EvoXProperties {
    
    /**
     * 是否启用 EvoX 自动配置
     */
    private boolean enabled = true;
    
    /**
     * LLM 配置
     */
    private LLMProperties llm = new LLMProperties();
    
    /**
     * Agent 配置
     */
    private AgentProperties agents = new AgentProperties();
    
    /**
     * Memory 配置
     */
    private MemoryProperties memory = new MemoryProperties();
    
    /**
     * Tools 配置
     */
    private ToolsProperties tools = new ToolsProperties();
    
    /**
     * LLM 配置
     */
    @Data
    public static class LLMProperties {
        /**
         * LLM 提供商 (openai, aliyun, ollama, siliconflow)
         */
        private String provider = "openai";
        
        /**
         * API Key
         */
        private String apiKey;
        
        /**
         * 模型名称
         */
        private String model = "gpt-4o-mini";
        
        /**
         * 温度参数
         */
        private Float temperature = 0.7f;
        
        /**
         * 最大 Token 数
         */
        private Integer maxTokens = 2000;
        
        /**
         * 超时时间（毫秒）
         */
        private Long timeout = 30000L;
    }
    
    /**
     * Agent 配置
     */
    @Data
    public static class AgentProperties {
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
    public static class MemoryProperties {
        /**
         * 短期记忆配置
         */
        private ShortTermMemoryProperties shortTerm = new ShortTermMemoryProperties();
        
        /**
         * 长期记忆配置
         */
        private LongTermMemoryProperties longTerm = new LongTermMemoryProperties();
        
        /**
         * 短期记忆配置
         */
        @Data
        public static class ShortTermMemoryProperties {
            /**
             * 容量
             */
            private Integer capacity = 100;
            
            /**
             * 滑动窗口大小
             */
            private Integer windowSize = 10;
        }
        
        /**
         * 长期记忆配置
         */
        @Data
        public static class LongTermMemoryProperties {
            /**
             * 是否启用
             */
            private Boolean enabled = false;
            
            /**
             * 存储类型 (in-memory, redis, database)
             */
            private String storageType = "in-memory";
        }
    }
    
    /**
     * Tools 配置
     */
    @Data
    public static class ToolsProperties {
        /**
         * 是否启用工具
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
}
