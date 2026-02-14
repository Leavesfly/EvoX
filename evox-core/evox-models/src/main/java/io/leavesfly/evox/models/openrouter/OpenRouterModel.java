package io.leavesfly.evox.models.openrouter;

import io.leavesfly.evox.core.llm.LLMConfig;
import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.models.base.LLMProvider;
import io.leavesfly.evox.models.client.ChatCompletionRequest;
import io.leavesfly.evox.models.client.OpenAiCompatibleClient;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

/**
 * OpenRouter模型实现
 * 通过 OpenAI 兼容 HTTP 客户端访问 OpenRouter API
 *
 * @author EvoX Team
 */
@Slf4j
@Data
public class OpenRouterModel implements LLMProvider {

    private static final String OPENROUTER_API_BASE = "https://openrouter.ai/api/v1";

    /**
     * OpenRouter API密钥
     */
    private String apiKey;

    /**
     * 模型名称
     */
    private String model;

    /**
     * HTTP 客户端
     */
    private transient OpenAiCompatibleClient client;

    /**
     * 配置
     */
    private LLMConfig config;

    public OpenRouterModel(LLMConfig config) {
        this.config = config;
        this.apiKey = config.getApiKey();
        this.model = config.getModel();
        initializeClient();
    }

    public OpenRouterModel(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model;
        initializeClient();
    }

    private void initializeClient() {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("OpenRouter API key is required");
        }

        try {
            Duration timeout = config != null ? config.getTimeout() : Duration.ofSeconds(60);
            this.client = new OpenAiCompatibleClient(OPENROUTER_API_BASE, apiKey, timeout);
            log.info("OpenRouter model initialized: {}", model);
        } catch (Exception e) {
            log.error("Failed to initialize OpenRouter model", e);
            throw new RuntimeException("OpenRouter initialization failed", e);
        }
    }

    @Override
    public String generate(String prompt) {
        if (client == null) {
            initializeClient();
        }

        try {
            ChatCompletionRequest request = OpenAiCompatibleClient.buildChatRequest(
                    model, prompt, 0.7f, 2000, null, null, null);

            String response = client.chatCompletion(request);
            return response != null ? response : "";
        } catch (Exception e) {
            log.error("OpenRouter generation failed", e);
            throw new RuntimeException("Generation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String getModelName() {
        return "openrouter/" + model;
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
