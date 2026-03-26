package io.leavesfly.evox.claudecode.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.leavesfly.evox.claudecode.permission.PermissionManager;
import io.leavesfly.evox.claudecode.tool.ToolRegistry;
import io.leavesfly.evox.tools.base.BaseTool;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * 工具执行器
 * 负责工具调用的权限检查、执行、重试、并行执行和结果格式化。
 * 实现 Closeable 接口，支持优雅关闭线程池。
 */
@Slf4j
public class ToolExecutor implements Closeable {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int MAX_TOOL_RESULT_LENGTH = 5000;
    private static final int MAX_TOOL_RETRIES = 1;
    private static final int POOL_CORE_SIZE = 4;
    private static final int POOL_MAX_SIZE = 8;
    private static final long POOL_KEEP_ALIVE_SECONDS = 60;
    private static final long SHUTDOWN_TIMEOUT_SECONDS = 30;

    private final ToolRegistry toolRegistry;
    private final PermissionManager permissionManager;
    private final Consumer<String> streamEmitter;

    /** 自定义命名线程池，用于并行工具执行 */
    private final ExecutorService toolExecutorPool;
    
    /** 关闭状态标记 */
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public ToolExecutor(ToolRegistry toolRegistry, PermissionManager permissionManager,
                        Consumer<String> streamEmitter) {
        this.toolRegistry = toolRegistry;
        this.permissionManager = permissionManager;
        this.streamEmitter = streamEmitter;
        this.toolExecutorPool = createToolExecutorPool();
    }

    private static ExecutorService createToolExecutorPool() {
        AtomicInteger threadCounter = new AtomicInteger(0);
        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable, "evox-tool-executor-" + threadCounter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
        return new ThreadPoolExecutor(
                POOL_CORE_SIZE, POOL_MAX_SIZE,
                POOL_KEEP_ALIVE_SECONDS, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                threadFactory);
    }

