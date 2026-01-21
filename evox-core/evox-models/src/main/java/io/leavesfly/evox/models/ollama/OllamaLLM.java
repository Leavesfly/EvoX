package io.leavesfly.evox.models.ollama;

import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.models.base.BaseLLM;
import io.leavesfly.evox.models.config.LLMConfig;
import io.leavesfly.evox.models.config.OllamaLLMConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Ollama LLM 实现
 * 使用 Spring AI 集成 Ollama 本地/远程服务
 *
 * @author EvoX Team
 */
@Slf4j
public class OllamaLLM implements BaseLLM {

    private final OllamaLLMConfig config;
    private final OllamaChatModel chatModel;
    private final ChatClient chatClient;

    public OllamaLLM(OllamaLLMConfig config) {
        this.config = config;

        String baseUrl = config.getEffectiveBaseUrl();
        OllamaApi ollamaApi = new OllamaApi(baseUrl);

        String model = config.getModel() != null && !config.getModel().isEmpty()
                ? config.getModel() : "llama2";
        OllamaOptions options = new OllamaOptions()
                .withModel(model)
                .withTemperature(config.getTemperature() != null ? config.getTemperature() : 0.7f)
                .withTopP(config.getTopP() != null ? config.getTopP() : 1.0f)
                .withNumPredict(config.getMaxTokens() != null ? config.getMaxTokens() : 1000);

        if (config.getFrequencyPenalty() != null && config.getFrequencyPenalty() != 0.0f) {
            options.withFrequencyPenalty(config.getFrequencyPenalty());
        }
        if (config.getPresencePenalty() != null && config.getPresencePenalty() != 0.0f) {
            options.withPresencePenalty(config.getPresencePenalty());
        }

        this.chatModel = new OllamaChatModel(ollamaApi, options);
        this.chatClient = ChatClient.create(chatModel);

        log.info("Initialized Ollama LLM with model: {} at {}", config.getModel(), baseUrl);
    }

    @Override
    public String generate(String prompt) {
        try {
            log.debug("Generating response for prompt: {}", prompt.substring(0, Math.min(50, prompt.length())));

            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

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
            return chatClient.prompt()
                    .user(prompt)
                    .stream()
                    .content()
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
