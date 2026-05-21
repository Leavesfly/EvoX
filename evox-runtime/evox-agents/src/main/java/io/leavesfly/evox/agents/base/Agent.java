package io.leavesfly.evox.agents.base;

import io.leavesfly.evox.core.agent.IAgent;
import io.leavesfly.evox.core.llm.ILLM;
import io.leavesfly.evox.core.llm.LLMConfig;
import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.core.message.MessageType;
import io.leavesfly.evox.core.module.BaseModule;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.*;

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
    private transient ILLM llm;

    /**
     * 系统提示词
     */
    private String systemPrompt;

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
     * 获取 LLM 实例
     *
     * <p>LLM 实例应通过构造函数、Builder 或 setter 注入。
     * 如需从 LLMConfig 自动创建，请使用 AgentBuilder 的 withConfig() 方法，
     * 或在应用层通过 LLMFactory 创建后注入。</p>
     *
     * @return LLM 实例，未设置时返回 null
     */
    public ILLM getLlm() {
        return llm;
    }

    @Override
    public void initModule() {
        // P0: 必填校验 — 快速失败
        validateRequiredFields();
        
        super.initModule();
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
     * 执行
     *
     * @param messages 消息列表
     * @return 执行结果消息
     */
    public abstract Message execute(List<Message> messages);

    /**
     * 异步执行
     *
     * <p>默认实现将同步 {@link #execute} 包装为 {@link Mono}。
     * 如果子类的底层 LLM 支持原生异步（如 {@code chatAsync}），
     * 应覆写此方法以获得真正的非阻塞执行。</p>
     *
     * @param messages 消息列表
     * @return 执行结果消息(Mono)
     */
    public Mono<Message> executeAsync(List<Message> messages) {
        return Mono.fromCallable(() -> execute(messages))
                .onErrorResume(e -> {
                    log.error("Async execution failed for agent {}: {}", name, e.getMessage(), e);
                    return Mono.just(Message.builder()
                            .messageType(MessageType.ERROR)
                            .content("Async execution failed: " + e.getMessage())
                            .build());
                });
    }

    /**
     * 便捷方法：字符串输入执行
     *
     * @param input 输入字符串
     * @return 执行结果消息
     */
    public Message call(String input) {
        List<Message> messages = List.of(Message.builder()
                .messageType(MessageType.INPUT)
                .content(input)
                .build());
        return execute(messages);
    }

    /**
     * 便捷方法：Map 输入执行
     *
     * @param inputs 输入参数
     * @return 执行结果消息
     */
    public Message call(Map<String, Object> inputs) {
        List<Message> messages = List.of(Message.builder()
                .messageType(MessageType.INPUT)
                .content(inputs)
                .build());
        return execute(messages);
    }

    /**
     * 异步便捷方法：字符串输入
     *
     * @param input 输入字符串
     * @return 执行结果消息(Mono)
     */
    public Mono<Message> callAsync(String input) {
        List<Message> messages = List.of(Message.builder()
                .messageType(MessageType.INPUT)
                .content(input)
                .build());
        return executeAsync(messages);
    }

    /**
     * 异步便捷方法：Map 输入
     *
     * @param inputs 输入参数
     * @return 执行结果消息(Mono)
     */
    public Mono<Message> callAsync(Map<String, Object> inputs) {
        List<Message> messages = List.of(Message.builder()
                .messageType(MessageType.INPUT)
                .content(inputs)
                .build());
        return executeAsync(messages);
    }

}
