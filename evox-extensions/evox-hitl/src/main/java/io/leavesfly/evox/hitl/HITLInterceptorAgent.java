package io.leavesfly.evox.hitl;

import io.leavesfly.evox.actions.base.Action;
import io.leavesfly.evox.actions.base.ActionInput;
import io.leavesfly.evox.actions.base.ActionOutput;
import io.leavesfly.evox.agents.base.Agent;
import io.leavesfly.evox.core.message.Message;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HITL拦截器智能体,在执行前或执行后拦截目标智能体/动作的执行
 * 以获取人类批准
 */
@Slf4j
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class HITLInterceptorAgent extends Agent {

    /**
     * 要拦截的目标智能体名称
     */
    private String targetAgentName;

    /**
     * 要拦截的目标动作名称
     */
    private String targetActionName;

    /**
     * HITL交互类型
     */
    private HITLInteractionType interactionType;

    /**
     * HITL模式(执行前/执行后)
     */
    private HITLMode mode;

    /**
     * HITL管理器引用
     */
    private transient HITLManager hitlManager;

    /**
     * 拦截并请求批准
     */
    public Mono<HITLResponse> intercept(
            String taskName,
            Map<String, Object> actionInputs,
            Object executionResult,
            String workflowGoal
    ) {
        if (hitlManager == null) {
            log.warn("HITL Manager not set, auto-approving");
            return Mono.just(HITLResponse.builder()
                    .decision(HITLDecision.APPROVE)
                    .feedback("No HITL manager configured")
                    .build());
        }

        log.info("Intercepting {} action '{}' for HITL approval",
                targetAgentName, targetActionName);

        return hitlManager.requestApproval(
                taskName,
                targetAgentName,
                targetActionName,
                interactionType,
                mode,
                actionInputs,
                executionResult,
                workflowGoal,
                new HashMap<>()
        );
    }

    /**
     * 执行拦截器智能体。
     * 这将触发HITL批准流程。
     */
    public Mono<String> executeAsync(List<Message> messages, Map<String, Object> inputs) {
        return Mono.fromCallable(() -> {
            // 从输入中提取任务信息
            String taskName = (String) inputs.getOrDefault("task_name", "unknown_task");
            String workflowGoal = (String) inputs.getOrDefault("workflow_goal", null);
            Object executionResult = inputs.get("execution_result");

            log.info("HITL Interceptor executing for task: {}", taskName);

            // 请求批准
            HITLResponse response = intercept(taskName, inputs, executionResult, workflowGoal)
                    .block();

            if (response == null) {
                log.error("HITL response is null");
                return "HITL approval failed - no response";
            }

            log.info("HITL decision: {}", response.getDecision());

            if (response.getDecision() == HITLDecision.APPROVE) {
                return "Approved: " + (response.getFeedback() != null ? response.getFeedback() : "Action approved");
            } else if (response.getDecision() == HITLDecision.REJECT) {
                return "Rejected: " + (response.getFeedback() != null ? response.getFeedback() : "Action rejected");
            } else if (response.getDecision() == HITLDecision.CONTINUE) {
                return "Continue: " + (response.getFeedback() != null ? response.getFeedback() : "Proceeding");
            } else {
                return "Modified: Action was modified by user";
            }
        });
    }

    /**
     * 获取智能体描述
     */
    public String getDescription() {
        return String.format(
                "HITL Interceptor for %s.%s (%s, %s)",
                targetAgentName,
                targetActionName,
                interactionType,
                mode
        );
    }

    /**
     * 实现 Agent 的抽象方法 execute
     */
    @Override
    public Message execute(String actionName, List<Message> messages) {
        log.info("HITL Interceptor executing for action: {}", actionName);
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("action_name", actionName);
        inputs.put("messages", messages);
        
        String result = executeAsync(messages, inputs).block();
        return Message.responseMessage(result, this.getName(), actionName);
    }

    /**
     * 创建HITL拦截器动作包装器
     */
    public static class HITLInterceptorAction extends Action {

        private final HITLInterceptorAgent interceptorAgent;

        public HITLInterceptorAction(HITLInterceptorAgent agent) {
            this.interceptorAgent = agent;
            setName("HITLInterceptorAction");
            setDescription(agent.getDescription());
        }

        @Override
        public ActionOutput execute(ActionInput input) {
            String result = interceptorAgent.executeAsync(null, input.getData()).block();
            return ActionOutput.builder()
                    .data(Map.of("result", result))
                    .build();
        }

        @Override
        public String[] getInputFields() {
            return new String[]{"task_name", "workflow_goal", "execution_result"};
        }

        @Override
        public String[] getOutputFields() {
            return new String[]{"result"};
        }
    }
}
