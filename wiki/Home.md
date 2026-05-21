# EvoX Wiki

**EvoX** — 自进化的企业级多智能体框架

基于 Java 17、Spring Boot 3.2+ 和 Project Reactor 构建的现代化智能代理框架。

---

## 📚 文档导航

### 入门
- [用户快速开始指南](User-Guide-QuickStart.md) — 从零开始，5 分钟完成第一个 AI 应用
- [架构总览](Architecture-Overview.md) — 系统分层、模块关系与设计理念
- [快速开始](Getting-Started.md) — 环境准备、安装配置与第一个示例

### 核心模块
- [核心抽象层 (evox-core)](Core-Module.md) — 基础接口、消息系统、动作抽象
- [LLM 模型适配 (evox-models)](LLM-Providers.md) — 多模型统一接入与使用
- [智能体系统 (evox-agents)](Agent-System.md) — Agent 类型、生命周期与扩展

### 运行时能力
- [工作流引擎 (evox-workflow)](Workflow-Engine.md) — DAG 编排、条件分支与并行执行
- [RAG 检索增强 (evox-rag)](RAG-Module.md) — 文档处理、向量检索与生成增强
- [工具系统 (evox-tools)](Tool-System.md) — 内置工具、自定义工具与 MCP 集成

### 高级特性
- [多智能体协作](Multi-Agent-Collaboration.md) — 辩论、共识、团队协作框架
- [优化器系统 (evox-optimizers)](Optimizers.md) — TextGrad、MIPRO、AFlow 等优化算法

### 集成与运维
- [Spring Boot 集成](Spring-Boot-Integration.md) — 自动配置与 Starter 使用
- [配置参考](Configuration-Reference.md) — 完整配置项说明

---

## 🔗 快速链接

| 资源 | 说明 |
|------|------|
| Java 17+ | 运行环境要求 |
| Maven 3.8+ | 构建工具 |
| Spring Boot 3.2.5 | 基础框架 |
| Project Reactor | 响应式编程基础 |

## 📦 模块全景

```
evox/
├── evox-core/          # 核心层
│   ├── evox-core       #   基础抽象与 SPI
│   ├── evox-models     #   LLM 模型适配
│   └── evox-mcp        #   MCP 协议定义
├── evox-runtime/       # 运行时层
│   ├── evox-agents     #   智能体运行时
│   ├── evox-workflow   #   工作流引擎
│   ├── evox-tools      #   工具集成
│   ├── evox-memory     #   记忆管理
│   ├── evox-rag        #   RAG 检索增强
│   ├── evox-storage    #   存储抽象
│   └── evox-mcp-runtime#   MCP 运行时
├── evox-extensions/    # 扩展层
│   ├── evox-optimizers #   优化器
│   ├── evox-hitl       #   人机协同
│   └── evox-evaluation #   评估框架
└── evox-application/   # 应用层
    ├── evox-examples   #   示例代码
    ├── evox-benchmark  #   基准测试
    ├── evox-claudecode #   Claude Code 集成
    ├── evox-cowork     #   协同工作
    └── evox-spring-boot-starter  # Spring Boot Starter
```
