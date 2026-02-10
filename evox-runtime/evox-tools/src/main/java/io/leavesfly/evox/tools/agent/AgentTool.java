package io.leavesfly.evox.tools.agent;

import io.leavesfly.evox.core.agent.IAgent;
import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.core.message.MessageType;
import io.leavesfly.evox.tools.base.BaseTool;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * AgentTool - 将智能体包装为工具
 * 支持 "Subagent as Tool" 模式，让一个智能体可以作为另一个智能体的工具被调用
 *
 * <p>使用场景：
 * <ul>
 *   <li>主智能体需要调用专业子智能体完成特定任务（如翻译、代码审查、数据分析等）</li>
 *   <li>将多个智能体组合为工具链，实现复杂工作流</li>
 *   <li>动态注册/注销子智能体，实现灵活的智能体编排</li>
 * </ul>
 *
 * <p>用法示例：
 * <pre>{@code
 * // 创建一个专业翻译Agent
 * Agent translatorAgent = ...;
 *
 * // 将其包装为工具
 * AgentTool translatorTool = AgentTool.wrap(translatorAgent);
 *
 * // 注册到主Agent的工具列表
 * toolAwareAgent.addTool(translatorTool);
 * }</pre>
 *
 * @author EvoX Team
 */
@Slf4j
public class AgentTool extends BaseTool {

    /**
     * 被包装的智能体实例
     */
    private final IAgent wrappedAgent;

    /**
     * 默认动作名称（为null时由被包装Agent决定）
     */
    private final String defaultActionName;

    /**
     * 是否在结果中包含Agent元数据
     */
    private final boolean includeMetadata;

    /**
     * 执行超时时间（毫秒），0表示不限制
     */
    private final long timeoutMs;

    /**
     * 构造函数
     *
     * @param wrappedAgent      被包装的智能体
     * @param toolName          工具名称（如果为null，使用 "agent_" + agent名称）
     * @param toolDescription   工具描述（如果为null，自动从Agent描述生成）
     * @param defaultActionName 默认动作名称
     * @param includeMetadata   是否包含元数据
     * @param timeoutMs         超时时间（毫秒）
     */
    public AgentTool(IAgent wrappedAgent,
                     String toolName,
                     String toolDescription,
                     String defaultActionName,
                     boolean includeMetadata,
                     long timeoutMs) {
        if (wrappedAgent == null) {
            throw new IllegalArgumentException("Wrapped agent cannot be null");
        }

        this.wrappedAgent = wrappedAgent;
        this.defaultActionName = defaultActionName;
        this.includeMetadata = includeMetadata;
        this.timeoutMs = timeoutMs;

        // 设置工具基本属性
        this.name = (toolName != null && !toolName.isEmpty())
                ? toolName
                : "agent_" + sanitizeName(wrappedAgent.getName());

        this.description = (toolDescription != null && !toolDescription.isEmpty())
                ? toolDescription
                : buildDefaultDescription(wrappedAgent);

        // 定义输入参数
        this.inputs = buildInputDefinitions();
        this.required = List.of("query");
    }

    /**
     * 快速包装：将Agent包装为Tool（使用默认配置）
     *
     * @param agent 要包装的智能体
     * @return AgentTool实例
     */
    public static AgentTool wrap(IAgent agent) {
        return new AgentTool(agent, null, null, null, true, 0);
    }

    /**
     * 带自定义名称和描述的包装
     *
     * @param agent       要包装的智能体
     * @param toolName    工具名称
     * @param description 工具描述
     * @return AgentTool实例
     */
    public static AgentTool wrap(IAgent agent, String toolName, String description) {
        return new AgentTool(agent, toolName, description, null, true, 0);
    }

    /**
     * Builder模式创建AgentTool
     *
     * @param agent 要包装的智能体
     * @return Builder实例
     */
    public static Builder builder(IAgent agent) {
        return new Builder(agent);
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        log.debug("AgentTool [{}] executing with wrapped agent [{}]", name, wrappedAgent.getName());

        try {
            // 验证必需参数
            validateParameters(parameters);

            // 提取参数
            String query = getParameter(parameters, "query", "");
            String actionName = getParameter(parameters, "action_name", defaultActionName);
            String context = getParameter(parameters, "context", "");

            if (query.isEmpty()) {
                return ToolResult.failure("Parameter 'query' cannot be empty");
            }

            // 构建消息列表
            List<Message> messages = buildMessages(query, context);

            // 执行Agent
            Message result = executeAgent(actionName, messages);

            // 转换结果
            return convertToToolResult(result);

        } catch (Exception e) {
            log.error("AgentTool [{}] execution failed: {}", name, e.getMessage(), e);
            return ToolResult.failure("Agent execution failed: " + e.getMessage());
        }
    }

    /**
     * 构建发送给Agent的消息列表
     */
    private List<Message> buildMessages(String query, String context) {
        List<Message> messages = new ArrayList<>();

        // 如果有上下文，先添加系统消息
        if (context != null && !context.isEmpty()) {
            messages.add(Message.builder()
                    .messageType(MessageType.SYSTEM)
                    .content(context)
                    .build());
        }

        // 添加用户输入消息
        messages.add(Message.builder()
                .messageType(MessageType.INPUT)
                .content(query)
                .build());

        return messages;
    }

