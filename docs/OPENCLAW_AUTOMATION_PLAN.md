# EvoX OpenClaw 自动化机制补齐方案

## 一、背景

[OpenClaw](https://docs.openclaw.ai/) 是一个自托管的多渠道 AI Agent 网关，将 WhatsApp、Telegram、Discord、iMessage 等聊天应用连接到 AI Agent。其自动化机制是核心差异化能力之一。

本文档基于 OpenClaw 官方文档，分析其自动化机制的设计，并制定 evox-openclaw 的补齐方案。

---

## 二、OpenClaw 自动化机制全景

```
┌─────────────────────────────────────────────────────────────────┐
│                     OpenClaw 自动化体系                          │
│                                                                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐  │
│  │  Heartbeat   │  │  Cron Jobs   │  │  Hooks (事件钩子)     │  │
│  │  定期心跳     │  │  定时任务     │  │  外部事件驱动         │  │
│  └──────┬───────┘  └──────┬───────┘  └──────────┬───────────┘  │
│         │                 │                      │              │
│         ▼                 ▼                      ▼              │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │              Session Manager (会话管理)                  │   │
│  │  ┌─────────────────┐    ┌──────────────────────────┐   │   │
│  │  │  Main Session   │    │  Isolated Session        │   │   │
│  │  │  共享上下文       │    │  cron:<jobId> 独立会话    │   │   │
│  │  └────────┬────────┘    └────────────┬─────────────┘   │   │
│  └───────────┼──────────────────────────┼─────────────────┘   │
│              │                          │                      │
│              ▼                          ▼                      │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │              Agent Execution (Agent 执行)                │   │
│  └──────────────────────────┬──────────────────────────────┘   │
│                             │                                   │
│                             ▼                                   │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │              Delivery (结果投递)                          │   │
│  │  announce → 指定渠道推送 │ none → 仅内部记录              │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

### 2.1 Heartbeat（心跳机制）

**定位**：Agent 的"闹钟"，定期唤醒 Agent 处理积压事件。

| 属性 | 说明 |
|------|------|
| 触发方式 | 每 N 分钟自动触发 |
| 会话模式 | 仅 Main Session |
| 执行逻辑 | 读取 `HEARTBEAT.md` 文件并执行 |
| 成本 | 单轮对话，随 HEARTBEAT.md 大小线性增长 |
| 适用场景 | 持续监控、定期检查、处理 Main Session Cron 事件 |

### 2.2 Cron Jobs（定时任务）

**定位**：Gateway 内置调度器，支持一次性和循环任务。

#### 三种调度模式

| 模式 | 说明 | 示例 |
|------|------|------|
| `at` | 一次性定时（ISO 8601） | `"2026-02-01T16:00:00Z"` |
| `every` | 固定间隔循环（毫秒） | `3600000`（每小时） |
| `cron` | 标准 Cron 表达式 + 时区 | `"0 7 * * *"` + `"Asia/Shanghai"` |

#### 两种执行模式

| 模式 | 会话 | Payload | 特点 |
|------|------|---------|------|
| Main Session | 主会话 | `systemEvent` | 共享上下文，通过 Heartbeat 处理 |
| Isolated Session | `cron:<jobId>` | `agentTurn` | 独立干净会话，支持模型/思考级别覆盖 |

#### 核心特性

- **持久化**：任务存储在 `~/.openclaw/cron/jobs.json`，重启不丢失
- **运行历史**：`~/.openclaw/cron/runs/<jobId>.jsonl`
- **失败重试**：指数退避（30s → 1m → 5m → 15m → 60m），成功后重置
- **一次性任务**：`at` 类型默认执行后自动删除
- **Announce 投递**：Isolated 任务结果可推送到指定渠道
- **Agent 绑定**：可指定特定 Agent 执行
- **模型覆盖**：Isolated 任务可覆盖模型和思考级别

### 2.3 Hooks（事件钩子）

**定位**：接收外部系统事件，触发 Agent 执行。

#### 核心配置模型

```json
{
  "hooks": {
    "enabled": true,
    "token": "OPENCLAW_HOOK_TOKEN",
    "path": "/hooks",
    "presets": ["gmail"],
    "mappings": [
      {
        "match": { "path": "/custom-event" },
        "action": "agent",
        "wakeMode": "now",
        "sessionKey": "hook:{{headers.x-source}}",
        "messageTemplate": "收到事件: {{body.event}}",
        "deliver": true,
        "channel": "telegram"
      }
    ]
  }
}
```

#### 内置 Hook 预设

| 预设 | 说明 |
|------|------|
| Gmail PubSub | 监听 Gmail 新邮件，自动触发 Agent 处理 |
| Webhooks | 接收外部 HTTP 回调 |
| Polls | 定期轮询外部数据源 |
| Auth Monitoring | 监控认证状态变化 |

#### 关键设计

- **映射配置**：`match` 匹配规则 → `action` 执行动作
- **模板化消息**：Mustache 语法，支持 `{{body.*}}`、`{{headers.*}}` 等变量
- **安全边界**：默认对外部内容进行 `external-content` 安全包装
- **会话隔离**：通过 `sessionKey` 模板实现按来源隔离
- **模型优先级**：Hook 映射 > Hook 预设 > Agent 默认 > 全局默认

---

## 三、evox 现有能力分析

### 3.1 evox-scheduler 现有能力

| 能力 | 状态 | 说明 |
|------|------|------|
| Cron 触发器 | ✅ | `CronTrigger`（6 字段标准 Cron） |
| 间隔触发器 | ✅ | `IntervalTrigger`（Duration） |
| 一次性触发器 | ✅ | `OnceTrigger`（immediate / at） |
| 事件触发器 | ✅ | `EventTrigger` + `EventBus` |
| Agent 任务 | ✅ | `AgentTask` 调用 `IAgent.execute()` |
| 任务状态管理 | ✅ | 7 种状态 + 暂停/恢复/取消 |
| 持久化 | ❌ | 仅内存存储 |
| 失败重试 | ❌ | 无重试逻辑 |
| Main/Isolated 会话 | ❌ | 无会话隔离概念 |
| Heartbeat 心跳 | ❌ | 无定期唤醒机制 |
| Announce 投递 | ❌ | `IPushService` 仅定义接口 |
| 任务超时 | ❌ | 无超时控制 |

### 3.2 evox-channels 现有能力

| 能力 | 状态 | 说明 |
|------|------|------|
| 渠道抽象 | ✅ | `IChannel` / `AbstractChannel` |
| 渠道注册中心 | ✅ | `ChannelRegistry` |
| 消息模型 | ✅ | `ChannelMessage` 支持多种内容类型 |
| Agent 路由 | ✅ | `AgentChannelListener` → `MessageAdapter` → `IAgent` |
| Webhook | ✅ | `WebhookChannel` |
| Telegram | ✅ | `TelegramChannel`（长轮询） |
| 钉钉 | ✅ | `DingTalkChannel`（回调模式） |
| Hooks 端点 | ❌ | 无外部事件接收能力 |
| 消息投递服务 | ❌ | 无主动推送到渠道的能力 |

---

## 四、补齐方案

### 4.1 总体架构

```
┌─────────────────────────────────────────────────────────────────────┐
│                     evox-openclaw 自动化体系                         │
│                                                                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────────┐  │
│  │  Heartbeat   │  │  Cron Jobs   │  │  Hooks (事件钩子)         │  │
│  │  HeartbeatRun│  │  增强 Task   │  │  HookRegistry            │  │
│  │  ner         │  │  Scheduler   │  │  + HookController        │  │
│  └──────┬───────┘  └──────┬───────┘  └──────────┬───────────────┘  │
│         │                 │                      │                  │
│         ▼                 ▼                      ▼                  │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │           SessionManager (增强会话管理)                      │   │
│  │  ┌─────────────────┐    ┌──────────────────────────────┐   │   │
│  │  │  Main Session   │    │  Isolated Session            │   │   │
│  │  │  共享上下文       │    │  task:<taskId> 独立会话       │   │   │
│  │  └────────┬────────┘    └────────────┬─────────────────┘   │   │
│  └───────────┼──────────────────────────┼─────────────────────┘   │
│              │                          │                          │
│              ▼                          ▼                          │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │           Agent Execution                                    │   │
│  └──────────────────────────┬──────────────────────────────────┘   │
│                             │                                       │
│                             ▼                                       │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │           DeliveryService (结果投递)                          │   │
│  │  announce → ChannelRegistry 推送 │ none → 仅记录              │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
```

### 4.2 Phase 1：增强 Cron 调度（P0，1.5 周）

#### 4.2.1 任务持久化

**目标**：任务定义和运行历史持久化到文件，重启不丢失。

**新增类**：

```
evox-runtime/evox-scheduler/src/main/java/io/leavesfly/evox/scheduler/
├── store/
│   ├── ITaskStore.java              # 任务存储接口
│   ├── FileTaskStore.java           # JSON 文件存储实现
│   └── TaskRunRecord.java           # 运行历史记录
```

**核心设计**：

```java
public interface ITaskStore {
    /** 保存任务定义 */
    void saveTask(ScheduledTaskDefinition definition);

    /** 删除任务定义 */
    void removeTask(String taskId);

    /** 加载所有任务定义 */
    List<ScheduledTaskDefinition> loadAllTasks();

    /** 记录运行历史 */
    void recordRun(TaskRunRecord record);

    /** 查询运行历史 */
    List<TaskRunRecord> getRunHistory(String taskId, int limit);
}
```

**存储路径**：
- 任务定义：`~/.evox/scheduler/tasks.json`
- 运行历史：`~/.evox/scheduler/runs/<taskId>.jsonl`

#### 4.2.2 失败重试与指数退避

**增强 `TaskScheduler`**：

```java
// 重试配置
@Data
public class RetryConfig {
    private boolean enabled = true;
    private int maxRetries = 5;
    private List<Duration> backoffIntervals = List.of(
        Duration.ofSeconds(30),
        Duration.ofMinutes(1),
        Duration.ofMinutes(5),
        Duration.ofMinutes(15),
        Duration.ofMinutes(60)
    );
}
```

**重试逻辑**：
- 循环任务（`cron` / `every`）失败后按指数退避重试
- 一次性任务（`at`）失败后标记为 `FAILED`，不重试
- 成功执行后重置退避计数器

#### 4.2.3 Main/Isolated 会话模式

**新增类**：

```
evox-runtime/evox-scheduler/src/main/java/io/leavesfly/evox/scheduler/
├── session/
│   ├── SessionTarget.java           # 枚举：MAIN / ISOLATED
│   ├── TaskPayload.java             # 任务载荷（systemEvent / agentTurn）
│   └── IsolatedSessionRunner.java   # 隔离会话执行器
```

**核心设计**：

```java
public enum SessionTarget {
    MAIN,       // 主会话：通过 systemEvent 注入，由 Heartbeat 处理
    ISOLATED    // 隔离会话：独立干净会话，task:<taskId>
}

@Data
@Builder
public class TaskPayload {
    private PayloadKind kind;        // SYSTEM_EVENT / AGENT_TURN
    private String message;          // 提示文本
    private String model;            // 模型覆盖（可选）
    private String thinkingLevel;    // 思考级别覆盖（可选）
    private int timeoutSeconds;      // 超时覆盖（可选）
}
```

**执行流程**：
- **Main Session**：将 systemEvent 加入主会话事件队列，等待 Heartbeat 处理
- **Isolated Session**：创建独立会话 `task:<taskId>`，执行 Agent Turn，完成后销毁

#### 4.2.4 Announce 投递

**新增类**：

```
evox-runtime/evox-scheduler/src/main/java/io/leavesfly/evox/scheduler/
├── delivery/
│   ├── DeliveryConfig.java          # 投递配置
│   ├── DeliveryMode.java            # 枚举：ANNOUNCE / NONE
│   └── DeliveryService.java         # 投递服务（调用 ChannelRegistry 发送）
```

**核心设计**：

```java
@Data
@Builder
public class DeliveryConfig {
    private DeliveryMode mode;       // ANNOUNCE / NONE
    private String channel;          // 目标渠道（telegram / dingtalk / webhook / last）
    private String target;           // 渠道内目标（聊天 ID / 用户 ID）
    private boolean bestEffort;      // 投递失败是否忽略
}
```

**投递流程**：
1. Isolated 任务执行完成，获取 Agent 输出
2. 根据 `DeliveryConfig` 确定目标渠道和接收者
3. 通过 `ChannelRegistry.getChannel(channelId)` 获取渠道实例
4. 调用 `channel.sendMessage(target, message)` 推送结果
5. 向 Main Session 发送简短摘要（如果 mode = ANNOUNCE）

#### 4.2.5 增强 AgentTask

**增强 `AgentTask`**，支持新特性：

```java
@Data
@Builder
public class AgentTask implements IScheduledTask {
    // 现有字段
    private String taskId;
    private String taskName;
    private IAgent agent;
    private String prompt;
    private ITrigger trigger;
    private boolean enabled;

    // 新增字段
    private SessionTarget sessionTarget;     // MAIN / ISOLATED
    private TaskPayload payload;             // 任务载荷
    private DeliveryConfig delivery;         // 投递配置
    private RetryConfig retryConfig;         // 重试配置
    private String agentId;                  // 指定 Agent（多 Agent 场景）
    private boolean deleteAfterRun;          // 一次性任务执行后删除
    private String description;              // 任务描述
}
```

#### 4.2.6 实现步骤

| 步骤 | 内容 | 工期 |
|------|------|------|
| 1 | 实现 `ITaskStore` + `FileTaskStore` 持久化 | 1 天 |
| 2 | 实现 `RetryConfig` + 指数退避重试逻辑 | 0.5 天 |
| 3 | 实现 `SessionTarget` + `IsolatedSessionRunner` | 1.5 天 |
| 4 | 实现 `DeliveryService` + `DeliveryConfig` | 1 天 |
| 5 | 增强 `AgentTask` + `TaskScheduler` 集成 | 1 天 |
| 6 | 增强 `TaskPayload` 支持模型/思考级别覆盖 | 0.5 天 |
| 7 | 集成测试 | 1 天 |

---

### 4.3 Phase 2：Heartbeat 心跳机制（P0，1 周）

#### 4.3.1 核心设计

**新增类**：

```
evox-runtime/evox-scheduler/src/main/java/io/leavesfly/evox/scheduler/
├── heartbeat/
│   ├── HeartbeatRunner.java         # 心跳执行器
│   ├── HeartbeatConfig.java         # 心跳配置
│   ├── SystemEvent.java             # 系统事件
│   └── SystemEventQueue.java        # 系统事件队列
```

**HeartbeatRunner 核心逻辑**：

```java
public class HeartbeatRunner {
    private final IAgent agent;
    private final SystemEventQueue eventQueue;
    private final HeartbeatConfig config;
    private final ScheduledExecutorService scheduler;

    /** 启动心跳 */
    public void start() {
        scheduler.scheduleWithFixedDelay(
            this::runHeartbeat,
            config.getInitialDelayMs(),
            config.getIntervalMs(),
            TimeUnit.MILLISECONDS
        );
    }

    /** 执行一次心跳 */
    private void runHeartbeat() {
        // 1. 收集待处理的系统事件
        List<SystemEvent> pendingEvents = eventQueue.drainAll();

        // 2. 构建心跳提示（包含 HEARTBEAT.md + 系统事件）
        String heartbeatPrompt = buildHeartbeatPrompt(pendingEvents);

        // 3. 在 Main Session 中执行 Agent
        Message result = agent.execute("heartbeat", heartbeatPrompt);

        // 4. 处理 Agent 输出（如果有需要投递的内容）
        processHeartbeatResult(result);
    }

    /** 立即唤醒（wakeMode = "now"） */
    public void wakeNow() {
        scheduler.submit(this::runHeartbeat);
    }
}
```

**SystemEventQueue**：

```java
public class SystemEventQueue {
    private final ConcurrentLinkedQueue<SystemEvent> queue = new ConcurrentLinkedQueue<>();

    /** 入队系统事件（Main Session Cron 任务使用） */
    public void enqueue(SystemEvent event) {
        queue.offer(event);
    }

    /** 取出所有待处理事件 */
    public List<SystemEvent> drainAll() {
        List<SystemEvent> events = new ArrayList<>();
        SystemEvent event;
        while ((event = queue.poll()) != null) {
            events.add(event);
        }
        return events;
    }
}
```

**HeartbeatConfig**：

```java
@Data
public class HeartbeatConfig {
    private boolean enabled = true;
    private long intervalMs = 300_000;       // 默认 5 分钟
    private long initialDelayMs = 10_000;    // 启动延迟 10 秒
    private String heartbeatFilePath;        // HEARTBEAT.md 路径（可选）
}
```

#### 4.3.2 与 Cron Main Session 的集成

```
Main Session Cron 任务触发
    │
    ▼
SystemEventQueue.enqueue(event)
    │
    ├── wakeMode = "now" ──────→ HeartbeatRunner.wakeNow()
    │                                    │
    │                                    ▼
    │                            立即执行 runHeartbeat()
    │
    └── wakeMode = "next-heartbeat" ──→ 等待下次定时心跳
                                              │
                                              ▼
                                       runHeartbeat() 处理积压事件
```

#### 4.3.3 实现步骤

| 步骤 | 内容 | 工期 |
|------|------|------|
| 1 | 实现 `SystemEvent` + `SystemEventQueue` | 0.5 天 |
| 2 | 实现 `HeartbeatRunner` + `HeartbeatConfig` | 1.5 天 |
| 3 | 集成 Main Session Cron → SystemEventQueue → Heartbeat | 1 天 |
| 4 | 在 `AssistantAutoConfiguration` 中装配 Heartbeat | 0.5 天 |
| 5 | 集成测试 | 0.5 天 |

---

### 4.4 Phase 3：Hooks 事件钩子（P1，1.5 周）

#### 4.4.1 核心设计

**新增模块/包**：

```
evox-runtime/evox-scheduler/src/main/java/io/leavesfly/evox/scheduler/
├── hook/
│   ├── IHook.java                   # Hook 接口
│   ├── HookRegistry.java            # Hook 注册中心
│   ├── HookMapping.java             # Hook 映射配置
│   ├── HookMatchRule.java           # 匹配规则
│   ├── HookContext.java             # Hook 执行上下文
│   ├── HookController.java          # HTTP 端点（接收外部事件）
│   └── template/
│       └── MustacheRenderer.java    # Mustache 模板渲染
```

**HookMapping 配置模型**：

```java
@Data
@Builder
public class HookMapping {
    private HookMatchRule match;         // 匹配规则（路径、Header 等）
    private String action;               // 执行动作（"agent"）
    private String wakeMode;             // 唤醒模式（"now" / "next-heartbeat"）
    private String sessionKey;           // 会话隔离键（支持模板变量）
    private String messageTemplate;      // 消息模板（Mustache 语法）
    private boolean deliver;             // 是否投递到聊天
    private String channel;              // 目标渠道
    private String target;               // 渠道内目标
    private String model;                // 模型覆盖（可选）
    private boolean wrapExternalContent; // 是否安全包装外部内容
}

@Data
@Builder
public class HookMatchRule {
    private String path;                 // 匹配路径（如 "/gmail"）
    private Map<String, String> headers; // 匹配 Header
    private String method;               // 匹配 HTTP 方法
}
```

**HookController（Spring MVC）**：

```java
@RestController
@RequestMapping("/hooks")
public class HookController {
    private final HookRegistry hookRegistry;
    private final HeartbeatRunner heartbeatRunner;
    private final IsolatedSessionRunner isolatedRunner;

    @PostMapping("/{**path}")
    public ResponseEntity<Map<String, Object>> handleHook(
            @PathVariable String path,
            @RequestHeader Map<String, String> headers,
            @RequestBody(required = false) String body) {

        // 1. 查找匹配的 HookMapping
        HookMapping mapping = hookRegistry.findMapping(path, headers);

        // 2. 渲染消息模板
        String message = renderTemplate(mapping.getMessageTemplate(), headers, body);

        // 3. 安全包装外部内容
        if (mapping.isWrapExternalContent()) {
            message = wrapWithSafetyBoundary(message);
        }

        // 4. 根据 sessionKey 决定会话模式
        if (isIsolatedSession(mapping)) {
            isolatedRunner.execute(mapping, message);
        } else {
            SystemEvent event = new SystemEvent(message);
            heartbeatRunner.enqueueEvent(event, mapping.getWakeMode());
        }

        return ResponseEntity.ok(Map.of("status", "accepted"));
    }
}
```

#### 4.4.2 内置 Hook 预设

| 预设 | 实现方式 | 说明 |
|------|---------|------|
| **Webhook** | `HookController` 直接接收 | 通用 HTTP 回调 |
| **Gmail PubSub** | 集成 Gmail API Watch + Pub/Sub | 新邮件触发 Agent |
| **Poll** | `PollHook` 定期轮询外部 URL | 数据变化触发 Agent |

#### 4.4.3 实现步骤

| 步骤 | 内容 | 工期 |
|------|------|------|
| 1 | 定义 `IHook`、`HookMapping`、`HookMatchRule` | 0.5 天 |
| 2 | 实现 `HookRegistry` + 映射匹配逻辑 | 1 天 |
| 3 | 实现 `MustacheRenderer` 模板渲染 | 0.5 天 |
| 4 | 实现 `HookController` HTTP 端点 | 1 天 |
| 5 | 实现安全边界包装 | 0.5 天 |
| 6 | 实现 `PollHook` 轮询预设 | 1 天 |
| 7 | 在 `AssistantAutoConfiguration` 中装配 | 0.5 天 |
| 8 | 集成测试 | 1 天 |

---

### 4.5 Phase 4：渠道扩展（P1，2 周）

#### 4.5.1 优先级排序

基于 OpenClaw 支持的渠道和国内使用场景：

| 优先级 | 渠道 | 说明 | 工期 |
|--------|------|------|------|
| P0 | **Discord** | 开发者社区主流 | 2 天 |
| P0 | **Slack** | 企业协作主流 | 2 天 |
| P1 | **飞书 (Feishu)** | 国内企业主流 | 2 天 |
| P1 | **WhatsApp** | 全球用户量最大 | 3 天 |
| P2 | **企业微信 (WeCom)** | 国内企业主流（OpenClaw 未支持） | 2 天 |
| P2 | **Signal** | 隐私优先 | 1 天 |

#### 4.5.2 渠道实现模板

每个新渠道需要实现：

```
evox-runtime/evox-channels/src/main/java/io/leavesfly/evox/channels/<channel>/
├── <Channel>Channel.java           # 继承 AbstractChannel
├── <Channel>Config.java            # 继承 ChannelConfig
└── <Channel>Controller.java        # 回调控制器（如需要）
```

---

### 4.6 Phase 5：管理 CLI（P2，1 周）

#### 4.6.1 目标

提供类似 OpenClaw 的 CLI 管理工具：

```bash
# Cron 管理
evox cron add --name "Morning brief" --cron "0 7 * * *" --session isolated --message "总结今日待办"
evox cron list
evox cron run <taskId>
evox cron edit <taskId> --message "更新后的提示"
evox cron runs --id <taskId> --limit 50

# 渠道管理
evox channels list
evox channels login telegram

# 系统事件
evox system event --mode now --text "检查邮件"

# Hook 管理
evox hooks list
evox hooks test /gmail
```

#### 4.6.2 实现方式

基于 Spring Shell 或 Picocli 实现 CLI 工具。

---

## 五、配置增强

### 5.1 application.yml 增强

```yaml
evox:
  assistant:
    # 现有配置...

    # 新增：心跳配置
    heartbeat:
      enabled: true
      interval-ms: 300000              # 5 分钟
      initial-delay-ms: 10000
      heartbeat-file: HEARTBEAT.md     # 心跳提示文件（可选）

    # 增强：调度器配置
    scheduler:
      enabled: true
      check-interval-ms: 1000
      store-path: ~/.evox/scheduler    # 持久化路径
      max-concurrent-runs: 1           # 最大并发任务数
      retry:
        enabled: true
        max-retries: 5

    # 新增：Hooks 配置
    hooks:
      enabled: true
      token: ${EVOX_HOOK_TOKEN:}
      path: /hooks
      presets:
        - webhook
      mappings: []
      wrap-external-content: true
```

---

## 六、实施路线图

```
Phase 1（第 1-2 周）：增强 Cron 调度
├── 任务持久化（FileTaskStore）
├── 失败重试 + 指数退避
├── Main/Isolated 会话模式
├── Announce 投递（DeliveryService）
└── 增强 AgentTask

Phase 2（第 2-3 周）：Heartbeat 心跳
├── SystemEventQueue
├── HeartbeatRunner
├── Main Session Cron → Heartbeat 集成
└── 配置装配

Phase 3（第 3-5 周）：Hooks 事件钩子
├── HookRegistry + HookMapping
├── HookController HTTP 端点
├── Mustache 模板渲染
├── 安全边界包装
└── PollHook 轮询预设

Phase 4（第 5-7 周）：渠道扩展
├── Discord Channel
├── Slack Channel
├── 飞书 Channel
├── WhatsApp Channel
└── 企业微信 Channel

Phase 5（第 7-8 周）：管理 CLI
├── Cron CLI 命令
├── Channel CLI 命令
└── Hook CLI 命令
```

**总工期**：约 **8 周**

---

## 七、与 OpenClaw 补齐后的对比

| 功能 | OpenClaw | evox-openclaw（补齐后） |
|------|----------|----------------------|
| 多渠道网关 | 20+ 渠道 | 8+ 渠道（含钉钉/飞书/企微） |
| Cron 定时任务 | ✅ at/every/cron | ✅ 完全对标 |
| Main/Isolated 会话 | ✅ | ✅ |
| Heartbeat 心跳 | ✅ | ✅ |
| Hooks 事件钩子 | ✅ Gmail/Webhook/Poll | ✅ Webhook/Poll |
| 任务持久化 | ✅ JSON 文件 | ✅ JSON 文件 |
| 失败重试 | ✅ 指数退避 | ✅ 指数退避 |
| Announce 投递 | ✅ | ✅ |
| 管理 CLI | ✅ | ✅ |
| RAG 知识库 | ❌ | ✅ EvoX 独有 |
| 工作流编排 | ❌ | ✅ EvoX 独有 |
| 多智能体协同 | ❌ | ✅ EvoX 独有 |
| MCP 协议 | ❌ | ✅ EvoX 独有 |
| 提示词优化 | ❌ | ✅ EvoX 独有 |
