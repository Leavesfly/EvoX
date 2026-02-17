package io.leavesfly.evox.frameworks.team;

import io.leavesfly.evox.frameworks.base.MultiAgentFramework;
import io.leavesfly.evox.workflow.base.Workflow;
import io.leavesfly.evox.workflow.base.WorkflowNode;
import io.leavesfly.evox.workflow.graph.WorkflowGraph;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 团队协作框架
 * 继承自 MultiAgentFramework，基于 Workflow DAG 引擎实现多智能体团队协作
 *
 * @param <T> 任务结果类型
 * @author EvoX Team
 */
@Slf4j
@Data
public class TeamFramework<T> extends MultiAgentFramework {

    /**
     * 团队成员列表
     */
    private List<TeamMember<T>> members;

    /**
     * 角色管理器
     */
    private RoleManager roleManager;

    /**
     * 协作模式
     */
    private CollaborationMode mode;

    /**
     * 团队配置
     */
    private TeamConfig config;

    /**
     * 任务队列
     */
    private Queue<TeamTask<T>> taskQueue;

    /**
     * 执行历史
     */
    private List<TaskExecution<T>> executionHistory;

    /**
     * 团队状态
     */
    private TeamStatus status;

    /**
     * 消息通道
     */
    private final Map<String, BlockingQueue<TeamMessage>> messageChannels = new ConcurrentHashMap<>();

    /**
     * 任务分解器
     */
    private Function<String, List<SubTask>> taskDecomposer;

    /**
     * 投票策略
     */
    private VotingStrategy votingStrategy = VotingStrategy.MAJORITY;

    /**
     * 团队上下文
     */
    private final Map<String, Object> teamContext = new ConcurrentHashMap<>();

    /**
     * 构造函数
     */
    public TeamFramework(List<TeamMember<T>> members, CollaborationMode mode, TeamConfig config) {
        this.members = new ArrayList<>(members);
        this.mode = mode;
        this.config = config;
        this.roleManager = new RoleManager();
        this.executionHistory = new ArrayList<>();
        this.taskQueue = new LinkedList<>();
        this.status = TeamStatus.IDLE;
        this.frameworkName = "TeamFramework";

        // 初始化成员角色和消息通道
        for (TeamMember<T> member : members) {
            roleManager.assignRole(member.getMemberId(), member.getRole());
            messageChannels.put(member.getMemberId(), new LinkedBlockingQueue<>());
        }
    }

    /**
     * 构造函数（使用默认配置）
     */
    public TeamFramework(List<TeamMember<T>> members, CollaborationMode mode) {
        this(members, mode, TeamConfig.builder().build());
    }

    // ============= MultiAgentFramework 抽象方法实现 =============

    @Override
    protected WorkflowGraph buildWorkflowGraph(String task) {
        log.debug("Building workflow graph for team collaboration task: {}", task);

        WorkflowGraph graph = new WorkflowGraph(task);

        switch (mode) {
            case PARALLEL:
                buildParallelWorkflowGraph(graph, task);
                break;
            case SEQUENTIAL:
                buildSequentialWorkflowGraph(graph, task);
                break;
            case COLLABORATIVE:
                buildCollaborativeWorkflowGraph(graph, task);
                break;
            case COMPETITIVE:
                buildCompetitiveWorkflowGraph(graph, task);
                break;
            case HIERARCHICAL:
                buildHierarchicalWorkflowGraph(graph, task);
                break;
            default:
                throw new IllegalArgumentException("Unknown collaboration mode: " + mode);
        }

        return graph;
    }

