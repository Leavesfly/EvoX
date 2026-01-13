package io.leavesfly.evox.agents.manager;

import io.leavesfly.evox.agents.base.Agent;
import io.leavesfly.evox.core.agent.IAgent;
import io.leavesfly.evox.core.agent.IAgentManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 智能体管理器
 * 负责管理所有智能体的生命周期和状态
 *
 * @author EvoX Team
 */
@Slf4j
@Component
public class AgentManager implements IAgentManager {

    /**
     * 智能体映射表(按名称索引)
     */
    private final Map<String, Agent> agentsByName = new ConcurrentHashMap<>();

    /**
     * 智能体映射表(按ID索引)
     */
    private final Map<String, Agent> agentsById = new ConcurrentHashMap<>();

    /**
     * 智能体状态映射表
     */
    private final Map<String, AgentState> agentStates = new ConcurrentHashMap<>();

    /**
     * 添加智能体
     *
     * @param agent 智能体实例
     */
    @Override
    public void addAgent(IAgent agent) {
        if (agent == null) {
            log.warn("Cannot add null agent");
            return;
        }

        // 确保是Agent类型以便存储到内部映射
        if (agent instanceof Agent) {
            Agent agentImpl = (Agent) agent;
            agentsByName.put(agentImpl.getName(), agentImpl);
            agentsById.put(agentImpl.getAgentId(), agentImpl);
            agentStates.put(agentImpl.getAgentId(), AgentState.IDLE);
            
            log.info("Added agent: {} (ID: {})", agentImpl.getName(), agentImpl.getAgentId());
        } else {
            log.warn("Agent must be an instance of Agent class");
        }
    }

    /**
     * 移除智能体（按名称）
     *
     * @param name 智能体名称
     * @return 被移除的智能体
     */
    @Override
    public IAgent removeAgent(String name) {
        Agent agent = agentsByName.remove(name);
        if (agent != null) {
            agentsById.remove(agent.getAgentId());
            agentStates.remove(agent.getAgentId());
            log.info("Removed agent: {} (ID: {})", agent.getName(), agent.getAgentId());
        }
        return agent;
    }
    
    /**
     * 移除智能体（按ID）
     *
     * @param agentId 智能体ID
     */
    public void removeAgentById(String agentId) {
        Agent agent = agentsById.remove(agentId);
        if (agent != null) {
            agentsByName.remove(agent.getName());
            agentStates.remove(agentId);
            log.info("Removed agent: {} (ID: {})", agent.getName(), agentId);
        }
    }

    /**
     * 根据名称获取智能体
     *
     * @param name 智能体名称
     * @return 智能体实例
     */
    @Override
    public IAgent getAgent(String name) {
        return agentsByName.get(name);
    }

    /**
     * 根据ID获取智能体
     *
     * @param agentId 智能体ID
     * @return 智能体实例
     */
    @Override
    public IAgent getAgentById(String agentId) {
        return agentsById.get(agentId);
    }

    /**
     * 获取所有智能体
     *
     * @return 智能体映射表
     */
    @Override
    public Map<String, IAgent> getAllAgents() {
        return new HashMap<>(agentsByName);
    }
    
    /**
     * 检查是否存在指定名称的智能体
     *
     * @param name 智能体名称
     * @return true 如果存在
     */
    @Override
    public boolean hasAgent(String name) {
        return agentsByName.containsKey(name);
    }
    
    /**
     * 获取智能体数量
     *
     * @return 智能体数量
     */
    @Override
    public int getAgentCount() {
        return agentsByName.size();
    }

    /**
     * 更新智能体状态
     *
     * @param agentId 智能体ID
     * @param state 新状态
     */
    public void updateAgentState(String agentId, AgentState state) {
        if (agentsById.containsKey(agentId)) {
            agentStates.put(agentId, state);
            log.debug("Updated agent {} state to {}", agentId, state);
        }
    }

    /**
     * 获取智能体状态
     *
     * @param agentId 智能体ID
     * @return 状态
     */
    public AgentState getAgentState(String agentId) {
        return agentStates.getOrDefault(agentId, AgentState.IDLE);
    }

    /**
     * 清空所有智能体
     */
    @Override
    public void clear() {
        agentsByName.clear();
        agentsById.clear();
        agentStates.clear();
        log.info("Cleared all agents");
    }

    /**
     * 获取智能体数量（别名方法）
     */
    public int size() {
        return getAgentCount();
    }
}
