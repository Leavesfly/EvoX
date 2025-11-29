# EvoX Frameworks 多智能体框架

本模块提供多种多智能体协同框架,支持不同的协作模式和共识机制。

## 📦 框架总览

| 框架 | 位置 | 核心类 | 状态 | 描述 |
|------|------|--------|------|------|
| **辩论框架** | `frameworks.debate` | `MultiAgentDebate` | ✅ 完成 | 多智能体轮次辩论达成共识 |
| **共识框架** | `frameworks.consensus` | `ConsensusFramework` | ✅ 完成 | 通用共识框架,支持4种策略 |
| **分层决策框架** | `frameworks.hierarchical` | `HierarchicalFramework` | 🆕 新增 | 管理者-执行者分层决策 |
| **拍卖框架** | `frameworks.auction` | `AuctionFramework` | 🆕 新增 | 支持6种拍卖机制 |
| **团队协作框架** | `frameworks.team` | `TeamFramework` | 🆕 新增 | 5种协作模式的团队管理 |

---

## 📦 框架详细说明

### 1. 辩论框架 (Debate Framework)

**位置**: `io.leavesfly.evox.agents.frameworks.debate`

**核心类**: `MultiAgentDebate`

**功能描述**:
- 允许多个智能体通过轮次辩论达成共识
- 每轮辩论中,所有智能体依次发表观点
- 自动检测是否达成共识
- 记录完整的辩论历史

**使用场景**:
- 多视角问题分析
- 决策优化
- 知识融合
- 观点冲突解决

**示例代码**:
```java
// 创建辩论智能体
List<DebateAgent> agents = Arrays.asList(
    new MyDebateAgent("Agent1"),
    new MyDebateAgent("Agent2"),
    new MyDebateAgent("Agent3")
);

// 初始化辩论框架
MultiAgentDebate debate = new MultiAgentDebate(agents, 5); // 最多5轮

// 开始辩论
String result = debate.debate("What is the best approach to solve this problem?");
```

---

### 2. 共识框架 (Consensus Framework)

**位置**: `io.leavesfly.evox.agents.frameworks.consensus`

**核心类**: `ConsensusFramework`

**功能描述**:
- 通用的共识达成框架
- 支持多种共识策略
- 可配置的早停机制
- 智能体反馈机制
- 完整的历史记录和元数据

**核心组件**:

#### 2.1 共识框架 (ConsensusFramework)
主框架类,协调整个共识过程:
- 管理多轮迭代
- 收集智能体提议
- 调用策略评估
- 早停控制
- 结果汇总

#### 2.2 共识策略 (ConsensusStrategy)
策略接口,定义共识判断逻辑。内置四种策略:

##### 多数投票策略 (MajorityVotingStrategy)
- **原理**: 简单多数投票,得票最多者胜出
- **适用**: 提议离散、选项有限的场景
- **配置**: 可设置最小支持率和置信度阈值
- **示例**:
```java
ConsensusConfig config = ConsensusConfig.builder()
    .consensusThreshold(0.8)
    .minSupportRate(0.5)
    .build();

ConsensusStrategy<String> strategy = new MajorityVotingStrategy<>(config);
```

##### 加权投票策略 (WeightedVotingStrategy)
- **原理**: 根据智能体权重加权投票
- **适用**: 智能体能力/信誉不同的场景
- **权重**: 通过 `ConsensusAgent.getWeight()` 设置
- **示例**:
```java
// 高权重专家智能体
ConsensusAgent<String> expert = new ConsensusAgent<>() {
    @Override
    public double getWeight() {
        return 2.0; // 双倍权重
    }
    // ... 其他方法
};
```

##### 贝叶斯共识策略 (BayesianConsensusStrategy)
- **原理**: 基于贝叶斯推理,考虑智能体历史准确率和先验概率
- **适用**: 需要综合历史表现的复杂决策场景
- **配置**: 可设置智能体准确率和先验概率
- **示例**:
```java
BayesianConsensusStrategy<String> strategy = new BayesianConsensusStrategy<>(config);

// 设置智能体历史准确率
strategy.setAgentAccuracy("Agent1", 0.9); // 90%准确率
strategy.setAgentAccuracy("Agent2", 0.7); // 70%准确率

// 设置先验概率
strategy.setPriorProbability("OptionA", 0.6);
strategy.setPriorProbability("OptionB", 0.4);
```

