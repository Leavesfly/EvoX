# EvoX Core 核心抽象模块

**层级**: 核心层 (Core Layer)  
**职责**: 提供框架最底层的抽象和基础设施，所有模块的零依赖基础  
**对外依赖**: 仅依赖 Spring Boot 3.2+、Project Reactor、Jackson

evox-core 是整个 EvoX 框架的基石，定义了所有模块共同依赖的核心接口、消息协议、动作抽象与配置体系。它本身不包含任何业务逻辑，只提供稳定的 SPI 接口与基础设施。

## 核心功能

### 1. 模块基类 — BaseModule

所有 EvoX 模块的基类（`io.leavesfly.evox.core.module`），提供统一的序列化与生命周期能力：

| 方法 | 说明 |
|------|------|
| `toJson()` / `toPrettyJson()` | 序列化为 JSON 字符串 |
| `toDict()` | 序列化为 `Map<String, Object>` |
| `fromJson(json, Class)` | 从 JSON 字符串反序列化（静态方法）|
| `fromDict(dict, Class)` | 从 Map 反序列化（静态方法）|
| `saveModule(Path)` | 持久化到文件 |
| `loadModule(Path, Class)` | 从文件加载（静态方法）|
| `copy()` | 创建深拷贝 |
| `initModule()` | 初始化钩子，子类可覆写 |

```java
public class MyAgent extends BaseModule {
    private String name;

    @Override
    public void initModule() {
        log.info("初始化 Agent: {}", name);
    }
}

// 序列化 / 持久化
MyAgent agent = new MyAgent();
String json = agent.toJson();
agent.saveModule(Paths.get("agent.json"));

// 反序列化 / 加载
MyAgent loaded = BaseModule.fromJson(json, MyAgent.class);
MyAgent restored = BaseModule.loadModule(Paths.get("agent.json"), MyAgent.class);
```

### 2. 智能体接口 — IAgent / IAgentManager

定义于 `io.leavesfly.evox.core.agent`，用于打破 evox-core 与 evox-agents 之间的循环依赖：

**IAgent** — 智能体的核心 SPI：

```java
public interface IAgent {
    String getAgentId();             // 唯一标识
    String getName();                // 名称
    String getDescription();         // 描述

    // 同步执行
    Message execute(String actionName, List<Message> messages);
    default Message execute(List<Message> messages);  // 使用默认动作

    // 异步执行（基于 Project Reactor）
    Mono<Message> executeAsync(String actionName, List<Message> messages);
    default Mono<Message> executeAsync(List<Message> messages);
    default Mono<Message> callAsync(String input);    // 字符串快捷方法

    boolean isHuman();               // 是否为人类用户
}
```

**IAgentManager** — 智能体注册与查询：

- `addAgent(IAgent)` — 注册智能体
- `getAgent(String name)` — 按名称查询
- `removeAgent(String name)` — 移除智能体
- `getAllAgents()` — 获取全部已注册智能体

### 3. 消息系统 — Message / MessageType

定义于 `io.leavesfly.evox.core.message`，是智能体间通信的统一载体：

**核心字段**：

| 字段 | 类型 | 说明 |
|------|------|------|
| `messageId` | `String` | 自动生成的 UUID |
| `content` | `Object` | 消息内容（支持任意类型）|
| `messageType` | `MessageType` | 消息类型 |
| `agent` | `String` | 发送 Agent 名称 |
| `action` | `String` | 关联 Action 名称 |
| `prompt` | `String` | 关联的提示词 |
| `timestamp` | `Instant` | 自动记录时间戳 |
| `metadata` | `Map<String, Object>` | 扩展字段（工作流语义等）|

**MessageType 枚举**：`INPUT` / `OUTPUT` / `RESPONSE` / `ERROR` / `SYSTEM` / `UNKNOWN`

**工作流扩展字段**（通过 metadata 承载，保持 Message 职责单一）：
- `workflowGoal` — 工作流目标
- `workflowTask` — 当前工作流任务
- `workflowTaskDesc` — 任务描述
- `nextActions` — 下一步动作列表

**静态工厂方法**：

```java
// 常用快捷方法
Message msg = Message.inputMessage("用户输入");
Message msg = Message.outputMessage("AI 响应");
Message msg = Message.systemMessage("系统提示");
Message msg = Message.errorMessage("错误信息");
Message msg = Message.responseMessage(content, "MyAgent", "chat");

// Builder 完整构建
Message msg = Message.builder()
    .content("内容")
    .messageType(MessageType.INPUT)
    .agent("MyAgent")
    .workflowGoal("完成对话任务")     // metadata 便捷方法
    .workflowTask("执行第一步")
    .build();
```

