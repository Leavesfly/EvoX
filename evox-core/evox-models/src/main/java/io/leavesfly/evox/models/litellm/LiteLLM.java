package io.leavesfly.evox.models.litellm;

import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.models.base.LLMProvider;
import io.leavesfly.evox.models.client.ChatCompletionRequest;
import io.leavesfly.evox.models.client.OpenAiCompatibleClient;
import io.leavesfly.evox.models.config.LLMConfig;
import io.leavesfly.evox.models.config.LiteLLMConfig;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * LiteLLM适配器
 * 通过 OpenAI 兼容 HTTP 客户端调用 LiteLLM 代理服务
 * 支持统一接口调用多种大模型
 *
 * @author EvoX Team
 */
@Slf4j
public class LiteLLM implements LLMProvider {

    private final LiteLLMConfig config;
    private final OpenAiCompatibleClient client;

    /**
     * 构造函数
     *
     * @param config LiteLLM配置
     */
    public LiteLLM(LiteLLMConfig config) {
        if (!config.isValid()) {
            throw new IllegalArgumentException("Invalid LiteLLMConfig: API key or base URL is required");
        }

        this.config = config;

        String baseUrl = config.getEffectiveBaseUrl();
        String apiKey = config.getEffectiveApiKey();
        this.client = new OpenAiCompatibleClient(baseUrl, apiKey, config.getTimeout());

        log.info("Initialized LiteLLM with model: {} at {}",
                config.getModel(), baseUrl != null ? baseUrl : "default OpenAI endpoint");
    }

    @Override
    public String generate(String prompt) {
        try {
            log.debug("Generating response for prompt: {}", prompt.substring(0, Math.min(50, prompt.length())));

            ChatCompletionRequest request = OpenAiCompatibleClient.buildChatRequest(
                    config.getModel(), prompt,
                    config.getTemperature(),
                    config.getMaxTokens(),
                    config.getTopP(),
                    config.getFrequencyPenalty(),
                    config.getPresencePenalty());

            String response = client.chatCompletion(request);

            if (Boolean.TRUE.equals(config.getOutputResponse())) {
                log.info("LiteLLM Response: {}", response);
            }

            return response;
        } catch (Exception e) {
            log.error("Error generating response", e);
            throw new RuntimeException("Failed to generate response from LiteLLM", e);
        }
    }

    @Override
    public Mono<String> generateAsync(String prompt) {
        return Mono.fromCallable(() -> generate(prompt));
    }

    @Override
    public Flux<String> generateStream(String prompt) {
        try {
            ChatCompletionRequest request = OpenAiCompatibleClient.buildChatRequest(
                    config.getModel(), prompt,
                    config.getTemperature(),
                    config.getMaxTokens(),
                    config.getTopP(),
                    config.getFrequencyPenalty(),
                    config.getPresencePenalty());

            return client.chatCompletionStream(request)
                    .doOnNext(chunk -> {
                        if (Boolean.TRUE.equals(config.getOutputResponse())) {
                            System.out.print(chunk);
                        }
                    })
                    .doOnComplete(() -> {
                        if (Boolean.TRUE.equals(config.getOutputResponse())) {
                            System.out.println();
                        }
                    });
        } catch (Exception e) {
            log.error("Error in stream generation", e);
            return Flux.error(new RuntimeException("Failed to generate stream from LiteLLM", e));
        }
    }

    @Override
    public String chat(List<Message> messages) {
        StringBuilder promptBuilder = new StringBuilder();
        for (Message msg : messages) {
            if (msg.getContent() != null) {
                promptBuilder.append(msg.getContent().toString()).append("\n");
            }
        }
        return generate(promptBuilder.toString());
    }

    @Override
    public Mono<String> chatAsync(List<Message> messages) {
        return Mono.fromCallable(() -> chat(messages));
    }

    @Override
    public Flux<String> chatStream(List<Message> messages) {
        StringBuilder promptBuilder = new StringBuilder();
        for (Message msg : messages) {
            if (msg.getContent() != null) {
                promptBuilder.append(msg.getContent().toString()).append("\n");
            }
        }
        return generateStream(promptBuilder.toString());
    }

    @Override
    public String getModelName() {
        return config.getModel();
    }

    @Override
    public LLMConfig getConfig() {
        return config;
    }
}
