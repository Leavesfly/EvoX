# EvoX Models — LLM 模型适配模块

## 📦 模块定位

**层级**: 核心层 (Core Layer)  
**职责**: 提供统一的 LLM 模型适配层，支持多种大模型提供商  
**依赖**: evox-core  
**架构**: Clean Architecture（SPI → Protocol → Support → Provider → Config）

## 🎯 核心功能

evox-models 为 EvoX 框架提供统一的大语言模型 (LLM) 抽象接口和多种模型提供商的适配实现，屏蔽不同模型 API 的差异，让上层业务无需关心具体模型实现。

### 支持的模型提供商

| 提供商 | 实现类 | 默认模型 | 协议 | Tool Use |
|--------|--------|---------|------|----------|
| **OpenAI** | `OpenAILLM` | `gpt-4o-mini` | OpenAI 兼容 | ✅ |
| **阿里云通义** | `AliyunLLM` | `qwen-turbo` | OpenAI 兼容 | ✅ |
| **DeepSeek** | `DeepSeekLLM` | `deepseek-chat` | OpenAI 兼容 | ✅ |
| **Anthropic** | `AnthropicLLM` | `claude-3-5-sonnet` | 自定义 | ✅ |
| **Gemini** | `GeminiLLM` | `gemini-pro` | 自定义 | ✅ |
| **OpenRouter** | `OpenRouterLLM` | 可选 | OpenAI 兼容 | ✅ |
| **硅基流动** | `SiliconFlowLLM` | 可选 | OpenAI 兼容 | ❌ |
| **Ollama** | `OllamaLLM` | `llama2` | OpenAI 兼容 | ❌ |

## 📂 包结构 (Clean Architecture)

```
io.leavesfly.evox.models/
│
├── spi/                        # 服务提供者接口层 (最内层)
│   ├── LLMProvider.java        #   核心 SPI 接口，继承 ILLM + ILLMToolUse
│   └── LLMException.java       #   统一异常定义
│
├── protocol/                   # 协议层 — OpenAI 兼容 HTTP 协议
│   ├── OpenAiCompatibleClient.java
│   ├── ChatCompletionRequest.java
│   ├── ChatCompletionResponse.java
│   ├── ChatCompletionResult.java
│   ├── ToolCall.java
│   ├── ToolDefinition.java
│   ├── EmbeddingRequest.java
│   ├── EmbeddingResponse.java
│   ├── ImageGenerationRequest.java
│   └── ImageGenerationResponse.java
│
├── support/                    # 支撑层 — 公共基类
│   └── OpenAiCompatibleLLM.java  # 所有 OpenAI 兼容 provider 的抽象基类
│
├── provider/                   # Provider 实现层 (每个 provider 内聚)
│   ├── openai/
│   │   ├── OpenAILLMConfig.java
│   │   └── OpenAILLM.java
│   ├── deepseek/
│   │   ├── DeepSeekLLMConfig.java
│   │   └── DeepSeekLLM.java
│   ├── aliyun/
│   │   ├── AliyunLLMConfig.java
│   │   └── AliyunLLM.java
│   ├── ollama/
│   │   ├── OllamaLLMConfig.java
│   │   └── OllamaLLM.java
│   ├── anthropic/
│   │   ├── AnthropicLLMConfig.java
│   │   └── AnthropicLLM.java
│   ├── gemini/
│   │   ├── GeminiLLMConfig.java
│   │   └── GeminiLLM.java
│   ├── openrouter/
│   │   ├── OpenRouterLLMConfig.java
│   │   └── OpenRouterLLM.java
│   └── siliconflow/
│       ├── SiliconFlowLLMConfig.java
│       └── SiliconFlowLLM.java
│
└── config/                     # 配置门面层 (最外层)
    ├── LLMConfigs.java         #   配置快捷创建工具
    └── LLMFactory.java         #   LLM 工厂，根据配置自动创建实例
```

### 架构分层说明

| 层 | 职责 | 依赖方向 |
|----|------|----------|
| **spi** | 定义核心接口和异常，不依赖任何实现 | 无外部依赖 |
| **protocol** | 封装 OpenAI 兼容 HTTP 协议细节 | → spi |
| **support** | 提取公共逻辑为抽象基类，消除重复代码 | → spi, protocol |
| **provider** | 各 provider 的实现类和配置类内聚在一起 | → spi, protocol, support |
| **config** | 对外暴露的配置门面和工厂 | → provider |

## 🚀 快速开始

