# LLM 模型适配 (evox-models)

## 模块概述

`evox-models` 是 EvoX 框架的 LLM 提供商适配层，基于 Clean Architecture 设计，支持多种主流大语言模型的统一接入。

---

## 支持的 LLM 提供商

| 提供商 | 实现类 | 默认模型 | API Key 环境变量 |
|--------|--------|----------|------------------|
| OpenAI | `OpenAILLM` | gpt-4o-mini | `OPENAI_API_KEY` |
| Anthropic | `AnthropicLLM` | claude-3-5-sonnet | `ANTHROPIC_API_KEY` |
| DeepSeek | `DeepSeekLLM` | deepseek-chat | `DEEPSEEK_API_KEY` |
| Gemini | `GeminiLLM` | gemini-pro | `GEMINI_API_KEY` |
| Ollama | `OllamaLLM` | llama2 | - |
| Aliyun (DashScope) | `AliyunLLM` | qwen-turbo | `DASHSCOPE_API_KEY` |
| OpenRouter | `OpenRouterLLM` | - | `OPENROUTER_API_KEY` |
| SiliconFlow | `SiliconFlowLLM` | - | `SILICONFLOW_API_KEY` |

---

## Clean Architecture 分层

```
spi         # 服务提供者接口定义（SPI）
protocol    # 协议层：统一的消息格式和通信协议
support     # 支撑层：HTTP 客户端、序列化等通用能力
provider    # 提供商实现层：各 LLM 的具体适配
config      # 配置层：LLMConfig、LLMConfigs 工厂
```

---

## 快速开始

### 1. 使用 LLMFactory 快捷创建

```java
// 通过工厂方法快速创建 LLM 实例
ILLM llm = LLMFactory.createOpenAI("gpt-4o-mini");
ILLM llm = LLMFactory.createDeepSeek("deepseek-chat");
ILLM llm = LLMFactory.createAliyun("qwen-turbo");
```

### 2. 使用 LLMConfigs 配置创建

```java
LLMConfig config = LLMConfigs.builder()
    .provider("openai")
    .apiKey(System.getenv("OPENAI_API_KEY"))
    .model("gpt-4o-mini")
    .temperature(0.7)
    .maxTokens(2048)
    .build();

ILLM llm = LLMFactory.create(config);
```

### 3. 直接使用 Provider

```java
OpenAILLM openAI = new OpenAILLM(new LLMConfig());
Message response = openAI.syncCall(inputMessage);
```

---

## 环境变量配置

在 `.env` 文件或系统环境中配置 API Key：

```bash
export OPENAI_API_KEY=sk-xxx
export DASHSCOPE_API_KEY=sk-xxx
export DEEPSEEK_API_KEY=sk-xxx
export SILICONFLOW_API_KEY=sk-xxx
export ANTHROPIC_API_KEY=sk-xxx
export GEMINI_API_KEY=AIzaSy-xxx
export OPENROUTER_API_KEY=sk-or-xxx
```

---

## 高级用法

### 消息历史对话

```java
List<Message> history = new ArrayList<>();
history.add(Message.ofUser("你好"));
history.add(Message.ofAssistant("你好！有什么可以帮助你的？"));
history.add(Message.ofUser("请介绍一下 Java"));

Message response = llm.syncCall(history);
```

### 流式输出（Flux）

```java
Flux<String> stream = llm.streamCall(inputMessage);
stream.subscribe(chunk -> System.out.print(chunk));
```

### Tool Use / Function Calling

```java
// 定义工具
Action tool = new Action("getWeather", "获取天气", params);

// 启用工具调用
ILLMToolUse toolLLM = (ILLMToolUse) llm;
Message response = toolLLM.callWithTools(inputMessage, List.of(tool));
```

---

## 扩展自定义 Provider

继承 `OpenAiCompatibleLLM` 快速接入兼容 OpenAI 格式的提供商：

```java
public class CustomLLM extends OpenAiCompatibleLLM {
    public CustomLLM(LLMConfig config) {
        super(config);
    }
    
    @Override
    protected String getBaseUrl() {
        return "https://api.custom-provider.com/v1";
    }
}
```

---

## Maven 依赖

```xml
<dependency>
    <groupId>io.leavesfly.evox</groupId>
    <artifactId>evox-models</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

---

## 包结构

```
io.leavesfly.evox.models
├── spi           # SPI 接口
├── protocol      # 协议定义
├── support       # 通用支撑组件
├── provider      # 各提供商实现
│   ├── openai
│   ├── anthropic
│   ├── deepseek
│   ├── gemini
│   ├── ollama
│   ├── aliyun
│   ├── openrouter
│   └── siliconflow
└── config        # 配置类
```
