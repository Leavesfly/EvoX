# EvoX：一种用于演化智能体工作流的自动化框架

<div align="center">

**🚀 基于 Spring Boot 和 Spring AI 的企业级智能代理框架**

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-1.0.0--M1-blue.svg)](https://spring.io/projects/spring-ai)
[![Maven](https://img.shields.io/badge/Maven-3.8+-blue.svg)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

[English](README_EN.md) | 简体中文

**一个强大的多智能体协同框架，用于构建可演化、可优化的 AI 工作流系统**

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

EvoX 是一个现代化的企业级智能代理（Agent）框架，基于 **Java 17**、**Spring Boot 3.2+** 和 **Spring AI 1.0+** 构建。它为开发者提供了一套完整的工具和抽象层，用于快速构建具有自主学习、协同决策和工作流自动化能力的 AI 应用系统。

EvoX 不仅仅是一个简单的 LLM 调用封装，而是一个完整的 **智能体生态系统**，包含：
- 🧠 **多智能体系统**：支持多个 Agent 协同工作，通过辩论、共识、分层等多种模式解决复杂问题
- 🔄 **工作流编排引擎**：提供 DAG 工作流、条件分支、循环控制等高级编排能力
- 💾 **记忆管理系统**：短期记忆（对话历史）+ 长期记忆（向量存储）双重记忆机制
- 🔍 **RAG 检索增强**：完整的文档处理、向量化、语义检索管道
- 🛠️ **丰富的工具集**：文件操作、HTTP 请求、数据库访问、网络搜索等开箱即用的工具
- 📊 **优化与评估**：内置 TextGrad、MIPRO、AFlow 等优化器，支持提示词和工作流优化
- 🤝 **人机协同（HITL）**：灵活的人工介入机制，适配审批、监督等业务场景

### 核心优势

| 特性 | 说明 |
|------|------|
| **企业级架构** | 基于 Spring 生态，分层清晰，依赖明确，适合大型项目 |
| **模块化设计** | 16+ 独立模块，按需引入，支持渐进式集成 |
| **多模型支持** | 统一抽象层，支持 OpenAI、阿里云通义、百度文心等多种 LLM |
| **响应式编程** | 基于 Project Reactor，支持异步、非阻塞调用 |
| **可扩展性强** | 丰富的扩展点，支持自定义 Agent、Action、Tool、优化器 |
| **生产就绪** | 完善的异常处理、日志记录、配置管理 |

### 适用场景

EvoX 适用于以下场景的 AI 应用开发：

| 场景类型 | 典型应用 | 推荐模块 |
|---------|---------|---------|
| **智能对话** | 客服机器人、虚拟助手、问答系统 | `evox-agents`, `evox-memory` |
| **知识管理** | 企业知识库、文档问答、智能搜索 | `evox-rag`, `evox-storage` |
| **流程自动化** | 业务流程编排、任务调度、审批流 | `evox-workflow`, `evox-hitl` |
| **多智能体协同** | 团队协作、辩论系统、共识决策 | `evox-frameworks` |
| **智能优化** | 提示词优化、工作流优化、超参调优 | `evox-optimizers` |
| **工具集成** | API 调用、数据处理、文件操作 | `evox-tools` |

## ✨ 核心特性

### 🧠 1. 智能代理系统

EvoX 提供了完整的智能代理生态，支持从单代理到多代理协同的各种场景：

**单智能体类型**：
- **ActionAgent**：函数执行型代理，适合确定性任务
- **ReActAgent**：推理-行动代理，支持复杂推理和多步骤任务
- **CustomizeAgent**：自定义代理，灵活适配特殊需求
- **TaskPlannerAgent**：任务规划代理，自动分解和规划任务
- **RouterAgent**：路由代理，智能分发请求到合适的处理器
- **ToolAgent**：工具代理，集成外部工具和 API
- **ChatAgent**：对话代理，处理自然语言交互

**多智能体协同框架**：
- **辩论框架（Debate）**：多个 Agent 通过辩论达成最优解
- **共识框架（Consensus）**：支持投票、加权等多种共识策略
- **分层决策框架（Hierarchical）**：层级化的决策和执行机制
- **拍卖框架（Auction）**：资源分配和任务竞标
- **团队协作框架（Team）**：协作、竞争等多种团队模式

### 🔄 2. 工作流编排引擎

强大的工作流编排能力，支持复杂业务流程自动化：

- ✅ **DAG 工作流**：有向无环图结构，清晰的依赖关系
- ✅ **条件分支**：基于运行时条件的动态路由
- ✅ **循环控制**：支持循环节点和迭代处理
- ✅ **并行执行**：多节点并行执行，提升效率
- ✅ **错误处理**：完善的异常捕获、重试、降级机制
- ✅ **状态管理**：工作流状态持久化和恢复
- ✅ **可视化**：工作流图可视化和监控

### 💾 3. 记忆管理系统

双层记忆架构，支持短期和长期记忆：

**短期记忆（ShortTermMemory）**：
- 基于滑动窗口的对话历史管理
- 容量限制和自动淘汰机制
- 支持按动作、工作流目标索引
- 高效的消息检索和过滤

**长期记忆（LongTermMemory）**：
- 基于向量数据库的持久化存储
- 语义相似度检索
- 自动去重（SHA-256 哈希）
- 支持关键词搜索和统计

### 🔍 4. RAG 检索增强生成

完整的 RAG 管道，从文档到知识库一站式解决：

```
文档加载 → 文本分块 → 向量嵌入 → 语义检索 → 上下文增强 → LLM 生成
```

- **文档处理器**：支持 PDF、TXT、Markdown、HTML 等多种格式
- **智能分块**：基于语义的文本分块策略
- **向量存储**：支持内存、FAISS、Milvus、Qdrant 等
- **混合检索**：向量检索 + 关键词检索 + 重排序
- **上下文优化**：智能选择和组织检索结果

### 🛠️ 5. 丰富的工具集

开箱即用的工具集，快速集成外部能力：

| 工具类型 | 功能说明 | 典型用途 |
|---------|---------|---------|
| **FileSystemTool** | 文件读写、目录管理 | 文件处理、日志分析 |
| **HttpTool** | HTTP/HTTPS 请求 | API 调用、数据获取 |
| **DatabaseTool** | SQL 查询和操作 | 数据查询、报表生成 |
| **WebSearchTool** | 网络搜索 | 信息检索、事实核查 |
| **CalculatorTool** | 数学计算 | 数值计算、公式求解 |
| **EmailTool** | 邮件发送 | 通知、报告发送 |
| **CodeExecutorTool** | 代码执行 | 动态代码生成和执行 |

**自定义工具**：实现 `BaseTool` 接口，5 分钟完成自定义工具开发。

### 📊 6. 优化与评估系统

内置多种优化器，自动优化提示词和工作流：

**优化器类型**：
- **TextGrad**：基于梯度的提示词优化，支持系统提示、指令等多种优化模式
- **MIPRO**：迭代式提示优化，支持轻量、中等、重度三种自动化级别
- **AFlow**：工作流结构优化，基于 MCTS 搜索最优工作流结构
- **SEW**：Self-Evolving Workflow，工作流自演化优化
- **EvoPrompt**：基于进化算法的提示词优化

**评估能力**：
- 自定义评估函数
- 批量评估和统计
- 优化历史追踪
- A/B 测试支持

### 🤝 7. 人机协同（HITL）

灵活的人工介入机制，适配各种业务审批场景：

**介入模式**：
- **前置审批**：执行前人工确认
- **后置审核**：执行后人工验证
- **异常介入**：出错时人工接管
- **持续监督**：全程人工监控

**介入类型**：
- 参数审批
- 结果验证
- 异常处理
- 决策确认

**使用场景**：
- 财务审批流程
- 邮件发送确认
- 数据修改审核
- 重要决策验证

### 🌐 8. 多模型支持

统一的 LLM 抽象层，轻松切换不同模型：

| 模型提供商 | 支持模型 | 状态 |
|-----------|---------|------|
| **OpenAI** | GPT-3.5, GPT-4, GPT-4o | ✅ 完整支持 |
| **阿里云** | 通义千问系列 | ✅ 完整支持 |
| **百度** | 文心一言系列 | ✅ 完整支持 |
| **自定义** | 符合 OpenAI API 规范的模型 | ✅ 支持 |
| **本地模型** | Ollama 等本地部署模型 | 🔄 规划中 |

**模型切换**：只需修改配置，无需改动业务代码。

## 🏗️ 架构设计

### 整体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                      应用层 (Application Layer)                 │
│         evox-examples / evox-benchmark / evox-starter          │
├─────────────────────────────────────────────────────────────────┤
│                    扩展层 (Extensions Layer)                    │
│  ┌──────────────┬──────────────┬──────────────────────────┐   │
│  │     RAG      │  Optimizers  │         HITL             │   │
│  │  检索增强生成  │   优化器      │      人机协同             │   │
│  └──────────────┴──────────────┴──────────────────────────┘   │
├─────────────────────────────────────────────────────────────────┤
│                   运行时层 (Runtime Layer)                      │
│  ┌─────────────┬─────────────┬─────────────────────────┐      │
│  │   Agents    │  Workflow   │     Capability          │      │
│  │   智能代理   │  工作流编排  │  记忆/存储/工具集        │      │
│  └─────────────┴─────────────┴─────────────────────────┘      │
├─────────────────────────────────────────────────────────────────┤
│                     核心层 (Core Layer)                        │
│  ┌────────────────┬────────────────┬────────────────────┐      │
│  │      Core      │     Models     │      Actions       │      │
│  │    核心抽象     │    模型适配     │     动作引擎        │      │
│  │  (基础接口)     │  (LLM适配)     │   (Action系统)      │      │
│  └────────────────┴────────────────┴────────────────────┘      │
├─────────────────────────────────────────────────────────────────┤
│                    基础设施层 (Infrastructure)                   │
│          Spring Boot 3.2+ / Spring AI 1.0+ / Reactor           │
└─────────────────────────────────────────────────────────────────┘







┌─────────────────────────────────────────────────────────────────┐
│                      应用层 (Applications)                       │
│          evox-examples / evox-benchmark / spring-starter        │
├─────────────────────────────────────────────────────────────────┤
│                      评估层 (Evaluation)                        │
│  ┌─────────────────────────┬────────────────────────┐          │
│  │  Task-Specific Evaluator │  LLM-Based Evaluator  │          │
│  └─────────────────────────┴────────────────────────┘          │
├─────────────────────────────────────────────────────────────────┤
│                      进化层 (Evolving)                          │
│  ┌──────────────┬──────────────────┬────────────────┐          │
│  │Agent Optimizer│Workflow Optimizer│Memory Optimizer│          │
│  └──────────────┴──────────────────┴────────────────┘          │
├─────────────────────────────────────────────────────────────────┤
│                     工作流层 (WorkFlow)                         │
│  ┌──────────┬──────────────┬───────────┬─────────────────┐     │
│  │ WFGraph  │ AgentManager │Environment│WorkflowGeneration│     │
│  ├──────────┼──────────────┼───────────┼─────────────────┤     │
│  │WFManager │StorageHandler│    RAG    │      HITL       │     │
│  └──────────┴──────────────┴───────────┴─────────────────┘     │
├─────────────────────────────────────────────────────────────────┤
│                      代理层 (Agent)                             │
│  ┌────────────┬─────────────┬──────────────┬─────────────┐     │
│  │    LLM     │   Memory    │ KnowledgeBase│    HITL     │     │
│  └────────────┴─────────────┴──────────────┴─────────────┘     │
├─────────────────────────────────────────────────────────────────┤
│                   基础组件层 (Basic Components)                  │
│  ┌────────────┬─────────────┬──────────────┬─────────────┐     │
│  │  Actions   │   Prompt    │    Tools     │     MCP     │     │
│  └────────────┴─────────────┴──────────────┴─────────────┘     │
├─────────────────────────────────────────────────────────────────┤
│                      基础层 (Basics)                            │
│  ┌──────────┬─────────┬─────────┬─────────┬──────────────┐     │
│  │BaseModule│ Logging │  Config │  Parser │  Storage     │     │
│  └──────────┴─────────┴─────────┴─────────┴──────────────┘     │
└─────────────────────────────────────────────────────────────────┘


```

**依赖关系说明:**
- **核心层**: Core 是所有模块的基础，Models 和 Actions 依赖 Core
- **运行时层**: Capability 统一提供记忆、存储、工具能力；Agents 依赖 Core/Models/Actions/Capability；Workflow 依赖 Core/Models/Agents/Capability
- **扩展层**: RAG 依赖 Core/Models/Capability；Optimizers 依赖 Core/Models/Agents/Workflow；HITL 依赖 Core/Agents
- **应用层**: Examples 和 Benchmark 可依赖所有下层模块；Starter 提供 Spring Boot 自动配置

### 模块依赖关系图

```
应用层:
  evox-examples ──┬──> evox-agents
                  ├──> evox-workflow
                  ├──> evox-capability
                  └──> evox-rag

  evox-benchmark ──┬──> evox-core
                   ├──> evox-models
                   ├──> evox-agents
                   └──> evox-capability

  evox-spring-boot-starter ──┬──> evox-core
                             ├──> evox-models
                             ├──> evox-agents
                             └──> evox-capability

扩展层:
  evox-rag ──┬──> evox-core
             ├──> evox-models
             └──> evox-capability

  evox-optimizers ──┬──> evox-core
                    ├──> evox-models
                    ├──> evox-agents
                    └──> evox-workflow

  evox-hitl ──┬──> evox-core
              └──> evox-agents

运行时层:
  evox-agents ──┬──> evox-core
                ├──> evox-models
                ├──> evox-actions
                └──> evox-capability

  evox-workflow ──┬──> evox-core
                  ├──> evox-models
                  ├──> evox-agents
                  └──> evox-capability

  evox-capability ──> evox-core

核心层:
  evox-models ──> evox-core

  evox-actions ──> evox-core

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

## 📦 模块一览

### 核心层模块 (Core Layer)

| 模块 | 功能描述 | 主要类/接口 | 状态 |
|------|---------|-----------|------|
| **evox-core** | 基础抽象、消息模型、模块注册 | `BaseModule`, `Message`, `Registry` | ✅ |
| **evox-models** | LLM 模型适配层 | `OpenAILLM`, `QianWenLLM`, `SiliconFlowLLM` | ✅ |
| **evox-actions** | Action 动作执行引擎 | `Action`, `TaskPlanningAction`, `ReflectionAction` | ✅ |

### 运行时层模块 (Runtime Layer)

| 模块 | 功能描述 | 主要类/接口 | 状态 |
|------|---------|-----------|------|
| **evox-capability** | 记忆、存储、工具集成 | `ShortTermMemory`, `LongTermMemory`, `VectorStore`, `FileSystemTool` | ✅ |
| **evox-agents** | 多种 Agent 实现和管理 | `ActionAgent`, `ReActAgent`, `ChatBotAgent` | ✅ |
| **evox-workflow** | DAG 工作流编排引擎 | `Workflow`, `WorkflowGraph`, `WorkflowNode` | ✅ |

### 扩展层模块 (Extensions Layer)

| 模块 | 功能描述 | 主要类/接口 | 状态 |
|------|---------|-----------|------|
| **evox-rag** | RAG 检索增强生成 | `RAGEngine`, `DocumentLoader`, `VectorRetriever` | ✅ |
| **evox-optimizers** | 提示词和工作流优化 | `TextGrad`, `MIPRO`, `AFlow` | ✅ |
| **evox-hitl** | 人机协同机制 | `HITLManager`, `HITLInterceptorAgent` | ✅ |

### 应用层模块 (Application Layer)

| 模块 | 功能描述 | 示例/测试 | 状态 |
|------|---------|---------|------|
| **evox-examples** | 完整示例应用 | `SimpleChatBot`, `ComprehensiveChatBot`, `WorkflowDemo` | ✅ |
| **evox-benchmark** | 性能基准测试 | `GSM8K`, `HotpotQA`, `HumanEval` | ✅ |
| **evox-spring-boot-starter** | Spring Boot 自动配置 | `EvoXAutoConfiguration`, `EvoXProperties` | ✅ |

## 🚀 快速开始

### 环境要求

在开始之前，请确保你的开发环境满足以下要求：

| 项目 | 要求 | 验证命令 |
|------|------|----------|
| **JDK** | 17+ | `java -version` |
| **Maven** | 3.8+ | `mvn -version` |
| **IDE** | IntelliJ IDEA / VS Code | 推荐 IntelliJ IDEA |
| **LLM API Key** | OpenAI 或其他提供商 | 可选，运行示例时需要 |

### 安装步骤

#### 1️⃣ 克隆项目

```bash
git clone https://github.com/your-org/evox.git
cd evox
```

#### 2️⃣ 编译安装

```bash
# 快速编译（跳过测试）
mvn clean install -DskipTests

# 完整编译（包含测试）
mvn clean install
```

> 💡 **提示**：编译需要 3-5 分钟，具体时间取决于网络速度和机器性能。

#### 3️⃣ 配置 API Key（可选）

如果要运行示例或连接真实 LLM，需要配置 API Key：

**方式 1：环境变量（推荐）**

```bash
export OPENAI_API_KEY="sk-your-api-key-here"
export OPENAI_MODEL="gpt-4o-mini"
```

**方式 2：配置文件**

创建 `application.yml`：

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4o-mini
          temperature: 0.7
          max-tokens: 2000
```

**方式 3：命令行参数**

```bash
mvn exec:java -DOPENAI_API_KEY=sk-xxx
```

### 第一个程序：简单聊天机器人

创建一个最简单的 AI 对话程序：

```java
import io.leavesfly.evox.agents.base.Agent;
import io.leavesfly.evox.models.openai.OpenAILLM;
import io.leavesfly.evox.models.config.LLMConfig;
import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.core.message.MessageType;

import java.util.Collections;

public class QuickStart {
    public static void main(String[] args) {
        // 1. 配置 LLM
        LLMConfig config = LLMConfig.builder()
            .apiKey(System.getenv("OPENAI_API_KEY"))
            .model("gpt-4o-mini")
            .temperature(0.7)
            .maxTokens(1000)
            .build();
        
        // 2. 创建 LLM 实例
        OpenAILLM llm = new OpenAILLM(config);
        
        // 3. 创建 Agent
        Agent agent = Agent.builder()
            .name("QuickStartBot")
            .description("A simple AI assistant")
            .llm(llm)
            .build();
        
        // 4. 发送消息
        Message userMessage = Message.builder()
            .content("你好！请简单介绍一下 EvoX 框架。")
            .messageType(MessageType.USER)
            .build();
        
        // 5. 获取回复
        Message response = agent.execute("chat", 
            Collections.singletonList(userMessage));
        
        // 6. 输出结果
        System.out.println("AI: " + response.getContent());
    }
}
```

**运行程序**：

```bash
# 方式 1：使用 Maven
mvn exec:java -Dexec.mainClass="QuickStart"

# 方式 2：直接运行
java QuickStart
```

**预期输出**：

```
AI: EvoX 是一个基于 Spring Boot 和 Spring AI 的企业级智能代理框架...
```

### 运行示例程序

EvoX 提供了多个开箱即用的示例：

```bash
cd evox-application/evox-examples

# 查看所有示例
./run-examples.sh

# 运行特定示例
mvn exec:java -Dexec.mainClass="io.leavesfly.evox.examples.SimpleChatBot"
mvn exec:java -Dexec.mainClass="io.leavesfly.evox.examples.ComprehensiveChatBot"
mvn exec:java -Dexec.mainClass="io.leavesfly.evox.examples.WorkflowDemo"
```

### 常见问题

<details>
<summary><strong>Q1: 编译失败怎么办？</strong></summary>

**问题**：`mvn clean install` 报错

**解决方案**：

```bash
# 1. 检查 Java 版本
java -version  # 应该是 17+

# 2. 检查 Maven 版本
mvn -version   # 应该是 3.8+

# 3. 清理并重新编译
mvn clean
rm -rf ~/.m2/repository/io/leavesfly/evox
mvn install -U -DskipTests
```
</details>

<details>
<summary><strong>Q2: API Key 错误怎么办？</strong></summary>

**问题**：`Unauthorized` 或 `Invalid API Key`

**解决方案**：

```bash
# 检查环境变量
echo $OPENAI_API_KEY

# 重新设置
export OPENAI_API_KEY="sk-your-correct-key"

# 测试 API Key
curl https://api.openai.com/v1/models \
  -H "Authorization: Bearer $OPENAI_API_KEY"
```
</details>

<details>
<summary><strong>Q3: 依赖下载慢怎么办？</strong></summary>

**解决方案**：配置国内 Maven 镜像

编辑 `~/.m2/settings.xml`：

```xml
<mirrors>
  <mirror>
    <id>aliyun</id>
    <mirrorOf>central</mirrorOf>
    <name>Aliyun Maven</name>
    <url>https://maven.aliyun.com/repository/public</url>
  </mirror>
</mirrors>
```
</details>

更多问题请查阅 [快速开始指南](doc/QUICKSTART.md) 或 [FAQ](doc/FAQ.md)。

## 💡 使用示例

### 示例 1：带记忆的对话

实现一个能够记住上下文的对话系统：

```java
import io.leavesfly.evox.memory.shortterm.ShortTermMemory;
import io.leavesfly.evox.agents.base.Agent;
import io.leavesfly.evox.models.openai.OpenAILLM;

public class MemoryChatBot {
    public static void main(String[] args) {
        // 1. 创建短期记忆
        ShortTermMemory memory = ShortTermMemory.builder()
            .capacity(100)        // 最大容量
            .windowSize(10)       // 滑动窗口大小
            .build();
        
        // 2. 创建带记忆的 Agent
        Agent agent = Agent.builder()
            .name("MemoryBot")
            .llm(new OpenAILLM(config))
            .memory(memory)
            .build();
        
        // 3. 多轮对话
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
            
            // 保存用户消息
            memory.addMessage(msg);
            
            // 获取历史上下文
            List<Message> context = memory.getLatestMessages(5);
            Message response = agent.execute("chat", context);
            
            // 保存 AI 回复
            memory.addMessage(response);
            
            System.out.println("Q: " + question);
            System.out.println("A: " + response.getContent());
            System.out.println();
        }
    }
}
```

**运行结果**：
```
Q: 我叫张三
A: 你好，张三！很高兴认识你。

Q: 我最喜欢的颜色是蓝色
A: 好的，我记住了，你喜欢蓝色。

Q: 你还记得我叫什么名字吗？
A: 当然记得，你叫张三。

Q: 我喜欢什么颜色？
A: 你最喜欢的颜色是蓝色。
```

### 示例 2：集成外部工具

让 Agent 具备调用外部工具的能力：

```java
import io.leavesfly.evox.tools.base.Toolkit;
import io.leavesfly.evox.tools.file.FileSystemTool;
import io.leavesfly.evox.tools.http.HttpTool;
import io.leavesfly.evox.tools.search.WebSearchTool;
import io.leavesfly.evox.agents.specialized.ToolAgent;

public class ToolIntegrationExample {
    public static void main(String[] args) {
        // 1. 创建工具集
        Toolkit toolkit = new Toolkit();
        toolkit.addTool(new FileSystemTool());     // 文件操作
        toolkit.addTool(new HttpTool());          // HTTP 请求
        toolkit.addTool(new WebSearchTool());     // 网络搜索
        
        // 2. 创建 ToolAgent
        ToolAgent toolAgent = ToolAgent.builder()
            .name("ToolBot")
            .llm(new OpenAILLM(config))
            .toolkit(toolkit)
            .build();
        
        // 3. 使用工具
        Message request = Message.builder()
            .content("请搜索最新的 AI 新闻，并保存到 news.txt 文件")
            .messageType(MessageType.USER)
            .build();
        
        Message response = toolAgent.execute("use-tool", 
            Collections.singletonList(request));
        
        System.out.println(response.getContent());
    }
}
```

### 示例 3：DAG 工作流编排

构建一个复杂的多步骤工作流：

```java
import io.leavesfly.evox.workflow.base.Workflow;
import io.leavesfly.evox.workflow.graph.WorkflowGraph;
import io.leavesfly.evox.workflow.graph.WorkflowNode;
import io.leavesfly.evox.workflow.graph.WorkflowNode.NodeType;

public class WorkflowExample {
    public static void main(String[] args) {
        // 1. 创建工作流节点
        WorkflowNode step1 = WorkflowNode.builder()
            .nodeId("analyze")
            .name("分析问题")
            .nodeType(NodeType.ACTION)
            .action(new AnalyzeAction())
            .build();
        
        WorkflowNode step2 = WorkflowNode.builder()
            .nodeId("search")
            .name("搜索信息")
            .nodeType(NodeType.ACTION)
            .action(new SearchAction())
            .build();
        
        WorkflowNode step3 = WorkflowNode.builder()
            .nodeId("summarize")
            .name("总结答案")
            .nodeType(NodeType.ACTION)
            .action(new SummarizeAction())
            .build();
        
        // 2. 构建工作流图
        WorkflowGraph graph = new WorkflowGraph();
        graph.addNode(step1);
        graph.addNode(step2);
        graph.addNode(step3);
        
        // 定义节点依赖关系
        graph.addEdge("analyze", "search");     // step1 -> step2
        graph.addEdge("search", "summarize");   // step2 -> step3
        
        // 3. 创建工作流
        Workflow workflow = Workflow.builder()
            .name("QA-Workflow")
            .description("问答系统工作流")
            .graph(graph)
            .llm(new OpenAILLM(config))
            .build();
        
        // 4. 执行工作流
        Map<String, Object> inputs = Map.of(
            "question", "什么是人工智能？"
        );
        
        Map<String, Object> result = workflow.execute(inputs);
        System.out.println("结果: " + result.get("answer"));
    }
}
```

**工作流执行过程**：
```
[分析问题] -> [搜索信息] -> [总结答案]
     ✅              ✅              ✅
```

### 示例 4：RAG 知识库问答

构建一个基于文档的问答系统：

```java
import io.leavesfly.evox.rag.base.RAGEngine;
import io.leavesfly.evox.rag.loader.DocumentLoader;
import io.leavesfly.evox.rag.retriever.VectorRetriever;
import io.leavesfly.evox.rag.config.RAGConfig;

public class RAGExample {
    public static void main(String[] args) {
        // 1. 创建文档加载器
        DocumentLoader loader = new DocumentLoader();
        List<Document> documents = loader.loadDirectory("./docs");
        
        System.out.println("已加载 " + documents.size() + " 个文档");
        
        // 2. 配置 RAG 引擎
        RAGConfig config = RAGConfig.builder()
            .chunkSize(500)          // 分块大小
            .chunkOverlap(50)        // 重叠大小
            .topK(3)                 // 检索 Top-K
            .similarityThreshold(0.7) // 相似度阈值
            .build();
        
        // 3. 创建 RAG 引擎
        RAGEngine ragEngine = RAGEngine.builder()
            .config(config)
            .llm(new OpenAILLM(llmConfig))
            .vectorStore(new InMemoryVectorStore())
            .build();
        
        // 4. 索引文档
        ragEngine.indexDocuments(documents);
        System.out.println("文档索引完成");
        
        // 5. 执行问答
        String question = "EvoX 框架的主要特性是什么？";
        RAGResult result = ragEngine.query(question);
        
        System.out.println("问题: " + question);
        System.out.println("答案: " + result.getAnswer());
        System.out.println("参考文档: " + result.getReferences());
    }
}
```

### 示例 5：多智能体辩论

让多个 Agent 通过辩论达成共识：

```java
import io.leavesfly.evox.frameworks.debate.MultiAgentDebate;
import io.leavesfly.evox.agents.base.Agent;

public class DebateExample {
    public static void main(String[] args) {
        // 1. 创建多个 Agent
        Agent agent1 = Agent.builder()
            .name("Optimist")
            .systemPrompt("你是一个乐观主义者")
            .llm(new OpenAILLM(config))
            .build();
        
        Agent agent2 = Agent.builder()
            .name("Pessimist")
            .systemPrompt("你是一个悲观主义者")
            .llm(new OpenAILLM(config))
            .build();
        
        Agent agent3 = Agent.builder()
            .name("Realist")
            .systemPrompt("你是一个现实主义者")
            .llm(new OpenAILLM(config))
            .build();
        
        // 2. 创建辩论框架
        MultiAgentDebate debate = MultiAgentDebate.builder()
            .agents(Arrays.asList(agent1, agent2, agent3))
            .maxRounds(3)              // 最多 3 轮辩论
            .moderator(new OpenAILLM(config))  // 主持人
            .build();
        
        // 3. 开始辩论
        String topic = "AI 是否会取代人类的大部分工作？";
        DebateResult result = debate.startDebate(topic);
        
        // 4. 查看结果
        System.out.println("辩题: " + topic);
        System.out.println("辩论轮次: " + result.getRounds());
        System.out.println("最终结论: " + result.getConclusion());
    }
}
```

### 示例 6：提示词优化

自动优化提示词获得更好效果：

```java
import io.leavesfly.evox.optimizers.TextGrad;
import io.leavesfly.evox.optimizers.config.OptimizerConfig;

public class OptimizerExample {
    public static void main(String[] args) {
        // 1. 准备训练数据
        List<TrainingSample> samples = Arrays.asList(
            new TrainingSample("What is AI?", "AI is..."),
            new TrainingSample("How does ML work?", "ML works by...")
        );
        
        // 2. 创建优化器
        OptimizerConfig config = OptimizerConfig.builder()
            .learningRate(0.1)
            .batchSize(4)
            .maxIterations(10)
            .build();
        
        TextGrad optimizer = new TextGrad(config);
        
        // 3. 定义评估函数
        EvaluationFunction evalFunc = (prompt, samples) -> {
            // 根据提示词和样本计算得分
            return calculateScore(prompt, samples);
        };
        
        // 4. 优化提示词
        String initialPrompt = "You are a helpful assistant.";
        OptimizationResult result = optimizer.optimize(
            initialPrompt, 
            samples, 
            evalFunc
        );
        
        System.out.println("原始提示词: " + initialPrompt);
        System.out.println("优化后: " + result.getBestPrompt());
        System.out.println("性能提升: " + result.getImprovement() + "%");
    }
}
```

### 更多示例

查看 [`evox-examples`](evox-application/evox-examples) 模块获取更多完整示例：

| 示例名称 | 功能说明 | 代码位置 |
|---------|---------|----------|
| **SimpleChatBot** | 基础聊天机器人 | `examples/SimpleChatBot.java` |
| **ComprehensiveChatBot** | 多代理协同聊天 | `examples/ComprehensiveChatBot.java` |
| **WorkflowDemo** | 复杂工作流示例 | `examples/WorkflowDemo.java` |
| **ActionAgentExample** | 动作执行示例 | `examples/ActionAgentExample.java` |
| **MemoryAgentExample** | 记忆管理示例 | `examples/MemoryAgentExample.java` |
| **ToolsExample** | 工具集成示例 | `examples/ToolsExample.java` |
| **BenchmarkExample** | 性能测试示例 | `examples/BenchmarkExample.java` |
| **OptimizerExample** | 优化器示例 | `examples/optimizer/OptimizerExample.java` |
| **HITLExample** | 人机协同示例 | `examples/hitl/HITLExample.java` |

**运行示例的方法**：

```bash
# 进入示例目录
cd evox-application/evox-examples

# 使用脚本运行（交互式菜单）
./run-examples.sh

# 或直接运行指定示例
mvn exec:java -Dexec.mainClass="io.leavesfly.evox.examples.SimpleChatBot"
```

## 🔧 开发指南

### 项目结构

项目采用分层目录结构，清晰反映架构设计：

```
evox/
├── pom.xml                           # 父 POM
├── README.md                         # 项目说明
├── docs/                             # 文档目录
│
├── evox-core/                        # 核心层
│   ├── evox-core/                    # 核心抽象
│   ├── evox-models/                  # 模型适配
│   └── evox-actions/                 # 动作引擎
│
├── evox-runtime/                     # 运行时层
│   ├── evox-capability/              # 能力集成（记忆/存储/工具）
│   ├── evox-agents/                  # 智能代理
│   └── evox-workflow/                # 工作流编排
│
├── evox-extensions/                  # 扩展层
│   ├── evox-rag/                     # RAG 检索增强
│   ├── evox-optimizers/              # 优化器
│   └── evox-hitl/                    # 人机协同
│
└── evox-application/                 # 应用层
    ├── evox-examples/                # 示例应用
    ├── evox-benchmark/               # 基准测试
    └── evox-spring-boot-starter/     # Spring Boot Starter
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

### 测试状态

> ⚠️ **重要提示**: 项目当前处于早期开发阶段，测试覆盖率严重不足

| 模块 | 单元测试 | 集成测试 | 状态 |
|------|---------|---------|------|
| evox-core | ❌ 缺失 | ❌ 缺失 | 待补充 |
| evox-models | ❌ 缺失 | ❌ 缺失 | 待补充 |
| evox-actions | ❌ 缺失 | ❌ 缺失 | 待补充 |
| evox-agents | ❌ 缺失 | ❌ 缺失 | 待补充 |
| evox-workflow | ❌ 缺失 | ❌ 缺失 | 待补充 |
| evox-memory | ❌ 缺失 | ❌ 缺失 | 待补充 |
| evox-tools | ❌ 缺失 | ❌ 缺失 | 待补充 |
| evox-optimizers | ✅ 13个测试 | ❌ 缺失 | 基础覆盖 |
| evox-hitl | ✅ 16个测试 | ❌ 缺失 | 基础覆盖 |

**测试总数**: ~30 个单元测试  
**集成测试**: 0 个  
**覆盖率**: < 20% (估计)

### 功能完整度

查看详细的功能实现状态: [FEATURE_STATUS.md](FEATURE_STATUS.md)

**完整实现**: 10/17 模块 (59%)  
**部分实现**: 5/17 模块 (29%)  
**占位符**: 2/17 模块 (12%)

### 性能指标

> ⚠️ **注意**: 以下性能指标为理论估计值，尚未经过实际基准测试验证

基于 `evox-benchmark` 模块的预期性能目标：

- **Agent 执行延迟**: < 100ms (不含 LLM 调用，待测试)
- **工作流编排延迟**: < 50ms (单节点，待测试)
- **记忆检索延迟**: < 10ms (1000 条记录，待测试)
- **并发支持**: 目标 1000+ 并发请求 (待验证)

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



## 🙏 致谢

感谢以下开源项目和贡献者：

- [Spring Framework](https://spring.io/)
- [Spring AI](https://spring.io/projects/spring-ai)
- [Project Reactor](https://projectreactor.io/)
- [OpenAI](https://openai.com/)

<div align="center">

**如果这个项目对你有帮助，请给我们一个 ⭐️ Star！**

Made with ❤️ by EvoX Team

</div>