    private void buildParallelWorkflowGraph(WorkflowGraph graph, String task) {
        List<String> memberNodeIds = new ArrayList<>();
        
        for (int i = 0; i < members.size(); i++) {
            Map<String, Object> handlerConfig = new HashMap<>();
            handlerConfig.put("memberIndex", i);
            
            WorkflowNode memberNode = createCollectNode(
                "member_" + i,
                "MemberExecuteHandler",
                handlerConfig
            );
            graph.addNode(memberNode);
            memberNodeIds.add(memberNode.getNodeId());
        }

        WorkflowNode parallelNode = createParallelNode(
            "parallel_execution",
            WorkflowNode.ParallelStrategy.ALL
        );
        parallelNode.setParallelNodes(memberNodeIds);
        graph.addNode(parallelNode);

        WorkflowNode aggregateNode = createCollectNode(
            "aggregate",
            "TeamAggregateHandler",
            Map.of()
        );
        graph.addNode(aggregateNode);

        graph.addEdge(parallelNode.getNodeId(), aggregateNode.getNodeId());
    }

    private void buildSequentialWorkflowGraph(WorkflowGraph graph, String task) {
        String previousNodeId = null;

        for (int i = 0; i < members.size(); i++) {
            Map<String, Object> handlerConfig = new HashMap<>();
            handlerConfig.put("memberIndex", i);
            
            WorkflowNode memberNode = createCollectNode(
                "member_" + i,
                "MemberExecuteHandler",
                handlerConfig
            );
            graph.addNode(memberNode);

            if (previousNodeId != null) {
                graph.addEdge(previousNodeId, memberNode.getNodeId());
            }
            previousNodeId = memberNode.getNodeId();
        }

        WorkflowNode aggregateNode = createCollectNode(
            "aggregate",
            "TeamAggregateHandler",
            Map.of()
        );
        graph.addNode(aggregateNode);

        if (previousNodeId != null) {
            graph.addEdge(previousNodeId, aggregateNode.getNodeId());
        }
    }

    private void buildCollaborativeWorkflowGraph(WorkflowGraph graph, String task) {
        WorkflowNode collabRoundNode = createCollectNode(
            "collab_round",
            "TeamCollaborativeRoundHandler",
            Map.of()
        );
        graph.addNode(collabRoundNode);

        WorkflowNode loopNode = createLoopNode(
            "collab_loop",
            10,
            "collaboration_converged == false"
        );
        loopNode.setLoopBodyNodeId(collabRoundNode.getNodeId());
        graph.addNode(loopNode);

        WorkflowNode aggregateNode = createCollectNode(
            "aggregate",
            "TeamAggregateHandler",
            Map.of()
        );
        graph.addNode(aggregateNode);

        graph.addEdge(loopNode.getNodeId(), aggregateNode.getNodeId());
    }

    private void buildCompetitiveWorkflowGraph(WorkflowGraph graph, String task) {
        List<String> memberNodeIds = new ArrayList<>();
        
        for (int i = 0; i < members.size(); i++) {
            Map<String, Object> handlerConfig = new HashMap<>();
            handlerConfig.put("memberIndex", i);
            
            WorkflowNode memberNode = createCollectNode(
                "member_" + i,
                "MemberExecuteHandler",
                handlerConfig
            );
            graph.addNode(memberNode);
            memberNodeIds.add(memberNode.getNodeId());
        }

        WorkflowNode parallelNode = createParallelNode(
            "parallel_execution",
            WorkflowNode.ParallelStrategy.ALL
        );
        parallelNode.setParallelNodes(memberNodeIds);
        graph.addNode(parallelNode);

        WorkflowNode aggregateNode = createCollectNode(
            "aggregate",
            "TeamAggregateHandler",
            Map.of()
        );
        graph.addNode(aggregateNode);

        graph.addEdge(parallelNode.getNodeId(), aggregateNode.getNodeId());
    }

