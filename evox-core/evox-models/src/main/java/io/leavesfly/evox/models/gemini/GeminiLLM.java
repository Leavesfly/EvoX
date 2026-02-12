package io.leavesfly.evox.models.gemini;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.core.message.MessageType;
import io.leavesfly.evox.models.base.LLMProvider;
import io.leavesfly.evox.models.client.ChatCompletionRequest;
import io.leavesfly.evox.models.client.ChatCompletionResponse;
import io.leavesfly.evox.models.client.ChatCompletionResult;
import io.leavesfly.evox.models.client.ToolCall;
import io.leavesfly.evox.models.client.ToolDefinition;
import io.leavesfly.evox.models.config.LLMConfig;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
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
 * Gemini LLM 实现
 * 通过 Google Generative AI API 调用 Gemini 模型
 *
 * @author EvoX Team
 */
@Slf4j
public class GeminiLLM implements LLMProvider {

    private static final String GENERATE_CONTENT_PATH = "/v1beta/models/%s:generateContent";
    private static final String STREAM_GENERATE_CONTENT_PATH = "/v1beta/models/%s:streamGenerateContent?alt=sse";
    private static final String SSE_DATA_PREFIX = "data: ";
    private static final String SSE_DONE_MARKER = "[DONE]";

    private final GeminiLLMConfig config;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public GeminiLLM(GeminiLLMConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();

        Duration timeout = config.getTimeout() != null ? config.getTimeout() : Duration.ofSeconds(60);

        this.webClient = WebClient.builder()
                .baseUrl(config.getBaseUrl())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();

        log.info("Initialized Gemini LLM with model: {}", config.getModel());
    }

    @Override
    public String generate(String prompt) {
        try {
            log.debug("Generating response for prompt: {}", prompt.substring(0, Math.min(50, prompt.length())));

            GeminiRequest request = buildGenerateRequest(prompt, null);
            String response = webClient.post()
                    .uri(String.format(GENERATE_CONTENT_PATH, config.getModel()) + "?key=" + config.getApiKey())
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(config.getTimeout())
                    .block();

            return extractContentFromResponse(response);
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
            GeminiRequest request = buildGenerateRequest(prompt, null);

            return webClient.post()
                    .uri(String.format(STREAM_GENERATE_CONTENT_PATH, config.getModel()) + "&key=" + config.getApiKey())
                    .bodyValue(request)
                    .retrieve()
                    .bodyToFlux(String.class)
                    .timeout(config.getTimeout())
                    .filter(line -> line.startsWith(SSE_DATA_PREFIX))
                    .map(line -> line.substring(SSE_DATA_PREFIX.length()).trim())
                    .filter(data -> !data.equals(SSE_DONE_MARKER))
                    .mapNotNull(this::parseStreamChunkContent)
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
            GeminiRequest request = buildChatRequest(messages, null);
            String response = webClient.post()
                    .uri(String.format(GENERATE_CONTENT_PATH, config.getModel()) + "?key=" + config.getApiKey())
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(config.getTimeout())
                    .block();

            return extractContentFromResponse(response);
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
        GeminiRequest request = buildChatRequest(messages, null);

        return webClient.post()
                .uri(String.format(STREAM_GENERATE_CONTENT_PATH, config.getModel()) + "&key=" + config.getApiKey())
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(config.getTimeout())
                .filter(line -> line.startsWith(SSE_DATA_PREFIX))
                .map(line -> line.substring(SSE_DATA_PREFIX.length()).trim())
                .filter(data -> !data.equals(SSE_DONE_MARKER))
                .mapNotNull(this::parseStreamChunkContent)
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

            List<GeminiTool> geminiTools = convertToolSchemasToGeminiTools(toolSchemas);
            GeminiRequest request = buildChatRequest(messages, geminiTools);
            String response = webClient.post()
                    .uri(String.format(GENERATE_CONTENT_PATH, config.getModel()) + "?key=" + config.getApiKey())
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(config.getTimeout())
                    .block();

            return parseToolUseResponse(response);
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

            List<GeminiTool> geminiTools = convertToolDefinitionsToGeminiTools(toolDefinitions);
            GeminiRequest request = buildChatRequest(messages, geminiTools);
            String response = webClient.post()
                    .uri(String.format(GENERATE_CONTENT_PATH, config.getModel()) + "?key=" + config.getApiKey())
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(config.getTimeout())
                    .block();

            return parseToolUseResponseToResult(response);
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

            List<GeminiTool> geminiTools = convertToolDefinitionsToGeminiTools(toolDefinitions);
            GeminiRequest request = buildChatRequest(messages, geminiTools);

            return webClient.post()
                    .uri(String.format(STREAM_GENERATE_CONTENT_PATH, config.getModel()) + "&key=" + config.getApiKey())
                    .bodyValue(request)
                    .retrieve()
                    .bodyToFlux(String.class)
                    .timeout(config.getTimeout())
                    .filter(line -> line.startsWith(SSE_DATA_PREFIX))
                    .map(line -> line.substring(SSE_DATA_PREFIX.length()).trim())
                    .filter(data -> !data.equals(SSE_DONE_MARKER))
                    .mapNotNull(this::parseStreamChunk);
        } catch (Exception e) {
            log.error("Error in streaming chat with tool definitions", e);
            return Flux.error(new RuntimeException("Failed to stream chat with tool definitions", e));
        }
    }

