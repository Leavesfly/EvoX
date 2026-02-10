
# 基于 EvoX 实现 OpenClaw 功能 — 完整实现方案

> **目标**: 基于 EvoX 现有的 Agent 核心能力，补齐消息渠道、主动调度、插件系统等能力，实现一个对标 OpenClaw 的个人 AI 助手平台。

---

## 一、整体架构设计

### 1.1 架构全景图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        用户触达层 (User Touchpoints)                     │
│  ┌──────────┬──────────┬──────────┬──────────┬──────────┬──────────┐   │
│  │ Telegram │  Slack   │ Discord  │ DingTalk │ WeChat   │ Webhook  │   │
│  └────┬─────┴────┬─────┴────┬─────┴────┬─────┴────┬─────┴────┬─────┘   │
│       └──────────┴──────────┴──────────┴──────────┴──────────┘         │
│                              ▼                                         │
├─────────────────────────────────────────────────────────────────────────┤
│                     网关层 (Gateway Layer)                              │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │  evox-gateway                                                   │   │
│  │  ┌──────────┬──────────┬──────────┬──────────┬──────────┐      │   │
│  │  │ 认证鉴权 │ 限流熔断 │ 消息路由 │ 会话管理 │ 审计日志 │      │   │
│  │  └──────────┴──────────┴──────────┴──────────┴──────────┘      │   │
│  └─────────────────────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────────────────────┤
│                     消息渠道层 (Channel Layer)                          │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │  evox-channels                                                  │   │
│  │  ┌──────────────┬──────────────┬──────────────┐                │   │
│  │  │ channel-core │ channel-tg   │ channel-slack│ ...            │   │
│  │  │ 渠道抽象     │ Telegram适配 │ Slack适配    │                │   │
│  │  └──────────────┴──────────────┴──────────────┘                │   │
│  └─────────────────────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────────────────────┤
│                     调度层 (Scheduler Layer)                           │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │  evox-scheduler                                                 │   │
│  │  ┌──────────┬──────────┬──────────┬──────────┐                 │   │
│  │  │ 定时任务 │ 事件监听 │ 条件触发 │ 主动推送 │                 │   │
│  │  └──────────┴──────────┴──────────┴──────────┘                 │   │
│  └─────────────────────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────────────────────┤
│                     插件层 (Skills Layer)                               │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │  evox-skills                                                    │   │
│  │  ┌──────────┬──────────┬──────────┬──────────┐                 │   │
│  │  │ 插件引擎 │ 生命周期 │ 配置管理 │ 内置Skills│                │   │
│  │  └──────────┴──────────┴──────────┴──────────┘                 │   │
│  └─────────────────────────────────────────────────────────────────┘   │
├═════════════════════════════════════════════════════════════════════════┤
│                  ↓↓↓ EvoX 已有能力（直接复用）↓↓↓                       │
├─────────────────────────────────────────────────────────────────────────┤
│  运行时层: evox-agents | evox-workflow | evox-memory | evox-tools      │
│           evox-rag    | evox-mcp-runtime                              │
├─────────────────────────────────────────────────────────────────────────┤
│  核心层:   evox-core   | evox-models  | evox-actions | evox-mcp       │
│           evox-storage                                                │
├─────────────────────────────────────────────────────────────────────────┤
│  基础设施: Spring Boot 3.2+ | Reactor | Jackson | Lombok              │
└─────────────────────────────────────────────────────────────────────────┘
```

### 1.2 新增模块清单

| 模块名 | 所属层级 | 优先级 | 预估工期 | 说明 |
|--------|---------|--------|---------|------|
| `evox-channels` | 运行时层 | **P0** | 2 周 | 消息渠道接入（Telegram/Slack/Discord/DingTalk/Webhook） |
| `evox-scheduler` | 运行时层 | **P0** | 1.5 周 | 主动式调度引擎（定时/事件/条件触发） |
| `evox-skills` | 扩展层 | **P1** | 1.5 周 | Skills 插件系统（动态加载/卸载/配置） |
| `evox-gateway` | 应用层 | **P1** | 1 周 | 统一网关（认证/限流/路由/审计） |
| `evox-assistant` | 应用层 | **P1** | 1 周 | 个人助手主应用（整合所有能力） |

### 1.3 对现有模块的增强

| 模块 | 增强内容 | 优先级 |
|------|---------|--------|
| `evox-tools` | 新增 ShellTool、CalendarTool、SystemMonitorTool | P1 |
| `evox-spring-boot-starter` | 新增 channels/scheduler/skills 的自动配置 | P1 |
| `evox-memory` | 增加按用户/渠道隔离的记忆管理 | P2 |

---

## 二、模块详细设计

---

### 2.1 消息渠道接入层 — `evox-channels`（P0）

#### 2.1.1 模块定位

提供统一的消息渠道抽象，让 Agent 能够通过各种 IM 工具与用户交互。这是实现 OpenClaw 体验的**最核心模块**。

#### 2.1.2 目录结构

```
evox-runtime/evox-channels/
├── pom.xml
├── src/main/java/io/leavesfly/evox/channels/
│   ├── core/                              # 渠道核心抽象
│   │   ├── IChannel.java                  # 渠道接口
│   │   ├── IChannelListener.java          # 消息监听器接口
│   │   ├── ChannelMessage.java            # 渠道消息（统一格式）
│   │   ├── ChannelUser.java               # 渠道用户
│   │   ├── ChannelConfig.java             # 渠道配置基类
│   │   ├── ChannelRegistry.java           # 渠道注册表
│   │   └── ChannelStatus.java             # 渠道状态枚举
│   ├── adapter/                           # 消息适配器
│   │   ├── MessageAdapter.java            # 消息格式转换（ChannelMessage ↔ Message）
│   │   └── MediaAdapter.java             # 多媒体适配（图片/文件/语音）
│   ├── router/                            # 消息路由
│   │   ├── MessageRouter.java             # 消息路由器
│   │   └── RoutingRule.java               # 路由规则
│   ├── telegram/                          # Telegram 适配
│   │   ├── TelegramChannel.java
│   │   ├── TelegramConfig.java
│   │   └── TelegramMessageParser.java
│   ├── slack/                             # Slack 适配
│   │   ├── SlackChannel.java
│   │   ├── SlackConfig.java
│   │   └── SlackMessageParser.java
│   ├── discord/                           # Discord 适配
│   │   ├── DiscordChannel.java
│   │   ├── DiscordConfig.java
│   │   └── DiscordMessageParser.java
│   ├── dingtalk/                          # 钉钉适配
│   │   ├── DingTalkChannel.java
│   │   ├── DingTalkConfig.java
│   │   └── DingTalkMessageParser.java
│   └── webhook/                           # 通用 Webhook
│       ├── WebhookChannel.java
│       ├── WebhookConfig.java
│       └── WebhookController.java
```

#### 2.1.3 核心接口设计

```java
/**
 * 渠道接口 — 所有消息渠道的统一抽象
 */
