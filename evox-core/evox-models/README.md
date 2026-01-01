# EvoX Models LLM模型适配模块

## 📦 模块定位

**层级**: 核心层 (Core Layer)  
**职责**: 提供统一的LLM模型适配层,支持多种大模型提供商  
**依赖**: evox-core

## 🎯 核心功能

evox-models 为 EvoX 框架提供统一的大语言模型(LLM)抽象接口和多种模型提供商的适配实现,屏蔽不同模型API的差异,让上层业务无需关心具体模型实现。

### 支持的模型提供商

| 提供商 | 实现类 | 默认模型 | 特点 |
|--------|--------|---------|------|
| **OpenAI** | `OpenAILLM` | `gpt-4o-mini` | 官方模型,性能强大 |
| **阿里云通义** | `AliyunLLM` | `qwen-plus` | 国产模型,中文友好 |
| **硅基流动** | `SiliconFlowLLM` | 可选 | 多种开源模型,成本低 |
| **LiteLLM** | `LiteLLM` | 可选 | 统一接口,支持100+模型 |
| **OpenRouter** | `OpenRouterLLM` | 可选 | 模型路由,灵活选择 |

### 1. BaseLLM 接口

所有LLM实现的统一接口,提供以下核心方法:

```java
public interface BaseLLM {
    // 同步生成
    String generate(String prompt);
    String generate(List<Message> messages);
    
    // 异步生成
    Mono<String> generateAsync(String prompt);
    Mono<String> generateAsync(List<Message> messages);
    
    // 流式生成
    Flux<String> stream(String prompt);
    Flux<String> stream(List<Message> messages);
    
    // 配置管理
    LLMConfig getConfig();
}
```

### 2. LLMConfig 配置体系

统一的配置基类,支持以下通用参数:

**基础配置**:
- `provider`: 提供商标识
- `model`: 模型名称
- `apiKey`: API密钥
- `baseUrl`: API基础URL

**生成参数**:
- `temperature`: 温度参数(0.0-2.0),控制随机性
- `maxTokens`: 最大生成token数
- `topP`: Top-p采样参数
- `frequencyPenalty`: 频率惩罚
- `presencePenalty`: 存在惩罚

**高级参数**:
- `stream`: 是否启用流式输出
- `timeout`: 请求超时时间
- `outputResponse`: 是否输出响应到控制台

### 3. 提供商实现

#### OpenAI

```java
OpenAILLMConfig config = OpenAILLMConfig.builder()
    .apiKey(System.getenv("OPENAI_API_KEY"))
    .model("gpt-4o-mini")
    .temperature(0.7f)
    .maxTokens(2000)
    .build();

OpenAILLM llm = new OpenAILLM(config);
String response = llm.generate("解释什么是人工智能");
```

#### 阿里云通义

```java
AliyunLLMConfig config = AliyunLLMConfig.builder()
    .apiKey(System.getenv("DASHSCOPE_API_KEY"))
    .model("qwen-plus")
    .temperature(0.7f)
    .build();

AliyunLLM llm = new AliyunLLM(config);
String response = llm.generate("写一首关于春天的诗");
```

#### 硅基流动 (SiliconFlow)

支持多种开源模型,性价比高:

```java
SiliconFlowConfig config = SiliconFlowConfig.builder()
    .apiKey(System.getenv("SILICONFLOW_API_KEY"))
    .model("Qwen/Qwen2.5-7B-Instruct")
    .temperature(0.7f)
    .maxTokens(1000)
    .build();

SiliconFlowLLM llm = new SiliconFlowLLM(config);
String response = llm.generate("编写一个Python快速排序");
```

**支持的模型示例**:
- Qwen系列: `Qwen/Qwen2.5-7B-Instruct`
- DeepSeek系列: `deepseek-ai/DeepSeek-V2.5`
- 其他开源模型...

#### LiteLLM (统一接口)

通过LiteLLM Proxy访问100+模型:

```java
LiteLLMConfig config = LiteLLMConfig.builder()
    .litellmBaseUrl("http://localhost:4000")
    .model("gpt-4o-mini")
    .openaiKey(System.getenv("OPENAI_API_KEY"))
    .anthropicKey(System.getenv("ANTHROPIC_API_KEY"))
    .build();

LiteLLM llm = new LiteLLM(config);
```

**支持的提供商**:
- OpenAI (gpt-4, gpt-3.5-turbo, ...)
- Anthropic (claude-3-5-sonnet, ...)
- Google (gemini-pro, ...)
- DeepSeek, Groq, Perplexity ...

### 4. 流式输出

所有LLM实现都支持流式输出:

```java
Flux<String> stream = llm.stream("讲一个有趣的故事");
stream.subscribe(
    chunk -> System.out.print(chunk),  // 处理每个chunk
    error -> log.error("Error", error),
    () -> System.out.println("\n完成")
);
```

### 5. 异步调用

基于 Reactor 的响应式编程支持:

```java
Mono<String> async = llm.generateAsync("分析市场趋势");
async.subscribe(
    result -> log.info("结果: {}", result),
    error -> log.error("错误", error)
);
```

