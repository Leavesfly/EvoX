# EvoX 架构设计文档

## 📐 架构概览

EvoX 采用分层架构设计，将系统划分为 4 个层次，从底层到顶层依次为：

1. **核心层** (Core Layer) — `evox-core/`
2. **运行时层** (Runtime Layer) — `evox-runtime/`
3. **扩展层** (Extensions Layer) — `evox-extensions/`
4. **应用层** (Application Layer) — `evox-application/`

## 🏗️ 分层架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                      应用层 (Applications)                       │
│       evox-examples / evox-benchmark / evox-spring-boot-starter │
│                   evox-claudecode / evox-cowork                 │
├─────────────────────────────────────────────────────────────────┤
│                     扩展层 (Extensions)                         │
│  ┌────────────────────────┬──────────────────────┐              │
│  │  evox-optimizers       │     evox-hitl         │              │
│  │  优化器(TextGrad等)    │     人机协同          │              │
│  │  evox-evaluation       │                      │              │
│  │  评估框架              │                      │              │
│  └────────────────────────┴──────────────────────┘              │
├─────────────────────────────────────────────────────────────────┤
│                     运行时层 (Runtime)                           │
│  ┌──────────┬──────────┬──────────┬──────────┬──────────┐      │
│  │  Agents  │ Workflow │   RAG    │  Memory  │ Storage  │      │
│  │ 智能代理 │ 工作流   │ 检索增强 │ 记忆管理 │ 存储适配 │      │
│  │          │          │          │          │          │      │
│  │  Tools   │          │          │          │          │      │
│  │ 工具集   │          │          │          │          │      │
│  └──────────┴──────────┴──────────┴──────────┴──────────┘      │
├─────────────────────────────────────────────────────────────────┤
│                     核心层 (Core)                                │
│  ┌────────────────┬────────────────┬────────────────────┐      │
│  │   evox-core    │  evox-models   │   evox-actions     │      │
│  │  核心抽象       │  模型适配       │   动作引擎         │      │
│  │ ILLM/IAgent    │  BaseLLM实现    │  Action系统        │      │
│  │ IAgentManager  │  LLMConfig     │                    │      │
│  │                │                │   evox-mcp         │      │
│  │                │                │  MCP协议支持        │      │
│  └────────────────┴────────────────┴────────────────────┘      │
├─────────────────────────────────────────────────────────────────┤
│                    基础设施层 (Infrastructure)                   │
│          Spring Boot 3.2+ / Spring AI 1.0+ / Reactor           │
└─────────────────────────────────────────────────────────────────┘
```

## 📂 目录结构映射

```
evox/
├── evox-core/              # 核心层
│   ├── evox-core/          #   核心抽象（BaseModule、Message、ILLM、IAgent）
│   ├── evox-models/        #   LLM 模型适配（BaseLLM、LLMConfig、LLMFactory）
│   ├── evox-actions/       #   动作抽象（Action、ActionInput、ActionOutput 基类）
│   └── evox-mcp/           #   MCP 协议定义（MCPProtocol、MCPResource、MCPTool）
├── evox-runtime/           # 运行时层
│   ├── evox-storage/       #   存储适配（内存、数据库、向量、图、StorageConfig）
│   ├── evox-tools/         #   工具集（文件、HTTP、数据库、搜索）
│   ├── evox-memory/        #   记忆管理（短期、长期）
│   ├── evox-rag/           #   检索增强生成
│   ├── evox-agents/        #   智能代理系统（Agent、具体Action实现、多智能体协同）
│   ├── evox-workflow/      #   工作流编排引擎
│   └── evox-mcp-runtime/   #   MCP 运行时（MCPServer、MCPClient、MCPTransport）
├── evox-extensions/        # 扩展层
│   ├── evox-optimizers/    #   性能优化器（TextGrad、MIPRO、AFlow）
│   ├── evox-evaluation/    #   评估框架
│   └── evox-hitl/          #   人机协同
└── evox-application/       # 应用层
    ├── evox-examples/      #   示例应用
    ├── evox-benchmark/     #   性能基准测试
    ├── evox-claudecode/    #   Claude Code 集成
    ├── evox-cowork/        #   协同工作
    └── evox-spring-boot-starter/  # Spring Boot Starter
