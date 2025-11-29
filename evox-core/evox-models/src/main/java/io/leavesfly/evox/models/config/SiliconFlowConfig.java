package io.leavesfly.evox.models.config;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * 硅基流动(SiliconFlow) LLM配置
 * 支持通过硅基流动API调用多种开源大模型
 *
 * @author EvoX Team
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class SiliconFlowConfig extends LLMConfig {

    /**
     * 硅基流动API密钥
     */
    private String siliconflowKey;

    /**
     * 最大完成token数
     */
    private Integer maxCompletionTokens;

    /**
     * 生成结果数量
     */
    private Integer n;

    /**
     * 流式输出选项
     */
    private Object streamOptions;

    /**
     * 工具列表 (Function Calling)
     */
    private List<Object> tools;

    /**
     * 工具选择策略
     */
    private String toolChoice;

    /**
     * 是否启用并行工具调用
     */
    private Boolean parallelToolCalls;

    /**
     * 是否返回token的对数概率
     */
    private Boolean logprobs;

    /**
     * 返回最可能的N个token及其对数概率
     */
    private Integer topLogprobs;

    /**
     * 响应格式
     * 支持JSON Schema等结构化输出
     */
    private Object responseFormat;

    /**
     * 默认构造函数
     * 设置默认模型为Qwen2.5-7B-Instruct
     */
    public SiliconFlowConfig() {
        setProvider("siliconflow");
        setModel("Qwen/Qwen2.5-7B-Instruct");
        setBaseUrl("https://api.siliconflow.cn/v1");
    }

    /**
     * 验证配置有效性
     *
     * @return true if configuration is valid
     */
    public boolean isValid() {
        return (siliconflowKey != null && !siliconflowKey.isEmpty()) ||
               (getApiKey() != null && !getApiKey().isEmpty());
    }

    /**
     * 获取有效的API Key
     *
     * @return API Key
     */
    public String getEffectiveApiKey() {
        return siliconflowKey != null ? siliconflowKey : getApiKey();
    }
}