    /**
     * 执行被包装的Agent
     */
    private Message executeAgent(String actionName, List<Message> messages) {
        long startTime = System.currentTimeMillis();

        try {
            Message result;

            if (timeoutMs > 0) {
                // 带超时的异步执行
                result = wrappedAgent.executeAsync(actionName, messages)
                        .block(java.time.Duration.ofMillis(timeoutMs));
            } else {
                // 同步执行
                result = wrappedAgent.execute(actionName, messages);
            }

            long elapsed = System.currentTimeMillis() - startTime;
            log.debug("AgentTool [{}] agent execution completed in {}ms", name, elapsed);

            return result;

        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("AgentTool [{}] agent execution failed after {}ms: {}",
                    name, elapsed, e.getMessage());
            throw e;
        }
    }

    /**
     * 将Agent的Message结果转换为ToolResult
     */
    private ToolResult convertToToolResult(Message result) {
        if (result == null) {
            return ToolResult.failure("Agent returned null result");
        }

        Object content = result.getContent();
        String contentStr = content != null ? String.valueOf(content) : "";

        // 检查是否为错误消息
        if (result.getMessageType() == MessageType.ERROR) {
            Map<String, Object> metadata = includeMetadata ? buildResultMetadata(result) : null;
            return ToolResult.failure(contentStr, metadata);
        }

        // 构建成功结果
        Map<String, Object> data = new HashMap<>();
        data.put("response", contentStr);
        data.put("agent_name", wrappedAgent.getName());

        if (result.getAgent() != null) {
            data.put("responding_agent", result.getAgent());
        }
        if (result.getAction() != null) {
            data.put("action", result.getAction());
        }

        Map<String, Object> metadata = includeMetadata ? buildResultMetadata(result) : null;
        return ToolResult.success(data, metadata);
    }

    /**
     * 构建结果元数据
     */
    private Map<String, Object> buildResultMetadata(Message result) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("agent_id", wrappedAgent.getAgentId());
        metadata.put("agent_name", wrappedAgent.getName());
        metadata.put("message_type", result.getMessageType().name());
        metadata.put("message_id", result.getMessageId());
        if (result.getTimestamp() != null) {
            metadata.put("timestamp", result.getTimestamp().toString());
        }
        return metadata;
    }

    /**
     * 构建输入参数定义
     */
    private Map<String, Map<String, String>> buildInputDefinitions() {
        Map<String, Map<String, String>> inputDefs = new LinkedHashMap<>();

        // query: 必需参数 - 发送给Agent的任务/问题
        Map<String, String> queryDef = new HashMap<>();
        queryDef.put("type", "string");
        queryDef.put("description", "The task or question to send to the agent '" + wrappedAgent.getName() + "'");
        inputDefs.put("query", queryDef);

        // action_name: 可选参数 - 指定Agent执行的动作
        Map<String, String> actionDef = new HashMap<>();
        actionDef.put("type", "string");
        actionDef.put("description", "The specific action for the agent to execute (optional, uses default if not specified)");
        inputDefs.put("action_name", actionDef);

        // context: 可选参数 - 额外的上下文信息
        Map<String, String> contextDef = new HashMap<>();
        contextDef.put("type", "string");
        contextDef.put("description", "Additional context or background information for the agent (optional)");
        inputDefs.put("context", contextDef);

        return inputDefs;
    }

    /**
     * 构建默认描述
     */
    private String buildDefaultDescription(IAgent agent) {
        String agentDesc = agent.getDescription();
        if (agentDesc != null && !agentDesc.isEmpty()) {
            return String.format("Delegate task to agent '%s': %s", agent.getName(), agentDesc);
        }
        return String.format("Delegate task to agent '%s'", agent.getName());
    }

    /**
     * 清理名称（确保可作为工具名使用）
     */
    private String sanitizeName(String agentName) {
        if (agentName == null || agentName.isEmpty()) {
            return "unknown";
        }
        // 替换非字母数字字符为下划线，转小写
        return agentName.replaceAll("[^a-zA-Z0-9_]", "_").toLowerCase();
    }

    /**
     * 获取被包装的Agent
     *
     * @return 被包装的Agent实例
     */
    public IAgent getWrappedAgent() {
        return wrappedAgent;
    }

    /**
     * AgentTool Builder
     */
    public static class Builder {
        private final IAgent agent;
        private String toolName;
        private String toolDescription;
        private String defaultActionName;
        private boolean includeMetadata = true;
        private long timeoutMs = 0;

        public Builder(IAgent agent) {
            this.agent = agent;
        }

        /**
         * 设置工具名称
         */
        public Builder toolName(String toolName) {
            this.toolName = toolName;
            return this;
        }

        /**
         * 设置工具描述
         */
        public Builder toolDescription(String toolDescription) {
            this.toolDescription = toolDescription;
            return this;
        }

        /**
         * 设置默认动作名称
         */
        public Builder defaultActionName(String defaultActionName) {
            this.defaultActionName = defaultActionName;
            return this;
        }

        /**
         * 设置是否包含元数据
         */
        public Builder includeMetadata(boolean includeMetadata) {
            this.includeMetadata = includeMetadata;
            return this;
        }

        /**
         * 设置超时时间（毫秒）
         */
        public Builder timeoutMs(long timeoutMs) {
            this.timeoutMs = timeoutMs;
            return this;
        }

        /**
         * 构建AgentTool
         */
        public AgentTool build() {
            return new AgentTool(agent, toolName, toolDescription,
                    defaultActionName, includeMetadata, timeoutMs);
        }
    }
}
