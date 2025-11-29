# 能力层 (Capability Layer)

能力层为业务层提供各种通用能力，包括存储、记忆、工具和工具类。

## 模块列表

| 模块 | 说明 | 依赖 |
|------|------|------|
| **evox-storage** | 存储适配层，支持内存、数据库、向量、图等多种存储 | evox-core |
| **evox-memory** | 记忆管理系统，提供短期和长期记忆能力 | evox-core, evox-storage |
| **evox-tools** | 工具集成框架，提供文件、HTTP、数据库、搜索等工具 | evox-core |
| **evox-utils** | 工具类库，提供通用工具函数 | 无 |

## 设计原则

- **可插拔**: 各能力模块可独立使用或组合使用
- **易扩展**: 提供扩展点，方便添加新的能力
- **高性能**: 注重性能优化，减少资源消耗
- **通用性**: 能力模块设计通用化，适用多种场景

## 依赖关系

```
evox-storage ──> evox-core
    ↑
evox-memory ──┘

evox-tools ──> evox-core

evox-utils (独立)
```
