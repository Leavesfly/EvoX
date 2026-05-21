# 工具系统 (evox-tools)

## 概述

EvoX 提供强大的工具系统，包含 **20+ 内置工具类型**，支持灵活扩展和自定义。工具系统使 Agent 能够与外部环境交互，执行文件操作、网络请求、数据库查询等任务。

---

## 工具分类

以下是 EvoX 支持的内置工具类型：

| 工具名称 | 功能描述 | 典型用途 |
|----------|----------|----------|
| **FileSystemTool** | 文件读写操作 | 读取配置文件、写入日志 |
| **HttpTool** | HTTP 请求 | API 调用、网页抓取 |
| **DatabaseTool** | 数据库操作 | SQL 查询、数据更新 |
| **CalculatorTool** | 数学计算 | 数值运算、公式计算 |
| **SearchTool** | 搜索引擎 | 网络搜索、知识检索 |
| **BrowserTool** | 浏览器自动化 | 网页交互、截图 |
| **ShellTool** | Shell 命令执行 | 系统命令、脚本运行 |
| **EmailTool** | 邮件发送 | 通知、报告发送 |
| **GitTool** | Git 操作 | 代码提交、分支管理 |
| **GrepTool** | 正则表达式检索 | 文本搜索、模式匹配 |
| **ImageTool** | 图像处理 | 图片转换、OCR 识别 |
| **CodeInterpreterTool** | 代码解释器 | Python/JS 代码执行 |
| **CalendarTool** | 日历管理 | 日程安排、会议提醒 |
| **ClipboardTool** | 剪贴板操作 | 复制粘贴文本 |
| **DocumentTool** | 文档处理 | PDF/Word 解析 |
| **JsonTool** | JSON 处理 | JSON 格式化、验证 |
| **SystemTool** | 系统信息查询 | CPU/内存监控 |
| **TaskTool** | 任务管理 | 待办事项、进度跟踪 |
| **ProjectTool** | 项目分析 | 代码统计、依赖分析 |
| **AgentTool** | 子智能体调用 | 多 Agent 协作 |

---

## BaseTool 基类

所有工具都继承自 `BaseTool` 基类。

### 核心方法

```java
public abstract class BaseTool {
    // 工具名称（唯一标识）
    public abstract String getName();
    
    // 工具描述（用于 LLM 理解）
    public abstract String getDescription();
    
    // 工具参数 schema（JSON Schema 格式）
    public abstract String getParametersSchema();
    
    // 执行工具
    public abstract ToolResult execute(Map<String, Object> arguments);
}
```

### 自定义工具开发指南

**步骤 1：继承 BaseTool**

```java
public class WeatherTool extends BaseTool {
    private final String apiKey;
    
    public WeatherTool(String apiKey) {
        this.apiKey = apiKey;
    }
    
    @Override
    public String getName() {
        return "get_weather";
    }
    
    @Override
    public String getDescription() {
        return "获取指定城市的天气信息";
    }
    
    @Override
    public String getParametersSchema() {
        return "{" +
            "\"type\": \"object\"," +
            "\"properties\": {" +
            "  \"city\": {\"type\": \"string\", \"description\": \"城市名称\"}" +
            "}," +
            "\"required\": [\"city\"]" +
            "}";
    }
    
    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        String city = (String) arguments.get("city");
        // 调用天气 API
        String weatherData = fetchWeather(city, apiKey);
        return ToolResult.success(weatherData);
    }
}
```

**步骤 2：注册工具**

```java
ToolRegistry registry = new ToolRegistry();
registry.register(new WeatherTool("your-api-key"));
```

---

## ToolAwareAgent 集成工具

`ToolAwareAgent` 是支持工具调用的智能体。

### 使用示例

```java
// 1. 创建工具注册表
ToolRegistry toolRegistry = new ToolRegistry();
toolRegistry.register(new FileSystemTool());
toolRegistry.register(new HttpTool());
toolRegistry.register(new CalculatorTool());

// 2. 创建 LLM 服务
LLMService llm = new OpenAIService(apiKey);

// 3. 创建 ToolAwareAgent
ToolAwareAgent agent = new ToolAwareAgent(llm, toolRegistry);

// 4. 执行任务（Agent 会自动选择并调用工具）
String result = agent.execute("帮我查询北京今天的天气，并计算 123 * 456");

System.out.println("结果：" + result);
```

### 工作流程

```
用户请求
    ↓
LLM 分析意图
    ↓
选择合适工具
    ↓
执行工具调用
    ↓
获取工具结果
    ↓
生成最终回答
```

---

## MCP 协议支持

EvoX 支持 **Model Context Protocol (MCP)**，实现工具的标准化暴露和远程调用。

### MCPServer - 注册工具并启动服务

```java
// 1. 创建 MCP 服务器
MCPServer server = new MCPServer("my-tools-server", 8080);

// 2. 注册工具
server.registerTool(new FileSystemTool());
server.registerTool(new HttpTool());
server.registerTool(new DatabaseTool());

// 3. 启动服务
server.start();

System.out.println("MCP Server started on port 8080");
```

