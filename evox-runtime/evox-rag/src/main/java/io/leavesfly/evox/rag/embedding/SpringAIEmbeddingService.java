package io.leavesfly.evox.rag.embedding;

import io.leavesfly.evox.models.protocol.OpenAiCompatibleClient;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 基于 OpenAI 兼容 HTTP 客户端的嵌入服务实现
 *
 * @author EvoX Team
 */
@Slf4j
public class SpringAIEmbeddingService implements EmbeddingService {

    private final OpenAiCompatibleClient client;
    private final String model;
    private final int dimensions;

    /**
     * 使用 OpenAI 默认配置构造
     *
     * @param apiKey     API 密钥
     * @param model      嵌入模型名称（如 text-embedding-ada-002）
     * @param dimensions 嵌入维度
     */
    public SpringAIEmbeddingService(String apiKey, String model, int dimensions) {
        this("https://api.openai.com/v1", apiKey, model, dimensions);
    }

    /**
     * 使用自定义 baseUrl 构造
     *
     * @param baseUrl    API 基础 URL
     * @param apiKey     API 密钥
     * @param model      嵌入模型名称
     * @param dimensions 嵌入维度
     */
    public SpringAIEmbeddingService(String baseUrl, String apiKey, String model, int dimensions) {
        this.client = new OpenAiCompatibleClient(baseUrl, apiKey, Duration.ofSeconds(60));
        this.model = model;
        this.dimensions = dimensions;
        log.info("Initialized Embedding Service with model: {}, dimensions: {}", model, dimensions);
    }

    @Override
    public List<Float> embed(String text) {
        try {
            if (text == null || text.trim().isEmpty()) {
                log.warn("Empty text provided for embedding");
                return new ArrayList<>();
            }

            String processedText = text.replace("\n", " ");

            io.leavesfly.evox.models.protocol.EmbeddingRequest request =
                    io.leavesfly.evox.models.protocol.EmbeddingRequest.builder()
                            .model(model)
                            .input(List.of(processedText))
                            .build();

            io.leavesfly.evox.models.protocol.EmbeddingResponse response = client.embeddings(request);

            if (response == null || response.getData() == null || response.getData().isEmpty()) {
                log.warn("No embedding result returned");
                return new ArrayList<>();
            }

            List<Float> embedding = response.getData().get(0)
                    .getEmbedding()
                    .stream()
                    .map(Double::floatValue)
                    .collect(Collectors.toList());

            log.debug("Generated embedding for text with {} dimensions", embedding.size());
            return embedding;

        } catch (Exception e) {
            log.error("Failed to generate embedding: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate embedding", e);
        }
    }

    @Override
    public List<List<Float>> embedBatch(List<String> texts) {
        try {
            if (texts == null || texts.isEmpty()) {
                log.warn("Empty text list provided for batch embedding");
                return new ArrayList<>();
            }

            List<String> processedTexts = texts.stream()
                    .map(text -> text != null ? text.replace("\n", " ") : "")
                    .collect(Collectors.toList());

            io.leavesfly.evox.models.protocol.EmbeddingRequest request =
                    io.leavesfly.evox.models.protocol.EmbeddingRequest.builder()
                            .model(model)
                            .input(processedTexts)
                            .build();

            io.leavesfly.evox.models.protocol.EmbeddingResponse response = client.embeddings(request);

            if (response == null || response.getData() == null) {
                log.warn("No embedding results returned");
                return new ArrayList<>();
            }

            List<List<Float>> embeddings = response.getData().stream()
                    .map(data -> data.getEmbedding().stream()
                            .map(Double::floatValue)
                            .collect(Collectors.toList()))
                    .collect(Collectors.toList());

            log.debug("Generated {} embeddings in batch", embeddings.size());
            return embeddings;

        } catch (Exception e) {
            log.error("Failed to generate batch embeddings: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate batch embeddings", e);
        }
    }

    @Override
    public int getDimensions() {
        return dimensions;
    }
}
