package io.leavesfly.evox.core.llm;

import io.leavesfly.evox.core.message.Message;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * LLM 异步调用接口
 * 在同步调用基础上扩展异步能力。
 * 需要异步但不需要流式的场景应依赖此接口。
 *
 * @author EvoX Team
 */
public interface ILLMAsync extends ILLMSync {

    /**
     * 异步生成文本
     *
     * @param prompt 提示词
     * @return 生成的文本(Mono)
     */
    Mono<String> generateAsync(String prompt);

    /**
     * 异步对话
     *
     * @param messages 消息列表
     * @return 响应文本(Mono)
     */
    Mono<String> chatAsync(List<Message> messages);
}
