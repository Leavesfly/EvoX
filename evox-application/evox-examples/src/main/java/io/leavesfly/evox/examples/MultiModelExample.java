package io.leavesfly.evox.examples;

import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.core.message.MessageType;
import io.leavesfly.evox.models.base.BaseLLM;
import io.leavesfly.evox.models.aliyun.AliyunLLM;
import io.leavesfly.evox.models.config.AliyunLLMConfig;
import io.leavesfly.evox.models.config.LiteLLMConfig;
import io.leavesfly.evox.models.litellm.LiteLLM;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 多模型适配器示例
 * 演示如何使用阿里云通义千问和 LiteLLM 通用适配器
 *
 * @author EvoX Team
 */
public class MultiModelExample {
    private static final Logger log = LoggerFactory.getLogger(MultiModelExample.class);

    public static void main(String[] args) {
        MultiModelExample example = new MultiModelExample();
        
        // 示例1：使用阿里云通义千问模型
        example.demonstrateAliyunModel();
        
        // 示例2：使用 LiteLLM 通用适配器
        example.demonstrateLiteLLM();
        
        // 示例3：配置对比
        example.demonstrateConfigComparison();
    }

    /**
     * 示例1：使用阿里云通义千问模型
     */
    private void demonstrateAliyunModel() {
        log.info("\n--- 示例1：阿里云通义千问模型 ---");
        
        // 创建阿里云模型配置
        AliyunLLMConfig config = AliyunLLMConfig.builder()
                .apiKey("your-dashscope-api-key")  // 实际使用时需要替换为真实 API Key
                .model("qwen-turbo")
                .temperature(0.7f)
                .topP(0.9f)
                .maxTokens(1500)
                .enableSearch(false)
                .repetitionPenalty(1.0f)
                .build();
        
        log.info("阿里云配置:");
        log.info("  模型: {}", config.getModel());
        log.info("  Temperature: {}", config.getTemperature());
        log.info("  TopP: {}", config.getTopP());
        log.info("  最大Token数: {}", config.getMaxTokens());
        log.info("  启用搜索: {}", config.getEnableSearch());
        
        // 创建阿里云 LLM 实例
        BaseLLM aliyunLLM = new AliyunLLM(config);
        log.info("✓ 阿里云通义千问模型已初始化");
        
        // 注意：实际调用需要有效的 API Key
        log.info("  提示：实际使用需要配置有效的 DashScope API Key");
    }

    /**
     * 示例2：使用 LiteLLM 通用适配器
     */
    private void demonstrateLiteLLM() {
        log.info("\n--- 示例2：LiteLLM 通用适配器 ---");
        
        // 配置1：使用 OpenAI 兼容接口
        LiteLLMConfig openaiConfig = LiteLLMConfig.builder()
                .apiKey("your-openai-api-key")
                .model("gpt-3.5-turbo")
                .baseUrl("https://api.openai.com/v1")
                .temperature(0.8f)
                .maxTokens(2000)
                .build();
        
        log.info("OpenAI 兼容配置:");
        log.info("  模型: {}", openaiConfig.getModel());
        log.info("  API地址: {}", openaiConfig.getBaseUrl());
        log.info("  Temperature: {}", openaiConfig.getTemperature());
        
        // 配置2：使用本地模型
        LiteLLMConfig localConfig = LiteLLMConfig.builder()
                .model("local-llama2")
                .baseUrl("http://localhost:8080")
                .isLocal(true)
                .temperature(0.7f)
                .build();
        
        log.info("\n本地模型配置:");
        log.info("  模型: {}", localConfig.getModel());
        log.info("  API地址: {}", localConfig.getBaseUrl());
        log.info("  本地模型: {}", localConfig.getIsLocal());
        
        // 配置3：使用 Azure OpenAI
        LiteLLMConfig azureConfig = LiteLLMConfig.builder()
                .model("gpt-4")
                .azureEndpoint("https://your-resource.openai.azure.com")
                .azureKey("your-azure-key")
                .apiVersion("2023-05-15")
                .temperature(0.7f)
                .build();
        
        log.info("\nAzure OpenAI 配置:");
        log.info("  模型: {}", azureConfig.getModel());
        log.info("  Azure Endpoint: {}", azureConfig.getAzureEndpoint());
        log.info("  API版本: {}", azureConfig.getApiVersion());
        
        // 创建 LiteLLM 实例
        BaseLLM liteLLM = new LiteLLM(openaiConfig);
        log.info("\n✓ LiteLLM 通用适配器已初始化");
        log.info("  提示：支持 OpenAI、Azure、本地模型等多种接口");
    }

    /**
     * 示例3：配置对比
     */
    private void demonstrateConfigComparison() {
        log.info("\n--- 示例3：配置对比 ---");
        
        AliyunLLMConfig aliyunConfig = AliyunLLMConfig.builder()
                .temperature(0.8f)
                .maxTokens(2000)
                .enableSearch(true)  // 阿里云特有功能
                .build();
        
        LiteLLMConfig litellmConfig = LiteLLMConfig.builder()
                .temperature(0.7f)
                .maxTokens(1500)
                .isLocal(false)  // LiteLLM 特有功能
                .build();
        
        log.info("配置继承演示:");
        log.info("  阿里云 Temperature: {}, LiteLLM Temperature: {}", 
                aliyunConfig.getTemperature(), litellmConfig.getTemperature());
        log.info("  阿里云 MaxTokens: {}, LiteLLM MaxTokens: {}", 
                aliyunConfig.getMaxTokens(), litellmConfig.getMaxTokens());
        
        log.info("\n特有功能:");
        log.info("  阿里云启用搜索: {}", aliyunConfig.getEnableSearch());
        log.info("  LiteLLM 本地模式: {}", litellmConfig.getIsLocal());
        
        log.info("\n✓ 两种配置都继承自 LLMConfig，共享基础参数");
    }

    /**
     * 创建测试消息列表
     */
    private static List<Message> createTestMessages() {
        return List.of(
            Message.builder()
                .messageType(MessageType.SYSTEM)
                .content("You are a helpful assistant.")
                .build(),
            Message.builder()
                .messageType(MessageType.INPUT)
                .content("你好，请介绍一下你自己。")
                .build()
        );
    }
}
