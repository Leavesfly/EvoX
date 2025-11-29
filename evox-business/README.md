# 业务层 (Business Layer)

业务层实现核心业务逻辑，包括智能代理、多智能体协同框架、工作流和RAG。

## 模块列表

| 模块 | 说明 | 依赖 |
|------|------|------|
| **evox-agents** | 智能代理系统，提供多种专业代理、代理管理 | evox-core, evox-models, evox-actions, evox-tools |
| **evox-frameworks** | 多智能体协同框架，支持5种核心框架(辩论、共识、分层、拍卖、团队) | evox-core, evox-agents |
| **evox-workflow** | 工作流编排引擎，支持 DAG、条件分支、循环控制 | evox-core, evox-models, evox-memory, evox-storage |
| **evox-rag** | 检索增强生成，提供文档处理、向量化、语义检索 | evox-core, evox-models, evox-storage |

## 设计原则

- **业务聚焦**: 专注于业务逻辑实现
- **灵活编排**: 支持灵活的业务流程编排
- **状态管理**: 合理管理业务状态
- **错误处理**: 完善的异常处理机制

## 依赖关系

```
evox-agents ──┬──> evox-core
              ├──> evox-models
              ├──> evox-actions
              └──> evox-tools

evox-frameworks ──┬──> evox-core
                 └──> evox-agents

evox-workflow ──┬──> evox-core
                ├──> evox-models
                ├──> evox-memory
                └──> evox-storage

evox-rag ──┬──> evox-core
           ├──> evox-models
           └──> evox-storage
```