    /**
     * 执行单个工具（带权限检查和重试）
     */
    public String executeWithPermission(String toolName, Map<String, Object> parameters) {
        if (!permissionManager.checkPermission(toolName, parameters)) {
            String deniedResult = "Tool call denied by user: " + toolName;
            emitStream("  ❌ " + deniedResult + "\n");
            return deniedResult;
        }

        BaseTool.ToolResult result = toolRegistry.executeTool(toolName, parameters);

        if (result.isSuccess()) {
            emitStream("  ✅ Success\n");
            return formatToolResult(result.getData());
        }

        for (int retry = 1; retry <= MAX_TOOL_RETRIES; retry++) {
            emitStream("  ⚠️ Tool failed, retrying (" + retry + "/" + MAX_TOOL_RETRIES + ")...\n");
            try {
                Thread.sleep(500L * retry);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            result = toolRegistry.executeTool(toolName, parameters);
            if (result.isSuccess()) {
                emitStream("  ✅ Success (after retry)\n");
                return formatToolResult(result.getData());
            }
        }

        String errorText = "Error: " + result.getError();
        emitStream("  ❌ " + errorText + "\n");
        return errorText;
    }

    /**
     * 并行执行多个工具调用，返回按原始顺序排列的结果列表
     */
    public List<String> executeInParallel(List<String> toolNames, List<Map<String, Object>> parametersList) {
        emitStream("\n⚡ Executing " + toolNames.size() + " tool calls in parallel...\n");

        for (int i = 0; i < toolNames.size(); i++) {
            emitStream("  🔧 [" + (i + 1) + "/" + toolNames.size() + "] "
                    + toolNames.get(i) + "(" + summarizeParams(parametersList.get(i)) + ")\n");
        }

        List<CompletableFuture<String>> futures = new ArrayList<>();
        for (int i = 0; i < toolNames.size(); i++) {
            String toolName = toolNames.get(i);
            Map<String, Object> parameters = parametersList.get(i);
            futures.add(CompletableFuture.supplyAsync(() ->
                    executeWithPermission(toolName, parameters), toolExecutorPool));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        List<String> results = new ArrayList<>();
        for (int i = 0; i < futures.size(); i++) {
            try {
                results.add(futures.get(i).get());
            } catch (Exception e) {
                String errorResult = "Error executing tool: " + e.getMessage();
                log.error("Parallel tool execution failed for '{}'", toolNames.get(i), e);
                results.add(errorResult);
            }
        }

        emitStream("  ✅ All " + toolNames.size() + " tool calls completed\n");
        return results;
    }

    /**
     * 解析 ToolCall 的 JSON arguments 字符串为 Map
     */
    public Map<String, Object> parseToolArguments(String argumentsJson) {
        if (argumentsJson == null || argumentsJson.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return OBJECT_MAPPER.readValue(argumentsJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse tool arguments JSON: {}", argumentsJson, e);
            return new LinkedHashMap<>();
        }
    }

    /**
     * 格式化工具执行结果，超长内容自动截断
     */
    public String formatToolResult(Object data) {
        if (data == null) {
            return "(no data)";
        }
        if (data instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) data;
            StringBuilder formatted = new StringBuilder();
            map.forEach((key, value) -> {
                String valueStr = value != null ? value.toString() : "null";
                if (valueStr.length() > MAX_TOOL_RESULT_LENGTH) {
                    valueStr = valueStr.substring(0, MAX_TOOL_RESULT_LENGTH)
                            + "\n... (truncated, " + valueStr.length() + " chars total)";
                }
                formatted.append(key).append(": ").append(valueStr).append("\n");
            });
            return formatted.toString().stripTrailing();
        }
        String result = data.toString();
        if (result.length() > MAX_TOOL_RESULT_LENGTH) {
            result = result.substring(0, MAX_TOOL_RESULT_LENGTH)
                    + "\n... (truncated, " + result.length() + " chars total)";
        }
        return result;
    }

    /**
     * 将工具参数摘要为简短字符串（用于日志和终端显示）
     */
    public String summarizeParams(Map<String, Object> parameters) {
        if (parameters.isEmpty()) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        parameters.forEach((key, value) -> {
            String valueStr = value != null ? value.toString() : "null";
            if (valueStr.length() > 60) {
                valueStr = valueStr.substring(0, 60) + "...";
            }
            parts.add(key + "=" + valueStr);
        });
        return String.join(", ", parts);
    }

    private void emitStream(String text) {
        if (streamEmitter != null) {
            streamEmitter.accept(text);
        }
    }
    
    /**
     * 检查执行器是否已关闭
     */
    public boolean isClosed() {
        return closed.get();
    }
    
    /**
     * 关闭工具执行器，优雅关闭线程池
     * 关闭顺序：shutdown -> awaitTermination -> shutdownNow
     */
    @Override
    public void close() throws IOException {
        if (closed.compareAndSet(false, true)) {
            log.info("Shutting down ToolExecutor thread pool...");
            
            // 第一步：平滑关闭，不再接受新任务
            toolExecutorPool.shutdown();
            
            try {
                // 第二步：等待现有任务完成
                if (!toolExecutorPool.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    log.warn("Thread pool did not terminate gracefully, forcing shutdown...");
                    
                    // 第三步：强制关闭
                    List<Runnable> pendingTasks = toolExecutorPool.shutdownNow();
                    if (!pendingTasks.isEmpty()) {
                        log.warn("{} tasks were pending and have been cancelled", pendingTasks.size());
                    }
                    
                    // 再次等待
                    if (!toolExecutorPool.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                        log.error("Thread pool did not terminate after forced shutdown");
                    }
                }
                
                log.info("ToolExecutor thread pool shutdown complete");
            } catch (InterruptedException e) {
                // 被中断时强制关闭
                log.warn("Shutdown interrupted, forcing immediate shutdown");
                toolExecutorPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * 注册 JVM 关闭钩子，确保线程池在 JVM 退出时被关闭
     */
    public void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                close();
            } catch (IOException e) {
                log.error("Error during shutdown hook cleanup", e);
            }
        }, "tool-executor-shutdown-hook"));
    }
}
