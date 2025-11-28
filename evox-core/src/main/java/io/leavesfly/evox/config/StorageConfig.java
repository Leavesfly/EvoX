package io.leavesfly.evox.config;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 存储配置类
 * 统一管理各类存储的配置
 *
 * @author EvoX Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageConfig {

    /**
     * 数据库配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DatabaseConfig {
        /** 数据库类型 (postgresql, sqlite, inmemory) */
        private String type;
        
        /** 连接URL */
        private String url;
        
        /** 用户名 */
        private String username;
        
        /** 密码 */
        private String password;
        
        /** 驱动类名 */
        private String driverClassName;
        
        /** 最大连接数 */
        @Builder.Default
        private Integer maxPoolSize = 10;
        
        /** 最小空闲连接数 */
        @Builder.Default
        private Integer minIdle = 2;
        
        /** 连接超时(毫秒) */
        @Builder.Default
        private Long connectionTimeout = 30000L;
    }

    /**
     * 向量存储配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VectorStoreConfig {
        /** 向量存储类型 (faiss, chroma, qdrant) */
        private String type;
        
        /** 维度 */
        @Builder.Default
        private Integer dimension = 1536;
        
        /** 索引类型 */
        @Builder.Default
        private String indexType = "Flat";
        
        /** 距离度量 (cosine, euclidean, dot_product) */
        @Builder.Default
        private String metric = "cosine";
        
        // Chroma 配置
        /** Chroma 服务器地址 */
        private String chromaUrl;
        
        /** 集合名称 */
        private String collectionName;
        
        // Qdrant 配置
        /** Qdrant 服务器地址 */
        private String qdrantUrl;
        
        /** Qdrant API Key */
        private String qdrantApiKey;
        
        /** Qdrant 集合名称 */
        private String qdrantCollection;
        
        // FAISS 配置
        /** FAISS 索引文件路径 */
        private String faissIndexPath;
    }

    /**
     * Redis 配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RedisConfig {
        /** Redis 主机 */
        @Builder.Default
        private String host = "localhost";
        
        /** Redis 端口 */
        @Builder.Default
        private Integer port = 6379;
        
        /** 密码 */
        private String password;
        
        /** 数据库索引 */
        @Builder.Default
        private Integer database = 0;
        
        /** 连接超时(毫秒) */
        @Builder.Default
        private Long timeout = 2000L;
        
        /** 最大连接数 */
        @Builder.Default
        private Integer maxTotal = 8;
        
        /** 最大空闲连接数 */
        @Builder.Default
        private Integer maxIdle = 8;
        
        /** 最小空闲连接数 */
        @Builder.Default
        private Integer minIdle = 0;
    }

    /**
     * 文件存储配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileStorageConfig {
        /** 存储根目录 */
        @Builder.Default
        private String rootPath = "./data";
        
        /** 是否启用压缩 */
        @Builder.Default
        private Boolean enableCompression = false;
        
        /** 最大文件大小(字节) */
        @Builder.Default
        private Long maxFileSize = 10485760L; // 10MB
        
        /** 允许的文件扩展名 */
        private String[] allowedExtensions;
    }

    // 主配置
    private DatabaseConfig database;
    private VectorStoreConfig vectorStore;
    private RedisConfig redis;
    private FileStorageConfig fileStorage;

    /**
     * 创建默认配置
     */
    public static StorageConfig createDefault() {
        return StorageConfig.builder()
                .database(DatabaseConfig.builder()
                        .type("inmemory")
                        .build())
                .vectorStore(VectorStoreConfig.builder()
                        .type("faiss")
                        .dimension(1536)
                        .build())
                .redis(RedisConfig.builder().build())
                .fileStorage(FileStorageConfig.builder().build())
                .build();
    }

    /**
     * 创建PostgreSQL配置
     */
    public static DatabaseConfig createPostgreSQLConfig(String host, int port, String database, 
                                                        String username, String password) {
        return DatabaseConfig.builder()
                .type("postgresql")
                .url(String.format("jdbc:postgresql://%s:%d/%s", host, port, database))
                .username(username)
                .password(password)
                .driverClassName("org.postgresql.Driver")
                .build();
    }

    /**
     * 创建SQLite配置
     */
    public static DatabaseConfig createSQLiteConfig(String filePath) {
        return DatabaseConfig.builder()
                .type("sqlite")
                .url("jdbc:sqlite:" + filePath)
                .driverClassName("org.sqlite.JDBC")
                .build();
    }

    /**
     * 创建FAISS向量存储配置
     */
    public static VectorStoreConfig createFAISSConfig(String indexPath, int dimension) {
        return VectorStoreConfig.builder()
                .type("faiss")
                .dimension(dimension)
                .faissIndexPath(indexPath)
                .build();
    }

    /**
     * 创建Chroma向量存储配置
     */
    public static VectorStoreConfig createChromaConfig(String url, String collectionName, int dimension) {
        return VectorStoreConfig.builder()
                .type("chroma")
                .chromaUrl(url)
                .collectionName(collectionName)
                .dimension(dimension)
                .build();
    }

    /**
     * 创建Qdrant向量存储配置
     */
    public static VectorStoreConfig createQdrantConfig(String url, String apiKey, 
                                                       String collection, int dimension) {
        return VectorStoreConfig.builder()
                .type("qdrant")
                .qdrantUrl(url)
                .qdrantApiKey(apiKey)
                .qdrantCollection(collection)
                .dimension(dimension)
                .build();
    }
}
