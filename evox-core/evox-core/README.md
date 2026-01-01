# EvoX Core 核心抽象模块

## 📦 模块定位

**层级**: 核心层 (Core Layer)  
**职责**: 提供框架最底层的抽象和基础设施  
**依赖**: 无(仅依赖 Spring Boot 和基础库)

## 🎯 核心功能

evox-core 是整个 EvoX 框架的基石，提供了所有模块共同依赖的核心抽象、消息系统、注册机制和容错能力。

### 1. 核心抽象

#### BaseModule
所有模块的基类，提供统一的序列化和生命周期管理能力：

- **序列化支持**: `toJson()`, `toPrettyJson()`, `toDict()`
- **反序列化支持**: `fromJson()`, `fromDict()`
- **文件持久化**: `saveModule()`, `loadModule()`
- **深拷贝**: `copy()`
- **初始化钩子**: `initModule()`

**使用示例**:
```java
// 自定义模块
public class MyModule extends BaseModule {
    private String name;
    private Integer value;
    
    @Override
    public void initModule() {
        // 自定义初始化逻辑
    }
}

// 序列化
MyModule module = new MyModule();
String json = module.toJson();

// 反序列化
MyModule loaded = BaseModule.fromJson(json, MyModule.class);

// 持久化
module.saveModule(Paths.get("module.json"));
```

#### IAgent
智能体核心接口，用于打破模块间的循环依赖：

- `getAgentId()`: 获取智能体唯一标识
- `getName()`: 获取智能体名称
- `execute()`: 同步执行指定动作
- `executeAsync()`: 异步执行指定动作
- `isHuman()`: 判断是否为人类用户

#### IAgentManager
智能体管理接口，提供智能体的注册、查询和管理：

- `addAgent()`: 添加智能体
- `getAgent()`: 根据名称获取智能体
- `removeAgent()`: 移除智能体
- `getAllAgents()`: 获取所有智能体

### 2. 消息系统

#### Message
统一的消息模型，支持智能体间的通信：

**核心字段**:
- `content`: 消息内容
- `messageType`: 消息类型 (INPUT/OUTPUT/RESPONSE/ERROR/SYSTEM)
- `agent`: 发送或接收的 Agent 名称
- `action`: 关联的 Action 名称
- `workflowGoal`: 工作流目标
- `workflowTask`: 工作流任务
- `timestamp`: 时间戳

**使用示例**:
```java
Message message = Message.builder()
    .content("用户输入内容")
    .messageType(MessageType.INPUT)
    .agent("ChatAgent")
    .action("chat")
    .workflowGoal("完成对话任务")
    .build();
```

### 3. 模块注册与配置

#### ModuleRegistry
模块注册表，管理所有模块的注册和查询。

#### EvoXCoreConfig
核心模块配置类，自动注册核心 Bean。

### 4. 容错机制

#### CircuitBreaker (熔断器)
保护系统不被失败调用压垮：

**核心特性**:
- **三种状态**: CLOSED(正常)、OPEN(熔断)、HALF_OPEN(尝试恢复)
- **自动状态转换**: 失败达到阈值自动熔断，超时后自动尝试恢复
- **快速失败**: 熔断状态下快速返回失败

**使用示例**:
```java
CircuitBreaker breaker = CircuitBreaker.defaultBreaker("myService");

// 执行操作
try {
    String result = breaker.execute(() -> {
        // 可能失败的操作
        return callExternalService();
    });
} catch (ExecutionException e) {
    // 处理熔断或执行失败
}

// 查询状态
CircuitBreaker.State state = breaker.getState();
```

**配置选项**:
- `failureThreshold`: 失败阈值(默认 5 次)
- `timeout`: 超时时间(默认 30 秒)
- `resetTimeout`: 重置超时(默认 1 分钟)

#### RetryPolicy (重试策略)
智能重试机制，支持指数退避和抖动：