##### 一致性检查策略 (ConsistencyCheckStrategy)
- **原理**: 基于提议相似度聚类,找出最大一致性簇
- **适用**: 连续值、文本等需要相似度判断的场景
- **配置**: 需提供相似度计算函数
- **示例**:
```java
// 自定义相似度函数(例如文本相似度)
BiFunction<String, String, Double> similarity = (a, b) -> {
    // 简单示例:字符串编辑距离
    double distance = calculateEditDistance(a, b);
    return 1.0 - (distance / Math.max(a.length(), b.length()));
};

ConsensusStrategy<String> strategy = new ConsistencyCheckStrategy<>(
    config, 
    similarity, 
    0.85 // 85%相似度阈值
);
```

#### 2.3 配置选项 (ConsensusConfig)
```java
ConsensusConfig config = ConsensusConfig.builder()
    .maxRounds(10)                      // 最大轮数
    .consensusThreshold(0.8)            // 共识阈值
    .minSupportRate(0.5)                // 最小支持率
    .enableEarlyStopping(true)          // 启用早停
    .earlyStoppingPatience(3)           // 早停耐心值
    .earlyStoppingThreshold(0.01)       // 早停改进阈值
    .enableAgentFeedback(true)          // 启用智能体反馈
    .ignoreFailedProposals(true)        // 忽略失败的提议
    .build();
```

**完整使用示例**:
```java
// 1. 创建共识智能体
List<ConsensusAgent<String>> agents = Arrays.asList(
    new MyConsensusAgent("Expert1", 2.0),  // 权重2.0
    new MyConsensusAgent("Expert2", 1.5),  // 权重1.5
    new MyConsensusAgent("Expert3", 1.0)   // 权重1.0
);

// 2. 选择策略
ConsensusConfig config = ConsensusConfig.builder()
    .maxRounds(10)
    .consensusThreshold(0.8)
    .build();

ConsensusStrategy<String> strategy = new WeightedVotingStrategy<>(config);

// 3. 创建框架
ConsensusFramework<String> framework = new ConsensusFramework<>(agents, strategy, config);

// 4. 执行共识
ConsensusResult<String> result = framework.reachConsensus("What is the optimal solution?");

// 5. 处理结果
if (result.isReached()) {
    System.out.println("Consensus reached: " + result.getResult());
    System.out.println("Confidence: " + result.getConfidence());
    System.out.println("Rounds: " + result.getRounds());
} else {
    System.out.println("No consensus, best effort: " + result.getResult());
}
```

**高级用法 - 实现自定义共识智能体**:
```java
public class MyConsensusAgent implements ConsensusFramework.ConsensusAgent<String> {
    
    private final String name;
    private final double weight;
    private String lastProposal;
    
    @Override
    public String propose(String question, List<ConsensusRecord<String>> history) {
        // 分析历史,生成提议
        if (history.isEmpty()) {
            // 首轮:基于问题生成初始提议
            return generateInitialProposal(question);
        } else {
            // 后续轮:考虑历史,调整提议
            ConsensusRecord<String> lastRound = history.get(history.size() - 1);
            return adjustProposal(question, lastRound);
        }
    }
    
    @Override
    public void onEvaluation(int round, ConsensusEvaluation<String> evaluation) {
        // 接收评估反馈,可用于下一轮调整策略
        if (evaluation.isConsensusReached()) {
            System.out.println(name + " agrees with consensus");
        } else {
            System.out.println(name + " will adjust in next round");
        }
    }
    
    @Override
    public double getWeight() {
        return weight;
    }
    
    @Override
    public String getName() {
        return name;
    }
}
```

**策略选择指南**:

| 策略 | 适用场景 | 优势 | 限制 |
|------|---------|------|------|
| **多数投票** | 离散选项、民主决策 | 简单直观、公平 | 不考虑智能体差异 |
| **加权投票** | 专家系统、信誉机制 | 重视专家意见 | 需要合理设置权重 |
| **贝叶斯** | 复杂推理、历史数据丰富 | 科学严谨、可解释 | 需要先验知识 |
| **一致性检查** | 连续值、文本生成 | 灵活、支持相似度 | 需要自定义相似度函数 |

