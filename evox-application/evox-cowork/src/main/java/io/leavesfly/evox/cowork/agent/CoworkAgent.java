package io.leavesfly.evox.cowork.agent;

import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.cowork.config.CoworkConfig;
import io.leavesfly.evox.cowork.context.CoworkContext;
import io.leavesfly.evox.cowork.permission.CoworkPermissionManager;
import io.leavesfly.evox.cowork.tool.CoworkToolRegistry;
import io.leavesfly.evox.tools.agent.SubAgentTool;
import io.leavesfly.evox.memory.manager.MemoryManager;
import io.leavesfly.evox.memory.shortterm.ShortTermMemory;
import io.leavesfly.evox.models.base.BaseLLM;
import io.leavesfly.evox.models.factory.LLMFactory;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cowork 智能代理核心类
 * 负责协调 LLM、工具注册表、权限管理器和上下文，执行对话和任务。
 */
@Slf4j
public class CoworkAgent {

    private static final Pattern TOOL_CALL_PATTERN = Pattern.compile(
            "<tool_call>\\s*<name>(.*?)</name>\\s*<parameters>(.*?)</parameters>\\s*</tool_call>", Pattern.DOTALL);
    private static final Pattern PARAM_PATTERN = Pattern.compile("<(\\w+)>(.*?)</\\1>", Pattern.DOTALL);

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
    private final BaseLLM llm;
    private Consumer<String> streamCallback;
    private final TaskProgress currentProgress;

    public CoworkAgent(CoworkConfig config, CoworkPermissionManager permissionManager, 
                       CoworkToolRegistry toolRegistry, CoworkContext coworkContext) {
        this.config = config;
        this.permissionManager = permissionManager;
        this.toolRegistry = toolRegistry;
        this.coworkContext = coworkContext;
        this.memoryManager = new MemoryManager(new ShortTermMemory(config.getMaxHistoryMessages()));
        this.llm = LLMFactory.create(config.getLlmConfig());
        this.currentProgress = new TaskProgress();
        initializeSubAgentExecutor();
    }

    public void setStreamCallback(Consumer<String> callback) {
        this.streamCallback = callback;
    }

    /**
     * 处理用户输入并进行对话
     * 包含核心的 ReAct (Reasoning + Acting) 循环
     *
     * @param userInput 用户输入文本
     * @return 代理的最终响应
     */
    public String chat(String userInput) {
        Message userMessage = Message.inputMessage(userInput);
        userMessage.setAgent("user");
        memoryManager.addMessage(userMessage);

        List<Message> conversationMessages = buildConversationMessages();

        for (int iteration = 0; iteration < config.getMaxIterations(); iteration++) {
            String llmResponse = llm.chat(conversationMessages);

            if (llmResponse == null || llmResponse.trim().isEmpty()) {
                emitStream("Error: LLM returned empty response");
                return "Error: No response from LLM";
            }

            List<ToolCallRequest> toolCalls = parseToolCalls(llmResponse);

            if (toolCalls.isEmpty()) {
                String cleanedResponse = cleanResponse(llmResponse);
                emitStream(cleanedResponse);
                Message assistantMessage = Message.responseMessage(cleanedResponse, "assistant", "chat");
                memoryManager.addMessage(assistantMessage);
                return cleanedResponse;
            }

            String textBeforeTools = extractTextBeforeTools(llmResponse);
            if (!textBeforeTools.isEmpty()) {
                emitStream(textBeforeTools);
            }

            Message assistantMessage = Message.responseMessage(llmResponse, "assistant", "tool_call");
            memoryManager.addMessage(assistantMessage);

            StringBuilder toolResults = new StringBuilder();
            toolResults.append("<tool_results>");

            for (ToolCallRequest toolCall : toolCalls) {
                emitStream(String.format("\n[Executing tool: %s with params: %s]", 
                    toolCall.toolName(), summarizeParams(toolCall.parameters())));

                if (!permissionManager.checkPermission(toolCall.toolName(), toolCall.parameters())) {
                    String deniedResult = String.format("Permission denied for tool: %s", toolCall.toolName());
                    toolResults.append(String.format("<tool_result name=\"%s\"><![CDATA[%s]]></tool_result>",
                        toolCall.toolName(), deniedResult));
                    continue;
                }

                try {
                    Object result = toolRegistry.executeTool(toolCall.toolName(), toolCall.parameters());
                    String formattedResult = formatToolResult(result);
                    toolResults.append(String.format("<tool_result name=\"%s\"><![CDATA[%s]]></tool_result>",
                        toolCall.toolName(), formattedResult));
                    
                    currentProgress.updateStep(String.format("Executed %s", toolCall.toolName()));
                    emitProgress(String.format("✓ Completed: %s", toolCall.toolName()));
                } catch (Exception e) {
                    String errorResult = String.format("Error executing %s: %s", toolCall.toolName(), e.getMessage());
                    toolResults.append(String.format("<tool_result name=\"%s\"><![CDATA[%s]]></tool_result>",
                        toolCall.toolName(), errorResult));
                    log.error("Tool execution error", e);
                }
            }

            toolResults.append("</tool_results>");
            Message toolResultMessage = Message.responseMessage(toolResults.toString(), "system", "tool_result");
            memoryManager.addMessage(toolResultMessage);
            conversationMessages.add(toolResultMessage);
        }

        return "Error: Maximum iterations reached without completion";
    }

