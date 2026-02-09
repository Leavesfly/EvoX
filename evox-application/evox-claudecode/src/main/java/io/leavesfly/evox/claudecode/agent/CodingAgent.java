package io.leavesfly.evox.claudecode.agent;

import io.leavesfly.evox.claudecode.config.ClaudeCodeConfig;
import io.leavesfly.evox.claudecode.context.ProjectContext;
import io.leavesfly.evox.claudecode.permission.PermissionManager;
import io.leavesfly.evox.claudecode.tool.ToolRegistry;
import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.core.message.MessageType;
import io.leavesfly.evox.memory.manager.MemoryManager;
import io.leavesfly.evox.memory.shortterm.ShortTermMemory;
import io.leavesfly.evox.models.base.BaseLLM;
import io.leavesfly.evox.models.factory.LLMFactory;
import io.leavesfly.evox.tools.base.BaseTool;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.Consumer;
import java.util.regex.*;

/**
 * 编码智能体
 * 实现 Function Calling 循环：用户输入 → LLM 思考 → 工具调用 → 结果反馈 → LLM 继续
 * 这是 ClaudeCode 的核心引擎
 */
@Slf4j
public class CodingAgent {

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

    private final BaseLLM llm;
    private Consumer<String> streamCallback;

    // tool call parsing patterns
    private static final Pattern TOOL_CALL_PATTERN = Pattern.compile(
            "<tool_call>\\s*<name>(.*?)</name>\\s*<parameters>(.*?)</parameters>\\s*</tool_call>",
            Pattern.DOTALL
    );
    private static final Pattern PARAM_PATTERN = Pattern.compile(
            "<(\\w+)>(.*?)</\\1>",
            Pattern.DOTALL
    );

    public CodingAgent(ClaudeCodeConfig config, PermissionManager permissionManager) {
        this.config = config;
        this.permissionManager = permissionManager;
        this.toolRegistry = new ToolRegistry(config.getWorkingDirectory());
        this.memoryManager = new MemoryManager(new ShortTermMemory(config.getMaxHistoryMessages()));
        this.projectContext = new ProjectContext(config.getWorkingDirectory());
        this.llm = LLMFactory.create(config.getLlmConfig());

        initializeProjectContext();
        initializeSubAgentExecutor();
    }

    /**
     * 设置流式输出回调
     */
    public void setStreamCallback(Consumer<String> callback) {
        this.streamCallback = callback;
    }

    /**
     * 处理用户输入，执行 Function Calling 循环
     *
     * @param userInput 用户输入
     * @return Agent 最终回复
     */
    public String chat(String userInput) {
        // add user message to memory
        Message userMessage = Message.inputMessage(userInput);
        userMessage.setAgent("user");
        memoryManager.addMessage(userMessage);

        // build conversation messages for LLM
        List<Message> conversationMessages = buildConversationMessages();

        // function calling loop
        int iteration = 0;
        while (iteration < config.getMaxIterations()) {
            iteration++;

            String llmResponse = llm.chat(conversationMessages);

            if (llmResponse == null || llmResponse.isBlank()) {
                return "I encountered an issue generating a response. Please try again.";
            }

            // check if LLM wants to call tools
            List<ToolCallRequest> toolCalls = parseToolCalls(llmResponse);

            if (toolCalls.isEmpty()) {
                // no tool calls - this is the final response
                String finalResponse = cleanResponse(llmResponse);
                emitStream(finalResponse);

                Message assistantMessage = Message.outputMessage(finalResponse);
                assistantMessage.setAgent("claudecode");
                memoryManager.addMessage(assistantMessage);

                return finalResponse;
            }

            // execute tool calls and collect results
            StringBuilder toolResultsText = new StringBuilder();
            String textBeforeTools = extractTextBeforeTools(llmResponse);
            if (!textBeforeTools.isBlank()) {
                emitStream(textBeforeTools + "\n");
            }

            // add assistant message with tool calls to conversation
            Message assistantToolMessage = Message.responseMessage(llmResponse, "claudecode", "tool_call");
            conversationMessages.add(assistantToolMessage);

            for (ToolCallRequest toolCall : toolCalls) {
                emitStream("\n🔧 " + toolCall.toolName + "(" + summarizeParams(toolCall.parameters) + ")\n");

                // check permission
                if (!permissionManager.checkPermission(toolCall.toolName, toolCall.parameters)) {
                    String deniedResult = "Tool call denied by user: " + toolCall.toolName;
                    toolResultsText.append("<tool_result>\n<name>").append(toolCall.toolName)
                            .append("</name>\n<result>").append(deniedResult)
                            .append("</result>\n</tool_result>\n");
                    emitStream("  ❌ " + deniedResult + "\n");
                    continue;
                }

                // execute tool
                BaseTool.ToolResult result = toolRegistry.executeTool(toolCall.toolName, toolCall.parameters);

                String resultText;
                if (result.isSuccess()) {
                    resultText = formatToolResult(result.getData());
                    emitStream("  ✅ Success\n");
                } else {
                    resultText = "Error: " + result.getError();
                    emitStream("  ❌ " + resultText + "\n");
                }

                toolResultsText.append("<tool_result>\n<name>").append(toolCall.toolName)
                        .append("</name>\n<result>").append(resultText)
                        .append("</result>\n</tool_result>\n");
            }

            // add tool results as a new message and continue the loop
            Message toolResultMessage = Message.inputMessage(toolResultsText.toString());
            toolResultMessage.setAgent("system");
            toolResultMessage.setAction("tool_result");
            conversationMessages.add(toolResultMessage);
        }

        String maxIterationResponse = "Reached maximum iteration limit (" + config.getMaxIterations()
                + "). Please provide more specific instructions.";
        emitStream(maxIterationResponse);
        return maxIterationResponse;
    }

