package io.leavesfly.evox.models.groq;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.leavesfly.evox.core.llm.LLMConfig;
import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.core.message.MessageType;
import io.leavesfly.evox.models.base.LLMProvider;
import io.leavesfly.evox.models.client.ChatCompletionRequest;
import io.leavesfly.evox.models.client.ChatCompletionResponse;
import io.leavesfly.evox.models.client.ChatCompletionResult;
import io.leavesfly.evox.models.client.OpenAiCompatibleClient;
import io.leavesfly.evox.models.client.ToolCall;
import io.leavesfly.evox.models.client.ToolDefinition;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Groq LLM 实现
 * Groq 提供快速的 AI 推理服务，兼容 OpenAI 协议
 * 支持语音转写功能
 *
 * @author EvoX Team
 */
@Slf4j
public class GroqLLM implements LLMProvider {

    private final GroqLLMConfig config;
    private final OpenAiCompatibleClient client;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public GroqLLM(GroqLLMConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();

        String baseUrl = config.getBaseUrl() != null ? config.getBaseUrl() : "https://api.groq.com/openai/v1";
        this.client = new OpenAiCompatibleClient(baseUrl, config.getApiKey(), config.getTimeout());

        WebClient.Builder builder = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        if (config.getApiKey() != null && !config.getApiKey().isEmpty()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApiKey());
        }

        this.webClient = builder.build();

        log.info("Initialized Groq LLM with model: {}", config.getModel());
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
                    0.0f,
                    0.0f);

            String response = client.chatCompletion(request);

            if (Boolean.TRUE.equals(config.getOutputResponse())) {
                log.info("Groq Response: {}", response);
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
                0.0f,
                0.0f);

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
                    0.0f,
                    0.0f);

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
                log.info("Groq Chat Response: {}", response);
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
                log.info("Groq Tool Call Result: content={}, toolCalls={}, finishReason={}",
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

            request.setStreamOptions(Map.of("include_usage", true));

            return client.chatCompletionStreamRaw(request);
        } catch (Exception e) {
            log.error("Error in streaming chat with tool definitions", e);
            return Flux.error(new RuntimeException("Failed to stream chat with tool definitions", e));
        }
    }

    /**
     * 语音转写功能
     * 使用 Groq 的 Whisper 模型将音频转换为文本
     *
     * @param audioData 音频数据（字节数组）
     * @param language  语言代码（可选，如 "en", "zh"）
     * @return 转写后的文本
     */
    public String transcribeAudio(byte[] audioData, String language) {
        try {
            log.debug("Transcribing audio with language: {}", language);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new AudioResource(audioData, "audio.wav"));
            body.add("model", config.getWhisperModel());

            if (language != null && !language.isEmpty()) {
                body.add("language", language);
            }

            Duration timeout = config.getTimeout() != null ? config.getTimeout() : Duration.ofSeconds(60);

            ResponseEntity<String> response = webClient.post()
                    .uri("/audio/transcriptions")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .bodyValue(body)
                    .retrieve()
                    .toEntity(String.class)
                    .timeout(timeout)
                    .block();

            if (response != null && response.getBody() != null) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                JsonNode textNode = jsonNode.get("text");
                if (textNode != null) {
                    String transcription = textNode.asText();
                    log.info("Audio transcribed successfully, length: {}", transcription.length());
                    return transcription;
                }
            }

            log.warn("Audio transcription returned empty result");
            return "";
        } catch (Exception e) {
            log.error("Error transcribing audio", e);
            throw new RuntimeException("Failed to transcribe audio", e);
        }
    }

    /**
     * 语音转写功能（不指定语言）
     *
     * @param audioData 音频数据（字节数组）
     * @return 转写后的文本
     */
    public String transcribeAudio(byte[] audioData) {
        return transcribeAudio(audioData, null);
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

    /**
     * 用于 multipart 上传的音频资源
     */
    private static class AudioResource extends ByteArrayResource {
        private final String filename;

        public AudioResource(byte[] byteArray, String filename) {
            super(byteArray);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}
