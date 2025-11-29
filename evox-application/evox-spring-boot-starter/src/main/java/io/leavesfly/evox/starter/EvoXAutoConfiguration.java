package io.leavesfly.evox.starter;

import io.leavesfly.evox.memory.shortterm.ShortTermMemory;
import io.leavesfly.evox.models.base.BaseLLM;
import io.leavesfly.evox.models.config.OpenAILLMConfig;
import io.leavesfly.evox.models.openai.OpenAILLM;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * EvoX 框架自动配置类
 * 
 * <p>当引入 evox-spring-boot-starter 依赖时，自动配置 EvoX 核心组件</p>
 * 
 * <h3>自动配置内容:</h3>
 * <ul>
 *   <li>LLM 实例（根据配置自动创建 OpenAI/DashScope 等）</li>
 *   <li>Memory 实例（短期记忆/长期记忆）</li>
 *   <li>Toolkit 实例（工具集）</li>
 * </ul>
 * 
 * <h3>使用方式:</h3>
 * <pre>{@code
 * @SpringBootApplication
 * public class MyApp {
 *     @Autowired
 *     private BaseLLM llm;  // 自动注入
 *     
 *     @Autowired
 *     private ShortTermMemory memory;  // 自动注入
 * }
 * }</pre>
 * 
 * @author EvoX Team
 * @since 1.0.0
 */
@Slf4j
@Configuration
@ConditionalOnClass(BaseLLM.class)
@EnableConfigurationProperties(EvoXProperties.class)
@ConditionalOnProperty(prefix = "evox", name = "enabled", havingValue = "true", matchIfMissing = true)
public class EvoXAutoConfiguration {
    
    private final EvoXProperties properties;
    
    public EvoXAutoConfiguration(EvoXProperties properties) {
        this.properties = properties;
        log.info("EvoX Auto-Configuration initialized with provider: {}", properties.getLlm().getProvider());
    }
    
    /**
     * 自动配置 BaseLLM
     * 
     * <p>根据配置自动创建 LLM 实例</p>
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "evox.llm", name = "provider", havingValue = "openai", matchIfMissing = true)
    public BaseLLM openAILLM() {
        log.info("Auto-configuring OpenAI LLM with model: {}", properties.getLlm().getModel());
        
        String apiKey = properties.getLlm().getApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            apiKey = System.getenv("OPENAI_API_KEY");
        }
        
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("OpenAI API Key is not configured. Please set evox.llm.api-key or OPENAI_API_KEY environment variable.");
            return null;
        }
        
        OpenAILLMConfig config = OpenAILLMConfig.builder()
            .apiKey(apiKey)
            .model(properties.getLlm().getModel())
            .temperature(properties.getLlm().getTemperature())
            .maxTokens(properties.getLlm().getMaxTokens())
            .build();
        
        return new OpenAILLM(config);
    }
    
    /**
     * 自动配置短期记忆
     */
    @Bean
    @ConditionalOnMissingBean
    public ShortTermMemory shortTermMemory() {
        log.info("Auto-configuring ShortTermMemory with capacity: {}, windowSize: {}", 
            properties.getMemory().getShortTerm().getCapacity(),
            properties.getMemory().getShortTerm().getWindowSize());
        
        return new ShortTermMemory(properties.getMemory().getShortTerm().getCapacity());
    }
    
    /**
     * 启动时打印欢迎信息
     */
    @Bean
    public EvoXBanner evoXBanner() {
        return new EvoXBanner();
    }
    
    /**
     * EvoX 欢迎横幅
     */
    public static class EvoXBanner {
        public EvoXBanner() {
            log.info("\n" +
                "╔═══════════════════════════════════════════╗\n" +
                "║                                           ║\n" +
                "║   ███████╗██╗   ██╗ ██████╗ ██╗  ██╗    ║\n" +
                "║   ██╔════╝██║   ██║██╔═══██╗╚██╗██╔╝    ║\n" +
                "║   █████╗  ██║   ██║██║   ██║ ╚███╔╝     ║\n" +
                "║   ██╔══╝  ╚██╗ ██╔╝██║   ██║ ██╔██╗     ║\n" +
                "║   ███████╗ ╚████╔╝ ╚██████╔╝██╔╝ ██╗    ║\n" +
                "║   ╚══════╝  ╚═══╝   ╚═════╝ ╚═╝  ╚═╝    ║\n" +
                "║                                           ║\n" +
                "║   Enterprise AI Agent Framework           ║\n" +
                "║   Spring Boot Auto-Configuration Ready   ║\n" +
                "║                                           ║\n" +
                "╚═══════════════════════════════════════════╝");
        }
    }
}
