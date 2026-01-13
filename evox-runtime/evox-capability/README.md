# EvoX Memory 记忆管理模块

## 📦 模块定位

**层级**: 能力层 (Capability Layer)  
**职责**: 提供短期和长期记忆管理能力  
**依赖**: evox-core, evox-storage

## 🎯 核心功能

evox-memory 为 EvoX 框架提供了类人的记忆管理机制,支持短期记忆(对话历史)和长期记忆(知识存储),让智能体能够记住上下文和积累经验。

### 双层记忆架构

```
┌─────────────────────────────────────┐
│         短期记忆 (STM)              │
│    最近对话历史、临时上下文          │
│    容量有限、自动淘汰               │
└─────────────────┬───────────────────┘
                  │
                  ↓ 重要信息提取
┌─────────────────────────────────────┐
│         长期记忆 (LTM)              │
│    持久化知识、向量检索             │
│    容量无限、语义搜索               │
└─────────────────────────────────────┘
```

### 1. 短期记忆 (ShortTermMemory)

管理最近的对话历史和临时上下文:

**核心特性**:
- **容量限制**: 可设置最大消息数量
- **自动淘汰**: 超出容量自动删除最旧消息
- **快速访问**: 基于列表的高效存储
- **滑动窗口**: 支持获取最近N条消息

**使用场景**:
- 对话上下文管理
- 聊天机器人会话历史
- 临时状态跟踪

```java
// 创建短期记忆
ShortTermMemory memory = new ShortTermMemory(100); // 最多100条

// 添加消息
Message userMsg = Message.builder()
    .content("你好,请介绍一下你自己")
    .messageType(MessageType.INPUT)
    .build();
memory.addMessage(userMsg);

Message assistantMsg = Message.builder()
    .content("我是EvoX智能助手...")
    .messageType(MessageType.RESPONSE)
    .build();
memory.addMessage(assistantMsg);

// 获取所有消息
List<Message> all = memory.getMessages();

// 获取最近5条
List<Message> recent = memory.getLatestMessages(5);

// 获取最后一条
Message last = memory.getLastMessage();

// 按Agent过滤
List<Message> agentMsgs = memory.getMessagesByAgent("ChatAgent");

// 检查状态
int size = memory.size();
boolean isFull = memory.isFull();
int remaining = memory.getRemainingCapacity();

// 清空记忆
memory.clear();
```

**高级操作**:

```java
// 动态调整容量
memory.resize(200);

// 批量添加
List<Message> messages = List.of(msg1, msg2, msg3);
memory.addMessages(messages);

// 按Action过滤
List<Message> actionMsgs = memory.getMessagesByAction("chat");
```

### 2. 长期记忆 (LongTermMemory)

持久化存储重要知识和经验:

**核心特性**:
- **持久化**: 基于向量数据库存储
- **语义检索**: 支持相似度搜索
- **容量无限**: 不受短期记忆限制
- **知识积累**: 长期保存重要信息

**使用场景**:
- 用户偏好记录
- 知识库构建
- 历史经验积累
- RAG检索增强

```java
// 创建长期记忆(需要向量存储)
VectorStore vectorStore = new InMemoryVectorStore(1536);
LongTermMemory ltm = new LongTermMemory(vectorStore);

// 保存知识
ltm.save("用户偏好", "用户喜欢技术类话题");
ltm.save("历史记录", "上次讨论了Java多线程");

// 检索相关记忆
List<String> results = ltm.search("用户兴趣", 5);

// 删除记忆
ltm.delete("某个ID");

// 清空
ltm.clear();
```

### 3. 记忆管理器 (MemoryManager)

统一管理短期和长期记忆:

```java
MemoryManager manager = new MemoryManager();

// 配置短期记忆
manager.setShortTermMemory(new ShortTermMemory(100));

// 配置长期记忆
manager.setLongTermMemory(new LongTermMemory(vectorStore));

// 统一添加消息
manager.addMessage(message);

// 智能检索(结合短期和长期)
List<Message> context = manager.getContext("相关主题", 10);
```

### 4. 记忆策略

#### 自动归档策略

将重要的短期记忆归档到长期记忆:

```java
public class AutoArchiveStrategy {
    private ShortTermMemory stm;
    private LongTermMemory ltm;
    
    public void archiveImportant() {
        // 提取重要消息
        List<Message> important = stm.getMessages().stream()
            .filter(this::isImportant)
            .collect(Collectors.toList());
        
        // 归档到长期记忆
        for (Message msg : important) {
            ltm.save(msg.getContent());
        }
    }
    
    private boolean isImportant(Message msg) {
        // 判断消息重要性的逻辑
        return msg.getContent().contains("重要") ||
               msg.getMessageType() == MessageType.SYSTEM;
    }
}
```

#### 滑动窗口策略

保持固定大小的上下文窗口:

```java
public class SlidingWindowStrategy {
    private ShortTermMemory memory;
    private int windowSize = 10;
    
    public List<Message> getContextWindow() {
        return memory.getLatestMessages(windowSize);
    }
}
```

