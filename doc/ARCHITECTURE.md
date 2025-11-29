# EvoX 架构设计文档

## 📐 架构概览

EvoX 采用分层架构设计，将系统划分为 6 个层次，从底层到顶层依次为：

1. **基础设施层** (Infrastructure)
2. **核心层** (Core Layer)
3. **能力层** (Capability Layer)
4. **业务层** (Business Layer)
5. **高级业务层** (Advanced Layer)
6. **框架层** (Framework Layer)
7. **应用层** (Application Layer)

## 🏗️ 分层架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                      应用层 (Applications)                       │
│                evox-examples / evox-benchmark                   │
├─────────────────────────────────────────────────────────────────┤
│                     框架层 (Frameworks)                          │
│                      evox-frameworks                            │
│                   (多智能体框架、辩论系统)                          │
├─────────────────────────────────────────────────────────────────┤
│                    高级业务层 (Advanced Services)                │
│  ┌──────────────┬──────────────┬──────────────────────────┐   │
│  │  Optimizers  │     HITL     │       Evaluators         │   │
│  │   优化器      │   人机协同    │        评估器            │   │
│  │ (依赖Workflow)│ (依赖Workflow)│      (独立服务)          │   │
│  └──────────────┴──────────────┴──────────────────────────┘   │
├─────────────────────────────────────────────────────────────────┤
│                     业务层 (Business Logic)                     │
│  ┌─────────────┬─────────────┬─────────────┬─────────────┐    │
│  │   Agents    │  Workflow   │     RAG     │   Prompts   │    │
│  │   代理系统   │   工作流     │   检索增强   │  提示词管理  │    │
│  │ (依赖Tools) │ (依赖Memory) │ (依赖Storage)│  (工具类)   │    │
│  └─────────────┴─────────────┴─────────────┴─────────────┘    │
├─────────────────────────────────────────────────────────────────┤
│                    能力层 (Capabilities)                        │
│  ┌─────────────┬─────────────┬─────────────┬─────────────┐    │
│  │   Memory    │    Tools    │   Storage   │    Utils    │    │
│  │   记忆管理   │   工具集     │   存储适配   │   工具类     │    │
│  │(依赖Storage)│ (独立模块)   │  (独立模块)  │  (独立模块)  │    │
│  └─────────────┴─────────────┴─────────────┴─────────────┘    │
├─────────────────────────────────────────────────────────────────┤
│                     核心层 (Core Services)                      │
│  ┌────────────────┬────────────────┬────────────────────┐      │
│  │      Core      │     Models     │      Actions       │      │
│  │    核心抽象     │    模型适配     │     动作引擎        │      │
│  │  (基础接口)     │  (LLM适配)     │   (Action系统)      │      │
│  └────────────────┴────────────────┴────────────────────┘      │
├─────────────────────────────────────────────────────────────────┤
│                    基础设施层 (Infrastructure)                   │
│          Spring Boot 3.2+ / Spring AI 1.0+ / Reactor           │
└─────────────────────────────────────────────────────────────────┘
```

## 📂 目录结构映射

新的目录结构直接反映了架构分层：

```
evox/
├── evox-core/              # 核心层：最底层，提供基础抽象
├── evox-capability/        # 能力层：提供通用能力
├── evox-business/          # 业务层：实现核心业务逻辑
├── evox-advanced/          # 高级业务层：提供高级业务能力
├── evox-framework/         # 框架层：多智能体协同框架
└── evox-application/       # 应用层：示例和测试
```

## 🔗 依赖关系原则

### 1. 依赖方向规则

- ✅ **向下依赖**: 上层可以依赖下层
- ❌ **禁止向上依赖**: 下层不能依赖上层
- ❌ **禁止跨层依赖**: 不能跨越中间层直接依赖
- ✅ **同层依赖**: 同层模块间可以相互依赖，但需避免循环依赖

### 2. 各层依赖规则

| 层级 | 可依赖的层级 |
|------|-------------|
| 应用层 | 所有下层 |
| 框架层 | 核心层、能力层、业务层 |
| 高级业务层 | 核心层、能力层、业务层 |
| 业务层 | 核心层、能力层 |
| 能力层 | 核心层（部分模块间可互相依赖） |
| 核心层 | 基础设施层 |

## 📦 各层详细说明

### 核心层 (Core Layer)

**位置**: `evox-core/`  
**职责**: 提供最底层的抽象和基础设施

| 模块 | 说明 | 依赖 |
|------|------|------|
| evox-core | 核心抽象（BaseModule、Message、Registry） | 无 |
| evox-models | LLM 模型适配 | evox-core |
| evox-actions | 动作执行引擎 | evox-core, evox-models |

**设计原则**:
- 最小依赖原则
- 稳定接口原则
- 高内聚低耦合

### 能力层 (Capability Layer)

**位置**: `evox-capability/`  
**职责**: 为业务层提供各种通用能力

| 模块 | 说明 | 依赖 |
|------|------|------|
| evox-storage | 存储适配（内存、数据库、向量、图） | evox-core |
| evox-memory | 记忆管理（短期、长期） | evox-core, evox-storage |
| evox-tools | 工具集（文件、HTTP、数据库、搜索） | evox-core |
| evox-utils | 工具类库 | 无 |

**设计原则**:
- 可插拔设计
- 易扩展性
- 高性能
- 通用性

### 业务层 (Business Layer)

**位置**: `evox-business/`  
**职责**: 实现核心业务逻辑

| 模块 | 说明 | 依赖 |
|------|------|------|
| evox-agents | 智能代理系统 | evox-core, evox-models, evox-actions, evox-tools |
| evox-workflow | 工作流编排引擎 | evox-core, evox-models, evox-memory, evox-storage |
| evox-rag | 检索增强生成 | evox-core, evox-models, evox-storage |
| evox-prompts | 提示词管理 | evox-core |

**设计原则**:
- 业务聚焦
- 灵活编排
- 状态管理
- 错误处理

### 高级业务层 (Advanced Layer)

**位置**: `evox-advanced/`  
**职责**: 提供高级业务能力（优化、评估、人机协同）

| 模块 | 说明 | 依赖 |
|------|------|------|
| evox-optimizers | 性能优化器（TextGrad、MIPRO、AFlow） | evox-core, evox-models, evox-agents, evox-workflow |
| evox-hitl | 人机协同 | evox-core, evox-agents, evox-workflow |
| evox-evaluators | 效果评估器 | evox-core |

**设计原则**:
- 智能优化
- 人机结合
- 效果量化
- 持续改进

### 框架层 (Framework Layer)

**位置**: `evox-framework/`  
**职责**: 提供多智能体协同框架

| 模块 | 说明 | 依赖 |
|------|------|------|
| evox-frameworks | 多智能体框架（辩论系统等） | evox-core, evox-agents |

**设计原则**:
- 协同优先
- 模式封装
- 易于使用
- 可扩展

### 应用层 (Application Layer)

**位置**: `evox-application/`  
**职责**: 提供示例应用和基准测试

| 模块 | 说明 | 依赖 |
|------|------|------|
| evox-examples | 示例应用 | 多个下层模块 |
| evox-benchmark | 性能基准测试 | evox-core |

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

### 3. 易于扩展

每一层都有明确的扩展点：
- 核心层：新增模型适配器
- 能力层：新增存储类型、工具
- 业务层：新增业务模块
- 框架层：新增协同模式

### 4. 维护性强

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

## 🔄 从扁平结构迁移指南

### 迁移步骤

1. ✅ **创建层级目录**
   ```bash
   mkdir -p evox-core evox-capability evox-business evox-advanced evox-framework evox-application
   ```

2. ✅ **移动模块到对应层级**
   ```bash
   # 核心层
   mv evox-core evox-core-temp && mkdir evox-core && mv evox-core-temp evox-core/evox-core
   mv evox-models evox-core/
   mv evox-actions evox-core/
   
   # 能力层
   mv evox-storage evox-capability/
   mv evox-memory evox-capability/
   mv evox-tools evox-capability/
   mv evox-utils evox-capability/
   
   # ... (其他层级类似)
   ```

3. ✅ **更新父 POM**
   ```xml
   <modules>
       <!-- 核心层 -->
       <module>evox-core/evox-core</module>
       <module>evox-core/evox-models</module>
       <!-- ... -->
   </modules>
   ```

4. ✅ **验证编译**
   ```bash
   mvn clean compile
   ```

5. ✅ **更新文档**
   - 更新 README.md
   - 为各层级添加 README

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

**文档版本**: 1.0.0  
**最后更新**: 2025-11-29  
**维护者**: EvoX Team
