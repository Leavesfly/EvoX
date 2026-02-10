package io.leavesfly.evox.rag;

import io.leavesfly.evox.rag.chunker.Chunker;
import io.leavesfly.evox.rag.chunker.FixedSizeChunker;
import io.leavesfly.evox.rag.chunker.SemanticChunker;
import io.leavesfly.evox.rag.config.RAGConfig;
import io.leavesfly.evox.rag.embedding.EmbeddingService;
import io.leavesfly.evox.rag.reader.DocumentReader;
import io.leavesfly.evox.rag.reader.UniversalDocumentReader;
import io.leavesfly.evox.rag.reranker.Reranker;
import io.leavesfly.evox.rag.reranker.CrossEncoderReranker;
import io.leavesfly.evox.rag.reranker.LLMReranker;
import io.leavesfly.evox.rag.retriever.Retriever;
import io.leavesfly.evox.rag.retriever.VectorRetriever;
import io.leavesfly.evox.rag.schema.Chunk;
import io.leavesfly.evox.rag.schema.Document;
import io.leavesfly.evox.rag.schema.Query;
import io.leavesfly.evox.rag.schema.RetrievalResult;
import io.leavesfly.evox.rag.vectorstore.DocumentVectorStore;
import io.leavesfly.evox.core.llm.ILLM;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * RAG引擎 - 检索增强生成系统核心
 *
 * @author EvoX Team
 */
@Slf4j
public class RAGEngine {

    private final RAGConfig config;
    private final DocumentReader reader;
    private final Chunker chunker;
    private final EmbeddingService embeddingService;
    private final DocumentVectorStore vectorStore;
    private final Retriever retriever;
    private final Reranker reranker;

    public RAGEngine(
            RAGConfig config,
            EmbeddingService embeddingService,
            DocumentVectorStore vectorStore) {
        this(config, new UniversalDocumentReader(), embeddingService, vectorStore, null);
    }

    public RAGEngine(
            RAGConfig config,
            DocumentReader reader,
            EmbeddingService embeddingService,
            DocumentVectorStore vectorStore) {
        this(config, reader, embeddingService, vectorStore, null);
    }

    public RAGEngine(
            RAGConfig config,
            DocumentReader reader,
            EmbeddingService embeddingService,
            DocumentVectorStore vectorStore,
            ILLM llm) {
        this.config = config;
        this.reader = reader;
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
        
        // 初始化分块器
        this.chunker = createChunker(config.getChunker());
        
        // 初始化检索器
        this.retriever = new VectorRetriever(vectorStore, embeddingService, 
                config.getRetriever().getTopK());
        
        // 初始化重排序器
        this.reranker = createReranker(config, llm);
        
        log.info("RAG Engine initialized with config: {}", config);
    }

    /**
     * 索引单个文档
     *
     * @param document 文档
     * @return 生成的分块数量
     */
    public int indexDocument(Document document) {
        log.debug("Indexing document: {}", document.getId());
        
        // 1. 分块
        List<Chunk> chunks = chunker.chunk(document);
        log.debug("Created {} chunks for document {}", chunks.size(), document.getId());
        
        // 2. 生成嵌入向量
        List<String> chunkTexts = chunks.stream()
                .map(Chunk::getText)
                .toList();
        List<List<Float>> embeddings = embeddingService.embedBatch(chunkTexts);
        
        // 3. 设置嵌入向量
        for (int i = 0; i < chunks.size(); i++) {
            chunks.get(i).setEmbedding(embeddings.get(i));
        }
        
        // 4. 存储到向量数据库
        vectorStore.addBatch(chunks);
        
        log.info("Successfully indexed document {} with {} chunks", 
                document.getId(), chunks.size());
        
        return chunks.size();
    }

    /**
     * 批量索引文档
     *
     * @param documents 文档列表
     * @return 总分块数量
     */
    public int indexDocuments(List<Document> documents) {
        log.info("Starting batch indexing for {} documents", documents.size());
        
        int totalChunks = 0;
        for (Document document : documents) {
            try {
                totalChunks += indexDocument(document);
            } catch (Exception e) {
                log.error("Failed to index document {}: {}", document.getId(), e.getMessage(), e);
            }
        }
        
        log.info("Batch indexing completed. Total chunks: {}", totalChunks);
        return totalChunks;
    }

    /**
     * 检索相关文档
     *
     * @param queryText 查询文本
     * @return 检索结果
     */
    public RetrievalResult retrieve(String queryText) {
        return retrieve(queryText, config.getRetriever().getTopK());
    }