## 🔧 扩展指南

### 实现自定义共识策略

```java
public class MyCustomStrategy<T> implements ConsensusStrategy<T> {
    
    @Override
    public ConsensusEvaluation<T> evaluate(List<T> proposals, 
                                           List<ConsensusFramework.ConsensusAgent<T>> agents) {
        // 实现自定义评估逻辑
        T consensusValue = customEvaluationLogic(proposals, agents);
        double confidence = calculateConfidence(proposals);
        double supportRate = calculateSupportRate(proposals, consensusValue);
        
        return ConsensusEvaluation.<T>builder()
            .consensusReached(confidence >= threshold)
            .consensusValue(consensusValue)
            .confidence(confidence)
            .supportRate(supportRate)
            .build();
    }
    
    @Override
    public ConsensusEvaluation<T> fallback(List<ConsensusRecord<T>> history, 
                                           List<ConsensusFramework.ConsensusAgent<T>> agents) {
        // 实现回退策略
        return evaluate(/* ... */);
    }
    
    @Override
    public String getStrategyName() {
        return "MyCustomStrategy";
    }
}
```

## 📊 性能建议

### 通用建议
1. **合理设置最大轮数**: 避免无限循环,建议5-10轮
2. **启用早停**: 防止无效迭代,节省资源
3. **异步执行**: 智能体提议可并行处理
4. **缓存历史**: 避免重复计算

### 分层决策框架
- 控制层级深度,避免过深递归
- 合理设置聚合策略,减少计算开销

### 拍卖框架
- 英式拍卖:设置合理的价格增量避免过多轮次
- 密封拍卖:一次性决策,性能最优

### 团队协作框架
- 并行模式:启用线程池提升并发性能
- 顺序模式:适合I/O密集型任务

## 🔍 调试技巧

启用详细日志:
```xml
<!-- logback配置 -->
<logger name="io.leavesfly.evox.agents.frameworks" level="DEBUG"/>
<logger name="io.leavesfly.evox.agents.frameworks.hierarchical" level="TRACE"/>
<logger name="io.leavesfly.evox.agents.frameworks.auction" level="TRACE"/>
<logger name="io.leavesfly.evox.agents.frameworks.team" level="TRACE"/>
```

## 📚 框架对比

| 框架 | 复杂度 | 适用规模 | 主要优势 | 限制 |
|------|--------|----------|----------|------|
| **辩论** | 低 | 3-10个智能体 | 观点融合 | 耗时较长 |
| **共识** | 中 | 3-20个智能体 | 灵活策略 | 需要调参 |
| **分层** | 高 | 任意层级 | 结构清晰 | 实现复杂 |
| **拍卖** | 中 | 2-100个竞价者 | 公平合理 | 特定场景 |
| **团队** | 中 | 2-50个成员 | 模式多样 | 资源开销 |

## 🔍 调试技巧

启用详细日志:
```xml
<!-- logback配置 -->
<logger name="io.leavesfly.evox.agents.frameworks" level="DEBUG"/>
```

---

### 3. 分层决策框架 (Hierarchical Decision Framework) 🆕

**位置**: `io.leavesfly.evox.agents.frameworks.hierarchical`

**核心类**: `HierarchicalFramework`

**功能描述**:
- 管理者-执行者分层架构
- 支持多层级决策委派
- 任务分解与聚合
- 自动层级递归执行

**使用场景**:
- 企业决策流程
- 复杂任务分解
- 组织结构模拟
- 分层审批系统

