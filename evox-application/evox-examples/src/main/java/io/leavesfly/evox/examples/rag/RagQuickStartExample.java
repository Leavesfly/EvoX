package io.leavesfly.evox.examples.rag;

import io.leavesfly.evox.rag.RAGEngine;
import io.leavesfly.evox.rag.config.RAGConfig;
import io.leavesfly.evox.rag.embedding.EmbeddingService;
import io.leavesfly.evox.rag.schema.Document;
import io.leavesfly.evox.rag.schema.RetrievalResult;
import io.leavesfly.evox.rag.vectorstore.InMemoryVectorStore;
import io.leavesfly.evox.rag.vectorstore.VectorStore;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * RAG quick start demo.
 *
 * <p>本示例使用内存向量库和轻量嵌入实现，无需外部服务即可运行。</p>
 */
@Slf4j
public class RagQuickStartExample {

    private static final int EMBEDDING_DIMENSIONS = 8;

    public static void main(String[] args) {
        log.info("=== EvoX RAG Quick Start Demo ===");

        // 构建 RAG 配置：固定分块 + 向量检索
        RAGConfig config = RAGConfig.builder()
                .embedding(RAGConfig.EmbeddingConfig.builder()
                        .dimension(EMBEDDING_DIMENSIONS)
                        .build())
                .chunker(RAGConfig.ChunkerConfig.builder()
                        .strategy("fixed")
                        .chunkSize(180)
                        .chunkOverlap(20)
                        .build())
                .retriever(RAGConfig.RetrieverConfig.builder()
                        .topK(3)
                        .similarityThreshold(0.2)
                        .build())
                .build();

        // 使用简易嵌入服务与内存向量库，方便本地演示
        EmbeddingService embeddingService = new SimpleHashEmbeddingService(EMBEDDING_DIMENSIONS);
        VectorStore vectorStore = new InMemoryVectorStore();

        // 初始化 RAG 引擎
        RAGEngine ragEngine = new RAGEngine(config, embeddingService, vectorStore);

        List<Document> documents = List.of(
                Document.builder()
                        .source("evox-overview")
                        .metadata(java.util.Map.of("source", "evox-overview"))
                        .text("EvoX provides agents, workflows, tools, and memory systems for building AI applications.")
                        .build(),
                Document.builder()
                        .source("evox-workflow")
                        .metadata(java.util.Map.of("source", "evox-workflow"))
                        .text("Workflow module supports DAG execution, branching, parallel steps, and loop control.")
                        .build(),
                Document.builder()
                        .source("evox-tools")
                        .metadata(java.util.Map.of("source", "evox-tools"))
                        .text("Tools include file system, HTTP, database, search, calculator, and browser integrations.")
                        .build()
        );

        // 索引文档到向量库
        int chunks = ragEngine.indexDocuments(documents);
        log.info("Indexed documents into {} chunks", chunks);

        // 执行检索
        String query = "How does EvoX handle workflows?";
        RetrievalResult result = ragEngine.retrieve(query);

        log.info("Query: {}", query);
        log.info("Top results: {}", result.getTotalResults());
        result.getChunks().forEach(chunk -> {
            String source = String.valueOf(chunk.getMetadata().get("source"));
            String score = String.format("%.4f", chunk.getSimilarityScore());
            log.info("Score: {} | Source: {} | Text: {}", score, source, chunk.getText());
        });
    }

    /**
     * 简易嵌入服务：将文本哈希为固定维度向量，仅用于演示。
     */
    private static class SimpleHashEmbeddingService implements EmbeddingService {
        private final int dimensions;

        private SimpleHashEmbeddingService(int dimensions) {
            this.dimensions = dimensions;
        }

        @Override
        public List<Float> embed(String text) {
            float[] vector = new float[dimensions];
            if (text != null) {
                for (char c : text.toCharArray()) {
                    int index = Math.abs(c) % dimensions;
                    vector[index] += 1.0f;
                }
            }
            normalize(vector);
            return toList(vector);
        }

        @Override
        public List<List<Float>> embedBatch(List<String> texts) {
            if (texts == null || texts.isEmpty()) {
                return List.of();
            }
            List<List<Float>> embeddings = new ArrayList<>();
            for (String text : texts) {
                embeddings.add(embed(text));
            }
            return embeddings;
        }

        @Override
        public int getDimensions() {
            return dimensions;
        }

        private void normalize(float[] vector) {
            double norm = 0.0;
            for (float value : vector) {
                norm += value * value;
            }
            if (norm == 0.0) {
                return;
            }
            double sqrt = Math.sqrt(norm);
            for (int i = 0; i < vector.length; i++) {
                vector[i] = (float) (vector[i] / sqrt);
            }
        }

        private List<Float> toList(float[] vector) {
            List<Float> values = new ArrayList<>(vector.length);
            for (float value : vector) {
                values.add(value);
            }
            return values;
        }
    }
}
