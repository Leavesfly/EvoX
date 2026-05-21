# EvoX 架构总览

## 设计理念

EvoX 是一个企业级 Java AI Agent 开发框架，遵循以下核心设计理念：

- **企业级**：基于成熟的 Java 生态，提供生产级别的稳定性、可维护性和可扩展性
- **模块化**：采用清晰的分层架构，各模块职责明确，支持按需引入和独立演进
- **响应式**：全面采用 Project Reactor 实现非阻塞异步编程，提升系统吞吐量和资源利用率
- **可扩展**：通过 SPI（Service Provider Interface）机制支持插件化扩展，满足多样化业务需求

## 四层架构

EvoX 采用分层架构设计，自下而上分为四层：

### 1. Core Layer（核心层）

提供框架的基础能力和抽象接口：

- **evox-core**：核心抽象，包括 Agent、LLM、Tool、Memory 等基础接口定义
- **evox-models**：大模型适配器，支持 OpenAI、Anthropic、通义千问等多种 LLM 提供商
- **evox-mcp**：Model Context Protocol 支持，实现标准化的工具调用协议

### 2. Runtime Layer（运行时层）

提供 Agent 运行所需的核心组件：

- **evox-agents**：Agent 实现，包括 ChatBotAgent、ReActAgent 等内置 Agent
- **evox-workflow**：工作流引擎，支持顺序、并行、条件分支等复杂执行流程
- **evox-tools**：工具集，提供文件操作、网络请求、代码执行等内置工具
- **evox-memory**：记忆管理，支持短期记忆（对话历史）和长期记忆（向量存储）
- **evox-rag**：检索增强生成，集成向量数据库实现知识检索
- **evox-storage**：持久化存储，支持对话历史、Agent 状态等的持久化
- **evox-mcp-runtime**：MCP 运行时，处理 MCP 协议的序列化和通信

### 3. Extensions Layer（扩展层）

提供高级功能和优化能力：

- **evox-optimizers**：提示词优化器，支持自动提示词工程
- **evox-hitl**：Human-in-the-Loop，支持人工介入和审批流程
- **evox-evaluation**：评估框架，提供 Agent 性能和质量评估工具

### 4. Application Layer（应用层）

提供示例应用和集成方案：

- **evox-examples**：示例代码，展示各种使用场景和最佳实践
- **evox-benchmark**：基准测试，用于性能对比和回归测试
- **evox-spring-boot-starter**：Spring Boot 集成，简化 Spring 项目中的接入
- **evox-claudecode**：Claude Code 集成，支持与 Anthropic Claude 生态对接
- **evox-cowork**：协同工作，支持多 Agent 协作场景

## 模块依赖关系

```
┌─────────────────────────────────────────────────────┐
│              Application Layer                       │
│  evox-examples → evox-benchmark → evox-spring-boot  │
│  evox-claudecode → evox-cowork                      │
└──────────────────────┬──────────────────────────────┘
                       │ depends on
┌──────────────────────▼──────────────────────────────┐
│              Extensions Layer                        │
│  evox-optimizers → evox-hitl → evox-evaluation      │
└──────────────────────┬──────────────────────────────┘
                       │ depends on
┌──────────────────────▼──────────────────────────────┐
│              Runtime Layer                           │
│  evox-agents → evox-workflow → evox-tools           │
│  evox-memory → evox-rag → evox-storage              │
│  evox-mcp-runtime                                    │
└──────────────────────┬──────────────────────────────┘
                       │ depends on
┌──────────────────────▼──────────────────────────────┐
│              Core Layer                              │
│  evox-core ← evox-models ← evox-mcp                 │
└─────────────────────────────────────────────────────┘
```

**依赖方向**：上层模块依赖下层模块，同层模块之间保持松散耦合

## 关键设计模式

### 1. SPI（Service Provider Interface）

EvoX 广泛使用 SPI 机制实现插件化扩展：

- LLM 提供商通过 SPI 注册，支持动态发现和加载
- Tool 实现通过 SPI 暴露，便于第三方扩展
- Memory Backend 通过 SPI 切换不同存储实现

### 2. Clean Architecture

遵循整洁架构原则：

- **依赖倒置**：高层模块不依赖低层模块的具体实现，而是依赖抽象接口
- **单一职责**：每个模块专注于一个领域，如 evox-models 只负责 LLM 适配
- **边界清晰**：通过包结构和方法可见性控制模块间访问

### 3. Builder Pattern

复杂对象采用 Builder 模式构建：

```java
ChatBotAgent agent = ChatBotAgent.builder()
    .name("Assistant")
    .llm(openaiLLM)
    .memory(memory)
    .tools(toolRegistry)
    .build();
```

### 4. 响应式编程

全面采用 Project Reactor：

- 所有 I/O 操作返回 `Mono` 或 `Flux`
- 支持背压（Backpressure）和异步流处理
- 通过 `subscribeOn` 和 `publishOn` 控制线程调度

## 技术栈

| 组件 | 版本/说明 |
|------|----------|
| Java | 17+ |
| Spring Boot | 3.2.5 |
| Project Reactor | 响应式编程核心库 |
| Jackson | 2.15.4（JSON 序列化） |
| Maven | 构建工具 |
| groupId | `io.leavesfly.evox` |

## 总结

EvoX 通过清晰的分层架构和模块化设计，实现了高内聚低耦合的企业级 AI Agent 框架。响应式编程模型确保了高性能和高并发能力，SPI 机制提供了良好的可扩展性，使得开发者可以灵活地定制和扩展框架功能。
