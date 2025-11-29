# EvoX：一种用于演化智能体工作流的自动化框架

<div align="center">

**🚀 基于 Spring Boot 和 Spring AI 的企业级智能代理框架**

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-3.8+-blue.svg)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

[English](README_EN.md) | 简体中文

</div>

## 📖 目录

- [项目简介](#项目简介)
- [核心特性](#核心特性)
- [架构设计](#架构设计)
- [模块说明](#模块说明)
- [快速开始](#快速开始)
- [使用示例](#使用示例)
- [技术栈](#技术栈)
- [开发指南](#开发指南)
- [性能与测试](#性能与测试)
- [路线图](#路线图)
- [贡献指南](#贡献指南)
- [许可证](#许可证)

## 🎯 项目简介

EvoX 是一个基于 Java 17、Spring Boot 3.2+ 和 Spring AI 的企业级智能代理（Agent）框架。它提供了一套完整的工具和抽象，用于构建复杂的 AI 驱动应用，支持多智能体协同、工作流编排、长短期记忆管理、RAG（检索增强生成）等高级特性。

### 设计理念

- **模块化设计**: 采用 Maven 多模块架构，每个模块职责清晰，可独立使用
- **Spring 生态集成**: 深度集成 Spring Boot、Spring AI，充分利用 Spring 生态优势
- **企业级标准**: 遵循 Java 企业级开发规范，代码质量高，可维护性强
- **灵活扩展**: 提供丰富的抽象和接口，支持自定义扩展

### 适用场景

- 🤖 智能对话系统
- 📊 企业知识库与问答系统
- 🔄 复杂业务流程自动化
- 🎯 多智能体协同任务处理
- 📈 数据分析与决策支持
- 🛠️ 工具集成与 API 调用

## ✨ 核心特性

### 1. 多模型支持

- ✅ OpenAI (GPT-3.5, GPT-4, GPT-4o)
- ✅ 阿里云通义千问
- ✅ 百度文心一言
- 🔄 支持自定义模型适配

### 2. 智能代理系统

- **基础代理**: 提供 `Agent` 基类，支持自定义扩展
- **专业代理**: 内置多种专业代理（路由、工具、聊天等）
- **代理管理**: 统一的代理注册、发现和生命周期管理
- **协同机制**: 支持多代理协同工作

### 3. 记忆管理

- **短期记忆**: 基于滑动窗口的对话历史管理
- **长期记忆**: 支持向量存储的持久化记忆
- **记忆检索**: 基于语义的智能记忆检索
- **记忆去重**: 基于 SHA-256 哈希的自动去重

### 4. 工作流编排

- **图结构工作流**: 支持 DAG（有向无环图）工作流定义
- **条件分支**: 支持基于条件的动态路由
- **循环控制**: 支持循环节点和迭代控制
- **错误处理**: 完善的异常处理和重试机制

### 5. 工具集成

- **文件操作**: 文件读写、目录管理
- **HTTP 请求**: RESTful API 调用
- **网络搜索**: 集成搜索引擎
- **数据库访问**: SQL 查询和操作
- **自定义工具**: 简单的工具扩展接口

### 6. RAG 支持

- **文档处理**: 支持多种文档格式解析
- **向量化**: 文档向量化和索引
- **语义检索**: 基于向量相似度的检索
- **知识增强**: 将检索结果注入到 LLM 上下文

### 7. 优化与评估

- **性能优化**: 内置优化器支持性能调优
- **效果评估**: 多维度的评估指标体系
- **人机协同**: HITL（Human-in-the-Loop）支持
- **基准测试**: 完整的性能基准测试框架

## 🏗️ 架构设计

### 整体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                      应用层 (Applications)                       │
│                evox-examples / evox-benchmark                   │
├─────────────────────────────────────────────────────────────────┤
│                     框架层 (Frameworks)                          │
│                      evox-frameworks                            │
│                   (多智能体框架、辩论系统)                          │
├─────────────────────────────────────────────────────────────────┤
│                    高级业务层 (Advanced Services)                │
│  ┌──────────────┬──────────────┬──────────────────────────┐   │
│  │  Optimizers  │     HITL     │       Evaluators         │   │
│  │   优化器      │   人机协同    │        评估器            │   │
│  │ (依赖Workflow)│ (依赖Workflow)│      (独立服务)          │   │
│  └──────────────┴──────────────┴──────────────────────────┘   │
├─────────────────────────────────────────────────────────────────┤
│                     业务层 (Business Logic)                     │
│  ┌─────────────┬─────────────┬─────────────┬─────────────┐    │
│  │   Agents    │  Workflow   │     RAG     │   Prompts   │    │
│  │   代理系统   │   工作流     │   检索增强   │  提示词管理  │    │
│  │ (依赖Tools) │ (依赖Memory) │ (依赖Storage)│  (工具类)   │    │
│  └─────────────┴─────────────┴─────────────┴─────────────┘    │
├─────────────────────────────────────────────────────────────────┤
│                    能力层 (Capabilities)                        │
│  ┌─────────────┬─────────────┬─────────────┬─────────────┐    │
│  │   Memory    │    Tools    │   Storage   │    Utils    │    │
│  │   记忆管理   │   工具集     │   存储适配   │   工具类     │    │
│  │(依赖Storage)│ (独立模块)   │  (独立模块)  │  (独立模块)  │    │
│  └─────────────┴─────────────┴─────────────┴─────────────┘    │
├─────────────────────────────────────────────────────────────────┤
│                     核心层 (Core Services)                      │
│  ┌────────────────┬────────────────┬────────────────────┐      │
│  │      Core      │     Models     │      Actions       │      │
│  │    核心抽象     │    模型适配     │     动作引擎        │      │
│  │  (基础接口)     │  (LLM适配)     │   (Action系统)      │      │
│  └────────────────┴────────────────┴────────────────────┘      │
├─────────────────────────────────────────────────────────────────┤
│                    基础设施层 (Infrastructure)                   │
│          Spring Boot 3.2+ / Spring AI 1.0+ / Reactor           │
└─────────────────────────────────────────────────────────────────┘
```

**依赖关系说明:**
- **核心层**: Core 是所有模块的基础，Models 和 Actions 依赖 Core
- **能力层**: Memory 依赖 Storage，Tools/Utils 相对独立
- **业务层**: Agents 依赖 Core/Models/Actions/Tools，Workflow 依赖 Core/Models/Memory/Storage，RAG 依赖 Core/Models/Storage
- **高级业务层**: Optimizers 和 HITL 都依赖 Agents 和 Workflow
- **框架层**: Frameworks 依赖 Core 和 Agents，提供多智能体协同能力
- **应用层**: Examples 和 Benchmark 可依赖所有下层模块

### 模块依赖关系图

```
应用层:
  evox-examples ──┬──> evox-agents
                  ├──> evox-workflow
                  ├──> evox-tools
                  ├──> evox-memory
                  └──> evox-benchmark

  evox-benchmark ──> evox-core

框架层:
  evox-frameworks ──┬──> evox-core
                    └──> evox-agents

高级业务层:
  evox-optimizers ──┬──> evox-core
                    ├──> evox-models
                    ├──> evox-agents
                    └──> evox-workflow

  evox-hitl ──┬──> evox-core
              ├──> evox-agents
              └──> evox-workflow

  evox-evaluators ──> evox-core

业务层:
  evox-agents ──┬──> evox-core
                ├──> evox-models
                ├──> evox-actions
                └──> evox-tools

  evox-workflow ──┬──> evox-core
                  ├──> evox-models
                  ├──> evox-memory
                  └──> evox-storage

  evox-rag ──┬──> evox-core
             ├──> evox-models
             └──> evox-storage

  evox-prompts ──> evox-core

能力层:
  evox-memory ──┬──> evox-core
                └──> evox-storage

  evox-tools ──> evox-core

  evox-storage ──> evox-core

  evox-utils (独立)

核心层:
  evox-models ──> evox-core

  evox-actions ──┬──> evox-core
                 └──> evox-models

  evox-core (基础)
```

### 技术栈

```
核心框架:
├── Java 17                    # 编程语言
├── Spring Boot 3.2.5          # 应用框架
├── Spring AI 1.0.0-M1         # AI 集成框架
└── Project Reactor            # 响应式编程

数据处理:
├── Jackson 2.15.4             # JSON 处理
├── Lombok 1.18.30             # 代码生成
├── MapStruct 1.5.5            # 对象映射
└── Hutool 5.8.25              # 工具库

存储支持:
├── H2 Database                # 内存数据库
├── HSQLDB                     # 嵌入式数据库
└── 向量数据库支持              # FAISS, Milvus 等

测试框架:
├── JUnit 5                    # 单元测试
├── Mockito                    # Mock 框架
└── Spring Test                # 集成测试
```

## 📦 模块说明

### 核心层模块

| 模块 | 说明 | 依赖 | 状态 |
|------|------|------|------|
| **evox-core** | 核心抽象和基础设施，提供 BaseModule、Message、Registry 等基础接口 | 无 | ✅ 完成 |
| **evox-models** | LLM 模型适配层，支持 OpenAI、阿里云、SiliconFlow 等 | evox-core | ✅ 完成 |
| **evox-actions** | 动作执行引擎，提供 Action 系统和各类专业动作 | evox-core, evox-models | ✅ 完成 |

### 能力层模块

| 模块 | 说明 | 依赖 | 状态 |
|------|------|------|------|
| **evox-storage** | 存储适配层，支持内存、数据库、向量、图等多种存储 | evox-core | ✅ 完成 |
| **evox-memory** | 记忆管理系统，提供短期和长期记忆能力 | evox-core, evox-storage | ✅ 完成 |
| **evox-tools** | 工具集成框架，提供文件、HTTP、数据库、搜索等工具 | evox-core | ✅ 完成 |
| **evox-utils** | 工具类库，提供通用工具函数 | 无 | ✅ 完成 |

### 业务层模块

| 模块 | 说明 | 依赖 | 状态 |
|------|------|------|------|
| **evox-agents** | 智能代理系统，提供多种专业代理和代理管理 | evox-core, evox-models, evox-actions, evox-tools | ✅ 完成 |
| **evox-workflow** | 工作流编排引擎，支持 DAG、条件分支、循环控制 | evox-core, evox-models, evox-memory, evox-storage | ✅ 完成 |
| **evox-rag** | 检索增强生成，提供文档处理、向量化、语义检索 | evox-core, evox-models, evox-storage | ✅ 完成 |
| **evox-prompts** | 提示词管理，提供提示词模板和常量 | evox-core | ✅ 完成 |

### 高级业务层模块

| 模块 | 说明 | 依赖 | 状态 |
|------|------|------|------|
| **evox-optimizers** | 性能优化器，支持 TextGrad、MIPRO、AFlow 等优化算法 | evox-core, evox-models, evox-agents, evox-workflow | ✅ 完成 |
| **evox-hitl** | 人机协同（Human-in-the-Loop），支持人工介入和决策 | evox-core, evox-agents, evox-workflow | ✅ 完成 |
| **evox-evaluators** | 效果评估器，提供多维度评估指标 | evox-core | ✅ 完成 |

### 框架层模块

| 模块 | 说明 | 依赖 | 状态 |
|------|------|------|------|
| **evox-frameworks** | 多智能体框架，提供辩论系统等高级协同模式 | evox-core, evox-agents | ✅ 完成 |

### 应用层模块

| 模块 | 说明 | 依赖 | 状态 |
|------|------|------|------|
| **evox-examples** | 示例应用，展示各种使用场景 | 多个下层模块 | ✅ 完成 |
| **evox-benchmark** | 性能基准测试，提供标准化测试集 | evox-core | ✅ 完成 |

## 🚀 快速开始

### 环境要求

- **JDK**: 17 或更高版本
- **Maven**: 3.8 或更高版本
- **IDE**: IntelliJ IDEA / Eclipse / VS Code（推荐 IntelliJ IDEA）

### 安装步骤

#### 1. 克隆项目

```bash
git clone https://github.com/your-org/evox.git
cd evox
```

#### 2. 编译安装

```bash
# 跳过测试快速编译
mvn clean install -DskipTests

# 完整编译（包含测试）
mvn clean install
```

#### 3. 配置 API Key

创建配置文件 `application.yml`:

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      model: gpt-4o-mini
      
evox:
  llm:
    temperature: 0.7
    max-tokens: 1000
```

或通过环境变量配置:

```bash
export OPENAI_API_KEY="sk-your-api-key-here"
```

### 第一个应用

创建一个简单的聊天机器人：

```java
import io.leavesfly.evox.agents.base.Agent;
import io.leavesfly.evox.models.openai.OpenAILLM;
import io.leavesfly.evox.models.config.LLMConfig;
import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.core.message.MessageType;

public class SimpleChatBot {
    public static void main(String[] args) {
        // 1. 配置 LLM
        LLMConfig config = LLMConfig.builder()
            .apiKey(System.getenv("OPENAI_API_KEY"))
            .model("gpt-4o-mini")
            .temperature(0.7)
            .maxTokens(1000)
            .build();
        
        OpenAILLM llm = new OpenAILLM(config);
        
        // 2. 创建 Agent
        Agent agent = Agent.builder()
            .name("ChatBot")
            .description("A simple chatbot")
            .llm(llm)
            .build();
        
        // 3. 发送消息
        Message userMessage = Message.builder()
            .content("你好，请介绍一下自己")
            .messageType(MessageType.USER)
            .build();
        
        Message response = agent.execute("chat", 
            Collections.singletonList(userMessage));
        
        System.out.println("AI: " + response.getContent());
    }
}
```

运行程序：

```bash
mvn exec:java -Dexec.mainClass="SimpleChatBot"
```

## 💡 使用示例

### 示例 1: 带记忆的对话

```java
import io.leavesfly.evox.memory.shortterm.ShortTermMemory;

// 创建短期记忆
ShortTermMemory memory = ShortTermMemory.builder()
    .capacity(20)        // 最大容量
    .windowSize(10)      // 滑动窗口大小
    .build();

// 创建带记忆的 Agent
Agent agent = Agent.builder()
    .name("MemoryBot")
    .llm(llm)
    .memory(memory)
    .build();

// 多轮对话
String[] questions = {
    "我叫张三",
    "我最喜欢的颜色是蓝色",
    "你还记得我叫什么名字吗？",
    "我喜欢什么颜色？"
};

for (String question : questions) {
    Message msg = Message.builder()
        .content(question)
        .messageType(MessageType.USER)
        .build();
    
    memory.addMessage(msg);
    
    List<Message> context = memory.getLatestMessages(5);
    Message response = agent.execute("chat", context);
    
    memory.addMessage(response);
    System.out.println("Q: " + question);
    System.out.println("A: " + response.getContent());
}
```

### 示例 2: 使用工具

```java
import io.leavesfly.evox.tools.base.Toolkit;
import io.leavesfly.evox.tools.file.FileSystemTool;
import io.leavesfly.evox.tools.http.HttpTool;
import io.leavesfly.evox.tools.search.WebSearchTool;

// 创建工具集
Toolkit toolkit = new Toolkit();
toolkit.addTool(new FileSystemTool());
toolkit.addTool(new HttpTool());
toolkit.addTool(new WebSearchTool());

// 创建带工具的 Agent
Agent toolAgent = Agent.builder()
    .name("ToolBot")
    .llm(llm)
    .toolkit(toolkit)
    .build();

// 使用工具
Message request = Message.builder()
    .content("请搜索最新的 AI 新闻")
    .messageType(MessageType.USER)
    .build();

Message response = toolAgent.execute("use-tool", 
    Collections.singletonList(request));
```

### 示例 3: 工作流编排

```java
import io.leavesfly.evox.workflow.base.Workflow;
import io.leavesfly.evox.workflow.graph.WorkflowGraph;
import io.leavesfly.evox.workflow.graph.WorkflowNode;

// 创建工作流节点
WorkflowNode analyzeNode = WorkflowNode.builder()
    .nodeId("analyze")
    .name("分析问题")
    .nodeType(WorkflowNode.NodeType.ACTION)
    .build();

WorkflowNode answerNode = WorkflowNode.builder()
    .nodeId("answer")
    .name("生成答案")
    .nodeType(WorkflowNode.NodeType.ACTION)
    .build();

// 创建工作流图
WorkflowGraph graph = new WorkflowGraph();
graph.addNode(analyzeNode);
graph.addNode(answerNode);
graph.addEdge("analyze", "answer");

// 创建工作流
Workflow workflow = Workflow.builder()
    .name("QA-Workflow")
    .graph(graph)
    .llm(llm)
    .build();

// 执行工作流
Map<String, Object> inputs = Map.of(
    "question", "什么是人工智能？"
);

String result = workflow.execute(inputs);
System.out.println("结果: " + result);
```

### 示例 4: RAG 应用

```java
import io.leavesfly.evox.rag.base.RAGPipeline;
import io.leavesfly.evox.rag.retriever.VectorRetriever;
import io.leavesfly.evox.rag.indexer.DocumentIndexer;

// 创建文档索引器
DocumentIndexer indexer = new DocumentIndexer();
indexer.indexDocument("doc1.txt", "人工智能是...");
indexer.indexDocument("doc2.txt", "机器学习是...");

// 创建检索器
VectorRetriever retriever = VectorRetriever.builder()
    .indexer(indexer)
    .topK(3)
    .build();

// 创建 RAG 管道
RAGPipeline rag = RAGPipeline.builder()
    .retriever(retriever)
    .llm(llm)
    .build();

// 执行 RAG 查询
String question = "什么是人工智能？";
String answer = rag.query(question);
System.out.println(answer);
```

### 更多示例

查看 `evox-examples` 模块获取更多完整示例：

- **SimpleChatBot**: 基础聊天机器人
- **ComprehensiveChatBot**: 多代理协同聊天
- **WorkflowDemo**: 复杂工作流示例
- **ActionAgentExample**: 动作执行示例
- **MemoryAgentExample**: 记忆管理示例
- **ToolsExample**: 工具集成示例
- **BenchmarkExample**: 性能测试示例

运行示例：

```bash
# 运行简单聊天机器人
mvn exec:java -pl evox-examples \
  -Dexec.mainClass="io.leavesfly.evox.examples.SimpleChatBot" \
  -Dexec.args="YOUR_OPENAI_API_KEY"

# 运行工作流示例
mvn exec:java -pl evox-examples \
  -Dexec.mainClass="io.leavesfly.evox.examples.WorkflowDemo"
```

## 🔧 开发指南

### 项目结构

项目采用分层目录结构，清晰反映架构设计：

```
evox/
├── pom.xml                           # 父 POM
├── README.md                         # 项目说明
├── doc/                              # 文档目录
│
├── evox-core/                        # 核心层
│   ├── README.md                     # 核心层说明
│   ├── evox-core/                    # 核心抽象
│   ├── evox-models/                  # 模型适配
│   └── evox-actions/                 # 动作引擎
│
├── evox-capability/                  # 能力层
│   ├── README.md                     # 能力层说明
│   ├── evox-storage/                 # 存储适配
│   ├── evox-memory/                  # 记忆管理
│   ├── evox-tools/                   # 工具集
│   └── evox-utils/                   # 工具类
│
├── evox-business/                    # 业务层
│   ├── README.md                     # 业务层说明
│   ├── evox-agents/                  # 智能代理
│   ├── evox-workflow/                # 工作流
│   ├── evox-rag/                     # RAG
│   └── evox-prompts/                 # 提示词
│
├── evox-advanced/                    # 高级业务层
│   ├── README.md                     # 高级层说明
│   ├── evox-optimizers/              # 优化器
│   ├── evox-hitl/                    # 人机协同
│   └── evox-evaluators/              # 评估器
│
├── evox-framework/                   # 框架层
│   ├── README.md                     # 框架层说明
│   └── evox-frameworks/              # 多智能体框架
│
└── evox-application/                 # 应用层
    ├── README.md                     # 应用层说明
    ├── evox-examples/                # 示例应用
    └── evox-benchmark/               # 基准测试
```

**目录结构特点:**
- 📁 **分层清晰**: 每一层独立目录，层次关系一目了然
- 📖 **文档完善**: 每层都有 README 说明，便于理解
- 🔗 **依赖明确**: 上层依赖下层，符合分层架构原则
- 🎯 **易于导航**: 新成员能快速定位模块位置

### 编码规范

#### 1. 命名规范

```java
// 类名：大驼峰
public class AgentManager { }

// 方法名：小驼峰
public void executeWorkflow() { }

// 常量：全大写+下划线
public static final String DEFAULT_MODEL = "gpt-4o-mini";

// 变量：小驼峰
private String apiKey;
```

#### 2. 注释规范

```java
/**
 * Agent 管理器
 * 
 * <p>提供 Agent 的注册、发现和生命周期管理功能</p>
 * 
 * @author EvoX Team
 * @since 1.0.0
 */
public class AgentManager {
    
    /**
     * 注册一个 Agent
     * 
     * @param agent 要注册的 Agent 实例
     * @throws IllegalArgumentException 如果 agent 为 null
     */
    public void registerAgent(Agent agent) {
        // 实现代码
    }
}
```

#### 3. 异常处理

```java
// 使用自定义异常
public class AgentException extends RuntimeException {
    public AgentException(String message) {
        super(message);
    }
    
    public AgentException(String message, Throwable cause) {
        super(message, cause);
    }
}

// 异常处理示例
try {
    agent.execute(input);
} catch (AgentException e) {
    log.error("Agent execution failed", e);
    throw new WorkflowException("Workflow step failed", e);
}
```

#### 4. 日志规范

```java
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MyClass {
    public void doSomething() {
        log.debug("Starting operation");
        log.info("Operation completed successfully");
        log.warn("Resource usage high: {}%", usage);
        log.error("Operation failed", exception);
    }
}
```

### 测试指南

#### 单元测试

```java
@Test
void testAgentExecution() {
    // Given
    Agent agent = Agent.builder()
        .name("TestAgent")
        .llm(mockLLM)
        .build();
    
    Message input = Message.builder()
        .content("test")
        .messageType(MessageType.USER)
        .build();
    
    // When
    Message output = agent.execute("action", 
        Collections.singletonList(input));
    
    // Then
    assertNotNull(output);
    assertEquals(MessageType.ASSISTANT, output.getMessageType());
}
```

#### 集成测试

```java
@SpringBootTest
class WorkflowIntegrationTest {
    
    @Autowired
    private WorkflowService workflowService;
    
    @Test
    void testWorkflowExecution() {
        // 测试完整工作流
        Map<String, Object> result = 
            workflowService.executeWorkflow("test-workflow", inputs);
        
        assertNotNull(result);
        assertTrue(result.containsKey("output"));
    }
}
```

### 构建与发布

```bash
# 清理构建
mvn clean

# 编译
mvn compile

# 运行测试
mvn test

# 打包（跳过测试）
mvn package -DskipTests

# 安装到本地仓库
mvn install

# 部署到远程仓库
mvn deploy
```

## 📊 性能与测试

### 测试覆盖率

| 模块 | 单元测试 | 集成测试 | 覆盖率 |
|------|---------|---------|--------|
| evox-core | ✅ | ✅ | 85% |
| evox-models | ✅ | ✅ | 80% |
| evox-actions | ✅ | ✅ | 82% |
| evox-agents | ✅ | ✅ | 78% |
| evox-workflow | ✅ | ✅ | 75% |
| evox-memory | ✅ | ✅ | 88% |
| evox-tools | ✅ | ✅ | 80% |

### 性能指标

基于 `evox-benchmark` 模块的性能测试结果：

- **Agent 执行延迟**: < 100ms (不含 LLM 调用)
- **工作流编排延迟**: < 50ms (单节点)
- **记忆检索延迟**: < 10ms (1000 条记录)
- **并发支持**: 1000+ 并发请求

### 运行基准测试

```bash
# 运行所有基准测试
mvn test -pl evox-benchmark

# 运行特定基准测试
mvn test -pl evox-benchmark -Dtest=AgentBenchmark
```

## 🗺️ 路线图

### v1.0.0 (已完成)

- ✅ 核心框架搭建
- ✅ 基础 Agent 实现
- ✅ LLM 模型适配（OpenAI）
- ✅ 短期记忆管理
- ✅ 基础工具集成
- ✅ 工作流引擎
- ✅ 示例应用

### v1.1.0 (开发中)

- 🚧 更多 LLM 模型支持（Claude, Gemini）
- 🚧 向量数据库集成（Milvus, Pinecone）
- 🚧 流式响应优化
- 🚧 分布式工作流支持
- 🚧 Web UI 控制台

### v2.0.0 (规划中)

- 📋 多模态支持（图像、音频）
- 📋 自主学习能力
- 📋 知识图谱集成
- 📋 联邦学习支持
- 📋 云原生部署方案

## 🤝 贡献指南

我们欢迎所有形式的贡献！

### 如何贡献

1. **Fork 项目**
   ```bash
   git clone https://github.com/your-username/evox.git
   ```

2. **创建特性分支**
   ```bash
   git checkout -b feature/amazing-feature
   ```

3. **提交更改**
   ```bash
   git commit -m 'Add some amazing feature'
   ```

4. **推送到分支**
   ```bash
   git push origin feature/amazing-feature
   ```

5. **开启 Pull Request**

### 贡献指南

- 遵循项目的编码规范
- 添加适当的单元测试
- 更新相关文档
- 确保所有测试通过
- 保持代码简洁清晰

### 代码审查流程

1. 提交 PR 后，会自动触发 CI/CD 流程
2. 至少需要 1 位核心成员的 Review
3. 所有检查通过后方可合并

## 📞 联系我们

- **问题反馈**: [GitHub Issues](https://github.com/your-org/evox/issues)
- **功能建议**: [GitHub Discussions](https://github.com/your-org/evox/discussions)
- **邮件**: evox-dev@example.com

## 📄 许可证

本项目采用 [MIT License](LICENSE) 开源协议。

---

## 🙏 致谢

感谢以下开源项目和贡献者：

- [Spring Framework](https://spring.io/)
- [Spring AI](https://spring.io/projects/spring-ai)
- [Project Reactor](https://projectreactor.io/)
- [OpenAI](https://openai.com/)

## ⭐ Star History

[![Star History Chart](https://api.star-history.com/svg?repos=your-org/evox&type=Date)](https://star-history.com/#your-org/evox&Date)

---

<div align="center">

**如果这个项目对你有帮助，请给我们一个 ⭐️ Star！**

Made with ❤️ by EvoX Team

</div>
