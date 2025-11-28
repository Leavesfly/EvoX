package io.leavesfly.evox.models.openai;

import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.models.base.BaseLLM;
import io.leavesfly.evox.models.config.LLMConfig;
import io.leavesfly.evox.models.config.OpenAILLMConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * OpenAI LLM实现
 * 使用Spring AI集成OpenAI
 *
 * @author EvoX Team
 */
@Slf4j
public class OpenAILLM implements BaseLLM {

    private final OpenAILLMConfig config;
    private final OpenAiChatModel chatModel;
    private final ChatClient chatClient;

    public OpenAILLM(OpenAILLMConfig config) {
        this.config = config;
        
        // 创建OpenAI API客户端
        OpenAiApi openAiApi = new OpenAiApi(config.getApiKey());
        
        // 创建聊天选项
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .withModel(config.getModel())
                .withTemperature(config.getTemperature() != null ? config.getTemperature().floatValue() : 0.7f)
                .withMaxTokens(config.getMaxTokens())
                .withTopP(config.getTopP() != null ? config.getTopP().floatValue() : 1.0f)
                .withFrequencyPenalty(config.getFrequencyPenalty() != null ? config.getFrequencyPenalty().floatValue() : 0.0f)
                .withPresencePenalty(config.getPresencePenalty() != null ? config.getPresencePenalty().floatValue() : 0.0f)
                .build();
        
        // 创建聊天模型
        this.chatModel = new OpenAiChatModel(openAiApi, options);
        this.chatClient = ChatClient.create(chatModel);
        
        log.info("Initialized OpenAI LLM with model: {}", config.getModel());
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
                log.info("OpenAI Response: {}", response);
            }
            
            return response;
        } catch (Exception e) {
            log.error("Error generating response", e);
            throw new RuntimeException("Failed to generate response", e);
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
            return Flux.error(new RuntimeException("Failed to generate stream", e));
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
