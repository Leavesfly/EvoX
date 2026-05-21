# 多智能体协作

## 概述

EvoX 提供五种多智能体协作模式，用于解决复杂任务：

1. **辩论 (Debate)** - 多智能体轮次辩论，通过观点碰撞达成共识
2. **共识 (Consensus)** - 基于投票和策略的决策机制
3. **团队 (Team)** - 分工协作完成复杂任务
4. **分层决策 (Hierarchical)** - 层级化决策与执行
5. **拍卖 (Auction)** - 通过竞价分配任务

---

## 辩论框架 (DebateFramework / MultiAgentDebate)

多智能体通过多轮辩论达成共识或最优解。

### 核心组件
- `DebateFramework` - 辩论框架主类
- `MultiAgentDebate` - 多智能体辩论实现
- `DebateAgent` - 参与辩论的智能体

### 代码示例

```java
// 创建参与辩论的智能体列表
List<DebateAgent> agents = Arrays.asList(
    DebateAgent.builder()
        .name("Agent-A")
        .role("Supporter")
        .llm(llm)
        .build(),
    DebateAgent.builder()
        .name("Agent-B")
        .role("Critic")
        .llm(llm)
        .build(),
    DebateAgent.builder()
        .name("Agent-C")
        .role("Moderator")
        .llm(llm)
        .build()
);

// 配置辩论参数
DebateConfig config = DebateConfig.builder()
    .maxRounds(3)
    .topic("Should we use microservices architecture?")
    .build();

// 创建辩论框架
DebateFramework debate = new DebateFramework(agents, config);

// 执行辩论
DebateResult result = debate.debate();

// 获取结果
System.out.println("Final Conclusion: " + result.getConclusion());
System.out.println("Round Count: " + result.getRoundCount());
```

---

## 共识框架 (ConsensusFramework)

基于多种策略的智能体共识达成机制。

### 内置共识策略

| 策略名称 | 说明 | 适用场景 |
|---------|------|---------|
| `MajorityVotingStrategy` | 多数投票 | 简单决策，快速达成共识 |
| `WeightedVotingStrategy` | 加权投票 | 考虑智能体专业度权重 |
| `BayesianConsensusStrategy` | 贝叶斯共识 | 概率推理场景 |
| `ConsistencyCheckStrategy` | 一致性检查 | 需要逻辑一致性的场景 |

### ConsensusConfig 配置

```java
ConsensusConfig config = ConsensusConfig.builder()
    .maxRounds(5)                    // 最大轮次
    .consensusThreshold(0.8)         // 共识阈值（80%同意）
    .minSupportRate(0.6)             // 最低支持率
    .enableEarlyStopping(true)       // 启用提前停止
    .strategy(ConsensusStrategy.MAJORITY_VOTING)  // 选择策略
    .build();
```

### 完整代码示例

```java
// 1. 创建参与共识的智能体
List<ConsensusAgent> agents = Arrays.asList(
    ConsensusAgent.builder()
        .name("Expert-A")
        .weight(0.4)
        .llm(llm)
        .build(),
    ConsensusAgent.builder()
        .name("Expert-B")
        .weight(0.3)
        .llm(llm)
        .build(),
    ConsensusAgent.builder()
        .name("Expert-C")
        .weight(0.3)
        .llm(llm)
        .build()
);

// 2. 配置共识策略
ConsensusConfig config = ConsensusConfig.builder()
    .maxRounds(3)
    .consensusThreshold(0.7)
    .minSupportRate(0.5)
    .enableEarlyStopping(true)
    .strategy(new WeightedVotingStrategy())
    .build();

// 3. 创建共识框架
ConsensusFramework framework = new ConsensusFramework(agents, config);

// 4. 达成共识
ConsensusResult result = framework.reachConsensus("What is the best approach?");

// 5. 处理结果
if (result.isConsensusReached()) {
    System.out.println("Consensus: " + result.getDecision());
    System.out.println("Support Rate: " + result.getSupportRate());
    System.out.println("Rounds Used: " + result.getRoundsUsed());
} else {
    System.out.println("No consensus reached after " + result.getRoundsUsed() + " rounds");
}
```

---

## 团队协作框架 (TeamFramework)

多个智能体分工协作完成复杂任务。

### 特点
- 角色分配：Leader、Executor、Reviewer 等
- 任务分解：自动拆分复杂任务
- 结果整合：汇总各成员输出

### 使用场景
- 复杂项目开发
- 多步骤分析任务
- 需要多领域知识的任务

