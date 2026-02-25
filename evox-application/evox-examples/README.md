# EvoX Examples 示例应用

## 📦 模块定位

**层级**: 应用层 (Application Layer)  
**职责**: 提供完整的示例应用，展示 EvoX 框架的各种使用场景  
**依赖**: evox-core、evox-models、evox-actions、evox-agents、evox-workflow、evox-memory、evox-tools、evox-rag、evox-benchmark、evox-optimizers、evox-hitl

## 📂 示例结构

```
evox-examples/
├── src/main/java/io/leavesfly/evox/examples/
│   ├── QuickStart.java              # 极简快速开始
│   ├── BuilderExample.java          # Builder 模式创建 Agent
│   ├── SimpleChatBot.java           # 简单聊天机器人（含模拟模式）
│   ├── ComprehensiveChatBot.java    # 综合聊天机器人
│   ├── MemoryAgentExample.java      # 记忆智能体
│   ├── ToolsExample.java            # 工具使用示例
│   ├── SequentialWorkflowExample.java # 顺序工作流
│   ├── WorkflowDemo.java            # 工作流演示
│   ├── ActionAgentExample.java      # Action 代理示例
│   ├── CustomizeAgentExample.java   # 自定义 Agent
│   ├── SpecializedAgentsExample.java # 专用智能体
│   ├── SubagentAsToolExample.java   # 子智能体作为工具
│   ├── LLMFactoryExample.java       # LLM 工厂示例
│   ├── MultiModelExample.java       # 多模型适配
│   ├── BenchmarkExample.java        # 基准测试
│   ├── core/
│   │   └── RetryAndCircuitBreakerExample.java  # 重试与熔断
│   ├── memory/
│   │   └── MemoryBasicsExample.java # 记忆系统基础
│   ├── rag/
│   │   └── RagQuickStartExample.java # RAG 快速开始
│   ├── optimizer/
│   │   └── SimpleOptimizerExample.java # 优化器示例
│   ├── hitl/
│   │   └── EmailSendingWithApprovalExample.java # 人工审批示例
│   ├── skill/
│   │   └── SkillExample.java            # Skill 系统示例
│   └── frameworks/
│       └── MultiAgentFrameworksExample.java # 多智能体框架
├── src/main/resources/
│   ├── application.yml              # 应用配置
│   ├── application-dev.yml          # 开发环境配置
│   └── application-prod.yml         # 生产环境配置
├── run-examples.sh                  # 快速运行脚本
├── QUICKSTART.md                    # 5 分钟快速开始指南
└── README.md                        # 本文件
```

## 🎯 示例分类

### 1. 快速入门

| 示例 | 说明 | 运行命令 |
|------|------|----------|
| `QuickStart` | 最简单的入门示例，4 步创建聊天机器人 | `mvn exec:java -Dexec.mainClass="io.leavesfly.evox.examples.QuickStart"` |
| `BuilderExample` | 使用 Builder 模式，链式调用更简洁 | `mvn exec:java -Dexec.mainClass="io.leavesfly.evox.examples.BuilderExample"` |

**QuickStart 核心代码**:
```java
// 第 1 步: 配置 OpenAI
OpenAILLMConfig config = OpenAILLMConfig.builder()
    .apiKey(System.getenv("OPENAI_API_KEY"))
    .model("gpt-4o-mini")
    .build();

// 第 2 步: 创建聊天机器人
ChatBotAgent agent = new ChatBotAgent(new OpenAILLM(config));
agent.setName("QuickBot");
agent.initModule();

// 第 3 步: 发送消息
Message response = agent.execute("chat", Collections.singletonList(userMsg));

// 第 4 步: 获取回复
System.out.println("AI: " + response.getContent());
```

### 2. 聊天机器人

| 示例 | 说明 |
|------|------|
| `SimpleChatBot` | 简单聊天机器人，支持模拟模式（无需 API Key）和真实模式 |
| `ComprehensiveChatBot` | 综合聊天机器人，集成记忆管理、错误处理等功能 |

**SimpleChatBot 特点**:
- 自动检测 API Key，无 Key 时使用模拟模式
- 内置短期记忆管理（保存最近 10 条消息）
- 完整的 Action 定义和执行流程

### 3. 记忆系统

