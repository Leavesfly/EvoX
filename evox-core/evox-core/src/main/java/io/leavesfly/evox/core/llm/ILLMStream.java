package io.leavesfly.evox.core.llm;

import io.leavesfly.evox.core.message.Message;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * LLM 流式调用接口
 * 定义流式生成能力，用于需要逐 token 输出的场景。
 *
 * @author EvoX Team
 */
public interface ILLMStream {

    /**
     * 流式生成文本
     *
     * @param prompt 提示词
     * @return 文本流(Flux)
     */
    Flux<String> generateStream(String prompt);

    /**
     * 流式对话
     *
     * @param messages 消息列表
     * @return 响应文本流(Flux)
     */
    Flux<String> chatStream(List<Message> messages);
}
