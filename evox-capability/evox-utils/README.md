# EvoX Utils 工具类模块

## 📦 模块定位

**层级**: 能力层 (Capability Layer)  
**职责**: 提供通用工具函数和提示词管理  
**依赖**: evox-core (可选)

## 🎯 核心功能

evox-utils 为 EvoX 框架提供了常用的工具类和提示词管理功能,是一个轻量级的工具库模块。

### 1. CommonUtils (通用工具类)

提供常用的工具方法:

**字符串处理**:
```java
// 判空
boolean isEmpty = CommonUtils.isEmpty(str);
boolean isNotEmpty = CommonUtils.isNotEmpty(str);

// 默认值
String value = CommonUtils.defaultIfEmpty(str, "default");

// 连接字符串
String joined = CommonUtils.join(list, ", ");

// 首字母大写
String capitalized = CommonUtils.capitalize("hello"); // "Hello"
```

**集合操作**:
```java
// 判空
boolean isEmpty = CommonUtils.isEmpty(list);
boolean isNotEmpty = CommonUtils.isNotEmpty(collection);

// 安全获取
String first = CommonUtils.getFirst(list);
String last = CommonUtils.getLast(list);

// 分页
List<String> page = CommonUtils.paginate(list, pageNum, pageSize);
```

**对象操作**:
```java
// 判空
boolean isNull = CommonUtils.isNull(obj);
boolean isNotNull = CommonUtils.isNotNull(obj);

// 默认值
Object value = CommonUtils.defaultIfNull(obj, defaultValue);

// 安全转换
String str = CommonUtils.toString(obj, "");
Integer num = CommonUtils.toInteger(str, 0);
```

**JSON处理**:
```java
// 对象转JSON
String json = CommonUtils.toJson(object);

// JSON转对象
MyClass obj = CommonUtils.fromJson(json, MyClass.class);

// 格式化
String formatted = CommonUtils.formatJson(json);
```

**日期时间**:
```java
// 格式化
String formatted = CommonUtils.formatDate(date, "yyyy-MM-dd HH:mm:ss");

// 解析
Date date = CommonUtils.parseDate("2024-01-01", "yyyy-MM-dd");

// 当前时间
String now = CommonUtils.now();
Instant instant = CommonUtils.nowInstant();
```

**随机生成**:
```java
// 随机字符串
String random = CommonUtils.randomString(10);

// 随机数字
int randomInt = CommonUtils.randomInt(1, 100);

// UUID
String uuid = CommonUtils.uuid();
```

### 2. SanitizeUtils (输入清理工具)

用于清理和验证用户输入:

**SQL注入防护**:
```java
// 清理SQL输入
String safeSql = SanitizeUtils.sanitizeSql(userInput);

// 验证SQL语句
boolean isSafe = SanitizeUtils.isSafeSql(sql);
```

**XSS防护**:
```java
// 清理HTML
String safeHtml = SanitizeUtils.sanitizeHtml(userInput);

// 转义特殊字符
String escaped = SanitizeUtils.escapeHtml("<script>alert('xss')</script>");
```

**路径遍历防护**:
```java
// 清理文件路径
String safePath = SanitizeUtils.sanitizePath(userPath);

// 验证路径安全性
boolean isSafePath = SanitizeUtils.isSafePath(path, baseDir);
```

**输入验证**:
```java
// Email验证
boolean isEmail = SanitizeUtils.isValidEmail(email);

// URL验证
boolean isUrl = SanitizeUtils.isValidUrl(url);

// 电话验证
boolean isPhone = SanitizeUtils.isValidPhone(phone);
```

### 3. PromptTemplate (提示词模板)

提示词管理和模板化:

**基础用法**:
```java
// 创建模板
PromptTemplate template = new PromptTemplate(
    "你好，{name}！今天是{date}，天气{weather}。"
);

// 填充变量
Map<String, String> vars = Map.of(
    "name", "Alice",
    "date", "2024-01-01",
    "weather", "晴朗"
);
String prompt = template.format(vars);
// 结果: "你好，Alice！今天是2024-01-01，天气晴朗。"
```

**链式调用**:
```java
String prompt = PromptTemplate.builder()
    .template("分析以下文本：\n{text}\n\n任务：{task}")
    .variable("text", document)
    .variable("task", "提取关键信息")
    .build();
```

**预定义模板**:
```java
// 系统提示词
String systemPrompt = PromptTemplate.SYSTEM_PROMPT
    .format(Map.of("role", "专业的AI助手"));

// 任务规划模板
String planPrompt = PromptTemplate.TASK_PLANNING
    .format(Map.of("goal", "完成项目开发"));

// 代码生成模板
String codePrompt = PromptTemplate.CODE_GENERATION
    .format(Map.of(
        "language", "Java",
        "requirement", "实现单例模式"
    ));
```

**条件模板**:
```java
PromptTemplate template = new PromptTemplate(
    "执行{task}" +
    "{if verbose}，请提供详细步骤{endif}" +
    "{if strict}，严格遵守规范{endif}"
);

Map<String, Object> vars = Map.of(
    "task", "代码审查",
    "verbose", true,
    "strict", false
);
String prompt = template.format(vars);
```

### 4. PromptLibrary (提示词库)

预定义的提示词集合:

```java
public class PromptLibrary {
    // ReAct提示词
    public static final String REACT_PROMPT = """
        You are a helpful assistant. Answer the following questions as best you can.
        You have access to the following tools:
        {tools}
        
        Use the following format:
        Thought: you should always think about what to do
        Action: the action to take
        Observation: the result of the action
        ... (repeat Thought/Action/Observation as needed)
        Final Answer: the final answer to the question
        
        Question: {question}
        """;
    
    // 任务分解提示词
    public static final String TASK_DECOMPOSITION = """
        请将以下目标分解为可执行的子任务:
        目标: {goal}
        
        要求:
        1. 每个子任务应该具体明确
        2. 子任务之间有清晰的依赖关系
        3. 提供任务的优先级
        
        以JSON格式输出任务列表。
        """;
    
    // 代码审查提示词
    public static final String CODE_REVIEW = """
        请审查以下代码:
        ```{language}
        {code}
        ```
        
        关注点:
        1. 代码质量和可读性
        2. 潜在的bug和安全问题
        3. 性能优化建议
        4. 最佳实践遵循情况
        
        提供详细的审查意见。
        """;
}
```

## 📂 目录结构

```
evox-utils/
├── CommonUtils.java            # 通用工具类
├── SanitizeUtils.java          # 输入清理工具
└── prompts/                    # 提示词管理
    ├── PromptTemplate.java     # 模板引擎
    └── PromptLibrary.java      # 提示词库
```

## 🚀 快速开始

### Maven 依赖

```xml
<dependency>
    <groupId>io.leavesfly.evox</groupId>
    <artifactId>evox-utils</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 基本用法

```java
// 1. 使用通用工具
String value = CommonUtils.defaultIfEmpty(input, "默认值");
String json = CommonUtils.toJson(object);

// 2. 清理用户输入
String safeInput = SanitizeUtils.sanitizeHtml(userInput);

// 3. 使用提示词模板
PromptTemplate template = new PromptTemplate("你好，{name}！");
String prompt = template.format(Map.of("name", "世界"));
```

## 💡 高级用法

### 1. 自定义提示词模板

```java
public class MyPrompts {
    // 客服对话模板
    public static final PromptTemplate CUSTOMER_SERVICE = 
        new PromptTemplate("""
            你是{company}的客服代表{agent_name}。
            
            客户问题: {question}
            
            请提供友好、专业的回答，包含以下要素:
            1. 问候客户
            2. 理解问题
            3. 提供解决方案
            4. 询问是否需要进一步帮助
            """);
    
    // 使用
    String prompt = CUSTOMER_SERVICE.format(Map.of(
        "company", "EvoX科技",
        "agent_name", "小智",
        "question", "如何使用这个功能?"
    ));
}
```

### 2. 动态模板加载

从文件或数据库加载模板:

```java
public class TemplateManager {
    private Map<String, PromptTemplate> templates = new HashMap<>();
    
    public void loadFromFile(Path file) throws IOException {
        String content = Files.readString(file);
        PromptTemplate template = new PromptTemplate(content);
        templates.put(file.getFileName().toString(), template);
    }
    
    public String format(String templateName, Map<String, String> vars) {
        PromptTemplate template = templates.get(templateName);
        return template.format(vars);
    }
}
```

### 3. 提示词版本管理

```java
public class VersionedPrompt {
    private Map<String, PromptTemplate> versions = new HashMap<>();
    private String currentVersion = "v1";
    
    public void addVersion(String version, String template) {
        versions.put(version, new PromptTemplate(template));
    }
    
    public String format(Map<String, String> vars) {
        return versions.get(currentVersion).format(vars);
    }
    
    public void switchVersion(String version) {
        if (versions.containsKey(version)) {
            this.currentVersion = version;
        }
    }
}
```

### 4. 多语言提示词

```java
public class I18nPrompt {
    private Map<String, PromptTemplate> templates;
    
    public I18nPrompt() {
        templates = Map.of(
            "zh", new PromptTemplate("你好，{name}！"),
            "en", new PromptTemplate("Hello, {name}!"),
            "ja", new PromptTemplate("こんにちは、{name}！")
        );
    }
    
    public String format(String locale, Map<String, String> vars) {
        return templates.get(locale).format(vars);
    }
}
```

## 🎓 设计原则

- **简单实用**: 提供常用功能,避免过度设计
- **零依赖**: utils模块可独立使用
- **安全第一**: 输入清理和验证
- **易于扩展**: 方便添加新的工具方法

## 📊 适用场景

- **输入验证**: 清理和验证用户输入
- **提示词管理**: 统一管理Prompt模板
- **通用工具**: 字符串、集合、日期处理
- **安全防护**: SQL注入、XSS防护
- **JSON处理**: 序列化和反序列化

## 🔗 相关模块

- **evox-core**: 可选依赖,提供BaseModule
- **evox-agents**: 使用PromptTemplate构建Prompt
- **evox-tools**: 使用SanitizeUtils验证输入
- **所有模块**: 都可使用CommonUtils工具方法

## ⚠️ 最佳实践

1. **输入验证**: 所有用户输入都应该清理和验证
2. **模板复用**: 提取通用Prompt为模板
3. **版本管理**: 重要Prompt应该有版本控制
4. **国际化**: 考虑多语言支持
5. **测试覆盖**: 工具方法需要充分测试