---

## 分层决策框架 (HierarchicalFramework)

层级化的决策与执行架构。

### 层级结构
```
┌─────────────────┐
│   Decision Layer │  ← 高层决策者
├─────────────────┤
│  Coordination    │  ← 协调层
├─────────────────┤
│  Execution Layer │  ← 执行层
└─────────────────┘
```

### 特点
- 清晰的职责划分
- 自上而下的指令传递
- 自下而上的结果反馈

---

## 拍卖框架 (AuctionFramework)

通过竞价机制分配任务给最合适的智能体。

### 工作流程
1. **任务发布** - 发布待分配任务
2. **智能体竞价** - 各智能体根据自身能力出价
3. **中标分配** - 选择最优出价者执行任务
4. **结果反馈** - 执行完成后反馈结果

### 适用场景
- 资源受限环境
- 需要优化成本的任务分配
- 动态负载均衡

---

## 自定义共识策略扩展指南

实现自定义共识策略需要实现 `ConsensusStrategy` 接口。

### 接口定义

```java
public interface ConsensusStrategy {
    
    /**
     * 评估是否达成共识
     * @param votes 投票结果
     * @param config 配置参数
     * @return 是否达成共识
     */
    boolean isConsensusReached(List<Vote> votes, ConsensusConfig config);
    
    /**
     * 计算最终决策
     * @param votes 投票结果
     * @return 最终决策
     */
    Decision computeDecision(List<Vote> votes);
    
    /**
     * 策略名称
     */
    String getName();
}
```

### 自定义策略示例

```java
public class CustomConsensusStrategy implements ConsensusStrategy {
    
    @Override
    public boolean isConsensusReached(List<Vote> votes, ConsensusConfig config) {
        // 自定义共识判断逻辑
        long agreeCount = votes.stream()
            .filter(v -> v.isAgree())
            .count();
        double rate = (double) agreeCount / votes.size();
        return rate >= config.getConsensusThreshold();
    }
    
    @Override
    public Decision computeDecision(List<Vote> votes) {
        // 自定义决策计算逻辑
        return votes.stream()
            .filter(Vote::isAgree)
            .findFirst()
            .map(Vote::getDecision)
            .orElse(null);
    }
    
    @Override
    public String getName() {
        return "CustomStrategy";
    }
}
```

### 注册使用

```java
ConsensusConfig config = ConsensusConfig.builder()
    .strategy(new CustomConsensusStrategy())
    .build();

ConsensusFramework framework = new ConsensusFramework(agents, config);
```

---

## 包结构

```
com.alibaba.aone.evox.collaboration
├── frameworks/
│   ├── debate/
│   │   ├── DebateFramework.java
│   │   ├── MultiAgentDebate.java
│   │   ├── DebateAgent.java
│   │   ├── DebateConfig.java
│   │   └── DebateResult.java
│   ├── consensus/
│   │   ├── ConsensusFramework.java
│   │   ├── ConsensusAgent.java
│   │   ├── ConsensusConfig.java
│   │   ├── ConsensusResult.java
│   │   └── strategy/
│   │       ├── ConsensusStrategy.java      # 策略接口
│   │       ├── MajorityVotingStrategy.java
│   │       ├── WeightedVotingStrategy.java
│   │       ├── BayesianConsensusStrategy.java
│   │       └── ConsistencyCheckStrategy.java
│   ├── team/
│   │   ├── TeamFramework.java
│   │   ├── TeamMember.java
│   │   └── TeamConfig.java
│   ├── hierarchical/
│   │   ├── HierarchicalFramework.java
│   │   ├── DecisionLayer.java
│   │   └── ExecutionLayer.java
│   └── auction/
│       ├── AuctionFramework.java
│       ├── Bid.java
│       └── AuctionResult.java
└── common/
    ├── Vote.java
    ├── Decision.java
    └── CollaborationResult.java
```

---

## 最佳实践

1. **选择合适的协作模式**
   - 简单决策 → 共识框架（多数投票）
   - 复杂分析 → 辩论框架
   - 大型项目 → 团队框架
   - 资源优化 → 拍卖框架

2. **配置合理的参数**
   - 根据任务复杂度设置 `maxRounds`
   - 根据容错要求设置 `consensusThreshold`
   - 启用 `earlyStopping` 提升效率

3. **智能体角色设计**
   - 确保角色互补
   - 明确职责边界
   - 避免功能重叠
