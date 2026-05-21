# 用户快速开始指南

本文是面向新用户的渐进式引导，从零开始，5 分钟内完成第一个 AI 应用。

---

## 📋 环境准备

| 依赖 | 最低版本 | 验证命令 |
|------|---------|---------|
| JDK | 17+ | `java -version` |
| Maven | 3.8+ | `mvn -version` |
| API Key | — | 任选一个 LLM 提供商 |

## 🔧 第一步：获取项目

```bash
git clone https://github.com/your-org/evox.git
cd evox
mvn clean install -DskipTests
```

## 🔑 第二步：配置 API Key

选择以下任一方式：

**方式 A：环境变量（推荐）**

```bash
# OpenAI
export OPENAI_API_KEY="sk-your-key"

# 或者阿里云通义千问
export DASHSCOPE_API_KEY="your-key"

# 或者 DeepSeek
export DEEPSEEK_API_KEY="your-key"
```

**方式 B：application.yml**

```yaml
evox:
  llm:
    provider: openai
    api-key: sk-your-key
    model: gpt-4o-mini
```

---

## 🎯 第三步：运行第一个示例

```bash
cd evox-application/evox-examples
mvn exec:java -Dexec.mainClass="io.leavesfly.evox.examples.QuickStart"
```

**预期输出**：
```
用户: 你好！请用一句话介绍你自己。
AI: 你好！我是一个基于 EvoX 框架的智能助手，可以帮助你解答问题、处理任务。
```

---

## 📝 代码解析：4 步创建聊天机器人

```java
import io.leavesfly.evox.agents.specialized.ChatBotAgent;
import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.core.message.MessageType;
import io.leavesfly.evox.models.provider.openai.OpenAILLM;
import io.leavesfly.evox.models.provider.openai.OpenAILLMConfig;

import java.util.Collections;

public class QuickStart {
    public static void main(String[] args) {
        // 1️⃣ 配置 LLM
        OpenAILLMConfig config = OpenAILLMConfig.builder()
            .apiKey(System.getenv("OPENAI_API_KEY"))
            .model("gpt-4o-mini")
            .build();

        // 2️⃣ 创建聊天机器人
        ChatBotAgent agent = new ChatBotAgent(new OpenAILLM(config));
        agent.setName("QuickBot");
        agent.initModule();

        // 3️⃣ 发送消息
        Message userMsg = Message.builder()
            .content("你好！请用一句话介绍你自己。")
            .messageType(MessageType.INPUT)
            .build();

        Message response = agent.execute("chat", Collections.singletonList(userMsg));

        // 4️⃣ 获取回复
        System.out.println("AI: " + response.getContent());
    }
}
```

---

## 🚶 渐进式学习路线

### Level 1：带记忆的多轮对话

让 AI 记住之前的对话内容：

```java
import io.leavesfly.evox.memory.ShortTermMemory;

// 创建短期记忆（容量 100 条消息）
ShortTermMemory memory = new ShortTermMemory(100);

// 创建带记忆的聊天机器人
ChatBotAgent agent = new ChatBotAgent(new OpenAILLM(config));
agent.setMemory(memory);
agent.initModule();

// 第一轮对话
agent.execute("chat", List.of(Message.inputMessage("我叫张三")));

// 第二轮对话 — AI 能记住你的名字
Message reply = agent.execute("chat", List.of(Message.inputMessage("我叫什么名字？")));
System.out.println("AI: " + reply.getContent());
// 输出: AI: 你叫张三
```

运行示例：
```bash
mvn exec:java -Dexec.mainClass="io.leavesfly.evox.examples.MemoryAgentExample"
```

---

### Level 2：让 AI 使用工具

赋予 AI 操作外部世界的能力：

```java
import io.leavesfly.evox.agents.specialized.ToolAwareAgent;
import io.leavesfly.evox.tools.impl.*;

// 创建工具列表
List<BaseTool> tools = List.of(
    new FileSystemTool(),
    new CalculatorTool(),
    new ShellTool()
);

// 创建工具感知智能体
ToolAwareAgent agent = new ToolAwareAgent(llm, tools);
agent.initModule();

// AI 会自动判断并调用合适的工具
Message response = agent.execute(List.of(
    Message.inputMessage("帮我计算 (12345 * 67890) + 42")
));
System.out.println("AI: " + response.getContent());
```

运行示例：
```bash
mvn exec:java -Dexec.mainClass="io.leavesfly.evox.examples.ToolsExample"
```

---

### Level 3：工作流编排