| 示例 | 说明 | 核心类 |
|------|------|--------|
| `MemoryAgentExample` | 记忆智能体集成示例 | `ShortTermMemory`, `InMemoryLongTermMemory`, `MemoryManager` |
| `MemoryBasicsExample` | 短期/长期记忆基础用法 | `ShortTermMemory`, `InMemoryLongTermMemory` |

**记忆系统核心代码**:
```java
// 短期记忆：容量为 5
ShortTermMemory stm = new ShortTermMemory(5);

// 长期记忆：内存实现（带去重）
InMemoryLongTermMemory ltm = new InMemoryLongTermMemory();

// 记忆管理器：统一管理
MemoryManager memoryManager = new MemoryManager(stm, null);
memoryManager.initModule();

// 存入记忆
memoryManager.addMessage(userMsg);

// 获取历史消息
List<Message> context = memoryManager.getLatestMessages(5);

// 长期记忆搜索
Map<String, Message> matches = ltm.search("workflow", 5);
```

### 4. 工具集成

| 示例 | 说明 |
|------|------|
| `ToolsExample` | 演示各种内置工具使用 |

**内置工具列表**:
- **CalculatorTool**: 计算器（加减乘除、幂运算、三角函数、统计）
- **HttpClientTool**: HTTP 客户端（GET/POST 请求）
- **FileSystemTool**: 文件系统（读写、目录操作）
- **SearchTool**: 搜索工具（Wikipedia、Google、DuckDuckGo）
- **BrowserTool**: 浏览器工具（导航、点击、输入、截图）

**工具使用示例**:
```java
CalculatorTool calculator = new CalculatorTool();
var result = calculator.add(10, 5);
var sqrt = calculator.sqrt(144);

FileSystemTool fileTool = new FileSystemTool();
fileTool.execute(Map.of("operation", "write", "filePath", "/tmp/test.txt", "content", "Hello"));
```

### 5. 工作流编排

| 示例 | 说明 |
|------|------|
| `SequentialWorkflowExample` | 顺序工作流，展示多步骤任务编排 |
| `WorkflowDemo` | 工作流演示，包含决策、并行、循环节点 |

**顺序工作流核心代码**:
```java
// 创建工作流节点
WorkflowNode node1 = new WorkflowNode();
node1.setNodeId("analyze");

WorkflowNode node2 = new WorkflowNode();
node2.setNodeId("answer");

// 创建工作流图
WorkflowGraph graph = new WorkflowGraph();
graph.addNode(node1);
graph.addNode(node2);
graph.addEdge("analyze", "answer");

// 执行工作流
Workflow workflow = new Workflow();
workflow.setGraph(graph);
workflow.setAgentManager(agentManager);
String result = workflow.execute(inputs);
```

### 6. RAG 检索增强

| 示例 | 说明 |
|------|------|
| `RagQuickStartExample` | RAG 入门示例，无需外部服务即可运行 |

**RAG 核心代码**:
```java
// 构建 RAG 配置
RAGConfig config = RAGConfig.builder()
    .embedding(RAGConfig.EmbeddingConfig.builder().dimension(8).build())
    .chunker(RAGConfig.ChunkerConfig.builder().strategy("fixed").chunkSize(180).build())
    .retriever(RAGConfig.RetrieverConfig.builder().topK(3).build())
    .build();

// 初始化 RAG 引擎
RAGEngine ragEngine = new RAGEngine(config, embeddingService, vectorStore);

// 索引文档
int chunks = ragEngine.indexDocuments(documents);

// 执行检索
RetrievalResult result = ragEngine.retrieve("How does EvoX handle workflows?");
```

### 7. 优化器

| 示例 | 说明 |
|------|------|
| `SimpleOptimizerExample` | 三种优化器使用演示 |

**优化器类型**:
- **TextGrad**: 基于梯度的提示词优化
- **MIPRO**: 多指令提示优化
- **AFlow**: 自动化工作流优化

**优化器使用示例**:
```java
// TextGrad 优化器
TextGradOptimizer optimizer = TextGradOptimizer.builder()
    .workflow(workflow)
    .optimizerLLM(llm)
    .optimizeMode("all")
    .batchSize(3)
    .maxSteps(5)
    .build();

OptimizationResult result = optimizer.optimize(dataset, kwargs);
optimizer.restoreBestWorkflow();
```

