# 优化器系统 (evox-optimizers)

## 模块定位

**Evolving Layer 进化层** - 负责智能体的自我优化与进化

### 依赖关系
- `evox-core` - 核心基础能力
- `evox-models` - LLM 模型抽象
- `evox-agents` - 智能体系统
- `evox-workflow` - 工作流引擎
- `evox-memory` - 记忆管理

---

## 统一优化范式

```
Target(t+1) = O(Target(t), E)
```

其中：
- **Target(t)**: 当前待优化目标（Prompt / Workflow / Memory）
- **O**: 优化算子（Optimizer）
- **E**: 评估反馈（EvaluationFeedback）
- **Target(t+1)**: 优化后的目标

---

## 三层优化器架构

```
┌─────────────────────────────────────────────┐
│              Optimizer (基类)                │
├──────────────┬──────────────┬───────────────┤
│              │              │               │
│  Agent       │  Workflow    │  BaseMemory   │
│  Optimizer   │  Optimizer   │  Optimizer    │
│              │              │               │
├──────┬───────┼──────┬───────┼───────┬───────┤
│Text  │ MIPRO │ AFlow│ SEW   │Memory │ ...   │
│Grad  │       │      │       │Opt.   │       │
└──────┴───────┴──────┴───────┴───────┴───────┘
```

---

## Layer 1: Agent Optimizer

针对智能体 Prompt 和配置的优化器。

### TextGrad - 基于文本梯度的 Prompt 优化

利用 LLM 生成"文本梯度"，指导 Prompt 的迭代改进。

**核心方法：**
- `optimizePrompt()` - 优化提示词
- `optimizeConfig()` - 优化配置参数

**代码示例：**
```java
TextGradOptimizer optimizer = TextGradOptimizer.builder()
    .optimizerLLM(llm)
    .currentPrompt("You are a helpful assistant...")
    .taskDescription("Answer questions accurately")
    .build();

OptimizationResult result = optimizer.optimizePrompt(evaluationFeedback);
String improvedPrompt = result.getOptimizedPrompt();
```

### MIPRO - 贝叶斯优化 + 指令生成 + 示例引导

结合贝叶斯优化搜索策略空间，自动生成指令和少样本示例。

**特点：**
- 自动探索最优指令组合
- 动态生成高质量示例
- 适用于复杂任务场景

### EvoPrompt - 进化算法驱动的 Prompt 优化

采用遗传算法思想，通过选择、交叉、变异操作进化 Prompt 种群。

**适用场景：**
- 需要多样化候选方案
- 全局搜索最优解

---

## Layer 2: Workflow Optimizer

针对工作流结构和执行逻辑的优化器。

### AFlow - 蒙特卡洛树搜索 (MCTS)

使用 MCTS 算法探索最优工作流拓扑和执行路径。

**核心方法：**
- `optimizeWorkflow()` - 优化工作流结构

**代码示例：**
```java
AFlowOptimizer optimizer = AFlowOptimizer.builder()
    .workflow(workflow)
    .maxIterations(10)
    .explorationConstant(1.414)
    .build();

Workflow optimizedWorkflow = optimizer.optimizeWorkflow(evaluationFeedback);
```

### SEW - 顺序工作流进化

针对线性工作流的渐进式优化，通过局部修改提升整体性能。

**特点：**
- 保持工作流基本结构
- 优化节点顺序和连接
- 计算开销较低

---

## Layer 3: Memory Optimizer

针对智能体记忆系统的优化器。

### MemoryOptimizer - 记忆压缩/裁剪/智能摘要

**核心方法：**
- `optimizeMemory()` - 综合优化记忆
- `compressMemory()` - 压缩记忆内容
- `pruneMemory()` - 裁剪冗余记忆

**代码示例：**
```java
MemoryOptimizer optimizer = MemoryOptimizer.builder()
    .llm(llm)
    .memory(shortTermMemory)
    .compressionRatio(0.7)
    .build();

Memory optimizedMemory = optimizer.compressMemory();
```

**功能特性：**
- 智能摘要：提取关键信息
- 冗余检测：识别并删除重复内容
- 重要性排序：保留高价值记忆

---

## EvaluationFeedback - 统一评估反馈机制

所有优化器共享的评估反馈数据结构。

**核心字段：**
- `primaryScore` - 主要评分（数值型）
- `evalMode` - 评估模式（automatic / human / hybrid）
- `sampleCount` - 评估样本数量
- `textualGradient` - 文本梯度描述（自然语言反馈）

**作用：**
为优化器提供统一的优化方向信号，支持多模态评估输入。

---

## 包结构

```
com.alibab.aone.evox.optimizers
├── core/
│   ├── Optimizer.java              # 优化器基类
│   ├── OptimizationResult.java     # 优化结果
│   └── EvaluationFeedback.java     # 评估反馈
├── agent/
│   ├── AgentOptimizer.java         # Agent 优化器基类
│   ├── TextGradOptimizer.java      # TextGrad 实现
│   ├── MiproOptimizer.java         # MIPRO 实现
│   └── EvoPromptOptimizer.java     # EvoPrompt 实现
├── workflow/
│   ├── WorkflowOptimizer.java      # Workflow 优化器基类
│   ├── AFlowOptimizer.java         # AFlow (MCTS) 实现
│   └── SewOptimizer.java           # SEW 实现
├── memory/
│   ├── BaseMemoryOptimizer.java    # Memory 优化器基类
│   └── MemoryOptimizer.java        # Memory 优化器实现
└── config/
    └── OptimizerConfig.java        # 优化器配置
```

---

## Maven 依赖

```xml
<dependency>
    <groupId>com.alibaba.aone.evox</groupId>
    <artifactId>evox-optimizers</artifactId>
    <version>${evox.version}</version>
</dependency>
```

---

## 参考文献

- **TextGrad**: Automatic differentiation for language models
- **MIPRO**: Multimodal Instruction and Prompt Optimization
- **AFlow**: Automating Agentic Workflow Generation with MCTS
- **SEW**: Sequential Evolution for Workflow Optimization
- **EvoAgentX**: Evolutionary Multi-Agent Systems
