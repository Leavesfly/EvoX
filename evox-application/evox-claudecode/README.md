# EvoX ClaudeCode

基于 EvoX 实现的 **Claude Code 风格编程助手**：通过 Agent + 工具（文件系统、代码解释器、Shell 命令）实现读代码库、执行命令、修改文件、运行测试等能力。

## 可行性说明

**可行。** Claude Code 的核心是“Agent + 编程相关工具”的编排：

- **读代码库**：使用 EvoX 的 `FileSystemTool`（list/read）
- **修改文件**：使用 `FileSystemTool`（write/append）
- **执行命令**：本模块提供的 `ShellCommandTool`（mvn、git、npm、python 等）
- **运行代码片段**：使用 EvoX 的 `CodeInterpreterTool`（JavaScript/Groovy/Python）

本模块在 `evox-application` 下将上述能力封装为 **ClaudeCodeAgent**，并提供一个控制台运行入口 **ClaudeCodeRunner**，便于在本地项目目录中与助手交互。

## 模块结构

```
evox-claudecode/
├── pom.xml
├── README.md
└── src/main/java/io/leavesfly/evox/claudecode/
    ├── ClaudeCodeRunner.java          # 控制台入口
    ├── agent/
    │   └── ClaudeCodeAgent.java       # 编程助手 Agent 工厂
    └── tools/
        └── ShellCommandTool.java      # Shell 命令执行工具
```

## 依赖

- evox-core, evox-models, evox-actions, evox-agents, evox-workflow, evox-capability

## 使用方式

### 1. 在代码中创建 ClaudeCode Agent

```java
import io.leavesfly.evox.claudecode.agent.ClaudeCodeAgent;
import io.leavesfly.evox.agents.specialized.ToolAwareAgent;
import io.leavesfly.evox.models.config.OpenAILLMConfig;
import io.leavesfly.evox.models.openai.OpenAILLM;

// 项目根目录
String projectRoot = "/path/to/your/project";
OpenAILLMConfig config = OpenAILLMConfig.builder()
    .apiKey(System.getenv("OPENAI_API_KEY"))
    .model("gpt-4o-mini")
    .temperature(0.2f)
    .build();
BaseLLM llm = new OpenAILLM(config);

ToolAwareAgent agent = ClaudeCodeAgent.create(projectRoot, config, llm);

// 执行用户请求（例如："list files in src/main/java"）
Message response = agent.execute(null, List.of(
    Message.builder().messageType(MessageType.INPUT).content("List files in src/main/java").build()));
```

### 2. 运行控制台入口

```bash
# 设置 API Key 后运行（默认使用当前目录为项目根）
export OPENAI_API_KEY=sk-xxx
mvn -pl evox-application/evox-claudecode exec:java -Dexec.mainClass="io.leavesfly.evox.claudecode.ClaudeCodeRunner"

# 或指定项目根
mvn -pl evox-application/evox-claudecode exec:java -Dexec.mainClass="io.leavesfly.evox.claudecode.ClaudeCodeRunner" -Dexec.args="/path/to/project"
```

在控制台输入自然语言指令，例如：

- “list files in src”
- “read the content of pom.xml”
- “run mvn test”
- “create a file hello.txt with content Hello World”

## 工具说明

| 工具 | 说明 |
|------|------|
| **file_system** | 读/写/追加/删除/列出文件，创建目录；支持常见代码与配置文件扩展名。 |
| **code_interpreter** | 在沙箱中执行 JavaScript/Groovy/Python 片段，适合快速计算或脚本验证。 |
| **shell_command** | 在项目目录下执行 shell 命令（mvn、git、npm、python 等），可配置超时与工作目录。 |

## 安全与限制

- **Shell 命令**：默认允许任意命令（`allowAllCommands=true`），生产或共享环境建议改为白名单（仅允许 mvn、git、npm 等）或由人工审批。
- **文件系统**：工作目录与扩展名白名单已做限制，避免随意读写系统文件。
- **代码解释器**：在沙箱内执行，超时 30 秒。

## 扩展建议

- 接入更多 LLM（如 Claude、本地模型）：实现/配置对应的 `LLMConfig` 与 `BaseLLM`，再传给 `ClaudeCodeAgent.create`。
- 增加 Git 专用工具：封装 `git status/diff/add/commit` 等，便于 Agent 理解版本变更。
- 与 EvoX Workflow / MCP 集成：将 ClaudeCode Agent 作为工作流节点或 MCP 工具暴露给 IDE/其他应用。