### 8. HITL 人机协同

| 示例 | 说明 |
|------|------|
| `EmailSendingWithApprovalExample` | 邮件发送前的人工审批流程 |

**HITL 核心代码**:
```java
// 创建 HITL 管理器
HITLManager hitlManager = new HITLManager();
hitlManager.activate();
hitlManager.setDefaultTimeout(600); // 10分钟超时

// 创建 HITL 拦截器
HITLInterceptorAgent interceptor = HITLInterceptorAgent.builder()
    .name("hitl_interceptor")
    .targetAgentName("email_agent")
    .interactionType(HITLInteractionType.APPROVE_REJECT)
    .mode(HITLMode.PRE_EXECUTION)
    .hitlManager(hitlManager)
    .build();

// 构建带审批的工作流
Workflow workflow = WorkflowBuilder.sequential()
    .step("extract_email_data", extractorAgent)
    .step("hitl_approval", interceptor)
    .step("send_email", emailAgent)
    .build();
```

### 9. 多智能体框架

| 示例 | 说明 |
|------|------|
| `MultiAgentFrameworksExample` | 五种多智能体协同框架演示 |
| `AutomaticWorkflowGenerationDemo` | 多智能体工作流自动化生成演示 |
**框架类型**:
- **Debate**: 辩论框架，多智能体通过辩论达成最优解
- **Team**: 团队协作框架，多种协作模式完成复杂任务
- **Consensus**: 共识框架，通过投票和讨论达成共识
- **Auction**: 拍卖框架，支持多种拍卖机制的资源分配
- **Hierarchical**: 分层决策框架，多层级管理与执行模式

### 10. 自动化工作流生成

| 示例 | 说明 |
|------|------|
| `AutomaticWorkflowGenerationDemo` | 多智能体工作流自动化生成演示 |

**核心功能**:
- **自然语言驱动**: 输入自然语言任务描述，自动生成工作流
- **模板驱动**: 基于预定义模板快速创建工作流
- **智能分解**: 复杂任务自动分解为可执行步骤
- **多阶段执行**: 支持多阶段任务的自动化执行

**自动化工作流核心代码**:
```java
// 1. 创建工作流生成器
WorkflowGenerator generator = new WorkflowGenerator(llm);
generator.setAvailableAgents(agentList);

// 2. 基于自然语言描述生成工作流
String taskDescription = "分析用户行为数据并生成报告";
Workflow workflow = generator.generateWorkflow(taskDescription);

// 3. 执行生成的工作流
String result = workflow.execute(inputs);

// 4. 基于模板快速创建工作流
WorkflowTemplateManager templateManager = new WorkflowTemplateManager(generator);
Workflow workflow = templateManager.createFromTemplate("data_processing", agentList);
```

**辩论框架示例**:
```java
List<MultiAgentDebate.DebateAgent> agents = List.of(
    DefaultDebateAgent.builder().name("乐观派").systemPrompt("...").llm(llm).build(),
    DefaultDebateAgent.builder().name("现实派").systemPrompt("...").llm(llm).build(),
    DefaultDebateAgent.builder().name("怀疑派").systemPrompt("...").llm(llm).build()
);

MultiAgentDebate debate = new MultiAgentDebate(agents, 3, llm);
DebateResult result = debate.debate("AI是否会取代程序员？");
```

### 10. 核心能力

| 示例 | 说明 |
|------|------|
| `RetryAndCircuitBreakerExample` | 重试与熔断机制演示 |

**重试机制**:
```java
RetryPolicy policy = RetryPolicy.builder()
    .maxAttempts(4)
    .initialDelay(Duration.ofMillis(50))
    .maxDelay(Duration.ofMillis(200))
    .backoffMultiplier(1.5)
    .build();

RetryExecutor executor = new RetryExecutor(policy);
String result = executor.execute(() -> "Success");
```

**熔断器**:
```java
CircuitBreaker breaker = new CircuitBreaker(
    "demo-breaker",
    2,                          // 失败阈值
    Duration.ofSeconds(1),      // 开启时长
    Duration.ofSeconds(1)       // 半开等待
);

String result = breaker.execute(() -> "Recovered");
```

### 11. Skill 系统