    private GeminiRequest buildGenerateRequest(String prompt, List<GeminiTool> tools) {
        GeminiRequest request = new GeminiRequest();
        GeminiContent content = new GeminiContent();
        content.role = "user";
        content.parts = List.of(new GeminiPart(prompt));
        request.contents = List.of(content);
        request.generationConfig = new GenerationConfig();
        request.generationConfig.temperature = config.getTemperature();
        request.generationConfig.maxOutputTokens = config.getMaxTokens();
        if (tools != null && !tools.isEmpty()) {
            request.tools = tools;
        }
        return request;
    }

    private GeminiRequest buildChatRequest(List<Message> messages, List<GeminiTool> tools) {
        GeminiRequest request = new GeminiRequest();
        List<GeminiContent> contents = new ArrayList<>();

        for (Message message : messages) {
            GeminiContent content = new GeminiContent();
            MessageType messageType = message.getMessageType();

            if (messageType == MessageType.SYSTEM) {
                content.role = "user";
                content.parts = List.of(new GeminiPart("System: " + message.getContent().toString()));
            } else if (messageType == MessageType.INPUT) {
                content.role = "user";
                content.parts = List.of(new GeminiPart(message.getContent().toString()));
            } else if (messageType == MessageType.OUTPUT || messageType == MessageType.RESPONSE) {
                content.role = "model";
                content.parts = List.of(new GeminiPart(message.getContent().toString()));
            } else {
                content.role = "user";
                content.parts = List.of(new GeminiPart(message.getContent().toString()));
            }
            contents.add(content);
        }

        request.contents = contents;
        request.generationConfig = new GenerationConfig();
        request.generationConfig.temperature = config.getTemperature();
        request.generationConfig.maxOutputTokens = config.getMaxTokens();
        if (tools != null && !tools.isEmpty()) {
            request.tools = tools;
        }
        return request;
    }