## 📂 目录结构

```
evox-memory/
├── base/                       # 基础抽象
│   └── Memory.java
├── shortterm/                  # 短期记忆
│   └── ShortTermMemory.java
├── longterm/                   # 长期记忆
│   └── LongTermMemory.java
└── manager/                    # 记忆管理
    └── MemoryManager.java
```

## 🚀 快速开始

### Maven 依赖

```xml
<dependency>
    <groupId>io.leavesfly.evox</groupId>
    <artifactId>evox-memory</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Spring Boot 自动配置

在 `application.yml` 中配置:

```yaml
evox:
  memory:
    short-term:
      capacity: 100          # 短期记忆容量
      window-size: 10        # 滑动窗口大小
    long-term:
      enabled: true          # 启用长期记忆
      storage-type: in-memory # 存储类型
```

### 基本用法

```java
// 1. 创建短期记忆
ShortTermMemory stm = new ShortTermMemory(100);

// 2. 在对话中使用
Message userInput = Message.builder()
    .content("今天天气怎么样?")
    .messageType(MessageType.INPUT)
    .build();
stm.addMessage(userInput);

// 3. 获取上下文
List<Message> context = stm.getLatestMessages(5);

// 4. 传递给LLM
String contextStr = context.stream()
    .map(Message::getContent)
    .collect(Collectors.joining("\n"));
    
String prompt = contextStr + "\n" + userInput.getContent();
String response = llm.generate(prompt);

// 5. 保存响应
Message assistantMsg = Message.builder()
    .content(response)
    .messageType(MessageType.RESPONSE)
    .build();
stm.addMessage(assistantMsg);
```

## 💡 高级用法

### 1. 会话隔离

为不同用户/会话维护独立记忆:

```java
Map<String, ShortTermMemory> sessions = new ConcurrentHashMap<>();

public ShortTermMemory getSession(String userId) {
    return sessions.computeIfAbsent(userId, 
        k -> new ShortTermMemory(100));
}

public void handleUserMessage(String userId, String message) {
    ShortTermMemory memory = getSession(userId);
    memory.addMessage(Message.builder()
        .content(message)
        .messageType(MessageType.INPUT)
        .build());
}
```

### 2. 记忆压缩

当记忆接近容量时进行压缩:

```java
public void compressMemory(ShortTermMemory memory) {
    if (memory.size() > memory.getMaxSize() * 0.8) {
        List<Message> messages = memory.getMessages();
        
        // 提取摘要
        String summary = llm.generate(
            "总结以下对话:\n" + 
            messages.stream()
                .map(Message::getContent)
                .collect(Collectors.joining("\n"))
        );
        
        // 清空并保留摘要
        memory.clear();
        memory.addMessage(Message.builder()
            .content(summary)
            .messageType(MessageType.SYSTEM)
            .build());
    }
}
```

### 3. 智能上下文选择

根据相关性选择上下文:

```java
public List<Message> getRelevantContext(
    ShortTermMemory memory, 
    String query,
    int maxSize
) {
    List<Message> all = memory.getMessages();
    
    // 计算相关性评分
    Map<Message, Double> scores = all.stream()
        .collect(Collectors.toMap(
            msg -> msg,
            msg -> calculateRelevance(msg.getContent(), query)
        ));
    
    // 按评分排序并返回Top-K
    return scores.entrySet().stream()
        .sorted(Map.Entry.<Message, Double>comparingByValue().reversed())
        .limit(maxSize)
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());
}
```

### 4. 记忆持久化

将短期记忆持久化到文件:

```java
// 保存
Path memoryFile = Paths.get("memory.json");
memory.saveModule(memoryFile);

// 加载
ShortTermMemory loaded = BaseModule.loadModule(
    memoryFile, 
    ShortTermMemory.class
);
```

## 🎓 设计原则

- **容量管理**: 短期记忆自动淘汰,避免无限增长
- **分层设计**: 短期快速访问,长期持久存储
- **灵活配置**: 支持多种存储后端
- **易于集成**: 与Agent和Workflow无缝集成

## 📊 适用场景

- **对话系统**: 维护对话上下文
- **智能客服**: 记录用户历史
- **个性化推荐**: 保存用户偏好
- **知识问答**: 结合RAG检索
- **任务跟踪**: 记录任务状态

## 🔗 相关模块

- **evox-core**: 提供Message消息模型
- **evox-storage**: 提供存储后端
- **evox-agents**: Agent使用Memory管理上下文
- **evox-workflow**: 工作流使用Memory传递状态
- **evox-rag**: RAG使用长期记忆检索知识

## ⚠️ 最佳实践

1. **合理设置容量**: 根据业务场景设置短期记忆容量
2. **定期清理**: 长时间会话需要定期清理或压缩
3. **会话隔离**: 不同用户使用独立的记忆实例
4. **重要信息归档**: 将关键信息保存到长期记忆
5. **性能优化**: 大规模场景考虑使用Redis等外部存储
