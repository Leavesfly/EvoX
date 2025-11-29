# 核心层 (Core Layer)

核心层是 EvoX 架构的基础，提供最底层的抽象和基础设施。

## 模块列表

| 模块 | 说明 | 依赖 |
|------|------|------|
| **evox-core** | 核心抽象和基础设施，提供 BaseModule、Message、Registry 等基础接口 | 无 |
| **evox-models** | LLM 模型适配层，支持 OpenAI、阿里云、SiliconFlow 等 | evox-core |
| **evox-actions** | 动作执行引擎，提供 Action 系统和各类专业动作 | evox-core, evox-models |

## 设计原则

- **最小依赖**: 核心层模块尽量减少外部依赖
- **稳定接口**: 提供稳定的 API 接口供上层使用
- **高内聚**: 每个模块职责单一，边界清晰
- **低耦合**: 模块间通过接口通信，降低耦合度

## 依赖关系

```
evox-core (基础)
    ↑
    ├── evox-models
    └── evox-actions
```
