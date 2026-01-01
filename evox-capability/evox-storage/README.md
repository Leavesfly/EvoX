# EvoX Storage 存储适配模块

## 📦 模块定位

**层级**: 能力层 (Capability Layer)  
**职责**: 提供统一的存储抽象和多种存储后端适配  
**依赖**: evox-core

## 🎯 核心功能

evox-storage 为 EvoX 框架提供了统一的存储抽象层,支持多种存储后端,包括内存存储、数据库存储、向量存储和图存储,满足不同场景的存储需求。

### 存储类型总览

| 存储类型 | 实现类 | 适用场景 | 状态 |
|---------|--------|---------|------|
| **内存存储** | `InMemoryStore` | 临时数据、缓存 | ✅ 完成 |
| **数据库存储** | `H2Store`, `PostgreSQLStore` | 结构化数据持久化 | ✅ 完成 |
| **向量存储** | `InMemoryVectorStore` | 语义检索、RAG | ✅ 完成 |
| **图存储** | `InMemoryGraphStore` | 知识图谱、关系网络 | ✅ 完成 |

### 1. 内存存储 (InMemoryStore)

基于内存的键值存储,适合临时数据:

**核心特性**:
- 高性能读写
- 支持过期时间(TTL)
- 线程安全
- 数据不持久化

```java
InMemoryStore store = new InMemoryStore();

// 存储数据
store.put("key1", "value1");
store.put("key2", Map.of("name", "EvoX", "version", "1.0"));

// 获取数据
String value = (String) store.get("key1");
Map<String, Object> data = (Map<String, Object>) store.get("key2");

// 检查存在
boolean exists = store.exists("key1");

// 删除
store.delete("key1");

// 清空
store.clear();

// 获取所有键
Set<String> keys = store.keys();
```

**带过期时间**:

```java
// 存储10秒后过期
store.put("temp", "temporary data", Duration.ofSeconds(10));

// 过期后自动删除
Thread.sleep(11000);
assert !store.exists("temp");
```

### 2. 数据库存储 (DatabaseStore)

支持关系型数据库持久化:

**H2 内存数据库** (开发测试):

```java
H2Store h2Store = new H2Store();
h2Store.initialize();

// 使用与内存存储相同的API
h2Store.put("user:1", userData);
Object data = h2Store.get("user:1");

h2Store.close();
```

**PostgreSQL** (生产环境):

```java
PostgreSQLStore pgStore = new PostgreSQLStore(
    "jdbc:postgresql://localhost:5432/evox",
    "username",
    "password"
);
pgStore.initialize();

pgStore.put("session:123", sessionData);
pgStore.close();
```

### 3. 向量存储 (VectorStore)

支持向量相似度检索:

**核心特性**:
- 向量相似度搜索
- 支持元数据过滤
- 批量操作
- 适合RAG场景

```java
// 创建向量存储(维度1536,用于OpenAI Embeddings)
InMemoryVectorStore vectorStore = new InMemoryVectorStore(1536);
vectorStore.initialize();

// 添加向量
float[] vector1 = new float[1536];
// ... 填充向量数据
Map<String, Object> metadata = Map.of(
    "text", "EvoX是一个企业级AI框架",
    "source", "doc1.txt"
);
vectorStore.addVector("vec1", vector1, metadata);

// 批量添加
List<String> ids = List.of("vec1", "vec2", "vec3");
List<float[]> vectors = List.of(vector1, vector2, vector3);
List<Map<String, Object>> metadataList = List.of(meta1, meta2, meta3);
vectorStore.addVectors(ids, vectors, metadataList);

// 相似度搜索
float[] queryVector = ...; // 查询向量
List<SearchResult> results = vectorStore.search(queryVector, 5);

for (SearchResult result : results) {
    System.out.println("ID: " + result.getId());
    System.out.println("相似度: " + result.getScore());
    System.out.println("元数据: " + result.getMetadata());
}

// 带过滤条件的搜索
Map<String, Object> filter = Map.of("source", "doc1.txt");
List<SearchResult> filtered = vectorStore.search(queryVector, 5, filter);

// 删除向量
vectorStore.deleteVector("vec1");

// 获取向量数量
long count = vectorStore.getVectorCount();

vectorStore.close();
```

**SearchResult 结构**:

```java
public class SearchResult {
    private String id;              // 向量ID
    private float score;            // 相似度分数
    private float[] vector;         // 向量数据
    private Map<String, Object> metadata; // 元数据
}
```

### 4. 图存储 (GraphStore)

支持图结构和知识图谱:

**核心特性**:
- 节点和边管理
- 路径查询
- 子图提取
- 知识推理

```java
InMemoryGraphStore graphStore = new InMemoryGraphStore();
graphStore.initialize();

// 添加节点
Map<String, Object> nodeProps = Map.of(
    "name", "EvoX",
    "type", "Framework"
);
graphStore.addNode("node1", nodeProps);

// 添加边(关系)
Map<String, Object> edgeProps = Map.of(
    "relation", "depends_on"
);
graphStore.addEdge("edge1", "node1", "node2", edgeProps);

// 查询节点
Map<String, Object> node = graphStore.getNode("node1");

// 查询邻居节点
List<String> neighbors = graphStore.getNeighbors("node1");

// 查找路径
List<List<String>> paths = graphStore.findPaths("node1", "node3");

// 删除节点(级联删除相关边)
graphStore.deleteNode("node1");

graphStore.close();
```