将多个步骤串联成自动化流程：

```java
import io.leavesfly.evox.workflow.builder.WorkflowBuilder;
import io.leavesfly.evox.workflow.core.Workflow;

// 构建顺序工作流
Workflow workflow = WorkflowBuilder.sequential()
    .name("文章生成流程")
    .goal("生成一篇技术文章")
    .step("提纲生成", outlineAgent)
    .step("内容撰写", writerAgent)
    .step("润色修改", editorAgent)
    .maxSteps(100)
    .build();

// 执行工作流
Message input = Message.inputMessage("请写一篇关于微服务架构的文章");
Message result = workflow.execute(List.of(input));
System.out.println(result.getContent());
```

运行示例：
```bash
mvn exec:java -Dexec.mainClass="io.leavesfly.evox.examples.SequentialWorkflowExample"
```

---

### Level 4：切换不同的 LLM

EvoX 支持 8 大模型提供商，切换只需替换配置：

```java
import io.leavesfly.evox.models.config.LLMFactory;
import io.leavesfly.evox.models.spi.LLMProvider;

// OpenAI
LLMProvider llm = LLMFactory.openai("sk-xxx", "gpt-4o");

// DeepSeek（性价比之选）
LLMProvider llm = LLMFactory.deepseek("sk-xxx");

// 阿里云通义千问
LLMProvider llm = LLMFactory.aliyun("sk-xxx", "qwen-max");

// Ollama 本地部署（免费、离线可用）
LLMProvider llm = LLMFactory.ollama("llama3");

// 使用环境变量（自动读取对应的 API Key）
LLMProvider llm = LLMFactory.openai();
```

> **提示**：Agent 使用方式完全相同，只需替换 LLM 实例即可。

---

### Level 5：在 Spring Boot 项目中集成

**添加依赖**：
```xml
<dependency>
    <groupId>io.leavesfly.evox</groupId>
    <artifactId>evox-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

**配置 application.yml**：
```yaml
evox:
  llm:
    provider: openai
    api-key: ${OPENAI_API_KEY}
    model: gpt-4o-mini
```

**直接注入使用**：
```java
@RestController
public class ChatController {

    @Autowired
    private LLMProvider llm;

    @PostMapping("/chat")
    public String chat(@RequestBody String question) {
        return llm.generate(question);
    }
}
```

---

## 📦 Maven 依赖选择指南

根据需求选择引入的模块：

| 需求 | 依赖 artifactId | 说明 |
|------|----------------|------|
| 只用 LLM 调用 | `evox-models` | 最小依赖，支持 8 大提供商 |
| 需要 Agent 能力 | `evox-agents` | 包含多种智能体类型 |
| 需要工作流 | `evox-workflow` | DAG 编排引擎 |
| 需要 RAG | `evox-rag` | 文档检索增强生成 |
| 需要工具集成 | `evox-tools` | 20+ 内置工具 |
| Spring Boot 项目 | `evox-spring-boot-starter` | 开箱即用 |
| 全部功能 | `evox-spring-boot-starter` | 传递依赖包含所有模块 |

```xml
<!-- 示例：引入 Agent 能力 -->
<dependency>
    <groupId>io.leavesfly.evox</groupId>
    <artifactId>evox-agents</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

---

## ❓ 常见问题

### 编译失败？

确保在项目根目录执行过完整编译：
```bash
mvn clean install -DskipTests
```

### API 调用超时？

可能需要配置网络代理：
```bash
export HTTP_PROXY=http://your-proxy:port
export HTTPS_PROXY=http://your-proxy:port
```

### 想使用本地模型（免费）？

安装 [Ollama](https://ollama.ai/) 后即可使用：
```bash
# 安装并启动模型
ollama run llama3

# 代码中使用
LLMProvider llm = LLMFactory.ollama("llama3");
```

### 如何查看更多示例？

```bash
ls evox-application/evox-examples/src/main/java/io/leavesfly/evox/examples/
```

---

## 🗺️ 下一步

| 方向 | 推荐阅读 |
|------|---------|
| 了解整体架构 | [架构总览](Architecture-Overview.md) |
| 深入 Agent 系统 | [智能体系统](Agent-System.md) |
| 学习工作流编排 | [工作流引擎](Workflow-Engine.md) |
| 使用 RAG 做知识问答 | [RAG 模块](RAG-Module.md) |
| 多 Agent 协同 | [多智能体协作](Multi-Agent-Collaboration.md) |
| 完整配置参考 | [配置参考](Configuration-Reference.md) |
