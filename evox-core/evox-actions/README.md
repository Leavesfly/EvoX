# EvoX Actions 动作执行引擎

## 📦 模块定位

**层级**: 核心层 (Core Layer)  
**职责**: 提供统一的动作(Action)执行引擎和专业化Action实现  
**依赖**: evox-core, evox-models

## 🎯 核心功能

evox-actions 为 EvoX 框架提供了灵活的动作执行机制,将复杂的任务分解为可复用的Action单元,每个Action封装了特定的功能逻辑。

### 1. Action 基础抽象

#### Action 基类

所有Action的基类,定义了统一的执行接口:

**核心属性**:
- `name`: 动作名称
- `description`: 动作描述
- `llm`: 语言模型实例(可选)

**核心方法**:
```java
// 同步执行
ActionOutput execute(ActionInput input);

// 异步执行
Mono<ActionOutput> executeAsync(ActionInput input);

// 字段定义
String[] getInputFields();
String[] getOutputFields();
```

#### ActionInput

动作输入封装:
- 支持Map形式的数据传递
- 提供类型安全的数据访问方法
- 可扩展自定义输入类型

#### ActionOutput

动作输出封装:
- `success`: 执行是否成功
- `data`: 输出数据
- `error`: 错误信息
- 提供便捷的成功/失败构造方法

### 2. 专业化Action实现

#### PlanAction (规划动作)

将复杂目标分解为可执行的子任务:

**功能**:
- 自动生成任务分解计划
- 提供任务优先级排序
- 支持自定义规划策略

**使用场景**:
- 复杂问题分解
- 项目任务规划
- 工作流设计

```java
PlanAction planAction = new PlanAction(llm);
planAction.setGoal("开发一个用户管理系统");

ActionInput input = new PlanActionInput("开发一个用户管理系统");
ActionOutput output = planAction.execute(input);

List<Task> tasks = (List<Task>) output.getData().get("tasks");
```

#### ReflectionAction (反思动作)

对已有输出进行批判性分析和改进:

**功能**:
- 分析输出质量
- 识别问题和不足
- 提供改进建议
- 生成优化版本

**使用场景**:
- 内容质量检查
- 代码审查
- 方案优化

```java
ReflectionAction reflection = new ReflectionAction(llm);

ReflectionInput input = ReflectionInput.builder()
    .taskDescription("编写用户注册功能")
    .currentOutput("当前代码实现...")
    .criteria("代码质量、安全性、可维护性")
    .build();

ActionOutput output = reflection.execute(input);
String improvements = (String) output.getData().get("improved_output");
```

#### CodeExtractionAction (代码提取动作)

从文本中提取或生成代码:

**功能**:
- 从自然语言生成代码
- 从混合文本提取代码块
- 支持多种编程语言
- 代码格式化

**使用场景**:
- 代码生成
- 文档中提取代码
- 代码模板生成

```java
CodeExtractionAction codeAction = new CodeExtractionAction(llm, "java");

ActionInput input = SimpleActionInput.of(Map.of(
    "requirements", "实现一个单例模式类"
));

ActionOutput output = codeAction.execute(input);
String code = (String) output.getData().get("code");
```

#### CustomizeAction (自定义动作)

灵活创建自定义Action:

**功能**:
- 支持Lambda表达式定义
- 快速封装业务逻辑
- 无需创建新类

```java
// 方式1: Lambda
CustomizeAction action = new CustomizeAction(
    "format",
    "格式化文本",
    input -> {
        String text = (String) input.getData().get("text");
        return SimpleActionOutput.success(text.toUpperCase());
    }
);

// 方式2: 函数引用
CustomizeAction validator = new CustomizeAction(
    "validate",
    "验证输入",
    this::validateInput
);
```

### 3. Action 管理

#### ActionRegistry

Action注册和管理:
- 注册Action到全局注册表
- 按名称查找Action
- 支持Action链式调用

```java
ActionRegistry registry = new ActionRegistry();
registry.register("plan", new PlanAction(llm));
registry.register("reflect", new ReflectionAction(llm));

Action action = registry.get("plan");
```

#### Action 链式执行

多个Action串联执行:

```java
// 规划 -> 执行 -> 反思 -> 优化
ActionInput planInput = new PlanActionInput("目标任务");
ActionOutput planOutput = planAction.execute(planInput);

List<Task> tasks = extractTasks(planOutput);
ActionOutput executeOutput = executeAction.execute(tasks);

ReflectionInput reflectInput = buildReflectionInput(executeOutput);
ActionOutput reflectOutput = reflectionAction.execute(reflectInput);
```

## 📂 目录结构

```
evox-actions/
├── base/                       # 基础类
│   ├── Action.java             # Action基类
│   ├── ActionInput.java        # 输入封装
│   ├── ActionOutput.java       # 输出封装
│   └── SimpleActionOutput.java # 简单输出实现
├── planning/                   # 规划相关
│   ├── PlanAction.java
│   └── Task.java
├── reflection/                 # 反思相关
│   ├── ReflectionAction.java
│   ├── ReflectionInput.java
│   └── ReflectionOutput.java
├── extraction/                 # 提取相关
│   └── CodeExtractionAction.java
├── coding/                     # 编码相关
│   └── CodingAction.java
└── customize/                  # 自定义
    └── CustomizeAction.java
```

