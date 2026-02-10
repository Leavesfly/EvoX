package io.leavesfly.evox.core.llm;

import io.leavesfly.evox.core.message.Message;

import java.util.List;

/**
 * LLM 同步调用接口
 * 定义语言模型最基础的同步调用能力。
 * 只需要同步调用的场景应依赖此接口，而非完整的 {@link ILLM}。
 *
 * @author EvoX Team
 */
public interface ILLMSync {

    /**
     * 同步生成文本
     *
     * @param prompt 提示词
     * @return 生成的文本
     */
    String generate(String prompt);

    /**
     * 同步对话
     *
     * @param messages 消息列表
     * @return 响应文本
     */
    String chat(List<Message> messages);

    /**
     * 获取模型名称
     *
     * @return 模型名称
     */
    String getModelName();
}
