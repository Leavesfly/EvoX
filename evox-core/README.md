# 核心层 (Core Layer)

核心层是 EvoX 整体架构的基石，提供最底层的抽象与基础设施，所有上层模块均依赖于此层的接口和能力。

## 模块概览

| 模块 | 说明 | 对外依赖 |
|------|------|---------|
| **[evox-core](evox-core/)** | 核心抽象与基础设施：BaseModule、Message、IAgent、LLM 接口、熔断器、重试策略、异常体系 | Spring Boot、Project Reactor |
| **[evox-models](evox-models/)** | LLM 多模型适配层：统一 LLMProvider 接口 + 8 大主流模型提供商实现 | evox-core |
| **[evox-mcp](evox-mcp/)** | MCP 协议核心：Model Context Protocol 协议定义、Client/Server/Transport 抽象 | evox-core |

## 各模块职责

### evox-core — 核心抽象

整个框架的零依赖基础，提供所有模块共享的核心抽象：

- **BaseModule**：所有模块的统一基类，提供序列化/反序列化、文件持久化、深拷贝、初始化钩子
- **消息系统**：`Message` / `MessageType`，定义智能体间通信的统一消息载体
- **智能体接口**：`IAgent` / `IAgentManager`，打破模块间循环依赖的核心 SPI
- **LLM 接口**：`ILLM` / `ILLMSync` / `ILLMAsync` / `ILLMStream` / `ILLMToolUse` / `LLMConfig`
- **注册机制**：`ModuleRegistry`，集中管理所有模块的注册与查找
- **容错能力**：`CircuitBreaker`（熔断器）、`RetryPolicy` + `RetryExecutor`（重试策略）
- **异常体系**：`EvoXException` 及派生的 `ModuleException`、`ExecutionException`、`ConfigurationException`、`ValidationException`

### evox-models — LLM 模型适配

基于 Clean Architecture 的多模型适配层，上层业务只依赖 `LLMProvider` 接口：

| 提供商 | 实现类 | 协议 | Tool Use |
|--------|--------|------|----------|
| OpenAI | `OpenAILLM` | OpenAI 兼容 | ✅ |
| 阿里云通义 | `AliyunLLM` | OpenAI 兼容 | ✅ |
| DeepSeek | `DeepSeekLLM` | OpenAI 兼容 | ✅ |
| Anthropic | `AnthropicLLM` | 自定义 | ✅ |
| Gemini | `GeminiLLM` | 自定义 | ✅ |
| OpenRouter | `OpenRouterLLM` | OpenAI 兼容 | ✅ |
| 硅基流动 | `SiliconFlowLLM` | OpenAI 兼容 | ❌ |
| Ollama | `OllamaLLM` | OpenAI 兼容 | ❌ |

分层架构：`spi` → `protocol` → `support` → `provider` → `config`

### evox-mcp — MCP 协议

[Model Context Protocol](https://modelcontextprotocol.io/) 协议的核心定义层，供 `evox-mcp-runtime`（运行时层）实现具体的 Client/Server 逻辑：

- **MCPProtocol**：JSON-RPC 2.0 消息格式、方法名常量、错误码、能力协商等全部协议定义
- **MCPClient / MCPServer**：客户端与服务端的抽象接口
- **MCPSession / MCPTransport**：会话管理与传输层抽象
- **MCPTool / MCPResource / MCPPrompt**：工具、资源、提示词的模型定义
- **MCPException**：MCP 专属异常体系

## 依赖关系

```
基础设施层 (Spring Boot 3.2+, Project Reactor, Jackson)
    ↑
evox-core          ← 零业务依赖的核心抽象
    ↑           ↑
evox-models    evox-mcp
```

依赖规则：**只允许向下依赖，禁止向上依赖**。

## 设计原则

| 原则 | 说明 |
|------|------|
| **最小依赖** | 核心层仅依赖 Spring Boot 和基础设施库，保持轻量 |
| **稳定接口** | 所有对外 SPI 接口遵循向后兼容约定，避免破坏性变更 |
| **高内聚** | 每个模块职责单一，包结构清晰，无交叉引用 |
| **低耦合** | 模块间通过接口/抽象类通信，具体实现对上层透明 |
| **响应式** | LLM 接口同时提供同步、异步、流式三种调用模式 |
| **容错设计** | 内置熔断器与重试策略，提升分布式调用可靠性 |

## 快速上手

```xml
<!-- 核心抽象 -->
<dependency>
    <groupId>io.leavesfly.evox</groupId>
    <artifactId>evox-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>

<!-- LLM 模型适配（按需引入） -->
<dependency>
    <groupId>io.leavesfly.evox</groupId>
    <artifactId>evox-models</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>

<!-- MCP 协议定义（按需引入） -->
<dependency>
    <groupId>io.leavesfly.evox</groupId>
    <artifactId>evox-mcp</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

```java
// 使用 LLMFactory 快速创建模型实例
LLMProvider llm = LLMFactory.openai("sk-xxx", "gpt-4o-mini");
String response = llm.generate("解释什么是多智能体框架");

// 使用熔断器保护 LLM 调用
CircuitBreaker breaker = CircuitBreaker.defaultBreaker("llm-service");
String result = breaker.execute(() -> llm.generate("你好"));

// 使用重试策略
RetryExecutor retry = new RetryExecutor(RetryPolicy.defaultPolicy());
String data = retry.execute(() -> llm.generate("请重试这个请求"));
```

## 子模块文档

- [evox-core 核心抽象详细文档](evox-core/README.md)
- [evox-models LLM 适配详细文档](evox-models/README.md)
