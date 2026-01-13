package io.leavesfly.evox.agents.specialized;

import io.leavesfly.evox.agents.base.Agent;
import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.core.message.MessageType;
import io.leavesfly.evox.models.base.BaseLLM;
import io.leavesfly.evox.models.config.LLMConfig;
import io.leavesfly.evox.tools.base.BaseTool;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ToolAwareAgent - 工具感知代理
 * 能够识别和使用工具的智能代理
 * 集成LLM进行工具选择和参数提取
 * 
 * @author EvoX Team
 */
@Data
@Slf4j
@EqualsAndHashCode(callSuper = true)
public class ToolAwareAgent extends Agent {

    /**
     * 可用工具列表
     */
    private List<BaseTool> tools = new ArrayList<>();

    /**
     * 工具映射表
     */
    private transient Map<String, BaseTool> toolMap = new ConcurrentHashMap<>();

    /**
     * 默认系统提示词
     */
    private static final String DEFAULT_SYSTEM_PROMPT =
        "You are a helpful AI agent with access to various tools. " +
        "When given a task, analyze it and determine which tool(s) to use. " +
        "For each tool call, extract the required parameters from the user's input.\n\n" +
        "Available tools:\n{tool_descriptions}\n\n" +
        "Output your tool calls in this format:\n" +
        "TOOL: <tool_name>\n" +
        "PARAMS: {\"param1\": \"value1\", \"param2\": \"value2\"}\n" +
        "END_TOOL";

    /**
     * 是否自动执行工具
     */
    private boolean autoExecuteTools = true;

    /**
     * 最大工具调用次数
     */
    private int maxToolCalls = 5;

    /**
     * 构建器构造函数
     */
    @Builder
    public ToolAwareAgent(
            String name,
            String description,
            String systemPrompt,
            LLMConfig llmConfig,
            BaseLLM llm,
            List<BaseTool> tools,
            Boolean autoExecuteTools,
            Integer maxToolCalls
    ) {
        this.setName(name != null ? name : "ToolAwareAgent");
        this.setDescription(description != null ? description :
            "An intelligent agent that can select and use appropriate tools to accomplish tasks");
        this.setSystemPrompt(systemPrompt != null ? systemPrompt : buildSystemPrompt(tools));
        this.setLlmConfig(llmConfig);
        this.setLlm(llm);
        this.autoExecuteTools = autoExecuteTools != null ? autoExecuteTools : true;
        this.maxToolCalls = maxToolCalls != null ? maxToolCalls : 5;
        this.setHuman(false);

        // 初始化工具
        if (tools != null) {
            this.tools = new ArrayList<>(tools);
            for (BaseTool tool : tools) {
                toolMap.put(tool.getName(), tool);
            }
        }
    }

