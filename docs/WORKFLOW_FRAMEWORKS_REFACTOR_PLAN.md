
# Frameworks 基于 Workflow 底座的重构方案

## 一、重构目标

将 `evox-agents/frameworks` 下的 5 个多智能体协同框架（debate、consensus、team、hierarchical、auction）重构为基于 `evox-workflow` DAG 引擎的上层实现，消除各框架自行实现的循环控制、并行执行、状态管理等重复逻辑。

**核心原则**：Workflow 是编排基座（HOW to execute），Frameworks 是协同语义（WHAT to coordinate）。

---

## 二、架构总览

### 重构前

```
evox-agents/frameworks/
├── debate/          → 自己实现 for 循环轮次 + 状态管理 + 共识检查
├── consensus/       → 自己实现 for 循环轮次 + 提议收集 + 策略评估
├── team/            → 自己实现 5 种模式的执行逻辑 + 线程池并行
├── hierarchical/    → 自己实现递归层级执行
└── auction/         → 自己实现 while 循环 + 6 种拍卖机制
```

每个框架独立维护：循环控制、并行执行、状态跟踪、错误处理、执行历史。

### 重构后

```
evox-workflow/                          ← 编排基座（不变，增强）
├── base/
│   ├── Workflow.java                   ← 不变
│   └── WorkflowNode.java              ← 新增 COLLECT 节点类型
├── graph/
│   └── WorkflowGraph.java             ← 不变
├── execution/
│   ├── WorkflowExecutor.java          ← 新增 COLLECT 节点执行 + 动态图支持
│   └── WorkflowContext.java           ← 不变
├── builder/
│   └── WorkflowBuilder.java           ← 不变
└── node/                              ← 新增：可复用的自定义节点处理器
    └── NodeHandler.java               ← 新增：节点处理器接口

evox-agents/frameworks/                 ← 协同语义（重构）
├── base/                              ← 新增：框架公共基类
│   ├── MultiAgentFramework.java       ← 所有框架的基类，持有 Workflow
│   └── FrameworkWorkflowBuilder.java  ← 框架专用的 Workflow 构建工具
├── debate/                            ← 重构：用 Workflow 编排辩论流程
│   ├── MultiAgentDebate.java          ← 重构：构建辩论 DAG，委托 Workflow 执行
│   ├── DebateAgent.java               ← 提取为独立接口（不变）
│   ├── DebateNodeHandler.java         ← 新增：辩论专用节点处理器
│   └── DebateConfig.java              ← 不变
├── consensus/                         ← 重构：用 Workflow 编排共识流程
│   ├── ConsensusFramework.java        ← 重构：构建共识 DAG
│   ├── ConsensusNodeHandler.java      ← 新增：共识专用节点处理器
│   └── ...                            ← 策略类不变
├── team/                              ← 重构：用 Workflow 编排团队协作
│   ├── TeamFramework.java             ← 重构：构建团队 DAG
│   ├── TeamNodeHandler.java           ← 新增：团队专用节点处理器
│   └── ...
├── hierarchical/                      ← 重构：用 SUBWORKFLOW 实现层级递归
│   ├── HierarchicalFramework.java     ← 重构
│   └── ...
└── auction/                           ← 重构：用 Workflow 编排拍卖流程
    ├── AuctionFramework.java          ← 重构
    ├── AuctionNodeHandler.java        ← 新增
    └── ...
```

---

## 三、Workflow 层增强（前置改动）

### 3.1 新增 `NodeHandler` 接口

在 `evox-workflow` 中新增节点处理器接口，允许 frameworks 注册自定义的节点执行逻辑，而不需要修改 `WorkflowExecutor`。

```java
package io.leavesfly.evox.workflow.node;

/**
 * 节点处理器接口
 * 允许外部模块注册自定义的节点执行逻辑
 */
public interface NodeHandler {
    /**
     * 执行节点
     * @param context 工作流上下文
     * @param node 当前节点
     * @return 执行结果
     */
    Mono<Object> handle(WorkflowContext context, WorkflowNode node);
}
```

### 3.2 新增 `COLLECT` 节点类型

