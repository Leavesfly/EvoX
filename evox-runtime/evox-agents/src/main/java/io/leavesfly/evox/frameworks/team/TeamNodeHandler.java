package io.leavesfly.evox.frameworks.team;

import io.leavesfly.evox.workflow.base.WorkflowNode;
import io.leavesfly.evox.workflow.execution.WorkflowContext;
import io.leavesfly.evox.workflow.node.NodeHandler;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 团队协作节点处理器
 * 原子化 Handler，每个 Handler 只负责单步原子操作
 *
 * @author EvoX Team
 */
@Slf4j
public class TeamNodeHandler {

    private static final String MEMBERS_KEY = "team_members";
    private static final String CONFIG_KEY = "team_config";
    private static final String EXECUTION_HISTORY_KEY = "execution_history";
    private static final String TASK_KEY = "task";
    private static final String MEMBER_RESULTS_KEY = "member_results";
    private static final String PREVIOUS_RESULT_KEY = "previous_result";
    private static final String COLLABORATION_CONVERGED_KEY = "collaboration_converged";
    private static final String ROUND_COUNT_KEY = "round_count";
    private static final String ROUND_RESULTS_KEY = "round_results";
    private static final String MODE_KEY = "mode";

    /**
     * 成员执行 Handler：执行单个成员的任务
     */
    @Slf4j
    public static class MemberExecuteHandler implements NodeHandler {

        @Override
        public Mono<Object> handle(WorkflowContext context, WorkflowNode node) {
            return Mono.fromCallable(() -> {
                log.debug("MemberExecuteHandler executing...");

                String task = (String) context.getExecutionData(TASK_KEY);
                @SuppressWarnings("unchecked")
                List<TeamMember<Object>> members = (List<TeamMember<Object>>) context.getExecutionData(MEMBERS_KEY);
                @SuppressWarnings("unchecked")
                List<TaskExecution<Object>> executionHistory = (List<TaskExecution<Object>>) context.getExecutionData(EXECUTION_HISTORY_KEY);
                
                Map<String, Object> handlerConfig = node.getHandlerConfig();
                int memberIndex = (Integer) handlerConfig.get("memberIndex");

                Object previousResult = context.getExecutionData(PREVIOUS_RESULT_KEY);

                String memberTask = handlerConfig.containsKey("memberTask") ? 
                    (String) handlerConfig.get("memberTask") : task;

                TeamMember<Object> member = members.get(memberIndex);
                long memberStart = System.currentTimeMillis();
                Object result = member.execute(memberTask, previousResult, executionHistory);
                long memberDuration = System.currentTimeMillis() - memberStart;

                TaskExecution<Object> execution = new TaskExecution<>(
                    member.getMemberId(),
                    memberTask,
                    result,
                    memberDuration,
                    System.currentTimeMillis()
                );
                executionHistory.add(execution);

                @SuppressWarnings("unchecked")
                List<Object> memberResults = (List<Object>) context.getExecutionData(MEMBER_RESULTS_KEY);
                memberResults.add(result);

                context.updateExecutionData(PREVIOUS_RESULT_KEY, result);
                context.updateExecutionData(EXECUTION_HISTORY_KEY, executionHistory);

                log.debug("Member {} executed successfully", member.getMemberId());
                return result;
            });
        }

        @Override
        public String getHandlerName() {
            return "MemberExecuteHandler";
        }
    }

    /**
     * 团队聚合 Handler：聚合所有成员的结果，构建 TeamResult
     */
    @Slf4j
    public static class TeamAggregateHandler implements NodeHandler {

        @Override
        public Mono<Object> handle(WorkflowContext context, WorkflowNode node) {
            return Mono.fromCallable(() -> {
                log.debug("TeamAggregateHandler executing...");

                @SuppressWarnings("unchecked")
                List<TeamMember<Object>> members = (List<TeamMember<Object>>) context.getExecutionData(MEMBERS_KEY);
                @SuppressWarnings("unchecked")
                List<TaskExecution<Object>> executionHistory = (List<TaskExecution<Object>>) context.getExecutionData(EXECUTION_HISTORY_KEY);
                @SuppressWarnings("unchecked")
                TeamConfig config = (TeamConfig) context.getExecutionData(CONFIG_KEY);
                String mode = (String) context.getExecutionData(MODE_KEY);

                @SuppressWarnings("unchecked")
                List<Object> memberResults = (List<Object>) context.getExecutionData(MEMBER_RESULTS_KEY);

                Object finalResult;
                if ("COMPETITIVE".equals(mode)) {
                    finalResult = selectBestResult(memberResults, executionHistory, config);
                } else {
                    finalResult = aggregateResults(memberResults, executionHistory, config);
                }

                TeamResult<Object> teamResult = TeamResult.builder()
                    .success(true)
                    .result(finalResult)
                    .contributions(executionHistory)
                    .participantCount(members.size())
                    .metadata(buildMetadata(mode, members, executionHistory))
                    .build();

                log.debug("Team aggregated successfully");
                return teamResult;
            });
        }

