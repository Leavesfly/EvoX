# 快速开始

本指南将帮助你快速上手 EvoX 框架，完成环境配置并运行第一个 AI Agent。

## 环境要求

在开始之前，请确保你的开发环境满足以下要求：

- **JDK**: 17 或更高版本
- **Maven**: 3.8 或更高版本
- **API Key**: 至少一个大模型提供商的 API Key（如 OpenAI、Anthropic、阿里云等）

### 验证环境

```bash
# 检查 Java 版本
java -version

# 检查 Maven 版本
mvn -version
```

## 安装步骤

### 1. 克隆仓库

```bash
git clone https://github.com/leavesfly/evox.git
cd evox
```

### 2. 编译项目

```bash
mvn clean install -DskipTests
```

编译完成后，所有模块将被安装到本地 Maven 仓库。

## API Key 配置

EvoX 支持多种方式配置大模型的 API Key。

### 方式一：环境变量（推荐）

```bash
# OpenAI
export OPENAI_API_KEY="sk-your-api-key"

# Anthropic
export ANTHROPIC_API_KEY="sk-ant-your-api-key"

# 阿里云通义千问
export DASHSCOPE_API_KEY="sk-your-api-key"
```

### 方式二：application.yml

在 Spring Boot 项目中，可以在 `application.yml` 中配置：

```yaml
evox:
  llm:
    openai:
      api-key: ${OPENAI_API_KEY}
      base-url: https://api.openai.com/v1
```

### 方式三：代码中直接设置

```java
OpenAILLM llm = OpenAILLM.builder()
    .apiKey("sk-your-api-key")
    .modelName("gpt-4")
    .build();
```

## 第一个示例：ChatBotAgent

下面是一个完整的 ChatBotAgent 示例，展示如何使用 EvoX 创建简单的对话机器人。

### Maven 依赖

在你的 `pom.xml` 中添加以下依赖：

```xml
<dependencies>
    <!-- EvoX 核心 -->
    <dependency>
        <groupId>io.leavesfly.evox</groupId>
        <artifactId>evox-core</artifactId>
        <version>0.1.0</version>
    </dependency>
    
    <!-- OpenAI 模型适配器 -->
    <dependency>
        <groupId>io.leavesfly.evox</groupId>
        <artifactId>evox-models</artifactId>
        <version>0.1.0</version>
    </dependency>
    
    <!-- Agent 实现 -->
    <dependency>
        <groupId>io.leavesfly.evox</groupId>
        <artifactId>evox-agents</artifactId>
        <version>0.1.0</version>
    </dependency>
</dependencies>
```

### 完整代码

```java
package com.example.demo;

import io.leavesfly.evox.agent.ChatBotAgent;
import io.leavesfly.evox.llm.LLM;
import io.leavesfly.evox.llm.openai.OpenAILLM;
import io.leavesfly.evox.memory.Memory;
import io.leavesfly.evox.memory.ConversationMemory;
import reactor.core.publisher.Mono;

public class ChatBotExample {
    
    public static void main(String[] args) {
        // 1. 创建 LLM 实例
        LLM llm = OpenAILLM.builder()
            .apiKey(System.getenv("OPENAI_API_KEY"))
            .modelName("gpt-4")
            .build();
        
        // 2. 创建记忆组件
        Memory memory = new ConversationMemory();
        
        // 3. 创建 ChatBotAgent
        ChatBotAgent agent = ChatBotAgent.builder()
            .name("Assistant")
            .llm(llm)
            .memory(memory)
            .systemPrompt("你是一个友好的AI助手，乐于帮助用户解答问题。")
            .build();
        
        // 4. 发送消息并获取响应
        String userMessage = "你好，请介绍一下你自己";
        
        Mono<String> response = agent.chat(userMessage);
        
        // 5. 订阅并打印结果
        response.subscribe(
            result -> System.out.println("AI: " + result),
            error -> System.err.println("Error: " + error.getMessage())
        );
        
        // 等待异步操作完成
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

### 代码说明

1. **LLM 实例**：使用 `OpenAILLM` 创建大语言模型实例，通过环境变量获取 API Key
2. **Memory 组件**：使用 `ConversationMemory` 管理对话历史，支持多轮对话上下文
3. **ChatBotAgent**：构建对话机器人，设置名称、LLM、记忆和系统提示词
4. **响应式调用**：`agent.chat()` 返回 `Mono<String>`，采用响应式编程模型
5. **异步处理**：通过 `subscribe` 处理异步响应，注意实际应用中应使用更优雅的异步处理方式

## 运行方式

### 方式一：使用 Maven Exec Plugin

在 `pom.xml` 中配置 exec-maven-plugin：

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.codehaus.mojo</groupId>
            <artifactId>exec-maven-plugin</artifactId>
            <version>3.1.0</version>
            <configuration>
                <mainClass>com.example.demo.ChatBotExample</mainClass>
            </configuration>
        </plugin>
    </plugins>
</build>
```

