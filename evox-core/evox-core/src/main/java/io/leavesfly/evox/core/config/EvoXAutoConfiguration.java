package io.leavesfly.evox.core.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * EvoX 自动配置类
 * 
 * <p>自动配置 EvoX 框架的核心组件</p>
 * 
 * @author EvoX Team
 * @since 1.0.0
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(EvoXProperties.class)
public class EvoXAutoConfiguration {

    /**
     * 配置属性
     */
    private final EvoXProperties properties;

    public EvoXAutoConfiguration(EvoXProperties properties) {
        this.properties = properties;
        log.info("EvoX Framework Auto-Configuration initialized");
        log.info("LLM Provider: {}", properties.getLlm().getProvider());
        log.info("LLM Temperature: {}", properties.getLlm().getTemperature());
        log.info("LLM Max Tokens: {}", properties.getLlm().getMaxTokens());
    }

    /**
     * 创建配置属性 Bean
     */
    @Bean
    @ConditionalOnProperty(prefix = "evox", name = "enabled", havingValue = "true", matchIfMissing = true)
    public EvoXProperties evoXProperties() {
        return properties;
    }
}
