package io.leavesfly.evox.agents.base;

import io.leavesfly.evox.actions.base.Action;
import io.leavesfly.evox.core.agent.IAgent;
import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.core.message.MessageType;
import io.leavesfly.evox.core.module.BaseModule;
import io.leavesfly.evox.models.base.BaseLLM;
import io.leavesfly.evox.models.config.LLMConfig;
import io.leavesfly.evox.models.factory.LLMFactory;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
public abstract class Agent extends BaseModule implements IAgent {

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

    /**
     * 获取 LLM 实例（支持懒初始化）
     *
     * <p>如果 llm 未直接设置但 llmConfig 已配置，
     * 会自动通过 LLMFactory 创建 LLM 实例。
     * 这意味着用户只需设置 llmConfig，无需手动创建 LLM 对象。</p>
     *
     * @return LLM 实例，如果既没有设置 llm 也没有设置 llmConfig 则返回 null
     */
    public BaseLLM getLlm() {
        if (llm == null && llmConfig != null) {
            try {
                llm = LLMFactory.create(llmConfig);
                log.debug("Auto-created LLM from config: provider={}, model={}",
                        llmConfig.getProvider(), llmConfig.getModel());
            } catch (Exception e) {
                log.error("Failed to auto-create LLM from config: {}", e.getMessage(), e);
            }
        }
        return llm;
    }

    @Override
    public void initModule() {
        // P0: 必填校验 — 快速失败
        validateRequiredFields();
        
        super.initModule();
        // 初始化动作映射
        if (actions != null) {
            for (Action action : actions) {
                actionMap.put(action.getName(), action);
            }
        }
    }

    /**
     * 校验 Agent 基本必填字段
     * 子类可以覆写此方法添加额外校验
     *
     * @throws IllegalStateException 如果必填字段缺失
     */
    protected void validateRequiredFields() {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalStateException(
                    getClass().getSimpleName() + ": 'name' must be set before initModule()");
        }
    }

    /**
     * 获取主要动作名称（供 execute(null, messages) 时使用）
     * 子类应该覆写此方法返回自己的 primaryActionName
     *
     * @return 主要动作名称
     */
    protected String getPrimaryActionName() {
        return null;
    }

    /**
     * 执行指定动作
     *
     * @param actionName 动作名称（传 null 使用默认动作）
     * @param messages 消息列表
     * @return 执行结果消息
     */
    public abstract Message execute(String actionName, List<Message> messages);

    /**
     * 异步执行指定动作
     *
     * <p>默认实现将同步 {@link #execute} 包装为 {@link Mono}。
     * 如果子类的底层 LLM 支持原生异步（如 {@code chatAsync}），
     * 应覆写此方法以获得真正的非阻塞执行。</p>
     *
     * @param actionName 动作名称
     * @param messages 消息列表
     * @return 执行结果消息(Mono)
     */
    public Mono<Message> executeAsync(String actionName, List<Message> messages) {
        return Mono.fromCallable(() -> execute(actionName, messages))
                .onErrorResume(e -> {
                    log.error("Async execution failed for agent {}: {}", name, e.getMessage(), e);
                    return Mono.just(Message.builder()
                            .messageType(MessageType.ERROR)
                            .content("Async execution failed: " + e.getMessage())
                            .build());
                });
    }

    /**
     * 异步执行（使用默认动作，便捷方法）
     *
     * @param input 输入字符串
     * @return 执行结果消息(Mono)
     */
    public Mono<Message> callAsync(String input) {
        List<Message> messages = List.of(Message.builder()
                .messageType(MessageType.INPUT)
                .content(input)
                .build());
        return executeAsync(getPrimaryActionName(), messages);
    }

    /**
     * 异步执行（使用默认动作，Map 输入）
     *
     * @param inputs 输入参数
     * @return 执行结果消息(Mono)
     */
    public Mono<Message> callAsync(Map<String, Object> inputs) {
        List<Message> messages = List.of(Message.builder()
                .messageType(MessageType.INPUT)
                .content(inputs)
                .build());
        return executeAsync(getPrimaryActionName(), messages);
    }

    /**
     * 使用默认动作执行（Map 输入，统一调用入口）
     *
     * <p>所有 Agent 子类均可通过此方法调用，无需知道内部 actionName</p>
     *
     * @param inputs 输入参数
     * @return 执行结果消息
     */
    public Message call(Map<String, Object> inputs) {
        List<Message> messages = List.of(Message.builder()
                .messageType(MessageType.INPUT)
                .content(inputs)
                .build());
        return execute(getPrimaryActionName(), messages);
    }

    /**
     * 使用默认动作执行（字符串输入，便捷方法）
     *
     * @param input 输入字符串
     * @return 执行结果消息
     */
    public Message call(String input) {
        List<Message> messages = List.of(Message.builder()
                .messageType(MessageType.INPUT)
                .content(input)
                .build());
        return execute(getPrimaryActionName(), messages);
    }

    /**
     * 解析 actionName，如果为 null 则使用 primaryActionName
     *
     * @param actionName 传入的 actionName
     * @return 实际使用的 actionName
     */
    protected String resolveActionName(String actionName) {
        if (actionName != null) {
            return actionName;
        }
        String primary = getPrimaryActionName();
        return primary != null ? primary : "default";
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
