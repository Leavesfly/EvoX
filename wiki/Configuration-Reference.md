# 配置参考

本文档提供 EvoX 框架的完整配置项参考，包括所有可用配置项、默认值、类型说明以及使用示例。

## 完整 YAML 配置树

```yaml
evox:
  # 是否启用 EvoX（默认 true）
  enabled: true
  
  # LLM 配置
  llm:
    provider: openai                    # LLM 提供商
    api-key: ${OPENAI_API_KEY}          # API Key（必填）
    model: gpt-4o-mini                  # 模型名称
    temperature: 0.7                    # 温度参数 (0-2)
    max-tokens: 2000                    # 最大 token 数
    timeout: 30000                      # 超时时间（毫秒）
  
  # Agent 配置
  agents:
    default-timeout: 60000              # 默认超时时间（毫秒）
    max-concurrent: 10                  # 最大并发数
  
  # 记忆配置
  memory:
    short-term:
      capacity: 100                     # 短期记忆容量
      window-size: 10                   # 上下文窗口大小
    long-term:
      enabled: false                    # 是否启用长期记忆
      storage-type: in-memory           # 存储类型：in-memory, redis, database
  
  # 工具配置
  tools:
    enabled: true                       # 是否启用工具调用
    timeout: 30000                      # 工具调用超时（毫秒）
    max-retries: 3                      # 工具调用最大重试次数
```

**注意**: 当前版本的 Spring Boot Starter **仅支持** `evox.llm.*`, `evox.agents.*`, `evox.memory.*`, `evox.tools.*` 配置项。`evox.storage.*` 和 `evox.workflow.*` 配置项暂未在 Starter 中实现，需要通过代码手动配置。

## 配置项详细说明

### evox.llm - LLM 配置

| 配置项 | 类型 | 默认值 | 可选值 | 说明 |
|--------|------|--------|--------|------|
| `provider` | String | `openai` | openai, deepseek, ollama, aliyun, siliconflow | LLM 提供商 |
| `api-key` | String | - | - | API Key（必填，也可通过环境变量提供） |
| `model` | String | `gpt-4o-mini` | - | 模型名称 |
| `temperature` | Float | `0.7` | 0.0-2.0 | 控制输出的随机性，越高越随机 |
| `max-tokens` | Integer | `2000` | 1-32000 | 生成文本的最大 token 数 |
| `timeout` | Long | `30000` | - | HTTP 请求超时时间（毫秒） |

**注意**: 当前 Starter **不支持** `base-url` 和 `retry.*` 配置项，这些需要通过代码自定义 LLM 实例来配置。

### evox.agents - Agent 配置

| 配置项 | 类型 | 默认值 | 可选值 | 说明 |
|--------|------|--------|--------|------|
| `default-timeout` | Long | `60000` | - | Agent 执行的默认超时时间（毫秒） |
| `max-concurrent` | Integer | `10` | 1-100 | 允许同时运行的最大 Agent 数量 |

### evox.memory - 记忆配置

| 配置项 | 类型 | 默认值 | 可选值 | 说明 |
|--------|------|--------|--------|------|
| `short-term.capacity` | Integer | `100` | 10-1000 | 短期记忆可存储的最大消息数 |
| `short-term.window-size` | Integer | `10` | 1-50 | 发送给 LLM 的上下文窗口大小 |
| `long-term.enabled` | Boolean | `false` | true/false | 是否启用长期记忆功能 |
| `long-term.storage-type` | String | `in-memory` | in-memory, redis, database | 长期记忆的存储后端类型 |

### evox.tools - 工具配置

| 配置项 | 类型 | 默认值 | 可选值 | 说明 |
|--------|------|--------|--------|------|
| `enabled` | Boolean | `true` | true/false | 是否启用工具调用功能 |
| `timeout` | Long | `30000` | - | 单个工具调用的超时时间（毫秒） |
| `max-retries` | Integer | `3` | 0-10 | 工具调用失败时的最大重试次数 |

**未实现的配置项**:

以下配置项在当前版本的 Spring Boot Starter 中**尚未实现**，需要通过代码手动配置：

