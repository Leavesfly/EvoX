# 应用层 (Application Layer)

应用层提供示例应用和性能基准测试。

## 模块列表

| 模块 | 说明 | 依赖 |
|------|------|------|
| **evox-examples** | 示例应用，展示各种使用场景 | 多个下层模块 |
| **evox-benchmark** | 性能基准测试，提供标准化测试集 | evox-core |

## 设计原则

- **场景丰富**: 覆盖各种典型使用场景
- **易于理解**: 代码简洁清晰，注释完善
- **开箱即用**: 提供完整的运行示例
- **性能标准**: 提供标准化的性能测试

## 依赖关系

```
evox-examples ──┬──> evox-agents
                ├──> evox-workflow
                ├──> evox-tools
                ├──> evox-memory
                └──> 其他业务模块

evox-benchmark ──> evox-core
```

## 示例列表

evox-examples 包含以下示例：

- **SimpleChatBot**: 基础聊天机器人
- **MemoryAgentExample**: 带记忆的对话系统
- **ToolsExample**: 工具集成示例
- **WorkflowDemo**: 工作流编排示例
- **ComprehensiveChatBot**: 综合型聊天机器人

## 基准测试

evox-benchmark 提供以下测试：

- **GSM8K**: 数学问题求解
- **HumanEval**: 代码生成评估
- **MBPP**: Python 编程基准
- **HotpotQA**: 多跳问答测试
