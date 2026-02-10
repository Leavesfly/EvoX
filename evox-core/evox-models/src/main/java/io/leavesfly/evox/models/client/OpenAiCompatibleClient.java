package io.leavesfly.evox.models.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

/**
 * OpenAI 兼容协议的 HTTP 客户端
 * 基于 WebClient 实现，支持同步、异步和流式调用
 * 适用于 OpenAI、Ollama、SiliconFlow、OpenRouter、LiteLLM 等所有兼容 OpenAI 协议的服务
 *
 * @author EvoX Team
 */
@Slf4j
public class OpenAiCompatibleClient {

    private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";
    private static final String EMBEDDINGS_PATH = "/embeddings";
    private static final String IMAGES_GENERATIONS_PATH = "/images/generations";
    private static final String SSE_DATA_PREFIX = "data: ";
    private static final String SSE_DONE_MARKER = "[DONE]";

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final Duration timeout;

    /**
     * 构造函数
     *
     * @param baseUrl API 基础 URL（如 https://api.openai.com/v1）
     * @param apiKey  API 密钥
     * @param timeout 请求超时时间
     */
    public OpenAiCompatibleClient(String baseUrl, String apiKey, Duration timeout) {
        this.objectMapper = new ObjectMapper();
        this.timeout = timeout != null ? timeout : Duration.ofSeconds(60);

        WebClient.Builder builder = WebClient.builder()
                .baseUrl(normalizeBaseUrl(baseUrl))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        if (apiKey != null && !apiKey.isEmpty()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        }

        this.webClient = builder.build();
    }

    /**
     * 同步调用 Chat Completion
     *
     * @param request 请求体
     * @return 响应内容文本
     */
    public String chatCompletion(ChatCompletionRequest request) {
        request.setStream(false);

        ChatCompletionResponse response = webClient.post()
                .uri(CHAT_COMPLETIONS_PATH)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ChatCompletionResponse.class)
                .timeout(timeout)
                .block();

