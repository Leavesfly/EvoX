package io.leavesfly.evox.models;

import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.core.message.MessageType;
import io.leavesfly.evox.models.provider.aliyun.AliyunLLMConfig;
import io.leavesfly.evox.models.provider.openai.OpenAILLMConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 多模型适配器测试
 */
class MultiModelAdapterTest {

    @Test
    void testAliyunLLMConfig() {
        AliyunLLMConfig config = AliyunLLMConfig.builder().apiKey("test-key").model("qwen-turbo").build();
        
        assertEquals("test-key", config.getApiKey());
        assertEquals("qwen-turbo", config.getModel());
        assertFalse(config.getEnableSearch());
    }

    @Test
    void testAliyunLLMConfigBuilder() {
        AliyunLLMConfig config = AliyunLLMConfig.builder()
                .apiKey("test-key")
                .model("qwen-plus")
                .temperature(0.8f)
                .maxTokens(2000)
                .enableSearch(true)

                .build();
        
        assertEquals("qwen-plus", config.getModel());
        assertEquals(0.8f, config.getTemperature());
        assertEquals(2000, config.getMaxTokens());
        assertTrue(config.getEnableSearch());

    }

    @Test
    void testOpenAILLMConfig() {
        OpenAILLMConfig config = OpenAILLMConfig.builder().apiKey("test-key").model("gpt-4o").build();
        
        assertEquals("test-key", config.getApiKey());
        assertEquals("gpt-4o", config.getModel());
    }

    @Test
    void testOpenAILLMConfigBuilder() {
        OpenAILLMConfig config = OpenAILLMConfig.builder()
                .apiKey("test-key")
                .model("gpt-4o-mini")
                .temperature(0.7f)
                .maxTokens(1500)
                .topP(0.9f)
                .build();
        
        assertEquals("gpt-4o-mini", config.getModel());
        assertEquals(0.7f, config.getTemperature());
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "DASHSCOPE_API_KEY", matches = ".+")
    void testAliyunLLMIntegration() {
        String apiKey = System.getenv("DASHSCOPE_API_KEY");
        
        AliyunLLMConfig config = AliyunLLMConfig.builder()
                .apiKey(apiKey)
                .model("qwen-turbo")
                .temperature(0.7f)
                .build();
        
        // 这里只测试配置，不实际调用 API
        assertNotNull(config);
        assertEquals("qwen-turbo", config.getModel());
    }

    @Test
    void testMessageConversion() {
        List<Message> messages = List.of(
            Message.builder()
                .messageType(MessageType.SYSTEM)
                .content("You are a helpful assistant")
                .build(),
            Message.builder()
                .messageType(MessageType.INPUT)
                .content("Hello")
                .build(),
            Message.builder()
                .messageType(MessageType.OUTPUT)
                .content("Hi there!")
                .build()
        );
        
        assertEquals(3, messages.size());
        assertEquals(MessageType.SYSTEM, messages.get(0).getMessageType());
        assertEquals(MessageType.INPUT, messages.get(1).getMessageType());
        assertEquals(MessageType.OUTPUT, messages.get(2).getMessageType());
    }

    @Test
    void testConfigInheritance() {
        AliyunLLMConfig aliyunConfig = AliyunLLMConfig.builder()
                .temperature(0.8f)
                .maxTokens(2000)
                .build();
        
        OpenAILLMConfig openaiConfig = OpenAILLMConfig.builder()
                .temperature(0.7f)
                .maxTokens(1500)
                .build();
        
        assertNotNull(aliyunConfig.getTemperature());
        assertNotNull(openaiConfig.getTemperature());
        
        assertTrue(aliyunConfig.getTemperature() > openaiConfig.getTemperature());
        assertTrue(aliyunConfig.getMaxTokens() > openaiConfig.getMaxTokens());
    }

    @Test
    void testAliyunSpecificFeatures() {
        AliyunLLMConfig config = AliyunLLMConfig.builder()
                .enableSearch(true)

                .repetitionPenalty(1.1f)
                .build();
        
        assertTrue(config.getEnableSearch());

        assertEquals(1.1f, config.getRepetitionPenalty());
    }

    @Test
    void testConfigDefaults() {
        AliyunLLMConfig aliyunConfig = new AliyunLLMConfig();
        assertEquals("qwen-turbo", aliyunConfig.getModel());
        assertFalse(aliyunConfig.getEnableSearch());
        
        OpenAILLMConfig openaiConfig = new OpenAILLMConfig();
        assertEquals("gpt-4o-mini", openaiConfig.getModel());
    }
}
