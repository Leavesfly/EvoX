package io.leavesfly.evox.rag;

import io.leavesfly.evox.rag.config.RAGConfig;
import io.leavesfly.evox.rag.embedding.EmbeddingService;
import io.leavesfly.evox.rag.schema.Document;
import io.leavesfly.evox.rag.schema.RetrievalResult;
import io.leavesfly.evox.rag.vectorstore.InMemoryVectorStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RAG引擎测试
 *
 * @author EvoX Team
 */
class RAGEngineTest {

    private RAGEngine ragEngine;
    private MockEmbeddingService embeddingService;

    @BeforeEach
    void setUp() {
        // 创建配置
        RAGConfig config = RAGConfig.builder()
                .chunker(RAGConfig.ChunkerConfig.builder()
                        .strategy("FIXED_SIZE")
                        .chunkSize(100)
                        .chunkOverlap(20)
                        .build())
                .retriever(RAGConfig.RetrieverConfig.builder()
                        .topK(3)
                        .similarityThreshold(0.5)
                        .build())
                .build();

        // 创建服务
        embeddingService = new MockEmbeddingService(128);
        InMemoryVectorStore vectorStore = new InMemoryVectorStore();

        // 创建RAG引擎
        ragEngine = new RAGEngine(config, embeddingService, vectorStore);
    }

    @Test
    void testIndexSingleDocument() {
        Document doc = Document.builder()
                .text("This is a test document for RAG system testing. " +
                      "It contains multiple sentences to test chunking.")
                .source("test.txt")
                .type(Document.DocumentType.TEXT)
                .build();

        int chunkCount = ragEngine.indexDocument(doc);
        
        assertTrue(chunkCount > 0, "Should create at least one chunk");
        assertEquals(chunkCount, ragEngine.getIndexedChunkCount());
    }

    @Test
    void testIndexMultipleDocuments() {
        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            documents.add(Document.builder()
                    .text("Document " + i + ": " + "Lorem ipsum dolor sit amet. ".repeat(10))
                    .source("doc" + i + ".txt")
                    .build());
        }

        int totalChunks = ragEngine.indexDocuments(documents);
        
        assertTrue(totalChunks > 0);
        assertEquals(totalChunks, ragEngine.getIndexedChunkCount());
    }

    @Test
    void testRetrieve() {
        // 索引测试文档
        Document doc1 = Document.builder()
                .text("Machine learning is a branch of artificial intelligence.")
                .source("ml.txt")
                .build();
        
        Document doc2 = Document.builder()
                .text("Natural language processing enables computers to understand human language.")
                .source("nlp.txt")
                .build();

        ragEngine.indexDocument(doc1);
        ragEngine.indexDocument(doc2);

        // 执行检索
        RetrievalResult result = ragEngine.retrieve("artificial intelligence");
        
        assertNotNull(result);
        assertTrue(result.getTotalResults() > 0);
        assertNotNull(result.getTopChunk());
        assertTrue(result.getLatencyMs() >= 0);
    }

    @Test
    void testRetrieveWithTopK() {
        // 索引多个文档
        for (int i = 0; i < 5; i++) {
            Document doc = Document.builder()
                    .text("Test document " + i + " with some content about testing.")
                    .source("test" + i + ".txt")
                    .build();
            ragEngine.indexDocument(doc);
        }

        // 检索并限制结果数量
        RetrievalResult result = ragEngine.retrieve("testing", 3);
        
        assertNotNull(result);
        assertTrue(result.getTotalResults() <= 3);
    }

    @Test
    void testClear() {
        Document doc = Document.builder()
                .text("Test document")
                .build();
        
        ragEngine.indexDocument(doc);
        assertTrue(ragEngine.getIndexedChunkCount() > 0);
        
        ragEngine.clear();
        assertEquals(0, ragEngine.getIndexedChunkCount());
    }

    @Test
    void testGetCombinedText() {
        Document doc = Document.builder()
                .text("First sentence. Second sentence. Third sentence.")
                .build();
        
        ragEngine.indexDocument(doc);
        
        RetrievalResult result = ragEngine.retrieve("sentence");
        String combined = result.getCombinedText(" | ");
        
        assertNotNull(combined);
        assertFalse(combined.isEmpty());
    }

    /**
     * 模拟嵌入服务 - 生成随机向量用于测试
     */
    private static class MockEmbeddingService implements EmbeddingService {
        private final int dimensions;
        private final Random random = new Random(42);

        public MockEmbeddingService(int dimensions) {
            this.dimensions = dimensions;
        }

        @Override
        public List<Float> embed(String text) {
            List<Float> embedding = new ArrayList<>();
            for (int i = 0; i < dimensions; i++) {
                embedding.add(random.nextFloat());
            }
            return embedding;
        }

        @Override
        public List<List<Float>> embedBatch(List<String> texts) {
            return texts.stream()
                    .map(this::embed)
                    .toList();
        }

        @Override
        public int getDimensions() {
            return dimensions;
        }
    }
}