public interface IChannel {

    /** 渠道唯一标识（如 "telegram", "slack"） */
    String getChannelId();

    /** 渠道显示名称 */
    String getChannelName();

    /** 启动渠道（开始监听消息） */
    void start() throws ChannelException;

    /** 停止渠道 */
    void stop();

    /** 发送消息到指定用户/群组 */
    CompletableFuture<Void> sendMessage(String targetId, ChannelMessage message);

    /** 发送富文本消息（Markdown/HTML） */
    CompletableFuture<Void> sendRichMessage(String targetId, String content, String format);

    /** 发送文件 */
    CompletableFuture<Void> sendFile(String targetId, byte[] fileData, String fileName);

    /** 注册消息监听器 */
    void addListener(IChannelListener listener);

    /** 获取渠道状态 */
    ChannelStatus getStatus();

    /** 获取渠道配置 */
    ChannelConfig getConfig();
}

/**
 * 消息监听器 — 接收来自渠道的消息
 */
public interface IChannelListener {

    /** 收到文本消息 */
    void onMessage(ChannelMessage message);

    /** 收到文件/图片 */
    void onMediaMessage(ChannelMessage message);

    /** 用户加入/离开 */
    void onUserEvent(ChannelUser user, String eventType);

    /** 渠道状态变更 */
    void onStatusChange(ChannelStatus oldStatus, ChannelStatus newStatus);
}

/**
 * 统一渠道消息格式
 */
@Data
@Builder
public class ChannelMessage {
    private String messageId;
    private String channelId;          // 来源渠道
    private String senderId;           // 发送者ID
    private String senderName;         // 发送者名称
    private String targetId;           // 目标（群组/私聊）
    private String content;            // 文本内容
    private MessageContentType contentType;  // TEXT, IMAGE, FILE, AUDIO, VIDEO
    private byte[] mediaData;          // 多媒体数据
    private String mediaUrl;           // 多媒体URL
    private Instant timestamp;
    private Map<String, Object> metadata;    // 渠道特有的元数据
    private ChannelMessage replyTo;    // 回复的消息（线程）
}
```

#### 2.1.4 消息流转流程

```
用户发消息 (Telegram/Slack/...)
    │
    ▼
┌─────────────────┐
│  XxxChannel      │  ← 各渠道 SDK 接收原始消息
│  (Telegram等)    │
└────────┬────────┘
         │ 转换为 ChannelMessage
         ▼
┌─────────────────┐
│  MessageRouter   │  ← 根据路由规则分发
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  MessageAdapter  │  ← ChannelMessage → evox Message
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Agent.execute() │  ← 复用 EvoX Agent 体系
│  (ReActAgent /   │
│   ToolAwareAgent)│
└────────┬────────┘
         │ 返回 Message
         ▼
