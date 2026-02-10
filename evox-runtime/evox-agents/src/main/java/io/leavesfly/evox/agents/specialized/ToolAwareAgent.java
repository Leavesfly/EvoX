package io.leavesfly.evox.agents.specialized;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.leavesfly.evox.agents.base.Agent;
import io.leavesfly.evox.core.agent.IAgent;
import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.core.message.MessageType;
import io.leavesfly.evox.core.llm.ILLM;
import io.leavesfly.evox.core.llm.LLMConfig;
import io.leavesfly.evox.tools.agent.AgentTool;
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
     * 注意: @Builder 生成的 build() 不会自动调用 initModule()，
     * 使用 ToolAwareAgent.create() 系列方法可自动初始化
     */
    @Builder
    public ToolAwareAgent(
            String name,
            String description,
            String systemPrompt,
            LLMConfig llmConfig,
            ILLM llm,
            List<BaseTool> tools,
            Boolean autoExecuteTools,
            Integer maxToolCalls,
            Boolean autoInit
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

        // P0: Builder.build() 自动调用 initModule()
        if (autoInit == null || autoInit) {
            initModule();
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
            ILLM llmInstance = getLlm();
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
     * JSON 解析器（线程安全，可复用）
     */
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    /**
     * 解析工具调用
     *
     * <p>支持两种格式：</p>
     * <ol>
     *   <li>文本协议格式（TOOL: / PARAMS: / END_TOOL）</li>
     *   <li>JSON 数组格式（LLM 原生 function calling 返回的 JSON）</li>
     * </ol>
     *
     * PARAMS 中的 JSON 会被真正解析为 Map，解析失败时回退到原始字符串。
     */
    private List<ToolCall> parseToolCalls(String llmResponse) {
        List<ToolCall> toolCalls = new ArrayList<>();

        if (llmResponse == null || llmResponse.trim().isEmpty()) {
            return toolCalls;
        }

        // 策略1：尝试解析为 JSON 数组格式 [{"tool": "xxx", "parameters": {...}}, ...]
        toolCalls = tryParseJsonArray(llmResponse);
        if (!toolCalls.isEmpty()) {
            return toolCalls.size() > maxToolCalls ? toolCalls.subList(0, maxToolCalls) : toolCalls;
        }

        // 策略2：文本协议格式（TOOL: / PARAMS: / END_TOOL）
        toolCalls = parseTextProtocol(llmResponse);
        return toolCalls.size() > maxToolCalls ? toolCalls.subList(0, maxToolCalls) : toolCalls;
    }

    /**
     * 尝试将 LLM 响应解析为 JSON 数组格式的工具调用
     *
     * <p>支持格式：</p>
     * <pre>{@code
     * [
     *   {"tool": "calculator", "parameters": {"expression": "2+3"}},
     *   {"tool": "search",     "parameters": {"query": "weather"}}
     * ]
     * }</pre>
     */
    private List<ToolCall> tryParseJsonArray(String llmResponse) {
        List<ToolCall> toolCalls = new ArrayList<>();
        String trimmed = llmResponse.trim();

        // 提取 JSON 数组（可能被包裹在 markdown 代码块中）
        String jsonStr = extractJsonBlock(trimmed);
        if (jsonStr == null || !jsonStr.startsWith("[")) {
            return toolCalls;
        }

        try {
            List<Map<String, Object>> parsed = JSON_MAPPER.readValue(
                    jsonStr, new TypeReference<List<Map<String, Object>>>() {});
            for (Map<String, Object> item : parsed) {
                String toolName = getStringValue(item, "tool", "name", "tool_name");
                if (toolName == null) continue;

                @SuppressWarnings("unchecked")
                Map<String, Object> params = item.containsKey("parameters")
                        ? (Map<String, Object>) item.get("parameters")
                        : item.containsKey("arguments")
                            ? (Map<String, Object>) item.get("arguments")
                            : new HashMap<>();
                toolCalls.add(ToolCall.builder().toolName(toolName).parameters(params).build());
            }
        } catch (JsonProcessingException e) {
            log.debug("LLM response is not a valid JSON tool-call array, falling back to text protocol");
        }
        return toolCalls;
    }

    /**
     * 解析文本协议格式的工具调用（TOOL: / PARAMS: / END_TOOL）
     */
    private List<ToolCall> parseTextProtocol(String llmResponse) {
        List<ToolCall> toolCalls = new ArrayList<>();
        String[] lines = llmResponse.split("\n");
        String currentTool = null;
        StringBuilder paramsBuilder = new StringBuilder();
        boolean collectingParams = false;

        for (String line : lines) {
            String trimmed = line.trim();

            if (trimmed.startsWith("TOOL:")) {
                currentTool = trimmed.substring(5).trim();
                paramsBuilder.setLength(0);
                collectingParams = false;
            } else if (trimmed.startsWith("PARAMS:")) {
                String inline = trimmed.substring(7).trim();
                paramsBuilder.setLength(0);
                paramsBuilder.append(inline);
                collectingParams = true;
            } else if (trimmed.equals("END_TOOL") && currentTool != null) {
                Map<String, Object> params = parseJsonParams(paramsBuilder.toString().trim());
                toolCalls.add(ToolCall.builder()
                        .toolName(currentTool)
                        .parameters(params)
                        .build());
                currentTool = null;
                paramsBuilder.setLength(0);
                collectingParams = false;

                if (toolCalls.size() >= maxToolCalls) {
                    break;
                }
            } else if (collectingParams) {
                // 多行 PARAMS 内容
                paramsBuilder.append("\n").append(trimmed);
            }
        }

        return toolCalls;
    }

    /**
     * 将参数字符串解析为 Map。
     * 优先 JSON 解析，失败则回退为 raw_params。
     */
    private Map<String, Object> parseJsonParams(String paramsStr) {
        if (paramsStr == null || paramsStr.isEmpty()) {
            return new HashMap<>();
        }
        try {
            return JSON_MAPPER.readValue(paramsStr, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            log.debug("Failed to parse PARAMS as JSON, using raw string: {}", paramsStr);
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("raw_params", paramsStr);
            return fallback;
        }
    }

    /**
     * 从可能被 markdown 代码块包裹的文本中提取 JSON 内容
     */
    private String extractJsonBlock(String text) {
        // 处理 ```json ... ``` 包裹
        if (text.contains("```")) {
            int start = text.indexOf("```");
            int contentStart = text.indexOf('\n', start);
            int end = text.indexOf("```", contentStart);
            if (contentStart >= 0 && end > contentStart) {
                return text.substring(contentStart + 1, end).trim();
            }
        }
        // 找到第一个 '[' 开始
        int idx = text.indexOf('[');
        return idx >= 0 ? text.substring(idx) : null;
    }

    /**
     * 从 Map 中按多个候选 key 取第一个非 null 的 String 值
     */
    private String getStringValue(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object val = map.get(key);
            if (val != null) {
                return String.valueOf(val);
            }
        }
        return null;
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
     * 将另一个Agent作为工具添加（使用默认配置）
     * 这是 "Subagent as Tool" 模式的快捷方法
     *
     * @param agent 要作为工具使用的智能体
     */
    public void addAgentAsTool(IAgent agent) {
        if (agent == null) {
            log.warn("Cannot add null agent as tool");
            return;
        }
        AgentTool agentTool = AgentTool.wrap(agent);
        addTool(agentTool);
        log.info("Added agent '{}' as tool '{}' to agent '{}'",
                agent.getName(), agentTool.getName(), getName());
    }

    /**
     * 将另一个Agent作为工具添加（自定义名称和描述）
     *
     * @param agent       要作为工具使用的智能体
     * @param toolName    工具名称
     * @param description 工具描述
     */
    public void addAgentAsTool(IAgent agent, String toolName, String description) {
        if (agent == null) {
            log.warn("Cannot add null agent as tool");
            return;
        }
        AgentTool agentTool = AgentTool.wrap(agent, toolName, description);
        addTool(agentTool);
        log.info("Added agent '{}' as tool '{}' to agent '{}'",
                agent.getName(), agentTool.getName(), getName());
    }

    /**
     * 将另一个Agent作为工具添加（使用Builder进行完整配置）
     *
     * @param agent 要作为工具使用的智能体
     * @return AgentTool.Builder 供进一步配置
     */
    public AgentTool.Builder agentAsToolBuilder(IAgent agent) {
        return AgentTool.builder(agent);
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
