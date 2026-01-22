package io.leavesfly.evox.frameworks.team;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 团队协作框架
 * 支持多种协作模式的团队管理框架
 *
 * @param <T> 任务结果类型
 * @author EvoX Team
 */
@Slf4j
@Data
public class TeamFramework<T> {

    /**
     * 团队成员
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
     * 消息通道 (memberId -> 消息队列)
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
     * 全局上下文
     */
    private final Map<String, Object> teamContext = new ConcurrentHashMap<>();

    public TeamFramework(List<TeamMember<T>> members, CollaborationMode mode, TeamConfig config) {
        this.members = members;
        this.mode = mode;
        this.config = config;
        this.roleManager = new RoleManager();
        this.taskQueue = new ConcurrentLinkedQueue<>();
        this.executionHistory = new ArrayList<>();
        this.status = TeamStatus.IDLE;
        
        // 注册成员角色和消息通道
        members.forEach(member -> {
            roleManager.assignRole(member.getMemberId(), member.getRole());
            messageChannels.put(member.getMemberId(), new LinkedBlockingQueue<>());
        });
    }

    public TeamFramework(List<TeamMember<T>> members, CollaborationMode mode) {
        this(members, mode, TeamConfig.builder().build());
    }

    /**
     * 执行团队任务
     *
     * @param task 任务
     * @return 团队执行结果
     */
    public TeamResult<T> executeTeamTask(String task) {
        log.info("Team starting task: {} with mode: {}", task, mode);
        
        long startTime = System.currentTimeMillis();
        status = TeamStatus.WORKING;
        
        try {
            TeamResult<T> result = switch (mode) {
                case PARALLEL -> executeParallel(task);
                case SEQUENTIAL -> executeSequential(task);
                case HIERARCHICAL -> executeHierarchical(task);
                case COLLABORATIVE -> executeCollaborative(task);
                case COMPETITIVE -> executeCompetitive(task);
            };
            
            status = TeamStatus.IDLE;
            result.setDuration(System.currentTimeMillis() - startTime);
            
            log.info("Team task completed in {}ms", result.getDuration());
            return result;
            
        } catch (Exception e) {
            status = TeamStatus.ERROR;
            log.error("Team task failed: {}", e.getMessage(), e);
            
            return TeamResult.<T>builder()
                .success(false)
                .error(e.getMessage())
                .duration(System.currentTimeMillis() - startTime)
                .build();
        }
    }

    /**
     * 并行协作模式:所有成员同时执行任务
     */
    private TeamResult<T> executeParallel(String task) {
        log.debug("Executing in PARALLEL mode");
        
        if (config.isEnableThreadPool()) {
            return executeParallelWithThreadPool(task);
        }
        
        // 同步执行
        List<T> results = new ArrayList<>();
        List<TaskExecution<T>> executions = new ArrayList<>();
        
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
        
        T finalResult = aggregateResults(results, executions);
        
        return TeamResult.<T>builder()
            .success(true)
            .result(finalResult)
            .contributions(executions)
            .participantCount(members.size())
            .metadata(buildMetadata("parallel"))
            .build();
    }

    /**
     * 使用线程池并行执行
     */
    private TeamResult<T> executeParallelWithThreadPool(String task) {
        ExecutorService executor = Executors.newFixedThreadPool(
            Math.min(members.size(), config.getMaxThreads())
        );
        
        List<Future<TaskExecution<T>>> futures = new ArrayList<>();
        
        for (TeamMember<T> member : members) {
            futures.add(executor.submit(() -> {
                long memberStart = System.currentTimeMillis();
                T result = member.execute(task, null, executionHistory);
                long memberDuration = System.currentTimeMillis() - memberStart;
                
                return new TaskExecution<>(
                    member.getMemberId(),
                    task,
                    result,
                    memberDuration,
                    System.currentTimeMillis()
                );
            }));
        }
        
        List<TaskExecution<T>> executions = new ArrayList<>();
        List<T> results = new ArrayList<>();
        
        try {
            for (Future<TaskExecution<T>> future : futures) {
                TaskExecution<T> execution = future.get(config.getTaskTimeout(), TimeUnit.MILLISECONDS);
                executions.add(execution);
                results.add(execution.getResult());
                executionHistory.add(execution);
            }
        } catch (Exception e) {
            log.error("Parallel execution failed: {}", e.getMessage());
        } finally {
            executor.shutdown();
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
        TeamFramework<T> tempFramework = new TeamFramework<>(sortedMembers, CollaborationMode.SEQUENTIAL, config);
        return tempFramework.executeSequential(task);
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
            return ((TeamFramework.AggregationStrategy<T>) config.getAggregationStrategy()).aggregate(results, executions);
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
            return ((TeamFramework.SelectionStrategy<T>) config.getSelectionStrategy()).select(proposals, executions);
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

    // ============= 内部数据类 =============

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
