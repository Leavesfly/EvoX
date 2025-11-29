# EvoX Spring Boot Starter

EvoX 框架的 Spring Boot Starter，提供自动配置功能，让你零配置快速开始使用 EvoX。

## ✨ 特性

- 🚀 **零配置启动**：引入依赖即可使用
- ⚙️ **自动装配**：自动创建 LLM、Memory、Toolkit 等核心组件
- 📝 **YAML 配置**：通过 `application.yml` 轻松配置
- 🔧 **灵活扩展**：支持自定义 Bean 覆盖默认配置

## 📦 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>io.leavesfly</groupId>
    <artifactId>evox-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. 配置 application.yml

```yaml
evox:
  llm:
    provider: openai
    api-key: ${OPENAI_API_KEY}
    model: gpt-4o-mini
```

### 3. 使用自动注入

```java
@SpringBootApplication
public class MyApplication {
    
    @Autowired
    private BaseLLM llm;  // 自动注入
    
    @Autowired
    private ShortTermMemory memory;  // 自动注入
    
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
    
    @Bean
    public CommandLineRunner demo() {
        return args -> {
            // 直接使用注入的组件
            ChatBotAgent agent = new ChatBotAgent(llm);
            agent.setMemory(memory);
            agent.initModule();
            
            // 开始聊天
            Message msg = Message.builder()
                .content("你好！")
                .messageType(MessageType.INPUT)
                .build();
            
            Message response = agent.execute("chat", List.of(msg));
            System.out.println("AI: " + response.getContent());
        };
    }
}
```

## ⚙️ 配置项

### 完整配置示例

```yaml
evox:
  # 是否启用自动配置
  enabled: true
  
  # LLM 配置
  llm:
    provider: openai          # 提供商: openai, dashscope, litellm
    api-key: ${OPENAI_API_KEY}
    model: gpt-4o-mini
    temperature: 0.7
    max-tokens: 2000
    timeout: 30000
  
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
      enabled: false
      storage-type: in-memory
  
  # Tools 配置
  tools:
    enabled: true
    timeout: 30000
    max-retries: 3
```

### 配置优先级

1. **代码中的 Bean 定义**（最高优先级）
2. **application.yml 配置**
3. **环境变量**
4. **默认值**（最低优先级）

## 🔧 自定义配置

### 覆盖默认 Bean

```java
@Configuration
public class MyEvoXConfig {
    
    @Bean
    @Primary  // 覆盖自动配置的 Bean
    public BaseLLM customLLM() {
        return new OpenAILLM(customConfig);
    }
    
    @Bean
    public ShortTermMemory customMemory() {
        return new ShortTermMemory(500);  // 自定义容量
    }
}
```

### 禁用自动配置

```yaml
evox:
  enabled: false  # 禁用所有自动配置
```

或者在启动类上排除：

```java
@SpringBootApplication(exclude = {EvoXAutoConfiguration.class})
public class MyApplication {
    // ...
}
```

## 📝 注意事项

1. **API Key 配置**：
   - 优先从 `evox.llm.api-key` 读取
   - 如果未配置，会尝试从环境变量 `OPENAI_API_KEY` 读取
   - 如果都未配置，LLM Bean 将不会创建

2. **依赖版本**：
   - 需要 Spring Boot 3.2+
   - 需要 JDK 17+

3. **性能优化**：
   - Memory 容量建议根据实际需求调整
   - Agent 并发数建议根据服务器性能配置

## 🐛 故障排查

### Q: 启动时没有看到 EvoX 欢迎信息？

A: 检查是否配置了 `evox.enabled=true` 或确保没有排除自动配置。

### Q: LLM Bean 注入失败？

A: 确保配置了 API Key：
```bash
export OPENAI_API_KEY=sk-your-key
```

### Q: 想要使用多个 LLM 实例？

A: 自定义 Configuration 并使用 `@Qualifier`：
```java
@Bean("openai")
public BaseLLM openAILLM() { ... }

@Bean("claude")
public BaseLLM claudeLLM() { ... }

// 使用时
@Autowired
@Qualifier("openai")
private BaseLLM openai;
```

## 📚 更多信息

- [EvoX 主项目](../../README.md)
- [快速开始指南](../../doc/QUICKSTART.md)
- [完整示例](../../evox-application/evox-examples)

---

**Happy Coding!** 🎉
