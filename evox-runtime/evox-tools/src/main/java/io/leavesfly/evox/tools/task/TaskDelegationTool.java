package io.leavesfly.evox.tools.task;

import io.leavesfly.evox.tools.base.BaseTool;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;

/**
 * 任务委派工具。
 *
 * 将复杂任务委派给外部执行器处理，执行器可实现 {@link TaskExecutor}，如 LLM 调用、远程服务或自定义逻辑。
 *
 * 核心特性：
 * - 任务隔离：每个任务在独立线程中执行
 * - 并发控制：批量任务并发执行，可配置最大并发数
 * - 超时保护：可配置超时时间，防止长时间阻塞
 * - 灵活扩展：通过注入 TaskExecutor 自定义执行逻辑
 *
 * 使用场景：拆分复杂任务为独立子任务、调研多个独立问题、执行需独立上下文的长时间任务、调用外部 API。
 *
 * 使用示例：
 * {@code TaskDelegationTool tool = new TaskDelegationTool("./workspace", (d, p) -> callService(p));}
 * {@code ToolResult result = tool.execute(Map.of("description", "数据分析", "prompt", "..."));}
 * {@code List<ToolResult> results = tool.executeBatch(tasks);}
 *
 * 注意事项：使用前需通过构造函数或 {@link #setExecutor} 设置执行器；执行器应线程安全；建议设置合理超时。
 *
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

    /** 默认构造，工作目录为当前用户目录，需后续设置执行器 */
    public TaskDelegationTool() {
        this(System.getProperty("user.dir"), null);
    }

    /**
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

        this.inputs = Map.of(
                "description", Map.of("type", "string", "description", "A short description of the task (used for tracking and logging)"),
                "prompt", Map.of("type", "string", "description", "Detailed instructions for the task. Be specific about what to do and what to return.")
        );
        this.required = List.of("description", "prompt");
    }

    /**
     * 执行任务委派。验证参数后调用执行器，在独立线程中运行以避免阻塞。
     *
     * @param parameters 任务参数，必须包含 description 和 prompt
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
     * 使用执行器执行任务。在独立线程中运行，支持超时控制。
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
     * 批量并发执行多个任务。使用线程池，并发数受 maxConcurrentTasks 限制。
     *
     * @param tasks 任务列表，每项包含 description 和 prompt
     * @return 执行结果列表，顺序与输入一致
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
