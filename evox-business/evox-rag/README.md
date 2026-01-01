# EvoX RAG 检索增强生成模块

## 📦 模块定位

**层级**: 业务层 (Business Layer)  
**职责**: 提供完整的RAG检索增强生成能力  
**依赖**: evox-core, evox-models, evox-storage

## 🎯 核心功能

evox-rag 为 EvoX 框架提供了完整的RAG(Retrieval Augmented Generation)检索增强生成能力,包括文档加载、分块、向量化、检索和生成。

### RAG 流程

```
文档 → 读取 → 分块 → 向量化 → 存储
                              ↓
查询 → 向量化 → 检索 ← ← ← ← ←
        ↓
   生成答案
```

### 1. 文档处理

**文档读取**:
```java
DocumentReader reader = new UniversalDocumentReader();
Document doc = reader.read(Paths.get("document.txt"));
```

**支持的格式**:
- 文本文件 (.txt, .md)
- PDF文档 (.pdf)
- Word文档 (.docx)
- HTML文件 (.html)

### 2. 文本分块

**固定大小分块**:
```java
Chunker chunker = new FixedSizeChunker(500, 50); // 块大小500,重叠50
List<Chunk> chunks = chunker.chunk(document);
```

**语义分块**:
```java
Chunker semanticChunker = new SemanticChunker(500, 1000, null);
List<Chunk> chunks = semanticChunker.chunk(document);
```

### 3. 向量化

```java
EmbeddingService embedding = new OpenAIEmbeddingService(apiKey);
List<Float> vector = embedding.embed("查询文本");
```

### 4. RAG引擎

**完整流程**:
```java
// 配置
RAGConfig config = RAGConfig.builder()
    .chunker(RAGConfig.ChunkerConfig.builder()
        .strategy("SEMANTIC")
        .chunkSize(500)
        .build())
    .retriever(RAGConfig.RetrieverConfig.builder()
        .topK(5)
        .similarityThreshold(0.7)
        .build())
    .build();

// 创建引擎
RAGEngine rag = new RAGEngine(config, embeddingService, vectorStore);

// 索引文档
rag.indexDocument(document);

// 检索
RetrievalResult result = rag.retrieve("如何使用RAG?", 5);

// 获取结果
List<Chunk> chunks = result.getChunks();
String combinedText = result.getCombinedText("\n");
```

### 5. 检索策略

**向量检索**:
```java
VectorRetriever retriever = new VectorRetriever(vectorStore, embeddingService);
List<Chunk> results = retriever.retrieve(query, 5);
```

**混合检索** (向量+关键词):
```java
HybridRetriever retriever = new HybridRetriever(vectorStore, embeddingService);
retriever.setVectorWeight(0.7);
retriever.setKeywordWeight(0.3);
```

## 📂 目录结构

```
evox-rag/
├── RAGEngine.java           # RAG引擎
├── reader/                  # 文档读取
│   ├── DocumentReader.java
│   └── UniversalDocumentReader.java
├── chunker/                 # 文本分块
│   ├── Chunker.java
│   ├── FixedSizeChunker.java
│   └── SemanticChunker.java
├── embedding/               # 向量化
│   └── EmbeddingService.java
├── retriever/               # 检索器
│   ├── Retriever.java
│   └── VectorRetriever.java
└── schema/                  # 数据模型
    ├── Document.java
    ├── Chunk.java
    └── RetrievalResult.java
```

## 🚀 快速开始

### Maven 依赖

```xml
<dependency>
    <groupId>io.leavesfly.evox</groupId>
    <artifactId>evox-rag</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 基本用法

```java
// 1. 准备组件
EmbeddingService embedding = new OpenAIEmbeddingService(apiKey);
VectorStore vectorStore = new InMemoryVectorStore();

// 2. 创建RAG引擎
RAGConfig config = RAGConfig.builder().build();
RAGEngine rag = new RAGEngine(config, embedding, vectorStore);

// 3. 索引文档
Document doc = Document.builder()
    .text("EvoX是一个企业级AI框架...")
    .source("intro.txt")
    .build();
rag.indexDocument(doc);

// 4. 检索
RetrievalResult result = rag.retrieve("什么是EvoX?", 3);

// 5. 生成答案
String context = result.getCombinedText("\n");
String prompt = "根据以下信息回答:\n" + context + "\n\n问题:" + query;
String answer = llm.generate(prompt);
```

## 🔗 相关模块

- **evox-core**: 基础抽象
- **evox-models**: Embedding模型
- **evox-storage**: 向量存储
- **evox-agents**: RAG Agent集成
