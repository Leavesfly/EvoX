package io.leavesfly.evox.models.provider.openrouter;

import io.leavesfly.evox.core.llm.LLMConfig;
import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.core.message.MessageType;
import io.leavesfly.evox.models.spi.LLMProvider;
import io.leavesfly.evox.models.protocol.ChatCompletionRequest;
import io.leavesfly.evox.models.protocol.ChatCompletionResponse;
import io.leavesfly.evox.models.protocol.ChatCompletionResult;
import io.leavesfly.evox.models.protocol.OpenAiCompatibleClient;
import io.leavesfly.evox.models.protocol.ToolCall;
import io.leavesfly.evox.models.protocol.ToolDefinition;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenRouter LLM 实现
 * 通过 OpenRouter API（兼容 OpenAI 协议）访问多种模型
 *
 * @author EvoX Team
 */
@Slf4j
public class OpenRouterLLM implements LLMProvider {

    private final OpenRouterLLMConfig config;
    private final WebClient webClient;
    private final OpenAiCompatibleClient client;

    public OpenRouterLLM(OpenRouterLLMConfig config) {
        this.config = config;

        String baseUrl = config.getBaseUrl() != null ? config.getBaseUrl() : "https://openrouter.ai/api/v1";
        Duration timeout = config.getTimeout() != null ? config.getTimeout() : Duration.ofSeconds(60);

        // 创建自定义 WebClient 以添加 OpenRouter 特定的请求头
        WebClient.Builder webClientBuilder = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        // 添加 API 密钥
        if (config.getApiKey() != null && !config.getApiKey().isEmpty()) {
            webClientBuilder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApiKey());
        }

        // 添加 OpenRouter 特定的请求头（如果配置了）
        if (config.getSiteUrl() != null && !config.getSiteUrl().isEmpty()) {
            webClientBuilder.defaultHeader("HTTP-Referer", config.getSiteUrl());
        }
        if (config.getSiteName() != null && !config.getSiteName().isEmpty()) {
            webClientBuilder.defaultHeader("X-Title", config.getSiteName());
        }

        this.webClient = webClientBuilder.build();
        
        // 创建基础客户端用于调用
        this.client = new OpenAiCompatibleClient(baseUrl, config.getApiKey(), timeout);

