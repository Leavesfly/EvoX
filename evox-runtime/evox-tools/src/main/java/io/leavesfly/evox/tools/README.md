# EvoX Tools 工具集成模块

## 📦 模块定位

**层级**: 能力层 (Capability Layer)  
**职责**: 提供丰富的工具集成,扩展Agent的能力边界  
**依赖**: evox-core

## 🎯 核心功能

evox-tools 为 EvoX 框架提供了丰富的工具集成能力,让智能体能够与外部系统交互,执行各种实际操作,如文件管理、HTTP请求、数据库访问、网络搜索等。

### 工具体系总览

| 工具类型 | 实现类 | 功能描述 | 状态 |
|---------|--------|---------|------|
| **文件系统** | `FileSystemTool` | 文件读写、目录管理 | ✅ 完成 |
| **HTTP** | `HttpTool` | HTTP请求、API调用 | ✅ 完成 |
| **数据库** | `DatabaseTool` | SQL查询、数据操作 | ✅ 完成 |
| **网络搜索** | `WebSearchTool` | 网页搜索、信息检索 | ✅ 完成 |
| **计算器** | `CalculatorTool` | 数学计算、表达式求值 | ✅ 完成 |
| **JSON处理** | `JsonTool` | JSON解析、格式化 | ✅ 完成 |
| **图像处理** | `ImageTool` | 图像分析、处理 | 🚧 规划中 |
| **浏览器** | `BrowserTool` | 网页浏览、抓取 | 🚧 规划中 |

### 1. BaseTool 基础抽象

所有工具的统一接口:

```java
public interface BaseTool {
    // 工具名称
    String getName();
    
    // 工具描述
    String getDescription();
    
    // 执行工具
    ToolResult execute(Map<String, Object> params);
    
    // 工具Schema(用于Function Calling)
    Map<String, Object> getToolSchema();
}
```

**ToolResult 结果封装**:

```java
public class ToolResult {
    private boolean success;        // 执行是否成功
    private Object data;            // 返回数据
    private String error;           // 错误信息
    
    public static ToolResult success(Object data);
    public static ToolResult failure(String error);
}
```

### 2. FileSystemTool (文件系统工具)

文件和目录操作:

**支持的操作**:
- `write`: 写入文件
- `read`: 读取文件
- `append`: 追加内容
- `delete`: 删除文件
- `list`: 列出目录
- `exists`: 检查存在
- `mkdir`: 创建目录

```java
FileSystemTool fileTool = new FileSystemTool();

// 写入文件
Map<String, Object> writeParams = Map.of(
    "operation", "write",
    "filePath", "/tmp/test.txt",
    "content", "Hello EvoX!"
);
ToolResult result = fileTool.execute(writeParams);

// 读取文件
Map<String, Object> readParams = Map.of(
    "operation", "read",
    "filePath", "/tmp/test.txt"
);
ToolResult readResult = fileTool.execute(readParams);
String content = (String) ((Map)readResult.getData()).get("content");

// 列出目录
Map<String, Object> listParams = Map.of(
    "operation", "list",
    "directory", "/tmp"
);
ToolResult listResult = fileTool.execute(listParams);

// 删除文件
Map<String, Object> deleteParams = Map.of(
    "operation", "delete",
    "filePath", "/tmp/test.txt"
);
fileTool.execute(deleteParams);
```

### 3. HttpTool (HTTP工具)

HTTP请求和API调用:

**支持的方法**:
- `GET`: 获取资源
- `POST`: 创建资源
- `PUT`: 更新资源
- `DELETE`: 删除资源
- `PATCH`: 部分更新

```java
HttpTool httpTool = new HttpTool();

// GET 请求
Map<String, Object> getParams = Map.of(
    "method", "GET",
    "url", "https://api.example.com/users"
);
ToolResult result = httpTool.execute(getParams);

// POST 请求
Map<String, Object> postParams = Map.of(
    "method", "POST",
    "url", "https://api.example.com/users",
    "body", "{\"name\": \"Alice\", \"age\": 30}",
    "headers", Map.of("Content-Type", "application/json")
);
ToolResult postResult = httpTool.execute(postParams);

// 获取响应
Map<String, Object> responseData = (Map) postResult.getData();
int statusCode = (int) responseData.get("status_code");
String body = (String) responseData.get("body");
Map<String, String> headers = (Map) responseData.get("headers");
```

### 4. DatabaseTool (数据库工具)

SQL查询和数据操作:

