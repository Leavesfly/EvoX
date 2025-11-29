package io.leavesfly.evox.core.agent;

import io.leavesfly.evox.core.message.Message;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Agent接口 - 智能体的核心抽象
 * 用于打破模块间的循环依赖
 * 
 * <p>该接口定义了智能体的核心能力，允许workflow模块依赖接口而非具体实现</p>
 */
public interface IAgent {
    
    /**
     * 获取智能体ID
     * 
     * @return 智能体唯一标识
     */
    String getAgentId();
    
    /**
     * 获取智能体名称
     * 
     * @return 智能体名称
     */
    String getName();
    
    /**
     * 获取智能体描述
     * 
     * @return 智能体描述信息
     */
    String getDescription();
    
    /**
     * 执行指定动作
     * 
     * @param actionName 动作名称
     * @param messages 消息列表
     * @return 执行结果消息
     */
    Message execute(String actionName, List<Message> messages);
    
    /**
     * 异步执行指定动作
     * 
     * @param actionName 动作名称
     * @param messages 消息列表
     * @return 执行结果消息(Mono)
     */
    Mono<Message> executeAsync(String actionName, List<Message> messages);
    
    /**
     * 检查是否为人类用户
     * 
     * @return true 如果是人类用户
     */
    boolean isHuman();
}