**核心特性**:
- **指数退避**: 延迟时间按倍数增长
- **抖动(Jitter)**: 避免重试风暴
- **最大延迟限制**: 防止无限等待
- **可重试异常判断**: 灵活控制哪些异常可重试

**预设策略**:
- `defaultPolicy()`: 默认策略(3 次重试，100ms 初始延迟)
- `fastRetry()`: 快速重试(3 次，50ms 初始延迟)
- `robustRetry()`: 稳健重试(5 次，500ms 初始延迟)

**使用示例**:
```java
// 使用默认策略
RetryPolicy policy = RetryPolicy.defaultPolicy();
RetryExecutor executor = new RetryExecutor(policy);

String result = executor.execute(() -> {
    // 可能失败的操作
    return callUnstableService();
});

// 自定义策略
RetryPolicy customPolicy = RetryPolicy.builder()
    .maxAttempts(5)
    .initialDelay(Duration.ofMillis(200))
    .backoffMultiplier(1.5)
    .maxDelay(Duration.ofSeconds(10))
    .retryableException(e -> e instanceof IOException)
    .build();
```

### 5. 异常体系

#### EvoXException
框架统一异常基类：

**核心特性**:
- **错误码**: 支持自定义错误码
- **上下文信息**: 携带错误上下文对象
- **结构化信息**: 便于日志记录和问题排查

**使用示例**:
```java
throw new EvoXException("AGENT_ERROR", "Agent执行失败", context);
```

**派生异常类**:
- `ModuleException`: 模块相关异常
- `ExecutionException`: 执行相关异常
- 其他业务异常...

## 📂 目录结构

```
evox-core/
├── agent/              # Agent 接口定义
│   ├── IAgent.java
│   └── IAgentManager.java
├── circuitbreaker/     # 熔断器
│   └── CircuitBreaker.java
├── config/             # 配置类
│   └── EvoXCoreConfig.java
├── exception/          # 异常体系
│   ├── EvoXException.java
│   ├── ModuleException.java
│   └── ExecutionException.java
├── message/            # 消息系统
│   ├── Message.java
│   └── MessageType.java
├── module/             # 模块抽象
│   └── BaseModule.java
├── registry/           # 注册机制
│   └── ModuleRegistry.java
└── retry/              # 重试机制
    ├── RetryPolicy.java
    └── RetryExecutor.java
```

## 🚀 快速开始

### Maven 依赖

```xml
<dependency>
    <groupId>io.leavesfly.evox</groupId>
    <artifactId>evox-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 基本用法

```java
// 1. 创建自定义模块
public class MyModule extends BaseModule {
    private String name;
    
    @Override
    public void initModule() {
        log.info("模块初始化: {}", name);
    }
}

// 2. 构建消息
Message message = Message.builder()
    .content("Hello EvoX")
    .messageType(MessageType.INPUT)
    .build();

// 3. 使用熔断器
CircuitBreaker breaker = CircuitBreaker.defaultBreaker("service");
String result = breaker.execute(() -> externalCall());

// 4. 使用重试
RetryExecutor retry = new RetryExecutor(RetryPolicy.defaultPolicy());
String data = retry.execute(() -> fetchData());
```

## 🎓 设计原则

- **最小依赖**: 仅依赖必要的基础库，保持轻量
- **稳定接口**: 提供稳定的 API 供上层模块使用
- **高内聚低耦合**: 职责清晰，模块独立
- **容错设计**: 内置容错机制，提升系统可靠性

## 📊 适用场景

- 作为所有 EvoX 模块的基础依赖
- 构建自定义模块和扩展
- 实现分布式调用的容错保护
- 统一消息通信协议

## 🔗 相关模块

- **evox-models**: 基于 evox-core 实现 LLM 模型适配
- **evox-actions**: 基于 evox-core 实现动作执行引擎
- **所有上层模块**: 都依赖 evox-core 提供的基础能力