┌─────────────────┐
│  MessageAdapter  │  ← evox Message → ChannelMessage
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  XxxChannel      │  ← 通过渠道 SDK 发送回复
│  .sendMessage()  │
└─────────────────┘
```

#### 2.1.5 依赖关系

```xml
<dependencies>
    <!-- 核心层 -->
    <dependency>
        <groupId>io.leavesfly.evox</groupId>
        <artifactId>evox-core</artifactId>
    </dependency>
    <!-- 渠道 SDK（各渠道按需引入） -->
    <dependency>
        <groupId>org.telegram</groupId>
        <artifactId>telegrambots-longpolling</artifactId>
        <version>8.0.0</version>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>com.slack.api</groupId>
        <artifactId>bolt</artifactId>
        <version>1.40.0</version>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>net.dv8tion</groupId>
        <artifactId>JDA</artifactId>
        <version>5.1.0</version>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>com.aliyun</groupId>
        <artifactId>dingtalk</artifactId>
        <version>2.1.0</version>
        <optional>true</optional>
    </dependency>
</dependencies>
```

#### 2.1.6 实现步骤

| 步骤 | 内容 | 工期 |
|------|------|------|
| 1 | 定义 `IChannel`、`ChannelMessage`、`MessageAdapter` 核心抽象 | 2天 |
| 2 | 实现 `WebhookChannel`（最简单，用于验证流程） | 1天 |
| 3 | 实现 `TelegramChannel`（最常用的个人助手渠道） | 2天 |
| 4 | 实现 `DingTalkChannel`（国内企业场景） | 2天 |
| 5 | 实现 `SlackChannel` + `DiscordChannel` | 2天 |
| 6 | 实现 `MessageRouter` 路由和多渠道管理 | 1天 |

---

### 2.2 主动式调度引擎 — `evox-scheduler`（P0）

#### 2.2.1 模块定位

让 Agent 具备**主动行动**的能力，而不仅仅是被动响应。这是 OpenClaw 区别于普通 ChatBot 的关键特性。

#### 2.2.2 目录结构

```
evox-runtime/evox-scheduler/
├── pom.xml
├── src/main/java/io/leavesfly/evox/scheduler/
│   ├── core/                              # 调度核心
│   │   ├── IScheduledTask.java            # 调度任务接口
│   │   ├── TaskScheduler.java             # 任务调度器
│   │   ├── TaskContext.java               # 任务执行上下文
│   │   ├── TaskResult.java                # 任务执行结果
│   │   └── SchedulerConfig.java           # 调度器配置
│   ├── trigger/                           # 触发器
│   │   ├── ITrigger.java                  # 触发器接口
│   │   ├── CronTrigger.java              # Cron 定时触发
│   │   ├── IntervalTrigger.java          # 固定间隔触发
│   │   ├── EventTrigger.java             # 事件驱动触发
│   │   ├── ConditionTrigger.java         # 条件触发
│   │   └── WebhookTrigger.java           # Webhook 触发
│   ├── event/                             # 事件系统
│   │   ├── IEventSource.java             # 事件源接口
│   │   ├── EventBus.java                 # 事件总线
│   │   ├── FileWatchEventSource.java     # 文件变更监听
│   │   ├── GitEventSource.java           # Git 事件（push/PR）
│   │   └── HttpEventSource.java          # HTTP Webhook 事件
│   ├── push/                              # 主动推送
│   │   ├── IPushService.java             # 推送服务接口
│   │   └── ChannelPushService.java       # 通过渠道推送
│   └── task/                              # 内置任务
│       ├── AgentTask.java                # Agent 执行任务
│       ├── WorkflowTask.java             # 工作流执行任务
│       └── HealthCheckTask.java          # 健康检查任务
```

#### 2.2.3 核心接口设计

```java
/**
 * 调度任务接口
 */
public interface IScheduledTask {

    /** 任务唯一标识 */
    String getTaskId();

    /** 任务名称 */
    String getTaskName();

    /** 执行任务 */
    TaskResult execute(TaskContext context);

    /** 获取关联的触发器 */
    ITrigger getTrigger();

    /** 任务是否启用 */
    boolean isEnabled();
}

/**
 * 触发器接口
 */
public interface ITrigger {

    /** 触发器类型 */
    String getType();

    /** 计算下次触发时间 */
    Instant getNextFireTime();

    /** 判断是否应该触发 */
    boolean shouldFire(TaskContext context);

    /** 触发后回调 */
    void onFired();
}

/**
 * 任务调度器 — 管理所有调度任务的生命周期
 */
public class TaskScheduler {

    /** 注册调度任务 */
    public void scheduleTask(IScheduledTask task);

    /** 取消调度任务 */
    public void cancelTask(String taskId);