在 `WorkflowNode.NodeType` 中新增 `COLLECT` 类型，用于"向多个 Agent 收集响应"的场景（辩论发言、共识提议、团队执行等）。

```java
public enum NodeType {
    ACTION,        // 单 Agent 执行
    DECISION,      // 条件分支
    PARALLEL,      // 并行执行
    LOOP,          // 循环
    SUBWORKFLOW,   // 子工作流
    COLLECT        // 新增：多 Agent 收集（广播问题，收集所有响应）
}
```

### 3.3 `WorkflowNode` 新增字段

```java
/**
 * 自定义节点处理器名称（用于 COLLECT 等自定义节点）
 * 执行时通过此名称从注册表中查找对应的 NodeHandler
 */
private String handlerName;

/**
 * 节点自定义配置（传递给 NodeHandler 的参数）
 */
private Map<String, Object> handlerConfig;
```

### 3.4 `WorkflowExecutor` 增强

1. 维护一个 `Map<String, NodeHandler> handlerRegistry`
2. 在 `executeNodeByType` 中增加 `COLLECT` 分支，委托给注册的 `NodeHandler`
3. 支持**动态图修改**：允许 `NodeHandler` 在执行过程中向图中添加新节点/边（用于苏格拉底模式等动态流程）

```java
// executeNodeByType 新增分支
case COLLECT -> executeCollectNode(node);

private Mono<Object> executeCollectNode(WorkflowNode node) {
    String handlerName = node.getHandlerName();
    NodeHandler handler = handlerRegistry.get(handlerName);
    if (handler == null) {
        return Mono.error(new RuntimeException("NodeHandler not found: " + handlerName));
    }
    return handler.handle(context, node);
}
```

### 3.5 `WorkflowBuilder` 增强

新增 `collect()` 方法，支持构建 COLLECT 节点：

```java
public WorkflowBuilder collect(String stepName, String handlerName, Map<String, Object> config) {
    // 创建 COLLECT 类型节点
}
```

---

## 四、框架公共基类

### 4.1 `MultiAgentFramework`

所有框架的公共基类，封装 Workflow 的构建和执行：

```java
package io.leavesfly.evox.frameworks.base;

public abstract class MultiAgentFramework<T> {
    
    protected Workflow workflow;
    protected WorkflowContext context;
    
    /**
     * 构建工作流 DAG（由子类实现）
     */
    protected abstract Workflow buildWorkflow(String task);
    
    /**
     * 注册节点处理器（由子类实现）
     */
    protected abstract Map<String, NodeHandler> getNodeHandlers();
    
    /**
     * 执行框架
     */
    public T execute(String task) {
        // 1. 构建 Workflow
        this.workflow = buildWorkflow(task);
        
        // 2. 注册 NodeHandlers
        Map<String, NodeHandler> handlers = getNodeHandlers();
        handlers.forEach((name, handler) -> 
            workflow.getExecutor().registerHandler(name, handler));
        
        // 3. 执行
        String result = workflow.execute(Map.of("task", task));
        
        // 4. 转换结果（由子类实现）
        return convertResult(result);
    }
    
    protected abstract T convertResult(String rawResult);
}
```

---

## 五、各框架重构详细设计

### 5.1 Debate 辩论框架

**DAG 结构**：

```
[LOOP: debate_loop (maxRounds)]
    │
    ├── [COLLECT: agent_responses]     ← 所有 DebateAgent 发言
    │       handlerName = "debate_respond"
    │
    ├── [ACTION: process_responses]    ← 处理响应（评分、观点跟踪）
    │       handlerName = "debate_process"
    │
    └── [DECISION: check_consensus]    ← 检查是否达成共识
            condition = "consensus_reached == true"
            ├── true  → 跳出循环
            └── false → 继续下一轮

[ACTION: generate_final_answer]        ← 生成最终答案
```

**对于不同辩论模式的处理**：
- **ROUND_ROBIN**：COLLECT 节点串行遍历所有 agent
- **ADVERSARIAL**：COLLECT 节点只收集正方+反方，后接交叉质询 ACTION
- **PANEL**：COLLECT 节点并行收集，后接回应 COLLECT
- **SOCRATIC**：DECISION 节点后接动态生成问题的 ACTION，修改下一轮 COLLECT 的输入

