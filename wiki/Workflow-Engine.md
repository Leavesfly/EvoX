# 工作流引擎 (evox-workflow)

## 模块定位

**业务层模块**，提供基于 DAG（有向无环图）的工作流引擎，用于编排和执行复杂的业务流程。

### 依赖关系
- **依赖**: `evox-core`, `evox-memory`
- **被依赖**: 上层应用模块（如 evox-application）

### Maven 坐标
```xml
<dependency>
    <groupId>com.alibaba.evox</groupId>
    <artifactId>evox-workflow</artifactId>
    <version>${evox.version}</version>
</dependency>
```

## DAG 工作流概念

工作流引擎基于 **DAG（Directed Acyclic Graph，有向无环图）** 模型：
- **节点（Node）**: 表示工作流中的一个执行单元（任务、条件判断、循环等）
- **边（Edge）**: 表示节点之间的执行顺序和依赖关系
- **无环**: 确保工作流不会出现无限循环（除非显式使用循环节点）

## WorkflowBuilder 流式 API

使用流式 API 构建工作流：

### 顺序执行
```java
Workflow workflow = WorkflowBuilder.create("my-workflow")
    .sequential()
        .addNode("step1", new TaskNode(() -> "Hello"))
        .addNode("step2", new TaskNode(input -> input + " World"))
        .addNode("step3", new TaskNode(input -> logAndReturn(input)))
    .build();
```

### 条件分支
```java
Workflow workflow = WorkflowBuilder.create("conditional-workflow")
    .sequential()
        .addNode("check", new ConditionNode(ctx -> {
            String value = ctx.get("input");
            return value != null && value.length() > 10;
        }))
        .conditional()
            .onTrue().addNode("long-input", new TaskNode(ctx -> "Long input detected"))
            .onFalse().addNode("short-input", new TaskNode(ctx -> "Short input"))
        .endConditional()
    .build();
```

## WorkflowGraph 图结构

### 核心组件

```java
// 工作流节点
public class WorkflowNode {
    private String id;           // 节点唯一标识
    private NodeType type;       // 节点类型
    private NodeExecutor executor; // 执行器
    private Map<String, Object> config; // 配置
}

// 节点类型
public enum NodeType {
    TASK,      // 普通任务
    CONDITION, // 条件判断
    LOOP,      // 循环
    PARALLEL,  // 并行
    HITL       // 人工审批
}
```

### 构建图结构

```java
WorkflowGraph graph = new WorkflowGraph();

// 添加节点
graph.addNode("start", new TaskNode(...));
graph.addNode("process", new TaskNode(...));
graph.addNode("end", new TaskNode(...));

// 添加边（定义执行顺序）
graph.addEdge("start", "process");
graph.addEdge("process", "end");

// 创建并执行工作流
Workflow workflow = new Workflow(graph);
WorkflowContext context = workflow.execute(initialContext).block();
```

## 条件分支示例

```java
Workflow workflow = WorkflowBuilder.create("approval-workflow")
    .sequential()
        .addNode("submit", new TaskNode(ctx -> {
            ctx.put("status", "submitted");
            return ctx;
        }))
        .addNode("review", new ConditionNode(ctx -> {
            int amount = (int) ctx.get("amount");
            return amount > 10000; // 大于1万需要高级审批
        }))
        .conditional()
            .onTrue()
                .addNode("senior-approval", new TaskNode(ctx -> {
                    ctx.put("approvedBy", "senior-manager");
                    return ctx;
                }))
            .onFalse()
                .addNode("normal-approval", new TaskNode(ctx -> {
                    ctx.put("approvedBy", "manager");
                    return ctx;
                }))
        .endConditional()
        .addNode("finalize", new TaskNode(ctx -> {
            ctx.put("status", "completed");
            return ctx;
        }))
    .build();
```

## 并行执行

使用 `executeParallel` 并行执行多个独立任务：

```java
Workflow workflow = WorkflowBuilder.create("parallel-workflow")
    .sequential()
        .addNode("fetch-data", new TaskNode(ctx -> fetchData()))
        .addNode("parallel-tasks", new ParallelNode(
            new TaskNode(ctx -> processA(ctx)),
            new TaskNode(ctx -> processB(ctx)),
            new TaskNode(ctx -> processC(ctx))
        ))
        .addNode("merge-results", new TaskNode(ctx -> mergeResults(ctx)))
    .build();

// 并行节点中的三个任务将同时执行
WorkflowContext result = workflow.execute(initialContext).block();
```

## 循环控制

使用 `NodeType.LOOP` 实现循环逻辑：

```java
LoopNode loopNode = new LoopNode.Builder()
    .maxIterations(10)  // 最大循环次数
    .condition(ctx -> {
        int count = (int) ctx.getOrDefault("retryCount", 0);
        boolean success = (boolean) ctx.getOrDefault("success", false);
        return !success && count < 10;
    })
    .body(new TaskNode(ctx -> {
        // 执行重试逻辑
        boolean result = tryExecute();
        ctx.put("success", result);
        ctx.put("retryCount", (int) ctx.getOrDefault("retryCount", 0) + 1);
        return ctx;
    }))
    .build();

Workflow workflow = WorkflowBuilder.create("retry-workflow")
    .sequential()
        .addNode("retry-loop", loopNode)
    .build();
```

