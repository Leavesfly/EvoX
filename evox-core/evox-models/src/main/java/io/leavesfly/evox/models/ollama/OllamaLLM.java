package io.leavesfly.evox.models.ollama;

import io.leavesfly.evox.core.llm.LLMConfig;
import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.models.base.LLMProvider;
import io.leavesfly.evox.models.client.ChatCompletionRequest;
import io.leavesfly.evox.models.client.OpenAiCompatibleClient;

import io.leavesfly.evox.models.config.OllamaLLMConfig;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Ollama LLM 实现
 * 通过 OpenAI 兼容 HTTP 客户端调用 Ollama 服务
 *
 * @author EvoX Team
 */
@Slf4j
public class OllamaLLM implements LLMProvider {

    private final OllamaLLMConfig config;
    private final OpenAiCompatibleClient client;

    public OllamaLLM(OllamaLLMConfig config) {
        this.config = config;

        String baseUrl = config.getEffectiveBaseUrl();
        this.client = new OpenAiCompatibleClient(baseUrl, config.getApiKey(), config.getTimeout());

        log.info("Initialized Ollama LLM with model: {} at {}", config.getModel(), baseUrl);
    }

    @Override
    public String generate(String prompt) {
        try {
            log.debug("Generating response for prompt: {}", prompt.substring(0, Math.min(50, prompt.length())));

            String model = config.getModel() != null && !config.getModel().isEmpty()
                    ? config.getModel() : "llama2";

            ChatCompletionRequest request = OpenAiCompatibleClient.buildChatRequest(
                    model, prompt,
                    config.getTemperature() != null ? config.getTemperature() : 0.7f,
                    config.getMaxTokens() != null ? config.getMaxTokens() : 1000,
                    config.getTopP() != null ? config.getTopP() : 1.0f,
                    config.getFrequencyPenalty(),
                    config.getPresencePenalty());

            String response = client.chatCompletion(request);

            if (Boolean.TRUE.equals(config.getOutputResponse())) {
                log.info("Ollama Response: {}", response);
            }

            return response != null ? response : "";
        } catch (Exception e) {
            log.error("Error generating response from Ollama", e);
            throw new RuntimeException("Failed to generate response from Ollama: " + e.getMessage(), e);
        }
    }

    @Override
    public Mono<String> generateAsync(String prompt) {
        return Mono.fromCallable(() -> generate(prompt));
    }

    @Override
    public Flux<String> generateStream(String prompt) {
        try {
            String model = config.getModel() != null && !config.getModel().isEmpty()
                    ? config.getModel() : "llama2";

            ChatCompletionRequest request = OpenAiCompatibleClient.buildChatRequest(
                    model, prompt,
                    config.getTemperature() != null ? config.getTemperature() : 0.7f,
                    config.getMaxTokens() != null ? config.getMaxTokens() : 1000,
                    config.getTopP() != null ? config.getTopP() : 1.0f,
                    config.getFrequencyPenalty(),
                    config.getPresencePenalty());

            return client.chatCompletionStream(request)
                    .doOnNext(chunk -> {
                        if (Boolean.TRUE.equals(config.getOutputResponse())) {
                            System.out.print(chunk != null ? chunk : "");
                        }
                    })
                    .doOnComplete(() -> {
                        if (Boolean.TRUE.equals(config.getOutputResponse())) {
                            System.out.println();
                        }
                    });
        } catch (Exception e) {
            log.error("Error in Ollama stream generation", e);
            return Flux.error(new RuntimeException("Failed to generate stream from Ollama", e));
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
