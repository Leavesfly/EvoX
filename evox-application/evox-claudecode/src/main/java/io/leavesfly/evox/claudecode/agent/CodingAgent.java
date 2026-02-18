package io.leavesfly.evox.claudecode.agent;

import io.leavesfly.evox.agents.base.Agent;
import io.leavesfly.evox.skill.SkillActivationResult;
import io.leavesfly.evox.skill.SkillTool;
import io.leavesfly.evox.claudecode.config.ClaudeCodeConfig;
import io.leavesfly.evox.claudecode.context.ProjectContext;
import io.leavesfly.evox.claudecode.permission.PermissionManager;
import io.leavesfly.evox.claudecode.tool.ToolRegistry;
import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.core.message.MessageType;
import io.leavesfly.evox.memory.manager.MemoryManager;
import io.leavesfly.evox.memory.shortterm.ShortTermMemory;
import io.leavesfly.evox.models.spi.LLMProvider;
import io.leavesfly.evox.models.protocol.ChatCompletionResult;
import io.leavesfly.evox.models.protocol.ToolCall;
import io.leavesfly.evox.models.protocol.ToolDefinition;
import io.leavesfly.evox.models.config.LLMFactory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 编码智能体
 * 继承 EvoX 框架的 {@link Agent} 基类，实现 {@link io.leavesfly.evox.core.agent.IAgent} 接口，
 * 使用原生 JSON Function Calling 进行流式工具调用循环。
 *
 * <p>核心循环：用户输入 → LLM 思考 → 工具调用 → 结果反馈 → LLM 继续</p>
 *
 * <p>通过继承 Agent 基类，CodingAgent 可以：</p>
 * <ul>
 *   <li>被 {@link io.leavesfly.evox.agents.manager.AgentManager} 注册和管理</li>
 *   <li>被 Workflow 编排</li>
 *   <li>通过 {@code AgentTool.wrap(codingAgent)} 作为工具嵌入其他 Agent</li>
 *   <li>使用 {@code call()} / {@code callAsync()} 等标准调用方式</li>
 * </ul>
 */
@Slf4j
public class CodingAgent extends Agent {

    private static final String AGENT_NAME = "CodingAgent";
    private static final String AGENT_DESCRIPTION =
            "An agentic coding assistant that uses native JSON Function Calling "
                    + "with streaming tool execution, permission management, and sub-agent delegation.";

    @Getter
    private final ClaudeCodeConfig config;
    @Getter
    private final ToolRegistry toolRegistry;
    @Getter
    private final PermissionManager permissionManager;
    @Getter
    private final MemoryManager memoryManager;
    @Getter
    private final ProjectContext projectContext;

    /** 强类型 LLM 引用（LLMProvider 扩展了 ILLM + ILLMToolUse） */
    private final LLMProvider llmProvider;
    private final StreamCollector streamCollector;
    private final ToolExecutor toolExecutor;
    private final HistoryCompactor historyCompactor;

    private Consumer<String> streamCallback;

    private long totalPromptTokens = 0;
    private long totalCompletionTokens = 0;

    private List<ToolDefinition> cachedToolDefinitions;

    /** 当前代理的递归深度（0 = 顶层代理） */
    private final int currentDepth;

    public CodingAgent(ClaudeCodeConfig config, PermissionManager permissionManager) {
        this(config, permissionManager, 0);
    }