### MCPClient - 远程调用工具

```java
// 1. 连接 MCP 服务器
MCPClient client = new MCPClient("localhost", 8080);

// 2. 列出可用工具
List<String> tools = client.listTools();
System.out.println("可用工具: " + tools);

// 3. 调用远程工具
Map<String, Object> params = Map.of("path", "/etc/config.yaml");
ToolResult result = client.callTool("read_file", params);

System.out.println("文件内容: " + result.getData());
```

### MCP 优势

- **标准化接口**：统一的工具调用协议
- **跨语言支持**：任何支持 MCP 的客户端都可调用
- **动态发现**：自动发现可用工具列表
- **权限控制**：支持细粒度的工具访问控制

---

## 技能系统

技能系统是工具的高级抽象，封装复杂的工作流。

### 核心组件

| 组件 | 说明 |
|------|------|
| **BaseSkill** | 技能基类 |
| **SkillDefinitionFile** | 技能定义文件（YAML/JSON） |
| **SkillLoader** | 技能加载器 |
| **SkillRegistry** | 技能注册表 |
| **SkillMarketplace** | 技能市场（在线技能库） |
| **SkillTool** | 将技能包装为工具 |

### 技能定义示例

```yaml
# skills/data-analysis.yml
name: data_analysis
description: "执行数据分析任务"
version: "1.0.0"

parameters:
  - name: dataset_path
    type: string
    required: true
    description: "数据集文件路径"
  - name: analysis_type
    type: string
    required: false
    default: "summary"
    description: "分析类型：summary/correlation/distribution"

steps:
  - tool: read_file
    args:
      path: "{{dataset_path}}"
  - tool: code_interpreter
    args:
      language: python
      code: |
        import pandas as pd
        df = pd.read_csv("{{dataset_path}}")
        if "{{analysis_type}}" == "summary":
            print(df.describe())
        elif "{{analysis_type}}" == "correlation":
            print(df.corr())
```

### 加载和使用技能

```java
// 1. 加载技能
SkillLoader loader = new SkillLoader();
BaseSkill skill = loader.loadFromFile("skills/data-analysis.yml");

// 2. 注册技能
SkillRegistry registry = new SkillRegistry();
registry.register(skill);

// 3. 将技能转换为工具
SkillTool skillTool = new SkillTool(skill);
toolRegistry.register(skillTool);

// 4. Agent 调用技能
String result = agent.execute("分析 /data/sales.csv 的数据摘要");
```

### 从技能市场安装技能

```java
SkillMarketplace marketplace = new SkillMarketplace("https://skills.evox.io");

// 搜索技能
List<SkillDefinition> results = marketplace.search("data visualization");

// 安装技能
BaseSkill skill = marketplace.install("chart-generator", "1.2.0");
registry.register(skill);
```

---

## 工具安全性

### 超时控制

防止工具执行时间过长导致阻塞。

```java
// 设置工具执行超时
ToolConfig config = ToolConfig.builder()
    .timeout(30, TimeUnit.SECONDS)  // 30秒超时
    .maxRetries(3)                   // 最多重试3次
    .build();

ToolExecutor executor = new ToolExecutor(config);
ToolResult result = executor.execute(tool, arguments);
```

### 权限限制

限制工具的访问范围和操作权限。

```java
// 文件系统工具权限配置
FileSystemTool fileTool = new FileSystemTool();
fileTool.setAllowedPaths(List.of("/workspace", "/tmp"));  // 仅允许访问这些目录
fileTool.setReadOnly(true);  // 只读模式，禁止写入

// Shell 工具权限配置
ShellTool shellTool = new ShellTool();
shellTool.setAllowedCommands(List.of("ls", "cat", "grep"));  // 白名单命令
shellTool.setBlockedCommands(List.of("rm", "sudo", "chmod"));  // 黑名单命令
```

### 沙箱执行

对于高风险工具（如代码解释器），在沙箱环境中执行。

```java
// 代码解释器沙箱配置
CodeInterpreterTool interpreter = new CodeInterpreterTool();
interpreter.setSandboxEnabled(true);
interpreter.setMaxMemoryMB(512);       // 最大内存 512MB
interpreter.setMaxCpuTimeSeconds(10);  // 最大 CPU 时间 10秒
interpreter.setNetworkAccess(false);   // 禁止网络访问
```

---

## Maven 依赖

```xml
<dependency>
    <groupId>io.leavesfly.evox</groupId>
    <artifactId>evox-tools</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

---

## 最佳实践

1. **工具命名**：使用清晰的动词+名词格式，如 `read_file`、`send_email`
2. **参数验证**：在执行前验证所有必需参数
3. **错误处理**：返回结构化的错误信息，便于 Agent 理解
4. **幂等性**：尽量设计为幂等操作，避免副作用
5. **日志记录**：记录工具调用的输入输出，便于调试
6. **资源清理**：确保工具执行后释放所有资源（文件句柄、网络连接等）