        @Override
        public String getHandlerName() {
            return "TeamAggregateHandler";
        }

        @SuppressWarnings("unchecked")
        private Object aggregateResults(List<Object> results, List<TaskExecution<Object>> executions, TeamConfig config) {
            if (config.getAggregationStrategy() != null) {
                return ((TeamFramework.AggregationStrategy<Object>) config.getAggregationStrategy())
                    .aggregate(results, executions);
            }
            return results.stream().filter(Objects::nonNull).findFirst().orElse(null);
        }

        @SuppressWarnings("unchecked")
        private Object selectBestResult(List<Object> proposals, List<TaskExecution<Object>> executions, TeamConfig config) {
            if (config.getSelectionStrategy() != null) {
                return ((TeamFramework.SelectionStrategy<Object>) config.getSelectionStrategy())
                    .select(proposals, executions);
            }
            return proposals.isEmpty() ? null : proposals.get(0);
        }

        private Map<String, Object> buildMetadata(String modeUsed, List<TeamMember<Object>> members, 
                List<TaskExecution<Object>> executionHistory) {
            return Map.of(
                "mode", modeUsed,
                "teamSize", members.size(),
                "totalExecutions", executionHistory.size()
            );
        }
    }

    /**
     * 协作轮次 Handler：执行一轮协作（所有成员执行+讨论）
     */
    @Slf4j
    public static class TeamCollaborativeRoundHandler implements NodeHandler {

        @Override
        public Mono<Object> handle(WorkflowContext context, WorkflowNode node) {
            return Mono.fromCallable(() -> {
                log.debug("TeamCollaborativeRoundHandler executing...");

                String task = (String) context.getExecutionData(TASK_KEY);
                @SuppressWarnings("unchecked")
                List<TeamMember<Object>> members = (List<TeamMember<Object>>) context.getExecutionData(MEMBERS_KEY);
                @SuppressWarnings("unchecked")
                List<TaskExecution<Object>> executionHistory = (List<TaskExecution<Object>>) context.getExecutionData(EXECUTION_HISTORY_KEY);
                @SuppressWarnings("unchecked")
                TeamConfig config = (TeamConfig) context.getExecutionData(CONFIG_KEY);

                @SuppressWarnings("unchecked")
                List<Object> roundResults = (List<Object>) context.getExecutionData(ROUND_RESULTS_KEY);
                roundResults.clear();

                for (TeamMember<Object> member : members) {
                    long memberStart = System.currentTimeMillis();
                    Object result = member.execute(task, null, executionHistory);
                    long memberDuration = System.currentTimeMillis() - memberStart;

                    roundResults.add(result);
                    TaskExecution<Object> execution = new TaskExecution<>(
                        member.getMemberId(),
                        task,
                        result,
                        memberDuration,
                        System.currentTimeMillis()
                    );
                    executionHistory.add(execution);
                }

                Object discussionResult = collaborativeDiscuss(roundResults, executionHistory, config);

                @SuppressWarnings("unchecked")
                List<Object> memberResults = (List<Object>) context.getExecutionData(MEMBER_RESULTS_KEY);
                memberResults.add(discussionResult);

                int roundCount = (Integer) context.getExecutionData(ROUND_COUNT_KEY);
                int maxRounds = 3;
                if (roundCount >= maxRounds) {
                    context.updateExecutionData(COLLABORATION_CONVERGED_KEY, true);
                } else {
                    roundCount++;
                    context.updateExecutionData(ROUND_COUNT_KEY, roundCount);
                }

                context.updateExecutionData(EXECUTION_HISTORY_KEY, executionHistory);
                context.updateExecutionData(TASK_KEY, discussionResult.toString());

                log.debug("Collaborative round {} completed", roundCount);
                return discussionResult;
            });
        }

        @Override
        public String getHandlerName() {
            return "TeamCollaborativeRoundHandler";
        }

        @SuppressWarnings("unchecked")
        private Object collaborativeDiscuss(List<Object> proposals, List<TaskExecution<Object>> executions, TeamConfig config) {
            if (config.getAggregationStrategy() != null) {
                return ((TeamFramework.AggregationStrategy<Object>) config.getAggregationStrategy())
                    .aggregate(proposals, executions);
            }
            return proposals.stream().filter(Objects::nonNull).findFirst().orElse(null);
        }
    }
}