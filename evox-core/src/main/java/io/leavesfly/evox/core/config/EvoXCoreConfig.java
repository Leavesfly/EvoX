package io.leavesfly.evox.core.config;

import io.leavesfly.evox.core.registry.ModuleRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * EvoX核心模块配置类
 *
 * @author EvoX Team
 */
@Configuration
public class EvoXCoreConfig {

    /**
     * 创建模块注册表Bean
     */
    @Bean
    public ModuleRegistry moduleRegistry() {
        return new ModuleRegistry();
    }
}
