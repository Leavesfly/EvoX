package io.leavesfly.evox.models.anthropic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.core.message.MessageType;
import io.leavesfly.evox.models.base.LLMProvider;
import io.leavesfly.evox.models.client.ChatCompletionResponse;
import io.leavesfly.evox.models.client.ChatCompletionResult;
import io.leavesfly.evox.models.client.ToolCall;
import io.leavesfly.evox.models.client.ToolDefinition;
import io.leavesfly.evox.models.config.LLMConfig;
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
 * Anthropic (Claude) LLM 实现
 * 使用 Anthropic Messages API
 *
 * @author EvoX Team
 */
@Slf4j
public class AnthropicLLM implements LLMProvider {

    private final AnthropicLLMConfig config;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public AnthropicLLM(AnthropicLLMConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();

        String baseUrl = config.getBaseUrl() != null ? config.getBaseUrl() : "https://api.anthropic.com";
        Duration timeout = config.getTimeout() != null ? config.getTimeout() : Duration.ofSeconds(60);

        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("x-api-key", config.getApiKey())
                .defaultHeader("anthropic-version", config.getAnthropicVersion())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        log.info("Initialized Anthropic LLM with model: {}", config.getModel());
    }

    @Override
    public String generate(String prompt) {
        try {
            log.debug("Generating response for prompt: {}", prompt.substring(0, Math.min(50, prompt.length())));

            Map<String, Object> requestBody = buildRequestBody(prompt, null, null);
            String response = webClient.post()
                    .uri("/v1/messages")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(config.getTimeout() != null ? config.getTimeout() : Duration.ofSeconds(60));

            if (Boolean.TRUE.equals(config.getOutputResponse())) {
                log.info("Anthropic Response: {}", response);
            }

            return extractTextContent(response);
        } catch (Exception e) {
            log.error("Error generating response", e);
            throw new RuntimeException("Failed to generate response", e);
        }
    }

