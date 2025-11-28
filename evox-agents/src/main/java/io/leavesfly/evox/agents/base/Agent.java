package io.leavesfly.evox.agents.base;

import io.leavesfly.evox.actions.base.Action;
import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.core.module.BaseModule;
import io.leavesfly.evox.models.base.BaseLLM;
import io.leavesfly.evox.models.config.LLMConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Agent基类
 * 所有智能体的基础类
 *
 * @author EvoX Team
 */
@Data
@Slf4j
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public abstract class Agent extends BaseModule {

    /**
     * 智能体唯一标识
     */
    private String agentId = UUID.randomUUID().toString();

    /**
     * 智能体名称
     */
    private String name;

    /**
     * 智能体描述
     */
    private String description;

    /**
     * LLM配置
     */
    private LLMConfig llmConfig;

    /**
     * LLM实例
     */
    private transient BaseLLM llm;

    /**
     * 系统提示词
     */
    private String systemPrompt;

    /**
     * 可用动作列表
     */
    private List<Action> actions = new ArrayList<>();

    /**
     * 动作映射表
     */
    private transient Map<String, Action> actionMap = new ConcurrentHashMap<>();

    /**
     * 是否为人类用户
     */
    private boolean isHuman = false;

    /**
     * 无参构造函数
     * 兼容 Lombok SuperBuilder 和直接实例化
     */
    public Agent() {
        super();
    }

    @Override
    public void initModule() {
        super.initModule();
        // 初始化动作映射
        if (actions != null) {
            for (Action action : actions) {
                actionMap.put(action.getName(), action);
            }
        }
    }

    /**
     * 执行指定动作
     *
     * @param actionName 动作名称
     * @param messages 消息列表
     * @return 执行结果消息
     */
    public abstract Message execute(String actionName, List<Message> messages);

    /**
     * 异步执行指定动作
     *
     * @param actionName 动作名称
     * @param messages 消息列表
     * @return 执行结果消息(Mono)
     */
    public Mono<Message> executeAsync(String actionName, List<Message> messages) {
        return Mono.fromCallable(() -> execute(actionName, messages));
    }

    /**
     * 获取动作
     *
     * @param actionName 动作名称
     * @return 动作实例
     */
    public Action getAction(String actionName) {
        return actionMap.get(actionName);
    }

    /**
     * 添加动作
     *
     * @param action 动作实例
     */
    public void addAction(Action action) {
        if (action != null) {
            actions.add(action);
            actionMap.put(action.getName(), action);
            log.debug("Added action {} to agent {}", action.getName(), name);
        }
    }

    /**
     * 移除动作
     *
     * @param actionName 动作名称
     */
    public void removeAction(String actionName) {
        Action removed = actionMap.remove(actionName);
        if (removed != null) {
            actions.remove(removed);
            log.debug("Removed action {} from agent {}", actionName, name);
        }
    }
}
