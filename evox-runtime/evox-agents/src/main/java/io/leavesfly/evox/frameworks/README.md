# EvoX Frameworks 多智能体协同框架

## 📦 模块定位

**层级**: 业务层 (Business Layer)  
**职责**: 提供多智能体协同框架,支持复杂的协作模式  
**依赖**: evox-core, evox-agents

## 🎯 核心功能

evox-frameworks 提供5种核心多智能体协同框架,支持辩论、共识、分层、拍卖、团队等不同协作场景。

## 📦 模块定位

**层级**: 业务层 (Business Layer)  
**职责**: 提供多智能体协同框架,支持复杂的协作模式  
**依赖**: evox-core, evox-agents

## 🎯 核心框架

| 框架 | 包路径 | 核心类 | 状态 |
|------|--------|--------|------|
| **辩论框架** | `debate` | `MultiAgentDebate` | ✅ 完成 |
| **共识框架** | `consensus` | `ConsensusFramework` | ✅ 完成 |
| **分层决策框架** | `hierarchical` | `HierarchicalFramework` | ✅ 完成 |
| **拍卖框架** | `auction` | `AuctionFramework` | ✅ 完成 |
| **团队协作框架** | `team` | `TeamFramework` | ✅ 完成 |

## 🚀 快速开始

### Maven依赖

```xml
<dependency>
    <groupId>io.leavesfly.evox</groupId>
    <artifactId>evox-frameworks</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 使用示例

```java
// 1. 共识框架
ConsensusFramework<String> consensus = new ConsensusFramework<>(
    agents, 
    new MajorityVotingStrategy<>()
);
ConsensusResult<String> result = consensus.reachConsensus("问题");

// 2. 拍卖框架
AuctionFramework<String> auction = new AuctionFramework<>(
    item,
    AuctionMechanism.ENGLISH,
    bidders
);
AuctionResult<String> auctionResult = auction.startAuction();

// 3. 团队协作框架
TeamFramework<String> team = new TeamFramework<>(
    members,
    CollaborationMode.COLLABORATIVE
);
TeamResult<String> teamResult = team.executeTeamTask("任务");
```

## 📚 详细文档

完整的框架使用文档请参见各子包的 README.md 文件。

## 🔗 相关模块

- **evox-core**: 核心抽象
- **evox-agents**: Agent基础实现
- **evox-workflow**: 工作流编排
