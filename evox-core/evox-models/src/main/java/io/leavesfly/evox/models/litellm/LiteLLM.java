package io.leavesfly.evox.models.litellm;

import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.models.base.BaseLLM;
import io.leavesfly.evox.models.config.LLMConfig;
import io.leavesfly.evox.models.config.LiteLLMConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * LiteLLM适配器
 * 通过OpenAI兼容接口调用LiteLLM代理服务
 * 支持统一接口调用多种大模型
 *
 * @author EvoX Team
 */
@Slf4j
public class LiteLLM implements BaseLLM {

    private final LiteLLMConfig config;
    private final OpenAiChatModel chatModel;
    private final ChatClient chatClient;

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

        // 创建OpenAI兼容API客户端
        String baseUrl = config.getEffectiveBaseUrl();
        String apiKey = config.getEffectiveApiKey();

        OpenAiApi openAiApi;
        if (baseUrl != null && !baseUrl.isEmpty()) {
            openAiApi = new OpenAiApi(baseUrl, apiKey);
        } else {
            openAiApi = new OpenAiApi(apiKey);
        }

        // 创建聊天选项
        OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder()
                .withModel(config.getModel());

        if (config.getTemperature() != null) {
            optionsBuilder.withTemperature(config.getTemperature());
        }
        if (config.getMaxTokens() != null) {
            optionsBuilder.withMaxTokens(config.getMaxTokens());
        }
        if (config.getTopP() != null) {
            optionsBuilder.withTopP(config.getTopP());
        }
        if (config.getFrequencyPenalty() != null) {
            optionsBuilder.withFrequencyPenalty(config.getFrequencyPenalty());
        }
        if (config.getPresencePenalty() != null) {
            optionsBuilder.withPresencePenalty(config.getPresencePenalty());
        }

        OpenAiChatOptions options = optionsBuilder.build();

        // 创建聊天模型
        this.chatModel = new OpenAiChatModel(openAiApi, options);
        this.chatClient = ChatClient.create(chatModel);

        log.info("Initialized LiteLLM with model: {} at {}", config.getModel(), 
                baseUrl != null ? baseUrl : "default OpenAI endpoint");
    }

    @Override
    public String generate(String prompt) {
        try {
            log.debug("Generating response for prompt: {}", prompt.substring(0, Math.min(50, prompt.length())));

            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            if (config.getOutputResponse()) {
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
            return chatClient.prompt()
                    .user(prompt)
                    .stream()
                    .content()
                    .doOnNext(chunk -> {
                        if (config.getOutputResponse()) {
                            System.out.print(chunk);
                        }
                    })
                    .doOnComplete(() -> {
                        if (config.getOutputResponse()) {
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
        // 简化实现：将消息列表转换为单个prompt
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