        log.info("Initialized OpenRouter LLM with model: {}", config.getModel());
    }

    @Override
    public String generate(String prompt) {
        try {
            log.debug("Generating response for prompt: {}", prompt.substring(0, Math.min(50, prompt.length())));

            ChatCompletionRequest request = OpenAiCompatibleClient.buildChatRequest(
                    config.getModel(), prompt,
                    config.getTemperature() != null ? config.getTemperature() : 0.7f,
                    config.getMaxTokens(),
                    config.getTopP() != null ? config.getTopP() : 1.0f,
                    config.getFrequencyPenalty() != null ? config.getFrequencyPenalty() : 0.0f,
                    config.getPresencePenalty() != null ? config.getPresencePenalty() : 0.0f);

            String response = callWithCustomHeaders(request);

            if (Boolean.TRUE.equals(config.getOutputResponse())) {
                log.info("OpenRouter Response: {}", response);
            }

            return response;
        } catch (Exception e) {
            log.error("Error generating response", e);
            throw new RuntimeException("Failed to generate response", e);
        }
    }

    @Override
    public Mono<String> generateAsync(String prompt) {
        ChatCompletionRequest request = OpenAiCompatibleClient.buildChatRequest(
                config.getModel(), prompt,
                config.getTemperature() != null ? config.getTemperature() : 0.7f,
                config.getMaxTokens(),
                config.getTopP() != null ? config.getTopP() : 1.0f,
                config.getFrequencyPenalty() != null ? config.getFrequencyPenalty() : 0.0f,
                config.getPresencePenalty() != null ? config.getPresencePenalty() : 0.0f);

        return callAsyncWithCustomHeaders(request);
    }

    @Override
    public Flux<String> generateStream(String prompt) {
        try {
            ChatCompletionRequest request = OpenAiCompatibleClient.buildChatRequest(
                    config.getModel(), prompt,
                    config.getTemperature() != null ? config.getTemperature() : 0.7f,
                    config.getMaxTokens(),
                    config.getTopP() != null ? config.getTopP() : 1.0f,
                    config.getFrequencyPenalty() != null ? config.getFrequencyPenalty() : 0.0f,
                    config.getPresencePenalty() != null ? config.getPresencePenalty() : 0.0f);

            return callStreamWithCustomHeaders(request)
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
            return Flux.error(new RuntimeException("Failed to generate stream", e));
        }
    }

    @Override
    public String chat(List<Message> messages) {
        try {
            List<ChatCompletionRequest.ChatMessage> chatMessages = convertMessages(messages);
            ChatCompletionRequest request = buildRequest(chatMessages, null, null);
            
            String response = callWithCustomHeaders(request);

            if (Boolean.TRUE.equals(config.getOutputResponse())) {
                log.info("OpenRouter Chat Response: {}", response);
            }

            return response;
        } catch (Exception e) {
            log.error("Error in chat", e);
            throw new RuntimeException("Failed to chat", e);
        }
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

    @Override
    public boolean supportsToolUse() {
        return true;
    }

    @Override
    public Map<String, Object> chatWithTools(List<Message> messages,
                                             List<Map<String, Object>> toolSchemas,
                                             String toolChoice) {
        try {
            log.debug("Chat with tools, messages count: {}, tools count: {}", 
                    messages.size(), toolSchemas.size());

            List<ChatCompletionRequest.ChatMessage> chatMessages = convertMessages(messages);
            
            List<ToolDefinition> tools = new ArrayList<>();
            for (Map<String, Object> toolSchema : toolSchemas) {
                ToolDefinition toolDef = ToolDefinition.fromToolSchema(toolSchema);
                if (toolDef != null) {
                    tools.add(toolDef);
                }
            }

            ChatCompletionRequest request = buildRequest(chatMessages, tools, toolChoice);
            ChatCompletionResult result = callWithToolsAndCustomHeaders(request);

            if (Boolean.TRUE.equals(config.getOutputResponse())) {
                log.info("OpenRouter Tool Call Result: content={}, toolCalls={}, finishReason={}", 
                        result.getContent(), result.getToolCalls(), result.getFinishReason());
            }

            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("content", result.getContent());
            resultMap.put("tool_calls", result.getToolCalls());
            resultMap.put("finish_reason", result.getFinishReason());
            resultMap.put("usage", result.getUsage());

            return resultMap;
        } catch (Exception e) {
            log.error("Error in chat with tools", e);
            throw new RuntimeException("Failed to chat with tools", e);
        }
    }

    @Override
    public ChatCompletionResult chatWithToolDefinitions(List<Message> messages,
                                                         List<ToolDefinition> toolDefinitions,
                                                         String toolChoice) {
        try {
            log.debug("Chat with tool definitions, messages count: {}, tools count: {}", 
                    messages.size(), toolDefinitions.size());

            List<ChatCompletionRequest.ChatMessage> chatMessages = convertMessages(messages);
            ChatCompletionRequest request = buildRequest(chatMessages, toolDefinitions, toolChoice);
            
            return callWithToolsAndCustomHeaders(request);
        } catch (Exception e) {
            log.error("Error in chat with tool definitions", e);
            throw new RuntimeException("Failed to chat with tool definitions", e);
        }
    }

    @Override
    public Flux<ChatCompletionResponse> chatWithToolDefinitionsStream(List<Message> messages,
                                                                      List<ToolDefinition> toolDefinitions,
                                                                      String toolChoice) {
        try {
            log.debug("Streaming chat with tool definitions, messages count: {}, tools count: {}",
                    messages.size(), toolDefinitions.size());

            List<ChatCompletionRequest.ChatMessage> chatMessages = convertMessages(messages);
            ChatCompletionRequest request = buildRequest(chatMessages, toolDefinitions, toolChoice);
            request.setStream(true);

            // request usage in stream mode so we can track token consumption
            request.setStreamOptions(Map.of("include_usage", true));

            return callStreamRawWithCustomHeaders(request);
        } catch (Exception e) {
            log.error("Error in streaming chat with tool definitions", e);
            return Flux.error(new RuntimeException("Failed to stream chat with tool definitions", e));
        }
    }

    private String callWithCustomHeaders(ChatCompletionRequest request) {
        request.setStream(false);

        ChatCompletionResponse response = webClient.post()
                .uri("/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ChatCompletionResponse.class)
                .timeout(config.getTimeout())
                .block();

        return extractContent(response);
    }

    private Mono<String> callAsyncWithCustomHeaders(ChatCompletionRequest request) {
        request.setStream(false);

        return webClient.post()
                .uri("/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ChatCompletionResponse.class)
                .timeout(config.getTimeout())
                .map(this::extractContent);
    }

    private Flux<String> callStreamWithCustomHeaders(ChatCompletionRequest request) {
        request.setStream(true);

        return webClient.post()
                .uri("/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(config.getTimeout())
                .filter(line -> line.startsWith("data: "))
                .map(line -> line.substring("data: ".length()).trim())
                .filter(data -> !data.equals("[DONE]"))
                .mapNotNull(this::parseStreamChunkContent);
    }

    private ChatCompletionResult callWithToolsAndCustomHeaders(ChatCompletionRequest request) {
        request.setStream(false);

        ChatCompletionResponse response = webClient.post()
                .uri("/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ChatCompletionResponse.class)
                .timeout(config.getTimeout())
                .block();

        return ChatCompletionResult.fromResponse(response);
    }

    private Flux<ChatCompletionResponse> callStreamRawWithCustomHeaders(ChatCompletionRequest request) {
        request.setStream(true);

        return webClient.post()
                .uri("/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(config.getTimeout())
                .filter(line -> line.startsWith("data: "))
                .map(line -> line.substring("data: ".length()).trim())
                .filter(data -> !data.equals("[DONE]"))
                .mapNotNull(this::parseStreamChunk);
    }

    private List<ChatCompletionRequest.ChatMessage> convertMessages(List<Message> messages) {
        List<ChatCompletionRequest.ChatMessage> chatMessages = new ArrayList<>();
        
        for (Message message : messages) {
            ChatCompletionRequest.ChatMessage.ChatMessageBuilder builder = 
                    ChatCompletionRequest.ChatMessage.builder();
            
            MessageType messageType = message.getMessageType();
            
            if (messageType == MessageType.SYSTEM) {
                builder.role("system");
            } else if (messageType == MessageType.INPUT) {
                builder.role("user");
            } else if (messageType == MessageType.OUTPUT || messageType == MessageType.RESPONSE) {
                builder.role("assistant");
                
                if (message.getMetadata() != null && message.getMetadata().containsKey("tool_calls")) {
                    Object toolCallsObj = message.getMetadata().get("tool_calls");
                    if (toolCallsObj instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<ToolCall> toolCalls = (List<ToolCall>) toolCallsObj;
                        builder.toolCalls(toolCalls);
                    }
                }
            } else if ("tool_result".equals(message.getAction())) {
                builder.role("tool");
                if (message.getMetadata() != null && message.getMetadata().containsKey("tool_call_id")) {
                    builder.toolCallId((String) message.getMetadata().get("tool_call_id"));
                }
            } else {
                builder.role("user");
            }

            if (message.getContent() != null) {
                builder.content(message.getContent().toString());
            }

            chatMessages.add(builder.build());
        }
        
        return chatMessages;
    }

    private ChatCompletionRequest buildRequest(List<ChatCompletionRequest.ChatMessage> chatMessages,
                                                List<ToolDefinition> tools,
                                                String toolChoice) {
        ChatCompletionRequest.ChatCompletionRequestBuilder builder = ChatCompletionRequest.builder()
                .model(config.getModel())
                .messages(chatMessages)
                .temperature(config.getTemperature() != null ? config.getTemperature() : 0.7f)
                .maxTokens(config.getMaxTokens())
                .topP(config.getTopP() != null ? config.getTopP() : 1.0f)
                .frequencyPenalty(config.getFrequencyPenalty() != null ? config.getFrequencyPenalty() : 0.0f)
                .presencePenalty(config.getPresencePenalty() != null ? config.getPresencePenalty() : 0.0f)
                .stream(false);

        if (tools != null && !tools.isEmpty()) {
            builder.tools(tools);
            builder.parallelToolCalls(true);
        }

        if (toolChoice != null && !toolChoice.isEmpty()) {
            builder.toolChoice(toolChoice);
        }

        return builder.build();
    }

    private String extractContent(ChatCompletionResponse response) {
        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
            return "";
        }
        ChatCompletionResponse.Choice firstChoice = response.getChoices().get(0);
        if (firstChoice.getMessage() != null && firstChoice.getMessage().getContent() != null) {
            return firstChoice.getMessage().getContent();
        }
        return "";
    }

    private String parseStreamChunkContent(String jsonData) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            ChatCompletionResponse chunk = mapper.readValue(jsonData, ChatCompletionResponse.class);
            if (chunk.getChoices() != null && !chunk.getChoices().isEmpty()) {
                ChatCompletionResponse.Delta delta = chunk.getChoices().get(0).getDelta();
                if (delta != null && delta.getContent() != null) {
                    return delta.getContent();
                }
            }
        } catch (Exception e) {
            log.debug("Failed to parse SSE chunk: {}", jsonData, e);
        }
        return null;
    }

    private ChatCompletionResponse parseStreamChunk(String jsonData) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(jsonData, ChatCompletionResponse.class);
        } catch (Exception e) {
            log.debug("Failed to parse SSE chunk: {}", jsonData, e);
            return null;
        }
    }
}
