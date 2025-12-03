package io.leavesfly.evox.hitl;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 来自人类的HITL响应,包含决策和反馈
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HITLResponse {
    
    /**
     * 此响应对应的请求ID
     */
    private String requestId;
    
    /**
     * 人类决策
     */
    private HITLDecision decision;
    
    /**
     * 修改后的内容(如果决策是MODIFY)
     */
    private Object modifiedContent;
    
    /**
     * 人类反馈/评论
     */
    private String feedback;
}
