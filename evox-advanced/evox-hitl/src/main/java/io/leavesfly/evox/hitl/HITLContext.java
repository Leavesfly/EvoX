package io.leavesfly.evox.hitl;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * HITL context information containing execution details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HITLContext {
    
    /**
     * Task name
     */
    private String taskName;
    
    /**
     * Agent name
     */
    private String agentName;
    
    /**
     * Action name
     */
    private String actionName;
    
    /**
     * Workflow goal
     */
    private String workflowGoal;
    
    /**
     * Action inputs
     */
    @Builder.Default
    private Map<String, Object> actionInputs = new HashMap<>();
    
    /**
     * Execution result (for post-execution review)
     */
    private Object executionResult;
    
    /**
     * Additional display context
     */
    @Builder.Default
    private Map<String, Object> displayContext = new HashMap<>();
}
