# Spring Boot 集成 (evox-spring-boot-starter)

EvoX 提供了官方的 Spring Boot Starter，让您能够在 Spring Boot 应用中零配置快速集成 EvoX 的 AI 能力。

## 特性

- **零配置启动**：提供合理的默认配置，最小化即可运行
- **自动装配**：自动配置 LLM、Memory、Agent 等核心组件
- **YAML 配置**：支持标准的 Spring Boot YAML 配置方式
- **灵活扩展**：支持通过 Bean 自定义覆盖默认配置

## Maven 依赖

```xml
<dependency>
    <groupId>io.leavesfly.evox</groupId>
    <artifactId>evox-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## 最简配置

在 `application.yml` 中配置最基本的 LLM 信息即可启动：

```yaml
evox:
  llm:
    provider: openai
    api-key: ${OPENAI_API_KEY}
    model: gpt-4o-mini
```

## 自动注入使用示例

Starter 会自动装配以下常用 Bean，您可以直接注入使用：

```java
@Service
public class ChatService {
    
    @Autowired
    private LLMProvider llm;
    
    @Autowired
    private ShortTermMemory memory;
    
    public String chat(String message) {
        return llm.chat(message);
    }
}
```

## 完整 Spring Boot 应用示例

```java
@SpringBootApplication
public class EvoXApplication implements CommandLineRunner {
    
    @Autowired
    private LLMProvider llm;
    
    @Autowired
    private ShortTermMemory memory;
    
    public static void main(String[] args) {
        SpringApplication.run(EvoXApplication.class, args);
    }
    
    @Override
    public void run(String... args) {
        // 创建聊天机器人 Agent
        ChatBotAgent chatBotAgent = new ChatBotAgent(llm);
        chatBotAgent.setName("ChatBot");
        chatBotAgent.setDescription("一个智能聊天机器人");
        chatBotAgent.initModule();
        
        // 直接使用 Agent 进行对话
        List<Message> messages = Arrays.asList(
            Message.builder()
                .content("你好，请介绍一下你自己")
                .messageType(MessageType.INPUT)
                .build()
        );
        
        Message response = chatBotAgent.execute("chat", messages);
        System.out.println("AI: " + response.getContent());
    }
}
```

**注意**: `ChatBotAgent` 需要手动创建并传入 `LLMProvider` 实例，Starter 不会自动配置 Agent Bean。

## 完整配置项说明

### evox.llm.* - LLM 配置

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| evox.llm.provider | String | openai | LLM 提供商：openai, deepseek, ollama, aliyun, siliconflow |
| evox.llm.api-key | String | - | API Key（必填，也可通过环境变量提供） |
| evox.llm.model | String | gpt-4o-mini | 模型名称 |
| evox.llm.temperature | Float | 0.7 | 温度参数（0-2） |
| evox.llm.max-tokens | Integer | 2000 | 最大 token 数 |
| evox.llm.timeout | Long | 30000 | 超时时间（毫秒） |

**注意**: 当前 Starter **不支持**通过 YAML 配置 `base-url` 和 `retry.*` 等高级选项。如需自定义这些参数，请通过 `@Bean` 手动创建 LLM 实例。

### evox.agents.* - Agent 配置

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| evox.agents.default-timeout | Long | 60000 | 默认超时时间（毫秒） |
| evox.agents.max-concurrent | Integer | 10 | 最大并发数 |

### evox.memory.* - 记忆配置

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| evox.memory.short-term.capacity | Integer | 100 | 短期记忆容量 |
| evox.memory.short-term.window-size | Integer | 10 | 上下文窗口大小 |
| evox.memory.long-term.enabled | Boolean | false | 是否启用长期记忆 |
| evox.memory.long-term.storage-type | String | in-memory | 存储类型：in-memory, redis, database |

### evox.tools.* - 工具配置

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| evox.tools.enabled | Boolean | true | 是否启用工具调用 |
| evox.tools.timeout | Long | 30000 | 工具调用超时（毫秒） |
| evox.tools.max-retries | Integer | 3 | 工具调用最大重试次数 |

**注意**: 当前版本的 Starter **不支持** `evox.storage.*` 和 `evox.workflow.*` 配置项。这些功能需要通过代码手动配置。

## 配置优先级

配置加载遵循以下优先级（从高到低）：

1. **代码中定义的 Bean**（最高优先级）
2. **application.yml / application.properties**
3. **环境变量**
4. **默认值**（最低优先级）

## 自定义配置覆盖

如果需要自定义某些组件，可以通过 `@Configuration` 和 `@Bean` 覆盖默认配置：

```java
@Configuration
public class CustomEvoXConfig {
    
