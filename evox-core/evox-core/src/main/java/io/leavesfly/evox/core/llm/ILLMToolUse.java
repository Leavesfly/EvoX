package io.leavesfly.evox.core.llm;

import io.leavesfly.evox.core.message.Message;

import java.util.List;
import java.util.Map;

/**
 * LLM 工具调用接口
 * 定义 Function Calling / Tool Use 能力，支持 LLM 原生工具调用协议。
 * 实现此接口的 LLM 可以在对话中声明可用工具，并由模型决定何时调用。
 *
 * @author EvoX Team
 */
public interface ILLMToolUse {

    /**
     * 带工具定义的对话调用
     * LLM 可能返回文本响应，也可能返回工具调用请求
     *
     * @param messages       消息列表
     * @param toolSchemas    工具定义列表（OpenAI function calling 格式）
     * @param toolChoice     工具选择策略："none" / "auto" / "required"
     * @return 包含文本和/或工具调用的完整结果（以 Map 形式返回，避免核心层依赖模型层类型）
     */
    default Map<String, Object> chatWithTools(List<Message> messages,
                                              List<Map<String, Object>> toolSchemas,
                                              String toolChoice) {
        throw new UnsupportedOperationException(
                "This LLM does not support tool use via generic schema. Use the typed API instead.");
    }

    /**
     * 检查当前 LLM 是否支持原生 Function Calling
     *
     * @return 是否支持
     */
    default boolean supportsToolUse() {
        return false;
    }
}
