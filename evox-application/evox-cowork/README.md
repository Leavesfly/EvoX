# EvoX Cowork

**Cowork** 是基于 EvoX 框架构建的智能知识工作桌面应用，参考 [OpenWork](https://github.com/different-ai/openwork) 架构设计，将 Agentic 工作流从命令行扩展到桌面端，面向非编程的知识工作场景。

## 核心架构

Cowork 采用 **后端服务 + 桌面 UI** 的双层架构：
- **后端**：Spring Boot 服务，提供完整的 REST API + SSE 实时事件流
- **桌面 GUI**：基于 JavaFX 的原生桌面应用，参考 OpenWork 的交互设计
- **前端**：也可对接 Web UI 框架（Tauri/Electron/Web）

### 运行模式

- **Desktop 模式**：通过 `CoworkDesktopApp` 启动 JavaFX 原生桌面应用，直接调用后端 Java 类
- **Host 模式**：通过 `CoworkApplication` 启动 Spring Boot 服务器（默认 `127.0.0.1:8090`），提供 REST API
- **Client 模式**：连接远程 Cowork 服务器，适用于移动端或远程协作

## 核心功能

### 💬 Session 管理
- 每个任务映射为一个 **Session**，支持创建/切换/列表/中止/摘要
- 会话隔离：每个 Session 拥有独立的 Agent、Context 和 ToolRegistry
- 自动标题生成：第一条用户消息自动成为会话标题

### 📡 SSE 实时事件流
- 通过 Server-Sent Events 实现实时 UI 更新
- 事件类型：流式响应、工具执行进度、权限请求、会话更新、进度通知
- 支持多客户端同时订阅

### 🔐 交互式权限审批
- 权限请求通过 SSE 推送到 UI，用户可选择：
  - **Once**：仅本次允许
  - **Always**：本会话始终允许
  - **Reject**：拒绝
- 5 分钟超时自动拒绝

### 📝 工作流模板
- 5 个内置模板：每日简报、文件整理、研究报告、数据分析、会议纪要
- 支持 `{{variable}}` 占位符，可自定义模板
- 模板持久化存储，支持搜索和分类

### 📁 Workspace 管理
- 项目文件夹选择和管理
- 支持置顶、最近访问排序
- 工作区级别的设置覆盖

### 🤖 智能任务执行引擎 (CoworkAgent)
- 基于 Function Calling 循环的任务执行引擎
- Sub-agent 协调，支持复杂任务的并行分解与执行

### 🔌 插件系统
- 内置插件：Productivity、Data Analysis、Research、Document Management
- 支持 YAML/JSON 格式的自定义插件和 Slash Commands

### 🔗 连接器系统
- 内置连接器：LocalFileConnector、WebConnector
- 网络访问白名单控制

## REST API

### Session API
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/cowork/sessions` | 创建会话 |
| GET | `/api/cowork/sessions` | 列出所有会话 |
| GET | `/api/cowork/sessions/{id}` | 获取会话详情 |
| GET | `/api/cowork/sessions/{id}/messages` | 获取会话消息 |
| POST | `/api/cowork/sessions/{id}/prompt` | 发送提示词 |
| POST | `/api/cowork/sessions/{id}/abort` | 中止会话 |
| GET | `/api/cowork/sessions/{id}/summarize` | 获取会话摘要 |
| POST | `/api/cowork/sessions/{id}/switch` | 切换活跃会话 |
| DELETE | `/api/cowork/sessions/{id}` | 删除会话 |

### Event API
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/cowork/events` | 订阅 SSE 事件流 |

### Permission API
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/cowork/permissions/pending` | 获取待审批权限 |
| POST | `/api/cowork/permissions/{id}/reply` | 回复权限请求 |

### Workspace / Template / Plugin / Connector / Task / Config API
| 方法 | 路径 | 说明 |
|------|------|------|
| GET/POST | `/api/cowork/workspaces` | 工作区管理 |
| GET/POST | `/api/cowork/templates` | 模板管理 |
| GET | `/api/cowork/plugins` | 插件管理 |
| GET | `/api/cowork/connectors` | 连接器管理 |
| GET/POST | `/api/cowork/tasks` | 任务管理 |
| GET | `/api/cowork/config` | 配置查询 |
| GET | `/api/cowork/health` | 健康检查 |
| GET | `/api/cowork/status` | 系统状态 |

## 架构设计

```
evox-cowork/
├── agent/          # CoworkAgent 核心引擎
├── api/            # REST API 控制器 (CoworkController)
├── config/         # 配置体系 (CoworkConfig)
├── context/        # 工作上下文管理 (CoworkContext)
├── connector/      # 连接器系统
│   └── builtin/    # 内置连接器 (LocalFile, Web)
├── event/          # SSE 事件总线 (CoworkEventBus)
├── permission/     # 权限管理 (Interactive + Base)
├── plugin/         # 插件系统 (Plugin, Loader, Manager)
├── session/        # Session 管理 (CoworkSession, SessionManager)
├── task/           # 任务管理 (Task, Decomposer, Manager)
├── template/       # 工作流模板 (Template, Manager)
├── tool/           # 工具注册中心 (CoworkToolRegistry)
├── ui/             # JavaFX 桌面 GUI
│   ├── CoworkDesktopApp.java      # JavaFX 应用入口
│   ├── CoworkMainLayout.java      # 主布局 (BorderPane)
│   ├── CoworkServiceBridge.java   # UI ↔ 后端桥接层
│   ├── SidebarPanel.java          # 左侧边栏 (会话/工作区/模板)
│   ├── ChatPanel.java             # 聊天消息区 + 输入区
│   └── PermissionCardView.java    # 权限审批卡片
└── workspace/      # 工作区管理 (Workspace, Manager)
```

### JavaFX 桌面 GUI

桌面 GUI 参考 [OpenWork](https://github.com/different-ai/openwork) 的交互设计，采用暗色主题：

| 组件 | 说明 |
|------|------|
| `CoworkDesktopApp` | JavaFX Application 入口，初始化服务桥接层和主窗口 |
| `CoworkServiceBridge` | UI 与后端服务的桥接层，封装异步调用和线程分发 |
| `CoworkMainLayout` | BorderPane 主布局：左侧边栏 + 右侧聊天区 |
| `SidebarPanel` | 会话列表、工作区管理、模板快捷入口 |
| `ChatPanel` | 消息气泡展示、流式输出、权限审批卡片、输入框 |
| `PermissionCardView` | 内嵌在聊天流中的权限审批卡片 (Allow Once / Always / Deny) |

**UI 特性：**
- 🎨 暗色主题（深色背景 + 紫色强调色）
- 💬 消息气泡（用户蓝色右对齐，AI 灰色左对齐）
- ⚡ 流式输出实时追加（带光标动画和 "Thinking..." 指示器）
- 🔐 权限审批卡片内嵌在聊天流中
- 📁 工作区管理（DirectoryChooser 选择文件夹，右键菜单 Pin/Remove）
- 📝 模板快捷入口（点击模板自动填入输入框）
- ⌨️ 快捷键（Enter 发送，Shift+Enter 换行）

## 复用的 EvoX 模块

| EvoX 模块 | 复用内容 |
|-----------|---------|
| evox-core | Message、BaseModule 等核心抽象 |
| evox-models | BaseLLM、LLMFactory、LLMConfig |
| evox-actions | 动作执行引擎 |
| evox-agents | SkillRegistry、TeamFramework |
| evox-capability | 工具集（File、Shell、Grep、HTTP、Search、Browser、Image、Document）、MemoryManager |
| evox-workflow | WorkflowGraph、WorkflowExecutor |
| evox-rag | RAG 检索增强 |
| evox-mcp | MCP 协议支持 |

## 快速开始

### 桌面应用（推荐）

```bash
# 构建
mvn clean package -pl evox-application/evox-cowork -am

# 运行桌面 GUI
java --module-path /path/to/javafx-sdk/lib \
     --add-modules javafx.controls,javafx.fxml \
     -cp evox-application/evox-cowork/target/evox-cowork-1.0.0-SNAPSHOT.jar \
     io.leavesfly.evox.cowork.ui.CoworkDesktopApp
```

### REST API 模式

```bash
# 运行 Spring Boot 服务
java -jar evox-application/evox-cowork/target/evox-cowork-1.0.0-SNAPSHOT.jar

# 创建会话
curl -X POST http://localhost:8090/api/cowork/sessions

# 发送提示词
curl -X POST http://localhost:8090/api/cowork/sessions/{sessionId}/prompt \
  -H "Content-Type: application/json" \
  -d '{"message": "帮我整理 Downloads 文件夹"}'

# 订阅实时事件
curl -N http://localhost:8090/api/cowork/events
```

## 配置

通过环境变量配置 LLM：
```bash
export OPENAI_API_KEY=your-key        # OpenAI
export DASHSCOPE_API_KEY=your-key     # 阿里云通义千问
```