    /** 暂停/恢复任务 */
    public void pauseTask(String taskId);
    public void resumeTask(String taskId);

    /** 立即执行一次 */
    public TaskResult executeNow(String taskId);

    /** 启动调度器 */
    public void start();

    /** 停止调度器 */
    public void shutdown();

    /** 获取所有任务状态 */
    public List<TaskStatus> getAllTaskStatus();
}

/**
 * Agent 调度任务 — 将 Agent 执行包装为调度任务
 *
 * 示例：每天早上 8 点让 Agent 总结昨天的 GitHub 通知
 */
public class AgentTask implements IScheduledTask {
    private IAgent agent;
    private String actionName;
    private String prompt;           // 发给 Agent 的指令
    private ITrigger trigger;
    private IPushService pushService; // 结果推送服务

    @Override
    public TaskResult execute(TaskContext context) {
        Message input = Message.inputMessage(prompt);
        Message result = agent.execute(actionName, List.of(input));

        // 主动推送结果到用户渠道
        pushService.push(context.getUserId(), result.getContent().toString());

        return TaskResult.success(result);
    }
}
```

#### 2.2.4 典型使用场景

```java
// 场景 1：每天早上 8 点总结 GitHub 通知
AgentTask githubSummary = AgentTask.builder()
    .taskId("github-daily-summary")
    .taskName("GitHub 每日摘要")
    .agent(reActAgent)
    .prompt("请检查我的 GitHub 通知，总结昨天的重要更新")
    .trigger(new CronTrigger("0 0 8 * * ?"))
    .pushService(channelPushService)
    .build();
scheduler.scheduleTask(githubSummary);

// 场景 2：监控文件变更，自动执行代码审查
AgentTask codeReview = AgentTask.builder()
    .taskId("auto-code-review")
    .taskName("自动代码审查")
    .agent(codeReviewAgent)
    .prompt("请审查最新的代码变更")
    .trigger(new EventTrigger(new FileWatchEventSource("./src")))
    .pushService(channelPushService)
    .build();
scheduler.scheduleTask(codeReview);

// 场景 3：股价跌破阈值时告警
AgentTask stockAlert = AgentTask.builder()
    .taskId("stock-alert")
    .taskName("股价告警")
    .agent(stockAgent)
    .prompt("检查 AAPL 股价，如果跌破 150 美元请告警")
    .trigger(new IntervalTrigger(Duration.ofMinutes(30)))
    .pushService(channelPushService)
    .build();
scheduler.scheduleTask(stockAlert);
```

#### 2.2.5 实现步骤

| 步骤 | 内容 | 工期 |
|------|------|------|
| 1 | 定义 `IScheduledTask`、`ITrigger`、`TaskScheduler` 核心抽象 | 1天 |
| 2 | 实现 `CronTrigger`、`IntervalTrigger` 基础触发器 | 1天 |
| 3 | 实现 `TaskScheduler` 调度引擎（基于 ScheduledExecutorService） | 2天 |
| 4 | 实现 `AgentTask`、`WorkflowTask` 任务包装 | 1天 |
| 5 | 实现 `EventTrigger` + `EventBus` 事件驱动 | 2天 |
| 6 | 实现 `ChannelPushService` 与 evox-channels 集成 | 1天 |
| 7 | 实现 `ConditionTrigger` 条件触发 | 1天 |

---

### 2.3 Skills 插件系统 — `evox-skills`（P1）

#### 2.3.1 模块定位

提供类似 OpenClaw 的 Skills 机制，让用户可以动态安装/卸载功能插件，扩展 Agent 的能力边界。

#### 2.3.2 目录结构

```
evox-extensions/evox-skills/
├── pom.xml
├── src/main/java/io/leavesfly/evox/skills/
│   ├── core/                              # 插件核心
│   │   ├── ISkill.java                    # Skill 接口
│   │   ├── SkillDescriptor.java           # Skill 描述符（元数据）
│   │   ├── SkillContext.java              # Skill 执行上下文
│   │   ├── SkillResult.java               # Skill 执行结果
│   │   └── SkillConfig.java               # Skill 配置
│   ├── engine/                            # 插件引擎
│   │   ├── SkillEngine.java               # 插件引擎（加载/卸载/执行）
│   │   ├── SkillRegistry.java             # 插件注册表
│   │   ├── SkillLoader.java               # 插件加载器
│   │   └── SkillLifecycle.java            # 生命周期管理
│   ├── bridge/                            # 桥接层
│   │   ├── SkillToolBridge.java           # Skill → BaseTool 桥接
│   │   └── SkillActionBridge.java         # Skill → Action 桥接
│   └── builtin/                           # 内置 Skills
│       ├── SmartHomeSkill.java            # 智能家居控制
│       ├── StockTrackerSkill.java         # 股票追踪
│       ├── CalendarSkill.java             # 日历管理
│       ├── EmailSkill.java                # 邮件管理
│       ├── GitHubSkill.java               # GitHub 集成
│       ├── ReminderSkill.java             # 提醒功能
│       └── WeatherSkill.java              # 天气查询
```

#### 2.3.3 核心接口设计

```java
/**
 * Skill 接口 — 所有插件的统一抽象
 */
