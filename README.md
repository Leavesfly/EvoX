# EvoX - EvoAgentX Java Spring 生态重构

## 项目概述

EvoX 是 EvoAgentX 的 Java 17 + Spring 生态系统重构版本,保持原有核心功能逻辑,采用现代化的技术栈和架构设计。

## 技术栈

- **Java**: 17+
- **构建工具**: Maven 3.8+
- **应用框架**: Spring Boot 3.2+
- **AI 集成**: Spring AI 1.0+, Spring AI Alibaba 1.0+
- **数据库**: H2 (内存数据库,默认)
- **异步编程**: Project Reactor 3.6+
- **日志**: SLF4J + Logback

## 模块结构

```
evox/
├── pom.xml                    # 父 POM
├── evox-core/                 # 核心基础模块
├── evox-models/               # 模型适配层
├── evox-actions/              # 动作系统
├── evox-agents/               # 智能体系统
├── evox-workflow/             # 工作流引擎
├── evox-memory/               # 记忆系统
├── evox-storage/              # 存储适配器
└── evox-tools/                # 工具集
```

## 快速开始

### 环境要求

- JDK 17 或更高版本
- Maven 3.8 或更高版本

### 构建项目

```bash
cd evox
mvn clean install
```

### 运行测试

```bash
mvn test
```

## 开发状态

当前处于第一阶段实现中,已完成:

- ✅ 项目结构和父 POM 配置
- ✅ evox-core 核心模块基础类
- 🚧 其他模块开发中...

## License

MIT License
