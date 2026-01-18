# EvoX Examples 示例应用

## 📦 模块定位

**层级**: 应用层 (Application Layer)  
**职责**: 提供完整的示例应用,展示各种使用场景  
**依赖**: 所有下层模块

## 🎯 示例列表

### 1. 基础示例

**SimpleChatBot** - 简单聊天机器人:
```java
public class SimpleChatBot {
    public static void main(String[] args) {
        BaseLLM llm = new OpenAILLM(config);
        Agent chatAgent = new ChatAgent(llm);
        
        Message input = Message.builder()
            .content("你好!")
            .build();
        Message response = chatAgent.execute("chat", List.of(input));
    }
}
```

**MemoryAgentExample** - 带记忆的对话:
```java
ShortTermMemory memory = new ShortTermMemory(100);
Agent agent = new ChatAgent(llm, memory);
```

### 2. 工具集成

**ToolsExample** - 工具使用示例:
```java
List<BaseTool> tools = List.of(
    new FileSystemTool(),
    new HttpTool(),
    new CalculatorTool()
);

Agent toolAgent = new ToolAgent(llm, tools);
```

### 2.1 RAG 检索增强

**RagQuickStartExample** - RAG 入门示例:
```java
RAGEngine rag = new RAGEngine(config, embeddingService, vectorStore);
rag.indexDocuments(documents);
RetrievalResult result = rag.retrieve("workflow");
```

### 2.2 记忆系统

**MemoryBasicsExample** - 短期/长期记忆基础用法:
```java
ShortTermMemory shortTerm = new ShortTermMemory(3);
InMemoryLongTermMemory longTerm = new InMemoryLongTermMemory();
```

### 3. 工作流编排

**WorkflowDemo** - 工作流示例:
```java
Workflow workflow = WorkflowBuilder.sequential()
    .step("步骤1", agent1)
    .step("步骤2", agent2)
    .build();
```

**SequentialWorkflowExample** - 顺序工作流:
```java
Workflow sequential = WorkflowBuilder.sequential()
    .name("数据处理流程")
    .step("提取", extractAgent)
    .step("转换", transformAgent)
    .step("加载", loadAgent)
    .build();
```

### 4. 高级示例

**ComprehensiveChatBot** - 综合聊天机器人:
- 集成记忆管理
- 工具调用
- RAG检索
- 多轮对话

**MultiModelExample** - 多模型切换:
```java
BaseLLM llm1 = new OpenAILLM(config1);
BaseLLM llm2 = new AliyunLLM(config2);
```

### 5. 特殊场景

**ActionAgentExample** - Action代理示例
**CustomizeAgentExample** - 自定义Agent
**SpecializedAgentsExample** - 专业Agent
**BuilderExample** - 构建器模式
**RetryAndCircuitBreakerExample** - 重试与熔断示例

## 🚀 运行示例

### 方式1: IDE运行

直接运行各示例类的main方法

### 方式2: 命令行

```bash
cd evox-application/evox-examples
mvn clean compile
mvn exec:java -Dexec.mainClass="io.leavesfly.evox.examples.SimpleChatBot"
```

### 方式3: 脚本运行

```bash
./run-examples.sh
```

## ⚙️ 配置

在 `application.yml` 中配置API密钥:

```yaml
evox:
  llm:
    api-key: ${OPENAI_API_KEY}
    model: gpt-4o-mini
```

或使用环境变量:

```bash
export OPENAI_API_KEY=sk-xxx
```

## 🔗 相关模块

所有示例都基于EvoX框架的各个模块构建,展示了实际应用场景。
