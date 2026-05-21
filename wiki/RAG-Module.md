# RAG 检索增强生成 (evox-rag)

## 模块定位

**业务层模块**，提供完整的 RAG（Retrieval-Augmented Generation）能力。

**依赖关系**：
- `evox-core` - 核心基础能力
- `evox-models` - LLM 模型接口
- `evox-storage` - 向量存储支持

---

## RAG 流程图

```
文档处理流程：
┌──────────┐    ┌────────┐    ┌────────┐    ┌──────────┐    ┌────────┐
│  文档     │───▶│ 读取    │───▶│ 分块    │───▶│ 向量化    │───▶│ 存储    │
│(txt/md/  │    │Reader  │    │Chunker │    │Embedding │    │Storage │
│ pdf/docx/│    │        │    │        │    │Service   │    │        │
│ html)    │    │        │    │        │    │          │    │        │
└──────────┘    └────────┘    └────────┘    └──────────┘    └────────┘

查询响应流程：
┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
│  用户查询 │───▶│ 向量化    │───▶│ 检索     │───▶│ 生成答案  │
│          │    │Embedding │    │Retriever │    │LLM +     │
│          │    │Service   │    │          │    │Context   │
└──────────┘    └──────────┘    └──────────┘    └──────────┘
```

---

## 文档处理

### DocumentReader

文档读取器接口，负责将各种格式的文档转换为纯文本。

### UniversalDocumentReader

通用文档读取器，支持多种文件格式：

| 格式 | 说明 |
|------|------|
| `.txt` | 纯文本文件 |
| `.md` | Markdown 文件 |
| `.pdf` | PDF 文档 |
| `.docx` | Word 文档 |
| `.html` | HTML 网页 |

**使用示例**：
```java
UniversalDocumentReader reader = new UniversalDocumentReader();
String content = reader.read("document.pdf");
```

---

## 文本分块

### FixedSizeChunker

固定大小分块器，按字符数切割文本。

**配置参数**：
- `chunkSize` - 每个分块的字符数
- `overlap` - 相邻分块的重叠字符数（避免语义断裂）

**使用示例**：
```java
FixedSizeChunker chunker = new FixedSizeChunker(500, 50);
List<String> chunks = chunker.chunk(documentContent);
```

### SemanticChunker

语义分块器，根据语义边界智能分割文本，保持段落完整性。

**使用示例**：
```java
SemanticChunker chunker = new SemanticChunker();
List<String> chunks = chunker.chunk(documentContent);
```

---

## 向量化

### EmbeddingService

向量化服务接口，将文本转换为向量表示。

### OpenAIEmbeddingService

OpenAI 向量化服务实现。

**使用示例**：
```java
OpenAIEmbeddingService embeddingService = new OpenAIEmbeddingService(apiKey);
float[] vector = embeddingService.embed("Hello World");
```

---

## RAGConfig 配置详解

`RAGConfig` 是 RAG 引擎的核心配置类。

### 主要配置项

| 配置项 | 类型 | 说明 | 默认值 |
|--------|------|------|--------|
| `chunkerStrategy` | String | 分块策略：`fixed` / `semantic` | `fixed` |
| `chunkSize` | int | 固定分块大小（字符数） | 500 |
| `chunkOverlap` | int | 分块重叠字符数 | 50 |
| `topK` | int | 检索返回的最相似文档数 | 5 |
| `similarityThreshold` | double | 相似度阈值（0-1） | 0.7 |
| `embeddingDimension` | int | 向量维度 | 1536 |
| `vectorWeight` | double | 混合检索中向量权重 | 0.7 |
| `keywordWeight` | double | 混合检索中关键词权重 | 0.3 |

**配置示例**：
```java
RAGConfig config = RAGConfig.builder()
    .chunkerStrategy("fixed")
    .chunkSize(500)
    .chunkOverlap(50)
    .topK(5)
    .similarityThreshold(0.7)
    .embeddingDimension(1536)
    .build();
```

---

## RAGEngine 完整流程示例

### 创建引擎

```java
// 1. 配置 RAG
RAGConfig config = RAGConfig.builder()
    .chunkSize(500)
    .chunkOverlap(50)
    .topK(5)
    .similarityThreshold(0.7)
    .build();

// 2. 初始化组件
EmbeddingService embeddingService = new OpenAIEmbeddingService(apiKey);
VectorStore vectorStore = new InMemoryVectorStore();

// 3. 创建 RAG 引擎
RAGEngine ragEngine = new RAGEngine(config, embeddingService, vectorStore);
```