**保留的独立逻辑**（放在 `DebateNodeHandler` 中）：
- `DebateAgent.respond()` 调用
- 观点跟踪 `viewpointTracker`
- 评分逻辑 `evaluateResponse()`
- 共识检查 `checkConsensus()`
- 最终答案生成 `generateFinalAnswer()`

**删除的重复逻辑**：
- `for (currentRound = 1; ...)` 循环 → 由 LOOP 节点替代
- `status` 状态管理 → 由 `WorkflowContext.ExecutionState` 替代
- `startTime` / `duration` 计时 → 由 Workflow 层统一管理
- `history` 记录 → 由 `WorkflowContext.messageHistory` 替代

### 5.2 Consensus 共识框架

**DAG 结构**：

```
[LOOP: consensus_loop (maxRounds)]
    │
    ├── [COLLECT: collect_proposals]    ← 收集所有 Agent 提议
    │       handlerName = "consensus_propose"
    │
    ├── [ACTION: evaluate_consensus]   ← 用 ConsensusStrategy 评估
    │       handlerName = "consensus_evaluate"
    │
    ├── [ACTION: notify_agents]        ← 通知 Agent 评估结果
    │       handlerName = "consensus_notify"
    │
    └── [DECISION: check_result]       ← 检查共识 + 早停
            ├── consensus_reached → 跳出
            ├── early_stop → 跳出
            └── continue → 下一轮

[ACTION: fallback]                     ← 未达成共识时的回退
```

**保留的独立逻辑**：
- `ConsensusStrategy` 接口及 4 种策略实现完全不变
- `ConsensusAgent` 接口不变
- 早停判断逻辑

**删除的重复逻辑**：
- `for (currentRound = 1; ...)` → LOOP 节点
- `history` 管理 → WorkflowContext
- 手动的 `startTime` / `duration` → Workflow 层

### 5.3 Team 团队协作框架

**DAG 结构（按模式动态构建）**：

**PARALLEL 模式**：
```
[PARALLEL: parallel_execute (ALL)]
    ├── [ACTION: member_1_execute]
    ├── [ACTION: member_2_execute]
    └── [ACTION: member_3_execute]

[ACTION: aggregate_results]
```

**SEQUENTIAL 模式**：
```
[ACTION: member_1_execute]
    → [ACTION: member_2_execute]
        → [ACTION: member_3_execute]
            → [ACTION: aggregate_results]
```

**COLLABORATIVE 模式**：
```
[LOOP: collaboration_loop (maxRounds)]
    ├── [COLLECT: all_members_execute]
    ├── [ACTION: collaborative_discuss]
    └── [DECISION: check_convergence]

[ACTION: aggregate_results]
```

**COMPETITIVE 模式**：
```
[PARALLEL: competitive_execute (ALL)]
    ├── [ACTION: member_1_execute]
    ├── [ACTION: member_2_execute]
    └── [ACTION: member_3_execute]

[ACTION: select_best_result]
```

**HIERARCHICAL 模式**：
```
[ACTION: leader_plan]
    → [PARALLEL: subordinates_execute]
        → [ACTION: leader_aggregate]
```

**保留的独立逻辑**：
- `TeamMember` 接口
- `RoleManager` 角色管理
- 投票功能 `vote()`
- 消息传递 `sendMessage()` / `broadcast()`
- 任务分解 `taskDecomposer`
- 聚合策略 `AggregationStrategy`

**删除的重复逻辑**：
- 5 个 `executeXxx()` 方法中的循环/并行/串行逻辑 → 由不同 DAG 拓扑替代
- `ExecutorService` 线程池管理 → 由 Workflow PARALLEL 节点替代
- `executionHistory` → WorkflowContext
- `status` 管理 → Workflow 层

### 5.4 Hierarchical 层级决策框架

**DAG 结构**：

利用 `SUBWORKFLOW` 节点天然支持递归嵌套：

