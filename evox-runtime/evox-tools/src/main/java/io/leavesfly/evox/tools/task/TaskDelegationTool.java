package io.leavesfly.evox.tools.task;

import io.leavesfly.evox.tools.base.BaseTool;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;

/**
 * 任务委派工具 (Task Delegation Tool)
 * 
 * <p>允许将复杂任务委派给外部执行器进行处理。执行器可以是任何实现了 {@link TaskExecutor} 接口的对象，
 * 例如另一个 LLM 调用、远程服务、或自定义的任务处理逻辑。</p>
 * 
 * <p><b>核心特性：</b></p>
 * <ul>
 *   <li><b>任务隔离</b>: 每个委派任务在独立线程中执行，互不干扰</li>
 *   <li><b>并发控制</b>: 支持批量任务的并发执行，可配置最大并发数</li>
 *   <li><b>超时保护</b>: 可配置任务执行超时时间，防止长时间阻塞</li>
 *   <li><b>灵活扩展</b>: 通过注入 TaskExecutor 实现自定义执行逻辑</li>
 * </ul>
 * 
 * <p><b>使用场景：</b></p>
 * <ul>
 *   <li>将复杂任务拆分为多个独立子任务并行执行</li>
 *   <li>调研多个相关但独立的问题</li>
 *   <li>执行需要独立上下文的长时间任务</li>
 *   <li>调用外部服务或 API 处理特定任务</li>
 * </ul>
 * 
 * <p><b>使用示例：</b></p>
 * <pre>{@code
 * // 创建任务委派工具
 * TaskDelegationTool tool = new TaskDelegationTool(
 *     "./workspace",
 *     (description, prompt) -> {
 *         // 自定义任务执行逻辑
 *         return callExternalService(prompt);
 *     }
 * );
 * 
 * // 执行单个任务
 * Map<String, Object> params = Map.of(
 *     "description", "数据分析",
 *     "prompt", "分析销售数据并提供洞察"
 * );
 * ToolResult result = tool.execute(params);
 * 
 * // 批量执行多个任务
 * List<Map<String, String>> tasks = List.of(
 *     Map.of("description", "Task1", "prompt", "..."),
 *     Map.of("description", "Task2", "prompt", "...")
 * );
 * List<ToolResult> results = tool.executeBatch(tasks);
 * }</pre>
 * 
 * <p><b>注意事项：</b></p>
 * <ul>
 *   <li>使用前必须通过构造函数或 {@link #setExecutor(TaskExecutor)} 设置执行器</li>
 *   <li>任务执行器应该是线程安全的，因为可能被并发调用</li>
 *   <li>建议为长时间运行的任务设置合理的超时时间</li>
 * </ul>
 *
 * @author EvoX Team
 * @see TaskExecutor
 * @see BaseTool
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class TaskDelegationTool extends BaseTool {

    /** 工作目录路径 */
    private String workingDirectory;
    
    /** 最大并发任务数 */
    private int maxConcurrentTasks;
    
    /** 任务执行超时时间（秒） */
    private long taskTimeoutSeconds;
    
    /** 任务执行器 */
    private TaskExecutor executor;

    /**
     * 默认构造函数
     * 使用当前用户目录作为工作目录，需要后续设置执行器
     */
    public TaskDelegationTool() {
        this(System.getProperty("user.dir"), null);
    }

    /**
     * 构造函数
     *
     * @param workingDirectory 工作目录路径
     * @param executor         任务执行器
     */
    public TaskDelegationTool(String workingDirectory, TaskExecutor executor) {
        super();
        this.name = "delegate_task";
        this.description = "Delegate a task to an external executor that runs independently. "
                + "Use this when you need to break down complex work into independent subtasks. "
                + "Each delegated task runs in isolation with its own context, but the executor "
                + "can be configured to share resources like tools or data access. "
                + "The task will execute and return the result.";
        this.workingDirectory = workingDirectory;
        this.executor = executor;
        this.maxConcurrentTasks = 3;
        this.taskTimeoutSeconds = 300;

        this.inputs = new HashMap<>();
        this.required = new ArrayList<>();

        // 定义 description 参数
        Map<String, String> descriptionParam = new HashMap<>();
        descriptionParam.put("type", "string");
        descriptionParam.put("description", "A short description of the task (used for tracking and logging)");
        this.inputs.put("description", descriptionParam);
        this.required.add("description");

        // 定义 prompt 参数
        Map<String, String> promptParam = new HashMap<>();
        promptParam.put("type", "string");
        promptParam.put("description", "Detailed instructions for the task. Be specific about what to do and what to return.");
        this.inputs.put("prompt", promptParam);
        this.required.add("prompt");
    }

    /**
     * 执行任务委派
     * 
     * <p>该方法验证参数，调用配置的执行器处理任务，并在独立线程中运行以避免阻塞。</p>
     *
     * @param parameters 任务参数，必须包含 "description" 和 "prompt"
     * @return 任务执行结果
     */
    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        validateParameters(parameters);
        
        String taskDescription = getParameter(parameters, "description", "");
        String taskPrompt = getParameter(parameters, "prompt", "");

        if (taskDescription.isBlank() || taskPrompt.isBlank()) {
            return ToolResult.failure("Both 'description' and 'prompt' are required and cannot be empty");
        }

        if (executor == null) {
            return ToolResult.failure("No TaskExecutor configured. "
                    + "Please set an executor via setExecutor() or constructor before using this tool.");
        }

        return executeWithExecutor(taskDescription, taskPrompt);
    }

    /**
     * 使用执行器执行任务
     * 
     * <p>在独立线程中执行任务，支持超时控制。</p>
     *
     * @param taskDescription 任务描述
     * @param taskPrompt      任务详细指令
     * @return 任务执行结果
     */
    private ToolResult executeWithExecutor(String taskDescription, String taskPrompt) {
        log.info("[TaskDelegationTool] 开始执行任务: {}", taskDescription);

        ExecutorService threadPool = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "task-delegation-" + taskDescription.hashCode());
            thread.setDaemon(true);
            return thread;
        });

        try {
            Future<String> future = threadPool.submit(() -> executor.execute(taskDescription, taskPrompt));
            String result = future.get(taskTimeoutSeconds, TimeUnit.SECONDS);

            log.info("[TaskDelegationTool] 任务完成: {}", taskDescription);

            Map<String, Object> resultData = new LinkedHashMap<>();
            resultData.put("taskDescription", taskDescription);
            resultData.put("status", "completed");
            resultData.put("result", result);
            return ToolResult.success(resultData);

        } catch (TimeoutException e) {
            log.warn("[TaskDelegationTool] 任务超时: {}", taskDescription);
            return ToolResult.failure(
                String.format("Task timed out after %d seconds: %s", taskTimeoutSeconds, taskDescription)
            );
        } catch (ExecutionException e) {
            log.error("[TaskDelegationTool] 任务执行失败: {}", taskDescription, e.getCause());
            return ToolResult.failure("Task execution failed: " + e.getCause().getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[TaskDelegationTool] 任务被中断: {}", taskDescription);
            return ToolResult.failure("Task was interrupted: " + taskDescription);
        } finally {
            threadPool.shutdownNow();
        }
    }

    /**
     * 批量并发执行多个任务
     * 
     * <p>使用线程池并发执行多个任务，每个任务独立运行。并发数受 {@link #maxConcurrentTasks} 限制。</p>
     * 
     * <p><b>使用示例：</b></p>
     * <pre>{@code
     * List<Map<String, String>> tasks = List.of(
     *     Map.of("description", "分析用户行为", "prompt", "..."),
     *     Map.of("description", "生成报告", "prompt", "..."),
     *     Map.of("description", "发送通知", "prompt", "...")
     * );
     * List<ToolResult> results = tool.executeBatch(tasks);
     * }</pre>
     *
     * @param tasks 任务列表，每个任务包含 "description" 和 "prompt" 字段
     * @return 所有任务的执行结果列表，顺序与输入任务列表一致
     */
    public List<ToolResult> executeBatch(List<Map<String, String>> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return Collections.emptyList();
        }

        if (executor == null) {
            return Collections.singletonList(
                ToolResult.failure("No TaskExecutor configured")
            );
        }

        log.info("[TaskDelegationTool] 开始批量执行 {} 个任务", tasks.size());

        int poolSize = Math.min(tasks.size(), maxConcurrentTasks);
        ExecutorService threadPool = Executors.newFixedThreadPool(poolSize, runnable -> {
            Thread thread = new Thread(runnable, "task-batch-" + Thread.activeCount());
            thread.setDaemon(true);
            return thread;
        });

        try {
            List<Future<ToolResult>> futures = new ArrayList<>();
            for (Map<String, String> task : tasks) {
                futures.add(threadPool.submit(() -> {
                    Map<String, Object> params = new LinkedHashMap<>(task);
                    return execute(params);
                }));
            }

            List<ToolResult> results = new ArrayList<>();
            for (Future<ToolResult> future : futures) {
                try {
                    results.add(future.get(taskTimeoutSeconds, TimeUnit.SECONDS));
                } catch (TimeoutException e) {
                    results.add(ToolResult.failure("Task timed out"));
                } catch (ExecutionException e) {
                    results.add(ToolResult.failure("Task failed: " + e.getCause().getMessage()));
                }
            }
            
            log.info("[TaskDelegationTool] 批量任务执行完成，成功: {}, 失败: {}", 
                results.stream().filter(r -> r.isSuccess()).count(),
                results.stream().filter(r -> !r.isSuccess()).count());
            
            return results;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[TaskDelegationTool] 批量任务执行被中断");
            return Collections.singletonList(ToolResult.failure("Batch execution interrupted"));
        } finally {
            threadPool.shutdownNow();
        }
    }
}
