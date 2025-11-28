package io.leavesfly.evox.models.config;

import io.leavesfly.evox.core.module.BaseModule;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.time.Duration;

/**
 * LLM配置基类
 * 所有LLM配置的基础类
 *
 * @author EvoX Team
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public abstract class LLMConfig extends BaseModule {

    /**
     * 提供商标识
     */
    private String provider;

    /**
     * 模型名称
     */
    private String model;

    /**
     * API密钥
     */
    private String apiKey;

    /**
     * API基础URL
     */
    private String baseUrl;

    /**
     * 温度参数(0.0-2.0)
     */
    private Float temperature;

    /**
     * 最大token数
     */
    private Integer maxTokens;

    /**
     * Top-p采样参数
     */
    private Float topP;

    /**
     * 频率惩罚
     */
    private Float frequencyPenalty;

    /**
     * 存在惩罚
     */
    private Float presencePenalty;

    /**
     * 是否启用流式输出
     */
    private Boolean stream = false;

    /**
     * 请求超时时间
     */
    private Duration timeout = Duration.ofSeconds(60);

    /**
     * 是否输出响应到控制台
     */
    private Boolean outputResponse;

    public LLMConfig() {
        super();
    }
}
