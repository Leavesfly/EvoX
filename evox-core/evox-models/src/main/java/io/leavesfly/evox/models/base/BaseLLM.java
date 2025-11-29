package io.leavesfly.evox.models.base;

import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.models.config.LLMConfig;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * LLM基础接口
 * 定义所有LLM实现必须提供的方法
 *
 * @author EvoX Team
 */
public interface BaseLLM {

    /**
     * 同步生成文本
     *
     * @param prompt 提示词
     * @return 生成的文本
     */
    String generate(String prompt);

    /**
     * 异步生成文本
     *
     * @param prompt 提示词
     * @return 生成的文本(Mono)
     */
    Mono<String> generateAsync(String prompt);

    /**
     * 流式生成文本
     *
     * @param prompt 提示词
     * @return 文本流(Flux)
     */
    Flux<String> generateStream(String prompt);

    /**
     * 同步对话
     *
     * @param messages 消息列表
     * @return 响应文本
     */
    String chat(List<Message> messages);

    /**
     * 异步对话
     *
     * @param messages 消息列表
     * @return 响应文本(Mono)
     */
    Mono<String> chatAsync(List<Message> messages);

    /**
     * 流式对话
     *
     * @param messages 消息列表
     * @return 响应文本流(Flux)
     */
    Flux<String> chatStream(List<Message> messages);

    /**
     * 获取模型名称
     *
     * @return 模型名称
     */
    String getModelName();

    /**
     * 获取配置
     *
     * @return LLM配置
     */
    LLMConfig getConfig();
}
