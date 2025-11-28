package io.leavesfly.evox.agents.manager;

import io.leavesfly.evox.agents.base.Agent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
public class AgentManager {

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
    public void addAgent(Agent agent) {
        if (agent == null) {
            log.warn("Cannot add null agent");
            return;
        }

        agentsByName.put(agent.getName(), agent);
        agentsById.put(agent.getAgentId(), agent);
        agentStates.put(agent.getAgentId(), AgentState.IDLE);
        
        log.info("Added agent: {} (ID: {})", agent.getName(), agent.getAgentId());
    }

    /**
     * 移除智能体
     *
     * @param agentId 智能体ID
     */
    public void removeAgent(String agentId) {
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
    public Agent getAgent(String name) {
        return agentsByName.get(name);
    }

    /**
     * 根据ID获取智能体
     *
     * @param agentId 智能体ID
     * @return 智能体实例
     */
    public Agent getAgentById(String agentId) {
        return agentsById.get(agentId);
    }

    /**
     * 获取所有智能体
     *
     * @return 智能体映射表
     */
    public Map<String, Agent> getAllAgents() {
        return new ConcurrentHashMap<>(agentsByName);
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
    public void clear() {
        agentsByName.clear();
        agentsById.clear();
        agentStates.clear();
        log.info("Cleared all agents");
    }

    /**
     * 获取智能体数量
     */
    public int size() {
        return agentsByName.size();
    }
}