### Maven 依赖

```xml
<dependency>
    <groupId>io.leavesfly.evox</groupId>
    <artifactId>evox-models</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 方式一：使用 LLMFactory 快捷创建

```java
import io.leavesfly.evox.models.config.LLMFactory;
import io.leavesfly.evox.models.spi.LLMProvider;

// OpenAI
LLMProvider llm = LLMFactory.openai("sk-xxx", "gpt-4o");

// 阿里云通义千问
LLMProvider llm = LLMFactory.aliyun("sk-xxx", "qwen-max");

// DeepSeek
LLMProvider llm = LLMFactory.deepseek("sk-xxx");

// Ollama 本地模型
LLMProvider llm = LLMFactory.ollama("llama3");

// 使用环境变量（自动读取 OPENAI_API_KEY 等）
LLMProvider llm = LLMFactory.openai();
```

### 方式二：使用 LLMConfigs 创建配置

```java
import io.leavesfly.evox.models.config.LLMConfigs;
import io.leavesfly.evox.models.config.LLMFactory;

var config = LLMConfigs.openAI("sk-xxx", "gpt-4o-mini");
LLMProvider llm = LLMFactory.create(config);

String response = llm.generate("解释什么是人工智能");
```

### 方式三：直接使用 Provider

```java
import io.leavesfly.evox.models.provider.openai.*;

OpenAILLMConfig config = OpenAILLMConfig.builder()
    .apiKey("sk-xxx")
    .model("gpt-4o-mini")
    .temperature(0.7f)
    .maxTokens(2000)
    .build();

OpenAILLM llm = new OpenAILLM(config);
String response = llm.generate("解释什么是人工智能");
```

### 环境变量配置

```bash
# OpenAI
OPENAI_API_KEY=sk-your-api-key

# 阿里云通义
DASHSCOPE_API_KEY=your-dashscope-key

# DeepSeek
DEEPSEEK_API_KEY=your-deepseek-key

# 硅基流动
SILICONFLOW_API_KEY=your-siliconflow-key
```

## 💡 高级用法

### 消息历史对话

```java
List<Message> messages = List.of(
    Message.builder()
        .content("你是一个Python专家")
        .messageType(MessageType.SYSTEM)
        .build(),
    Message.builder()
        .content("如何实现快速排序?")
        .messageType(MessageType.INPUT)
        .build()
);

String response = llm.generate(messages);
```

### 流式输出

```java
Flux<String> stream = llm.stream("讲一个有趣的故事");
stream.subscribe(
    chunk -> System.out.print(chunk),
    error -> log.error("Error", error),
    () -> System.out.println("\n完成")
);
```

### Tool Use / Function Calling

```java
import io.leavesfly.evox.models.protocol.ToolDefinition;

List<ToolDefinition> tools = List.of(
    ToolDefinition.builder()
        .name("get_weather")
        .description("获取天气信息")
        .parameters(paramSchema)
        .build()
);

ChatCompletionResult result = llm.chatWithTools(messages, tools);
```

### 扩展自定义 Provider

继承 `OpenAiCompatibleLLM` 即可快速接入任何 OpenAI 兼容的 API：

```java
public class MyCustomLLM extends OpenAiCompatibleLLM {
    public MyCustomLLM(MyCustomConfig config) {
        super(config, "https://my-api.example.com/v1");
    }
}
```

## 🎓 设计原则

- **Clean Architecture**: 严格分层，依赖方向由外向内
- **Provider 内聚**: 每个 provider 的实现类和配置类放在同一个包下
- **消除重复**: OpenAI 兼容 provider 共享 `OpenAiCompatibleLLM` 基类
- **SPI 解耦**: 上层模块只依赖 `LLMProvider` 接口，不感知具体实现
- **响应式编程**: 基于 Reactor，支持同步、异步和流式调用

## 🔗 相关模块

- **evox-core**: 提供基础抽象和消息系统
- **evox-actions**: 使用 LLM 执行各种动作
- **evox-agents**: Agent 使用 LLM 进行推理和决策
- **evox-optimizers**: 优化 Prompt 和模型参数

## ⚠️ 注意事项

1. **API 密钥安全**: 不要将 API 密钥硬编码，使用环境变量
2. **成本控制**: 设置合理的 `maxTokens`，避免过度消耗
3. **超时设置**: 根据业务场景设置合理的 `timeout`
4. **错误处理**: 捕获并处理 `LLMException`
5. **速率限制**: 注意各提供商的 API 速率限制