```
[ACTION: top_layer_decide]
    │
    [DECISION: need_delegation?]
        ├── no  → [ACTION: return_result]
        └── yes → [PARALLEL: delegate_subtasks]
                    ├── [SUBWORKFLOW: subtask_1]  ← 递归嵌套同结构的子 Workflow
                    ├── [SUBWORKFLOW: subtask_2]
                    └── [SUBWORKFLOW: subtask_3]
                        → [ACTION: aggregate_sub_results]
```

**保留的独立逻辑**：
- `DecisionLayer` 接口
- `AggregationStrategy`

**删除的重复逻辑**：
- `executeRecursive()` 递归方法 → 由 SUBWORKFLOW 嵌套替代
- `history` 管理 → WorkflowContext

### 5.5 Auction 拍卖框架

**DAG 结构**：

**英式拍卖（递增价格）**：
```
[LOOP: auction_loop (maxRounds)]
    ├── [COLLECT: collect_bids]        ← 收集所有竞价者出价
    │       handlerName = "auction_bid"
    │
    ├── [ACTION: evaluate_bids]        ← 评估出价，更新当前价格
    │
    └── [DECISION: auction_end?]       ← 无人出价 / 达到保留价
            ├── end → 跳出
            └── continue → 下一轮

[ACTION: determine_winner]
```

**荷兰式拍卖（递减价格）**：
```
[LOOP: dutch_loop (maxRounds)]
    ├── [COLLECT: ask_accept]          ← 询问竞价者是否接受当前价格
    │
    ├── [DECISION: anyone_accepted?]
    │       ├── yes → 跳出，确定赢家
    │       └── no  → 降价
    │
    └── [ACTION: decrease_price]

[ACTION: determine_winner]
```

**密封拍卖（第一价格/第二价格/Vickrey/全付）**：
```
[COLLECT: sealed_bids]                 ← 一次性收集所有密封出价

[ACTION: determine_winner]             ← 根据机制选择赢家和价格
```

**保留的独立逻辑**：
- `Bidder` 接口
- `AuctionMechanism` 枚举
- 各种拍卖机制的赢家确定逻辑

---

## 六、依赖关系变更

### 重构前

```
evox-agents (pom.xml)
├── evox-core
├── evox-models
├── evox-actions
├── evox-memory
└── evox-tools

evox-workflow (pom.xml)
├── evox-core
├── evox-memory
└── evox-agents (test scope)
```

### 重构后

```
evox-agents (pom.xml)
├── evox-core
├── evox-models
├── evox-actions
├── evox-memory
├── evox-tools
└── evox-workflow          ← 新增：frameworks 依赖 workflow

evox-workflow (pom.xml)
├── evox-core
├── evox-memory
└── evox-agents (test scope，不变)
```

注意：不存在循环依赖。`evox-workflow` 不依赖 `evox-agents`（仅 test scope），`evox-agents` 新增对 `evox-workflow` 的依赖。

---

## 七、需要新增的文件清单

### evox-workflow 层（3 个文件）

| 文件 | 说明 |
|------|------|
| `workflow/node/NodeHandler.java` | 节点处理器接口 |
| `workflow/node/NodeHandlerRegistry.java` | 处理器注册表 |
| 修改 `WorkflowNode.java` | 新增 COLLECT 类型 + handlerName/handlerConfig 字段 |
| 修改 `WorkflowExecutor.java` | 新增 COLLECT 执行 + handlerRegistry + 动态图支持 |

### evox-agents/frameworks 层（8 个新文件 + 5 个重构文件）

| 文件 | 说明 |
|------|------|
| `frameworks/base/MultiAgentFramework.java` | 框架公共基类 |
| `frameworks/base/FrameworkWorkflowBuilder.java` | 框架专用 Workflow 构建工具 |
| `frameworks/debate/DebateNodeHandler.java` | 辩论节点处理器 |
| `frameworks/consensus/ConsensusNodeHandler.java` | 共识节点处理器 |
| `frameworks/team/TeamNodeHandler.java` | 团队节点处理器 |
| `frameworks/hierarchical/HierarchicalNodeHandler.java` | 层级节点处理器 |
| `frameworks/auction/AuctionNodeHandler.java` | 拍卖节点处理器 |
| 重构 `MultiAgentDebate.java` | 构建辩论 DAG，委托 Workflow |
| 重构 `ConsensusFramework.java` | 构建共识 DAG，委托 Workflow |
| 重构 `TeamFramework.java` | 构建团队 DAG，委托 Workflow |
| 重构 `HierarchicalFramework.java` | 构建层级 DAG，委托 Workflow |
| 重构 `AuctionFramework.java` | 构建拍卖 DAG，委托 Workflow |

