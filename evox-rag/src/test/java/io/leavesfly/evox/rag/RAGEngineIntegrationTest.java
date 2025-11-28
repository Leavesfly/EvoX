package io.leavesfly.evox.rag;

import io.leavesfly.evox.rag.config.RAGConfig;
import io.leavesfly.evox.rag.embedding.EmbeddingService;
import io.leavesfly.evox.rag.schema.Document;
import io.leavesfly.evox.rag.vectorstore.InMemoryVectorStore;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * RAG引擎集成测试
 */
@Slf4j
class RAGEngineIntegrationTest {

    private RAGEngine ragEngine;
    private EmbeddingService embeddingService;

    @BeforeEach
    void setUp() {
        // 创建RAG配置
        RAGConfig config = RAGConfig.builder()
                .embedding(RAGConfig.EmbeddingConfig.builder()
                        .modelName("text-embedding-ada-002")
                        .provider("openai")
                        .dimension(1536)
                        .build())
                .chunker(RAGConfig.ChunkerConfig.builder()
                        .strategy("fixed")
                        .chunkSize(256)
                        .chunkOverlap(20)
                        .build())
                .retriever(RAGConfig.RetrieverConfig.builder()
                        .retrievalType("vector")
                        .topK(3)
                        .similarityThreshold(0.7)
                        .build())
                .reader(RAGConfig.ReaderConfig.builder()
                        .readerType("txt")
                        .build())
                .generator(RAGConfig.GeneratorConfig.builder()
                        .modelName("gpt-3.5-turbo")
                        .temperature(0.7f)
                        .build())
                .build();

        // 创建模拟的嵌入服务
        embeddingService = mock(EmbeddingService.class);
        when(embeddingService.embed(anyString())).thenReturn(Arrays.asList(0.1f, 0.2f, 0.3f));
        when(embeddingService.embedBatch(anyList())).thenReturn(
            Arrays.asList(
                Arrays.asList(0.1f, 0.2f, 0.3f),
                Arrays.asList(0.4f, 0.5f, 0.6f),
                Arrays.asList(0.7f, 0.8f, 0.9f)
            )
        );
        when(embeddingService.getDimensions()).thenReturn(1536);

        InMemoryVectorStore vectorStore = new InMemoryVectorStore();

        ragEngine = new RAGEngine(config, embeddingService, vectorStore);
    }

    @Test
    void testSimpleRAG() {
        log.info("=== 测试简单RAG流程 ===");

        // 准备测试文档
        List<Document> documents = Arrays.asList(
                Document.builder()
                        .id("doc1")
                        .text("机器学习是人工智能的一个分支，它使计算机能够从数据中学习。")
                        .build(),
                Document.builder()
                        .id("doc2")
                        .text("深度学习是机器学习的一个子集，使用神经网络来模拟人脑的工作方式。")
                        .build(),
                Document.builder()
                        .id("doc3")
                        .text("自然语言处理是计算机科学和人工智能领域的一个重要方向，用于处理和理解人类语言。")
                        .build()
        );

        // 索引文档
        ragEngine.indexDocuments(documents);

        // 验证嵌入服务被调用
        verify(embeddingService, atLeastOnce()).embedBatch(anyList());

        log.info("成功完成简单RAG测试");
    }

    @Test
    void testAdvancedRAG() {
        log.info("=== 测试高级RAG流程 ===");

        // 创建更复杂的配置
        RAGConfig config = RAGConfig.builder()
                .embedding(RAGConfig.EmbeddingConfig.builder()
                        .modelName("text-embedding-ada-002")
                        .provider("openai")
                        .dimension(1536)
                        .build())
                .chunker(RAGConfig.ChunkerConfig.builder()
                        .strategy("semantic")
                        .chunkSize(512)
                        .chunkOverlap(50)
                        .build())
                .retriever(RAGConfig.RetrieverConfig.builder()
                        .retrievalType("hybrid")
                        .topK(5)
                        .similarityThreshold(0.6)
                        .build())
                .reader(RAGConfig.ReaderConfig.builder()
                        .readerType("txt")
                        .extractMetadata(true)
                        .cleanText(true)
                        .build())
                .generator(RAGConfig.GeneratorConfig.builder()
                        .modelName("gpt-4")
                        .temperature(0.5f)
                        .maxTokens(1500)
                        .build())
                .build();

        // 创建模拟的嵌入服务
        EmbeddingService advancedEmbeddingService = mock(EmbeddingService.class);
        when(advancedEmbeddingService.embed(anyString())).thenReturn(Arrays.asList(0.1f, 0.2f, 0.3f));
        when(advancedEmbeddingService.embedBatch(anyList())).thenReturn(
            Arrays.asList(
                Arrays.asList(0.1f, 0.2f, 0.3f),
                Arrays.asList(0.4f, 0.5f, 0.6f),
                Arrays.asList(0.7f, 0.8f, 0.9f),
                Arrays.asList(0.2f, 0.3f, 0.4f),
                Arrays.asList(0.5f, 0.6f, 0.7f)
            )
        );
        when(advancedEmbeddingService.getDimensions()).thenReturn(1536);

        InMemoryVectorStore vectorStore = new InMemoryVectorStore();

        RAGEngine advancedEngine = new RAGEngine(config, advancedEmbeddingService, vectorStore);

        // 准备测试文档
        List<Document> documents = Arrays.asList(
                Document.builder()
                        .id("doc1")
                        .text("人工智能（AI）是计算机科学的一个分支，旨在创建能够执行通常需要人类智能的任务的系统。")
                        .build(),
                Document.builder()
                        .id("doc2")
                        .text("机器学习是实现人工智能的一种方法，它使计算机能够从经验中学习。")
                        .build(),
                Document.builder()
                        .id("doc3")
                        .text("深度学习是机器学习的一个子领域，它使用多层神经网络来学习数据的复杂模式。")
                        .build(),
                Document.builder()
                        .id("doc4")
                        .text("自然语言处理（NLP）是人工智能的一个重要应用领域，专注于计算机与人类语言的交互。")
                        .build(),
                Document.builder()
                        .id("doc5")
                        .text("计算机视觉是人工智能的另一个重要分支，专注于使计算机能够理解和解释视觉信息。")
                        .build()
        );

        // 索引文档
        advancedEngine.indexDocuments(documents);

        // 验证嵌入服务被调用
        verify(advancedEmbeddingService, atLeastOnce()).embedBatch(anyList());

        log.info("成功完成高级RAG测试");
    }
}