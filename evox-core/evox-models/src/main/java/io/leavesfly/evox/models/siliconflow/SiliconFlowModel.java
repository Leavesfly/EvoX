package io.leavesfly.evox.models.siliconflow;

import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.models.base.BaseLLM;
import io.leavesfly.evox.models.config.LLMConfig;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * SiliconFlow模型实现
 * 通过SiliconFlow API访问国产大模型
 * 
 * @author EvoX Team
 */
@Slf4j
@Data
public class SiliconFlowModel implements BaseLLM {

    private static final String SILICONFLOW_API_BASE = "https://api.siliconflow.cn/v1";

    /**
     * SiliconFlow API密钥
     */
    private String apiKey;

    /**
     * 模型名称
     */
    private String model;

    /**
     * Spring AI ChatModel
     */
    private transient ChatModel chatModel;

    /**
     * 配置
     */
    private LLMConfig config;

    public SiliconFlowModel(LLMConfig config) {
        this.config = config;
        this.apiKey = config.getApiKey();
        this.model = config.getModel();
        initializeClient();
    }

    public SiliconFlowModel(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model;
        initializeClient();
    }

    private void initializeClient() {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("SiliconFlow API key is required");
        }

        try {
            // 使用SiliconFlow的API endpoint
            OpenAiApi openAiApi = new OpenAiApi(SILICONFLOW_API_BASE, apiKey);
            
            OpenAiChatOptions options = OpenAiChatOptions.builder()
                .withModel(model)
                .withTemperature(0.7f)
                .withMaxTokens(2000)
                .build();

            this.chatModel = new OpenAiChatModel(openAiApi, options);
            
            log.info("SiliconFlow model initialized: {}", model);
        } catch (Exception e) {
            log.error("Failed to initialize SiliconFlow model", e);
            throw new RuntimeException("SiliconFlow initialization failed", e);
        }
    }

    @Override
    public String generate(String prompt) {
        if (chatModel == null) {
            initializeClient();
        }

        try {
            Prompt chatPrompt = new Prompt(prompt);
            ChatResponse response = chatModel.call(chatPrompt);
            
            if (response != null && response.getResult() != null) {
                return response.getResult().getOutput().getContent();
            }
            
            return "";
        } catch (Exception e) {
            log.error("SiliconFlow generation failed", e);
            throw new RuntimeException("Generation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String getModelName() {
        return "siliconflow/" + model;
    }

    @Override
    public Mono<String> generateAsync(String prompt) {
        return Mono.fromCallable(() -> generate(prompt));
    }

    @Override
    public Flux<String> generateStream(String prompt) {
        return Flux.just(generate(prompt));
    }

    @Override
    public String chat(List<Message> messages) {
        // 简化实现：转换为单个prompt
        StringBuilder sb = new StringBuilder();
        for (Message msg : messages) {
            String role = msg.getAgent() != null ? msg.getAgent() : "user";
            sb.append(role).append(": ").append(msg.getContent()).append("\n");
        }
        return generate(sb.toString());
    }

    @Override
    public Mono<String> chatAsync(List<Message> messages) {
        return Mono.fromCallable(() -> chat(messages));
    }

    @Override
    public Flux<String> chatStream(List<Message> messages) {
        return Flux.just(chat(messages));
    }

    @Override
    public LLMConfig getConfig() {
        return config;
    }
}