```java
DatabaseTool dbTool = new DatabaseTool(dataSource);

// 查询数据
Map<String, Object> queryParams = Map.of(
    "operation", "query",
    "sql", "SELECT * FROM users WHERE age > ?",
    "params", List.of(25)
);
ToolResult result = dbTool.execute(queryParams);
List<Map<String, Object>> rows = (List) result.getData();

// 插入数据
Map<String, Object> insertParams = Map.of(
    "operation", "execute",
    "sql", "INSERT INTO users (name, age) VALUES (?, ?)",
    "params", List.of("Bob", 28)
);
dbTool.execute(insertParams);

// 更新数据
Map<String, Object> updateParams = Map.of(
    "operation", "execute",
    "sql", "UPDATE users SET age = ? WHERE name = ?",
    "params", List.of(29, "Bob")
);
dbTool.execute(updateParams);
```

### 5. WebSearchTool (网络搜索工具)

网页搜索和信息检索:

```java
WebSearchTool searchTool = new WebSearchTool();

// 搜索
Map<String, Object> searchParams = Map.of(
    "query", "EvoX AI framework",
    "num_results", 5
);
ToolResult result = searchTool.execute(searchParams);

List<Map<String, Object>> results = (List) result.getData();
for (Map<String, Object> item : results) {
    System.out.println("标题: " + item.get("title"));
    System.out.println("URL: " + item.get("url"));
    System.out.println("摘要: " + item.get("snippet"));
}
```

### 6. CalculatorTool (计算器工具)

数学计算和表达式求值:

```java
CalculatorTool calcTool = new CalculatorTool();

// 简单计算
Map<String, Object> params = Map.of(
    "expression", "2 + 3 * 4"
);
ToolResult result = calcTool.execute(params);
double value = (double) result.getData(); // 14.0

// 复杂表达式
Map<String, Object> complexParams = Map.of(
    "expression", "sqrt(16) + pow(2, 3)"
);
ToolResult complexResult = calcTool.execute(complexParams);
// 结果: 12.0 (4 + 8)
```

### 7. JsonTool (JSON处理工具)

JSON解析和格式化:

```java
JsonTool jsonTool = new JsonTool();

// 解析JSON
Map<String, Object> parseParams = Map.of(
    "operation", "parse",
    "json", "{\"name\": \"EvoX\", \"version\": \"1.0\"}"
);
ToolResult parseResult = jsonTool.execute(parseParams);
Map<String, Object> data = (Map) parseResult.getData();

// 格式化JSON
Map<String, Object> formatParams = Map.of(
    "operation", "format",
    "json", "{\"name\":\"EvoX\",\"version\":\"1.0\"}",
    "indent", 2
);
ToolResult formatResult = jsonTool.execute(formatParams);
String formatted = (String) formatResult.getData();
```

### 8. 工具与Agent集成

在Agent中使用工具:

```java
public class ToolAgent extends Agent {
    private List<BaseTool> tools;
    
    public ToolAgent(BaseLLM llm, List<BaseTool> tools) {
        super(llm);
        this.tools = tools;
    }
    
    @Override
    public Message execute(String actionName, List<Message> messages) {
        // 1. 让LLM决定使用哪个工具
        String prompt = buildPromptWithTools(messages);
        String response = llm.generate(prompt);
        
        // 2. 解析工具调用
        ToolCall toolCall = parseToolCall(response);
        
        // 3. 执行工具
        BaseTool tool = findTool(toolCall.getName());
        ToolResult result = tool.execute(toolCall.getParams());
        
        // 4. 返回结果
        return Message.builder()
            .content(result.getData().toString())
            .messageType(MessageType.RESPONSE)
            .build();
    }
}
```

### 9. Function Calling 支持

提供OpenAI Function Calling格式的Schema:

```java
BaseTool tool = new FileSystemTool();
Map<String, Object> schema = tool.getToolSchema();

// Schema 格式:
{
  "type": "function",
  "function": {
    "name": "file_system",
    "description": "文件系统操作工具",
    "parameters": {
      "type": "object",
      "properties": {
        "operation": {
          "type": "string",
          "enum": ["read", "write", "delete", "list"]
        },
        "filePath": {
          "type": "string",
          "description": "文件路径"
        }
      },
      "required": ["operation"]
    }
  }
}
```

## 📂 目录结构