    public String executeSubTask(String taskDescription, String taskPrompt) {
        CoworkAgent subAgent = new CoworkAgent(config, permissionManager, toolRegistry, coworkContext);
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

        prompt.append("Available Tools:\n").append(toolRegistry.generateToolDescriptions()).append("\n\n");
        prompt.append(getToolCallingInstructions());

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

    private String getToolCallingInstructions() {
        return "When you need to use a tool, format your response as follows:\n" +
            "<tool_call>\n" +
            "  <name>tool_name</name>\n" +
            "  <parameters>\n" +
            "    <param1>value1</param1>\n" +
            "    <param2>value2</param2>\n" +
            "  </parameters>\n" +
            "</tool_call>\n\n" +
            "You can make multiple tool calls in a single response. Each tool call should be wrapped in its own <tool_call> tag.\n" +
            "Tool results will be provided in <tool_result> tags for your reference.";
    }

    private List<ToolCallRequest> parseToolCalls(String response) {
        List<ToolCallRequest> toolCalls = new ArrayList<>();
        Matcher matcher = TOOL_CALL_PATTERN.matcher(response);

        while (matcher.find()) {
            String toolName = matcher.group(1).trim();
            String paramsXml = matcher.group(2);
            Map<String, Object> parameters = parseParameters(paramsXml);
            toolCalls.add(new ToolCallRequest(toolName, parameters));
        }

        return toolCalls;
    }

    private Map<String, Object> parseParameters(String paramsXml) {
        Map<String, Object> parameters = new HashMap<>();
        Matcher paramMatcher = PARAM_PATTERN.matcher(paramsXml);

        while (paramMatcher.find()) {
            String paramName = paramMatcher.group(1);
            String paramValue = paramMatcher.group(2).trim();

            Object value = paramValue;
            
            if ("true".equalsIgnoreCase(paramValue) || "false".equalsIgnoreCase(paramValue)) {
                value = Boolean.parseBoolean(paramValue);
            } else {
                try {
                    if (paramValue.matches("-?\\d+")) {
                        value = Integer.parseInt(paramValue);
                    } else if (paramValue.matches("-?\\d+\\.\\d+")) {
                        value = Double.parseDouble(paramValue);
                    }
                } catch (NumberFormatException e) {
                    value = paramValue;
                }
            }

            parameters.put(paramName, value);
        }

        return parameters;
    }

    private String extractTextBeforeTools(String response) {
        int toolCallIndex = response.indexOf("<tool_call>");
        if (toolCallIndex == -1) {
            return response;
        }
        return response.substring(0, toolCallIndex).trim();
    }

    private String cleanResponse(String response) {
        return response.replaceAll("<tool_call>.*?</tool_call>", "")
                      .replaceAll("<tool_results>.*?</tool_results>", "")
                      .trim();
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

    public record ToolCallRequest(String toolName, Map<String, Object> parameters) {}

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
