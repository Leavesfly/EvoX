# EvoX Workflow 工作流编排引擎

## 📦 模块定位

**层级**: 业务层 (Business Layer)  
**职责**: 提供DAG工作流编排引擎,支持复杂业务流程自动化  
**依赖**: evox-core, evox-models, evox-memory, evox-storage

## 🎯 核心功能

evox-workflow 为 EvoX 框架提供了强大的工作流编排能力,支持DAG(有向无环图)、条件分支、循环控制、并行执行等高级特性。

### 1. 工作流构建

**流式API构建**:
```java
Workflow workflow = WorkflowBuilder.sequential()
    .name("用户注册流程")
    .goal("完成新用户注册")
    .step("验证", validationAgent)
    .step("创建账户", createAccountAgent)
    .step("发送邮件", emailAgent)
    .maxSteps(100)
    .build();
```

**DAG图结构**:
```java
WorkflowGraph graph = new WorkflowGraph("数据处理流程");

// 添加节点
WorkflowNode extractNode = new WorkflowNode();
extractNode.setName("数据提取");
graph.addNode(extractNode);

WorkflowNode transformNode = new WorkflowNode();
transformNode.setName("数据转换");
graph.addNode(transformNode);

// 添加边(依赖关系)
graph.addEdge(extractNode.getNodeId(), transformNode.getNodeId());
```

### 2. 条件分支

```java
WorkflowBuilder.conditional()
    .step("检查条件", checkAgent)
    .branch(
        condition -> (Boolean) condition.getData("approved"),
        approvedWorkflow,
        rejectedWorkflow
    )
    .build();
```

### 3. 并行执行

```java
// 多个Agent并行执行
workflow.executeParallel(List.of(
    agent1,
    agent2,
    agent3
));
```

### 4. 循环控制

```java
WorkflowNode loopNode = new WorkflowNode();
loopNode.setNodeType(WorkflowNode.NodeType.LOOP);
loopNode.setMaxIterations(10);
```

### 5. 工作流执行

```java
// 同步执行
Message input = Message.builder()
    .content("开始执行")
    .messageType(MessageType.INPUT)
    .build();
    
Message result = workflow.execute(List.of(input));

// 异步执行
Mono<Message> asyncResult = workflow.executeAsync(List.of(input));
```

## 📂 目录结构

```
evox-workflow/
├── core/
│   ├── Workflow.java          # 工作流主类
│   ├── WorkflowGraph.java     # 工作流图
│   └── WorkflowNode.java      # 工作流节点
├── builder/
│   └── WorkflowBuilder.java   # 流式构建器
├── executor/
│   └── WorkflowExecutor.java  # 执行引擎
├── context/
│   └── WorkflowContext.java   # 执行上下文
└── operator/
    └── Operator.java          # 操作符
```

## 🚀 快速开始

### Maven 依赖

```xml
<dependency>
    <groupId>io.leavesfly.evox</groupId>
    <artifactId>evox-workflow</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 基本用法

```java
// 1. 创建Agent
Agent step1 = new ActionAgent(llm, "步骤1");
Agent step2 = new ActionAgent(llm, "步骤2");

// 2. 构建工作流
Workflow workflow = WorkflowBuilder.sequential()
    .name("简单流程")
    .step("第一步", step1)
    .step("第二步", step2)
    .build();

// 3. 执行
Message input = Message.builder()
    .content("输入数据")
    .build();
Message result = workflow.execute(List.of(input));
```

## 🔗 相关模块

- **evox-core**: 提供基础抽象
- **evox-models**: LLM模型支持
- **evox-memory**: 工作流状态管理
- **evox-agents**: 节点执行Agent
- **evox-hitl**: 人工审批节点
