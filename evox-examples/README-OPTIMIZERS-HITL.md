# EvoX Optimizers 和 HITL 示例应用

本目录包含 EvoX 优化器和人机协同(HITL)模块的实际使用示例。

## 📁 示例目录结构

```
evox-examples/
├── src/main/java/io/leavesfly/evox/examples/
│   ├── optimizer/
│   │   └── SimpleOptimizerExample.java      # 优化器基础使用示例
│   └── hitl/
│       └── EmailSendingWithApprovalExample.java  # HITL 审批示例
└── README-OPTIMIZERS-HITL.md                # 本文档
```

## 🎯 示例概览

### 1. 优化器示例 (SimpleOptimizerExample)

**场景**: 展示三种优化器的基本使用方法

**包含内容**:
- TextGrad 优化器配置和使用
- MIPRO 优化器配置和使用  
- AFlow 优化器配置和使用
- 优化结果对比

**运行方式**:
```bash
cd evox-examples
mvn clean compile
mvn exec:java -Dexec.mainClass="io.leavesfly.evox.examples.optimizer.SimpleOptimizerExample"
```

**预期输出**:
```
========================================
优化器示例：三种优化器使用演示
========================================

【示例 1】TextGrad 优化器
----------------------------------------
配置:
  - 优化模式: all
  - 批量大小: 3
  - 最大步数: 5
开始优化...
优化结果:
  - 成功: true
  - 最终得分: 0.7000
  - 总步数: 5
  ...
```

### 2. HITL 审批示例 (EmailSendingWithApprovalExample)

**场景**: 邮件发送前需要人工审批

**工作流程**:
1. **数据提取代理** - 从原始文本中提取邮件信息
2. **HITL 拦截器** - 拦截并请求人工审批
3. **邮件发送代理** - 发送邮件(仅在批准后执行)

**运行方式**:
```bash
mvn exec:java -Dexec.mainClass="io.leavesfly.evox.examples.hitl.EmailSendingWithApprovalExample"
```

**交互示例**:
```
🔔 Human-in-the-Loop Approval Request
================================================================================
Task: email_sending_task
Agent: EmailSendingAgent
Action: EmailSendingAction (PRE-EXECUTION)
Workflow Goal: 发送订单确认邮件
Mode: Pre-Execution Approval

Parameters to be executed:
  email_data: {"recipient":"customer@example.com",...}
================================================================================

Please select [a]pprove / [r]eject: a

✅ Approved! Proceeding with email sending...
```

## 🔧 配置说明

### 1. OpenAI API Key

示例中使用了 OpenAI API，需要配置有效的 API Key:

```java
OpenAILLMConfig config = OpenAILLMConfig.builder()
    .model("gpt-4o-mini")
    .apiKey(System.getenv("OPENAI_API_KEY"))  // 从环境变量读取
    .temperature(0.7)
    .build();
```

设置环境变量:
```bash
export OPENAI_API_KEY="your-api-key-here"
```

### 2. 优化器参数

#### TextGrad 优化器参数

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `optimizeMode` | 优化模式 (all/system_prompt/instruction) | all |
| `batchSize` | 批量大小 | 3 |
| `maxSteps` | 最大优化步数 | 5 |
| `learningRate` | 学习率 | 0.1 |

#### MIPRO 优化器参数

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `autoMode` | 自动模式 (light/medium/heavy) | medium |
| `maxBootstrappedDemos` | 最大引导示例数 | 4 |
| `maxLabeledDemos` | 最大标注示例数 | 4 |
| `numCandidates` | 候选数 (自动设置) | 12 |

#### AFlow 优化器参数

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `maxIterations` | 最大迭代次数 | 10 |
| `populationSize` | 种群大小 | 5 |
| `convergenceWindow` | 收敛窗口 | 3 |

### 3. HITL 配置

#### HITLManager 配置

```java
HITLManager hitlManager = new HITLManager();
hitlManager.activate();                    // 激活 HITL
hitlManager.setDefaultTimeout(1800);       // 设置超时(秒)
```

#### HITLInterceptorAgent 配置

```java
HITLInterceptorAgent interceptor = HITLInterceptorAgent.builder()
    .name("InterceptorName")
    .targetAgentName("TargetAgent")        // 要拦截的代理名
    .targetActionName("TargetAction")      // 要拦截的动作名
    .interactionType(HITLInteractionType.APPROVE_REJECT)  // 交互类型
    .mode(HITLMode.PRE_EXECUTION)          // 拦截模式
    .hitlManager(hitlManager)              // HITL 管理器
    .build();
```