### 4. LLM 接口体系 — ILLM 族

定义于 `io.leavesfly.evox.core.llm`，采用分级接口设计，按需依赖：

```
ILLMSync          — 同步调用：generate(String) / chat(List<Message>) / getModelName()
  └─ ILLMAsync    — 异步调用：generateAsync / chatAsync（返回 Mono）
      └─ ILLM     — 流式调用：generateStream / chatStream（返回 Flux）

ILLMToolUse       — 工具调用能力（独立接口）：chatWithTools
LLMConfig         — 配置基类：apiKey / model / temperature / maxTokens / timeout
```

**使用建议**：
- 只需文本生成 → 依赖 `ILLMSync`
- 需要异步调用 → 依赖 `ILLMAsync`  
- 需要流式输出 → 依赖 `ILLM`
- 需要 Function Calling → 同时依赖 `ILLMToolUse`

### 5. 动作系统 — Action / ActionInput / ActionOutput

定义于 `io.leavesfly.evox.actions.base`，是智能体执行的最小单元：

```java
// 自定义 Action
public class MyAction extends Action {

    @Override
    public ActionOutput execute(ActionInput input) {
        String prompt = input.get("prompt");
        String result = llm.generate(prompt);
        return SimpleActionOutput.of(result);
    }

    @Override
    public String[] getInputFields() { return new String[]{"prompt"}; }

    @Override
    public String[] getOutputFields() { return new String[]{"result"}; }
}
```

- `Action` — 抽象基类，持有 `ILLM` 实例，支持同步 `execute` 和异步 `executeAsync`
- `ActionInput` — 输入容器，Map 结构，键值对传参
- `ActionOutput` — 输出接口，支持结构化结果
- `SimpleActionOutput` — 纯文本输出的默认实现

### 6. 提示词管理 — PromptTemplate / PromptConstants

定义于 `io.leavesfly.evox.prompt`：

- **PromptTemplate** — 提示词模板引擎，支持变量占位符替换
- **PromptConstants** — 内置常量提示词（系统提示、角色设定、任务描述等）

### 7. 评估接口 — IEvaluator

定义于 `io.leavesfly.evox.core.evaluation`，供 `evox-optimizers` 依赖，具体实现在 `evox-evaluation`：

```java
public interface IEvaluator {
    EvaluationResult evaluate(Object prediction, Object label);          // 单样本评估
    EvaluationResult evaluateBatch(Object[] predictions, Object[] labels); // 批量评估
    EvaluationResult evaluateWorkflow(Function<Map,String> workflow,      // 工作流评估
                                      List<Map.Entry<Map,Object>> dataset); // P = T(W, D)
    Mono<EvaluationResult> evaluateAsync(...);                           // 异步版本
}
```

### 8. 注册机制 — IModuleRegistry

定义于 `io.leavesfly.evox.core.registry`，提供模块注册与查找的标准 SPI。

### 9. 异常体系

定义于 `io.leavesfly.evox.core.exception`：

| 异常类 | 说明 |
|--------|------|
| `EvoXException` | 框架统一异常基类，携带错误码和上下文 |
| `ModuleException` | 模块初始化、注册相关异常 |
| `ExecutionException` | 动作/智能体执行期间的异常 |
| `ConfigurationException` | 配置缺失或格式错误 |
| `ValidationException` | 输入参数校验失败 |

### 10. 配置属性 — EvoXProperties

定义于 `io.leavesfly.evox.core.config`，Spring Boot `@ConfigurationProperties` 绑定 `evox.*`：

```yaml
evox:
  llm:
    provider: openai        # LLM 提供商
    temperature: 0.7
    max-tokens: 2000
    timeout: 30000          # ms
    retry:
      max-attempts: 3
      initial-delay: 1000
      max-delay: 10000
  agents:
    default-timeout: 60000  # ms
    max-concurrent: 10
  memory:
    short-term:
      capacity: 100
      window-size: 10
    long-term:
      enabled: true
      storage-type: in-memory  # in-memory | redis | database
  storage:
    type: in-memory            # in-memory | h2 | postgresql
    vector:
      enabled: false
      provider: in-memory      # in-memory | qdrant | milvus
      dimension: 1536
  workflow:
    max-depth: 10
    timeout: 300000
    enable-parallel: true
  tools:
    enabled: true
    timeout: 30000
    max-retries: 3
```

