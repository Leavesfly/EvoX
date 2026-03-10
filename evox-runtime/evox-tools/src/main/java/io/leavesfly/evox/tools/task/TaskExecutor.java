package io.leavesfly.evox.tools.task;

/**
 * 任务执行器接口。
 *
 * 定义委派任务的标准执行契约，实现类负责具体逻辑，如调用 LLM、访问外部 API 或复杂数据处理。
 *
 * 设计原则：
 * - 函数式接口：支持 Lambda 表达式
 * - 无状态：执行器应无状态或线程安全
 * - 异常透明：异常向上抛出，由调用方处理
 *
 * 使用示例：
 * {@code TaskExecutor executor = (desc, prompt) -> "结果: " + prompt;}
 * {@code TaskExecutor llmExecutor = MyLLMService::processTask;}
 *
 * 实现建议：
 * - 长时间任务可添加进度日志
 * - 调用外部服务时建议添加重试与错误处理
 * - 返回结构化、易解析的字符串
 * - 失败时可抛出异常或返回包含错误信息的结果
 *
 * 线程安全：实现应为线程安全，因 {@link TaskDelegationTool#executeBatch} 可能并发调用同一实例。
 *
 * @see TaskDelegationTool
 */
@FunctionalInterface
public interface TaskExecutor {

    /**
     * 执行委派任务。
     *
     * 接收任务描述与详细指令，执行处理逻辑并返回结果。
     *
     * @param taskDescription 任务简短描述，用于日志与追踪
     * @param taskPrompt      任务详细指令，含执行要求与期望输出
     * @return 任务执行结果字符串
     * @throws Exception 执行失败时抛出，由 {@link TaskDelegationTool} 捕获并转为 ToolResult
     */
    String execute(String taskDescription, String taskPrompt) throws Exception;
}