    private void buildHierarchicalWorkflowGraph(WorkflowGraph graph, String task) {
        List<TeamMember<T>> sortedMembers = members.stream()
            .sorted(Comparator.comparingInt(m -> roleManager.getRolePriority(m.getRole())))
            .collect(Collectors.toList());

        TeamMember<T> leader = sortedMembers.get(0);
        int leaderIndex = members.indexOf(leader);
        
        Map<String, Object> leaderConfig = new HashMap<>();
        leaderConfig.put("memberIndex", leaderIndex);
        leaderConfig.put("memberTask", task);
        
        WorkflowNode leaderPlanNode = createCollectNode(
            "leader_plan",
            "MemberExecuteHandler",
            leaderConfig
        );
        graph.addNode(leaderPlanNode);

        List<String> subordinateNodeIds = new ArrayList<>();
        for (int i = 1; i < sortedMembers.size(); i++) {
            TeamMember<T> subordinate = sortedMembers.get(i);
            int subIndex = members.indexOf(subordinate);
            
            Map<String, Object> handlerConfig = new HashMap<>();
            handlerConfig.put("memberIndex", subIndex);
            
            WorkflowNode subordinateNode = createCollectNode(
                "subordinate_" + i,
                "MemberExecuteHandler",
                handlerConfig
            );
            graph.addNode(subordinateNode);
            subordinateNodeIds.add(subordinateNode.getNodeId());
        }

        WorkflowNode parallelNode = createParallelNode(
            "parallel_execution",
            WorkflowNode.ParallelStrategy.ALL
        );
        parallelNode.setParallelNodes(subordinateNodeIds);
        graph.addNode(parallelNode);

        WorkflowNode aggregateNode = createCollectNode(
            "aggregate",
            "TeamAggregateHandler",
            Map.of()
        );
        graph.addNode(aggregateNode);

        graph.addEdge(leaderPlanNode.getNodeId(), parallelNode.getNodeId());
        graph.addEdge(parallelNode.getNodeId(), aggregateNode.getNodeId());
    }

    @Override
    protected void registerNodeHandlers(Workflow workflow) {
        workflow.registerHandler("MemberExecuteHandler", new TeamNodeHandler.MemberExecuteHandler());
        workflow.registerHandler("TeamAggregateHandler", new TeamNodeHandler.TeamAggregateHandler());
        workflow.registerHandler("TeamCollaborativeRoundHandler", new TeamNodeHandler.TeamCollaborativeRoundHandler());
    }

    @Override
    protected void beforeExecute(String task) {
        status = TeamStatus.WORKING;
        log.info("Team framework starting execution for task: {}", task);
    }

    @Override
    protected void afterExecute(String rawResult) {
        status = TeamStatus.IDLE;
        log.info("Team framework execution completed");
    }

    // ============= 核心执行方法 =============

    /**
     * 执行团队任务
     */
    public TeamResult<T> executeTeamTask(String task) {
        log.info("Starting team task execution: {}", task);

        long startTime = System.currentTimeMillis();

        try {
            Map<String, Object> inputs = new HashMap<>();
            inputs.put("task", task);
            inputs.put("team_members", members);
            inputs.put("team_config", config);
            inputs.put("execution_history", executionHistory);
            inputs.put("member_results", new ArrayList<>());
            inputs.put("previous_result", null);
            inputs.put("collaboration_converged", false);
            inputs.put("round_count", 0);
            inputs.put("round_results", new ArrayList<>());
            inputs.put("mode", mode.name());

            // 通过 workflow 执行
            String rawResult = executeWorkflow(task, inputs);

            // 从 workflow graph 节点结果中提取 TeamResult
            @SuppressWarnings("unchecked")
            TeamResult<T> result = null;
            if (workflow != null && workflow.getGraph() != null) {
                for (WorkflowNode node : workflow.getGraph().getNodes().values()) {
                    if (node.getResult() instanceof TeamResult) {
                        result = (TeamResult<T>) node.getResult();
                        break;
                    }
                }
            }

            if (result != null) {
                result.setDuration(System.currentTimeMillis() - startTime);
                return result;
            }

            // Fallback: 如果 workflow 没有返回结果，使用旧逻辑
            return executeTaskDirectly(task, startTime);

        } catch (Exception e) {
            status = TeamStatus.ERROR;
            log.error("Team task execution failed", e);

            return TeamResult.<T>builder()
                .success(false)
                .error(e.getMessage())
                .duration(System.currentTimeMillis() - startTime)
                .build();
        }
    }