    private String extractContentFromResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode candidates = root.get("candidates");
            if (candidates != null && candidates.isArray() && candidates.size() > 0) {
                JsonNode firstCandidate = candidates.get(0);
                JsonNode content = firstCandidate.get("content");
                if (content != null) {
                    JsonNode parts = content.get("parts");
                    if (parts != null && parts.isArray() && parts.size() > 0) {
                        JsonNode firstPart = parts.get(0);
                        JsonNode textNode = firstPart.get("text");
                        if (textNode != null) {
                            return textNode.asText();
                        }
                    }
                }
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to parse response", e);
        }
        return "";
    }

    private String parseStreamChunkContent(String jsonData) {
        try {
            JsonNode root = objectMapper.readTree(jsonData);
            JsonNode candidates = root.get("candidates");
            if (candidates != null && candidates.isArray() && candidates.size() > 0) {
                JsonNode firstCandidate = candidates.get(0);
                JsonNode content = firstCandidate.get("content");
                if (content != null) {
                    JsonNode parts = content.get("parts");
                    if (parts != null && parts.isArray() && parts.size() > 0) {
                        JsonNode firstPart = parts.get(0);
                        JsonNode textNode = firstPart.get("text");
                        if (textNode != null) {
                            return textNode.asText();
                        }
                    }
                }
            }
        } catch (JsonProcessingException e) {
            log.debug("Failed to parse SSE chunk: {}", jsonData, e);
        }
        return null;
    }

    private ChatCompletionResponse parseStreamChunk(String jsonData) {
        try {
            JsonNode root = objectMapper.readTree(jsonData);
            ChatCompletionResponse response = new ChatCompletionResponse();
            List<ChatCompletionResponse.Choice> choices = new ArrayList<>();

            JsonNode candidates = root.get("candidates");
            if (candidates != null && candidates.isArray() && candidates.size() > 0) {
                JsonNode firstCandidate = candidates.get(0);
                ChatCompletionResponse.Choice choice = new ChatCompletionResponse.Choice();
                ChatCompletionResponse.Delta delta = new ChatCompletionResponse.Delta();

                JsonNode content = firstCandidate.get("content");
                if (content != null) {
                    JsonNode parts = content.get("parts");
                    if (parts != null && parts.isArray() && parts.size() > 0) {
                        JsonNode firstPart = parts.get(0);
                        JsonNode textNode = firstPart.get("text");
                        if (textNode != null) {
                            delta.setContent(textNode.asText());
                        }
                        JsonNode functionCallNode = firstPart.get("functionCall");
                        if (functionCallNode != null) {
                            ToolCall toolCall = convertGeminiFunctionCallToToolCall(functionCallNode);
                            List<ToolCall> toolCalls = new ArrayList<>();
                            toolCalls.add(toolCall);
                            delta.setToolCalls(toolCalls);
                        }
                    }
                }

                choice.setDelta(delta);
                choices.add(choice);
            }

            response.setChoices(choices);
            return response;
        } catch (JsonProcessingException e) {
            log.debug("Failed to parse SSE chunk: {}", jsonData, e);
            return null;
        }
    }

    private List<GeminiTool> convertToolSchemasToGeminiTools(List<Map<String, Object>> toolSchemas) {
        List<GeminiTool> tools = new ArrayList<>();
        for (Map<String, Object> schema : toolSchemas) {
            @SuppressWarnings("unchecked")
            Map<String, Object> functionMap = (Map<String, Object>) schema.get("function");
            if (functionMap != null) {
                GeminiTool tool = new GeminiTool();
                tool.functionDeclarations = new ArrayList<>();
                GeminiFunctionDeclaration funcDecl = new GeminiFunctionDeclaration();
                funcDecl.name = (String) functionMap.get("name");
                funcDecl.description = (String) functionMap.get("description");

                @SuppressWarnings("unchecked")
                Map<String, Object> paramsMap = (Map<String, Object>) functionMap.get("parameters");
                if (paramsMap != null) {
                    funcDecl.parameters = objectMapper.convertValue(paramsMap, JsonNode.class);
                }

                tool.functionDeclarations.add(funcDecl);
                tools.add(tool);
            }
        }
        return tools;
    }

    private List<GeminiTool> convertToolDefinitionsToGeminiTools(List<ToolDefinition> toolDefinitions) {
        List<GeminiTool> tools = new ArrayList<>();
        for (ToolDefinition toolDef : toolDefinitions) {
            GeminiTool tool = new GeminiTool();
            tool.functionDeclarations = new ArrayList<>();
            GeminiFunctionDeclaration funcDecl = new GeminiFunctionDeclaration();
            funcDecl.name = toolDef.getFunction().getName();
            funcDecl.description = toolDef.getFunction().getDescription();

            if (toolDef.getFunction().getParameters() != null) {
                funcDecl.parameters = objectMapper.convertValue(toolDef.getFunction().getParameters(), JsonNode.class);
            }

            tool.functionDeclarations.add(funcDecl);
            tools.add(tool);
        }
        return tools;
    }

    private Map<String, Object> parseToolUseResponse(String response) {
        Map<String, Object> resultMap = new HashMap<>();
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode candidates = root.get("candidates");
            
            if (candidates != null && candidates.isArray() && candidates.size() > 0) {
                JsonNode firstCandidate = candidates.get(0);
                JsonNode content = firstCandidate.get("content");
                
                StringBuilder textContent = new StringBuilder();
                List<ToolCall> toolCalls = new ArrayList<>();

                if (content != null) {
                    JsonNode parts = content.get("parts");
                    if (parts != null && parts.isArray()) {
                        for (JsonNode part : parts) {
                            JsonNode textNode = part.get("text");
                            if (textNode != null) {
                                textContent.append(textNode.asText());
                            }
                            JsonNode functionCallNode = part.get("functionCall");
                            if (functionCallNode != null) {
                                ToolCall toolCall = convertGeminiFunctionCallToToolCall(functionCallNode);
                                toolCalls.add(toolCall);
                            }
                        }
                    }
                }

                resultMap.put("content", textContent.toString());
                resultMap.put("tool_calls", toolCalls);
                resultMap.put("finish_reason", toolCalls.isEmpty() ? "stop" : "tool_calls");
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to parse tool use response", e);
        }
        return resultMap;
    }

    private ChatCompletionResult parseToolUseResponseToResult(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode candidates = root.get("candidates");
            
            if (candidates != null && candidates.isArray() && candidates.size() > 0) {
                JsonNode firstCandidate = candidates.get(0);
                JsonNode content = firstCandidate.get("content");
                JsonNode finishReasonNode = firstCandidate.get("finishReason");
                String finishReason = finishReasonNode != null ? finishReasonNode.asText() : "stop";
                
                StringBuilder textContent = new StringBuilder();
                List<ToolCall> toolCalls = new ArrayList<>();

                if (content != null) {
                    JsonNode parts = content.get("parts");
                    if (parts != null && parts.isArray()) {
                        for (JsonNode part : parts) {
                            JsonNode textNode = part.get("text");
                            if (textNode != null) {
                                textContent.append(textNode.asText());
                            }
                            JsonNode functionCallNode = part.get("functionCall");
                            if (functionCallNode != null) {
                                ToolCall toolCall = convertGeminiFunctionCallToToolCall(functionCallNode);
                                toolCalls.add(toolCall);
                            }
                        }
                    }
                }

                ChatCompletionResult result = new ChatCompletionResult();
                result.setContent(textContent.toString());
                result.setToolCalls(toolCalls);
                result.setFinishReason(toolCalls.isEmpty() ? "stop" : "tool_calls");
                result.setUsage(ChatCompletionResult.TokenUsage.builder()
                        .promptTokens(0)
                        .completionTokens(0)
                        .totalTokens(0)
                        .build());
                return result;
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to parse tool use response", e);
        }
        return new ChatCompletionResult();
    }

    private ToolCall convertGeminiFunctionCallToToolCall(JsonNode functionCallNode) {
        String name = functionCallNode.has("name") ? functionCallNode.get("name").asText() : "";
        JsonNode argsNode = functionCallNode.get("args");
        String argsJson = argsNode != null ? argsNode.toString() : "{}";

        return ToolCall.builder()
                .id(java.util.UUID.randomUUID().toString())
                .type("function")
                .function(ToolCall.FunctionCall.builder()
                        .name(name)
                        .arguments(argsJson)
                        .build())
                .build();
    }

    // Gemini API 数据结构
    @Data
    private static class GeminiRequest {
        private List<GeminiContent> contents;
        private GenerationConfig generationConfig;
        private List<GeminiTool> tools;
    }

    @Data
    private static class GeminiContent {
        private String role;
        private List<GeminiPart> parts;
    }

    @Data
    private static class GeminiPart {
        private String text;
        
        public GeminiPart(String text) {
            this.text = text;
        }
    }

    @Data
    private static class GenerationConfig {
        private Float temperature;
        private Integer maxOutputTokens;
        private Float topP;
    }

    @Data
    private static class GeminiTool {
        @JsonProperty("functionDeclarations")
        private List<GeminiFunctionDeclaration> functionDeclarations;
    }

    @Data
    private static class GeminiFunctionDeclaration {
        private String name;
        private String description;
        private JsonNode parameters;
    }
}
