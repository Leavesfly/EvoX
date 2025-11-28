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
 * HITL User Input Collector Agent for collecting user input during workflow execution.
 */
@Slf4j
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class HITLUserInputCollectorAgent extends Agent {

    /**
     * Field definitions for input collection
     */
    private Map<String, FieldDefinition> fieldDefinitions;

    /**
     * HITL manager reference
     */
    private transient HITLManager hitlManager;

    /**
     * Collect user input based on field definitions.
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
     * Field definition for user input.
     */
    @Data
    public static class FieldDefinition {
        private String name;
        private String type; // string, int, boolean, etc.
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