### 5. 存储适配器模式

统一的存储接口,易于切换实现:

```java
public interface Store {
    void initialize();
    void close();
    void put(String key, Object value);
    Object get(String key);
    boolean exists(String key);
    void delete(String key);
    void clear();
    Set<String> keys();
}
```

**切换存储实现**:

```java
// 开发环境
Store store = new InMemoryStore();

// 生产环境
Store store = new PostgreSQLStore(config);

// 使用相同的API
store.put("key", value);
Object data = store.get("key");
```

## 📂 目录结构

```
evox-storage/
├── base/                       # 基础接口
│   ├── Store.java
│   └── VectorStore.java
├── inmemory/                   # 内存存储
│   └── InMemoryStore.java
├── db/                         # 数据库存储
│   ├── H2Store.java
│   └── PostgreSQLStore.java
├── vector/                     # 向量存储
│   ├── InMemoryVectorStore.java
│   ├── QdrantVectorStore.java  # 待实现
│   ├── MilvusVectorStore.java  # 待实现
│   └── SearchResult.java
└── graph/                      # 图存储
    ├── InMemoryGraphStore.java
    └── GraphNode.java
```

## 🚀 快速开始

### Maven 依赖

```xml
<dependency>
    <groupId>io.leavesfly.evox</groupId>
    <artifactId>evox-storage</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Spring Boot 配置

```yaml
evox:
  storage:
    type: in-memory              # 或 h2, postgresql
    vector:
      enabled: true
      provider: in-memory        # 或 qdrant, milvus
      dimension: 1536
```

### 基本用法

```java
// 1. 创建存储
InMemoryStore store = new InMemoryStore();
store.initialize();

// 2. 存储数据
store.put("user:1", Map.of(
    "name", "Alice",
    "age", 30
));

// 3. 读取数据
Map<String, Object> user = (Map<String, Object>) store.get("user:1");

// 4. 检查和删除
if (store.exists("user:1")) {
    store.delete("user:1");
}

// 5. 关闭
store.close();
```

## 💡 高级用法

### 1. 向量相似度搜索集成

与Embedding模型集成:

```java
public class VectorSearchService {
    private VectorStore vectorStore;
    private EmbeddingModel embeddingModel;
    
    public List<SearchResult> semanticSearch(String query, int topK) {
        // 1. 生成查询向量
        float[] queryVector = embeddingModel.embed(query);
        
        // 2. 向量搜索
        List<SearchResult> results = vectorStore.search(queryVector, topK);
        
        return results;
    }
    
    public void addDocument(String id, String text) {
        // 1. 生成向量
        float[] vector = embeddingModel.embed(text);
        
        // 2. 保存向量和元数据
        Map<String, Object> metadata = Map.of("text", text);
        vectorStore.addVector(id, vector, metadata);
    }
}
```

### 2. 分层缓存策略

结合内存和数据库:

```java
public class LayeredStore {
    private InMemoryStore cache;
    private PostgreSQLStore persistent;
    
    public Object get(String key) {
        // 先查缓存
        Object value = cache.get(key);
        if (value != null) {
            return value;
        }
        
        // 再查数据库
        value = persistent.get(key);
        if (value != null) {
            // 回写缓存
            cache.put(key, value, Duration.ofMinutes(10));
        }
        
        return value;
    }
    
    public void put(String key, Object value) {
        // 双写
        cache.put(key, value);
        persistent.put(key, value);
    }
}
```

### 3. 知识图谱构建

使用图存储构建知识网络:

```java
public class KnowledgeGraph {
    private GraphStore graph;
    
    public void addFact(String subject, String predicate, String object) {
        // 添加主体节点
        graph.addNode(subject, Map.of("type", "entity"));
        
        // 添加客体节点
        graph.addNode(object, Map.of("type", "entity"));
        
        // 添加关系边
        String edgeId = subject + "_" + predicate + "_" + object;
        graph.addEdge(edgeId, subject, object, 
            Map.of("relation", predicate));
    }
    
    public List<String> query(String entity, String relation) {
        // 查询某实体的特定关系
        List<String> neighbors = graph.getNeighbors(entity);
        
        return neighbors.stream()
            .filter(n -> hasRelation(entity, n, relation))
            .collect(Collectors.toList());
    }
}
```

## 🎓 设计原则

- **统一抽象**: 不同存储类型提供统一接口
- **可插拔**: 易于切换存储后端
- **高性能**: 内存存储快速响应
- **可扩展**: 易于添加新的存储类型

## 📊 适用场景

- **临时数据**: 使用内存存储
- **会话状态**: 使用内存或Redis
- **用户数据**: 使用数据库存储
- **向量检索**: 使用向量存储
- **知识图谱**: 使用图存储
- **RAG系统**: 向量存储+文档元数据

## 🔗 相关模块

- **evox-core**: 提供基础抽象
- **evox-memory**: 使用存储保存记忆
- **evox-rag**: 使用向量存储检索文档
- **evox-workflow**: 使用存储保存工作流状态

## ⚠️ 注意事项

1. **内存限制**: InMemoryStore数据仅存在于内存,重启丢失
2. **并发安全**: 多线程场景注意线程安全
3. **资源释放**: 使用完毕后调用close()释放资源
4. **向量维度**: 向量存储的维度必须与Embedding模型一致
5. **外部存储**: 生产环境建议使用Qdrant/Milvus等专业向量库