public interface ISkill {

    /** Skill 描述符（名称、版本、作者、描述等） */
    SkillDescriptor getDescriptor();

    /** 初始化 Skill（加载配置、建立连接等） */
    void initialize(SkillConfig config) throws SkillException;

    /** 执行 Skill */
    SkillResult execute(String action, Map<String, Object> parameters, SkillContext context);

    /** 获取支持的动作列表 */
    List<String> getSupportedActions();

    /** 获取动作的参数 Schema（供 LLM 理解如何调用） */
    Map<String, Object> getActionSchema(String action);

    /** 销毁 Skill（释放资源） */
    void destroy();

    /** Skill 是否就绪 */
    boolean isReady();
}

/**
 * Skill 描述符 — 插件元数据
 */
@Data
@Builder
public class SkillDescriptor {
    private String skillId;
    private String name;
    private String version;
    private String author;
    private String description;
    private String category;          // "productivity", "smart-home", "finance", ...
    private List<String> tags;
    private List<String> requiredPermissions;  // "file:read", "network", "shell", ...
    private Map<String, Object> configSchema;  // 配置项 Schema
}

/**
 * Skill 引擎 — 管理所有 Skill 的生命周期
 */
public class SkillEngine {

    /** 安装 Skill */
    public void installSkill(ISkill skill, SkillConfig config);

    /** 卸载 Skill */
    public void uninstallSkill(String skillId);

    /** 启用/禁用 Skill */
    public void enableSkill(String skillId);
    public void disableSkill(String skillId);

    /** 执行 Skill */
    public SkillResult executeSkill(String skillId, String action, Map<String, Object> params);

    /** 获取所有已安装的 Skill */
    public List<SkillDescriptor> getInstalledSkills();

    /** 将所有 Skill 桥接为 BaseTool，注入到 Agent 的工具集 */
    public List<BaseTool> bridgeToTools();
}

/**
 * Skill → BaseTool 桥接器
 *
 * 关键设计：将 Skill 自动转换为 EvoX 的 BaseTool，
 * 这样 ToolAwareAgent / ReActAgent 可以无缝调用 Skill
 */
public class SkillToolBridge extends BaseTool {
    private ISkill skill;
    private String action;

    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        SkillResult result = skill.execute(action, parameters, new SkillContext());
        return result.isSuccess()
            ? ToolResult.success(result.getData())
            : ToolResult.failure(result.getError());
    }
}
```

#### 2.3.4 与 EvoX 现有体系的集成

```
┌─────────────────────────────────────────────┐
│  SkillEngine                                │
│  ┌──────────┬──────────┬──────────┐        │
│  │ Skill A  │ Skill B  │ Skill C  │        │
│  └────┬─────┴────┬─────┴────┬─────┘        │
│       │          │          │               │
│       ▼          ▼          ▼               │
│  ┌──────────────────────────────────┐      │
│  │  SkillToolBridge                 │      │
│  │  (每个 Skill Action → BaseTool)  │      │
│  └──────────────┬───────────────────┘      │
└─────────────────┼───────────────────────────┘
                  │ 注入
                  ▼
┌─────────────────────────────────────────────┐
│  ToolRegistry / Toolkit                     │  ← EvoX 现有
│  (统一管理所有工具，包括 Skill 桥接的工具)    │
└─────────────────┬───────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────┐
│  ToolAwareAgent / ReActAgent                │  ← EvoX 现有
│  (Agent 通过工具调用机制自动使用 Skills)      │
└─────────────────────────────────────────────┘
```

#### 2.3.5 实现步骤

| 步骤 | 内容 | 工期 |
|------|------|------|
| 1 | 定义 `ISkill`、`SkillDescriptor`、`SkillEngine` 核心抽象 | 1天 |
| 2 | 实现 `SkillToolBridge` 桥接器（Skill → BaseTool） | 1天 |
| 3 | 实现 `SkillRegistry` + `SkillLifecycle` 生命周期管理 | 1天 |
| 4 | 实现 3-5 个内置 Skills（Weather、Reminder、GitHub、Calendar、StockTracker） | 3天 |
| 5 | 实现 Skill 配置管理（YAML 配置 + 热加载） | 1天 |
| 6 | 与 `ToolRegistry` 集成测试 | 1天 |

---

### 2.4 系统级访问增强 — 增强 `evox-tools`（P1）

#### 2.4.1 新增工具清单

```
evox-runtime/evox-tools/src/main/java/io/leavesfly/evox/tools/
├── shell/
│   └── ShellTool.java              # 已有，需增强
│       ├── 增加：超时控制
│       ├── 增加：工作目录切换
│       ├── 增加：环境变量注入
│       └── 增加：危险命令拦截（rm -rf / 等）
├── system/                          # 新增：系统工具
│   ├── SystemInfoTool.java          # CPU/内存/磁盘/网络信息
│   ├── ProcessManagerTool.java      # 进程管理（列出/杀死进程）
│   └── NotificationTool.java       # 系统通知（macOS/Linux）
├── calendar/                        # 新增：日历工具
│   ├── CalendarTool.java            # 日历管理
│   ├── GoogleCalendarAdapter.java   # Google Calendar 适配
│   └── ICalAdapter.java            # iCal 格式适配
├── email/                           # 新增：邮件工具
│   ├── EmailTool.java               # 邮件收发
│   ├── IMAPAdapter.java            # IMAP 收件
│   └── SMTPAdapter.java            # SMTP 发件
└── clipboard/                       # 新增：剪贴板工具
    └── ClipboardTool.java           # 剪贴板读写