## 🚀 快速开始

### Maven 依赖

```xml
<dependency>
    <groupId>io.leavesfly.evox</groupId>
    <artifactId>evox-actions</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 基本用法

```java
// 1. 创建Action
BaseLLM llm = ...; // 获取LLM实例
PlanAction planAction = new PlanAction(llm);

// 2. 准备输入
ActionInput input = new PlanActionInput("开发博客系统");

// 3. 执行Action
ActionOutput output = planAction.execute(input);

// 4. 处理输出
if (output.isSuccess()) {
    List<Task> tasks = (List<Task>) output.getData().get("tasks");
    tasks.forEach(task -> 
        log.info("Task: {}", task.getDescription())
    );
} else {
    log.error("执行失败: {}", output.getError());
}
```

### 异步执行

```java
Mono<ActionOutput> async = planAction.executeAsync(input);

async.subscribe(
    output -> handleOutput(output),
    error -> log.error("Error", error),
    () -> log.info("Complete")
);
```

## 💡 高级用法

### 1. 自定义Action

创建自己的Action实现:

```java
@Data
@EqualsAndHashCode(callSuper = true)
public class MyAction extends Action {
    
    @Override
    public ActionOutput execute(ActionInput input) {
        try {
            // 1. 获取输入
            String data = (String) input.getData().get("data");
            
            // 2. 执行逻辑
            String result = processData(data);
            
            // 3. 返回结果
            return SimpleActionOutput.success(Map.of(
                "result", result
            ));
        } catch (Exception e) {
            return SimpleActionOutput.failure(e.getMessage());
        }
    }
    
    @Override
    public String[] getInputFields() {
        return new String[]{"data"};
    }
    
    @Override
    public String[] getOutputFields() {
        return new String[]{"result"};
    }
    
    private String processData(String data) {
        // 业务逻辑
        return data.toUpperCase();
    }
}
```

### 2. Action组合

将多个Action组合成工作流:

```java
public class ActionPipeline {
    private final List<Action> actions;
    
    public ActionOutput execute(ActionInput initialInput) {
        ActionInput current = initialInput;
        
        for (Action action : actions) {
            ActionOutput output = action.execute(current);
            
            if (!output.isSuccess()) {
                return output; // 失败则终止
            }
            
            // 将输出转为下一个Action的输入
            current = new SimpleActionInput(output.getData());
        }
        
        return ((SimpleActionInput)current).toOutput();
    }
}
```

### 3. 带LLM的Action

利用LLM能力:

```java
public class SummarizeAction extends Action {
    
    @Override
    public ActionOutput execute(ActionInput input) {
        String text = (String) input.getData().get("text");
        
        // 使用LLM生成摘要
        String prompt = "请对以下文本生成摘要:\n\n" + text;
        String summary = getLlm().generate(prompt);
        
        return SimpleActionOutput.success(Map.of(
            "summary", summary
        ));
    }
    
    @Override
    public String[] getInputFields() {
        return new String[]{"text"};
    }
    
    @Override
    public String[] getOutputFields() {
        return new String[]{"summary"};
    }
}
```

### 4. 条件Action

根据条件选择不同的执行路径:

```java
public class ConditionalAction extends Action {
    private final Predicate<ActionInput> condition;
    private final Action trueAction;
    private final Action falseAction;
    
    @Override
    public ActionOutput execute(ActionInput input) {
        if (condition.test(input)) {
            return trueAction.execute(input);
        } else {
            return falseAction.execute(input);
        }
    }
}
```

## 🎓 设计原则

- **单一职责**: 每个Action专注一个特定功能
- **可组合性**: Action可以组合成复杂工作流
- **可测试性**: 输入输出明确,易于单元测试
- **可扩展性**: 易于添加新的Action类型
- **LLM集成**: 无缝集成大语言模型能力

## 📊 适用场景

- **任务分解**: 使用PlanAction分解复杂任务
- **质量检查**: 使用ReflectionAction审查输出
- **代码生成**: 使用CodeExtractionAction生成代码
- **数据处理**: 自定义Action处理业务数据
- **工作流编排**: 组合Action构建复杂流程

## 🔗 相关模块

- **evox-core**: 提供BaseModule抽象
- **evox-models**: 提供LLM能力
- **evox-agents**: Agent使用Action执行任务
- **evox-workflow**: 工作流中使用Action作为节点

## ⚠️ 最佳实践

1. **输入验证**: 在execute方法开始时验证输入
2. **异常处理**: 捕获异常并返回失败的ActionOutput
3. **日志记录**: 记录Action执行的关键步骤
4. **幂等性**: 尽量设计幂等的Action
5. **资源管理**: 妥善管理LLM等资源
6. **命名规范**: Action名称应清晰描述其功能
