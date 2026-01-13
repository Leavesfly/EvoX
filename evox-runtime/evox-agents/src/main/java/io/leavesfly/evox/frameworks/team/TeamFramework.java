package io.leavesfly.evox.frameworks.team;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;
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

    public TeamFramework(List<TeamMember<T>> members, CollaborationMode mode, TeamConfig config) {
        this.members = members;
        this.mode = mode;
        this.config = config;
        this.roleManager = new RoleManager();
        this.taskQueue = new ConcurrentLinkedQueue<>();
        this.executionHistory = new ArrayList<>();
        this.status = TeamStatus.IDLE;
        
        // 注册成员角色
        members.forEach(member -> roleManager.assignRole(member.getMemberId(), member.getRole()));
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
}