    /**
     * 内部构造函数，支持指定递归深度（用于子代理创建）
     *
     * @param depth 当前递归深度（0 = 顶层代理）
     */
    CodingAgent(ClaudeCodeConfig config, PermissionManager permissionManager, int depth) {
        super(); // Agent() → BaseModule()
        this.config = config;
        this.permissionManager = permissionManager;
        this.currentDepth = depth;
        this.toolRegistry = new ToolRegistry(config.getWorkingDirectory());
        this.memoryManager = new MemoryManager(new ShortTermMemory(config.getMaxHistoryMessages()));
        this.projectContext = new ProjectContext(config.getWorkingDirectory());
        this.llmProvider = LLMFactory.create(config.getLlmConfig());

        // set Agent base class fields
        setName(AGENT_NAME);
        setDescription(AGENT_DESCRIPTION);
        setLlm(llmProvider); // inject into base class (ILLM type)
        setHuman(false);

        // initialize delegated components (streamCallback is set later via setStreamCallback)
        this.streamCollector = new StreamCollector(llmProvider, this::emitStream);
        this.toolExecutor = new ToolExecutor(toolRegistry, permissionManager, this::emitStream);
        this.historyCompactor = new HistoryCompactor(memoryManager, llmProvider,
                config.getContextWindow(), this::emitStream);

        initializeProjectContext();
        initializeTaskExecutor();
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
            log.error("CodingAgent execution failed", e);
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

    /**
     * 设置流式输出回调
     */
    public void setStreamCallback(Consumer<String> callback) {
        this.streamCallback = callback;
    }

    /**
     * 处理用户输入，使用原生 Function Calling 进行工具调用循环
     *
     * @param userInput 用户输入
     * @return Agent 最终回复
     */
    public String chat(String userInput) {
        Message userMessage = Message.inputMessage(userInput);
        userMessage.setAgent("user");
        memoryManager.addMessage(userMessage);

        // auto-compact if estimated token usage exceeds context window threshold
        autoCompactIfNeeded();

        return chatWithNativeFunctionCalling();
    }

    /**
     * 检查是否需要自动压缩对话历史（委托给 HistoryCompactor）
     */
    private void autoCompactIfNeeded() {
        historyCompactor.autoCompactIfNeeded(buildConversationMessages());
    }

    /**
     * 获取 Token 使用统计
     */
    public Map<String, Long> getTokenUsage() {
        Map<String, Long> usage = new LinkedHashMap<>();
        usage.put("prompt_tokens", totalPromptTokens);
        usage.put("completion_tokens", totalCompletionTokens);
        usage.put("total_tokens", totalPromptTokens + totalCompletionTokens);
        return usage;
    }

    /**
     * 重置 Token 使用统计
     */
    public void resetTokenUsage() {
        totalPromptTokens = 0;
        totalCompletionTokens = 0;
    }

    /**
     * 清除对话历史
     */
    public void clearHistory() {
        memoryManager.clearShortTerm();
        resetTokenUsage();
    }

    /**
     * 压缩对话历史（委托给 HistoryCompactor）
     */
    public void compactHistory() {
        historyCompactor.compact();
    }

    // ==================== Native Function Calling ====================

    /**
     * 使用 LLM 原生 JSON Function Calling 进行流式工具调用循环。
     * 文本 token 实时输出到终端，ToolCall 增量在后台拼接，
     * 流结束后统一执行工具调用并将结果反馈给 LLM 进入下一轮迭代。
     */
    private String chatWithNativeFunctionCalling() {
        List<Message> conversationMessages = buildConversationMessages();
        List<ToolDefinition> toolDefinitions = getToolDefinitions();

        int iteration = 0;
        while (iteration < config.getMaxIterations()) {
            iteration++;

            ChatCompletionResult result = streamCollector.collectWithRetry(conversationMessages, toolDefinitions);

            if (result == null) {
                return emitAndStore("I encountered an issue generating a response after multiple attempts. Please try again.");
            }

            trackTokenUsage(result);

            // LLM returned text only — final response
            if (result.isTextResponse()) {
                Message assistantMessage = Message.outputMessage(result.getContent());
                assistantMessage.setAgent("claudecode");
                memoryManager.addMessage(assistantMessage);
                return result.getContent();
            }

            // LLM wants to call tools
            if (result.hasToolCalls()) {
                Message assistantMessage = Message.responseMessage(
                        result.getContent() != null ? result.getContent() : "", "claudecode", "tool_call");
                assistantMessage.putMetadata("tool_calls", result.getToolCalls());
                conversationMessages.add(assistantMessage);

                List<ToolCall> toolCalls = result.getToolCalls();

                if (toolCalls.size() == 1) {
                    ToolCall toolCall = toolCalls.get(0);
                    String toolName = toolCall.getFunction().getName();
                    Map<String, Object> parameters = toolExecutor.parseToolArguments(toolCall.getFunction().getArguments());

                    emitStream("\n🔧 " + toolName + "(" + toolExecutor.summarizeParams(parameters) + ")\n");

                    // Skill 上下文注入（对齐 Claude Code 的双消息注入机制）
                    if (SkillTool.TOOL_NAME.equals(toolName)) {
                        String toolResultContent = toolExecutor.executeWithPermission(toolName, parameters);
                        handleSkillActivation(parameters, conversationMessages);
                        Message toolResultMessage = Message.responseMessage(toolResultContent, "claudecode", "tool_result");
                        toolResultMessage.putMetadata("tool_call_id", toolCall.getId());
                        conversationMessages.add(toolResultMessage);
                    } else {
                        String toolResultContent = toolExecutor.executeWithPermission(toolName, parameters);
                        Message toolResultMessage = Message.responseMessage(toolResultContent, "claudecode", "tool_result");
                        toolResultMessage.putMetadata("tool_call_id", toolCall.getId());
                        conversationMessages.add(toolResultMessage);
                    }
                } else {
                    // multiple tool calls — prepare and delegate to ToolExecutor for parallel execution
                    List<String> toolNames = new ArrayList<>();
                    List<Map<String, Object>> parametersList = new ArrayList<>();
                    for (ToolCall toolCall : toolCalls) {
                        toolNames.add(toolCall.getFunction().getName());
                        parametersList.add(toolExecutor.parseToolArguments(toolCall.getFunction().getArguments()));
                    }

                    List<String> results = toolExecutor.executeInParallel(toolNames, parametersList);

                    for (int i = 0; i < toolCalls.size(); i++) {
                        Message toolResultMessage = Message.responseMessage(
                                results.get(i), "claudecode", "tool_result");
                        toolResultMessage.putMetadata("tool_call_id", toolCalls.get(i).getId());
                        conversationMessages.add(toolResultMessage);
                    }
                }

                continue;
            }

            // unexpected: no content and no tool calls
            return emitAndStore("I received an unexpected response. Please try again.");
        }

        return emitAndStore("Reached maximum iteration limit (" + config.getMaxIterations()
                + "). Please provide more specific instructions.");
    }

    /**
     * 追踪 Token 使用量
     */
    private void trackTokenUsage(ChatCompletionResult result) {
        if (result != null && result.getUsage() != null) {
            totalPromptTokens += result.getUsage().getPromptTokens();
            totalCompletionTokens += result.getUsage().getCompletionTokens();
        }
    }

    /**
     * 获取工具定义列表（带缓存）
     */
    private List<ToolDefinition> getToolDefinitions() {
        if (cachedToolDefinitions == null) {
            cachedToolDefinitions = toolRegistry.getToolSchemas().stream()
                    .map(ToolDefinition::fromToolSchema)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            log.debug("Built {} tool definitions for function calling", cachedToolDefinitions.size());
        }
        return cachedToolDefinitions;
    }

    /**
     * 使缓存的工具定义失效（当工具列表变化时调用，如 MCP 工具动态注册）
     */
    public void invalidateToolDefinitionCache() {
        cachedToolDefinitions = null;
        log.debug("Tool definition cache invalidated");
    }

    /**
     * 输出流式文本并存储到记忆
     */
    private String emitAndStore(String response) {
        emitStream(response);
        Message assistantMessage = Message.outputMessage(response);
        assistantMessage.setAgent("claudecode");
        memoryManager.addMessage(assistantMessage);
        return response;
    }

    private void initializeProjectContext() {
        projectContext.scanProject();
        projectContext.loadProjectRules(config.getProjectRulesFileName());
    }

    /**
     * 初始化任务委派执行器（带递归深度限制）
     */
    private void initializeTaskExecutor() {
        var taskDelegationTool = toolRegistry.getTaskDelegationTool();
        if (taskDelegationTool != null) {
            int nextDepth = currentDepth + 1;
            int maxDepth = config.getMaxSubAgentDepth();

            taskDelegationTool.setExecutor((taskDescription, taskPrompt) -> {
                if (nextDepth > maxDepth) {
                    String errorMessage = "Task delegation rejected: maximum recursion depth ("
                            + maxDepth + ") exceeded at depth " + currentDepth
                            + ". Please handle this task directly.";
                    log.warn(errorMessage);
                    return errorMessage;
                }

                log.info("Delegating task at depth {}/{}: {}", nextDepth, maxDepth, taskDescription);

                PermissionManager childPermissionManager = new PermissionManager(config, (toolName, params) ->
                        permissionManager.checkPermission(toolName, params));

                CodingAgent childAgent = new CodingAgent(config, childPermissionManager, nextDepth);
                return childAgent.chat(taskPrompt);
            });
            log.info("Task delegation executor initialized (current depth: {}, max depth: {})", currentDepth, maxDepth);
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

        if (config.getSystemPrompt() != null && !config.getSystemPrompt().isBlank()) {
            prompt.append(config.getSystemPrompt());
        } else {
            prompt.append(getDefaultSystemPrompt());
        }

        String contextSummary = projectContext.toContextSummary();
        if (!contextSummary.isBlank()) {
            prompt.append("\n\n").append(contextSummary);
        }

        return prompt.toString();
    }

    private String getDefaultSystemPrompt() {
        return loadResourceFile("default-system-prompt.txt");
    }

    /**
     * 从 classpath 资源文件加载文本内容
     */
    private String loadResourceFile(String resourceName) {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            if (inputStream == null) {
                log.warn("Resource file not found: {}, using fallback prompt", resourceName);
                return "You are an expert software engineer working as a coding assistant.";
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            log.error("Failed to load resource file: {}", resourceName, e);
            return "You are an expert software engineer working as a coding assistant.";
        }
    }

    /**
     * 处理 Skill 激活后的上下文注入。
     * 对齐 Claude Code 的双消息注入机制：
     * <ol>
     *   <li>Message 1 (用户可见): 状态消息，告知用户 Skill 已激活</li>
     *   <li>Message 2 (隐藏/meta): Skill prompt 注入，LLM 可见但用户不可见</li>
     * </ol>
     * 同时处理执行上下文修改：预批准 allowed-tools。
     *
     * <p>直接从 SkillRegistry 获取 SkillActivationResult，
     * 而不是解析 ToolExecutor 的文本输出，确保数据完整性。
     *
     * @param parameters SkillTool 的调用参数（包含 "command" 字段）
     * @param conversationMessages 当前对话消息列表
     */
    private void handleSkillActivation(Map<String, Object> parameters, List<Message> conversationMessages) {
        try {
            String command = parameters.getOrDefault("command", "").toString().trim().replaceFirst("^/", "");
            if (command.isBlank()) {
                return;
            }

            SkillActivationResult activation =
                    toolRegistry.getSkillRegistry().activateSkill(command);

            if (activation == null || !activation.isSuccess()) {
                return;
            }

            // Message 1: 用户可见的状态消息
            String metadataMessage = activation.getMetadataMessage();
            if (metadataMessage != null && !metadataMessage.isBlank()) {
                emitStream("\n✨ " + metadataMessage + "\n");
            }

            // Message 2: 隐藏的 Skill prompt 注入（对齐 Claude Code 的 isMeta=true 消息）
            String skillPrompt = activation.getSkillPrompt();
            if (skillPrompt != null && !skillPrompt.isBlank()) {
                Message skillPromptMessage = Message.systemMessage(skillPrompt);
                skillPromptMessage.putMetadata("isMeta", true);
                skillPromptMessage.putMetadata("skillName", activation.getSkillName());
                conversationMessages.add(skillPromptMessage);
                log.info("Injected Skill prompt for '{}' ({} chars)", activation.getSkillName(), skillPrompt.length());
            }

            // 执行上下文修改：预批准 allowed-tools
            List<String> allowedTools = activation.getAllowedTools();
            if (allowedTools != null && !allowedTools.isEmpty()) {
                permissionManager.preApproveToolsForSkill(allowedTools);
                log.info("Pre-approved {} tools for Skill '{}'", allowedTools.size(), activation.getSkillName());
            }

        } catch (Exception e) {
            log.warn("Failed to process Skill activation: {}", e.getMessage());
        }
    }

    private void emitStream(String text) {
        if (streamCallback != null) {
            streamCallback.accept(text);
        }
    }

}