## 📊 优化器使用场景

### TextGrad 适用场景
- ✅ 提示词细粒度优化
- ✅ 系统提示和指令联合优化
- ✅ 需要梯度反馈的优化任务

### MIPRO 适用场景
- ✅ 提示模板参数化优化
- ✅ 示例驱动的优化
- ✅ 多候选方案搜索

### AFlow 适用场景
- ✅ 工作流结构优化
- ✅ 算子选择和组合
- ✅ 需要探索型搜索的优化

## 🔍 HITL 使用场景

### 前置审批 (PRE_EXECUTION)
- ✅ 敏感操作确认(如发送邮件、删除数据)
- ✅ 参数验证
- ✅ 风险控制

### 后置审批 (POST_EXECUTION)  
- ✅ 结果审查
- ✅ 内容审核
- ✅ 质量检查

### 用户输入收集
- ✅ 动态参数获取
- ✅ 用户反馈收集
- ✅ 交互式配置

## 💡 最佳实践

### 优化器最佳实践

1. **选择合适的优化器**
   - 提示词优化 → TextGrad
   - 参数搜索 → MIPRO
   - 结构优化 → AFlow

2. **设置合理的参数**
   ```java
   // 开始时使用较小的参数快速迭代
   .maxSteps(3)
   .batchSize(2)
   
   // 验证后增加参数进行精细优化
   .maxSteps(20)
   .batchSize(5)
   ```

3. **监控优化过程**
   ```java
   optimizer.optimize(dataset, kwargs);
   List<StepResult> history = optimizer.getHistory();
   // 分析优化历史
   ```

4. **保存和恢复最佳模型**
   ```java
   textGradOptimizer.restoreBestWorkflow();
   ```

### HITL 最佳实践

1. **选择合适的拦截模式**
   ```java
   // 高风险操作使用前置拦截
   .mode(HITLMode.PRE_EXECUTION)
   
   // 内容审核使用后置拦截
   .mode(HITLMode.POST_EXECUTION)
   ```

2. **设置合理的超时**
   ```java
   hitlManager.setDefaultTimeout(300);  // 5分钟
   ```

3. **提供清晰的上下文信息**
   ```java
   inputs.put("task_name", "clear_task_description");
   inputs.put("workflow_goal", "what_we_want_to_achieve");
   ```

4. **处理审批结果**
   ```java
   HITLResponse response = interceptor.intercept(...).block();
   
   switch (response.getDecision()) {
       case APPROVE -> proceedWithAction();
       case REJECT -> handleRejection(response.getFeedback());
       case MODIFY -> applyModifications(response.getModifiedContent());
   }
   ```

## 🐛 常见问题

### Q1: 优化器没有实际优化效果?

**A**: 当前示例使用的是简化实现，实际使用需要:
1. 提供真实的数据集
2. 实现具体的评估函数
3. 配置有效的 LLM API

### Q2: HITL 拦截器没有触发?

**A**: 检查以下几点:
1. HITLManager 是否已激活: `hitlManager.activate()`
2. 拦截器是否正确配置目标代理和动作名称
3. 拦截器是否添加到 AgentManager

### Q3: 优化过程中内存占用过高?

**A**: 可以:
1. 减小批量大小 `batchSize`
2. 限制经验缓冲区大小
3. 定期清理历史记录

## 📚 参考资料

### 优化器
- [TextGrad 论文](https://www.nature.com/articles/s41586-025-08661-4)
- [MIPRO 论文](https://arxiv.org/abs/2406.11695)
- [AFlow 论文](https://arxiv.org/abs/2410.10762)

### HITL
- [人机协同设计模式](../docs/hitl-patterns.md)
- [工作流集成指南](../docs/workflow-integration.md)

### 源码
- [evox-optimizers 源码](../evox-optimizers/src/main/java/io/leavesfly/evox/optimizers/)
- [evox-hitl 源码](../evox-hitl/src/main/java/io/leavesfly/evox/hitl/)
- [优化器单元测试](../evox-optimizers/src/test/java/io/leavesfly/evox/optimizers/)
- [HITL 单元测试](../evox-hitl/src/test/java/io/leavesfly/evox/hitl/)

## 🤝 贡献

欢迎提交更多示例! 请参考 [贡献指南](../CONTRIBUTING.md)

## 📄 许可证

本项目采用 Apache 2.0 许可证
