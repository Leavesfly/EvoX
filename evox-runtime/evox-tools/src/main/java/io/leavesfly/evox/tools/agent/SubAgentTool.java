package io.leavesfly.evox.tools.agent;

import io.leavesfly.evox.tools.base.BaseTool;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;

/**
 * 子代理工具（Sub-Agent Tool）
 * 允许主 Agent 将复杂任务委派给子 Agent 并行处理。
 * 每个子 Agent 拥有独立的对话历史，但共享 LLM 配置和工作目录。
 *
 * <p>使用场景：
 * <ul>
 *   <li>将复杂任务拆分为多个独立子任务并行执行</li>
 *   <li>调研多个相关但独立的问题</li>
 *   <li>执行需要独立上下文的长任务</li>
 * </ul>
 *
 * <p>SubAgentTool 本身不依赖 LLM，通过 {@link SubAgentExecutor} 接口
 * 由上层应用注入具体的子 Agent 执行逻辑。
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class SubAgentTool extends BaseTool {

    private String workingDirectory;
    private int maxConcurrentAgents;
    private long taskTimeoutSeconds;
    private SubAgentExecutor executor;

    /**
     * 子代理执行器接口
     * 由上层应用实现，定义子 Agent 如何执行任务
     */
    @FunctionalInterface
    public interface SubAgentExecutor {
        /**
         * 执行子代理任务
         *
         * @param taskDescription 任务描述
         * @param taskPrompt      任务详细提示
         * @return 任务执行结果
         */
        String execute(String taskDescription, String taskPrompt);
    }

    public SubAgentTool() {
        this(System.getProperty("user.dir"), null);
    }

    public SubAgentTool(String workingDirectory, SubAgentExecutor executor) {
        super();
        this.name = "sub_agent";
        this.description = "Delegate a task to a sub-agent that runs independently. "
                + "Use this for complex tasks that can be broken down into independent subtasks. "
                + "Each sub-agent has its own conversation context but shares the same tools and project access. "
                + "The sub-agent will execute the task and return the result.";
        this.workingDirectory = workingDirectory;
        this.executor = executor;
        this.maxConcurrentAgents = 3;
        this.taskTimeoutSeconds = 300;

        this.inputs = new HashMap<>();
        this.required = new ArrayList<>();

        Map<String, String> descriptionParam = new HashMap<>();
        descriptionParam.put("type", "string");
        descriptionParam.put("description", "A short description of the task for the sub-agent (used for tracking)");
        this.inputs.put("description", descriptionParam);
        this.required.add("description");

        Map<String, String> promptParam = new HashMap<>();
        promptParam.put("type", "string");
        promptParam.put("description", "Detailed instructions for the sub-agent. Be specific about what to do and what to return.");
        this.inputs.put("prompt", promptParam);
        this.required.add("prompt");
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        validateParameters(parameters);
        String taskDescription = getParameter(parameters, "description", "");
        String taskPrompt = getParameter(parameters, "prompt", "");

        if (taskDescription.isBlank() || taskPrompt.isBlank()) {
            return ToolResult.failure("Both 'description' and 'prompt' are required");
        }

        if (executor == null) {
            return ToolResult.failure("No SubAgentExecutor configured. "
                    + "Please set an executor via setExecutor() before using sub-agent.");
        }

        return executeWithExecutor(taskDescription, taskPrompt);
    }

    private ToolResult executeWithExecutor(String taskDescription, String taskPrompt) {
        log.info("Sub-agent starting task: {}", taskDescription);

        ExecutorService threadPool = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "sub-agent-" + taskDescription.hashCode());
            thread.setDaemon(true);
            return thread;
        });

        try {
            Future<String> future = threadPool.submit(() -> executor.execute(taskDescription, taskPrompt));
            String result = future.get(taskTimeoutSeconds, TimeUnit.SECONDS);

            log.info("Sub-agent completed task: {}", taskDescription);

            Map<String, Object> resultData = new LinkedHashMap<>();
            resultData.put("taskDescription", taskDescription);
            resultData.put("status", "completed");
            resultData.put("result", result);
            return ToolResult.success(resultData);

        } catch (TimeoutException e) {
            log.warn("Sub-agent task timed out: {}", taskDescription);
            return ToolResult.failure("Sub-agent task timed out after " + taskTimeoutSeconds + " seconds: " + taskDescription);
        } catch (ExecutionException e) {
            log.error("Sub-agent task failed: {}", taskDescription, e.getCause());
            return ToolResult.failure("Sub-agent task failed: " + e.getCause().getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ToolResult.failure("Sub-agent task interrupted: " + taskDescription);
        } finally {
            threadPool.shutdownNow();
        }
    }

    /**
     * 并发执行多个子任务
     *
     * @param tasks 任务列表，每个任务包含 description 和 prompt
     * @return 所有任务的执行结果
     */
    public List<ToolResult> executeBatch(List<Map<String, String>> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return Collections.emptyList();
        }

        if (executor == null) {
            return Collections.singletonList(ToolResult.failure("No SubAgentExecutor configured"));
        }

        int poolSize = Math.min(tasks.size(), maxConcurrentAgents);
        ExecutorService threadPool = Executors.newFixedThreadPool(poolSize, runnable -> {
            Thread thread = new Thread(runnable, "sub-agent-batch-" + Thread.activeCount());
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
            return results;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Collections.singletonList(ToolResult.failure("Batch execution interrupted"));
        } finally {
            threadPool.shutdownNow();
        }
    }
}
