# EvoX Benchmark 性能基准测试

## 📦 模块定位

**层级**: 应用层 (Application Layer)  
**职责**: 提供标准化性能基准测试  
**依赖**: evox-core

## 🎯 测试集

### 1. GSM8K - 数学推理

8000+小学数学问题:

```java
GSM8K benchmark = new GSM8K(llm);
BenchmarkResult result = benchmark.run();

System.out.println("准确率: " + result.getAccuracy());
```

### 2. HumanEval - 代码生成

164个Python编程问题:

```java
HumanEval benchmark = new HumanEval(llm);
BenchmarkResult result = benchmark.run();
```

### 3. MBPP - Python编程

500+Python编程基准:

```java
MBPP benchmark = new MBPP(llm);
BenchmarkResult result = benchmark.run();
```

### 4. HotpotQA - 多跳问答

多跳推理问答测试:

```java
HotpotQA benchmark = new HotpotQA(llm);
BenchmarkResult result = benchmark.run();
```

### 5. MMLU - 大规模多任务语言理解

57个学科、15,000+多选题，覆盖STEM、人文、社科等领域，LLM综合能力评测的核心基准:

```java
MMLU benchmark = new MMLU("/path/to/mmlu.jsonl");

// 单样本评估
Map<String, Double> result = benchmark.evaluate("A", "A");

// 按学科分组评估
Map<String, Double> subjectScores = benchmark.evaluateBySubject(predictions);

// 生成few-shot prompt
String prompt = benchmark.formatPrompt(example, fewShotExamples);
```

### 6. DROP - 段落离散推理

96,000+阅读理解样本，需要数值推理（加减、计数、排序、日期比较等）:

```java
DROP benchmark = new DROP("/path/to/drop.jsonl");

// 评估（支持多答案匹配）
Map<String, Double> result = benchmark.evaluate(
    "3 touchdowns", 
    Arrays.asList("3", "three")
);
// -> {exact_match: 1.0, f1_score: 1.0}

// 生成prompt
String prompt = benchmark.formatPrompt(example);
```

### 7. C-Eval - 中文综合能力评测 🇨🇳

13,948个多选题，52个学科，4个难度级别（初中→专业），中文LLM必测基准:

```java
CEval benchmark = new CEval("/path/to/ceval.jsonl");

// 单样本评估
Map<String, Double> result = benchmark.evaluate("A", "A");

// 按学科分组评估
Map<String, Double> subjectScores = benchmark.evaluateBySubject(predictions);

// 按难度级别评估
Map<String, Double> difficultyScores = benchmark.evaluateByDifficulty(predictions);

// 生成中文few-shot prompt
String prompt = benchmark.formatPrompt(example, fewShotExamples);
// -> "以下是关于高等数学的单项选择题，请直接给出正确答案的选项。\n\n题目：..."
```

### 8. CMMLU - 中文多任务语言理解 🇨🇳

67个主题，侧重中国文化特有知识（中医、古文、传统文化、公务员考试等）:

```java
CMMLU benchmark = new CMMLU("/path/to/cmmlu.jsonl");

// 单样本评估
Map<String, Double> result = benchmark.evaluate("B", "B");

// 按主题分组评估
Map<String, Double> subjectScores = benchmark.evaluateBySubject(predictions);

// 按大类评估（STEM/社科/人文/中国特色/其他）
Map<String, Double> categoryScores = benchmark.evaluateByCategory(predictions);

// 生成中文few-shot prompt
String prompt = benchmark.formatPrompt(example, fewShotExamples);
```

## 🚀 运行测试

### 运行单个基准

```bash
mvn test -Dtest=GSM8KTest
```

### 运行所有基准

```bash
mvn test
```

## ⚙️ 配置

```yaml
evox:
  benchmark:
    warmup-iterations: 3
    measurement-iterations: 10
    timeout: 300000
    output-directory: ./benchmark-results
```

## 📊 结果

测试结果保存在 `benchmark-results/` 目录:

- 准确率
- 平均耗时
- 详细日志

## 🔗 相关模块

- **evox-core**: 基础框架
- **evox-models**: LLM模型
