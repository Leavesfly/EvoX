# EvoX Optimizers 优化器模块

## 📦 模块定位

**层级**: Evolving Layer（进化层）  
**职责**: 提供 Agent、Workflow、Memory 三层优化能力，支持评估反馈驱动的迭代优化  
**依赖**: evox-core, evox-models, evox-agents, evox-workflow, evox-memory

## 🏗️ 架构概览

基于 EvoAgentX 论文的 **Evolving Layer** 架构，本模块将优化器划分为三个层级，每层遵循统一的优化范式：

```
Target(t+1) = O(Target(t), E)
```

其中 `O` 为优化算子，`E` 为评估反馈（`EvaluationFeedback`）。

```
                    ┌─────────────────────┐
                    │     Optimizer        │  统一基类：迭代优化框架
                    │  (abstract base)     │  收敛检查、评估反馈
                    └──────────┬──────────┘
           ┌───────────────────┼───────────────────┐
           ▼                   ▼                   ▼
  ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
  │ AgentOptimizer   │ │WorkflowOptimizer│ │BaseMemoryOptimizer│
  │ (Prompt(t+1),    │ │ W(t+1) =        │ │ M(t+1) =         │
  │  θ(t+1)) =       │ │ O_wf(W(t), E)   │ │ O_mem(M(t), E)   │
  │ O_ag(P(t),θ(t),E)│ │                 │ │                  │
  └──────┬──────────┘ └──────┬──────────┘ └──────┬──────────┘
    ┌────┼────┐         ┌────┼────┐              │
    ▼    ▼    ▼         ▼         ▼              ▼
TextGrad MIPRO EvoPrompt AFlow    SEW     MemoryOptimizer
```

## 🎯 三层优化器

### Layer 1: Agent Optimizer — 公式 (3)

> **(Prompt(t+1), θ(t+1)) = O_agent(Prompt(t), θ(t), E)**

优化 agent 的 prompt 模板、工具配置和行动策略。

| 优化器 | 算法 | 核心方法 |
|--------|------|---------|
| **TextGrad** | 基于文本梯度的 prompt 优化 | `optimizePrompt()` / `optimizeConfig()` |
| **MIPRO** | 贝叶斯优化 + 指令生成 + 示例引导 | `optimizePrompt()` / `optimizeConfig()` |
| **EvoPrompt** | 进化算法驱动的 prompt 优化 | `optimizePrompt()` / `optimizeConfig()` |

```java
// TextGrad: 基于梯度的 prompt 优化
TextGradOptimizer optimizer = TextGradOptimizer.builder()
        .optimizerLLM(llm)
        .executorLLM(executorLlm)
        .currentPrompt("Analyze this text")
        .optimizeMode("all")
        .batchSize(3)
        .maxSteps(10)
        .convergenceThreshold(3)
        .build();

OptimizationResult result = optimizer.optimize(dataset, Map.of());

// 使用评估反馈进行单步优化
EvaluationFeedback feedback = optimizer.evaluateWithFeedback(dataset, "validation", Map.of());
String optimizedPrompt = optimizer.optimizePrompt(currentPrompt, agentConfig, feedback);
```

### Layer 2: Workflow Optimizer — 公式 (4)

> **W(t+1) = O_workflow(W(t), E)**

优化工作流图结构 W = (V, E)，通过重排节点、修改依赖关系和探索替代执行策略。

| 优化器 | 算法 | 核心方法 |
|--------|------|---------|
| **AFlow** | 蒙特卡洛树搜索 (MCTS) | `optimizeWorkflow()` |
| **SEW** | 顺序工作流进化 | `optimizeWorkflow()` |

```java
// AFlow: 基于 MCTS 的工作流优化
AFlowOptimizer optimizer = AFlowOptimizer.builder()
        .workflow(workflow)
        .optimizerLLM(llm)
        .maxIterations(10)
        .populationSize(5)
        .convergenceWindow(3)
        .maxSteps(20)
        .convergenceThreshold(3)
        .build();

OptimizationResult result = optimizer.optimize(dataset, Map.of());
optimizer.restoreBestWorkflow();
```

### Layer 3: Memory Optimizer — 公式 (5)

> **M(t+1) = O_memory(M(t), E)**

优化 agent 的记忆管理策略，支持选择性保留、动态裁剪和基于优先级的检索。

| 优化器 | 功能 | 核心方法 |
|--------|------|---------|
| **MemoryOptimizer** | 记忆压缩、裁剪、智能摘要 | `optimizeMemory()` / `compressMemory()` / `pruneMemory()` |

```java
// Memory: 记忆优化
MemoryOptimizer optimizer = MemoryOptimizer.builder()
        .llm(llm)
        .memory(shortTermMemory)
        .compressionRatio(0.7)
        .enableSmartSummary(true)
        .maxSteps(5)
        .convergenceThreshold(3)
        .build();

OptimizationResult result = optimizer.optimize(dataset, Map.of());
```

## 🔄 统一评估反馈

所有优化器共享统一的 `EvaluationFeedback` 机制，对应论文中的 **E**：

```java
// 通过优化器生成评估反馈
EvaluationFeedback feedback = optimizer.evaluateWithFeedback(dataset, "validation", Map.of());

// 手动构建评估反馈
EvaluationFeedback feedback = EvaluationFeedback.builder()
        .primaryScore(0.85)
        .evalMode("validation")
        .sampleCount(100)
        .textualGradient("Be more specific in instructions")
        .build();

feedback.putMetric("accuracy", 0.9);
double accuracy = feedback.getMetric("accuracy");
```

## 📁 包结构

```
io.leavesfly.evox.optimizers
├── Optimizer.java                    # 统一基类
├── base/
│   ├── EvaluationFeedback.java       # 统一评估反馈 E
│   ├── OptimizationContext.java      # 优化上下文管理
│   └── OptimizationType.java         # 优化类型枚举 (AGENT/WORKFLOW/MEMORY)
├── agent/
│   └── AgentOptimizer.java           # Agent 级优化器抽象基类
├── workflow/
│   └── WorkflowOptimizer.java        # Workflow 级优化器抽象基类
├── memory/
│   └── BaseMemoryOptimizer.java      # Memory 级优化器抽象基类
├── TextGradOptimizer.java            # TextGrad 实现 (Agent)
├── MIPROOptimizer.java               # MIPRO 实现 (Agent)
├── EvoPromptOptimizer.java           # EvoPrompt 实现 (Agent)
├── AFlowOptimizer.java               # AFlow 实现 (Workflow)
├── SEWOptimizer.java                 # SEW 实现 (Workflow)
├── MemoryOptimizer.java              # Memory 优化器实现
└── evaluators/                       # 评估器
    ├── AFlowEvaluator.java
    └── metrics/
        └── EvaluationMetric.java
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

## 🔗 相关模块

- **evox-core**: 基础抽象（BaseModule、IEvaluator）
- **evox-models**: LLM 模型提供者
- **evox-agents**: Agent 运行时
- **evox-workflow**: 工作流引擎
- **evox-memory**: 记忆管理

## 📚 参考文献

- **TextGrad**: Yuksekgonul et al., 2024 — 基于文本梯度的自动微分
- **MIPRO**: Opsahl-Ong et al., 2024 — 多指标迭代提示优化
- **AFlow**: Zhang et al., 2024b — 基于 MCTS 的自动工作流优化
- **SEW**: Liu et al., 2025 — 自进化工作流优化
- **EvoAgentX**: Evolving Layer 三层优化架构