### 不变的文件

以下文件保持不变，无需修改：
- `ConsensusStrategy.java` 及 4 种策略实现
- `ConsensusConfig.java`、`ConsensusResult.java`、`ConsensusEvaluation.java`、`ConsensusRecord.java`
- `DefaultConsensusAgent.java`、`DefaultDebateAgent.java`
- `TeamRole.java`、`TeamTask.java`、`TeamConfig.java`、`TeamMember.java`、`TeamResult.java`
- `RoleManager.java`、`TaskExecution.java`、`CollaborationMode.java`、`DefaultTeamMember.java`
- `DecisionLayer.java`、`LayerDecision.java`、`ExecutionRecord.java`、`HierarchicalConfig.java`、`HierarchicalResult.java`
- `DefaultDecisionLayer.java`
- `Bidder.java`、`Bid.java`、`BidType.java`、`BidRecord.java`、`AuctionConfig.java`、`AuctionResult.java`
- `DefaultBidder.java`、`AuctionMechanism.java`

---

## 八、API 兼容性

### 对外 API 保持不变

所有框架的对外 API 签名保持不变，用户代码无需修改：

```java
// Debate - 不变
MultiAgentDebate debate = new MultiAgentDebate(agents, maxRounds, moderator);
DebateResult result = debate.debate("question");

// Consensus - 不变
ConsensusFramework<String> framework = new ConsensusFramework<>(agents, strategy, config);
ConsensusResult<String> result = framework.reachConsensus("question");

// Team - 不变
TeamFramework<String> team = new TeamFramework<>(members, mode, config);
TeamResult<String> result = team.executeTeamTask("task");

// Hierarchical - 不变
HierarchicalFramework<String> hierarchy = new HierarchicalFramework<>(layers, config);
HierarchicalResult<String> result = hierarchy.executeHierarchical("task");

// Auction - 不变
AuctionFramework<String> auction = new AuctionFramework<>(item, mechanism, bidders, config);
AuctionResult<String> result = auction.startAuction();
```

### 新增能力

重构后，用户可以获得以下新能力：

1. **获取 Workflow 实例**：`framework.getWorkflow()` 可以拿到底层 Workflow，进行自定义扩展
2. **查看执行进度**：`framework.getWorkflow().getProgress()` 实时查看进度
3. **组合框架**：可以将一个框架作为另一个框架的 SUBWORKFLOW 节点
4. **可视化**：利用 workflow 的 visualization 模块可视化协同流程

---

## 九、实施顺序

| 步骤 | 内容 | 涉及模块 |
|------|------|---------|
| 1 | Workflow 层增强：NodeHandler + COLLECT + handlerRegistry | evox-workflow |
| 2 | 框架公共基类：MultiAgentFramework + FrameworkWorkflowBuilder | evox-agents/frameworks/base |
| 3 | 重构 Consensus 框架（最简单，验证模式） | evox-agents/frameworks/consensus |
| 4 | 重构 Debate 框架 | evox-agents/frameworks/debate |
| 5 | 重构 Auction 框架 | evox-agents/frameworks/auction |
| 6 | 重构 Hierarchical 框架 | evox-agents/frameworks/hierarchical |
| 7 | 重构 Team 框架（最复杂） | evox-agents/frameworks/team |
| 8 | 更新 pom.xml 依赖 | evox-agents/pom.xml |
| 9 | 更新 README 文档 | 两个模块的 README |

---

## 十、代码量预估

| 类别 | 预估 |
|------|------|
| 新增代码 | ~1200 行（NodeHandler 体系 + 基类 + 各 NodeHandler 实现） |
| 删除代码 | ~1500 行（各框架中的循环/并行/状态管理重复逻辑） |
| 修改代码 | ~800 行（5 个框架主类重构） |
| **净减少** | **~300 行** |

重构后代码更少、职责更清晰、可组合性更强。