```

#### 2.4.2 ShellTool 增强设计

```java
public class ShellTool extends BaseTool {

    // 新增：安全配置
    private List<String> blockedCommands = List.of(
        "rm -rf /", "mkfs", "dd if=", ":(){ :|:& };:"
    );
    private long timeoutSeconds = 30;
    private String workingDirectory;
    private boolean requireApproval = false;  // 危险命令需要人工审批

    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        String command = (String) parameters.get("command");

        // 1. 安全检查
        if (isBlockedCommand(command)) {
            return ToolResult.failure("命令被安全策略拦截: " + command);
        }

        // 2. 需要审批的命令
        if (requireApproval && isDangerousCommand(command)) {
            return ToolResult.failure("此命令需要人工审批: " + command);
        }

        // 3. 执行命令（带超时）
        ProcessBuilder pb = new ProcessBuilder("sh", "-c", command);
        pb.directory(new File(workingDirectory));
        pb.redirectErrorStream(true);

        Process process = pb.start();
        boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

        if (!finished) {
            process.destroyForcibly();
            return ToolResult.failure("命令执行超时（" + timeoutSeconds + "s）");
        }

        String output = new String(process.getInputStream().readAllBytes());
        return ToolResult.success(output);
    }
}
```

#### 2.4.3 实现步骤

| 步骤 | 内容 | 工期 |
|------|------|------|
| 1 | 增强 `ShellTool`（安全拦截、超时、审批） | 1天 |
| 2 | 实现 `SystemInfoTool` + `ProcessManagerTool` | 1天 |
| 3 | 实现 `CalendarTool`（Google Calendar / iCal） | 2天 |
| 4 | 实现 `EmailTool`（IMAP + SMTP） | 1天 |
| 5 | 实现 `ClipboardTool` + `NotificationTool` | 1天 |

---

### 2.5 统一网关 — `evox-gateway`（P1）

#### 2.5.1 目录结构

```
evox-application/evox-gateway/
├── pom.xml
├── src/main/java/io/leavesfly/evox/gateway/
│   ├── auth/                              # 认证鉴权
│   │   ├── IAuthProvider.java             # 认证提供者接口
│   │   ├── ApiKeyAuthProvider.java        # API Key 认证
│   │   ├── OAuth2AuthProvider.java        # OAuth2 认证
│   │   └── UserSession.java              # 用户会话
│   ├── ratelimit/                         # 限流
│   │   ├── RateLimiter.java              # 限流器
│   │   └── RateLimitConfig.java          # 限流配置
│   ├── routing/                           # 路由
│   │   ├── GatewayRouter.java            # 网关路由器
│   │   └── AgentSelector.java            # Agent 选择器
│   ├── session/                           # 会话管理
│   │   ├── SessionManager.java           # 会话管理器
│   │   └── ConversationContext.java      # 对话上下文
│   ├── audit/                             # 审计
│   │   ├── AuditLogger.java             # 审计日志
│   │   └── AuditEvent.java              # 审计事件
│   └── config/                            # 配置
│       └── GatewayConfig.java            # 网关配置
```

#### 2.5.2 核心流程

```
用户消息 (from Channel)
    │
    ▼
┌──────────────┐
│ 认证鉴权      │ → 验证用户身份（API Key / OAuth2 / Channel Token）
└──────┬───────┘
       │
       ▼
┌──────────────┐
│ 限流检查      │ → 检查用户请求频率
└──────┬───────┘
       │
       ▼
┌──────────────┐
│ 会话恢复      │ → 从 SessionManager 恢复对话上下文
│              │ → 加载用户的 ShortTermMemory
└──────┬───────┘
       │
       ▼
┌──────────────┐
│ Agent 选择    │ → 根据消息内容/用户配置选择合适的 Agent
└──────┬───────┘
       │
       ▼
