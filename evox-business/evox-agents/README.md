# EvoX Agents 智能体模块

**智能代理(Agent)系统**是 EvoX 框架的核心业务模块,提供多种类型的智能体实现和多智能体协同框架。

## 📦 模块结构

```
evox-agents/
├── base/           # 基础Agent抽象类
├── action/         # 函数执行型Agent
├── react/          # ReAct推理Agent
├── customize/      # 自定义Agent
├── plan/           # 任务规划Agent
├── specialized/    # 专用Agent(路由、工具、聊天等)
├── manager/        # Agent管理器
└── frameworks/     # 多智能体协同框架 ⭐
    ├── debate/         # 辩论框架
    └── consensus/      # 共识框架
        └── strategy/       # 共识策略实现
```

## 🎯 核心功能

### 1. 单智能体系统

#### 1.1 Agent基类
- **Agent**: 所有智能体的基础抽象类
- 提供统一的执行接口和生命周期管理
- 支持动作(Action)管理和LLM集成

#### 1.2 专用Agent类型

| Agent类型 | 说明 | 适用场景 |
|----------|------|---------|
| **ActionAgent** | 函数执行代理 | 工具封装、确定性任务 |
| **ReActAgent** | 推理-行动代理 | 复杂推理、多步骤任务 |
| **CustomizeAgent** | 自定义代理 | 灵活定制、特殊需求 |
| **TaskPlannerAgent** | 任务规划代理 | 任务分解、计划制定 |
| **RouterAgent** | 路由代理 | 请求分发、智能路由 |
| **ToolAgent** | 工具代理 | 工具调用、API集成 |
| **ChatAgent** | 聊天代理 | 对话交互、问答系统 |

### 2. 多智能体协同框架 ⭐

evox-agents 模块整合了多智能体协同框架,提供两大协作模式:

#### 2.1 辩论框架 (Debate Framework)
- **位置**: `frameworks.debate`
- **核心**: `MultiAgentDebate`
- **功能**: 多智能体轮次辩论达成共识
- **用途**: 多视角分析、决策优化、观点融合

**快速示例**:
```java
List<DebateAgent> agents = Arrays.asList(
    new MyDebateAgent("Analyst"),
    new MyDebateAgent("Critic"),
    new MyDebateAgent("Optimizer")
);

MultiAgentDebate debate = new MultiAgentDebate(agents, 5);
String result = debate.debate("What is the best solution?");
```

#### 2.2 共识框架 (Consensus Framework) 🆕
- **位置**: `frameworks.consensus`
- **核心**: `ConsensusFramework`
- **功能**: 通用共识达成框架,支持多种共识策略
- **特性**:
  - ✅ 4种内置共识策略(多数投票、加权投票、贝叶斯、一致性检查)
  - ✅ 可配置的早停机制
  - ✅ 智能体反馈机制
  - ✅ 完整的历史记录和元数据

**内置共识策略**:

| 策略 | 类名 | 适用场景 |
|------|------|---------|
| 多数投票 | `MajorityVotingStrategy` | 离散选项、民主决策 |
| 加权投票 | `WeightedVotingStrategy` | 专家系统、信誉机制 |
| 贝叶斯共识 | `BayesianConsensusStrategy` | 复杂推理、历史数据丰富 |
| 一致性检查 | `ConsistencyCheckStrategy` | 连续值、文本生成 |

**快速示例**:
```java
// 1. 创建共识智能体
List<ConsensusAgent<String>> agents = Arrays.asList(
    new MyConsensusAgent("Expert1", 2.0),
    new MyConsensusAgent("Expert2", 1.5),
    new MyConsensusAgent("Expert3", 1.0)
);

// 2. 配置并选择策略
ConsensusConfig config = ConsensusConfig.builder()
    .maxRounds(10)
    .consensusThreshold(0.8)
    .minSupportRate(0.5)
    .enableEarlyStopping(true)
    .build();

ConsensusStrategy<String> strategy = new WeightedVotingStrategy<>(config);

// 3. 创建框架并执行
ConsensusFramework<String> framework = new ConsensusFramework<>(agents, strategy, config);
ConsensusResult<String> result = framework.reachConsensus("Optimize this design");

// 4. 处理结果
if (result.isReached()) {
    System.out.println("✅ Consensus: " + result.getResult());
    System.out.println("📊 Confidence: " + result.getConfidence());
    System.out.println("🔄 Rounds: " + result.getRounds());
}
```

**详细文档**: 查看 [`frameworks/README.md`](src/main/java/io/leavesfly/evox/agents/frameworks/README.md) 了解完整的使用指南和高级特性。

### 3. Agent管理器

**AgentManager** 提供:
- 智能体注册与管理
- 生命周期控制
- 资源清理

## 🚀 快速开始

### 基础使用

```java
// 1. 创建简单Agent
ActionAgent agent = ActionAgent.builder()
    .agentId("agent-001")
    .name("Calculator")
    .description("Simple calculator agent")
    .build();

// 2. 添加动作
agent.addAction("add", new AddAction());

// 3. 执行
Message result = agent.execute("add", messages);
```

### 多智能体协同

```java
// 使用辩论框架
MultiAgentDebate debate = new MultiAgentDebate(debateAgents, 5);
String debateResult = debate.debate(question);

// 使用共识框架
ConsensusFramework<String> consensus = new ConsensusFramework<>(
    consensusAgents, 
    new MajorityVotingStrategy<>()
);
ConsensusResult<String> consensusResult = consensus.reachConsensus(question);
```

## 📚 依赖关系

```
evox-agents
├── evox-core        # 核心抽象
├── evox-models      # LLM模型
├── evox-actions     # 动作引擎
└── evox-tools       # 工具集成
```

## 🔧 扩展开发

### 实现自定义Agent

```java
public class MyCustomAgent extends Agent {
    @Override
    protected Mono<Message> doExecute(String actionName, List<Message> messages) {
        // 自定义执行逻辑
        return Mono.just(new Message("result"));
    }
}
```

### 实现自定义共识策略

```java
public class MyConsensusStrategy<T> implements ConsensusStrategy<T> {
    @Override
    public ConsensusEvaluation<T> evaluate(List<T> proposals, 
                                           List<ConsensusAgent<T>> agents) {
        // 自定义共识评估逻辑
        return ConsensusEvaluation.<T>builder()
            .consensusReached(true)
            .consensusValue(bestProposal)
            .confidence(0.95)
            .build();
    }
    
    // ... 其他方法
}
```

## 📊 性能特性

- **异步执行**: 基于 Reactor 的响应式编程
- **并发安全**: 线程安全的智能体管理
- **资源优化**: 自动清理和回收机制

## 🔍 调试与监控

启用详细日志:
```xml
<logger name="io.leavesfly.evox.agents" level="DEBUG"/>
<logger name="io.leavesfly.evox.agents.frameworks" level="DEBUG"/>
```

## 📖 参考文档

- [Agent API详细文档](../../doc/AGENT_API.md)
- [Frameworks使用指南](src/main/java/io/leavesfly/evox/agents/frameworks/README.md)
- [多智能体协同最佳实践](../../doc/MULTI_AGENT_PATTERNS.md)

## 🤝 贡献

欢迎贡献新的Agent类型和协同框架实现!

## 版本历史

- **v1.0.0**: 基础Agent系统
- **v1.1.0**: 增加辩论框架
- **v1.2.0**: 🆕 增加共识框架,支持4种共识策略