| 示例 | 说明 |
|------|------|
| `SkillExample` | 声明式 Skill 系统完整演示（对齐 Claude Code 标准） |

**Skill 系统核心概念**:
- **Skill = Prompt 模板**: 以 SKILL.md 文件定义，包含 YAML frontmatter 和 Markdown 正文
- **Meta-Tool**: SkillTool 动态生成 `<available_skills>` 列表，LLM 自主选择激活
- **上下文注入**: 激活 Skill 后注入专家指令到对话上下文（用户不可见）
- **工具预批准**: Skill 的 `allowed-tools` 自动预批准，无需用户确认

**Skill 使用示例**:
```java
// 1. 加载内置 Skill
SkillRegistry registry = new SkillRegistry();
registry.loadBuiltinSkills();

// 2. 激活 Skill（返回上下文注入指令）
SkillActivationResult activation = registry.activateSkill("code_review");
String expertPrompt = activation.getSkillPrompt();      // 注入到 LLM 上下文
List<String> tools = activation.getAllowedTools();        // 预批准的工具

// 3. 创建自定义 SKILL.md
// .claude/skills/my_skill/SKILL.md:
// ---
// name: my_skill
// description: My custom skill
// when_to_use: When the user asks for ...
// allowed-tools: [shell, file_system]
// ---
// You are an expert at ...
```

### 12. 其他示例

| 示例 | 说明 |
|------|------|
| `ActionAgentExample` | Action 代理执行函数（无需 LLM） |
| `CustomizeAgentExample` | 自定义 Agent 创建和使用 |
| `SpecializedAgentsExample` | 专用智能体示例 |
| `SubagentAsToolExample` | 子智能体作为工具使用 |
| `LLMFactoryExample` | LLM 工厂和配置管理 |
| `MultiModelExample` | 多模型适配器（OpenAI、通义千问、LiteLLM） |
| `BenchmarkExample` | 基准测试（GSM8K、MBPP） |

## 🚀 运行示例

### 方式 1: 交互式脚本

```bash
cd evox-application/evox-examples
./run-examples.sh
```

菜单选项:
1. 优化器示例 (SimpleOptimizerExample)
2. HITL 审批示例 (EmailSendingWithApprovalExample)
3. 编译所有模块
4. 运行所有测试

### 方式 2: Maven 命令

```bash
cd evox-application/evox-examples

# 编译
mvn clean compile

# 运行指定示例
mvn exec:java -Dexec.mainClass="io.leavesfly.evox.examples.QuickStart"
```

### 方式 3: IDE 运行

直接在 IDE 中运行各示例类的 `main` 方法。

## ⚙️ 配置说明

### 环境变量

```bash
# 必需：OpenAI API Key
export OPENAI_API_KEY="sk-your-key-here"

# 可选：OpenAI Base URL（用于代理）
export OPENAI_BASE_URL="https://api.openai.com"

# 可选：阿里云通义千问
export DASHSCOPE_API_KEY="your-dashscope-key"
```

### 配置文件

`application.yml` 核心配置:

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY:}
      chat:
        options:
          model: ${OPENAI_MODEL:gpt-4o-mini}
          temperature: 0.7

evox:
  llm:
    provider: ${LLM_PROVIDER:openai}
    temperature: ${LLM_TEMPERATURE:0.7}
    max-tokens: ${LLM_MAX_TOKENS:2000}
    retry:
      max-attempts: 3
  
  memory:
    short-term:
      capacity: 100
    long-term:
      enabled: true
      storage-type: in-memory
  
  workflow:
    max-depth: 10
    enable-parallel: true
```

## 📚 相关文章

- [快速开始指南](QUICKSTART.md) - 5 分钟上手教程
- [架构设计](../../docs/ARCHITECTURE.md) - 框架整体架构
- [示例总结](../../docs/EXAMPLES_SUMMARY.md) - 更多示例说明

## 💡 使用建议

1. **新手入门**: 从 `QuickStart` 开始，然后尝试 `SimpleChatBot`
2. **无需 API Key**: `SimpleChatBot` 和 `RagQuickStartExample` 支持模拟模式
3. **深入学习**: 查看 `MultiAgentFrameworksExample` 了解高级用法
4. **生产参考**: `ComprehensiveChatBot` 展示了完整的错误处理和配置管理