┌──────────────┐
│ Agent 执行    │ → 调用 Agent.execute()
└──────┬───────┘
       │
       ▼
┌──────────────┐
│ 审计记录      │ → 记录请求/响应/耗时/Token用量
└──────┬───────┘
       │
       ▼
  返回响应 (to Channel)
```

#### 2.5.3 实现步骤

| 步骤 | 内容 | 工期 |
|------|------|------|
| 1 | 实现 `ApiKeyAuthProvider` + `UserSession` | 1天 |
| 2 | 实现 `SessionManager` + `ConversationContext` | 1天 |
| 3 | 实现 `GatewayRouter` + `AgentSelector` | 1天 |
| 4 | 实现 `RateLimiter` 限流 | 0.5天 |
| 5 | 实现 `AuditLogger` 审计日志 | 0.5天 |
| 6 | 集成测试 | 1天 |

---

### 2.6 个人助手主应用 — `evox-assistant`（P1）

#### 2.6.1 模块定位

整合所有能力的**主应用入口**，类似 OpenClaw 的 `openclaw.mjs`。提供开箱即用的个人 AI 助手体验。

#### 2.6.2 目录结构

```
evox-application/evox-assistant/
├── pom.xml
├── src/main/java/io/leavesfly/evox/assistant/
│   ├── EvoXAssistantApplication.java      # Spring Boot 主入口
│   ├── config/
│   │   ├── AssistantConfig.java           # 助手配置
│   │   ├── ChannelAutoConfig.java         # 渠道自动配置
│   │   ├── SchedulerAutoConfig.java       # 调度器自动配置
│   │   └── SkillAutoConfig.java           # Skills 自动配置
│   ├── agent/
│   │   └── PersonalAssistantAgent.java    # 个人助手 Agent（整合所有能力）
│   └── api/
│       ├── AssistantController.java       # REST API
│       └── WebSocketHandler.java          # WebSocket 实时通信
├── src/main/resources/
│   ├── application.yml                    # 默认配置
│   └── skills/                            # 内置 Skills 配置
│       ├── weather.yml
│       ├── reminder.yml
│       └── github.yml
├── Dockerfile
└── docker-compose.yml
```

#### 2.6.3 配置示例

```yaml
# application.yml
evox:
  enabled: true

  # LLM 配置
  llm:
    provider: openai
    api-key: ${OPENAI_API_KEY}
    model: gpt-4o-mini

  # 渠道配置
  channels:
    telegram:
      enabled: true
      bot-token: ${TELEGRAM_BOT_TOKEN}
    dingtalk:
      enabled: true
      app-key: ${DINGTALK_APP_KEY}
      app-secret: ${DINGTALK_APP_SECRET}
    webhook:
      enabled: true
      port: 8080
      path: /api/webhook

  # 调度配置
  scheduler:
    enabled: true
    tasks:
      - name: github-daily-summary
        cron: "0 0 8 * * ?"
        agent: personal-assistant
        prompt: "总结我的 GitHub 通知"
      - name: weather-morning
        cron: "0 0 7 * * ?"
        agent: personal-assistant
        prompt: "查询今天杭州的天气"

  # Skills 配置
  skills:
    enabled: true
    auto-discover: true
    installed:
      - weather
      - reminder
      - github

  # 安全配置
  gateway:
    auth:
      type: api-key
      keys:
        - ${EVOX_API_KEY}
    rate-limit:
      requests-per-minute: 60
```

---

## 三、实施路线图

### Phase 1：核心通路打通（第 1-3 周）

**目标**: 实现最小可用版本 — 用户可以通过 Telegram 与 AI 助手对话

```
Week 1:
├── Day 1-2: evox-channels 核心抽象（IChannel、ChannelMessage、MessageAdapter）
├── Day 3:   WebhookChannel 实现 + 端到端验证
├── Day 4-5: TelegramChannel 实现

Week 2:
├── Day 1-2: evox-scheduler 核心（TaskScheduler、CronTrigger、IntervalTrigger）
├── Day 3:   AgentTask + ChannelPushService
├── Day 4:   EventTrigger + EventBus
├── Day 5:   DingTalkChannel 实现

Week 3:
├── Day 1-2: evox-gateway（认证 + 会话管理 + 路由）
├── Day 3:   evox-assistant 主应用搭建
├── Day 4:   端到端集成测试
├── Day 5:   Docker 打包 + 部署验证
```

**里程碑交付物**:
- ✅ 通过 Telegram / 钉钉 / Webhook 与 AI 助手对话
- ✅ 定时任务自动执行并推送结果
- ✅ 基本的认证和会话管理

### Phase 2：能力增强（第 4-5 周）

**目标**: 补齐 Skills 插件系统和系统级工具

```
Week 4:
├── Day 1-2: evox-skills 核心（ISkill、SkillEngine、SkillToolBridge）
├── Day 3-5: 内置 Skills 实现（Weather、Reminder、GitHub、Calendar）

