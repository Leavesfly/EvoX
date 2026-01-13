# EvoX HITL 人机协同模块

## 📦 模块定位

**层级**: 高级业务层 (Advanced Layer)  
**职责**: 提供人机协同(Human-in-the-Loop)能力  
**依赖**: evox-core, evox-agents, evox-workflow

## 🎯 核心功能

evox-hitl 为 EvoX 框架提供了灵活的人工介入机制,支持审批、监督、修正等人机协同场景。

### 1. HITL 模式

**交互类型**:
- `APPROVAL`: 审批模式
- `REVIEW`: 审查模式  
- `CORRECTION`: 修正模式
- `FEEDBACK`: 反馈模式

### 2. HITL Manager

```java
HITLManager hitl = new HITLManager();

// 配置模式
hitl.setMode(HITLMode.APPROVAL);

// 请求人工介入
HITLRequest request = HITLRequest.builder()
    .context("需要审批的内容")
    .type(HITLInteractionType.APPROVAL)
    .build();

HITLResponse response = hitl.requestHumanInput(request);

if (response.isApproved()) {
    // 继续执行
} else {
    // 处理拒绝
}
```

### 3. HITL Agent

```java
HITLInterceptorAgent agent = new HITLInterceptorAgent(
    baseAgent,
    hitlManager
);

// Agent自动在关键点请求人工介入
Message result = agent.execute(actionName, messages);
```

### 4. 工作流集成

```java
Workflow workflow = WorkflowBuilder.sequential()
    .step("自动处理", autoAgent)
    .step("人工审批", hitlAgent)  // HITL节点
    .step("后续处理", nextAgent)
    .build();
```

## 🚀 快速开始

### Maven 依赖

```xml
<dependency>
    <groupId>io.leavesfly.evox</groupId>
    <artifactId>evox-hitl</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 基本用法

```java
// 1. 创建HITL Manager
HITLManager hitl = new HITLManager();
hitl.setMode(HITLMode.APPROVAL);

// 2. 请求审批
HITLRequest request = HITLRequest.builder()
    .context("订单金额: 10000元")
    .type(HITLInteractionType.APPROVAL)
    .build();

// 3. 等待人工决策
HITLResponse response = hitl.requestHumanInput(request);

// 4. 根据决策执行
if (response.isApproved()) {
    processOrder();
}
```

## 🔗 相关模块

- **evox-core**: 基础抽象
- **evox-agents**: Agent集成
- **evox-workflow**: 工作流集成