```

## 🔗 依赖关系原则

### 1. 依赖方向规则

- ✅ **向下依赖**: 上层可以依赖下层
- ❌ **禁止向上依赖**: 下层不能依赖上层
- ❌ **禁止跨层反向依赖**: 核心层不能依赖运行时层
- ✅ **同层依赖**: 同层模块间可以相互依赖，但需避免循环依赖

### 2. 接口解耦机制

核心层通过接口抽象实现与上层的解耦：

| 核心层接口 | 运行时层实现 | 说明 |
|-----------|-------------|------|
| `ILLM` | `BaseLLM` | LLM 能力抽象，Action/Workflow 依赖接口而非实现 |
| `IAgent` | `Agent` | 智能体抽象，Workflow/Extensions 依赖接口而非实现 |
| `IAgentManager` | `AgentManager` | 智能体管理抽象 |

### 3. 各层依赖规则

| 层级 | 可依赖的层级 |
|------|-------------|
| 应用层 | 所有下层 |
| 扩展层 | 核心层、运行时层 |
| 运行时层 | 核心层 |
| 核心层 | 基础设施层 |

## 📦 各层详细说明

### 核心层 (Core Layer)

**位置**: `evox-core/`  
**职责**: 提供最底层的抽象、接口定义和基础设施

| 模块 | 说明 | 依赖 |
|------|------|------|
| evox-core | 核心抽象（BaseModule、Message、ILLM、IAgent、IAgentManager） | 无 |
| evox-models | LLM 模型适配（BaseLLM extends ILLM、LLMConfig、LLMFactory） | evox-core |
| evox-actions | 动作抽象（Action、ActionInput、ActionOutput 基类，不含具体实现） | evox-core |
| evox-mcp | MCP 协议定义（MCPProtocol、MCPResource、MCPTool、MCPPrompt） | evox-core |

**注意**: evox-storage 已从核心层移至运行时层，详见运行时层说明。

**设计原则**:
- 最小依赖原则：evox-actions 仅依赖 evox-core，不依赖 evox-models
- 纯抽象原则：核心层只包含接口和抽象类，具体实现在运行时层
- 接口隔离原则：通过 ILLM/IAgent 接口实现跨层解耦
- 稳定接口原则
- 高内聚低耦合

### 运行时层 (Runtime Layer)

**位置**: `evox-runtime/`  
**职责**: 提供智能体运行时所需的各种能力

| 模块 | 说明 | 依赖 |
|------|------|------|
| evox-storage | 存储适配（内存、数据库、向量、图、StorageConfig） | evox-core |
| evox-tools | 工具集（文件、HTTP、数据库、搜索） | evox-core |
| evox-memory | 记忆管理（短期、长期） | evox-core, evox-storage |
| evox-rag | 检索增强生成 | evox-core, evox-models, evox-storage |
| evox-agents | 智能代理系统（Agent implements IAgent、具体Action实现） | evox-core, evox-models, evox-actions, evox-memory(optional), evox-tools(optional) |
| evox-workflow | 工作流编排引擎（依赖 ILLM/IAgent 接口，不依赖 evox-agents/evox-models） | evox-core, evox-memory |
| evox-mcp-runtime | MCP 运行时（MCPServer、MCPClient、MCPTransport、MCPSession） | evox-core, evox-mcp |

**设计原则**:
- 可插拔设计
- 接口依赖：evox-workflow 通过 IAgent/ILLM 接口与 evox-agents/evox-models 解耦
- 易扩展性
- 高性能

### 扩展层 (Extensions Layer)

**位置**: `evox-extensions/`  
**职责**: 提供高级业务能力（优化、评估、人机协同）

| 模块 | 说明 | 依赖 |
|------|------|------|
| evox-optimizers | 性能优化器（TextGrad、MIPRO、AFlow） | evox-core, evox-models, evox-agents, evox-workflow |
| evox-evaluation | 评估框架 | evox-core, evox-models |
| evox-hitl | 人机协同 | evox-core, evox-agents, evox-workflow |

**设计原则**:
- 智能优化
- 人机结合
- 效果量化
- 持续改进

### 应用层 (Application Layer)

**位置**: `evox-application/`  
**职责**: 提供示例应用、基准测试和集成方案

| 模块 | 说明 | 依赖 |
|------|------|------|
| evox-examples | 示例应用 | 多个下层模块 |
| evox-benchmark | 性能基准测试 | evox-core |
| evox-spring-boot-starter | Spring Boot 自动配置 | 多个下层模块 |
| evox-claudecode | Claude Code 集成 | 多个下层模块 |
| evox-cowork | 协同工作 | 多个下层模块 |

**设计原则**:
- 场景丰富
- 易于理解
- 开箱即用
- 性能标准

## 🎯 架构优势

### 1. 清晰的层次关系

通过目录结构直接反映架构分层，降低学习成本：
- 新成员能快速定位模块位置
- 理解模块间的依赖关系更直观
- 便于进行架构评审和重构

### 2. 依赖关系可控

分层架构确保依赖关系单向、清晰：
- 避免循环依赖
- 降低模块间耦合度
- 便于独立测试和部署

### 3. 接口解耦

通过核心层接口实现跨层解耦：
- `ILLM` 接口让 Action/Workflow 不依赖具体 LLM 实现
- `IAgent` 接口让 Workflow/Extensions 不依赖具体 Agent 实现
- 运行时层模块可独立演进，不影响核心层稳定性

### 4. 易于扩展

每一层都有明确的扩展点：
- 核心层：新增模型适配器、核心接口
- 运行时层：新增存储类型、工具、Agent 类型、工作流模式
- 扩展层：新增优化算法、评估指标

### 5. 维护性强

分层架构降低维护成本：
- 变更影响范围可控
- 便于模块独立演进
- 测试策略清晰

## 📝 开发规范

### 模块创建规范

1. **确定层级**: 根据模块职责确定所属层级
2. **目录放置**: 将模块放入对应层级目录
3. **更新 POM**: 在父 POM 中添加模块引用（按层级分组）
4. **添加文档**: 在模块中添加 README 说明

### 依赖添加规范

1. **检查层级**: 确保依赖符合分层规则
2. **最小依赖**: 只依赖必要的模块
3. **避免传递**: 明确声明直接依赖
4. **版本管理**: 使用父 POM 统一管理版本

### 重构规范

1. **保持分层**: 重构时保持分层结构不变
2. **渐进式**: 采用渐进式重构，避免大规模改动
3. **测试保障**: 确保测试覆盖，避免回归
4. **文档同步**: 及时更新架构文档

## 🔄 架构演进记录

### 已完成的架构优化

1. ✅ **分层目录重组** — 从扁平结构迁移到 `evox-core/`、`evox-runtime/`、`evox-extensions/`、`evox-application/` 四层结构
2. ✅ **evox-rag 归位** — 从核心层移到运行时层，修复跨层反向依赖
3. ✅ **evox-capability 拆分** — 拆分为 evox-memory、evox-tools、evox-storage 三个独立模块，职责单一化
4. ✅ **evox-actions 解耦** — 创建 `ILLM` 接口，Action 不再直接依赖 evox-models
5. ✅ **evox-workflow 解耦** — 通过 `IAgent`/`ILLM` 接口，Workflow 不再直接依赖 evox-agents/evox-models
6. ✅ **evox-evaluation 解耦** — 移除对 evox-agents 的直接依赖
7. ✅ **evox-capability 清理** — 删除与 evox-memory/evox-tools/evox-storage 代码重复的遗留模块
8. ✅ **evox-tools 解耦** — 移除对 evox-models 的多余依赖，仅依赖 evox-core（ILLM 接口）
9. ✅ **领域异常下沉** — 将 StorageException 移至 evox-storage，LLMException 移至 evox-models，核心层仅保留通用异常
10. ✅ **核心层纯净化** — 将 StorageConfig 移至 evox-storage，删除核心层冗余的 EvoXAutoConfiguration
11. ✅ **Action 抽象与实现分离** — 核心层 evox-actions 仅保留 Action 基类，具体 Action 实现（CodeGeneration、TaskPlanning、CodeExtraction、Reflection、Customize）移至 evox-agents
12. ✅ **evox-agents 依赖减轻** — 将 evox-memory、evox-tools 标记为 optional 依赖
13. ✅ **evox-mcp 拆分** — 协议定义（MCPProtocol、MCPResource 等）留在核心层，Server/Client/Transport/Session 实现移至新模块 evox-runtime/evox-mcp-runtime

## 🚀 未来演进方向

### 短期（1-3 个月）

- 完善各层级的文档说明
- 补充架构设计原则的培训材料
- 建立代码审查 Checklist

### 中期（3-6 个月）

- 引入依赖检查工具（Maven Enforcer Plugin）
- 建立自动化架构合规性检查
- 完善单元测试和集成测试

### 长期（6-12 个月）

- 支持模块独立发布
- 建立模块版本管理策略
- 探索微服务化部署方案

## 📚 参考资料

- [Spring Boot 官方文档](https://spring.io/projects/spring-boot)
- [Maven 多模块项目最佳实践](https://maven.apache.org/guides/mini/guide-multiple-modules.html)
- [Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [分层架构模式](https://www.oreilly.com/library/view/software-architecture-patterns/9781491971437/ch01.html)

---

**文档版本**: 2.0.0  
**最后更新**: 2026-02-10  
**维护者**: EvoX Team
