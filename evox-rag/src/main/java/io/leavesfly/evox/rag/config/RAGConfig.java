package io.leavesfly.evox.rag.config;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * RAG配置类
 * 统一管理RAG系统的各项配置
 *
 * @author EvoX Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RAGConfig {

    /**
     * Embedding配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmbeddingConfig {
        /** Embedding模型名称 */
        @Builder.Default
        private String modelName = "text-embedding-ada-002";
        
        /** API提供商 (openai, huggingface, local) */
        @Builder.Default
        private String provider = "openai";
        
        /** API密钥 */
        private String apiKey;
        
        /** 模型维度 */
        @Builder.Default
        private Integer dimension = 1536;
        
        /** 批处理大小 */
        @Builder.Default
        private Integer batchSize = 100;
    }

    /**
     * Chunker配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChunkerConfig {
        /** 分块策略 (fixed, sentence, semantic) */
        @Builder.Default
        private String strategy = "fixed";
        
        /** 块大小 */
        @Builder.Default
        private Integer chunkSize = 512;
        
        /** 重叠大小 */
        @Builder.Default
        private Integer chunkOverlap = 50;
        
        /** 分隔符 */
        @Builder.Default
        private String separator = "\n\n";
    }

    /**
     * Retriever配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RetrieverConfig {
        /** 检索类型 (vector, keyword, hybrid) */
        @Builder.Default
        private String retrievalType = "vector";
        
        /** 返回结果数量 */
        @Builder.Default
        private Integer topK = 5;
        
        /** 相似度阈值 */
        @Builder.Default
        private Double similarityThreshold = 0.7;
        
        /** 向量存储类型 (faiss, chroma, qdrant) */
        @Builder.Default
        private String vectorStoreType = "faiss";
        
        /** 是否启用重排序 */
        @Builder.Default
        private Boolean enableReranking = false;
        
        /** 重排序模型 */
        private String rerankModel;
    }

    /**
     * Reader配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReaderConfig {
        /** 文档读取器类型 (pdf, docx, txt, html) */
        @Builder.Default
        private String readerType = "txt";
        
        /** 是否提取元数据 */
        @Builder.Default
        private Boolean extractMetadata = true;
        
        /** 是否清理文本 */
        @Builder.Default
        private Boolean cleanText = true;
    }

    /**
     * Generator配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeneratorConfig {
        /** LLM模型名称 */
        @Builder.Default
        private String modelName = "gpt-4";
        
        /** 温度参数 */
        @Builder.Default
        private Float temperature = 0.7f;
        
        /** 最大生成token数 */
        @Builder.Default
        private Integer maxTokens = 1000;
        
        /** 是否流式输出 */
        @Builder.Default
        private Boolean stream = false;
        
        /** 提示词模板 */
        private String promptTemplate;
    }

    // 主配置
    private EmbeddingConfig embedding;
    private ChunkerConfig chunker;
    private RetrieverConfig retriever;
    private ReaderConfig reader;
    private GeneratorConfig generator;

    /**
     * 创建默认配置
     */
    public static RAGConfig createDefault() {
        return RAGConfig.builder()
                .embedding(EmbeddingConfig.builder().build())
                .chunker(ChunkerConfig.builder().build())
                .retriever(RetrieverConfig.builder().build())
                .reader(ReaderConfig.builder().build())
                .generator(GeneratorConfig.builder().build())
                .build();
    }

    /**
     * 创建快速配置 (适用于原型开发)
     */
    public static RAGConfig createQuick() {
        return RAGConfig.builder()
                .embedding(EmbeddingConfig.builder()
                        .modelName("text-embedding-ada-002")
                        .dimension(1536)
                        .build())
                .chunker(ChunkerConfig.builder()
                        .chunkSize(256)
                        .chunkOverlap(20)
                        .build())
                .retriever(RetrieverConfig.builder()
                        .topK(3)
                        .similarityThreshold(0.6)
                        .build())
                .generator(GeneratorConfig.builder()
                        .modelName("gpt-3.5-turbo")
                        .temperature(0.5f)
                        .build())
                .build();
    }

    /**
     * 创建高质量配置 (适用于生产环境)
     */
    public static RAGConfig createHighQuality() {
        return RAGConfig.builder()
                .embedding(EmbeddingConfig.builder()
                        .modelName("text-embedding-3-large")
                        .dimension(3072)
                        .batchSize(50)
                        .build())
                .chunker(ChunkerConfig.builder()
                        .strategy("semantic")
                        .chunkSize(1024)
                        .chunkOverlap(100)
                        .build())
                .retriever(RetrieverConfig.builder()
                        .retrievalType("hybrid")
                        .topK(10)
                        .similarityThreshold(0.75)
                        .enableReranking(true)
                        .rerankModel("cross-encoder/ms-marco-MiniLM-L-12-v2")
                        .build())
                .generator(GeneratorConfig.builder()
                        .modelName("gpt-4")
                        .temperature(0.3f)
                        .maxTokens(2000)
                        .build())
                .build();
    }
}
