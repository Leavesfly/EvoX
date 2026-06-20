# EvoX Evaluation

EvoX 评估模块，提供任务特定评估（Task-Specific）和 LLM 评估（LLM-as-Judge）两种互补的评估方式，用于量化 Agent、工作流和模型输出的性能表现。

## 模块概述

`evox-evaluation` 是 EvoX 框架自进化闭环中的关键组件，对应论文中的评估函数 **P = T(W, D)**——将工作流执行映射为聚合性能指标。支持 WorkFlowGraph（端到端）和 ActionGraph（节点级）两种抽象级别的评估。

## 核心架构

```
io.leavesfly.evox.evaluation
├── Evaluator                        # 评估器基类（支持指标注册 & 批量评估）
├── dataset/                         # 评估数据集
│   ├── EvaluationDataset            # 数据集接口
│   ├── SimpleEvaluationDataset      # 内存数据集实现
│   └── DatasetSplit                 # 数据集分割
├── metrics/                         # 评估指标
│   ├── EvaluationMetric             # 指标接口
│   ├── ExactMatchMetric             # 精确匹配
│   ├── F1ScoreMetric                # F1 分数（Token 级别）
│   ├── PassAtKMetric                # Pass@K（代码生成）
│   └── SolveRateMetric              # 求解率（数学推理）
├── task/                            # 任务特定评估器
│   ├── QAEvaluator                  # 问答评估
│   ├── CodeEvaluator                # 代码生成评估
│   ├── DialogueEvaluator            # 对话评估
│   └── SummarizationEvaluator       # 摘要评估
├── llm/                             # LLM 评估器
│   └── LLMJudgeEvaluator            # LLM-as-Judge 多维度评估
├── workflow/                        # 工作流评估
│   └── WorkflowEvaluator            # 工作流级别评估 P = T(W, D)
└── pipeline/                        # 评估流水线
    └── EvaluationPipeline           # 编排多评估器的综合评估流程
```

## 快速开始

### Maven 依赖

```xml
<dependency>
    <groupId>io.leavesfly.evox</groupId>
    <artifactId>evox-evaluation</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 基本用法：任务特定评估

```java
// 创建 QA 评估器
QAEvaluator qaEvaluator = new QAEvaluator();
qaEvaluator.setEnableSemanticSimilarity(true);

// 注册额外指标
qaEvaluator.registerMetric(new ExactMatchMetric());
qaEvaluator.registerMetric(new F1ScoreMetric());

// 单样本评估
EvaluationResult result = qaEvaluator.evaluate(
    "Paris is the capital of France.",
    "The capital of France is Paris."
);

// 批量评估
Object[] predictions = {"answer1", "answer2", "answer3"};
Object[] labels = {"label1", "label2", "label3"};
EvaluationResult batchResult = qaEvaluator.evaluateBatch(predictions, labels);
```

### LLM-as-Judge 评估

```java
LLMJudgeEvaluator llmJudge = new LLMJudgeEvaluator();
llmJudge.setJudgeLLM(llmProvider);
llmJudge.setMaxScore(10);
llmJudge.setEnableConsistencyCheck(true);

// 添加自定义评估标准
llmJudge.addDynamicCriterion("creativity（创造性）：回答是否具有创新性？");
llmJudge.addDynamicCriterion("professionalism（专业性）：回答是否体现领域专业知识？");

EvaluationResult result = llmJudge.evaluate(prediction, label);
// 返回归一化到 [0, 1] 的多维评分：accuracy, completeness, relevance, clarity, overall...
```

### 工作流级别评估

```java
// 创建工作流评估器
WorkflowEvaluator workflowEval = new WorkflowEvaluator();
workflowEval.setMaxConcurrentTasks(10);
workflowEval.setContinueOnFailure(true);
workflowEval.registerMetric(new ExactMatchMetric());
workflowEval.registerMetric(new F1ScoreMetric());

