package io.leavesfly.evox.cowork.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.leavesfly.evox.agents.base.Agent;
import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.core.message.MessageType;
import io.leavesfly.evox.cowork.config.CoworkConfig;
import io.leavesfly.evox.cowork.context.CoworkContext;
import io.leavesfly.evox.cowork.permission.CoworkPermissionManager;
import io.leavesfly.evox.cowork.tool.CoworkToolRegistry;
import io.leavesfly.evox.models.client.ChatCompletionResult;
import io.leavesfly.evox.models.client.ToolCall;
import io.leavesfly.evox.models.client.ToolDefinition;
import io.leavesfly.evox.tools.agent.SubAgentTool;
import io.leavesfly.evox.tools.base.BaseTool;
import io.leavesfly.evox.memory.manager.MemoryManager;
import io.leavesfly.evox.memory.shortterm.ShortTermMemory;
import io.leavesfly.evox.models.base.LLMProvider;
import io.leavesfly.evox.models.factory.LLMFactory;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.Consumer;

/**
 * Cowork 智能代理核心类
 * 继承 EvoX 框架的 {@link Agent} 基类，实现 {@link io.leavesfly.evox.core.agent.IAgent} 接口，
 * 负责协调 LLM、工具注册表、权限管理器和上下文，执行对话和任务。
 *
 * <p>通过继承 Agent 基类，CoworkAgent 可以：</p>
 * <ul>
 *   <li>被 {@link io.leavesfly.evox.agents.manager.AgentManager} 注册和管理</li>
 *   <li>被 Workflow 编排</li>
 *   <li>通过 {@code AgentTool.wrap(coworkAgent)} 作为工具嵌入其他 Agent</li>
 *   <li>使用 {@code call()} / {@code callAsync()} 等标准调用方式</li>
 * </ul>
 */
@Slf4j
public class CoworkAgent extends Agent {

    private static final String AGENT_NAME = "CoworkAgent";
    private static final String AGENT_DESCRIPTION =
            "An agentic knowledge work assistant that coordinates LLM, tools, "
                    + "permissions, and context for document processing and task execution.";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Getter
    private final CoworkConfig config;
    @Getter
    private final CoworkToolRegistry toolRegistry;
    @Getter
    private final CoworkPermissionManager permissionManager;
    @Getter
    private final MemoryManager memoryManager;
    @Getter
    private final CoworkContext coworkContext;

    /** 强类型 LLM 引用（LLMProvider 扩展了 ILLM + ILLMToolUse） */
    private final LLMProvider llmProvider;
    private Consumer<String> streamCallback;
    private final TaskProgress currentProgress;

    /** 当前代理的递归深度（0 = 顶层代理） */
    private final int currentDepth;

    public CoworkAgent(CoworkConfig config, CoworkPermissionManager permissionManager,
                       CoworkToolRegistry toolRegistry, CoworkContext coworkContext) {
        this(config, permissionManager, toolRegistry, coworkContext, 0);
    }

    /**
     * 内部构造函数，支持指定递归深度（用于子代理创建）
     *
     * @param depth 当前递归深度（0 = 顶层代理）
     */
    CoworkAgent(CoworkConfig config, CoworkPermissionManager permissionManager,
                CoworkToolRegistry toolRegistry, CoworkContext coworkContext, int depth) {
        super(); // Agent() → BaseModule()
        this.config = config;
        this.permissionManager = permissionManager;
        this.toolRegistry = toolRegistry;
        this.coworkContext = coworkContext;
        this.currentDepth = depth;
        this.memoryManager = new MemoryManager(new ShortTermMemory(config.getMaxHistoryMessages()));
        this.llmProvider = LLMFactory.create(config.getLlmConfig());
        this.currentProgress = new TaskProgress();

        // set Agent base class fields
        setName(AGENT_NAME);
        setDescription(AGENT_DESCRIPTION);
        setLlm(llmProvider); // inject into base class (ILLM type)
        setHuman(false);

        initializeSubAgentExecutor();
    }

    // ==================== IAgent / Agent contract ====================

