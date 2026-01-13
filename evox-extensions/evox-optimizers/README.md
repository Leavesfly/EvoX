# EvoX Optimizers 优化器模块

## 📦 模块定位

**层级**: 高级业务层 (Advanced Layer)  
**职责**: 提供Prompt和工作流优化能力,包含评估器  
**依赖**: evox-core, evox-models, evox-agents, evox-workflow

## 🎯 核心功能

evox-optimizers 为 EvoX 框架提供了智能优化能力,支持Prompt优化、工作流优化和效果评估。

### 优化器类型

| 优化器 | 功能描述 | 适用场景 |
|--------|---------|---------|
| **TextGrad** | 基于梯度的文本优化 | Prompt优化 |
| **MIPRO** | 多指标迭代优化 | 复杂任务优化 |
| **AFlow** | 自动工作流优化 | 流程优化 |
| **SEW** | 自进化工作流 | 持续改进 |
| **EvoPrompt** | 进化式Prompt优化 | 大规模优化 |

### 1. TextGrad 优化器

```java
TextGradOptimizer optimizer = new TextGradOptimizer(llm);

String initialPrompt = "分析这段文本";
String optimizedPrompt = optimizer.optimize(
    initialPrompt,
    trainingData,
    evaluator
);
```

### 2. MIPRO 优化器

```java
MIPROOptimizer mipro = new MIPROOptimizer(llm);

OptimizationResult result = mipro.optimize(
    workflow,
    examples,
    metrics
);
```

### 3. 评估器

```java
Evaluator evaluator = new Evaluator();

EvaluationResult result = evaluator.evaluate(
    predictions,
    groundTruth,
    metrics
);

double accuracy = result.getMetric("accuracy");
double f1Score = result.getMetric("f1");
```

## 🚀 快速开始

### Maven 依赖

```xml
<dependency>
    <groupId>io.leavesfly.evox</groupId>
    <artifactId>evox-optimizers</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 基本用法

```java
// 1. 创建优化器
TextGradOptimizer optimizer = new TextGradOptimizer(llm);

// 2. 准备训练数据
List<Example> examples = loadExamples();

// 3. 优化
String optimized = optimizer.optimize(
    initialPrompt,
    examples,
    evaluator
);
```

## 🔗 相关模块

- **evox-core**: 基础抽象
- **evox-models**: LLM模型
- **evox-agents**: Agent优化
- **evox-workflow**: 工作流优化