    /**
     * 清除对话历史
     */
    public void clearHistory() {
        memoryManager.clearShortTerm();
    }

    /**
     * 压缩对话历史（保留关键信息）
     */
    public void compactHistory() {
        List<Message> allMessages = memoryManager.getAllMessages();
        if (allMessages.size() <= 10) {
            return;
        }

        // keep system message, first 2 and last 8 messages
        List<Message> compacted = new ArrayList<>();
        if (allMessages.size() > 10) {
            compacted.addAll(allMessages.subList(0, 2));

            // add a summary marker
            Message summaryMarker = Message.systemMessage(
                    "[Previous conversation history has been compacted. " + (allMessages.size() - 10)
                            + " messages were summarized.]"
            );
            compacted.add(summaryMarker);

            compacted.addAll(allMessages.subList(allMessages.size() - 8, allMessages.size()));
        } else {
            compacted.addAll(allMessages);
        }

        memoryManager.clearShortTerm();
        memoryManager.addMessages(compacted);
    }

    private void initializeProjectContext() {
        projectContext.loadProjectRules(config.getProjectRulesFileName());
    }

    /**
     * 初始化子代理执行器
     * 将 SubAgentTool 的 executor 绑定为创建独立 CodingAgent 子实例
     */
    private void initializeSubAgentExecutor() {
        var subAgentTool = toolRegistry.getSubAgentTool();
        if (subAgentTool != null) {
            subAgentTool.setExecutor((taskDescription, taskPrompt) -> {
                log.info("Sub-agent executing: {}", taskDescription);

                // create a child CodingAgent with shared config but independent history
                PermissionManager childPermissionManager = new PermissionManager(config, (toolName, params) -> {
                    // sub-agents inherit the parent's permission approvals
                    return permissionManager.checkPermission(toolName, params);
                });

                CodingAgent childAgent = new CodingAgent(config, childPermissionManager);
                return childAgent.chat(taskPrompt);
            });
            log.info("Sub-agent executor initialized");
        }
    }

    private List<Message> buildConversationMessages() {
        List<Message> messages = new ArrayList<>();

        // system prompt
        String systemPrompt = buildSystemPrompt();
        messages.add(Message.systemMessage(systemPrompt));

        // conversation history
        messages.addAll(memoryManager.getAllMessages());

        return messages;
    }

    private String buildSystemPrompt() {
        StringBuilder prompt = new StringBuilder();

        // base system prompt
        if (config.getSystemPrompt() != null && !config.getSystemPrompt().isBlank()) {
            prompt.append(config.getSystemPrompt()).append("\n\n");
        } else {
            prompt.append(getDefaultSystemPrompt()).append("\n\n");
        }

        // project context
        prompt.append(projectContext.toContextSummary()).append("\n\n");

        // tool descriptions
        prompt.append(toolRegistry.generateToolDescriptions()).append("\n\n");

        // tool calling format
        prompt.append(getToolCallingInstructions());

        return prompt.toString();
    }

