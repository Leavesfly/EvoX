package io.leavesfly.evox.rag.embedding;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 基于Spring AI的嵌入服务实现
 *
 * @author EvoX Team
 */
@Slf4j
public class SpringAIEmbeddingService implements EmbeddingService {

    private final EmbeddingModel embeddingModel;
    private final int dimensions;

    public SpringAIEmbeddingService(EmbeddingModel embeddingModel) {
        this(embeddingModel, 1536); // OpenAI text-embedding-ada-002 default
    }

    public SpringAIEmbeddingService(EmbeddingModel embeddingModel, int dimensions) {
        this.embeddingModel = embeddingModel;
        this.dimensions = dimensions;
        log.info("Initialized Spring AI Embedding Service with dimensions: {}", dimensions);
    }

    @Override
    public List<Float> embed(String text) {
        try {
            if (text == null || text.trim().isEmpty()) {
                log.warn("Empty text provided for embedding");
                return new ArrayList<>();
            }

            // 移除换行符
            String processedText = text.replace("\n", " ");

            EmbeddingResponse response = embeddingModel.embedForResponse(List.of(processedText));
            
            if (response.getResults().isEmpty()) {
                log.warn("No embedding result returned");
                return new ArrayList<>();
            }

            // 转换为Float列表
            List<Float> embedding = response.getResults().get(0)
                    .getOutput()
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

            // 处理文本列表
            List<String> processedTexts = texts.stream()
                    .map(text -> text != null ? text.replace("\n", " ") : "")
                    .collect(Collectors.toList());

            EmbeddingResponse response = embeddingModel.embedForResponse(processedTexts);
            
            // 转换为Float列表
            List<List<Float>> embeddings = response.getResults().stream()
                    .map(result -> result.getOutput().stream()
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