// 在数据集上评估工作流
EvaluationResult result = workflowEval.evaluateOnDataset(
    input -> myWorkflow.execute(input),  // 工作流执行函数
    dataset                               // 评估数据集
);

// 异步评估（带并发控制）
Mono<EvaluationResult> asyncResult = workflowEval.evaluateOnDatasetAsync(
    input -> myWorkflow.execute(input),
    dataset
);
```

### 评估流水线（组合多个评估器）

```java
// 构建流水线
EvaluationPipeline pipeline = new EvaluationPipeline("qa-pipeline");
pipeline.addEvaluator(new QAEvaluator())
        .addEvaluator(llmJudge)
        .withWorkflowEvaluator(workflowEval);

// WorkFlowGraph 级别评估（端到端）
EvaluationResult workflowResult = pipeline.evaluateWorkflowGraph(
    input -> myWorkflow.execute(input),
    dataset
);

// ActionGraph 级别评估（逐节点）
Map<String, EvaluationResult> actionResults = pipeline.evaluateActionGraph(
    actionExecutors,  // Map<ActionName, ExecutionFunction>
    actionDatasets    // Map<ActionName, Dataset>
);

// 综合评估（同时执行两个级别）
EvaluationPipeline.CombinedEvaluationResult combined = pipeline.evaluateCombined(
    workflowExecutor, workflowDataset,
    actionExecutors, actionDatasets
);
```

### 构建评估数据集

```java
// 方式一：从列表构建
List<Map<String, Object>> inputs = List.of(
    Map.of("question", "What is 2+2?"),
    Map.of("question", "Capital of France?")
);
List<Object> labels = List.of("4", "Paris");

SimpleEvaluationDataset dataset = SimpleEvaluationDataset.of("math-qa", inputs, labels);

// 方式二：获取子集
EvaluationDataset subset = dataset.subset(0, 10);
```

## 评估指标

| 指标 | 类名 | 适用场景 | 输出范围 |
|------|------|---------|---------|
| 精确匹配 | `ExactMatchMetric` | 通用 | 0 或 1 |
| F1 分数 | `F1ScoreMetric` | QA（HotpotQA 等） | [0, 1] |
| Pass@K | `PassAtKMetric` | 代码生成（MBPP/HumanEval） | 0 或 1 |
| 求解率 | `SolveRateMetric` | 数学推理（GSM8K/MATH） | 0 或 1 |

## 任务评估器

| 评估器 | 评估维度 |
|--------|---------|
| `QAEvaluator` | 精确匹配、语义相似度、完整性、相关性 |
| `CodeEvaluator` | 语法正确性、代码相似度、代码质量、测试通过率 |
| `DialogueEvaluator` | 相关性、连贯性、信息量、安全性、有用性 |
| `SummarizationEvaluator` | ROUGE-1/2/L、压缩率、流畅性、连贯性 |

## LLM 评估器维度

`LLMJudgeEvaluator` 支持三种评估模式：

1. **质量评估** — 准确性、完整性、相关性、清晰度（+ 可扩展的动态标准）
2. **一致性检查** — 事实一致性、语义相似度、无矛盾度
3. **动态标准评估** — 用户自定义评估维度，适应静态指标无法捕捉的场景

所有 LLM 评分归一化到 `[0, 1]` 范围。

## 评估级别

| 级别 | 说明 | API |
|------|------|-----|
| **WorkFlowGraph** | 端到端评估整个工作流 | `pipeline.evaluateWorkflowGraph(...)` |
| **ActionGraph** | 独立评估每个 Action 节点 | `pipeline.evaluateActionGraph(...)` |
| **Combined** | 同时执行上述两个级别 | `pipeline.evaluateCombined(...)` |

## 依赖

- `evox-core` — 核心接口（IEvaluator, EvaluationResult, BaseModule）
- `evox-models` — LLM Provider 接口（用于 LLMJudgeEvaluator）
- Spring Boot Starter
- Project Reactor（异步评估支持）
- Jackson（JSON 解析）
- Lombok
