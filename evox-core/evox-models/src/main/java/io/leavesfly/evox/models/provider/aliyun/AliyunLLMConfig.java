package io.leavesfly.evox.models.provider.aliyun;

import io.leavesfly.evox.core.llm.LLMConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * 阿里云百炼(通义千问) LLM配置
 * 支持通过DashScope API调用阿里云大模型服务
 *
 * @author EvoX Team
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class AliyunLLMConfig extends LLMConfig {

    /**
     * 阿里云API密钥 (DashScope API Key)
     */
    private String aliyunApiKey;

    /**
     * 阿里云访问密钥ID (可选,用于SDK认证)
     */
    private String aliyunAccessKeyId;

    /**
     * 阿里云访问密钥Secret (可选,用于SDK认证)
     */
    private String aliyunAccessKeySecret;

    /**
     * Top-k采样参数
     * 仅采样前k个最可能的token
     */
    private Integer topK;

    /**
     * 重复惩罚参数
     * 值越大,模型越倾向于避免重复内容
     */
    private Float repetitionPenalty;

    /**
     * 工具列表 (Function Calling)
     * 阿里云特定模型支持工具调用
     */
    private List<Object> tools;

    /**
     * 工具选择策略
     * 可选值: "none"(不调用工具), "auto"(模型自动决定)
     */
    private String toolChoice;

    /**
     * 模型名称别名
     * 例如: "qwen-max", "qwen-turbo", "qwen-plus"
     */
    private String modelName;

    /**
     * 是否启用网络搜索增强
     * 部分模型支持实时网络搜索
     */
    @lombok.Builder.Default
    private Boolean enableSearch = false;

    /**
     * 响应格式
     * 支持JSON Schema等结构化输出
     */
    private Object responseFormat;

    /**
     * 输出模态类型
     * 支持多模态模型的输出类型配置,如 ["text", "image"]
     */
    private List<String> outputModalities;

    /**
     * 是否返回token的对数概率
     */
    private Boolean logprobs;

    /**
     * 返回最可能的N个token及其对数概率
     * 需要logprobs=true
     */
    private Integer topLogprobs;

    /**
     * 默认构造函数
     * 设置默认模型为 qwen-turbo
     */
    public AliyunLLMConfig() {
        setProvider("aliyun");
        setModel("qwen-turbo");
        setBaseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1");
        if (this.enableSearch == null) {
            this.enableSearch = false;
        }
    }

    /**
     * 验证配置有效性
     *
     * @return true if configuration is valid
     */
    public boolean isValid() {
        return aliyunApiKey != null && !aliyunApiKey.isEmpty();
    }

    /**
     * 获取实际使用的模型名称
     * 优先使用modelName,否则使用model字段
     *
     * @return 模型名称
     */
    public String getEffectiveModelName() {
        return modelName != null ? modelName : getModel();
    }
}
