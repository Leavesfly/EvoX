package io.leavesfly.evox.actions.base;

import io.leavesfly.evox.core.module.BaseModule;
import io.leavesfly.evox.models.base.BaseLLM;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Action基类
 * 所有Action的基础抽象类
 *
 * @author EvoX Team
 */
@Slf4j
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public abstract class Action extends BaseModule {

    /**
     * 动作名称
     */
    private String name;

    /**
     * 动作描述
     */
    private String description;

    /**
     * 语言模型实例
     */
    private transient BaseLLM llm;

    /**
     * 同步执行动作
     *
     * @param input 输入参数
     * @return 输出结果
     */
    public abstract ActionOutput execute(ActionInput input);

    /**
     * 异步执行动作
     *
     * @param input 输入参数
     * @return 输出结果(Mono)
     */
    public Mono<ActionOutput> executeAsync(ActionInput input) {
        return Mono.fromCallable(() -> execute(input));
    }

    /**
     * 获取输入字段名称列表
     *
     * @return 字段名称列表
     */
    public abstract String[] getInputFields();

    /**
     * 获取输出字段名称列表
     *
     * @return 字段名称列表
     */
    public abstract String[] getOutputFields();
}