### 索引文档

```java
// 读取并索引文档
String documentPath = "docs/knowledge-base.md";
ragEngine.indexDocument(documentPath);

// 或直接索引文本内容
ragEngine.indexText("document-id", "This is the document content...");
```

### 检索相关片段

```java
// 执行检索
String query = "如何配置 RAG 引擎？";
RetrievalResult result = ragEngine.retrieve(query);

// 获取分块列表
List<TextChunk> chunks = result.getChunks();

// 获取合并后的文本
String combinedText = result.getCombinedText();
```

### 与 LLM 结合生成答案

```java
// 1. 检索相关上下文
RetrievalResult retrievalResult = ragEngine.retrieve(userQuery);
String context = retrievalResult.getCombinedText();

// 2. 构建提示词
String prompt = String.format(
    "基于以下上下文回答问题：\n\n%s\n\n问题：%s",
    context, userQuery
);

// 3. 调用 LLM 生成答案
LLMService llm = new OpenAIService(apiKey);
String answer = llm.chat(prompt);

System.out.println("答案：" + answer);
```

---

## 检索策略

### VectorRetriever

纯向量检索器，基于向量相似度进行检索。

**特点**：
- 语义匹配能力强
- 适合概念性查询
- 对关键词不敏感

**使用示例**：
```java
VectorRetriever retriever = new VectorRetriever(vectorStore, embeddingService);
RetrievalResult result = retriever.retrieve(query, topK);
```

### HybridRetriever

混合检索器，结合向量检索和关键词检索。

**配置参数**：
- `vectorWeight` - 向量检索权重（0-1）
- `keywordWeight` - 关键词检索权重（0-1）

**特点**：
- 兼顾语义匹配和关键词精确匹配
- 适合专业术语查询
- 召回率更高

**使用示例**：
```java
HybridRetriever retriever = new HybridRetriever(
    vectorStore, 
    embeddingService,
    0.7,  // vectorWeight
    0.3   // keywordWeight
);
RetrievalResult result = retriever.retrieve(query, topK);
```

---

## RetrievalResult 结果处理

`RetrievalResult` 封装了检索结果。

### 主要方法

| 方法 | 返回类型 | 说明 |
|------|----------|------|
| `getChunks()` | `List<TextChunk>` | 获取所有匹配的分块 |
| `getCombinedText()` | `String` | 获取合并后的文本（用于 LLM 上下文） |
| `getScores()` | `List<Double>` | 获取各分块的相似度分数 |
| `isEmpty()` | `boolean` | 判断是否有检索结果 |

**使用示例**：
```java
RetrievalResult result = ragEngine.retrieve(query);

if (!result.isEmpty()) {
    // 方式1：直接使用合并文本
    String context = result.getCombinedText();
    
    // 方式2：遍历各个分块
    for (TextChunk chunk : result.getChunks()) {
        System.out.println("内容: " + chunk.getText());
        System.out.println("相似度: " + chunk.getScore());
    }
}
```

---

## 目录结构

```
evox-rag/
├── src/main/java/io/leavesfly/evox/rag/
│   ├── RAGEngine.java              # RAG 引擎主类
│   ├── RAGConfig.java              # RAG 配置类
│   ├── reader/                     # 文档读取器
│   │   ├── DocumentReader.java     # 读取器接口
│   │   └── UniversalDocumentReader.java  # 通用读取器
│   ├── chunker/                    # 文本分块器
│   │   ├── Chunker.java            # 分块器接口
│   │   ├── FixedSizeChunker.java   # 固定大小分块
│   │   └── SemanticChunker.java    # 语义分块
│   ├── embedding/                  # 向量化服务
│   │   ├── EmbeddingService.java   # 向量化接口
│   │   └── OpenAIEmbeddingService.java  # OpenAI 实现
│   ├── retriever/                  # 检索器
│   │   ├── Retriever.java          # 检索器接口
│   │   ├── VectorRetriever.java    # 向量检索
│   │   └── HybridRetriever.java    # 混合检索
│   └── schema/                     # 数据模型
│       ├── TextChunk.java          # 文本分块
│       └── RetrievalResult.java    # 检索结果
└── pom.xml
```

---

## Maven 依赖

```xml
<dependency>
    <groupId>io.leavesfly.evox</groupId>
    <artifactId>evox-rag</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```
