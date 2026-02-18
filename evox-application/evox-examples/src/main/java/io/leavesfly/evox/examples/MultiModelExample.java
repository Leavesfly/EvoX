package io.leavesfly.evox.examples;

import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.core.message.MessageType;
import io.leavesfly.evox.models.spi.LLMProvider;
import io.leavesfly.evox.models.provider.aliyun.AliyunLLM;
import io.leavesfly.evox.models.provider.aliyun.AliyunLLMConfig;
import io.leavesfly.evox.models.provider.openai.OpenAILLMConfig;
import io.leavesfly.evox.models.provider.openai.OpenAILLM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 多模型适配器示例
 * 演示如何使用阿里云通义千问和 OpenAI 适配器
 *
 * @author EvoX Team
 */
public class MultiModelExample {
    private static final Logger log = LoggerFactory.getLogger(MultiModelExample.class);

    public static void main(String[] args) {
        MultiModelExample example = new MultiModelExample();
        
        // 示例1：使用阿里云通义千问模型
        example.demonstrateAliyunModel();
        
        // 示例2：使用 OpenAI 模型
        example.demonstrateOpenAIModel();
        
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
        LLMProvider aliyunLLM = new AliyunLLM(config);
        log.info("✓ 阿里云通义千问模型已初始化");
        
        // 注意：实际调用需要有效的 API Key
        log.info("  提示：实际使用需要配置有效的 DashScope API Key");
    }

    /**
     * 示例2：使用 OpenAI 模型
     */
    private void demonstrateOpenAIModel() {
        log.info("\n--- 示例2：OpenAI 模型 ---");
        
        OpenAILLMConfig openaiConfig = OpenAILLMConfig.builder()
                .apiKey("your-openai-api-key")
                .model("gpt-4o-mini")
                .temperature(0.8f)
                .maxTokens(2000)
                .build();
        
        log.info("OpenAI 配置:");
        log.info("  模型: {}", openaiConfig.getModel());
        log.info("  API地址: {}", openaiConfig.getBaseUrl());
        log.info("  Temperature: {}", openaiConfig.getTemperature());
        
        LLMProvider openaiLLM = new OpenAILLM(openaiConfig);
        log.info("✓ OpenAI 模型已初始化");
        log.info("  提示：实际使用需要配置有效的 OpenAI API Key");
    }

    /**
     * 示例3：配置对比
     */
    private void demonstrateConfigComparison() {
        log.info("\n--- 示例3：配置对比 ---");
        
        AliyunLLMConfig aliyunConfig = AliyunLLMConfig.builder()
                .temperature(0.8f)
                .maxTokens(2000)
                .enableSearch(true)
                .build();
        
        OpenAILLMConfig openaiConfig = OpenAILLMConfig.builder()
                .temperature(0.7f)
                .maxTokens(1500)
                .build();
        
        log.info("配置继承演示:");
        log.info("  阿里云 Temperature: {}, OpenAI Temperature: {}", 
                aliyunConfig.getTemperature(), openaiConfig.getTemperature());
        log.info("  阿里云 MaxTokens: {}, OpenAI MaxTokens: {}", 
                aliyunConfig.getMaxTokens(), openaiConfig.getMaxTokens());
        
        log.info("\n特有功能:");
        log.info("  阿里云启用搜索: {}", aliyunConfig.getEnableSearch());
        
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