**示例代码**:
```java
// 1. 定义决策层级
DecisionLayer<String> ceoLayer = new DecisionLayer<>() {
    @Override
    public LayerDecision<String> decide(String task, LayerDecision<String> parent) {
        LayerDecision<String> decision = new LayerDecision<>("CEO", task);
        // CEO分解任务给管理层
        decision.addSubTask("市场调研");
        decision.addSubTask("产品设计");
        decision.addSubTask("技术开发");
        return decision;
    }
    // ... 其他方法
};

DecisionLayer<String> managerLayer = new DecisionLayer<>() {
    @Override
    public LayerDecision<String> decide(String task, LayerDecision<String> parent) {
        LayerDecision<String> decision = new LayerDecision<>("Manager", task);
        // 管理层进一步细分或直接执行
        decision.setResult("完成任务: " + task);
        return decision;
    }
};

// 2. 创建框架
List<DecisionLayer<String>> layers = Arrays.asList(ceoLayer, managerLayer);
HierarchicalFramework<String> framework = new HierarchicalFramework<>(layers);

// 3. 执行分层决策
HierarchicalResult<String> result = framework.executeHierarchical("开发新产品");

if (result.isSuccess()) {
    System.out.println("决策结果: " + result.getResult());
    System.out.println("层级数: " + result.getLayers());
}
```

**配置选项**:
```java
HierarchicalConfig config = HierarchicalConfig.builder()
    .maxDepth(10)                       // 最大层级深度
    .allowCrossLevelDelegation(false)   // 是否允许跨层委派
    .enableParallelExecution(false)     // 是否并行执行
    .executionTimeout(300000)           // 执行超时
    .aggregationStrategy(customStrategy)// 自定义聚合策略
    .build();
```

---

### 4. 拍卖框架 (Auction Framework) 🆕

**位置**: `io.leavesfly.evox.agents.frameworks.auction`

**核心类**: `AuctionFramework`

**功能描述**:
- 支持6种拍卖机制
- 完整的竞价历史
- 多种出价策略
- 公平性验证

**支持的拍卖机制**:

| 机制 | 类型 | 说明 | 适用场景 |
|------|------|------|----------|
| **英式拍卖** | ENGLISH | 递增价格,公开竞价 | 艺术品、房产 |
| **荷兰式拍卖** | DUTCH | 递减价格,首个接受者获胜 | 鲜花、水产 |
| **第一价格密封** | FIRST_PRICE_SEALED | 密封出价,支付自己出价 | 政府采购 |
| **第二价格密封** | SECOND_PRICE_SEALED | 密封出价,支付第二高价 | 广告竞价 |
| **Vickrey拍卖** | VICKREY | 第二价格别名 | 网络广告 |
| **全付拍卖** | ALL_PAY | 所有人支付,只有最高者获胜 | 研发竞赛 |

**示例代码**:
```java
// 1. 创建竞价者
Bidder<String> bidder1 = new Bidder<>() {
    @Override
    public double bid(String item, double currentPrice, List<BidRecord<String>> history) {
        // 竞价策略: 每次加价100
        return currentPrice + 100;
    }
    
    @Override
    public double sealedBid(String item) {
        // 密封出价: 根据估值
        return getValuation(item) * 0.9; // 出价90%估值
    }
    // ... 其他方法
};

List<Bidder<String>> bidders = Arrays.asList(bidder1, bidder2, bidder3);

// 2. 配置拍卖
AuctionConfig config = AuctionConfig.builder()
    .startingPrice(1000)      // 起拍价
    .reservePrice(5000)       // 保留价
    .priceIncrement(100)      // 价格增量
    .maxRounds(50)            // 最大轮数
    .build();

// 3. 创建拍卖
AuctionFramework<String> auction = new AuctionFramework<>(
    "珍稀藏品",
    AuctionMechanism.ENGLISH,  // 使用英式拍卖
    bidders,
    config
);

// 4. 开始拍卖
AuctionResult<String> result = auction.startAuction();

if (result.isSuccess()) {
    System.out.println("获胜者: " + result.getWinner().getBidderName());
    System.out.println("成交价: " + result.getFinalPrice());
    System.out.println("总轮数: " + result.getTotalRounds());
}
```

**拍卖机制对比**:

| 特性 | 英式 | 荷兰式 | 第一价格 | 第二价格 | Vickrey | 全付 |
|------|------|--------|---------|---------|---------|------|
| **公开性** | 公开 | 公开 | 密封 | 密封 | 密封 | 密封 |
| **价格趋势** | 上升 | 下降 | 固定 | 固定 | 固定 | 固定 |
| **支付价格** | 最高价 | 当前价 | 最高价 | 第二高价 | 第二高价 | 自己出价 |
| **策略复杂度** | 低 | 中 | 高 | 中 | 中 | 高 |
| **效率** | 高 | 高 | 中 | 高 | 高 | 低 |