    /**
     * 直接执行任务（作为 fallback）
     */
    private TeamResult<T> executeTaskDirectly(String task, long startTime) {
        switch (mode) {
            case PARALLEL:
                return executeParallel(task);
            case SEQUENTIAL:
                return executeSequential(task);
            case HIERARCHICAL:
                return executeHierarchical(task);
            case COLLABORATIVE:
                return executeCollaborative(task);
            case COMPETITIVE:
                return executeCompetitive(task);
            default:
                throw new IllegalArgumentException("Unknown collaboration mode: " + mode);
        }
    }

    /**
     * 并行协作模式:所有成员同时工作
     */
    private TeamResult<T> executeParallel(String task) {
        log.debug("Executing in PARALLEL mode");

        List<T> results = new CopyOnWriteArrayList<>();
        List<TaskExecution<T>> executions = new CopyOnWriteArrayList<>();

        if (config.isEnableThreadPool()) {
            ExecutorService executor = Executors.newFixedThreadPool(config.getMaxThreads());
            try {
                List<Future<?>> futures = new ArrayList<>();
                for (TeamMember<T> member : members) {
                    Future<?> future = executor.submit(() -> {
                        long memberStart = System.currentTimeMillis();
                        T result = member.execute(task, null, executionHistory);
                        long memberDuration = System.currentTimeMillis() - memberStart;

                        results.add(result);
                        TaskExecution<T> execution = new TaskExecution<>(
                            member.getMemberId(),
                            task,
                            result,
                            memberDuration,
                            System.currentTimeMillis()
                        );
                        executions.add(execution);
                        executionHistory.add(execution);
                    });
                    futures.add(future);
                }

                for (Future<?> future : futures) {
                    try {
                        future.get(config.getTaskTimeout(), TimeUnit.MILLISECONDS);
                    } catch (Exception e) {
                        log.error("Parallel execution failed: {}", e.getMessage());
                    }
                }
            } finally {
                executor.shutdown();
            }
        } else {
            for (TeamMember<T> member : members) {
                long memberStart = System.currentTimeMillis();
                T result = member.execute(task, null, executionHistory);
                long memberDuration = System.currentTimeMillis() - memberStart;

                results.add(result);
                TaskExecution<T> execution = new TaskExecution<>(
                    member.getMemberId(),
                    task,
                    result,
                    memberDuration,
                    System.currentTimeMillis()
                );
                executions.add(execution);
                executionHistory.add(execution);
            }
        }

        T finalResult = aggregateResults(results, executions);

        return TeamResult.<T>builder()
            .success(true)
            .result(finalResult)
            .contributions(executions)
            .participantCount(members.size())
            .metadata(buildMetadata("parallel_threaded"))
            .build();
    }

    /**
     * 顺序协作模式:成员依次执行,后者基于前者结果
     */
    private TeamResult<T> executeSequential(String task) {
        log.debug("Executing in SEQUENTIAL mode");

        T currentResult = null;
        List<TaskExecution<T>> executions = new ArrayList<>();

        for (TeamMember<T> member : members) {
            long memberStart = System.currentTimeMillis();
            T result = member.execute(task, currentResult, executionHistory);
            long memberDuration = System.currentTimeMillis() - memberStart;

            currentResult = result;

            TaskExecution<T> execution = new TaskExecution<>(
                member.getMemberId(),
                task,
                result,
                memberDuration,
                System.currentTimeMillis()
            );
            executions.add(execution);
            executionHistory.add(execution);
        }

        return TeamResult.<T>builder()
            .success(true)
            .result(currentResult)
            .contributions(executions)
            .participantCount(members.size())
            .metadata(buildMetadata("sequential"))
            .build();
    }

