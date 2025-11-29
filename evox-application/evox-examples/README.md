# EvoX 示例应用

本模块包含 EvoX 框架的示例应用,展示框架的核心功能和使用方法。

## 📋 目录

- [快速开始](#快速开始)
- [示例说明](#示例说明)
  - [SimpleChatBot](#simplechatbot)
  - [ComprehensiveChatBot](#comprehensivechatbot)
- [配置说明](#配置说明)
- [常见问题](#常见问题)

## 🚀 快速开始

### 前置要求

- Java 17 或更高版本
- Maven 3.6+
- OpenAI API Key (用于 LLM 调用)

### 安装依赖

首先编译整个 EvoX 项目:

```bash
# 进入项目根目录
cd evox

# 编译并安装所有模块
mvn clean install -DskipTests
```

### 运行示例

#### 1. SimpleChatBot - 基础聊天机器人

最简单的示例,展示 Agent + Memory + Tools 的基本集成。

```bash
# 运行 SimpleChatBot
mvn exec:java -pl evox-examples \
  -Dexec.mainClass="io.leavesfly.evox.examples.SimpleChatBot" \
  -Dexec.args="YOUR_OPENAI_API_KEY"
```

**功能特性:**
- ✅ 单个智能体 (QuestionAgent)
- ✅ 短期记忆 (保持对话历史)
- ✅ 工具集成 (文件、HTTP、搜索)
- ✅ 简单易懂的代码结构

#### 2. ComprehensiveChatBot - 综合聊天机器人

更完整的示例,展示多 Agent 协同工作。

```bash
# 运行 ComprehensiveChatBot
mvn exec:java -pl evox-examples \
  -Dexec.mainClass="io.leavesfly.evox.examples.ComprehensiveChatBot" \
  -Dexec.args="YOUR_OPENAI_API_KEY"
```

**功能特性:**
- ✅ 多智能体协同 (RouterAgent + ToolAgent + ChatAgent)
- ✅ 智能路由 (自动选择合适的 Agent)
- ✅ 记忆管理 (短期记忆 + 滑动窗口)
- ✅ 完整的对话流程

## 📚 示例说明

### SimpleChatBot

**代码文件:** `src/main/java/io/leavesfly/evox/examples/SimpleChatBot.java`

**架构设计:**

```
用户输入 → QuestionAgent → Tools(可选) → LLM生成 → 响应输出
                ↓
            ShortTermMemory
```

**核心组件:**

1. **QuestionAgent**: 自定义 Agent,继承自 `Agent` 类
   - 添加了 `ToolAwareAction` 处理工具调用
   
2. **ShortTermMemory**: 短期记忆管理
   - 容量限制: 20 条消息
   - 滑动窗口: 保留最近 10 条
   
3. **Toolkit**: 工具集管理
   - FileSystemTool: 文件读写
   - HttpTool: HTTP 请求
   - WebSearchTool: 网络搜索

**关键代码片段:**

```java
// 创建自定义 Agent
static class QuestionAgent extends Agent {
    public QuestionAgent(Toolkit toolkit) {
        super();
        // 添加工具感知的 Action
        addAction(new ToolAwareAction(toolkit));
    }
}

// 初始化 Agent 和工具
Toolkit toolkit = new Toolkit();
toolkit.addTool(new FileSystemTool());
toolkit.addTool(new HttpTool());
toolkit.addTool(new WebSearchTool());

QuestionAgent agent = new QuestionAgent(toolkit);
```

### ComprehensiveChatBot

**代码文件:** `src/main/java/io/leavesfly/evox/examples/ComprehensiveChatBot.java`

**架构设计:**

```
用户输入 → RouterAgent (决策) → ToolAgent (工具处理)
                ↓                      ↓
           ChatAgent (普通聊天)  ←  AgentManager
                ↓
          ShortTermMemory
                ↓
            响应输出
```

**核心组件:**

1. **RouterAgent**: 路由智能体
   - 分析用户输入
   - 决定使用哪个 Agent 处理
   
2. **ToolAgent**: 工具处理智能体
   - 处理需要工具的请求
   - 调用文件、搜索等工具
   
3. **ChatAgent**: 聊天智能体
   - 处理普通对话
   - 调用 LLM 生成回复

4. **AgentManager**: 智能体管理器
   - 注册和管理所有 Agent
   - 提供 Agent 查找功能

**关键代码片段:**

```java
// 创建路由 Agent
static class RouterAgent extends Agent {
    public RouterAgent() {
        super();
        addAction(new RouteAction());
    }
}

// 路由逻辑
static class RouteAction extends Action {
    @Override
    public ActionOutput execute(ActionInput input) {
        String userInput = getUserInput(input);
        
        // 判断是否需要工具
        boolean needTool = userInput.contains("搜索") || 
                         userInput.contains("读取") || 
                         userInput.contains("文件");
        
        String selectedAgent = needTool ? "ToolAgent" : "ChatAgent";
        return SimpleActionOutput.success("选择: " + selectedAgent);
    }
}

// 执行对话流程
Message routeResult = routerAgent.execute("route", messages);
String selectedAgent = extractSelectedAgent(routeResult);

Agent agent = agentManager.getAgent(selectedAgent);
Message response = agent.execute("process", messages);
```

## ⚙️ 配置说明

### OpenAI 配置

示例使用 OpenAI 的 GPT 模型,需要提供 API Key:

```java
OpenAILLMConfig llmConfig = OpenAILLMConfig.builder()
    .apiKey(args[0])  // 从命令行参数获取
    .modelName("gpt-4o-mini")  // 模型名称
    .temperature(0.7)  // 温度参数
    .maxTokens(1000)   // 最大 token 数
    .build();

OpenAILLM llm = new OpenAILLM(llmConfig);
```

### 记忆配置

短期记忆配置示例:

```java
ShortTermMemory memory = ShortTermMemory.builder()
    .capacity(20)        // 最大容量 20 条消息
    .windowSize(10)      // 滑动窗口大小 10
    .build();
```

### 工具配置

工具集配置示例:

```java
Toolkit toolkit = new Toolkit();

// 添加文件工具
toolkit.addTool(new FileSystemTool());

// 添加 HTTP 工具
toolkit.addTool(new HttpTool());

// 添加搜索工具
toolkit.addTool(new WebSearchTool());
```

## 📖 使用场景

### 场景 1: 简单问答

```bash
> 用户: 你好,请介绍一下自己
> 机器人: 你好!我是一个基于 EvoX 框架的智能聊天助手...
```

### 场景 2: 工具调用

```bash
> 用户: 请搜索最新的 AI 新闻
> 机器人: [调用 WebSearchTool]
> 机器人: 根据搜索结果,最新的 AI 新闻包括...
```

### 场景 3: 文件操作

```bash
> 用户: 读取 README.md 文件
> 机器人: [调用 FileSystemTool]
> 机器人: 文件内容如下: ...
```

### 场景 4: 上下文记忆

```bash
> 用户: 我叫张三
> 机器人: 很高兴认识你,张三!
> 用户: 我叫什么名字?
> 机器人: [从记忆中检索] 你叫张三!
```

## 🔧 常见问题

### Q1: 编译失败怎么办?

**问题:** 执行 `mvn clean install` 时报错

**解决方案:**
```bash
# 检查 Java 版本
java -version  # 应该是 17 或更高

# 检查 Maven 版本
mvn -version   # 应该是 3.6+

# 清理后重新编译
mvn clean install -U -DskipTests
```

### Q2: 运行时找不到类?

**问题:** `ClassNotFoundException` 或 `NoClassDefFoundError`

**解决方案:**
```bash
# 确保先安装了所有依赖模块
cd evox
mvn clean install -DskipTests

# 然后再运行示例
mvn exec:java -pl evox-examples -Dexec.mainClass="..."
```

### Q3: OpenAI API 调用失败?

**问题:** 提示 API Key 无效或网络错误

**解决方案:**
1. 检查 API Key 是否正确
2. 检查网络连接
3. 检查 OpenAI 服务状态
4. 尝试设置代理:
```bash
export HTTP_PROXY=http://your-proxy:port
export HTTPS_PROXY=http://your-proxy:port
```

### Q4: 记忆没有保存?

**问题:** 对话历史没有保留

**解决方案:**
确保在每次对话后调用记忆的保存方法:
```java
// 保存用户消息
memory.addMessage(userMessage);

// 保存 AI 响应
memory.addMessage(aiResponse);
```

### Q5: 工具调用没有生效?

**问题:** Agent 没有使用工具

**解决方案:**
1. 确保工具已添加到 Toolkit
2. 确保 Action 中实现了工具调用逻辑
3. 检查用户输入是否触发了工具条件

## 📝 开发建议

### 1. 创建自定义 Agent

继承 `Agent` 类并添加自定义 Action:

```java
public class MyCustomAgent extends Agent {
    public MyCustomAgent() {
        super();
        addAction(new MyCustomAction());
    }
}

public class MyCustomAction extends Action {
    @Override
    public ActionOutput execute(ActionInput input) {
        // 实现自定义逻辑
        return SimpleActionOutput.success("结果");
    }
}
```

### 2. 扩展工具集

实现 `BaseTool` 接口:

```java
public class MyCustomTool implements BaseTool {
    @Override
    public String getName() {
        return "MyTool";
    }
    
    @Override
    public String getDescription() {
        return "我的自定义工具";
    }
    
    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        // 实现工具逻辑
        return ToolResult.success("结果");
    }
}
```

### 3. 使用工作流编排

参考 Workflow API 创建复杂的多步骤流程:

```java
Workflow workflow = new Workflow();
workflow.setName("MyWorkflow");

WorkflowNode node1 = new WorkflowNode();
node1.setName("Step1");
node1.setNodeType(WorkflowNode.NodeType.ACTION);

WorkflowNode node2 = new WorkflowNode();
node2.setName("Step2");
node2.setNodeType(WorkflowNode.NodeType.CONDITION);

workflow.addNode(node1);
workflow.addNode(node2);
workflow.addEdge(node1, node2);
```

## 🔗 相关资源

- [EvoX 文档](../README.md)
- [API 文档](../docs/API.md)
- [架构设计](../docs/ARCHITECTURE.md)
- [开发指南](../docs/DEVELOPMENT.md)

## 📄 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](../LICENSE) 文件。

## 🤝 贡献

欢迎贡献代码、报告问题或提出建议!

1. Fork 本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 📧 联系方式

如有问题或建议,请通过以下方式联系:

- 提交 Issue: [GitHub Issues](https://github.com/your-repo/evox/issues)
- 邮件: your-email@example.com

---

**Happy Coding! 🎉**
