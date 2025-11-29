package io.leavesfly.evox.core.agent;

import java.util.Map;

/**
 * AgentManager接口 - 智能体管理器的核心抽象
 * 用于打破模块间的循环依赖
 * 
 * <p>该接口定义了智能体管理的核心能力，允许workflow模块依赖接口而非具体实现</p>
 */
public interface IAgentManager {
    
    /**
     * 根据名称获取智能体
     * 
     * @param name 智能体名称
     * @return 智能体实例，如果不存在则返回null
     */
    IAgent getAgent(String name);
    
    /**
     * 根据ID获取智能体
     * 
     * @param agentId 智能体ID
     * @return 智能体实例，如果不存在则返回null
     */
    IAgent getAgentById(String agentId);
    
    /**
     * 添加智能体
     * 
     * @param agent 智能体实例
     */
    void addAgent(IAgent agent);
    
    /**
     * 移除智能体
     * 
     * @param name 智能体名称
     * @return 被移除的智能体实例，如果不存在则返回null
     */
    IAgent removeAgent(String name);
    
    /**
     * 检查是否存在指定名称的智能体
     * 
     * @param name 智能体名称
     * @return true 如果存在
     */
    boolean hasAgent(String name);
    
    /**
     * 获取所有智能体
     * 
     * @return 智能体名称到实例的映射
     */
    Map<String, IAgent> getAllAgents();
    
    /**
     * 获取智能体数量
     * 
     * @return 智能体数量
     */
    int getAgentCount();
    
    /**
     * 清空所有智能体
     */
    void clear();
}
