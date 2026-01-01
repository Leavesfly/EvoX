# EvoX 示例应用总结

## 概述

本文档总结了为 EvoX Optimizers 和 HITL 模块创建的示例应用，展示实际使用场景。

## 📦 已创建的示例

### 1. 优化器示例 (SimpleOptimizerExample)

**文件位置**: `evox-examples/src/main/java/io/leavesfly/evox/examples/optimizer/SimpleOptimizerExample.java`

**功能**: 
- 演示三种优化器的基本配置和使用
- 展示优化结果对比

**涵盖的优化器**:
- ✅ **TextGrad 优化器** - 提示词梯度优化
  - 优化模式: all/system_prompt/instruction
  - 批量大小、学习率配置
  - 历史追踪

- ✅ **MIPRO 优化器** - 迭代提示优化
  - 自动模式: light/medium/heavy
  - 引导示例和标注示例
  - 候选生成

- ✅ **AFlow 优化器** - 工作流结构优化
  - MCTS 风格探索
  - 种群管理
  - 经验回放

**运行方式**:
```bash
cd evox-examples
./run-examples.sh
# 选择选项 1
```

**预期输出**:
```
优化器        最终得分    总步数
----------------------------------------
TextGrad      0.7000      5
MIPRO         0.7500      5
AFlow         0.7800      5
```

---

### 2. HITL 审批示例 (EmailSendingWithApprovalExample)

**文件位置**: `evox-examples/src/main/java/io/leavesfly/evox/examples/hitl/EmailSendingWithApprovalExample.java`

**场景**: 
邮件发送前需要人工审批，确保内容和收件人正确

**工作流**:
```
数据提取 → HITL拦截器(审批) → 邮件发送
```

**核心组件**:
- ✅ **DataExtractionAction** - 提取邮件信息
- ✅ **HITLInterceptorAgent** - 前置拦截审批
- ✅ **EmailSendingAction** - 发送邮件
- ✅ **HITLManager** - 管理人机交互

**运行方式**:
```bash
cd evox-examples
./run-examples.sh
# 选择选项 2
```

**交互流程**:
```
🔔 Human-in-the-Loop Approval Request
================================================================================
Task: email_sending_task
Agent: EmailSendingAgent
Action: EmailSendingAction
Mode: Pre-Execution Approval

Parameters to be executed:
  recipient: customer@example.com
  subject: 订单确认
  content: 您的订单已确认...
================================================================================

Please select [a]pprove / [r]eject: _
```

---

## 🚀 快速开始

### 方法 1: 使用运行脚本 (推荐)

```bash
cd evox/evox-examples
./run-examples.sh
```

脚本提供的选项:
1. 运行优化器示例
2. 运行 HITL 审批示例
3. 编译所有模块
4. 运行所有测试

### 方法 2: 直接使用 Maven

```bash
# 优化器示例
mvn exec:java -Dexec.mainClass="io.leavesfly.evox.examples.optimizer.SimpleOptimizerExample"

# HITL 示例
mvn exec:java -Dexec.mainClass="io.leavesfly.evox.examples.hitl.EmailSendingWithApprovalExample"
```

---

## 📋 示例特性对比

| 特性 | 优化器示例 | HITL 示例 |
|------|-----------|----------|
| **复杂度** | 简单 | 中等 |
| **交互性** | 无需交互 | 需要人工输入 |
| **运行时间** | < 10秒 | 取决于审批速度 |
| **依赖** | 基础模块 | 基础模块 + 工作流 |
| **适用场景** | 学习优化器 | 学习 HITL 机制 |

---

## 🔧 配置要求

### 必需配置

1. **Java 17+**
   ```bash
   java -version
   # java version "17.0.x"
   ```

2. **Maven 3.8+**
   ```bash
   mvn -version
   # Apache Maven 3.8.x
   ```

### 可选配置

1. **OpenAI API Key** (用于实际 LLM 调用)
   ```bash
   export OPENAI_API_KEY="sk-your-api-key"
   ```

2. **日志级别**
   ```bash
   # 设置为 DEBUG 查看详细日志
   export LOGGING_LEVEL_ROOT=DEBUG
   ```

---

## 📚 学习路径

### 初学者
1. 先运行优化器示例，理解三种优化器的区别
2. 阅读源码，了解优化器的参数配置
3. 尝试修改参数，观察输出变化

### 进阶用户
1. 运行 HITL 示例，理解人机交互流程
2. 修改工作流节点，添加更多业务逻辑
3. 结合优化器和 HITL 创建复杂应用

### 高级用户
1. 集成真实数据集和评估函数
2. 实现自定义优化器策略
3. 扩展 HITL 交互类型

---

## 📖 扩展阅读

### 示例文档
- [示例详细说明](evox-examples/README-OPTIMIZERS-HITL.md)
- [优化器实现总结](OPTIMIZERS_HITL_IMPLEMENTATION.md)
- [项目进度](PROGRESS_TRACKING.md)

### 源码参考
- [Optimizer 基类](evox-optimizers/src/main/java/io/leavesfly/evox/optimizers/Optimizer.java)
- [HITLManager](evox-hitl/src/main/java/io/leavesfly/evox/hitl/HITLManager.java)
- [HITLInterceptorAgent](evox-hitl/src/main/java/io/leavesfly/evox/hitl/HITLInterceptorAgent.java)

### 测试用例
- [优化器测试](evox-optimizers/src/test/java/io/leavesfly/evox/optimizers/OptimizerTest.java)
- [HITL 测试](evox-hitl/src/test/java/io/leavesfly/evox/hitl/HITLTest.java)

---

## 🎯 下一步

### 短期目标
- [ ] 添加更多实际场景示例
- [ ] 集成 Benchmark 数据集
- [ ] 创建 GUI 交互界面

### 中期目标
- [ ] 性能基准测试
- [ ] 优化器对比分析
- [ ] HITL 模式最佳实践文档

### 长期目标
- [ ] 生产环境部署示例
- [ ] 分布式优化示例
- [ ] 可视化优化过程

---

## 🤝 贡献

欢迎贡献更多示例！

**贡献指南**:
1. Fork 项目
2. 在 `evox-examples/src/main/java/io/leavesfly/evox/examples/` 下创建新示例
3. 添加详细注释和 README
4. 提交 Pull Request

**示例要求**:
- 代码清晰，注释完整
- 提供运行说明
- 包含预期输出
- 遵循项目代码规范

---

## 📞 支持

如有问题，请:
1. 查看 [FAQ](FAQ.md)
2. 提交 [Issue](https://github.com/your-org/evox/issues)
3. 加入社区讨论

---

## 📄 许可证

本项目采用 Apache 2.0 许可证

---

**最后更新**: 2025-11-25  
**维护者**: EvoX Team  
**版本**: v1.0
