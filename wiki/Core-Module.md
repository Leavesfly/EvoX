# 核心抽象层 (evox-core)

## 模块定位

**零依赖基础，所有模块的基石**

`evox-core` 是 EvoX 框架的核心抽象层，提供所有上层模块（Agents、Workflow、MCP、Memory 等）共享的基础接口和抽象类。该模块不依赖任何外部库，确保最大程度的可移植性和稳定性。

---

## 核心组件

### 1. BaseModule 基类

所有模块的基类，提供统一的序列化、持久化和初始化能力。

**核心方法：**

- `toJson()` - 将模块序列化为 JSON 字符串
- `fromJson(String json)` - 从 JSON 字符串反序列化模块
- `saveModule(String path)` - 保存模块到指定路径
- `loadModule(String path)` - 从指定路径加载模块
- `initModule()` - 初始化模块（子类可重写）
- `copy()` - 深拷贝模块实例

---

### 2. Agent 接口体系

#### IAgent 接口

定义智能体的基本执行契约：

```java
public interface IAgent {
    Message execute(Message input);           // 同步执行
    CompletableFuture<Message> executeAsync(Message input);  // 异步执行
    CompletableFuture<Message> callAsync(Message input);     // 异步调用
    boolean isHuman();                        // 是否为人类智能体
}
```

#### IAgentManager 接口

管理多个 Agent 的生命周期和调度：

```java
public interface IAgentManager {
    void registerAgent(IAgent agent);
    IAgent getAgent(String agentId);
    List<IAgent> listAgents();
    void removeAgent(String agentId);
}
```

---

### 3. Message 消息系统

统一的消息传递模型，支持多类型消息流转。

**Message 字段：**

| 字段 | 类型 | 说明 |
|------|------|------|
| messageId | String | 消息唯一标识 |
| content | String | 消息内容 |
| messageType | MessageType | 消息类型 |
| agent | String | 发送/接收代理 ID |
| action | String | 关联的动作名称 |
| prompt | String | 提示词上下文 |
| timestamp | Long | 时间戳 |
| metadata | Map<String, Object> | 扩展元数据 |

**MessageType 枚举：**

- `INPUT` - 用户输入消息
- `OUTPUT` - 模型输出消息
- `RESPONSE` - 响应消息
- `ERROR` - 错误消息
- `SYSTEM` - 系统内部消息

---

### 4. LLM 接口层级

三层抽象设计，支持同步、异步和工具调用场景。

```
ILLMSync (同步接口)
    ↓ 继承
ILLMAsync (异步接口)
    ↓ 继承
ILLM (完整接口)
    ↓ 扩展
ILLMToolUse (工具调用接口)
```

**LLMConfig 配置类：**

```java
public class LLMConfig {
    private String apiKey;
    private String baseUrl;
    private String model;
    private Double temperature;
    private Integer maxTokens;
    private Map<String, Object> extraParams;
}
```

---

### 5. Action 动作系统

标准化的动作输入输出封装。

**核心类：**

- `Action` - 动作定义（名称、描述、参数 schema）
- `ActionInput` - 动作输入封装
- `ActionOutput` - 动作输出基类
- `SimpleActionOutput` - 简单文本输出实现

---

### 6. PromptTemplate 提示词管理

支持变量替换的提示词模板引擎：

```java
PromptTemplate template = new PromptTemplate("你好，{{name}}！");
String result = template.render(Map.of("name", "World"));
// 输出：你好，World！
```

---

### 7. IEvaluator 评估接口

用于评估 Agent 执行结果的标准化接口：

```java
public interface IEvaluator {
    EvaluationResult evaluate(Message input, Message output);
}
```

---

### 8. 异常体系

分层异常设计，便于精确错误处理：

| 异常类 | 说明 |
|--------|------|
| `EvoXException` | 顶层异常基类 |
| `ModuleException` | 模块级别异常 |
| `ExecutionException` | 执行过程异常 |
| `ConfigurationException` | 配置错误异常 |
| `ValidationException` | 参数校验异常 |

---

## 包结构

```
io.leavesfly.evox.core
├── module          # BaseModule 及相关
├── agent           # IAgent / IAgentManager
├── message         # Message / MessageType
├── llm             # ILLM 接口层级 / LLMConfig
├── action          # Action 系统
├── prompt          # PromptTemplate
├── evaluator       # IEvaluator
└── exception       # 异常体系
```

---

## Maven 依赖

```xml
<dependency>
    <groupId>io.leavesfly.evox</groupId>
    <artifactId>evox-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```
