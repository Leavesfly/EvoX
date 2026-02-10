## EvoX OpenClaw — 个人 AI 助手主应用

EvoX OpenClaw 是 EvoX 框架的**开箱即用个人 AI 助手主应用**，整合了渠道接入、网关路由、调度引擎、Skills 插件、系统工具等核心能力，提供类似 [OpenClaw](https://github.com/nicepkg/openclaw) 的个人 AI 助手体验。

### 核心能力

- **多渠道接入** — 支持 Webhook、Telegram、钉钉等消息渠道，统一消息收发
- **统一网关** — API Key 认证、会话管理、限流、审计日志，保障安全与可观测性
- **Skills 插件系统** — 内置天气、提醒、GitHub、日历、股票追踪等技能，支持动态注册/卸载
- **系统级工具** — 系统信息、进程管理、通知、剪贴板、邮件、日历等工具开箱即用
- **定时调度** — 支持 Cron / Interval 定时任务，自动执行并推送结果
- **Web UI** — 内置前端页面，提供可视化交互界面

---

### 项目结构

```
evox-openclaw/
├── src/main/java/io/leavesfly/evox/assistant/
│   ├── AssistantApplication.java              # Spring Boot 启动入口
│   ├── AssistantBootstrap.java                # 生命周期引导（绑定 Agent → Channel、启动调度器）
│   ├── config/
│   │   ├── AssistantProperties.java           # 配置属性（evox.assistant.*）
│   │   └── AssistantAutoConfiguration.java    # 自动装配（网关、渠道、调度、Skills、工具）
│   └── controller/
│       ├── ChatController.java                # 对话 API（/api/chat）
│       ├── AdminController.java               # 管理 API（/api/admin/*）
│       └── HealthController.java              # 健康检查（/api/health）
├── src/main/resources/
│   ├── application.yml                        # 默认配置
│   └── static/
│       └── index.html                         # 内置 Web UI
└── pom.xml
```

---

### 快速开始

#### 1. 环境要求

- **JDK** 17+
- **Maven** 3.8+

#### 2. 配置

编辑 `src/main/resources/application.yml`，根据需要调整配置：

```yaml
server:
  port: 8080

evox:
  assistant:
    name: EvoX Assistant
    default-agent: default          # 默认使用的 Agent 名称

    # 网关配置
    gateway:
      enabled: true
      api-keys: []                  # API Key 列表，为空则不启用 Key 认证
      rate-limit-per-minute: 60     # 每分钟请求限流
      allow-anonymous-channel-access: true
      session-timeout-hours: 24
      max-audit-events: 10000

    # Webhook 渠道
    webhook:
      enabled: true
      path: /api/webhook

    # Telegram 渠道
    telegram:
      enabled: false
      # bot-token: YOUR_BOT_TOKEN
      # bot-username: YOUR_BOT_USERNAME

    # 钉钉渠道
    dingtalk:
      enabled: false
      # app-key: YOUR_APP_KEY
      # app-secret: YOUR_APP_SECRET
      # robot-code: YOUR_ROBOT_CODE

    # 调度器
    scheduler:
      enabled: true
      check-interval-ms: 1000
```

#### 3. 构建与运行

```bash
# 在项目根目录下构建
mvn clean package -pl evox-application/evox-openclaw -am

# 运行
java -jar evox-application/evox-openclaw/target/evox-openclaw-1.0.0-SNAPSHOT.jar
```

启动后访问 `http://localhost:8080` 即可打开 Web UI。

---

### API 接口

#### 对话接口

**POST** `/api/chat` — 发送消息并获取 AI 回复

Request：

```json
{
  "message": "今天杭州天气怎么样？",
  "userId": "user-001",
  "channelId": "webhook"
}
```

Response：

```json
{
  "success": true,
  "reply": "杭州今天晴，气温 15-25°C..."
}
```

**POST** `/api/chat/skill` — 直接调用指定 Skill

Request：

```json
{
  "skillName": "weather",
  "input": "杭州",
  "parameters": { "unit": "celsius" }
}
```

Response：

```json
{
  "success": true,
  "output": "杭州今天晴，气温 15-25°C...",
  "error": null
}
```

#### 健康检查

**GET** `/api/health` — 查看系统状态

```json
{
  "status": "UP",
  "timestamp": "2025-01-01T00:00:00Z",
  "components": {
    "channels": { "webhook": "RUNNING" },
    "agentCount": 1,
    "scheduledTasks": 0,
    "activeSessions": 3,
    "auditEvents": 128,
    "skills": { "count": 5, "names": ["weather", "reminder", "github", "calendar", "stock-tracker"] },
    "tools": { "count": 12, "names": ["system-info", "process-manager", "notification"] }
  }
}
```

#### 管理接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/dashboard` | 系统概览（Agent/渠道/Skill/工具/会话/审计数量） |
| GET | `/api/admin/agents` | 查看所有已注册 Agent |
| GET | `/api/admin/agents/{name}` | 查看指定 Agent 详情 |
| GET | `/api/admin/channels` | 查看所有渠道及状态 |
| POST | `/api/admin/channels/{id}/start` | 启动指定渠道 |
| POST | `/api/admin/channels/{id}/stop` | 停止指定渠道 |
| GET | `/api/admin/skills` | 查看所有已注册 Skill |
| POST | `/api/admin/skills/{name}/execute` | 执行指定 Skill |
| DELETE | `/api/admin/skills/{name}` | 卸载指定 Skill |
| GET | `/api/admin/tools` | 查看所有已注册工具 |
| GET | `/api/admin/tools/categories` | 查看工具分类 |
| GET | `/api/admin/sessions` | 查看活跃会话数 |
| DELETE | `/api/admin/sessions/{userId}` | 移除指定用户会话 |
| GET | `/api/admin/audit` | 查看最近审计事件（支持 `?count=N`） |
| GET | `/api/admin/audit/user/{userId}` | 查看指定用户的审计事件 |

---

### 内置 Skills

| Skill | 说明 |
|-------|------|
| **WeatherSkill** | 天气查询 |
| **ReminderSkill** | 提醒设置 |
| **GitHubSkill** | GitHub 通知与仓库操作 |
| **CalendarSkill** | 日历管理 |
| **StockTrackerSkill** | 股票行情追踪 |

### 内置工具

| 工具 | 分类 | 说明 |
|------|------|------|
| **SystemInfoTool** | system | CPU/内存/磁盘/网络信息 |
| **ProcessManagerTool** | system | 进程列表与管理 |
| **NotificationTool** | system | 系统通知推送 |
| **ClipboardTool** | utility | 剪贴板读写 |
| **EmailTool** | utility | 邮件收发 |
| **CalendarTool** | utility | 日历事件管理 |

---

### 渠道接入指南

#### Webhook

默认启用，接收 HTTP POST 请求：

```bash
curl -X POST http://localhost:8080/api/webhook \
  -H "Content-Type: application/json" \
  -d '{"message": "你好", "userId": "user-001"}'
```

#### Telegram

1. 通过 [@BotFather](https://t.me/BotFather) 创建 Bot，获取 `bot-token` 和 `bot-username`
2. 在 `application.yml` 中配置：

```yaml
evox:
  assistant:
    telegram:
      enabled: true
      bot-token: YOUR_BOT_TOKEN
      bot-username: YOUR_BOT_USERNAME
```

#### 钉钉

1. 在[钉钉开放平台](https://open.dingtalk.com)创建企业内部应用，获取 `app-key`、`app-secret`、`robot-code`
2. 在 `application.yml` 中配置：

```yaml
evox:
  assistant:
    dingtalk:
      enabled: true
      app-key: YOUR_APP_KEY
      app-secret: YOUR_APP_SECRET
      robot-code: YOUR_ROBOT_CODE
```

---

### 架构概览

```
用户消息 (Telegram / 钉钉 / Webhook / Web UI)
    │
    ▼
┌──────────────────────┐
│  ChannelRegistry     │  ← 多渠道统一接入
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│  GatewayRouter       │  ← 认证 → 限流 → 会话恢复 → Agent 路由 → 审计
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│  Agent (IAgent)      │  ← 调用 Skills / Tools 处理请求
│  ├── SkillRegistry   │
│  └── ToolRegistry    │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│  TaskScheduler       │  ← 定时任务主动推送
└──────────────────────┘
```

---

### 依赖模块

| 模块 | 说明 |
|------|------|
| `evox-core` | 核心抽象（Agent、Message 等） |
| `evox-models` | LLM 模型接入 |
| `evox-agents` | Agent 实现与 Skill 体系 |
| `evox-tools` | 系统工具集 |
| `evox-memory` | 记忆管理 |
| `evox-channels` | 消息渠道（Webhook / Telegram / 钉钉） |
| `evox-scheduler` | 定时调度引擎 |
| `evox-gateway` | 统一网关（认证/限流/审计） |