---

### 5. 团队协作框架 (Team Collaboration Framework) 🆕

**位置**: `io.leavesfly.evox.agents.frameworks.team`

**核心类**: `TeamFramework`

**功能描述**:
- 5种协作模式
- 角色管理
- 负载均衡
- 并行/串行执行

**协作模式**:

| 模式 | 类型 | 说明 | 适用场景 |
|------|------|------|----------|
| **并行模式** | PARALLEL | 所有成员同时工作 | 并行任务、快速响应 |
| **顺序模式** | SEQUENTIAL | 成员依次工作,可传递结果 | 流水线、步骤依赖 |
| **分层模式** | HIERARCHICAL | 按角色层级工作 | 组织结构、审批流程 |
| **协同模式** | COLLABORATIVE | 成员相互协商讨论 | 团队头脑风暴、创意合作 |
| **竞争模式** | COMPETITIVE | 选择最佳方案 | 方案竞选、质量保障 |

**示例代码**:
```java
// 1. 创建团队成员
TeamMember<String> leader = new TeamMember<>() {
    @Override
    public String execute(String task, String previousResult, 
                         List<TaskExecution<String>> history) {
        return "领导决策: " + task;
    }
    
    @Override
    public TeamRole getRole() {
        return TeamRole.LEADER;
    }
    // ... 其他方法
};

List<TeamMember<String>> members = Arrays.asList(
    leader, 
    expertMember, 
    executorMember
);

// 2. 配置团队
TeamConfig config = TeamConfig.builder()
    .enableThreadPool(true)       // 启用线程池
    .maxThreads(5)                // 最大线程数
    .taskTimeout(60000)           // 任务超时
    .enableLoadBalancing(true)    // 负载均衡
    .build();

// 3. 创建团队框架
TeamFramework<String> team = new TeamFramework<>(
    members,
    CollaborationMode.COLLABORATIVE,  // 使用协同模式
    config
);

// 4. 执行团队任务
TeamResult<String> result = team.executeTeamTask("开发新功能");

if (result.isSuccess()) {
    System.out.println("团队结果: " + result.getResult());
    System.out.println("参与人数: " + result.getParticipantCount());
    System.out.println("耗时: " + result.getDuration() + "ms");
    
    // 查看每个成员的贡献
    result.getContributions().forEach(contribution -> 
        System.out.println(contribution.getMemberId() + ": " + contribution.getResult())
    );
}
```

**团队角色**:
- **LEADER** (领导者): 优先级 1
- **MANAGER** (管理者): 优先级 2
- **EXPERT** (专家): 优先级 3
- **COORDINATOR** (协调者): 优先级 3
- **REVIEWER** (审核者): 优先级 3
- **EXECUTOR** (执行者): 优先级 4
- **MEMBER** (普通成员): 优先级 5

---

## 📊 性能建议

## 📚 参考资料

- [Multi-Agent Debate论文](https://arxiv.org/abs/xxxx.xxxxx)
- [Bayesian Consensus理论](https://example.com)
- [Hierarchical Multi-Agent Systems](https://example.com)
- [Auction Theory](https://en.wikipedia.org/wiki/Auction_theory)
- [Team Coordination Mechanisms](https://example.com)
- [Agent协作模式](https://example.com)

## ✨ 快速选择指南

**根据你的需求选择框架**:

- 🗣️ **需要观点融合** → 使用 **辩论框架**
- 🤝 **需要达成一致** → 使用 **共识框架**
- 🏛️ **有组织结构** → 使用 **分层决策框架**
- 💰 **需要资源分配** → 使用 **拍卖框架**
- 👥 **需要团队协作** → 使用 **团队协作框架**

**组合使用示例**:
```java
// 场景: 企业项目决策
// 1. 顶层使用分层框架分解任务
// 2. 中层使用团队框架执行
// 3. 底层使用共识框架达成一致
```

## 🤝 贡献

欢迎提交新的框架实现和共识策略!