    @Bean
    @Primary
    public LLMProvider customLLM() {
        OpenAILLMConfig config = OpenAILLMConfig.builder()
            .apiKey("custom-api-key")
            .model("gpt-4")
            .temperature(0.9f)
            .build();
        return new OpenAILLM(config);
    }
    
    @Bean
    @Primary
    public ShortTermMemory customMemory() {
        return new ShortTermMemory(200);
    }
}
```

**注意**: 
- `ShortTermMemory` 构造函数只接受 `capacity` 参数
- 需要使用具体的 LLM 配置类（如 `OpenAILLMConfig`），而非抽象的 `LLMConfig`

## 禁用自动配置

如果不需要某些自动配置功能，可以通过以下方式禁用：

### 方式一：通过配置禁用

```yaml
evox:
  enabled: false
```

### 方式二：排除自动配置类

```java
@SpringBootApplication(exclude = {
    EvoXAutoConfiguration.class
})
public class Application {
    // ...
}
```

## 多 LLM 实例

如果需要同时使用多个 LLM 实例，可以定义多个 Bean 并使用 `@Qualifier` 区分：

```java
@Configuration
public class MultiLLMConfig {
    
    @Bean(name = "openaiLLM")
    public LLMProvider openaiLLM() {
        OpenAILLMConfig config = OpenAILLMConfig.builder()
            .apiKey(System.getenv("OPENAI_API_KEY"))
            .model("gpt-4o")
            .build();
        return new OpenAILLM(config);
    }
    
    @Bean(name = "deepseekLLM")
    public LLMProvider deepseekLLM() {
        DeepSeekLLMConfig config = DeepSeekLLMConfig.builder()
            .apiKey(System.getenv("DEEPSEEK_API_KEY"))
            .model("deepseek-chat")
            .build();
        return new DeepSeekLLM(config);
    }
}
```

使用时指定具体的 Bean：

```java
@Service
public class MultiLLMService {
    
    @Autowired
    @Qualifier("openaiLLM")
    private LLMProvider openaiLLM;
    
    @Autowired
    @Qualifier("deepseekLLM")
    private LLMProvider deepseekLLM;
}
```

## 故障排查 FAQ

### Q1: 启动时报错 "No qualifying bean of type 'BaseLLM'"

**原因**：未配置 LLM 或 API Key 缺失

**解决**：确保在 `application.yml` 中配置了 `evox.llm.api-key`，或通过环境变量提供

### Q2: 调用 LLM 时超时

**原因**：网络问题或超时配置过短

**解决**：增加超时时间配置
```yaml
evox:
  llm:
    timeout: 60000
```

### Q3: 自定义 Bean 未生效

**原因**：缺少 `@Primary` 注解或 Bean 名称冲突

**解决**：确保自定义 Bean 添加了 `@Primary` 注解，或使用 `@Qualifier` 明确指定

### Q4: 如何查看当前使用的配置？

**解决**：启用 DEBUG 日志
```yaml
logging:
  level:
    io.leavesfly.evox: DEBUG
```

### Q5: 支持哪些 LLM Provider？

**支持列表**：
- `openai` - OpenAI GPT 系列
- `deepseek` - DeepSeek 模型
- `ollama` - 本地 Ollama 服务
- `aliyun` - 阿里云通义千问
- `siliconflow` - SiliconFlow 平台

详细配置请参考 [LLM Providers](./LLM-Providers.md)
