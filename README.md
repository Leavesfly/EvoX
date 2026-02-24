# EvoX：自进化的多智能体框架

<div align="center">

**现代化的企业级 Java 智能代理框架**

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-3.8+-blue.svg)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

[快速开始](#-快速开始) • [核心特性](#-核心特性) • [架构设计](#-架构设计) • [示例代码](#-示例代码) • [文档](#-文档)

</div>

---

## 📖 简介

EvoX 是一个现代化的企业级智能代理（Agent）框架，基于 **Java 17**、**Spring Boot 3.2+** 和 **Project Reactor** 构建。它为开发者提供了一套完整的工具和抽象层，用于快速构建具有自主学习、协同决策和工作流自动化能力的 AI 应用系统。

EvoX 不仅是一个简单的 LLM 调用封装，而是一个完整的"智能体生态系统"，涵盖多智能体系统、工作流编排、记忆管理、RAG 检索增强、工具集成、技能系统、弹性机制、优化评估和人机协同等核心特性。

### ✨ 核心优势

- 🏢 **企业级架构**：基于 Spring 生态，分层清晰，依赖明确，适合大型项目
- 🧩 **模块化设计**：20+ 独立模块，按需引入，支持渐进式集成
- 🤖 **多模型支持**：统一接口层级（ILLMSync → ILLMAsync → ILLM），支持 OpenAI、Anthropic、DeepSeek、Gemini、Ollama、阿里云通义千问、OpenRouter、SiliconFlow 等 8 大提供商
- ⚡ **响应式编程**：基于 Project Reactor，支持异步、非阻塞调用
- 🔌 **可扩展性强**：丰富的扩展点，支持自定义 Agent、Action、Tool、Skill、优化器
- 🛡️ **弹性能力**：内置熔断器（CircuitBreaker）与重试执行器（RetryExecutor），提升系统稳定性
- 🎯 **技能系统**：支持通过 Skill 文件定义、加载和共享可复用的智能体技能，内置 SkillMarketplace
- 🚀 **生产就绪**：完善的异常处理、日志记录、配置管理

### 🎯 适用场景

- **智能对话**：客服机器人、虚拟助手、问答系统
- **知识管理**：企业知识库、文档问答、智能搜索
- **流程自动化**：业务流程编排、任务调度、审批流
- **多智能体协同**：团队协作、辩论系统、共识决策
- **代码辅助**：Claude Code 风格代码助手、代码生成与重构
- **智能优化**：提示词优化、工作流优化、超参调优
- **工具集成**：API 调用、文件操作、Shell 脚本、邮件、Git 等 20+ 工具类型

---

## 🚀 快速开始

### 📋 前置要求

- ✅ JDK 17+
- ✅ Maven 3.8+
- ✅ OpenAI API Key（或其他 LLM 提供商的 Key）

### 🔧 安装

```bash
# 克隆项目
git clone https://github.com/your-org/evox.git
cd evox

# 编译安装
mvn clean install -DskipTests
```

### ⚙️ 配置 API Key

```bash
# 方式一：环境变量（推荐）
export OPENAI_API_KEY="sk-your-actual-api-key-here"

# 方式二：配置文件（application.yml）
# 在 evox-application/evox-examples/src/main/resources/application.yml 中配置
```

### 🎉 第一个示例

创建一个简单的聊天机器人：

```java
import io.leavesfly.evox.agents.specialized.ChatBotAgent;
import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.core.message.MessageType;
import io.leavesfly.evox.models.provider.openai.OpenAILLM;
import io.leavesfly.evox.models.provider.openai.OpenAILLMConfig;

import java.util.Collections;

public class QuickStart {
    public static void main(String[] args) {
        // 1. 配置 OpenAI（也可换成 Ollama / DeepSeek / Gemini 等其他提供商）
        OpenAILLMConfig config = new OpenAILLMConfig();
        // config.setApiKey(System.getenv("OPENAI_API_KEY"));
        // config.setModel("gpt-4o-mini");

        // 2. 创建聊天机器人
        ChatBotAgent agent = new ChatBotAgent(new OpenAILLM(config));
        agent.setName("QuickBot");
        agent.initModule();

        // 3. 发送消息
        Message userMsg = Message.builder()
            .content("你好！请用一句话介绍你自己。")
            .messageType(MessageType.INPUT)
            .build();

        Message response = agent.execute("chat", Collections.singletonList(userMsg));

        // 4. 获取回复
        System.out.println("AI: " + response.getContent());
    }
}
```

### ▶️ 运行示例

```bash
# 运行快速开始示例
cd evox-application/evox-examples
mvn exec:java -Dexec.mainClass="io.leavesfly.evox.examples.QuickStart"
```

**输出**：
```
AI: 你好！我是一个基于 EvoX 框架的智能助手，可以帮助你解答问题、处理任务。
```

---

## 🌟 核心特性

### 1️⃣ 智能体系统

EvoX 提供多种类型的智能体，满足不同场景需求：

| Agent 类型 | 实现类 | 说明 | 适用场景 |
|-----------|--------|------|---------|
| **聊天机器人** | `ChatBotAgent` | 多轮对话智能体 | 对话交互、问答系统 |
| **工具感知智能体** | `ToolAwareAgent` | 具备工具调用能力的智能体 | 工具调用、API 集成、ReAct 推理 |
| **任务规划智能体** | `TaskPlannerAgent` | 自动分解任务并规划步骤 | 任务分解、计划制定 |
| **自定义智能体** | `CustomizeAgent` | 灵活扩展的基础智能体 | 特殊需求、业务定制 |

**示例：带记忆的对话**

```java
// 创建短期记忆
ShortTermMemory memory = new ShortTermMemory(100);

// 创建带记忆的聊天机器人
ChatBotAgent agent = new ChatBotAgent(llm, memory);

// 持续对话，机器人能记住之前的内容
agent.execute("chat", List.of(Message.of("我叫张三")));
agent.execute("chat", List.of(Message.of("我叫什么名字？"))); // AI 回复：你叫张三
```

**IAgent 接口核心方法**：

```java
// 同步执行
Message result = agent.execute(actionName, messages);
// 使用默认动作的简便方式
Message result = agent.execute(messages);
// 异步执行（返回 Reactor Mono）
Mono<Message> resultMono = agent.callAsync("你好");
```

### 2️⃣ 多智能体协作框架

支持多种协作模式，构建复杂的智能体系统：

- **辩论框架（DebateFramework）**：多个智能体辩论达成结论
- **共识框架（ConsensusFramework）**：通过投票、加权等策略达成共识
- **团队协作框架（TeamFramework）**：分工协作完成复杂任务
- **分层决策框架（HierarchicalFramework）**：层级化决策与执行
- **拍卖框架（AuctionFramework）**：通过竞价分配任务

**示例：辩论框架**

```java
DebateFramework framework = new DebateFramework();
framework.addAgent(agent1); // 正方
framework.addAgent(agent2); // 反方

// 辩论主题
Message result = framework.debate("人工智能是否会取代人类工作？", 3); // 3轮辩论
```

### 3️⃣ 工作流编排引擎

强大的 DAG 工作流引擎，支持复杂业务流程自动化：

```java
// 顺序工作流
Workflow workflow = WorkflowBuilder.sequential()
    .name("用户注册流程")
    .goal("完成新用户注册")
    .step("验证信息", validationAgent)
    .step("创建账户", createAccountAgent)
    .step("发送欢迎邮件", emailAgent)
    .maxSteps(100)
    .build();

// 执行工作流
WorkflowResult result = workflow.execute(initialInput);
```

**支持特性**：
- ✅ 顺序执行
- ✅ 条件分支（if-else）
- ✅ 并行执行
- ✅ 循环控制
- ✅ 错误处理与重试
- ✅ 状态持久化
- ✅ 可视化监控

### 4️⃣ RAG 检索增强生成

完整的 RAG 管道，提升 AI 回答的准确性和可靠性：

```java
// 构建 RAG 配置
RAGConfig config = RAGConfig.builder()
    .embedding(RAGConfig.EmbeddingConfig.builder()
        .dimension(1536)
        .build())
    .chunker(RAGConfig.ChunkerConfig.builder()
        .strategy("fixed")
        .chunkSize(500)
        .chunkOverlap(50)
        .build())
    .retriever(RAGConfig.RetrieverConfig.builder()
        .topK(3)
        .similarityThreshold(0.7)
        .build())
    .build();

// 创建 RAG 引擎
RAGEngine ragEngine = new RAGEngine(config, embeddingService, vectorStore);

// 索引文档
ragEngine.indexDocuments(documents);

// 检索相关文档
RetrievalResult result = ragEngine.retrieve("如何使用 EvoX 创建工作流？");
```

**功能覆盖**：
- 📄 文档加载与解析
- ✂️ 智能分块（Fixed、Semantic、Recursive）
- 🔢 向量嵌入与存储
- 🔍 相似度检索与重排序
- 📊 上下文优化与合并

### 5️⃣ 记忆管理系统

双层记忆架构，让智能体具备长期记忆能力：

```java
// 短期记忆（滑动窗口）
ShortTermMemory shortTerm = new ShortTermMemory(100);

// 长期记忆（向量检索）
InMemoryLongTermMemory longTerm = new InMemoryLongTermMemory();

// 记忆管理器（自动协调）
MemoryManager memoryManager = new MemoryManager(shortTerm, longTerm);

// 存储记忆
memoryManager.addMessage(message);

// 检索相关记忆
List<Message> relevantMemories = memoryManager.retrieve("上次讨论的话题");
```

### 6️⃣ 多模型支持（LLM Providers）

EvoX 采用层级接口设计（`ILLMSync → ILLMAsync → ILLM`），统一适配 8 大主流 LLM 提供商：

| 提供商 | 实现类 | 说明 |
|-------|--------|------|
| **OpenAI** | `OpenAILLM` | GPT-3.5、GPT-4、GPT-4o |
| **Anthropic** | `AnthropicLLM` | Claude 3 系列 |
| **DeepSeek** | `DeepSeekLLM` | DeepSeek-V2、DeepSeek-R1 |
| **Google Gemini** | `GeminiLLM` | Gemini 1.5 Pro/Flash |
| **Ollama** | `OllamaLLM` | 本地部署任意开源模型 |
| **阿里云通义千问** | `AliyunLLM` | Qwen 系列 |
| **OpenRouter** | `OpenRouterLLM` | 统一网关，支持 100+ 模型 |
| **SiliconFlow** | `SiliconFlowLLM` | 硅基流动平台 |

```java
// 切换模型提供商只需替换 LLM 实现类
ILLM llm = new DeepSeekLLM(new DeepSeekLLMConfig());
// 或
ILLM llm = new AnthropicLLM(new AnthropicLLMConfig());
// 或本地 Ollama
ILLM llm = new OllamaLLM(new OllamaLLMConfig());

// 智能体调用方式完全相同
ChatBotAgent agent = new ChatBotAgent(llm);
```

### 7️⃣ 工具系统

丰富的内置工具（20+ 类型），轻松集成外部能力：

| 工具类型 | 说明 |
|---------|------|
| **FileSystemTool** | 文件读写、目录遍历 |
| **HttpTool** | HTTP GET/POST/PUT/DELETE |
| **DatabaseTool** | SQL 查询、CRUD |
| **CalculatorTool** | 数学运算、表达式求值 |
| **SearchTool** | 网页搜索、信息检索 |
| **BrowserTool** | 浏览器自动化、网页抓取 |
| **ShellTool** | Shell 命令执行 |
| **EmailTool** | 邮件发送与接收 |
| **GitTool** | Git 仓库操作 |
| **GrepTool** | 代码/文本正则检索 |
| **ImageTool** | 图像生成与处理 |
| **CodeInterpreterTool** | 代码解释与执行 |
| **CalendarTool** | 日历与日程管理 |
| **ClipboardTool** | 系统剪贴板读写 |
| **DocumentTool** | 文档解析与生成 |
| **JsonTool** | JSON 数据处理 |
| **SystemTool** | 系统信息与进程管理 |
| **TaskTool** | 任务创建与跟踪 |
| **ProjectTool** | 项目结构分析 |
| **AgentTool** | 子智能体作为工具调用 |

**示例：工具集成**

```java
List<BaseTool> tools = List.of(
    new FileSystemTool(),
    new HttpTool(),
    new ShellTool(),
    new CalculatorTool()
);

ToolAwareAgent agent = new ToolAwareAgent(llm, tools);
agent.execute(List.of(Message.builder()
    .content("帮我读取 data.txt 并计算文件中所有数字的总和")
    .messageType(MessageType.INPUT)
    .build()));
```

### 8️⃣ 优化与评估系统

内置多种优化算法，持续提升系统性能：

- **TextGrad**：基于梯度的提示词优化
- **MIPRO**：多指标联合优化
- **AFlow**：自动化工作流优化
- **PromptOptimizer**：通用提示词优化器
- **MemoryOptimizer**：记忆管理优化

**示例：提示词优化**

```java
PromptOptimizer optimizer = new PromptOptimizer(llm, evaluator);
String optimizedPrompt = optimizer.optimize(originalPrompt, trainingData);
```

### 9️⃣ 人机协同（HITL）

灵活的人工介入机制，确保关键决策的准确性：

```java
HITLManager hitlManager = new HITLManager();

// 注册审批介入点
hitlManager.registerInterceptor("approval", (context) -> {
    // 展示待审批内容
    System.out.println("待审批: " + context.getData());
    
    // 等待人工审批
    return getUserApproval();
});

// 在工作流中触发人工介入
workflow.addHITLPoint("approval");
```

**介入模式**：
- 🔒 前置介入：执行前审批
- ✅ 后置介入：执行后审查
- ⚠️ 异常介入：错误时人工修正
- 🔄 持续介入：全程监控与指导

### 🔟 MCP 协议支持

完整支持 Model Context Protocol（MCP），实现标准化的工具集成：

```java
// MCP 服务器
MCPServer server = new MCPServer();
server.registerTool(new FileSystemTool());
server.start();

// MCP 客户端
MCPClient client = new MCPClient("http://localhost:8080");
client.callTool("readFile", Map.of("path", "data.txt"));
```

### 1️⃣1️⃣ 技能系统（Skill System）

EvoX 内置完整的技能系统，支持通过结构化文件定义可复用的智能体技能，并提供技能市场：

```java
// 加载技能定义文件
SkillLoader loader = new SkillLoader();
BaseSkill skill = loader.load("skills/code-review.yaml");

// 注册到技能注册表
SkillRegistry registry = new SkillRegistry();
registry.register(skill);

// 作为工具挂载到智能体
ToolAwareAgent agent = new ToolAwareAgent(llm, List.of(new SkillTool(registry)));
```

**核心组件**：
- `BaseSkill` - 技能基础抽象，定义技能输入/输出/提示词
- `SkillDefinitionFile` - YAML/JSON 技能定义文件格式
- `SkillLoader` - 从文件或 classpath 加载技能
- `SkillRegistry` - 技能注册与查询
- `SkillMarketplace` - 技能市场，支持技能发现与共享
- `SkillTool` - 将技能包装为标准工具，挂载给智能体

### 1️⃣2️⃣ 弹性系统（Resilience System）

内置熔断器与重试机制，保障智能体在网络抖动或 LLM 限流时的稳定运行：

```java
// 配置重试策略
RetryPolicy policy = RetryPolicy.builder()
    .maxAttempts(3)
    .initialDelay(Duration.ofSeconds(1))
    .maxDelay(Duration.ofSeconds(10))
    .build();

// 配置熔断器
CircuitBreaker breaker = new CircuitBreaker("llm-breaker");

// 通过重试执行器执行智能体调用
RetryExecutor executor = new RetryExecutor(policy, breaker);
Message result = executor.execute(() -> agent.execute(messages));
```

**弹性组件**：
- `CircuitBreaker` - 熔断器，自动检测失败率并熔断请求
- `RetryExecutor` - 带退避策略的重试执行器
- `RetryPolicy` - 重试策略配置（最大次数、延迟、退避类型）

---

## 🏗️ 架构设计

EvoX 采用清晰的分层架构，模块间依赖明确，遵循"向下依赖、禁止跨层"原则：

```
┌─────────────────────────────────────────────────┐
│          应用层 (Application Layer)              │
│  evox-examples, evox-cowork, evox-claudecode     │
│  evox-openclaw, evox-benchmark, evox-starter     │
└─────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────┐
│          扩展层 (Extensions Layer)               │
│  evox-optimizers, evox-hitl, evox-evaluation    │
│  evox-channels, evox-scheduler, evox-gateway    │
└─────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────┐
│          运行时层 (Runtime Layer)                │
│  evox-agents (含 Skill/Resilience/Frameworks)   │
│  evox-workflow, evox-rag, evox-memory           │
│  evox-tools, evox-storage, evox-mcp-runtime     │
└─────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────┐
│          核心层 (Core Layer)                     │
│  evox-core (含 Actions), evox-models, evox-mcp  │
└─────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────┐
│      基础设施层 (Infrastructure Layer)           │
│  Spring Boot 3.2+, Project Reactor, Jackson     │
└─────────────────────────────────────────────────┘
```

### 📦 模块概览

#### 核心层（Core Layer）

| 模块 | 说明 |
|------|------|
| **evox-core** | 核心抽象、消息系统、配置管理、动作系统（Actions 已内置） |
| **evox-models** | LLM 多模型适配器（8 大提供商）及接口层级（ILLMSync/ILLMAsync/ILLM） |
| **evox-mcp** | MCP 协议核心定义 |

#### 运行时层（Runtime Layer）

| 模块 | 说明 |
|------|------|
| **evox-agents** | 智能体（ChatBotAgent、ToolAwareAgent、TaskPlannerAgent）、多智能体协作框架（5 种）、技能系统、弹性系统 |
| **evox-workflow** | 工作流编排引擎（DAG、分支、循环、并行、可视化） |
| **evox-rag** | RAG 检索增强生成 |
| **evox-memory** | 记忆管理（短期/长期记忆、Session 持久化） |
| **evox-tools** | 工具集成（20+ 种工具） |
| **evox-storage** | 存储抽象（内存、向量、图、关系数据库） |
| **evox-mcp-runtime** | MCP 协议运行时（Server/Client） |

#### 扩展层（Extensions Layer）

| 模块 | 说明 |
|------|------|
| **evox-optimizers** | 优化器（TextGrad、MIPRO、AFlow） |
| **evox-hitl** | 人机协同（HITL） |
| **evox-evaluation** | 评估系统 |
| **evox-channels** | 多渠道通信 |
| **evox-scheduler** | 任务调度（Cron、触发器、心跳） |
| **evox-gateway** | 网关与安全（认证、限流、审计） |

#### 应用层（Application Layer）

| 模块 | 说明 |
|------|------|
| **evox-examples** | 示例应用（15+ 完整示例） |
| **evox-cowork** | 多智能体协作应用（含 UI、插件、权限、连接器） |
| **evox-claudecode** | Claude Code 风格代码助手（CLI、MCP 集成、权限管理） |
| **evox-openclaw** | OpenClaw 助手 |
| **evox-benchmark** | 性能基准测试 |
| **evox-spring-boot-starter** | Spring Boot 自动配置 |

---

## 💡 示例代码

EvoX 提供 15+ 完整示例，涵盖各种应用场景：

### 基础示例

- **[QuickStart.java](evox-application/evox-examples/src/main/java/io/leavesfly/evox/examples/QuickStart.java)** - 极简快速开始
- **[SimpleChatBot.java](evox-application/evox-examples/src/main/java/io/leavesfly/evox/examples/SimpleChatBot.java)** - 完整聊天机器人
- **[ComprehensiveChatBot.java](evox-application/evox-examples/src/main/java/io/leavesfly/evox/examples/ComprehensiveChatBot.java)** - 综合对话机器人（多轮、记忆、工具）
- **[MemoryAgentExample.java](evox-application/evox-examples/src/main/java/io/leavesfly/evox/examples/MemoryAgentExample.java)** - 带记忆的对话

### 多模型与构建

- **[LLMFactoryExample.java](evox-application/evox-examples/src/main/java/io/leavesfly/evox/examples/LLMFactoryExample.java)** - 多模型提供商切换（8 个提供商）
- **[MultiModelExample.java](evox-application/evox-examples/src/main/java/io/leavesfly/evox/examples/MultiModelExample.java)** - 多模型对比调用
- **[BuilderExample.java](evox-application/evox-examples/src/main/java/io/leavesfly/evox/examples/BuilderExample.java)** - AgentBuilder 构建器模式

### 工具与集成

- **[ToolsExample.java](evox-application/evox-examples/src/main/java/io/leavesfly/evox/examples/ToolsExample.java)** - 工具使用示例（20+ 工具）

### 工作流与编排

- **[SequentialWorkflowExample.java](evox-application/evox-examples/src/main/java/io/leavesfly/evox/examples/SequentialWorkflowExample.java)** - 顺序工作流
- **[WorkflowDemo.java](evox-application/evox-examples/src/main/java/io/leavesfly/evox/examples/WorkflowDemo.java)** - 复杂工作流演示

### 专业智能体

- **[SpecializedAgentsExample.java](evox-application/evox-examples/src/main/java/io/leavesfly/evox/examples/SpecializedAgentsExample.java)** - 专业智能体（TaskPlanner、ToolAware）
- **[CustomizeAgentExample.java](evox-application/evox-examples/src/main/java/io/leavesfly/evox/examples/CustomizeAgentExample.java)** - 自定义智能体

### 多智能体协作

- **[frameworks/DebateFrameworkExample.java](evox-application/evox-examples/src/main/java/io/leavesfly/evox/examples/frameworks/DebateFrameworkExample.java)** - 辩论框架
- **[frameworks/ConsensusFrameworkExample.java](evox-application/evox-examples/src/main/java/io/leavesfly/evox/examples/frameworks/ConsensusFrameworkExample.java)** - 共识框架

### RAG、记忆与技能

- **[rag/RagQuickStartExample.java](evox-application/evox-examples/src/main/java/io/leavesfly/evox/examples/rag/RagQuickStartExample.java)** - RAG 快速开始
- **[memory/MemoryBasicsExample.java](evox-application/evox-examples/src/main/java/io/leavesfly/evox/examples/memory/MemoryBasicsExample.java)** - 记忆系统基础
- **[skill/（目录）](evox-application/evox-examples/src/main/java/io/leavesfly/evox/examples/skill/)** - 技能系统示例

### 高级应用

- **[optimizer/OptimizerExample.java](evox-application/evox-examples/src/main/java/io/leavesfly/evox/examples/optimizer/OptimizerExample.java)** - 优化器示例
- **[hitl/HITLExample.java](evox-application/evox-examples/src/main/java/io/leavesfly/evox/examples/hitl/HITLExample.java)** - 人机协同
- **[BenchmarkExample.java](evox-application/evox-examples/src/main/java/io/leavesfly/evox/examples/BenchmarkExample.java)** - 性能基准测试

### 运行所有示例

```bash
cd evox-application/evox-examples

# 使用脚本运行
./run-examples.sh

# 或使用 Maven 单独运行
mvn exec:java -Dexec.mainClass="io.leavesfly.evox.examples.QuickStart"
mvn exec:java -Dexec.mainClass="io.leavesfly.evox.examples.LLMFactoryExample"
mvn exec:java -Dexec.mainClass="io.leavesfly.evox.examples.SpecializedAgentsExample"
```

---

## 📚 文档

### 快速导航

- **[快速开始指南](evox-application/evox-examples/QUICKSTART.md)** - 5 分钟快速上手
- **[架构设计文档](docs/ARCHITECTURE.md)** - 深入理解系统架构
- **[示例代码说明](evox-application/evox-examples/README.md)** - 完整示例列表
- **[配置参考](evox-application/evox-examples/src/main/resources/application.yml)** - 配置项说明

### 模块文档

- [evox-core 核心抽象](evox-core/evox-core/README.md)
- [evox-models LLM 多模型适配](evox-core/evox-models/README.md)
- [evox-agents 智能体系统](evox-runtime/evox-agents/README.md)
- [evox-workflow 工作流引擎](evox-runtime/evox-workflow/README.md)
- [evox-rag RAG 系统](evox-runtime/evox-rag/README.md)
- [evox-memory 记忆管理](evox-runtime/evox-memory/README.md)
- [evox-tools 工具系统（20+ 工具）](evox-runtime/evox-tools/src/main/java/io/leavesfly/evox/tools/README.md)
- [evox-optimizers 优化器](evox-extensions/evox-optimizers/README.md)
- [evox-hitl 人机协同](evox-extensions/evox-hitl/README.md)

---

## 🛠️ 技术栈

| 类型 | 技术 | 版本 |
|-----|------|------|
| 语言 | Java | 17+ |
| 框架 | Spring Boot | 3.2.5 |
| 响应式 | Project Reactor | - |
| JSON | Jackson | 2.15.4 |
| 构建工具 | Maven | 3.8+ |
| 工具库 | Lombok | 1.18.30 |
| 工具库 | Guava | 32.1.3-jre |
| 工具库 | MapStruct | 1.5.5.Final |
| 数据库 | H2 | 2.2.224 |
| 数据库 | HSQLDB | 2.7.2 |
| 脚本引擎 | Groovy | 4.0.18 |
| 测试 | JUnit 5 | 5.10.2 |
| 测试 | Mockito | 5.8.0 |

---

## ⚙️ 配置说明

### 基础配置（application.yml）

```yaml
evox:
  # LLM 配置
  llm:
    provider: openai  # openai, dashscope, litellm
    temperature: 0.7
    max-tokens: 2000
    timeout: 30000
    retry:
      max-attempts: 3
      initial-delay: 1000
      max-delay: 10000
  
  # Agent 配置
  agents:
    default-timeout: 60000
    max-concurrent: 10
  
  # Memory 配置
  memory:
    short-term:
      capacity: 100
      window-size: 10
    long-term:
      enabled: true
      storage-type: in-memory  # in-memory, redis, database
  
  # Workflow 配置
  workflow:
    max-depth: 10
    timeout: 300000
    enable-parallel: true
  
  # Tools 配置
  tools:
    enabled: true
    timeout: 30000
    max-retries: 3
```

### 环境变量配置

```bash
# LLM Provider
export LLM_PROVIDER=openai
export OPENAI_API_KEY=sk-your-key-here

# 阿里云通义千问
export DASHSCOPE_API_KEY=your-dashscope-key

# 数据库（可选）
export DB_URL=jdbc:postgresql://localhost:5432/evox
export DB_USERNAME=evox
export DB_PASSWORD=password

# 向量存储（可选）
export VECTOR_PROVIDER=qdrant
export VECTOR_HOST=localhost
export VECTOR_PORT=6333
```

---

## 🔍 故障排查

### Q: 编译失败？

**A:** 检查 Java 和 Maven 版本：

```bash
java -version  # 应为 17+
mvn -version   # 应为 3.8+

# 清理并重新编译
mvn clean install -U -DskipTests
```

### Q: API Key 错误？

**A:** 确认环境变量设置：

```bash
echo $OPENAI_API_KEY  # 检查是否正确设置

# 测试 API Key 有效性
curl https://api.openai.com/v1/models \
  -H "Authorization: Bearer $OPENAI_API_KEY"
```

### Q: 依赖下载慢？

**A:** 配置国内 Maven 镜像（~/.m2/settings.xml）：

```xml
<mirrors>
  <mirror>
    <id>aliyun</id>
    <mirrorOf>central</mirrorOf>
    <url>https://maven.aliyun.com/repository/public</url>
  </mirror>
</mirrors>
```

### Q: 运行时出现 OutOfMemoryError？

**A:** 增加 JVM 堆内存：

```bash
export MAVEN_OPTS="-Xmx2048m -Xms512m"
mvn exec:java -Dexec.mainClass="..."
```

---

## 🤝 贡献指南

欢迎贡献代码、报告问题或提出建议！

### 开发流程

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'Add amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 提交 Pull Request

### 代码规范

- 遵循 Java 代码规范
- 所有 public 方法必须有 Javadoc
- 单元测试覆盖率 > 30%
- 提交信息遵循 [Conventional Commits](https://www.conventionalcommits.org/)

---

## 📄 许可证

本项目采用 [Apache License 2.0](LICENSE) 许可证。

---

## 🙏 致谢

感谢以下开源项目的启发和支持：

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Spring AI](https://spring.io/projects/spring-ai)
- [Project Reactor](https://projectreactor.io/)
- [LangChain](https://github.com/langchain-ai/langchain)
- [AutoGen](https://github.com/microsoft/autogen)

---

## 📞 联系我们

- **Issue Tracker**: [GitHub Issues](https://github.com/your-org/evox/issues)
- **讨论区**: [GitHub Discussions](https://github.com/your-org/evox/discussions)

---

<div align="center">

**⭐ 如果 EvoX 对你有帮助，请给我们一个 Star！**

Made with ❤️ by EvoX Team

</div>