    /**
     * 分层协作模式:按角色层级执行
     */
    private TeamResult<T> executeHierarchical(String task) {
        log.debug("Executing in HIERARCHICAL mode");

        // 按角色优先级排序
        List<TeamMember<T>> sortedMembers = members.stream()
            .sorted(Comparator.comparingInt(m -> roleManager.getRolePriority(m.getRole())))
            .collect(Collectors.toList());

        // 使用顺序执行
        return executeSequential(task);
    }

    /**
     * 协同协作模式:成员之间可以相互交流和协商
     */
    private TeamResult<T> executeCollaborative(String task) {
        log.debug("Executing in COLLABORATIVE mode");

        List<T> proposals = new ArrayList<>();
        List<TaskExecution<T>> executions = new ArrayList<>();

        // 第一轮:所有成员提出方案
        for (TeamMember<T> member : members) {
            long memberStart = System.currentTimeMillis();
            T result = member.execute(task, null, executionHistory);
            long memberDuration = System.currentTimeMillis() - memberStart;

            proposals.add(result);
            TaskExecution<T> execution = new TaskExecution<>(
                member.getMemberId(),
                task,
                result,
                memberDuration,
                System.currentTimeMillis()
            );
            executions.add(execution);
            executionHistory.add(execution);
        }

        // 协商和整合
        T finalResult = collaborativeDiscuss(proposals, executions);

        return TeamResult.<T>builder()
            .success(true)
            .result(finalResult)
            .contributions(executions)
            .participantCount(members.size())
            .metadata(buildMetadata("collaborative"))
            .build();
    }

    /**
     * 竞争协作模式:选择最佳方案
     */
    private TeamResult<T> executeCompetitive(String task) {
        log.debug("Executing in COMPETITIVE mode");

        List<T> proposals = new ArrayList<>();
        List<TaskExecution<T>> executions = new ArrayList<>();

        // 所有成员提出方案
        for (TeamMember<T> member : members) {
            long memberStart = System.currentTimeMillis();
            T result = member.execute(task, null, executionHistory);
            long memberDuration = System.currentTimeMillis() - memberStart;

            proposals.add(result);
            TaskExecution<T> execution = new TaskExecution<>(
                member.getMemberId(),
                task,
                result,
                memberDuration,
                System.currentTimeMillis()
            );
            executions.add(execution);
            executionHistory.add(execution);
        }

        // 选择最佳方案
        T bestResult = selectBestResult(proposals, executions);

        return TeamResult.<T>builder()
            .success(true)
            .result(bestResult)
            .contributions(executions)
            .participantCount(members.size())
            .metadata(buildMetadata("competitive"))
            .build();
    }

    /**
     * 聚合结果
     */
    @SuppressWarnings("unchecked")
    private T aggregateResults(List<T> results, List<TaskExecution<T>> executions) {
        if (config.getAggregationStrategy() != null) {
            return ((AggregationStrategy<T>) config.getAggregationStrategy()).aggregate(results, executions);
        }

        // 默认返回第一个非空结果
        return results.stream().filter(Objects::nonNull).findFirst().orElse(null);
    }

    /**
     * 协商讨论
     */
    private T collaborativeDiscuss(List<T> proposals, List<TaskExecution<T>> executions) {
        // 默认聚合所有提案
        return aggregateResults(proposals, executions);
    }

    /**
     * 选择最佳结果
     */
    @SuppressWarnings("unchecked")
    private T selectBestResult(List<T> proposals, List<TaskExecution<T>> executions) {
        if (config.getSelectionStrategy() != null) {
            return ((SelectionStrategy<T>) config.getSelectionStrategy()).select(proposals, executions);
        }

        // 默认返回第一个
        return proposals.isEmpty() ? null : proposals.get(0);
    }

    /**
     * 构建元数据
     */
    private Map<String, Object> buildMetadata(String modeUsed) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("mode", modeUsed);
        metadata.put("teamSize", members.size());
        metadata.put("totalExecutions", executionHistory.size());