    @Override
    public Message execute(String actionName, List<Message> messages) {
        log.debug("ToolAwareAgent {} executing with {} tools available", getName(), tools.size());

        try {
            // 提取用户输入
            String userInput = extractUserInput(messages);
            
            if (userInput == null || userInput.trim().isEmpty()) {
                throw new IllegalArgumentException("No input provided");
            }

            // 使用LLM进行工具选择和参数提取
            BaseLLM llmInstance = getLlm();
            if (llmInstance == null) {
                throw new IllegalStateException("LLM not initialized for ToolAwareAgent");
            }

            // 构造LLM消息
            List<Message> llmMessages = new ArrayList<>();
            llmMessages.add(Message.builder()
                    .messageType(MessageType.SYSTEM)
                    .content(getSystemPrompt())
                    .build());
            llmMessages.add(Message.builder()
                    .messageType(MessageType.INPUT)
                    .content(userInput)
                    .build());

            // 获取LLM响应
            String llmResponse = llmInstance.chat(llmMessages);

            // 解析工具调用
            List<ToolCall> toolCalls = parseToolCalls(llmResponse);

            // 执行工具调用
            StringBuilder resultBuilder = new StringBuilder();
            resultBuilder.append("Tool execution results:\n\n");

            for (ToolCall toolCall : toolCalls) {
                if (autoExecuteTools) {
                    BaseTool.ToolResult result = executeTool(toolCall);
                    resultBuilder.append("Tool: ").append(toolCall.getToolName()).append("\n");
                    resultBuilder.append("Result: ").append(result.getData()).append("\n");
                    resultBuilder.append("Success: ").append(result.isSuccess()).append("\n\n");
                } else {
                    resultBuilder.append("Tool call planned: ").append(toolCall.getToolName()).append("\n");
                    resultBuilder.append("Parameters: ").append(toolCall.getParameters()).append("\n\n");
                }
            }

            return Message.builder()
                    .messageType(MessageType.RESPONSE)
                    .content(resultBuilder.toString())
                    .build();

        } catch (Exception e) {
            log.error("Error in ToolAwareAgent execution: {}", e.getMessage(), e);
            return Message.builder()
                    .messageType(MessageType.RESPONSE)
                    .content("Error: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 从消息中提取用户输入
     */
    private String extractUserInput(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return null;
        }

        Message lastMessage = messages.get(messages.size() - 1);
        Object content = lastMessage.getContent();
        
        return content != null ? String.valueOf(content) : null;
    }

    /**
     * 构建系统提示词
     */
    private String buildSystemPrompt(List<BaseTool> tools) {
        if (tools == null || tools.isEmpty()) {
            return DEFAULT_SYSTEM_PROMPT.replace("{tool_descriptions}", "No tools available");
        }

        StringBuilder toolDescs = new StringBuilder();
        for (BaseTool tool : tools) {
            toolDescs.append("- ").append(tool.getName()).append(": ")
                    .append(tool.getDescription()).append("\n");
        }

        return DEFAULT_SYSTEM_PROMPT.replace("{tool_descriptions}", toolDescs.toString());
    }

    /**
     * 解析工具调用
     */
    private List<ToolCall> parseToolCalls(String llmResponse) {
        List<ToolCall> toolCalls = new ArrayList<>();
        
        if (llmResponse == null || llmResponse.trim().isEmpty()) {
            return toolCalls;
        }

        // 简单解析：查找 TOOL: 和 PARAMS: 块
        String[] lines = llmResponse.split("\n");
        String currentTool = null;
        Map<String, Object> currentParams = new HashMap<>();

        for (String line : lines) {
            String trimmed = line.trim();
            
            if (trimmed.startsWith("TOOL:")) {
                currentTool = trimmed.substring(5).trim();
            } else if (trimmed.startsWith("PARAMS:")) {
                // 简化处理：假设参数已经是可解析的格式
                String paramsStr = trimmed.substring(7).trim();
                // 这里应该用JSON解析，简化为直接使用
                currentParams = new HashMap<>();
                currentParams.put("raw_params", paramsStr);
            } else if (trimmed.equals("END_TOOL") && currentTool != null) {
                toolCalls.add(ToolCall.builder()
                        .toolName(currentTool)
                        .parameters(currentParams)
                        .build());
                
                currentTool = null;
                currentParams = new HashMap<>();
                
                if (toolCalls.size() >= maxToolCalls) {
                    break;
                }
            }
        }

        return toolCalls;
    }

    /**
     * 执行工具调用
     */
    private BaseTool.ToolResult executeTool(ToolCall toolCall) {
        BaseTool tool = toolMap.get(toolCall.getToolName());
        
        if (tool == null) {
            return BaseTool.ToolResult.failure("Tool not found: " + toolCall.getToolName());
        }

        try {
            return tool.execute(toolCall.getParameters());
        } catch (Exception e) {
            log.error("Error executing tool {}: {}", toolCall.getToolName(), e.getMessage(), e);
            return BaseTool.ToolResult.failure("Tool execution error: " + e.getMessage());
        }
    }

    /**
     * 添加工具
     */
    public void addTool(BaseTool tool) {
        if (tool != null) {
            tools.add(tool);
            toolMap.put(tool.getName(), tool);
            // 更新系统提示词
            setSystemPrompt(buildSystemPrompt(tools));
            log.debug("Added tool {} to agent {}", tool.getName(), getName());
        }
    }

    /**
     * 移除工具
     */
    public void removeTool(String toolName) {
        BaseTool removed = toolMap.remove(toolName);
        if (removed != null) {
            tools.remove(removed);
            // 更新系统提示词
            setSystemPrompt(buildSystemPrompt(tools));
            log.debug("Removed tool {} from agent {}", toolName, getName());
        }
    }

    /**
     * 工具调用
     */
    @Data
    @Builder
    public static class ToolCall {
        /**
         * 工具名称
         */
        private String toolName;

        /**
         * 工具参数
         */
        private Map<String, Object> parameters;
    }
}
