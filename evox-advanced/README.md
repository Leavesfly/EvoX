# 高级业务层 (Advanced Layer)

高级业务层提供更高级的业务能力，包括优化器(含评估器)和人机协同。

## 模块列表

| 模块 | 说明 | 依赖 |
|------|------|------|
| **evox-optimizers** | 性能优化器，支持 TextGrad、MIPRO、AFlow 等优化算法，包含评估器 | evox-core, evox-models, evox-agents, evox-workflow |
| **evox-hitl** | 人机协同（Human-in-the-Loop），支持人工介入和决策 | evox-core, evox-agents, evox-workflow |

## 设计原则

- **智能优化**: 基于业务层提供智能优化能力
- **人机结合**: 支持人工介入关键决策
- **效果量化**: 提供量化的评估指标
- **持续改进**: 支持系统持续优化改进

## 依赖关系

```
evox-optimizers ──┬──> evox-core
                  ├──> evox-models
                  ├──> evox-agents
                  └──> evox-workflow

evox-hitl ──┬──> evox-core
            ├──> evox-agents
            └──> evox-workflow
```

## 特点

这一层的模块通常：
- 依赖业务层（Agents、Workflow）
- 提供元级能力（优化、评估、人机协同）
- 面向系统整体质量提升
