package io.leavesfly.evox.hitl;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * HITL上下文信息,包含执行详情
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HITLContext {
    
    /**
     * 任务名称
     */
    private String taskName;
    
    /**
     * 智能体名称
     */
    private String agentName;
    
    /**
     * 动作名称
     */
    private String actionName;
    
    /**
     * 工作流目标
     */
    private String workflowGoal;
    
    /**
     * 动作输入参数
     */
    @Builder.Default
    private Map<String, Object> actionInputs = new HashMap<>();
    
    /**
     * 执行结果(用于执行后审查)
     */
    private Object executionResult;
    
    /**
     * 额外的显示上下文
     */
    @Builder.Default
    private Map<String, Object> displayContext = new HashMap<>();
}
