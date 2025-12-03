package io.leavesfly.evox.hitl;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 发送给人类进行审查/批准的HITL请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HITLRequest {
    
    /**
     * 唯一请求ID
     */
    @Builder.Default
    private String requestId = UUID.randomUUID().toString();
    
    /**
     * 交互类型
     */
    private HITLInteractionType interactionType;
    
    /**
     * 执行模式
     */
    private HITLMode mode;
    
    /**
     * 上下文信息
     */
    private HITLContext context;
    
    /**
     * 显示给用户的提示信息
     */
    private String promptMessage;
}
