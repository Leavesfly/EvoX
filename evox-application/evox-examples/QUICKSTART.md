# 🚀 5 分钟快速开始

这是 EvoX 框架最简单的入门指南。

## 📋 前置要求

- ✅ JDK 17+
- ✅ Maven 3.8+
- ✅ OpenAI API Key

## 🎯 快速开始

### 1. 设置 API Key

```bash
export OPENAI_API_KEY="sk-your-actual-api-key-here"
```

### 2. 运行极简示例

```bash
cd evox-application/evox-examples
mvn exec:java -Dexec.mainClass="io.leavesfly.evox.examples.QuickStart"
```

### 3. 查看输出

```
用户: 你好！请用一句话介绍你自己。
AI: 你好！我是一个基于 EvoX 框架的智能助手。
```

---

## 📝 示例代码解析

### 极简示例 (QuickStart.java) - 4 步完成

```java
// 第 1 步: 配置 OpenAI
OpenAILLMConfig config = OpenAILLMConfig.builder()
    .apiKey(System.getenv("OPENAI_API_KEY"))
    .model("gpt-4o-mini")
    .build();

// 第 2 步: 创建聊天机器人
ChatBotAgent agent = new ChatBotAgent(new OpenAILLM(config));
agent.setName("QuickBot");
agent.initModule();

// 第 3 步: 发送消息
Message userMsg = Message.builder()
    .content("你好！")
    .messageType(MessageType.INPUT)
    .build();

Message response = agent.execute("chat", Collections.singletonList(userMsg));

// 第 4 步: 获取回复
System.out.println("AI: " + response.getContent());
```

---

## 📚 更多示例

### 1. 带记忆的对话 (MemoryAgentExample.java)

```bash
mvn exec:java -Dexec.mainClass="io.leavesfly.evox.examples.MemoryAgentExample"
```

展示如何让 AI 记住对话历史。

### 2. 工具集成 (ToolsExample.java)

```bash
mvn exec:java -Dexec.mainClass="io.leavesfly.evox.examples.ToolsExample"
```

展示如何让 AI 调用工具（文件、计算器等）。

### 3. 工作流编排 (SequentialWorkflowExample.java)

```bash
mvn exec:java -Dexec.mainClass="io.leavesfly.evox.examples.SequentialWorkflowExample"
```

展示如何组织多步骤的 AI 任务。

### 4. 完整聊天机器人 (SimpleChatBot.java)

```bash
mvn exec:java -Dexec.mainClass="io.leavesfly.evox.examples.SimpleChatBot"
```

包含完整功能的聊天机器人（记忆、错误处理等）。

---

## ❓ 常见问题

### Q: 没有 API Key 怎么办？

A: 访问 [OpenAI 官网](https://platform.openai.com/) 注册并获取 API Key。

### Q: 编译失败？

A: 先在项目根目录执行：
```bash
mvn clean install -DskipTests
```

### Q: 网络连接失败？

A: 可能需要配置代理：
```bash
export HTTP_PROXY=http://your-proxy:port
export HTTPS_PROXY=http://your-proxy:port
```

---

## 🎓 下一步学习

1. 📖 阅读 [完整文档](../../README.md)
2. 🏗️ 了解 [架构设计](../../doc/ARCHITECTURE.md)
3. 💡 查看 [更多示例](README.md)
4. 🔧 尝试 [自定义 Agent](CustomizeAgentExample.java)

---

**祝你使用愉快！** 🎉

如有问题，欢迎提交 [Issue](https://github.com/your-org/evox/issues)