    /**
     * 检索相关文档
     *
     * @param queryText 查询文本
     * @param topK Top K结果数量
     * @return 检索结果
     */
    public RetrievalResult retrieve(String queryText, int topK) {
        Query query = Query.builder()
                .queryText(queryText)
                .topK(topK)
                .similarityThreshold(config.getRetriever().getSimilarityThreshold().floatValue())
                .build();
        
        return retriever.retrieve(query);
    }

    /**
     * 检索相关文档
     *
     * @param query 查询对象
     * @return 检索结果
     */
    public RetrievalResult retrieve(Query query) {
        RetrievalResult result = retriever.retrieve(query);
        
        // 如果启用了重排序且reranker不为null，进行重排序
        if (config.getRetriever().getEnableReranking() && reranker != null) {
            log.debug("Applying reranking with {}", reranker.getName());
            List<Chunk> rerankedChunks = reranker.rerank(query, result.getChunks());
            result.setChunks(rerankedChunks);
            result.setTotalResults(rerankedChunks.size());
            result.getMetadata().put("reranker", reranker.getName());
        }
        
        return result;
    }

    /**
     * 删除文档
     *
     * @param documentId 文档ID
     * @return 是否成功
     */
    public boolean deleteDocument(String documentId) {
        log.info("Deleting document: {}", documentId);
        // 实现需要向量存储支持按文档ID删除
        return vectorStore.delete(documentId);
    }

    /**
     * 清空所有数据
     */
    public void clear() {
        log.info("Clearing all indexed data");
        vectorStore.clear();
    }

    /**
     * 获取已索引的分块数量
     *
     * @return 数量
     */
    public int getIndexedChunkCount() {
        return vectorStore.size();
    }

    /**
     * 保存索引到磁盘
     *
     * @param path 保存路径
     */
    public void save(String path) {
        log.info("Saving index to: {}", path);
        vectorStore.save(path);
    }

    /**
     * 从磁盘加载索引
     *
     * @param path 加载路径
     */
    public void load(String path) {
        log.info("Loading index from: {}", path);
        vectorStore.load(path);
    }

    /**
     * 创建重排序器
     */
    private Reranker createReranker(RAGConfig config, ILLM llm) {
        if (!config.getRetriever().getEnableReranking() || config.getReranker() == null) {
            return null;
        }
        
        RAGConfig.RerankerConfig rerankerConfig = config.getReranker();
        String type = rerankerConfig.getType();
        
        if ("llm".equalsIgnoreCase(type)) {
            if (llm == null) {
                log.warn("LLM reranker requested but no LLM provided, skipping reranking");
                return null;
            }
            return new LLMReranker(
                    llm,
                    rerankerConfig.getPromptTemplate(),
                    rerankerConfig.getScoreThreshold()
            );
        } else if ("crossencoder".equalsIgnoreCase(type)) {
            return new CrossEncoderReranker(
                    rerankerConfig.getModelName(),
                    rerankerConfig.getScoreThreshold()
            );
        }
        
        log.warn("Unknown reranker type: {}, skipping reranking", type);
        return null;
    }

    /**
     * 创建分块器
     */
    private Chunker createChunker(RAGConfig.ChunkerConfig config) {
        return switch (config.getStrategy()) {
            case "fixed" -> new FixedSizeChunker(
                    config.getChunkSize(),
                    config.getChunkOverlap(),
                    1000  // default maxChunks
            );
            case "semantic" -> new SemanticChunker(
                    config.getChunkSize(),
                    config.getChunkSize() * 2,  // maxChunkSize = 2x targetSize
                    1000  // default maxChunks
            );
            default -> new FixedSizeChunker(
                    config.getChunkSize(),
                    config.getChunkOverlap(),
                    1000  // default maxChunks
            );
        };
    }

    /**
     * 从文件读取并索引文档
     *
     * @param filePath 文件路径
     * @return 生成的分块数量
     */
    public int indexFromFile(Path filePath) throws IOException {
        log.info("Reading and indexing file: {}", filePath);
        Document document = reader.loadFromFile(filePath);
        return indexDocument(document);
    }

    /**
     * 从目录批量读取并索引文档
     *
     * @param dirPath 目录路径
     * @param recursive 是否递归
     * @return 总分块数量
     */
    public int indexFromDirectory(Path dirPath, boolean recursive) throws IOException {
        log.info("Reading and indexing directory: {} (recursive={})", dirPath, recursive);
        List<Document> documents = reader.loadFromDirectory(dirPath, recursive);
        return indexDocuments(documents);
    }

    /**
     * 获取配置
     */
    public RAGConfig getConfig() {
        return config;
    }

    /**
     * 获取文档读取器
     */
    public DocumentReader getReader() {
        return reader;
    }
}