## 同步/异步执行

### 同步执行（阻塞）
```java
WorkflowContext context = workflow.execute(initialContext).block();
String result = context.getOutput();
```

### 异步执行（非阻塞）
```java
workflow.execute(initialContext)
    .subscribe(context -> {
        System.out.println("Workflow completed: " + context.getOutput());
    }, error -> {
        System.err.println("Workflow failed: " + error.getMessage());
    });
```

### 链式异步处理
```java
workflow.execute(initialContext)
    .map(ctx -> transform(ctx))
    .flatMap(transformed -> anotherWorkflow.execute(transformed))
    .subscribe(finalResult -> { ... });
```

## WorkflowContext 执行上下文

`WorkflowContext` 在工作流执行过程中传递数据：

```java
public class WorkflowContext {
    // 获取输入
    public <T> T getInput();
    
    // 设置/获取变量
    public void put(String key, Object value);
    public <T> T get(String key);
    public <T> T getOrDefault(String key, T defaultValue);
    
    // 获取输出
    public <T> T getOutput();
    
    // 记录日志
    public void log(String message);
    
    // 获取执行状态
    public ExecutionStatus getStatus();
}
```

### 使用示例
```java
TaskNode task = new TaskNode(ctx -> {
    // 读取输入
    String input = ctx.getInput();
    
    // 中间变量存储
    ctx.put("processedData", processData(input));
    ctx.put("timestamp", System.currentTimeMillis());
    
    // 后续节点可以读取这些变量
    return ctx;
});
```

## 错误处理与重试

### 节点级错误处理
```java
TaskNode task = new TaskNode.Builder()
    .executor(ctx -> {
        // 可能抛出异常的任务
        return riskyOperation();
    })
    .onError(ctx -> {
        // 错误处理逻辑
        ctx.put("error", ctx.getLastException().getMessage());
        ctx.put("status", "failed");
        return ctx;
    })
    .retry(3)  // 最多重试3次
    .retryDelay(Duration.ofSeconds(1))  // 重试间隔
    .build();
```

### 工作流级错误处理
```java
workflow.execute(initialContext)
    .onErrorResume(error -> {
        log.error("Workflow failed", error);
        return Mono.just(createFallbackContext());
    })
    .subscribe(result -> { ... });
```

## 与 HITL 人工审批节点集成

`HITLNode` 允许工作流在执行过程中暂停，等待人工审批：

```java
// 创建人工审批节点
HITLNode approvalNode = new HITLNode.Builder()
    .approvalType(ApprovalType.MANUAL)
    .timeout(Duration.ofHours(24))  // 24小时超时
    .onApprove(ctx -> {
        ctx.put("approved", true);
        return ctx;
    })
    .onReject(ctx -> {
        ctx.put("approved", false);
        return ctx;
    })
    .build();

// 构建包含审批的工作流
Workflow workflow = WorkflowBuilder.create("approval-workflow")
    .sequential()
        .addNode("submit-request", new TaskNode(ctx -> submitRequest()))
        .addNode("wait-approval", approvalNode)
        .addNode("check-approval", new ConditionNode(ctx -> 
            (boolean) ctx.getOrDefault("approved", false)
        ))
        .conditional()
            .onTrue().addNode("proceed", new TaskNode(ctx -> proceed()))
            .onFalse().addNode("reject", new TaskNode(ctx -> reject()))
        .endConditional()
    .build();

// 执行工作流 - 会在审批节点暂停
WorkflowContext context = workflow.execute(initialContext).block();

// 在另一个线程/请求中完成审批
hitlService.approve(context.getExecutionId(), approverId);
// 或
hitlService.reject(context.getExecutionId(), approverId, reason);
```

## 目录结构

```
com.alibaba.evox.workflow/
├── core/                  # 核心类
│   ├── Workflow.java
│   ├── WorkflowGraph.java
│   ├── WorkflowNode.java
│   └── NodeType.java
├── builder/               # 构建器
│   ├── WorkflowBuilder.java
│   └── NodeBuilder.java
├── executor/              # 执行器
│   ├── WorkflowExecutor.java
│   ├── NodeExecutor.java
│   └── ParallelExecutor.java
├── context/               # 上下文
│   ├── WorkflowContext.java
│   └── ExecutionContext.java
└── operator/              # 操作符/节点类型
    ├── TaskNode.java
    ├── ConditionNode.java
    ├── LoopNode.java
    ├── ParallelNode.java
    └── HITLNode.java
```

## 最佳实践

1. **保持工作流简洁**: 避免过深的嵌套和过多的节点，复杂逻辑拆分为子工作流
2. **合理使用并行**: 仅对无依赖关系的任务使用并行执行
3. **设置超时和重试**: 为可能失败的任务配置合理的重试策略和超时时间
4. **上下文管理**: 避免在上下文中存储过大的对象，使用引用而非拷贝
5. **错误处理**: 在每个关键节点添加错误处理逻辑，避免整个工作流失败
6. **监控和日志**: 使用 `WorkflowContext.log()` 记录关键步骤，便于调试和追踪
7. **HITL 超时**: 为人工审批节点设置合理的超时时间，避免工作流长期挂起