## 📂 目录结构

```
evox-models/
├── base/                   # 基础接口
│   └── BaseLLM.java
├── config/                 # 配置类
│   ├── LLMConfig.java      # 配置基类
│   ├── OpenAILLMConfig.java
│   ├── AliyunLLMConfig.java
│   ├── SiliconFlowConfig.java
│   ├── LiteLLMConfig.java
│   └── OpenRouterConfig.java
├── openai/                 # OpenAI实现
│   └── OpenAILLM.java
├── aliyun/                 # 阿里云实现
│   └── AliyunLLM.java
├── siliconflow/            # 硅基流动实现
│   ├── SiliconFlowLLM.java
│   └── SiliconFlowModel.java
├── litellm/                # LiteLLM实现
│   └── LiteLLM.java
└── openrouter/             # OpenRouter实现
    └── OpenRouterLLM.java
```

## 🚀 快速开始

### Maven 依赖

```xml
<dependency>
    <groupId>io.leavesfly.evox</groupId>
    <artifactId>evox-models</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Spring Boot 自动配置

在 `application.yml` 中配置:

```yaml
evox:
  llm:
    provider: openai              # 或 dashscope, litellm, siliconflow
    api-key: ${OPENAI_API_KEY}
    model: gpt-4o-mini
    temperature: 0.7
    max-tokens: 2000
    timeout: 30000
```

然后直接注入使用:

```java
@Autowired
private BaseLLM llm;

public String chat(String input) {
    return llm.generate(input);
}
```

### 环境变量配置

创建 `.env` 文件:

```bash
# OpenAI
OPENAI_API_KEY=sk-your-api-key
OPENAI_MODEL=gpt-4o-mini

# 阿里云通义
DASHSCOPE_API_KEY=your-dashscope-key
DASHSCOPE_MODEL=qwen-plus

# 硅基流动
SILICONFLOW_API_KEY=your-siliconflow-key

# LiteLLM
LITELLM_API_KEY=your-api-key
LITELLM_BASE_URL=http://localhost:4000
```

### 编程式创建

```java
// 方式1: 使用Builder
OpenAILLMConfig config = OpenAILLMConfig.builder()
    .apiKey("sk-xxx")
    .model("gpt-4o-mini")
    .temperature(0.7f)
    .maxTokens(2000)
    .build();
    
OpenAILLM llm = new OpenAILLM(config);

// 方式2: 配置对象
LLMConfig config = new OpenAILLMConfig();
config.setApiKey("sk-xxx");
config.setModel("gpt-4o-mini");
config.setTemperature(0.7f);

BaseLLM llm = new OpenAILLM((OpenAILLMConfig) config);
```

## 💡 高级用法

### 1. 消息历史对话

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

### 2. 自定义参数

```java
OpenAILLMConfig config = OpenAILLMConfig.builder()
    .apiKey("sk-xxx")
    .model("gpt-4o")
    .temperature(0.9f)           // 更高的创造性
    .maxTokens(4000)             // 更长的输出
    .topP(0.95f)                 // Top-p采样
    .frequencyPenalty(0.5f)      // 频率惩罚
    .presencePenalty(0.5f)       // 存在惩罚
    .build();
```

### 3. 切换模型

```java
// 开发环境: 使用快速便宜的模型
BaseLLM devLLM = new OpenAILLM(
    OpenAILLMConfig.builder()
        .model("gpt-4o-mini")
        .build()
);

// 生产环境: 使用更强大的模型
BaseLLM prodLLM = new OpenAILLM(
    OpenAILLMConfig.builder()
        .model("gpt-4o")
        .build()
);
```

### 4. 统一多模型访问

使用 LiteLLM 统一访问多个提供商:

```java
LiteLLMConfig config = LiteLLMConfig.builder()
    .litellmBaseUrl("http://localhost:4000")
    .model("gpt-4o-mini")          // OpenAI
    // .model("claude-3-5-sonnet")  // Anthropic
    // .model("gemini-pro")         // Google
    .build();
```

## 🎓 设计原则

- **统一抽象**: BaseLLM接口屏蔽不同模型差异
- **灵活配置**: 支持代码配置和环境变量配置
- **响应式编程**: 基于Reactor,支持异步和流式
- **可扩展性**: 易于添加新的模型提供商

## 📊 适用场景

- 智能对话系统
- 文本生成和改写
- 代码生成和解释
- 知识问答
- 内容摘要和翻译
- Prompt优化和测试

## 🔗 相关模块

- **evox-core**: 提供基础抽象和消息系统
- **evox-actions**: 使用LLM执行各种动作
- **evox-agents**: Agent使用LLM进行推理和决策
- **evox-optimizers**: 优化Prompt和模型参数

## ⚠️ 注意事项

1. **API密钥安全**: 不要将API密钥硬编码,使用环境变量
2. **成本控制**: 设置合理的`maxTokens`,避免过度消耗
3. **超时设置**: 根据业务场景设置合理的`timeout`
4. **错误处理**: 捕获并处理LLM调用异常
5. **速率限制**: 注意各提供商的API速率限制