然后运行：

```bash
mvn exec:java
```

### 方式二：直接运行 Java 程序

```bash
# 编译
mvn compile

# 运行
java -cp target/classes:$(mvn dependency:build-classpath -q -DincludeScope=runtime -Dmdep.outputFile=/dev/stdout) com.example.demo.ChatBotExample
```

### 方式三：IDE 中运行

在 IntelliJ IDEA 或 Eclipse 中直接运行 `ChatBotExample` 类的 `main` 方法。

## 常见问题排查

### 1. 编译错误：找不到符号

**问题**：`cannot find symbol: class ChatBotAgent`

**解决方案**：
- 确认已正确添加 Maven 依赖
- 执行 `mvn clean install` 重新编译项目
- 检查 groupId 和 artifactId 是否正确：`io.leavesfly.evox`

### 2. 运行时错误：API Key 未配置

**问题**：`IllegalArgumentException: API key must not be null or empty`

**解决方案**：
- 确认已设置环境变量：`export OPENAI_API_KEY="your-key"`
- 或在代码中直接传入 API Key
- 验证 API Key 是否有效（可在对应平台控制台测试）

### 3. 网络连接超时

**问题**：`ConnectTimeoutException` 或 `ReadTimeoutException`

**解决方案**：
- 检查网络连接是否正常
- 确认是否可以访问大模型 API 端点（如 `https://api.openai.com`）
- 如需要代理，配置 JVM 代理参数：
  ```bash
  java -Dhttp.proxyHost=proxy.example.com -Dhttp.proxyPort=8080 ...
  ```

### 4. 响应式流未执行

**问题**：程序立即退出，没有看到 AI 响应

**解决方案**：
- 响应式流需要订阅才会执行，确保调用了 `subscribe()`
- 主线程不能过早退出，可以使用 `CountDownLatch` 或 `block()` 等待：
  ```java
  // 阻塞等待结果（仅用于测试，生产环境建议使用异步方式）
  String result = agent.chat(message).block();
  System.out.println("AI: " + result);
  ```

### 5. Maven 依赖解析失败

**问题**：`Could not resolve dependencies for project`

**解决方案**：
- 确认已执行 `mvn clean install -DskipTests` 安装了 EvoX 到本地仓库
- 检查 `~/.m2/repository/io/leavesfly/evox` 目录是否存在对应的 jar 包
- 清除 Maven 缓存后重试：`rm -rf ~/.m2/repository/io/leavesfly/evox`

### 6. JDK 版本不兼容

**问题**：`UnsupportedClassVersionError` 或编译错误

**解决方案**：
- 确认使用 JDK 17 或更高版本
- 检查 `JAVA_HOME` 环境变量指向正确的 JDK 路径
- 在 IDE 中确认项目 SDK 设置为 Java 17+

## 下一步

完成快速开始后，你可以：

1. 查看 [架构总览](Architecture-Overview.md) 了解 EvoX 的整体设计
2. 浏览 `evox-examples` 模块获取更多示例代码
3. 探索 [Core Module](Core-Module.md) 深入了解核心 API
4. 尝试使用 ReActAgent 实现具备工具调用能力的智能 Agent

## 获取帮助

- 查阅完整文档：[EvoX Wiki](../wiki/Home.md)
- 提交 Issue：[GitHub Issues](https://github.com/leavesfly/evox/issues)
- 参与讨论：[GitHub Discussions](https://github.com/leavesfly/evox/discussions)
