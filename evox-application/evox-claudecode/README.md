# EvoX ClaudeCode

基于 EvoX 框架构建的 **Agentic 编码 CLI 工具**，提供类似 Claude Code 的终端交互式编程体验。

## 功能特性

### 核心能力

- **🖥️ 终端 REPL 交互** — 命令行对话式编程，支持 JLine3 增强终端
- **📁 文件操作** — 读取、创建、精确编辑（diff 式替换、多处替换、行号定位）、删除文件
- **⚡ Shell 命令执行** — 在项目目录下执行任意 Shell 命令，支持超时控制
- **🔍 代码搜索** — Grep（正则文本搜索）和 Glob（文件路径搜索）
- **🔧 Git 操作** — status、diff、log、add、commit、branch、checkout、show、blame、push、pull、fetch、stash、merge、rebase、tag
- **📊 项目感知** — 自动识别项目类型、扫描目录结构
- **🔐 权限控制** — 危险操作需用户确认，支持会话级批准
- **📝 项目规则** — 支持 `CLAUDE.md` 项目级指令文件
- **🔌 多模型支持** — OpenAI、阿里云通义千问、Ollama、SiliconFlow

### 智能体能力

- **🤖 流式 Function Calling** — LLM 原生工具调用 + 实时流式输出 + ToolCall 增量拼接
- **⚡ 并行工具调用** — 多个工具调用自动并行执行（CompletableFuture）
- **🧠 智能 Compact** — LLM 摘要压缩对话历史，保留关键上下文
- **📦 自动上下文管理** — 基于 token 估算自动触发 compact，防止超出上下文窗口
- **🔄 错误重试与自愈** — LLM 请求失败自动重试（最多 2 次），工具执行失败自动重试（最多 1 次）
- **👥 子代理委派** — 复杂多步骤任务自动委派给子 Agent 并行执行
- **🔌 MCP 协议集成** — 动态加载外部 MCP 工具服务器（SSE/STDIO/Local）

### 终端体验

- **🎨 Markdown 渲染** — 流式 Markdown 终端渲染（代码块、标题、粗体、列表、引用）
- **🎛️ 丰富配置** — 支持 `--max-tokens`、`--temperature`、`--context-window`、`--no-color`、`--no-markdown` 等 CLI 参数
- **💾 会话持久化** — 自动保存/恢复会话，支持 `/sessions`、`/resume`、`/save`

## 快速开始

### 1. 构建

```bash
cd /path/to/evox
mvn clean package -DskipTests -pl evox-application/evox-claudecode -am
```

### 2. 配置 API Key

```bash
# OpenAI
export OPENAI_API_KEY=your-key

# 或阿里云通义千问
export DASHSCOPE_API_KEY=your-key
```

### 3. 运行

```bash
# 交互模式（默认使用 OpenAI gpt-4o）
java -jar evox-application/evox-claudecode/target/evox-claudecode-1.0.0-SNAPSHOT.jar

# 使用阿里云通义千问
java -jar evox-claudecode.jar --provider aliyun

# 使用本地 Ollama
java -jar evox-claudecode.jar --provider ollama --model llama3

# 单次执行模式
java -jar evox-claudecode.jar -p "fix the bug in Main.java"

# 自定义参数
java -jar evox-claudecode.jar --max-tokens 4096 --temperature 0.3 --context-window 200000
```

### 4. 交互命令

| 命令 | 说明 |
|------|------|
| `/help` | 显示帮助信息 |
| `/clear` | 清除对话历史并开始新会话 |
| `/compact` | 压缩对话历史（LLM 摘要） |
| `/tools` | 列出可用工具 |
| `/skills` | 列出可用技能 |
| `/context` | 显示项目上下文 |
| `/sessions` | 列出已保存的会话 |
| `/resume <id>` | 恢复指定会话 |
| `/save` | 保存当前会话 |
| `/usage` | 显示 Token 使用统计 |
| `/mcp connect <name> <url>` | 连接 MCP 服务器（SSE） |
| `/mcp connect-stdio <name> <cmd>` | 连接 MCP 服务器（STDIO） |
| `/mcp disconnect <name>` | 断开 MCP 服务器 |
| `/mcp list` | 列出已连接的 MCP 服务器 |
| `/quit` | 退出 |

### 5. CLI 参数

| 参数 | 说明 |
|------|------|
| `--provider <name>` | LLM 提供商（openai/aliyun/ollama/siliconflow） |
| `--model <name>` | 模型名称 |
| `--api-key <key>` | API 密钥 |
| `-p <prompt>` | 单次执行模式 |
| `--resume <id>` | 恢复指定会话 |
| `--max-tokens <n>` | 最大输出 token 数 |
| `--temperature <f>` | 温度参数（0.0-2.0） |
| `--top-p <f>` | Top-P 采样参数 |
| `--context-window <n>` | 上下文窗口大小 |
| `--max-iterations <n>` | 最大工具调用迭代次数 |
| `--no-approval` | 禁用权限确认 |
| `--no-color` | 禁用终端颜色 |
| `--no-markdown` | 禁用 Markdown 渲染 |

## 架构设计

```
evox-claudecode/
├── cli/                    # CLI 交互层
│   ├── ClaudeCodeRepl      # REPL 循环（JLine3）
│   ├── CliRenderer         # 终端渲染（ANSI 着色 + Markdown）
│   └── MarkdownStreamRenderer  # 流式 Markdown 渲染器
├── agent/                  # 智能体层
│   └── CodingAgent         # 编码 Agent（Function Calling 循环）
├── mcp/                    # MCP 集成层
│   ├── MCPConnectionManager    # MCP 连接管理
│   └── MCPToolBridge       # MCP 工具桥接
├── tool/                   # 工具注册层
│   └── ToolRegistry        # 工具注册中心
├── config/                 # 配置层
│   └── ClaudeCodeConfig    # 配置管理
├── context/                # 上下文层
│   └── ProjectContext      # 项目上下文
├── permission/             # 权限层
│   └── PermissionManager   # 权限管理
└── ClaudeCodeApplication   # 应用入口
```

### 依赖的 EvoX 模块

| 模块 | 用途 |
|------|------|
| `evox-core` | 核心抽象（Message、BaseModule、ILLM） |
| `evox-models` | LLM 模型适配（OpenAI、阿里云、Ollama、SiliconFlow） |
| `evox-actions` | Action 引擎 |
| `evox-agents` | Agent 框架（SubAgentTool） |
| `evox-tools` | 工具集（文件、Shell、搜索、Git） |
| `evox-mcp` | MCP 协议定义 |
| `evox-mcp-runtime` | MCP 运行时（Client、Transport） |
| `evox-memory` | 记忆管理（短期记忆、会话持久化） |

### 工具列表

| 工具 | 说明 |
|------|------|
| `file_system` | 文件读写、目录操作 |
| `file_edit` | Diff 式精确编辑（支持多处替换、行号定位） |
| `shell` | Shell 命令执行（超时控制、安全拦截） |
| `grep` | 正则文本搜索 |
| `glob` | 文件路径搜索 |
| `git` | Git 操作（16 种操作：status/diff/log/add/commit/branch/checkout/show/blame/push/pull/fetch/stash/merge/rebase/tag） |
| `project_context` | 项目结构分析 |
| `sub_agent` | 子代理任务委派 |
| `mcp_*` | 动态加载的 MCP 外部工具 |