Week 5:
├── Day 1:   ShellTool 增强（安全拦截 + 超时）
├── Day 2:   SystemInfoTool + ProcessManagerTool
├── Day 3:   CalendarTool + EmailTool
├── Day 4:   SlackChannel + DiscordChannel
├── Day 5:   集成测试 + 文档
```

**里程碑交付物**:
- ✅ Skills 插件系统可用，支持动态安装/卸载
- ✅ 5+ 内置 Skills
- ✅ 系统级工具（Shell、系统信息、日历、邮件）
- ✅ 5 个消息渠道全部可用

### Phase 3：体验优化（第 6-8 周，可选）

```
- Web UI 管理控制台
- 多用户/多租户支持
- Skills 市场
- 性能优化和压测
- 完善文档和示例
```

---

## 四、技术决策

### 4.1 为什么不用 Spring Integration？

Spring Integration 虽然提供了消息渠道抽象，但它面向的是企业集成场景（EIP），过于重量级。我们的渠道层更轻量，专注于 IM 消息的收发。

### 4.2 为什么 Skills 要桥接为 BaseTool？

EvoX 的 `ToolAwareAgent` 和 `ReActAgent` 已经有成熟的工具调用机制（JSON 格式 + 文本协议）。通过 `SkillToolBridge` 将 Skill 桥接为 `BaseTool`，可以**零改动**地让现有 Agent 使用 Skills，避免重复造轮子。

### 4.3 调度引擎为什么不用 Quartz？

Quartz 功能强大但过于重量级（需要数据库存储）。对于个人助手场景，基于 `ScheduledExecutorService` 的轻量调度器足够，且启动更快、配置更简单。如果未来需要分布式调度，可以替换为 Quartz 或 XXL-Job。

### 4.4 渠道 SDK 为什么标记为 optional？

不同用户只需要部分渠道。通过 `<optional>true</optional>` 标记，用户只需引入自己需要的渠道 SDK，避免不必要的依赖膨胀。Spring Boot 的 `@ConditionalOnClass` 可以自动检测。

---

## 五、与 OpenClaw 的功能对比（实施后）

| 功能 | OpenClaw | EvoX Assistant | 对比 |
|------|----------|---------------|------|
| 多渠道消息 | ✅ WhatsApp/Telegram/Slack/Discord/Google Chat | ✅ Telegram/Slack/Discord/DingTalk/Webhook | 🟢 对等，且支持钉钉 |
| 多模型支持 | ✅ Claude/GPT/Ollama | ✅ OpenAI/阿里云/Ollama/LiteLLM/SiliconFlow | 🟢 更丰富 |
| 主动调度 | ✅ 定时任务/事件驱动 | ✅ Cron/Interval/Event/Condition 触发 | 🟢 更灵活 |
| Skills 插件 | ✅ Skills 目录 | ✅ Skills 引擎 + 桥接 | 🟢 对等 |
| 本地文件访问 | ✅ | ✅ FileSystemTool | 🟢 对等 |
| Shell 执行 | ✅ | ✅ ShellTool（增强版） | 🟢 更安全 |
| 记忆管理 | ✅ 基础 | ✅ 短期+长期+向量检索 | 🟢 更强 |
| RAG 知识库 | ❌ | ✅ evox-rag | 🟢 EvoX 独有 |
| 工作流编排 | ❌ | ✅ DAG 工作流引擎 | 🟢 EvoX 独有 |
| 多智能体协同 | ❌ | ✅ 分层/共识/拍卖框架 | 🟢 EvoX 独有 |
| MCP 协议 | ❌ | ✅ 完整 MCP 支持 | 🟢 EvoX 独有 |
| 提示词优化 | ❌ | ✅ TextGrad/MIPRO/AFlow | 🟢 EvoX 独有 |
| 人机协同 | ❌ | ✅ evox-hitl | 🟢 EvoX 独有 |
| 自托管 | ✅ Docker | ✅ Docker + Spring Boot | 🟢 对等 |
| 技术栈 | TypeScript/Node.js | Java/Spring Boot | 🟡 各有优势 |

---

## 六、总结

基于 EvoX 实现 OpenClaw 功能，核心工作量集中在 **3 个新模块**：

1. **evox-channels** — 消息渠道接入（~2 周）
2. **evox-scheduler** — 主动调度引擎（~1.5 周）
3. **evox-skills** — Skills 插件系统（~1.5 周）

加上网关、主应用和工具增强，**总工期约 5-6 周**即可实现一个功能对标甚至超越 OpenClaw 的产品。

EvoX 的核心优势在于：已有的 **Agent 体系、工作流引擎、RAG、MCP、多智能体协同、提示词优化** 等能力是 OpenClaw 所不具备的，这些能力让 EvoX Assistant 在智能化程度上远超 OpenClaw。