        return extractContent(response);
    }

    /**
     * 同步调用 Chat Completion，返回完整结果（包含工具调用信息）
     *
     * @param request 请求体
     * @return 包含文本和工具调用的完整结果
     */
    public ChatCompletionResult chatCompletionWithTools(ChatCompletionRequest request) {
        request.setStream(false);

        ChatCompletionResponse response = webClient.post()
                .uri(CHAT_COMPLETIONS_PATH)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ChatCompletionResponse.class)
                .timeout(timeout)
                .block();

        return ChatCompletionResult.fromResponse(response);
    }

    /**
     * 异步调用 Chat Completion
     *
     * @param request 请求体
     * @return 响应内容文本（Mono）
     */
    public Mono<String> chatCompletionAsync(ChatCompletionRequest request) {
        request.setStream(false);

        return webClient.post()
                .uri(CHAT_COMPLETIONS_PATH)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ChatCompletionResponse.class)
                .timeout(timeout)
                .map(this::extractContent);
    }

    /**
     * 流式调用 Chat Completion（SSE）
     *
     * @param request 请求体
     * @return 响应内容文本流（Flux），每个元素为一个增量 token
     */
    public Flux<String> chatCompletionStream(ChatCompletionRequest request) {
        request.setStream(true);

        return webClient.post()
                .uri(CHAT_COMPLETIONS_PATH)
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(timeout)
                .filter(line -> line.startsWith(SSE_DATA_PREFIX))
                .map(line -> line.substring(SSE_DATA_PREFIX.length()).trim())
                .filter(data -> !data.equals(SSE_DONE_MARKER))
                .mapNotNull(this::parseStreamChunkContent);
    }

    /**
     * 流式调用 Chat Completion，返回完整的 SSE chunk（包含工具调用增量）
     *
     * @param request 请求体
     * @return SSE chunk 流，每个元素为一个解析后的 ChatCompletionResponse
     */
    public Flux<ChatCompletionResponse> chatCompletionStreamRaw(ChatCompletionRequest request) {
        request.setStream(true);

        return webClient.post()
                .uri(CHAT_COMPLETIONS_PATH)
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(timeout)
                .filter(line -> line.startsWith(SSE_DATA_PREFIX))
                .map(line -> line.substring(SSE_DATA_PREFIX.length()).trim())
                .filter(data -> !data.equals(SSE_DONE_MARKER))
                .mapNotNull(this::parseStreamChunk);
    }

    /**
     * 调用 Embedding 接口
     *
     * @param request 请求体
     * @return Embedding 响应
     */
    public EmbeddingResponse embeddings(EmbeddingRequest request) {
        return webClient.post()
                .uri(EMBEDDINGS_PATH)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(EmbeddingResponse.class)
                .timeout(timeout)
                .block();
    }

    /**
     * 调用图像生成接口
     *
     * @param request 请求体
     * @return 图像生成响应
     */
    public ImageGenerationResponse imageGeneration(ImageGenerationRequest request) {
        return webClient.post()
                .uri(IMAGES_GENERATIONS_PATH)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ImageGenerationResponse.class)
                .timeout(timeout)
                .block();
    }

    /**
     * 构建 Chat Completion 请求
     */
    public static ChatCompletionRequest buildChatRequest(String model, String userMessage,
                                                          Float temperature, Integer maxTokens,
                                                          Float topP, Float frequencyPenalty,
                                                          Float presencePenalty) {
        ChatCompletionRequest.ChatMessage message = ChatCompletionRequest.ChatMessage.builder()
                .role("user")
                .content(userMessage)
                .build();

        return ChatCompletionRequest.builder()
                .model(model)
                .messages(List.of(message))
                .temperature(temperature)
                .maxTokens(maxTokens)
                .topP(topP)
                .frequencyPenalty(frequencyPenalty)
                .presencePenalty(presencePenalty)
                .build();
    }

    /**
     * 构建多消息的 Chat Completion 请求
     */
    public static ChatCompletionRequest buildChatRequest(String model, List<ChatCompletionRequest.ChatMessage> messages,
                                                          Float temperature, Integer maxTokens,
                                                          Float topP, Float frequencyPenalty,
                                                          Float presencePenalty) {
        return ChatCompletionRequest.builder()
                .model(model)
                .messages(messages)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .topP(topP)
                .frequencyPenalty(frequencyPenalty)
                .presencePenalty(presencePenalty)
                .build();
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
            ChatCompletionResponse chunk = objectMapper.readValue(jsonData, ChatCompletionResponse.class);
            if (chunk.getChoices() != null && !chunk.getChoices().isEmpty()) {
                ChatCompletionResponse.Delta delta = chunk.getChoices().get(0).getDelta();
                if (delta != null && delta.getContent() != null) {
                    return delta.getContent();
                }
            }
        } catch (JsonProcessingException e) {
            log.debug("Failed to parse SSE chunk: {}", jsonData, e);
        }
        return null;
    }

    private ChatCompletionResponse parseStreamChunk(String jsonData) {
        try {
            return objectMapper.readValue(jsonData, ChatCompletionResponse.class);
        } catch (JsonProcessingException e) {
            log.debug("Failed to parse SSE chunk: {}", jsonData, e);
            return null;
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isEmpty()) {
            return "https://api.openai.com/v1";
        }
        // 移除尾部斜杠
        String normalized = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        // 如果 URL 不以 /v1 结尾且不包含版本路径，自动追加
        if (!normalized.endsWith("/v1") && !normalized.matches(".*/(v\\d+)$")) {
            // 对于 Ollama 等不需要 /v1 的服务，保持原样
            // Ollama 的 OpenAI 兼容端点是 /v1，所以也需要追加
            if (!normalized.contains("/api")) {
                normalized = normalized + "/v1";
            }
        }
        return normalized;
    }
}