    @Override
    protected String getPrimaryActionName() {
        return "chat";
    }

    /**
     * 实现 {@link Agent#execute(String, List)} — IAgent 标准入口。
     * 将 EvoX 框架的 Message 列表转换为用户输入，委托给 {@link #chat(String)}。
     */
    @Override
    public Message execute(String actionName, List<Message> messages) {
        String userInput = extractUserInput(messages);
        if (userInput == null || userInput.isBlank()) {
            return Message.builder()
                    .messageType(MessageType.ERROR)
                    .content("No input provided")
                    .build();
        }

        try {
            String response = chat(userInput);
            return Message.builder()
                    .messageType(MessageType.RESPONSE)
                    .content(response)
                    .build();
        } catch (Exception e) {
            log.error("CoworkAgent execution failed", e);
            return Message.builder()
                    .messageType(MessageType.ERROR)
                    .content("Execution failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 从 EvoX Message 列表中提取最后一条用户输入文本
     */
    private String extractUserInput(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message msg = messages.get(i);
            if (msg.getMessageType() == MessageType.INPUT) {
                Object content = msg.getContent();
                return content != null ? content.toString() : null;
            }
        }
        // fallback: use last message regardless of type
        Object content = messages.get(messages.size() - 1).getContent();
        return content != null ? content.toString() : null;
    }

    // ==================== Chat & Tool Calling ====================

    public void setStreamCallback(Consumer<String> callback) {
        this.streamCallback = callback;
    }

    /**
     * 处理用户输入并进行对话
     * 使用 LLM 原生 function calling 进行 ReAct (Reasoning + Acting) 循环
     *
     * @param userInput 用户输入文本
     * @return 代理的最终响应
     */
    public String chat(String userInput) {
        Message userMessage = Message.inputMessage(userInput);
        userMessage.setAgent("user");
        memoryManager.addMessage(userMessage);

        List<Message> conversationMessages = buildConversationMessages();
        List<ToolDefinition> toolDefinitions = buildToolDefinitions();

        for (int iteration = 0; iteration < config.getMaxIterations(); iteration++) {
            ChatCompletionResult completionResult = llmProvider.chatWithToolDefinitions(
                    conversationMessages, toolDefinitions, "auto");

            if (completionResult == null) {
                emitStream("Error: LLM returned empty response");
                return "Error: No response from LLM";
            }

            if (completionResult.isTextResponse()) {
                String responseContent = completionResult.getContent();
                emitStream(responseContent);
                Message assistantMessage = Message.responseMessage(responseContent, "assistant", "chat");
                memoryManager.addMessage(assistantMessage);
                return responseContent;
            }

            if (!completionResult.hasToolCalls()) {
                String fallbackContent = completionResult.getContent() != null
                        ? completionResult.getContent() : "Error: No response from LLM";
                emitStream(fallbackContent);
                Message assistantMessage = Message.responseMessage(fallbackContent, "assistant", "chat");
                memoryManager.addMessage(assistantMessage);
                return fallbackContent;
            }

            if (completionResult.getContent() != null && !completionResult.getContent().isBlank()) {
                emitStream(completionResult.getContent());
            }

            Message assistantMessage = Message.responseMessage(
                    completionResult.getContent() != null ? completionResult.getContent() : "",
                    "assistant", "tool_call");
            memoryManager.addMessage(assistantMessage);

            for (ToolCall toolCall : completionResult.getToolCalls()) {
                String toolName = toolCall.getFunction().getName();
                Map<String, Object> parameters = parseToolCallArguments(toolCall.getFunction().getArguments());

                emitStream(String.format("\n[Executing tool: %s with params: %s]",
                        toolName, summarizeParams(parameters)));

                if (!permissionManager.checkPermission(toolName, parameters)) {
                    String deniedResult = String.format("Permission denied for tool: %s", toolName);
                    Message toolResultMsg = buildToolResultMessage(toolCall.getId(), toolName, deniedResult);
                    memoryManager.addMessage(toolResultMsg);
                    conversationMessages.add(toolResultMsg);
                    continue;
                }

                try {
                    BaseTool.ToolResult result = toolRegistry.executeTool(toolName, parameters);
                    String formattedResult = formatToolResult(result);
                    Message toolResultMsg = buildToolResultMessage(toolCall.getId(), toolName, formattedResult);
                    memoryManager.addMessage(toolResultMsg);
                    conversationMessages.add(toolResultMsg);

                    currentProgress.updateStep(String.format("Executed %s", toolName));
                    emitProgress(String.format("✓ Completed: %s", toolName));
                } catch (Exception e) {
                    String errorResult = String.format("Error executing %s: %s", toolName, e.getMessage());
                    Message toolResultMsg = buildToolResultMessage(toolCall.getId(), toolName, errorResult);
                    memoryManager.addMessage(toolResultMsg);
                    conversationMessages.add(toolResultMsg);
                    log.error("Tool execution error for {}", toolName, e);
                }
            }
        }

        return "Error: Maximum iterations reached without completion";
    }

    /**
     * 执行子任务（带递归深度限制）
     */
    public String executeSubTask(String taskDescription, String taskPrompt) {
        int nextDepth = currentDepth + 1;
        int maxDepth = config.getMaxSubAgentDepth();

        if (nextDepth > maxDepth) {
            String errorMessage = "Sub-agent delegation rejected: maximum recursion depth ("
                    + maxDepth + ") exceeded at depth " + currentDepth
                    + ". Please handle this task directly.";
            log.warn(errorMessage);
            return errorMessage;
        }

        log.info("Sub-agent executing at depth {}/{}: {}", nextDepth, maxDepth, taskDescription);
        CoworkAgent subAgent = new CoworkAgent(config, permissionManager, toolRegistry, coworkContext, nextDepth);
        subAgent.setStreamCallback(this.streamCallback);
        return subAgent.chat(taskPrompt);
    }

    public void clearHistory() {
        memoryManager.clearShortTerm();
    }

    public void compactHistory() {
        List<Message> messages = memoryManager.getAllMessages();
        if (messages.size() > 10) {
            List<Message> compacted = new ArrayList<>();
            compacted.addAll(messages.subList(0, 2));
            
            String summary = String.format("[... %d messages summarized ...]", messages.size() - 10);
            Message summaryMessage = Message.responseMessage(summary, "system", "summary");
            compacted.add(summaryMessage);
            
            compacted.addAll(messages.subList(messages.size() - 8, messages.size()));
            
            memoryManager.clearShortTerm();
            memoryManager.addMessages(compacted);
        }
    }

    public TaskProgress getCurrentProgress() {
        return currentProgress;
    }

    private void initializeSubAgentExecutor() {
        SubAgentTool subAgentTool = toolRegistry.getSubAgentTool();
        if (subAgentTool != null) {
            subAgentTool.setExecutor(this::executeSubTask);
        }
    }

    private List<Message> buildConversationMessages() {
        List<Message> messages = new ArrayList<>();
        messages.add(Message.systemMessage(buildSystemPrompt()));
        messages.addAll(memoryManager.getAllMessages());
        return messages;
    }

    private String buildSystemPrompt() {
        StringBuilder prompt = new StringBuilder();
        
        if (config.getSystemPrompt() != null && !config.getSystemPrompt().isEmpty()) {
            prompt.append(config.getSystemPrompt()).append("\n\n");
        } else {
            prompt.append(getDefaultSystemPrompt()).append("\n\n");
        }

        if (coworkContext != null) {
            prompt.append("Current Context:\n").append(coworkContext.toContextSummary()).append("\n\n");
        }

        prompt.append("Available Tools:\n").append(toolRegistry.generateToolDescriptions());

        return prompt.toString();
    }

    private String getDefaultSystemPrompt() {
        return "You are an intelligent knowledge work assistant specialized in helping users with various professional tasks.\n\n" +
            "Your core capabilities include:\n" +
            "1. File and Document Management: Reading, writing, organizing, and analyzing documents across various formats\n" +
            "2. Research and Analysis: Conducting research, gathering information, and performing data analysis\n" +
            "3. Document Creation: Creating professional documents including Excel spreadsheets, PowerPoint presentations, and Word documents\n" +
            "4. Data Processing and Visualization: Processing data, generating insights, and creating visual representations\n" +
            "5. Multi-step Task Decomposition: Breaking down complex tasks into manageable steps and executing them systematically\n" +
            "6. Parallel Execution: Identifying independent tasks that can be executed concurrently for efficiency\n\n" +
            "Important Guidelines:\n" +
            "- Always confirm before performing destructive operations (like file deletion or overwriting)\n" +
            "- Provide clear explanations before executing complex operations\n" +
            "- Be thorough in your analysis and provide comprehensive results\n" +
            "- If you need more information to complete a task, ask clarifying questions\n" +
            "- Use tools efficiently and avoid unnecessary operations\n" +
            "- Keep responses concise but informative";
    }

    /**
     * 将 CoworkToolRegistry 中的工具 schema 转换为 LLM 原生 function calling 的 ToolDefinition 列表
     */
    private List<ToolDefinition> buildToolDefinitions() {
        List<Map<String, Object>> toolSchemas = toolRegistry.getToolSchemas();
        List<ToolDefinition> definitions = new ArrayList<>(toolSchemas.size());
        for (Map<String, Object> schema : toolSchemas) {
            ToolDefinition definition = ToolDefinition.fromToolSchema(schema);
            if (definition != null) {
                definitions.add(definition);
            }
        }
        return definitions;
    }

    /**
     * 解析 LLM 返回的 ToolCall arguments JSON 字符串为 Map
     */
    private Map<String, Object> parseToolCallArguments(String argumentsJson) {
        if (argumentsJson == null || argumentsJson.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return OBJECT_MAPPER.readValue(argumentsJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("Failed to parse tool call arguments: {}", argumentsJson, e);
            return Collections.emptyMap();
        }
    }

    /**
     * 构建工具结果消息，通过 metadata 携带 toolCallId 和 toolName
     */
    private Message buildToolResultMessage(String toolCallId, String toolName, String resultContent) {
        Message message = Message.builder()
                .content(resultContent)
                .agent("tool")
                .action(toolName)
                .messageType(MessageType.RESPONSE)
                .build();
        message.putMetadata("toolCallId", toolCallId);
        message.putMetadata("toolName", toolName);
        message.putMetadata("role", "tool");
        return message;
    }

    private String formatToolResult(Object data) {
        if (data == null) {
            return "No result";
        }

        String result;
        if (data instanceof Map) {
            StringBuilder sb = new StringBuilder();
            Map<?, ?> map = (Map<?, ?>) data;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
            result = sb.toString();
        } else {
            result = data.toString();
        }

        if (result.length() > 5000) {
            result = result.substring(0, 5000) + "\n... [truncated]";
        }

        return result;
    }

    private String summarizeParams(Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return "{}";
        }

        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            first = false;

            String valueStr = String.valueOf(entry.getValue());
            if (valueStr.length() > 60) {
                valueStr = valueStr.substring(0, 60) + "...";
            }
            sb.append(entry.getKey()).append("=").append(valueStr);
        }
        sb.append("}");

        return sb.toString();
    }

    private void emitStream(String text) {
        if (streamCallback != null) {
            streamCallback.accept(text);
        }
    }

    private void emitProgress(String stepDescription) {
        emitStream(String.format("[Progress] %s", stepDescription));
    }

    @Data
    public static class TaskProgress {
        private String taskId;
        private int totalSteps = 0;
        private int completedSteps = 0;
        private String currentStepDescription;
        private String status = "IDLE";

        public void startTask(String taskId) {
            this.taskId = taskId;
            this.status = "RUNNING";
            this.completedSteps = 0;
        }

        public void updateStep(String description) {
            this.currentStepDescription = description;
            this.completedSteps++;
        }

        public void complete() {
            this.status = "COMPLETED";
        }

        public void fail(String reason) {
            this.status = "FAILED";
            this.currentStepDescription = reason;
        }
    }
}