    private String getDefaultSystemPrompt() {
        return """
                You are an expert software engineer working as a coding assistant.
                You have access to tools that let you read, search, and edit files, run shell commands, and manage git.
                You can also delegate complex tasks to sub-agents and use specialized skills.
                
                Key principles:
                1. Always read files before editing them to understand the current content.
                2. Use grep/glob to search the codebase before making assumptions.
                3. Make precise, minimal edits using the file_edit tool.
                4. Run tests after making changes when appropriate.
                5. Explain your reasoning and what you're doing.
                6. If you're unsure, ask the user for clarification.
                7. For complex multi-step tasks, use the sub_agent tool to delegate independent subtasks.
                8. For specialized tasks like code review, writing tests, or refactoring, use the skill tool.
                
                When you need to use a tool, wrap it in the tool call format specified below.
                When you have completed the task or want to respond to the user, just write your response directly without tool calls.""";
    }

    private String getToolCallingInstructions() {
        return """
                ## Tool Calling Format
                
                When you need to use a tool, use this XML format:
                
                <tool_call>
                <name>tool_name</name>
                <parameters>
                <param_name>param_value</param_name>
                </parameters>
                </tool_call>
                
                You can make multiple tool calls in a single response. After each tool call, you will receive the results.
                Continue calling tools until you have enough information to provide a complete answer.
                When you are done, provide your final response without any tool calls.""";
    }

    private List<ToolCallRequest> parseToolCalls(String response) {
        List<ToolCallRequest> toolCalls = new ArrayList<>();
        Matcher matcher = TOOL_CALL_PATTERN.matcher(response);

        while (matcher.find()) {
            String toolName = matcher.group(1).trim();
            String parametersXml = matcher.group(2).trim();

            Map<String, Object> parameters = new LinkedHashMap<>();
            Matcher paramMatcher = PARAM_PATTERN.matcher(parametersXml);
            while (paramMatcher.find()) {
                String paramName = paramMatcher.group(1);
                String paramValue = paramMatcher.group(2).trim();

                // try to parse as boolean or number
                if ("true".equalsIgnoreCase(paramValue) || "false".equalsIgnoreCase(paramValue)) {
                    parameters.put(paramName, Boolean.parseBoolean(paramValue));
                } else {
                    try {
                        parameters.put(paramName, Integer.parseInt(paramValue));
                    } catch (NumberFormatException e) {
                        parameters.put(paramName, paramValue);
                    }
                }
            }

            toolCalls.add(new ToolCallRequest(toolName, parameters));
        }

        return toolCalls;
    }

    private String extractTextBeforeTools(String response) {
        int firstToolCallIndex = response.indexOf("<tool_call>");
        if (firstToolCallIndex > 0) {
            return response.substring(0, firstToolCallIndex).trim();
        }
        return "";
    }

    private String cleanResponse(String response) {
        // remove any stray tool_call or tool_result tags
        return response
                .replaceAll("<tool_call>.*?</tool_call>", "")
                .replaceAll("<tool_result>.*?</tool_result>", "")
                .trim();
    }

    private String formatToolResult(Object data) {
        if (data == null) {
            return "(no data)";
        }
        if (data instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) data;
            StringBuilder formatted = new StringBuilder();
            map.forEach((key, value) -> {
                String valueStr = value != null ? value.toString() : "null";
                // truncate very long values
                if (valueStr.length() > 5000) {
                    valueStr = valueStr.substring(0, 5000) + "\n... (truncated, " + valueStr.length() + " chars total)";
                }
                formatted.append(key).append(": ").append(valueStr).append("\n");
            });
            return formatted.toString().stripTrailing();
        }
        String result = data.toString();
        if (result.length() > 5000) {
            result = result.substring(0, 5000) + "\n... (truncated, " + result.length() + " chars total)";
        }
        return result;
    }

    private String summarizeParams(Map<String, Object> parameters) {
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
        if (streamCallback != null) {
            streamCallback.accept(text);
        }
    }

    /**
     * 工具调用请求
     */
    private record ToolCallRequest(String toolName, Map<String, Object> parameters) {
    }
}
