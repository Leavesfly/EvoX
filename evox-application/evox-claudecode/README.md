# EvoX ClaudeCode

基于 EvoX 框架构建的 **Agentic 编码 CLI 工具**，提供类似 Claude Code 的终端交互式编程体验。

## 功能特性

- **🖥️ 终端 REPL 交互** - 命令行对话式编程，支持 JLine3 增强终端
- **📁 文件操作** - 读取、创建、精确编辑（diff 式替换）、删除文件
- **⚡ Shell 命令执行** - 在项目目录下执行任意 Shell 命令，支持超时控制
- **🔍 代码搜索** - Grep（正则文本搜索）和 Glob（文件路径搜索）
- **🔧 Git 操作** - 状态查看、提交、分支管理、日志查看
- **📊 项目感知** - 自动识别项目类型、扫描目录结构
- **🔐 权限控制** - 危险操作需用户确认，支持会话级批准
- **🤖 Function Calling 循环** - LLM → 工具调用 → 结果反馈 → 继续推理
- **📝 项目规则** - 支持 `CLAUDE.md` 项目级指令文件
- **🔌 多模型支持** - OpenAI、阿里云通义千问、Ollama、SiliconFlow

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
```

### 4. 交互命令

| 命令 | 说明 |
|------|------|
| `/help` | 显示帮助信息 |
| `/clear` | 清除对话历史 |
| `/compact` | 压缩对话历史 |
| `/tools` | 列出可用工具 |
| `/context` | 显示项目上下文 |
| `/quit` | 退出 |

## 架构设计

```
evox-claudecode/
├── cli/                    # CLI 交互层
│   ├── ClaudeCodeRepl      # REPL 循环（JLine3）
│   └── CliRenderer         # 终端渲染（ANSI 着色）
├── agent/                  # 智能体层
│   └── CodingAgent         # 编码 Agent（Function Calling 循环）
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
| `evox-core` | 核心抽象（Message、BaseModule） |
| `evox-models` | LLM 模型适配（OpenAI、阿里云、Ollama） |
| `evox-actions` | Action 引擎 |
| `evox-agents` | Agent 框架 |
| `evox-capability` | 工具集（文件、Shell、搜索、Git） |
| `evox-mcp` | MCP 协议支持 |

### 新增到 evox-capability 的工具

| 工具 | 包路径 | 说明 |
|------|--------|------|
| `ShellTool` | `tools.shell` | Shell 命令执行 |
| `GrepTool` | `tools.grep` | 正则文本搜索 |
| `GlobTool` | `tools.grep` | 文件路径搜索 |
| `FileEditTool` | `tools.file` | Diff 式精确编辑 |
| `GitTool` | `tools.git` | Git 操作 |
| `ProjectContextTool` | `tools.project` | 项目结构分析 |
