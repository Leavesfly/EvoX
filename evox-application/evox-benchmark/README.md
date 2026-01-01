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