```
evox-tools/
├── base/                       # 基础抽象
│   ├── BaseTool.java
│   └── ToolResult.java
├── file/                       # 文件工具
│   └── FileSystemTool.java
├── http/                       # HTTP工具
│   ├── HttpTool.java
│   └── HttpResponse.java
├── database/                   # 数据库工具
│   ├── DatabaseTool.java
│   ├── QueryTool.java
│   └── ExecuteTool.java
├── search/                     # 搜索工具
│   ├── WebSearchTool.java
│   ├── GoogleSearchTool.java
│   └── SearchResult.java
├── calculator/                 # 计算工具
│   └── CalculatorTool.java
├── json/                       # JSON工具
│   └── JsonTool.java
├── image/                      # 图像工具
│   └── ImageTool.java
└── browser/                    # 浏览器工具
    └── BrowserTool.java
```

## 🚀 快速开始

### Maven 依赖

```xml
<dependency>
    <groupId>io.leavesfly.evox</groupId>
    <artifactId>evox-tools</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 基本用法

```java
// 1. 创建工具
FileSystemTool fileTool = new FileSystemTool();

// 2. 准备参数
Map<String, Object> params = Map.of(
    "operation", "write",
    "filePath", "/tmp/test.txt",
    "content", "Hello World"
);

// 3. 执行工具
ToolResult result = fileTool.execute(params);

// 4. 处理结果
if (result.isSuccess()) {
    System.out.println("成功: " + result.getData());
} else {
    System.err.println("失败: " + result.getError());
}
```

## 💡 高级用法

### 1. 自定义工具

创建自己的工具:

```java
public class CustomTool implements BaseTool {
    
    @Override
    public String getName() {
        return "custom_tool";
    }
    
    @Override
    public String getDescription() {
        return "自定义工具示例";
    }
    
    @Override
    public ToolResult execute(Map<String, Object> params) {
        try {
            // 执行业务逻辑
            String input = (String) params.get("input");
            String result = processInput(input);
            
            return ToolResult.success(result);
        } catch (Exception e) {
            return ToolResult.failure(e.getMessage());
        }
    }
    
    @Override
    public Map<String, Object> getToolSchema() {
        return Map.of(
            "type", "function",
            "function", Map.of(
                "name", getName(),
                "description", getDescription(),
                "parameters", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "input", Map.of(
                            "type", "string",
                            "description", "输入参数"
                        )
                    ),
                    "required", List.of("input")
                )
            )
        );
    }
}
```

### 2. 工具链

组合多个工具:

```java
public class ToolChain {
    private List<BaseTool> tools;
    
    public ToolResult executeChain(List<Map<String, Object>> steps) {
        Object data = null;
        
        for (Map<String, Object> step : steps) {
            String toolName = (String) step.get("tool");
            Map<String, Object> params = (Map) step.get("params");
            
            // 将上一步的输出作为当前步骤的输入
            if (data != null) {
                params.put("input", data);
            }
            
            BaseTool tool = findTool(toolName);
            ToolResult result = tool.execute(params);
            
            if (!result.isSuccess()) {
                return result;
            }
            
            data = result.getData();
        }
        
        return ToolResult.success(data);
    }
}
```

### 3. 工具权限控制

添加权限检查:

```java
public class SecuredTool implements BaseTool {
    private BaseTool delegate;
    private PermissionChecker checker;
    
    @Override
    public ToolResult execute(Map<String, Object> params) {
        // 检查权限
        if (!checker.hasPermission(getName())) {
            return ToolResult.failure("权限不足");
        }
        
        // 执行工具
        return delegate.execute(params);
    }
}
```

## 🎓 设计原则

- **统一接口**: 所有工具实现BaseTool接口
- **参数灵活**: 使用Map传递参数,支持任意结构
- **结果标准**: ToolResult统一封装执行结果
- **易于扩展**: 简单实现接口即可添加新工具

## 📊 适用场景

- **文件操作**: 读写日志、配置文件
- **API调用**: 集成第三方服务
- **数据查询**: 访问数据库
- **信息检索**: 网络搜索、知识查询
- **数据处理**: JSON解析、计算
- **自动化任务**: 批量文件处理、数据迁移

## 🔗 相关模块

- **evox-core**: 提供基础抽象
- **evox-agents**: Agent使用工具扩展能力
- **evox-workflow**: 工作流节点可调用工具
- **evox-rag**: 使用搜索工具获取外部知识

## ⚠️ 最佳实践

1. **参数验证**: 执行前验证必需参数
2. **异常处理**: 捕获异常并返回友好错误信息
3. **资源管理**: 及时释放文件句柄、数据库连接等
4. **安全考虑**: 文件路径、SQL注入等安全问题
5. **超时控制**: 设置合理的超时时间
6. **日志记录**: 记录工具执行情况便于调试
