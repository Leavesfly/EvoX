package io.leavesfly.evox.hitl;

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
 * HITL用户输入收集器智能体,用于在工作流执行期间收集用户输入
 */
@Slf4j
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class HITLUserInputCollectorAgent extends Agent {

    /**
     * 输入收集的字段定义
     */
    private Map<String, FieldDefinition> fieldDefinitions;

    /**
     * HITL管理器引用
     */
    private transient HITLManager hitlManager;

    /**
     * 根据字段定义收集用户输入
     */
    public Mono<Map<String, Object>> collectInput(String taskName, String workflowGoal) {
        if (hitlManager == null) {
            log.warn("HITL Manager not set, returning empty input");
            return Mono.just(new HashMap<>());
        }

        log.info("Collecting user input for task: {}", taskName);

        Map<String, Object> actionInputs = new HashMap<>();
        actionInputs.put("field_definitions", fieldDefinitions);

        return hitlManager.requestApproval(
                taskName,
                this.getName(),
                "CollectUserInput",
                HITLInteractionType.COLLECT_USER_INPUT,
                HITLMode.PRE_EXECUTION,
                actionInputs,
                null,
                workflowGoal,
                new HashMap<>()
        ).map(response -> {
            if (response.getDecision() == HITLDecision.CONTINUE && response.getModifiedContent() != null) {
                return (Map<String, Object>) response.getModifiedContent();
            }
            return new HashMap<>();
        });
    }

    /**
     * 用户输入的字段定义
     */
    @Data
    public static class FieldDefinition {
        private String name;
        private String type; // string, int, boolean, 等
        private String description;
        private boolean required;
        private Object defaultValue;
    }

    /**
     * 实现 Agent 的抽象方法 execute
     */
    @Override
    public Message execute(String actionName, List<Message> messages) {
        log.warn("HITLUserInputCollectorAgent.execute called, this agent is for user input collection");
        return Message.responseMessage(
                "User input collection completed",
                this.getName(),
                actionName
        );
    }
}