- `evox.storage.*` - 存储配置（type, vector.*）
- `evox.workflow.*` - 工作流配置（max-depth, timeout, enable-parallel）
- `evox.llm.base-url` - 自定义 API 地址
- `evox.llm.retry.*` - 重试配置

## 环境变量对照表

以下环境变量可直接用于配置 API Key，无需在 YAML 中硬编码：

| 环境变量 | 对应配置 | 说明 |
|----------|----------|------|
| `OPENAI_API_KEY` | `evox.llm.api-key` | OpenAI API Key |
| `DASHSCOPE_API_KEY` | `evox.llm.api-key` | 阿里云通义千问 API Key |
| `DEEPSEEK_API_KEY` | `evox.llm.api-key` | DeepSeek API Key |
| `SILICONFLOW_API_KEY` | `evox.llm.api-key` | SiliconFlow API Key |

使用示例：
```yaml
evox:
  llm:
    api-key: ${OPENAI_API_KEY}
```

或在启动时通过环境变量传入：
```bash
export OPENAI_API_KEY=sk-your-api-key
java -jar your-app.jar
```

## LLM Provider 配置示例

### OpenAI

```yaml
evox:
  llm:
    provider: openai
    api-key: ${OPENAI_API_KEY}
    model: gpt-4o-mini
```

**注意**: 如需自定义 `base-url`，请通过 `@Bean` 手动创建 `OpenAILLM` 实例。

### DeepSeek

```yaml
evox:
  llm:
    provider: deepseek
    api-key: ${DEEPSEEK_API_KEY}
    model: deepseek-chat
```

**注意**: 如需自定义 `base-url`，请通过 `@Bean` 手动创建 `DeepSeekLLM` 实例。

### Ollama（本地部署）

```yaml
evox:
  llm:
    provider: ollama
    api-key: ""  # Ollama 通常不需要 API Key
    model: llama3.2
```

**注意**: 如需自定义 `base-url`（默认为 `http://localhost:11434/v1`），请通过 `@Bean` 手动创建 `OllamaLLM` 实例。

### Aliyun（通义千问）

```yaml
evox:
  llm:
    provider: aliyun
    api-key: ${DASHSCOPE_API_KEY}
    model: qwen-turbo
```

### SiliconFlow

```yaml
evox:
  llm:
    provider: siliconflow
    api-key: ${SILICONFLOW_API_KEY}
    model: Qwen/Qwen2.5-7B-Instruct
```

## 生产环境推荐配置

```yaml
evox:
  llm:
    provider: openai
    api-key: ${OPENAI_API_KEY}
    model: gpt-4o
    temperature: 0.7
    max-tokens: 4000
    timeout: 60000
  
  agents:
    default-timeout: 120000
    max-concurrent: 20
  
  memory:
    short-term:
      capacity: 200
      window-size: 20
    long-term:
      enabled: true
      storage-type: redis  # 生产环境建议使用 Redis
  
  tools:
    enabled: true
    timeout: 60000
    max-retries: 3
```

**生产环境要点**：
- 增加超时时间和并发限制
- 通过环境变量管理敏感信息（API Key）
- 长期记忆建议使用 Redis 持久化
- 其他高级配置（如 storage、workflow）需通过代码自定义

## 开发环境推荐配置

```yaml
evox:
  llm:
    provider: openai
    api-key: ${OPENAI_API_KEY}
    model: gpt-4o-mini  # 开发环境使用更经济的模型
    temperature: 0.9    # 更高的创造性
    max-tokens: 2000
    timeout: 30000
  
  agents:
    default-timeout: 60000
    max-concurrent: 5
  
  memory:
    short-term:
      capacity: 50
      window-size: 10
    long-term:
      enabled: false  # 开发环境可禁用长期记忆
  
  tools:
    enabled: true
    timeout: 30000
    max-retries: 2
```

**开发环境要点**：
- 降低资源消耗，加快启动速度
- 使用经济型模型降低成本
- 简化配置，快速迭代
- 长期记忆默认禁用以减少依赖
