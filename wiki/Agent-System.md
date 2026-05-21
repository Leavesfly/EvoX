# 智能体系统 (evox-agents)

## 模块定位

**业务层模块**，提供多种智能体（Agent）实现，用于构建复杂的 AI 应用。

### 依赖关系
- **依赖**: `evox-core`, `evox-models`, `evox-tools`
- **被依赖**: 上层应用模块（如 evox-application）

### Maven 坐标
```xml
<dependency>
    <groupId>com.alibaba.evox</groupId>
    <artifactId>evox-agents</artifactId>
    <version>${evox.version}</version>
</dependency>
```

## Agent 类型

| Agent 类型 | 说明 | 适用场景 |
|-----------|------|---------|
| **ActionAgent** | 函数执行智能体 | 直接调用预定义的工具/函数 |
| **ReActAgent** | 推理-行动智能体 | 需要多步推理和工具调用的复杂任务 |
| **CustomizeAgent** | 自定义智能体 | 用户自定义行为逻辑 |
| **TaskPlannerAgent** | 任务规划智能体 | 将复杂目标分解为可执行子任务 |
| **RouterAgent** | 路由智能体 | 根据输入内容路由到不同的处理流程 |
| **ToolAwareAgent** | 工具感知智能体 | 动态感知并选择合适的工具 |
| **ChatBotAgent** | 聊天智能体 | 对话式交互场景 |

## Agent 基类结构与生命周期

### 核心基类

所有 Agent 继承自 `BaseAgent`，提供统一的生命周期管理：

```java
public abstract class BaseAgent {
    // 初始化阶段
    public void initialize() { ... }
    
    // 执行阶段 - 返回 Reactor Mono
    public Mono<AgentResponse> execute(AgentRequest request) { ... }
    
    // 清理阶段
    public void cleanup() { ... }
}
```

### 生命周期

1. **Initialization**: 加载配置、初始化工具集、建立记忆存储
2. **Execution**: 接收请求 → 处理逻辑 → 调用工具（可选）→ 生成响应
3. **Cleanup**: 释放资源、保存状态

## 带记忆的对话示例

使用 `ShortTermMemory` 与 `ChatBotAgent` 实现有状态的对话：

```java
// 创建短期记忆
ShortTermMemory memory = new ShortTermMemory(10); // 保留最近10轮对话

// 创建带记忆的 ChatBotAgent
ChatBotAgent agent = ChatBotAgent.builder()
    .model(chatModel)
    .memory(memory)
    .systemPrompt("你是一个专业的技术助手")
    .build();

// 第一轮对话
AgentResponse response1 = agent.execute(
    AgentRequest.of("什么是 Reactor?")
).block();

// 第二轮对话 - 能记住上下文
AgentResponse response2 = agent.execute(
    AgentRequest.of("它和 RxJava 有什么区别?")
).block();
```

## ToolAwareAgent 工具集成示例

`ToolAwareAgent` 能够自动感知可用工具并选择合适的工具执行：

```java
// 定义工具
@Tool(name = "search_knowledge", description = "搜索知识库")
public String searchKnowledge(String query) { ... }

@Tool(name = "calculate", description = "执行数学计算")
public double calculate(String expression) { ... }

// 创建 ToolAwareAgent
ToolAwareAgent agent = ToolAwareAgent.builder()
    .model(chatModel)
    .tools(searchKnowledge, calculate)
    .maxToolCalls(5)
    .build();

// 执行 - Agent 会自动选择合适工具
AgentResponse response = agent.execute(
    AgentRequest.of("帮我查一下 EvoX 的架构，然后计算 2^10")
).block();
```

## TaskPlannerAgent 任务规划示例

将复杂目标分解为多个子任务依次执行：

```java
TaskPlannerAgent planner = TaskPlannerAgent.builder()
    .model(chatModel)
    .executorAgent(executorAgent)  // 用于执行子任务的 Agent
    .maxSubTasks(10)
    .build();

AgentResponse response = planner.execute(
    AgentRequest.of("分析这个 Java 项目的代码质量，给出改进建议")
).block();

// 内部流程：
// 1. 规划子任务: [扫描代码结构, 检查复杂度, 分析依赖, 生成报告]
// 2. 依次执行每个子任务
// 3. 汇总结果返回
```

## AgentManager 管理器

`AgentManager` 用于管理和协调多个 Agent：

```java
AgentManager manager = new AgentManager();

// 注册 Agent
manager.register("chatbot", chatBotAgent);
manager.register("planner", taskPlannerAgent);
manager.register("router", routerAgent);

// 获取并使用 Agent
Agent agent = manager.getAgent("chatbot");
AgentResponse response = agent.execute(request).block();

// 批量执行
Map<String, AgentResponse> results = manager.executeAll(request).block();
```

## 自定义 Agent 扩展指南

继承 `BaseAgent` 创建自定义 Agent：

```java
public class MyCustomAgent extends BaseAgent {
    
    private final ChatModel model;
    private final List<Tool> tools;
    
    public MyCustomAgent(ChatModel model, List<Tool> tools) {
        this.model = model;
        this.tools = tools;
    }
    
    @Override
    public Mono<AgentResponse> execute(AgentRequest request) {
        return Mono.fromCallable(() -> {
            // 1. 预处理请求
            String processedInput = preprocess(request.getInput());
            
            // 2. 调用模型
            String result = model.chat(processedInput);
            
            // 3. 后处理结果
            return postprocess(result);
        });
    }
    
    private String preprocess(String input) { ... }
    private AgentResponse postprocess(String result) { ... }
}
```

## 异步执行（Reactor Mono）

所有 Agent 的 `execute` 方法返回 `Mono<AgentResponse>`，支持响应式编程：

```java
// 单个 Agent 异步执行
agent.execute(request)
    .subscribe(response -> System.out.println(response));

// 多个 Agent 并行执行
Mono.zip(
    agent1.execute(request1),
    agent2.execute(request2),
    agent3.execute(request3)
).subscribe(results -> { ... });

// 链式调用
agent.execute(request)
    .map(response -> transform(response))
    .flatMap(transformed -> anotherAgent.execute(transformed))
    .subscribe(finalResponse -> { ... });
```

## 包结构

```
com.alibaba.evox.agents/
├── base/                  # 基础类
│   ├── BaseAgent.java
│   ├── AgentRequest.java
│   ├── AgentResponse.java
│   └── AgentConfig.java
├── action/                # ActionAgent
│   └── ActionAgent.java
├── react/                 # ReActAgent
│   └── ReActAgent.java
├── customize/             # CustomizeAgent
│   └── CustomizeAgent.java
├── plan/                  # TaskPlannerAgent
│   └── TaskPlannerAgent.java
├── specialized/           # 专用 Agent
│   ├── RouterAgent.java
│   ├── ToolAwareAgent.java
│   └── ChatBotAgent.java
├── manager/               # Agent 管理器
│   └── AgentManager.java
└── frameworks/            # 框架集成
    └── SpringAgentFactory.java
```

## 最佳实践

1. **选择合适的 Agent 类型**: 简单对话用 `ChatBotAgent`，复杂任务用 `ReActAgent` 或 `TaskPlannerAgent`
2. **合理使用记忆**: 需要上下文时使用 `ShortTermMemory`，注意设置合适的窗口大小
3. **工具设计**: 工具应有清晰的名称和描述，便于 Agent 正确选择
4. **错误处理**: 使用 `onErrorResume` 处理 Agent 执行异常
5. **资源管理**: 长时间运行的 Agent 应定期调用 `cleanup()` 释放资源