    @Override
    public Mono<String> generateAsync(String prompt) {
        Map<String, Object> requestBody = buildRequestBody(prompt, null, null);
        return webClient.post()
                .uri("/v1/messages")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::extractTextContent);
    }

    @Override
    public Flux<String> generateStream(String prompt) {
        Map<String, Object> requestBody = buildRequestBody(prompt, null, null);
        requestBody.put("stream", true);

        return webClient.post()
                .uri("/v1/messages")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .flatMap(this::parseStreamEvent)
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
    }

    @Override
    public String chat(List<Message> messages) {
        try {
            List<Map<String, Object>> anthropicMessages = convertMessages(messages);
            Map<String, Object> requestBody = buildChatRequestBody(anthropicMessages, null, null);

            String response = webClient.post()
                    .uri("/v1/messages")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(config.getTimeout() != null ? config.getTimeout() : Duration.ofSeconds(60));

            if (Boolean.TRUE.equals(config.getOutputResponse())) {
                log.info("Anthropic Chat Response: {}", response);
            }

            return extractTextContent(response);
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
        List<Map<String, Object>> anthropicMessages = convertMessages(messages);
        Map<String, Object> requestBody = buildChatRequestBody(anthropicMessages, null, null);
        requestBody.put("stream", true);

        return webClient.post()
                .uri("/v1/messages")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .flatMap(this::parseStreamEvent);
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

            List<Map<String, Object>> anthropicMessages = convertMessages(messages);
            List<Map<String, Object>> anthropicTools = convertToolSchemas(toolSchemas);

            Map<String, Object> requestBody = buildChatRequestBody(anthropicMessages, anthropicTools, toolChoice);
            String response = webClient.post()
                    .uri("/v1/messages")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(config.getTimeout() != null ? config.getTimeout() : Duration.ofSeconds(60));

            if (Boolean.TRUE.equals(config.getOutputResponse())) {
                log.info("Anthropic Tool Call Result: {}", response);
            }

            return parseToolResponse(response);
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

            List<Map<String, Object>> anthropicMessages = convertMessages(messages);
            List<Map<String, Object>> anthropicTools = convertToolDefinitions(toolDefinitions);

            Map<String, Object> requestBody = buildChatRequestBody(anthropicMessages, anthropicTools, toolChoice);
            String response = webClient.post()
                    .uri("/v1/messages")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(config.getTimeout() != null ? config.getTimeout() : Duration.ofSeconds(60));

            return parseToolResult(response);
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

            List<Map<String, Object>> anthropicMessages = convertMessages(messages);
            List<Map<String, Object>> anthropicTools = convertToolDefinitions(toolDefinitions);

            Map<String, Object> requestBody = buildChatRequestBody(anthropicMessages, anthropicTools, toolChoice);
            requestBody.put("stream", true);

            return webClient.post()
                    .uri("/v1/messages")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToFlux(String.class)
                    .flatMap(this::parseStreamEventRaw);
        } catch (Exception e) {
            log.error("Error in streaming chat with tool definitions", e);
            return Flux.error(new RuntimeException("Failed to stream chat with tool definitions", e));
        }
    }

    private Map<String, Object> buildRequestBody(String prompt, List<Map<String, Object>> tools, String toolChoice) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", config.getModel());
        body.put("max_tokens", config.getMaxTokens() != null ? config.getMaxTokens() : 4096);

        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);
        messages.add(message);
        body.put("messages", messages);

        if (config.getTemperature() != null) {
            body.put("temperature", config.getTemperature());
        }
        if (config.getTopP() != null) {
            body.put("top_p", config.getTopP());
        }

        if (tools != null && !tools.isEmpty()) {
            body.put("tools", tools);
        }

        return body;
    }

    private Map<String, Object> buildChatRequestBody(List<Map<String, Object>> messages,
                                                      List<Map<String, Object>> tools,
                                                      String toolChoice) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", config.getModel());
        body.put("max_tokens", config.getMaxTokens() != null ? config.getMaxTokens() : 4096);
        body.put("messages", messages);

        if (config.getTemperature() != null) {
            body.put("temperature", config.getTemperature());
        }
        if (config.getTopP() != null) {
            body.put("top_p", config.getTopP());
        }

        if (tools != null && !tools.isEmpty()) {
            body.put("tools", tools);
        }

        return body;
    }

    private List<Map<String, Object>> convertMessages(List<Message> messages) {
        List<Map<String, Object>> anthropicMessages = new ArrayList<>();

        for (Message message : messages) {
            Map<String, Object> anthropicMessage = new HashMap<>();
            MessageType messageType = message.getMessageType();

            if (messageType == MessageType.SYSTEM) {
                anthropicMessage.put("role", "user");
                anthropicMessage.put("content", "System: " + message.getContent());
            } else if (messageType == MessageType.INPUT) {
                anthropicMessage.put("role", "user");
                anthropicMessage.put("content", message.getContent());
            } else if (messageType == MessageType.OUTPUT || messageType == MessageType.RESPONSE) {
                anthropicMessage.put("role", "assistant");
                anthropicMessage.put("content", message.getContent());
            } else if ("tool_result".equals(message.getAction())) {
                Map<String, Object> content = new HashMap<>();
                content.put("type", "tool_result");
                content.put("tool_use_id", message.getMetadata() != null ? message.getMetadata().get("tool_call_id") : "");
                content.put("content", message.getContent());
                
                anthropicMessage.put("role", "user");
                anthropicMessage.put("content", List.of(content));
            } else {
                anthropicMessage.put("role", "user");
                anthropicMessage.put("content", message.getContent());
            }

            anthropicMessages.add(anthropicMessage);
        }

        return anthropicMessages;
    }

    private List<Map<String, Object>> convertToolSchemas(List<Map<String, Object>> toolSchemas) {
        List<Map<String, Object>> anthropicTools = new ArrayList<>();

        for (Map<String, Object> toolSchema : toolSchemas) {
            @SuppressWarnings("unchecked")
            Map<String, Object> functionMap = (Map<String, Object>) toolSchema.get("function");
            if (functionMap == null) continue;

            Map<String, Object> anthropicTool = new HashMap<>();
            anthropicTool.put("name", functionMap.get("name"));
            anthropicTool.put("description", functionMap.get("description"));
            anthropicTool.put("input_schema", functionMap.get("parameters"));

            anthropicTools.add(anthropicTool);
        }

        return anthropicTools;
    }

    private List<Map<String, Object>> convertToolDefinitions(List<ToolDefinition> toolDefinitions) {
        List<Map<String, Object>> anthropicTools = new ArrayList<>();

        for (ToolDefinition toolDef : toolDefinitions) {
            if (toolDef.getFunction() == null) continue;

            Map<String, Object> anthropicTool = new HashMap<>();
            anthropicTool.put("name", toolDef.getFunction().getName());
            anthropicTool.put("description", toolDef.getFunction().getDescription());
            anthropicTool.put("input_schema", toolDef.getFunction().getParameters());

            anthropicTools.add(anthropicTool);
        }

        return anthropicTools;
    }

    private String extractTextContent(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode contentArray = root.get("content");
            if (contentArray != null && contentArray.isArray()) {
                StringBuilder textBuilder = new StringBuilder();
                for (JsonNode contentItem : contentArray) {
                    if ("text".equals(contentItem.get("type").asText())) {
                        textBuilder.append(contentItem.get("text").asText());
                    }
                }
                return textBuilder.toString();
            }
            return "";
        } catch (Exception e) {
            log.error("Failed to extract text content", e);
            return response;
        }
    }

    private Flux<String> parseStreamEvent(String event) {
        return Flux.fromIterable(extractStreamTexts(event));
    }

    private Flux<ChatCompletionResponse> parseStreamEventRaw(String event) {
        try {
            JsonNode root = objectMapper.readTree(event);
            if (root.has("type") && "content_block_delta".equals(root.get("type").asText())) {
                JsonNode delta = root.get("delta");
                if (delta != null && delta.has("text")) {
                    ChatCompletionResponse response = new ChatCompletionResponse();
                    response.setModel(config.getModel());
                    
                    List<ChatCompletionResponse.Choice> choices = new ArrayList<>();
                    ChatCompletionResponse.Choice choice = new ChatCompletionResponse.Choice();
                    ChatCompletionResponse.Delta deltaObj = new ChatCompletionResponse.Delta();
                    deltaObj.setContent(delta.get("text").asText());
                    choice.setDelta(deltaObj);
                    choices.add(choice);
                    response.setChoices(choices);
                    
                    return Flux.just(response);
                }
            }
            return Flux.empty();
        } catch (Exception e) {
            return Flux.empty();
        }
    }

    private List<String> extractStreamTexts(String event) {
        List<String> texts = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(event);
            if (root.has("type") && "content_block_delta".equals(root.get("type").asText())) {
                JsonNode delta = root.get("delta");
                if (delta != null && delta.has("text")) {
                    texts.add(delta.get("text").asText());
                }
            }
        } catch (Exception e) {
            log.debug("Failed to parse stream event", e);
        }
        return texts;
    }

    private Map<String, Object> parseToolResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode contentArray = root.get("content");

            StringBuilder textBuilder = new StringBuilder();
            List<ToolCall> toolCalls = new ArrayList<>();

            if (contentArray != null && contentArray.isArray()) {
                for (JsonNode contentItem : contentArray) {
                    String type = contentItem.get("type").asText();
                    if ("text".equals(type)) {
                        textBuilder.append(contentItem.get("text").asText());
                    } else if ("tool_use".equals(type)) {
                        ToolCall toolCall = new ToolCall();
                        toolCall.setId(contentItem.get("id").asText());
                        toolCall.setType("function");
                        toolCall.setFunction(new ToolCall.FunctionCall());
                        toolCall.getFunction().setName(contentItem.get("name").asText());
                        toolCall.getFunction().setArguments(contentItem.get("input").toString());
                        toolCalls.add(toolCall);
                    }
                }
            }

            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("content", textBuilder.toString());
            resultMap.put("tool_calls", toolCalls);
            resultMap.put("finish_reason", root.has("stop_reason") ? root.get("stop_reason").asText() : "end_turn");

            return resultMap;
        } catch (Exception e) {
            log.error("Failed to parse tool response", e);
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("content", response);
            resultMap.put("finish_reason", "error");
            return resultMap;
        }
    }

    private ChatCompletionResult parseToolResult(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode contentArray = root.get("content");

            StringBuilder textBuilder = new StringBuilder();
            List<ToolCall> toolCalls = new ArrayList<>();

            if (contentArray != null && contentArray.isArray()) {
                for (JsonNode contentItem : contentArray) {
                    String type = contentItem.get("type").asText();
                    if ("text".equals(type)) {
                        textBuilder.append(contentItem.get("text").asText());
                    } else if ("tool_use".equals(type)) {
                        ToolCall toolCall = new ToolCall();
                        toolCall.setId(contentItem.get("id").asText());
                        toolCall.setType("function");
                        toolCall.setFunction(new ToolCall.FunctionCall());
                        toolCall.getFunction().setName(contentItem.get("name").asText());
                        toolCall.getFunction().setArguments(contentItem.get("input").toString());
                        toolCalls.add(toolCall);
                    }
                }
            }

            return ChatCompletionResult.builder()
                    .content(textBuilder.toString())
                    .toolCalls(toolCalls)
                    .finishReason(root.has("stop_reason") ? root.get("stop_reason").asText() : "end_turn")
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse tool result", e);
            return ChatCompletionResult.builder()
                    .content(response)
                    .finishReason("error")
                    .build();
        }
    }
}