        return metadata;
    }

    /**
     * 添加成员
     */
    public void addMember(TeamMember<T> member) {
        members.add(member);
        roleManager.assignRole(member.getMemberId(), member.getRole());
        messageChannels.put(member.getMemberId(), new LinkedBlockingQueue<>());
    }

    /**
     * 移除成员
     */
    public void removeMember(String memberId) {
        members.removeIf(m -> m.getMemberId().equals(memberId));
        roleManager.removeRole(memberId);
        messageChannels.remove(memberId);
    }

    // ============= 任务分解功能 =============

    /**
     * 设置任务分解器
     */
    public void setTaskDecomposer(Function<String, List<SubTask>> decomposer) {
        this.taskDecomposer = decomposer;
    }

    /**
     * 执行分解任务
     */
    public TeamResult<T> executeDecomposedTask(String task) {
        log.info("开始执行分解任务: {}", task);

        if (taskDecomposer == null) {
            log.warn("未配置任务分解器，回退到普通执行");
            return executeTeamTask(task);
        }

        long startTime = System.currentTimeMillis();
        status = TeamStatus.WORKING;

        try {
            // 分解任务
            List<SubTask> subTasks = taskDecomposer.apply(task);
            log.info("任务分解为 {} 个子任务", subTasks.size());

            // 分配子任务
            Map<TeamMember<T>, List<SubTask>> assignments = assignSubTasks(subTasks);

            // 执行子任务
            List<TaskExecution<T>> allExecutions = new ArrayList<>();
            List<T> results = new ArrayList<>();

            for (Map.Entry<TeamMember<T>, List<SubTask>> entry : assignments.entrySet()) {
                TeamMember<T> member = entry.getKey();
                for (SubTask subTask : entry.getValue()) {
                    long memberStart = System.currentTimeMillis();
                    T result = member.execute(subTask.getDescription(), null, executionHistory);
                    long memberDuration = System.currentTimeMillis() - memberStart;

                    results.add(result);
                    TaskExecution<T> execution = new TaskExecution<>(
                        member.getMemberId(),
                        subTask.getDescription(),
                        result,
                        memberDuration,
                        System.currentTimeMillis()
                    );
                    allExecutions.add(execution);
                    executionHistory.add(execution);
                }
            }

            // 聚合结果
            T finalResult = aggregateResults(results, allExecutions);

            status = TeamStatus.IDLE;

            return TeamResult.<T>builder()
                .success(true)
                .result(finalResult)
                .contributions(allExecutions)
                .participantCount(assignments.size())
                .duration(System.currentTimeMillis() - startTime)
                .metadata(buildMetadata("decomposed"))
                .build();

        } catch (Exception e) {
            status = TeamStatus.ERROR;
            log.error("分解任务执行失败", e);

            return TeamResult.<T>builder()
                .success(false)
                .error(e.getMessage())
                .duration(System.currentTimeMillis() - startTime)
                .build();
        }
    }

    /**
     * 分配子任务给成员
     */
    private Map<TeamMember<T>, List<SubTask>> assignSubTasks(List<SubTask> subTasks) {
        Map<TeamMember<T>, List<SubTask>> assignments = new HashMap<>();

        for (SubTask subTask : subTasks) {
            TeamMember<T> bestMember = findBestMemberForTask(subTask);
            assignments.computeIfAbsent(bestMember, k -> new ArrayList<>()).add(subTask);
        }

        return assignments;
    }

    /**
     * 查找最适合执行子任务的成员
     */
    private TeamMember<T> findBestMemberForTask(SubTask subTask) {
        // 根据所需技能匹配成员
        if (subTask.getRequiredSkills() != null && !subTask.getRequiredSkills().isEmpty()) {
            for (TeamMember<T> member : members) {
                if (member.getSkills() != null &&
                    member.getSkills().containsAll(subTask.getRequiredSkills())) {
                    return member;
                }
            }
        }

        // 根据指定角色匹配
        if (subTask.getAssignedRole() != null) {
            for (TeamMember<T> member : members) {
                if (member.getRole() == subTask.getAssignedRole()) {
                    return member;
                }
            }
        }

        // 轮询分配
        int index = subTasks.indexOf(subTask) % members.size();
        return members.get(Math.max(0, index));
    }

    // 子任务列表缓存（用于轮询）
    private List<SubTask> subTasks = new ArrayList<>();

    // ============= 投票决策功能 =============

    /**
     * 设置投票策略
     */
    public void setVotingStrategy(VotingStrategy strategy) {
        this.votingStrategy = strategy;
    }

    /**
     * 发起团队投票
     */
    public VoteResult vote(String topic, List<String> options) {
        log.info("发起投票: {} - 选项: {}", topic, options);

        Map<String, Integer> votes = new ConcurrentHashMap<>();
        options.forEach(opt -> votes.put(opt, 0));

        Map<String, String> memberVotes = new ConcurrentHashMap<>();

        // 收集每个成员的投票
        for (TeamMember<T> member : members) {
            try {
                String choice = member.vote(topic, options);
                if (choice != null && votes.containsKey(choice)) {
                    votes.merge(choice, 1, Integer::sum);
                    memberVotes.put(member.getMemberId(), choice);
                }
            } catch (Exception e) {
                log.warn("成员 {} 投票失败: {}", member.getMemberId(), e.getMessage());
            }
        }

        // 根据策略决定结果
        String winner = determineVoteWinner(votes, memberVotes);

        return VoteResult.builder()
            .topic(topic)
            .options(options)
            .votes(votes)
            .memberVotes(memberVotes)
            .winner(winner)
            .strategy(votingStrategy)
            .totalVotes(memberVotes.size())
            .build();
    }

    /**
     * 根据投票策略决定获胜者
     */
    private String determineVoteWinner(Map<String, Integer> votes, Map<String, String> memberVotes) {
        if (votes.isEmpty()) return null;

        return switch (votingStrategy) {
            case MAJORITY -> {
                // 简单多数
                yield votes.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);
            }
            case SUPERMAJORITY -> {
                // 绝对多数 (>2/3)
                int threshold = (int) Math.ceil(members.size() * 2.0 / 3);
                yield votes.entrySet().stream()
                    .filter(e -> e.getValue() >= threshold)
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);
            }
            case UNANIMITY -> {
                // 全票通过
                yield votes.entrySet().stream()
                    .filter(e -> e.getValue() == members.size())
                    .findFirst()
                    .map(Map.Entry::getKey)
                    .orElse(null);
            }
            case WEIGHTED -> {
                // 按角色权重投票
                Map<String, Double> weightedVotes = new HashMap<>();
                memberVotes.forEach((memberId, choice) -> {
                    TeamRole role = roleManager.getRole(memberId);
                    double weight = roleManager.getRolePriority(role) > 0 ?
                        1.0 / roleManager.getRolePriority(role) : 1.0;
                    weightedVotes.merge(choice, weight, Double::sum);
                });
                yield weightedVotes.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);
            }
        };
    }

    // ============= 消息传递功能 =============

    /**
     * 发送消息给指定成员
     */
    public void sendMessage(String fromMemberId, String toMemberId, String content) {
        BlockingQueue<TeamMessage> channel = messageChannels.get(toMemberId);
        if (channel == null) {
            throw new IllegalArgumentException("目标成员不存在: " + toMemberId);
        }

        TeamMessage message = TeamMessage.builder()
            .fromMemberId(fromMemberId)
            .toMemberId(toMemberId)
            .content(content)
            .timestamp(System.currentTimeMillis())
            .type(MessageType.DIRECT)
            .build();

        channel.offer(message);
        log.debug("消息已发送: {} -> {}", fromMemberId, toMemberId);
    }

    /**
     * 广播消息给所有成员
     */
    public void broadcast(String fromMemberId, String content) {
        TeamMessage message = TeamMessage.builder()
            .fromMemberId(fromMemberId)
            .toMemberId(null)
            .content(content)
            .timestamp(System.currentTimeMillis())
            .type(MessageType.BROADCAST)
            .build();

        messageChannels.values().forEach(channel -> channel.offer(message));
        log.debug("广播消息: {} -> 所有成员", fromMemberId);
    }

    /**
     * 获取成员的未读消息
     */
    public List<TeamMessage> getMessages(String memberId) {
        BlockingQueue<TeamMessage> channel = messageChannels.get(memberId);
        if (channel == null) {
            return Collections.emptyList();
        }

        List<TeamMessage> messages = new ArrayList<>();
        channel.drainTo(messages);
        return messages;
    }

    // ============= 上下文管理 =============

    /**
     * 设置团队上下文
     */
    public void setContext(String key, Object value) {
        teamContext.put(key, value);
    }

    /**
     * 获取团队上下文
     */
    @SuppressWarnings("unchecked")
    public <V> V getContext(String key) {
        return (V) teamContext.get(key);
    }

    /**
     * 获取所有上下文
     */
    public Map<String, Object> getAllContext() {
        return new HashMap<>(teamContext);
    }

    // ============= 进度监控 =============

    /**
     * 获取团队统计信息
     */
    public TeamStatistics getStatistics() {
        long totalDuration = executionHistory.stream()
            .mapToLong(TaskExecution::getDuration)
            .sum();

        double avgDuration = executionHistory.isEmpty() ? 0 :
            totalDuration / (double) executionHistory.size();

        Map<String, Long> memberContributions = executionHistory.stream()
            .collect(Collectors.groupingBy(TaskExecution::getMemberId, Collectors.counting()));

        return TeamStatistics.builder()
            .totalTasks(executionHistory.size())
            .totalDuration(totalDuration)
            .averageDuration(avgDuration)
            .memberCount(members.size())
            .memberContributions(memberContributions)
            .status(status)
            .build();
    }

    // ============= 内部类/枚举/接口 =============

    /**
     * 团队状态枚举
     */
    public enum TeamStatus {
        IDLE,       // 空闲
        WORKING,    // 工作中
        ERROR       // 错误
    }

    /**
     * 聚合策略接口
     */
    public interface AggregationStrategy<T> {
        T aggregate(List<T> results, List<TaskExecution<T>> executions);
    }

    /**
     * 选择策略接口
     */
    public interface SelectionStrategy<T> {
        T select(List<T> proposals, List<TaskExecution<T>> executions);
    }

    /**
     * 子任务
     */
    @Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class SubTask {
        private String id;
        private String description;
        private List<String> requiredSkills;
        private TeamRole assignedRole;
        private int priority;
    }

    /**
     * 投票策略
     */
    public enum VotingStrategy {
        MAJORITY,      // 简单多数
        SUPERMAJORITY, // 绝对多数 (>2/3)
        UNANIMITY,     // 全票通过
        WEIGHTED       // 按角色权重
    }

    /**
     * 投票结果
     */
    @Data
    @lombok.Builder
    public static class VoteResult {
        private String topic;
        private List<String> options;
        private Map<String, Integer> votes;
        private Map<String, String> memberVotes;
        private String winner;
        private VotingStrategy strategy;
        private int totalVotes;
    }

    /**
     * 团队消息
     */
    @Data
    @lombok.Builder
    public static class TeamMessage {
        private String fromMemberId;
        private String toMemberId;
        private String content;
        private long timestamp;
        private MessageType type;
    }

    /**
     * 消息类型
     */
    public enum MessageType {
        DIRECT,    // 直接消息
        BROADCAST, // 广播消息
        SYSTEM     // 系统消息
    }

    /**
     * 团队统计信息
     */
    @Data
    @lombok.Builder
    public static class TeamStatistics {
        private int totalTasks;
        private long totalDuration;
        private double averageDuration;
        private int memberCount;
        private Map<String, Long> memberContributions;
        private TeamStatus status;
    }
}
