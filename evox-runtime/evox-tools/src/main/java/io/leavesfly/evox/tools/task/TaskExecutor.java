package io.leavesfly.evox.tools.task;

/**
 * 任务执行器接口
 * 
 * <p>定义了如何执行一个委派任务的标准接口。实现此接口的类负责处理具体的任务执行逻辑，
 * 例如调用 LLM、访问外部 API、或执行复杂的数据处理。</p>
 * 
 * <p><b>设计原则：</b></p>
 * <ul>
 *   <li><b>函数式接口</b>: 使用 @FunctionalInterface 注解，支持 Lambda 表达式</li>
 *   <li><b>无状态</b>: 执行器应该是无状态的或线程安全的</li>
 *   <li><b>异常透明</b>: 执行过程中的异常应该向上抛出，由调用方处理</li>
 * </ul>
 * 
 * <p><b>使用示例：</b></p>
 * <pre>{@code
 * // Lambda 表达式实现
 * TaskExecutor simpleExecutor = (description, prompt) -> {
 *     System.out.println("执行任务: " + description);
 *     return "任务完成: " + prompt;
 * };
 * 
 * // 方法引用实现
 * TaskExecutor llmExecutor = MyLLMService::processTask;
 * 
 * // 匿名类实现
 * TaskExecutor complexExecutor = new TaskExecutor() {
 *     @Override
 *     public String execute(String taskDescription, String taskPrompt) {
 *         // 复杂的任务处理逻辑
 *         return processComplexTask(taskPrompt);
 *     }
 * };
 * }</pre>
 * 
 * <p><b>实现建议：</b></p>
 * <ul>
 *   <li>对于长时间运行的任务，考虑在实现中添加进度日志</li>
 *   <li>如果调用外部服务，建议添加重试逻辑和错误处理</li>
 *   <li>返回的结果应该是结构化的、易于解析的字符串</li>
 *   <li>如果任务失败，可以抛出异常或返回包含错误信息的结果</li>
 * </ul>
 * 
 * <p><b>线程安全：</b></p>
 * <p>TaskExecutor 的实现应该是线程安全的，因为 {@link TaskDelegationTool#executeBatch(java.util.List)}
 * 可能会并发调用同一个执行器实例。</p>
 *
 * @author EvoX Team
 * @see TaskDelegationTool
 */
@FunctionalInterface
public interface TaskExecutor {
    
    /**
     * 执行委派的任务
     * 
     * <p>该方法接收任务描述和详细指令，执行相应的处理逻辑，并返回执行结果。</p>
     * 
     * <p><b>参数说明：</b></p>
     * <ul>
     *   <li><b>taskDescription</b>: 任务的简短描述，主要用于日志记录和跟踪</li>
     *   <li><b>taskPrompt</b>: 任务的详细指令，包含具体的执行要求和期望输出</li>
     * </ul>
     * 
     * <p><b>返回值：</b></p>
     * <p>返回任务执行的结果字符串。结果应该包含足够的信息以便调用方理解任务的执行情况。
     * 如果任务失败，可以返回错误描述，或者抛出异常由上层处理。</p>
     * 
     * <p><b>异常处理：</b></p>
     * <p>实现可以抛出任何异常。{@link TaskDelegationTool} 会捕获这些异常并转换为失败的 ToolResult。</p>
     *
     * @param taskDescription 任务的简短描述，用于标识和追踪任务
     * @param taskPrompt      任务的详细执行指令
     * @return 任务执行结果字符串
     * @throws Exception 如果任务执行过程中发生错误
     */
    String execute(String taskDescription, String taskPrompt) throws Exception;
}
