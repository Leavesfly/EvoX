package io.leavesfly.evox.models.openai;

import io.leavesfly.evox.core.llm.ILLMToolUse;
import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.core.message.MessageType;
import io.leavesfly.evox.models.base.LLMProvider;
import io.leavesfly.evox.models.client.ChatCompletionRequest;
import io.leavesfly.evox.models.client.ChatCompletionResponse;
import io.leavesfly.evox.models.client.ChatCompletionResult;
import io.leavesfly.evox.models.client.OpenAiCompatibleClient;
import io.leavesfly.evox.models.client.ToolCall;
import io.leavesfly.evox.models.client.ToolDefinition;
import io.leavesfly.evox.models.config.LLMConfig;
import io.leavesfly.evox.models.config.OpenAILLMConfig;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI LLM实现
 * 通过 OpenAI 兼容 HTTP 客户端调用 OpenAI API
 *
 * @author EvoX Team
 */
@Slf4j
public class OpenAILLM implements LLMProvider {

    private final OpenAILLMConfig config;
    private final OpenAiCompatibleClient client;

    public OpenAILLM(OpenAILLMConfig config) {
        this.config = config;

        String baseUrl = config.getBaseUrl() != null ? config.getBaseUrl() : "https://api.openai.com/v1";
        this.client = new OpenAiCompatibleClient(baseUrl, config.getApiKey(), config.getTimeout());

        log.info("Initialized OpenAI LLM with model: {}", config.getModel());
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

            String response = client.chatCompletion(request);

            if (Boolean.TRUE.equals(config.getOutputResponse())) {
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
        ChatCompletionRequest request = OpenAiCompatibleClient.buildChatRequest(
                config.getModel(), prompt,
                config.getTemperature() != null ? config.getTemperature() : 0.7f,
                config.getMaxTokens(),
                config.getTopP() != null ? config.getTopP() : 1.0f,
                config.getFrequencyPenalty() != null ? config.getFrequencyPenalty() : 0.0f,
                config.getPresencePenalty() != null ? config.getPresencePenalty() : 0.0f);

        return client.chatCompletionAsync(request);
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
            return Flux.error(new RuntimeException("Failed to generate stream", e));
        }
    }

    @Override
    public String chat(List<Message> messages) {
        try {
            List<ChatCompletionRequest.ChatMessage> chatMessages = convertMessages(messages);
            ChatCompletionRequest request = buildRequest(chatMessages, null, null);
            
            String response = client.chatCompletion(request);

            if (Boolean.TRUE.equals(config.getOutputResponse())) {
                log.info("OpenAI Chat Response: {}", response);
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
            ChatCompletionResult result = client.chatCompletionWithTools(request);

            if (Boolean.TRUE.equals(config.getOutputResponse())) {
                log.info("OpenAI Tool Call Result: content={}, toolCalls={}, finishReason={}", 
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
            
            return client.chatCompletionWithTools(request);
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

            return client.chatCompletionStreamRaw(request);
        } catch (Exception e) {
            log.error("Error in streaming chat with tool definitions", e);
            return Flux.error(new RuntimeException("Failed to stream chat with tool definitions", e));
        }
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
}