### 11. 工具类 — utils

定义于 `io.leavesfly.evox.utils`：

- **CommonUtils** — 通用工具方法（字符串处理、集合操作等）
- **SanitizeUtils** — 输入清洗工具，防止 prompt 注入

## 包结构

```
io.leavesfly.evox
│
├── core/
│   ├── agent/                 # 智能体 SPI
│   │   ├── IAgent.java
│   │   └── IAgentManager.java
│   ├── config/                # Spring Boot 配置
│   │   ├── EvoXCoreConfig.java
│   │   └── EvoXProperties.java
│   ├── evaluation/            # 评估器 SPI
│   │   ├── IEvaluator.java
│   │   └── EvaluationResult.java
│   ├── exception/             # 异常体系
│   │   ├── EvoXException.java
│   │   ├── ModuleException.java
│   │   ├── ExecutionException.java
│   │   ├── ConfigurationException.java
│   │   └── ValidationException.java
│   ├── llm/                   # LLM 接口族
│   │   ├── ILLM.java
│   │   ├── ILLMSync.java
│   │   ├── ILLMAsync.java
│   │   ├── ILLMStream.java
│   │   ├── ILLMToolUse.java
│   │   └── LLMConfig.java
│   ├── message/               # 消息系统
│   │   ├── Message.java
│   │   └── MessageType.java
│   ├── module/                # 模块基类
│   │   └── BaseModule.java
│   └── registry/              # 注册 SPI
│       └── IModuleRegistry.java
│
├── actions/
│   └── base/                  # 动作基础抽象
│       ├── Action.java
│       ├── ActionInput.java
│       ├── ActionOutput.java
│       └── SimpleActionOutput.java
│
├── prompt/                    # 提示词管理
│   ├── PromptTemplate.java
│   └── PromptConstants.java
│
└── utils/                     # 工具类
    ├── CommonUtils.java
    └── SanitizeUtils.java
```

## 快速上手

### Maven 依赖

```xml
<dependency>
    <groupId>io.leavesfly.evox</groupId>
    <artifactId>evox-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 常用示例

```java
// 1. 创建消息
Message input = Message.inputMessage("请帮我分析这段代码");
Message sys   = Message.systemMessage("你是一个代码分析专家");

// 2. 自定义模块
public class MyModule extends BaseModule {
    private String name;
    @Override public void initModule() { log.info("初始化: {}", name); }
}
MyModule m = new MyModule();
m.saveModule(Paths.get("module.json"));
MyModule restored = BaseModule.loadModule(Paths.get("module.json"), MyModule.class);

// 3. 自定义 Action
public class SummarizeAction extends Action {
    @Override
    public ActionOutput execute(ActionInput input) {
        String text = (String) input.get("text");
        return SimpleActionOutput.of(llm.generate("总结：" + text));
    }
    @Override public String[] getInputFields()  { return new String[]{"text"}; }
    @Override public String[] getOutputFields() { return new String[]{"summary"}; }
}

// 4. 使用提示词模板
String prompt = PromptTemplate.of("你是 {{role}}，请回答：{{question}}")
    .bind("role", "Java 专家")
    .bind("question", "什么是虚拟线程？")
    .render();
```

## 设计原则

| 原则 | 说明 |
|------|------|
| **最小依赖** | 仅依赖 Spring Boot 基础库，不引入任何业务框架 |
| **稳定接口** | SPI 接口遵循向后兼容，上层模块不受影响 |
| **职责单一** | 每个包只负责一个关注点，无交叉引用 |
| **依赖倒置** | 上层依赖此处的抽象，而非具体实现 |
| **响应式友好** | 所有异步接口基于 Reactor `Mono`/`Flux` |

## 相关模块

| 模块 | 与 evox-core 的关系 |
|------|---------------------|
| **evox-models** | 实现 `ILLM` / `ILLMToolUse` 接口，提供具体模型适配 |
| **evox-mcp** | 依赖 evox-core 消息协议，定义 MCP 协议层 |
| **evox-agents** | 实现 `IAgent` 接口，提供智能体运行时 |
| **evox-workflow** | 基于 `IAgent` 接口编排工作流，无需感知具体实现 |
| **evox-evaluation** | 实现 `IEvaluator` 接口，提供评估算法 |
| **所有上层模块** | 均以 evox-core 为最终依赖基础 |
