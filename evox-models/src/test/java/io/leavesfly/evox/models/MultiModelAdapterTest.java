package io.leavesfly.evox.models;

import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.core.message.MessageType;
import io.leavesfly.evox.models.config.AliyunLLMConfig;
import io.leavesfly.evox.models.config.LiteLLMConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

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
    void testLiteLLMConfig() {
        LiteLLMConfig config = LiteLLMConfig.builder().apiKey("test-key").model("gpt-4").build();
        
        assertEquals("test-key", config.getApiKey());
        assertEquals("gpt-4", config.getModel());
        assertFalse(config.getIsLocal());
    }

    @Test
    void testLiteLLMConfigBuilder() {
        LiteLLMConfig config = LiteLLMConfig.builder()
                .apiKey("test-key")
                .model("claude-2")

                .temperature(0.7f)
                .maxTokens(1500)
                .topP(0.9f)
                .build();
        
        assertEquals("claude-2", config.getModel());

        assertEquals(0.7f, config.getTemperature());
    }

    @Test
    void testLiteLLMConfigAzure() {
        LiteLLMConfig config = LiteLLMConfig.builder()
                .model("gpt-4")
                .azureEndpoint("https://my-resource.openai.azure.com")

                .azureKey("azure-key")
                .build();
        
        assertEquals("https://my-resource.openai.azure.com", config.getAzureEndpoint());

        assertEquals("azure-key", config.getAzureKey());
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
        
        LiteLLMConfig litellmConfig = LiteLLMConfig.builder()
                .temperature(0.7f)
                .maxTokens(1500)
                .build();
        
        // 两种配置都继承自 LLMConfig
        assertNotNull(aliyunConfig.getTemperature());
        assertNotNull(litellmConfig.getTemperature());
        
        assertTrue(aliyunConfig.getTemperature() > litellmConfig.getTemperature());
        assertTrue(aliyunConfig.getMaxTokens() > litellmConfig.getMaxTokens());
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
    void testLiteLLMLocalModel() {
        LiteLLMConfig config = LiteLLMConfig.builder()
                .model("local-model")

                .isLocal(true)
                .build();
        
        assertTrue(config.getIsLocal());

    }

    @Test
    void testConfigDefaults() {
        AliyunLLMConfig aliyunConfig = new AliyunLLMConfig();
        assertEquals("qwen-turbo", aliyunConfig.getModel());
        assertFalse(aliyunConfig.getEnableSearch());
        
        LiteLLMConfig litellmConfig = new LiteLLMConfig();
        assertFalse(litellmConfig.getIsLocal());
    }
}
