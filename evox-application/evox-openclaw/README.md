## EvoX OpenClaw — 自进化个人 AI 助手

EvoX OpenClaw 是 EvoX 框架的**开箱即用个人 AI 助手主应用**，整合了渠道接入、网关路由、调度引擎、Skills 插件、系统工具等核心能力，并具备 **Heartbeat 主动唤醒**、**Prompt 自进化**、**Skill 自动生成** 三大高级能力，提供类似 [OpenClaw](https://github.com/nicepkg/openclaw) 的个人 AI 助手体验。

### 核心能力

- **多渠道接入** — 支持 Webhook、Telegram、钉钉等消息渠道，统一消息收发
- **统一网关** — API Key 认证、会话管理、限流、审计日志，保障安全与可观测性
- **Skills 插件系统** — 内置天气、提醒、GitHub、日历、股票追踪等技能，支持动态注册/卸载
- **系统级工具** — 系统信息、进程管理、通知、剪贴板、邮件、日历等工具开箱即用
- **定时调度** — 支持 Cron / Interval 定时任务，自动执行并推送结果
- **Web UI** — 内置前端页面，提供可视化交互界面（含 Evolution 控制台）

### 🧬 三大进化能力（OpenClaw 对标）

| 能力 | 说明 | 对标 OpenClaw |
|------|------|---------------|
| **💓 Heartbeat 主动唤醒** | Agent 定期自动唤醒，处理积压的系统事件。支持定时心跳和 `wakeNow()` 立即唤醒 | Heartbeat 机制 |
| **🧠 Self-Evolution 自进化** | 收集用户反馈 → 基于进化算法优化 Agent 系统提示词 → 版本历史 + 回滚 | 自我改进 |
| **🛠️ Skill Generator 技能自动生成** | 用户描述需求 → Agent 自主生成 Skill 定义 → 动态安装到 SkillMarketplace | 自主编写代码 |

---

### 项目结构

```
evox-openclaw/
├── src/main/java/io/leavesfly/evox/assistant/
│   ├── AssistantApplication.java              # Spring Boot 启动入口
│   ├── AssistantBootstrap.java                # 生命周期引导（绑定 Agent → Channel、启动 Heartbeat/自进化）
│   ├── config/
│   │   ├── AssistantProperties.java           # 配置属性（evox.assistant.*）
│   │   └── AssistantAutoConfiguration.java    # 自动装配（网关、渠道、调度、Skills、工具、进化能力）
│   ├── controller/
│   │   ├── ChatController.java                # 对话 API（/api/chat）
│   │   ├── AdminController.java               # 管理 API（/api/admin/*）
│   │   ├── EvolutionController.java           # 进化能力 API（/api/evolution/*）
│   │   └── HealthController.java              # 健康检查（/api/health）
│   └── evolution/
│       ├── SelfEvolutionService.java           # Prompt 自进化服务
│       └── SkillGenerator.java                 # 技能自动生成器
├── src/main/resources/
│   ├── application.yml                        # 默认配置
│   └── static/
│       └── index.html                         # 内置 Web UI（含 Evolution 控制台）
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

    # 心跳主动唤醒
    heartbeat:
      enabled: true
      interval-ms: 300000          # 5 分钟
      initial-delay-ms: 10000      # 启动延迟 10 秒

    # 自进化（默认关闭，需积累反馈后开启）
    self-evolution:
      enabled: false
      optimization-interval-ms: 3600000
      min-feedback-for-optimization: 10
      improvement-threshold: 0.5

    # 技能自动生成
    skill-generator:
      enabled: true
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

#### 进化能力接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/evolution/status` | 获取所有进化能力的综合状态 |
| GET | `/api/evolution/heartbeat` | Heartbeat 详细状态 |
| POST | `/api/evolution/heartbeat/wake` | 手动触发立即唤醒 |
| POST | `/api/evolution/heartbeat/event` | 发送系统事件到事件队列 |
| GET | `/api/evolution/self-evolution` | 自进化统计信息 |
| POST | `/api/evolution/self-evolution/feedback` | 提交反馈信号 |
| GET | `/api/evolution/self-evolution/history` | Prompt 版本历史 |
| POST | `/api/evolution/self-evolution/rollback/{version}` | 回滚到指定版本 Prompt |
| POST | `/api/evolution/self-evolution/optimize` | 手动触发一轮优化 |
| GET | `/api/evolution/skill-generator` | Skill Generator 状态和已生成技能列表 |
| POST | `/api/evolution/skill-generator/generate` | 根据描述生成并安装新 Skill |
| DELETE | `/api/evolution/skill-generator/{skillName}` | 卸载已生成的 Skill |

#### 管理接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/dashboard` | 系统概览（含进化能力状态） |
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
┌──────────────────────────────────────────────┐
│  Agent (IAgent)                              │
│  ├── SkillRegistry (含 DynamicSkill)         │  ← 调用 Skills / Tools 处理请求
│  ├── ToolRegistry                            │
│  └── SkillGenerator → SkillMarketplace       │  ← 🛠️ 自主生成新 Skill
└──────────┬───────────────────────────────────┘
           │
     ┌─────┴─────┐
     ▼           ▼
┌──────────┐ ┌──────────────────────────┐
│ Scheduler│ │  HeartbeatRunner         │  ← 💓 定时心跳 + 立即唤醒
│          │ │  ├── SystemEventQueue    │
│          │ │  └── wakeNow()           │
└──────────┘ └──────────────────────────┘
                      │
                      ▼
             ┌──────────────────┐
             │ SelfEvolution    │  ← 🧠 收集反馈 → 优化 Prompt → 版本管理
             │ Service          │
             └──────────────────┘
```

---

---

### 🎓 演示指南（Demo Walkthrough）

启动应用后访问 `http://localhost:8080`，在 Web UI 中体验以下核心能力：

#### Demo 1：💓 Heartbeat 主动唤醒

1. 点击侧边栏 **🧬 Evolution** 进入进化控制台
2. 在 Heartbeat 区域查看心跳状态（Running / Total Heartbeats / Pending Events）
3. 点击 **⚡ Trigger Wake Now** 手动触发一次心跳
4. 点击 **📨 Send System Event** 发送一条系统事件（如 "用户请求生成日报"），观察 Pending Events 增加
5. 等待下次心跳或再次 Wake，观察事件被消费

```bash
# 也可通过 API 操作
curl -X POST http://localhost:8080/api/evolution/heartbeat/wake
curl -X POST http://localhost:8080/api/evolution/heartbeat/event \
  -H "Content-Type: application/json" \
  -d '{"source": "demo", "message": "Generate daily report", "wakeMode": "NOW"}'
```

#### Demo 2：🧠 Self-Evolution 自进化

1. 在 Evolution 页面的 Self-Evolution 区域，点击 **📝 Submit Feedback** 提交多条反馈
2. 反馈积累到阈值后（默认 10 条），点击 **🚀 Trigger Optimization** 触发优化
3. 点击 **📜 View Prompt History** 查看 Prompt 版本历史
4. 对比不同版本的 Prompt，可点击 **Rollback** 回滚到任意历史版本

```bash
# 提交反馈
curl -X POST http://localhost:8080/api/evolution/self-evolution/feedback \
  -H "Content-Type: application/json" \
  -d '{"type": "USER_RATING", "score": 0.9, "comment": "回答很准确"}'

# 查看 Prompt 版本历史
curl http://localhost:8080/api/evolution/self-evolution/history
```

#### Demo 3：🛠️ Skill Generator 技能自动生成

1. 在 Evolution 页面的 Skill Generator 区域，输入技能描述（如 "一个能够将中文翻译成英文的技能"）
2. 点击 **🪄 Generate Skill**，等待 Agent 生成并安装
3. 生成成功后，在 Generated Skills 表格中查看新技能
4. 切换到 **⚡ Skills** 页面，确认新技能已出现在 Skill 列表中
5. 可在 Chat 页面直接使用新技能，或在 Generated Skills 中点击 **Uninstall** 卸载

```bash
# 生成新技能
curl -X POST http://localhost:8080/api/evolution/skill-generator/generate \
  -H "Content-Type: application/json" \
  -d '{"description": "一个能够将中文翻译成英文的技能"}'

# 查看已生成技能
curl http://localhost:8080/api/evolution/skill-generator
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
| `evox-scheduler` | 定时调度引擎（含 Heartbeat） |
| `evox-gateway` | 统一网关（认证/限流/审计） |
| `evox-optimizers` | 进化算法优化器（Self-Evolution 依赖） |
| `evox-workflow` | 工作流引擎 |
