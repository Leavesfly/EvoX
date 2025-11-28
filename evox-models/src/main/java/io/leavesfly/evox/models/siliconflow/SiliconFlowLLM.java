package io.leavesfly.evox.models.siliconflow;

import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.models.base.BaseLLM;
import io.leavesfly.evox.models.config.LLMConfig;
import io.leavesfly.evox.models.config.SiliconFlowConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 硅基流动LLM实现
 * 使用OpenAI兼容接口调用硅基流动服务
 *
 * @author EvoX Team
 */
@Slf4j
public class SiliconFlowLLM implements BaseLLM {

    private final SiliconFlowConfig config;
    private final OpenAiChatModel chatModel;
    private final ChatClient chatClient;

    /**
     * 构造函数
     *
     * @param config 硅基流动配置
     */
    public SiliconFlowLLM(SiliconFlowConfig config) {
        if (!config.isValid()) {
            throw new IllegalArgumentException("Invalid SiliconFlowConfig: API key is required");
        }

        this.config = config;

        // 创建OpenAI兼容API客户端
        String baseUrl = config.getBaseUrl();
        String apiKey = config.getEffectiveApiKey();

        OpenAiApi openAiApi = new OpenAiApi(baseUrl, apiKey);

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

        OpenAiChatOptions options = optionsBuilder.build();

        // 创建聊天模型
        this.chatModel = new OpenAiChatModel(openAiApi, options);
        this.chatClient = ChatClient.create(chatModel);

        log.info("Initialized SiliconFlow LLM with model: {}", config.getModel());
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
                log.info("SiliconFlow Response: {}", response);
            }

            return response;
        } catch (Exception e) {
            log.error("Error generating response", e);
            throw new RuntimeException("Failed to generate response from SiliconFlow", e);
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
            return Flux.error(new RuntimeException("Failed to generate stream from SiliconFlow", e));
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
