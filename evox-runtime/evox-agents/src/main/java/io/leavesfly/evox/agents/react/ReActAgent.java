package io.leavesfly.evox.agents.react;

import io.leavesfly.evox.actions.base.Action;
import io.leavesfly.evox.actions.base.ActionInput;
import io.leavesfly.evox.actions.base.ActionOutput;
import io.leavesfly.evox.actions.base.SimpleActionOutput;
import io.leavesfly.evox.agents.base.Agent;
import io.leavesfly.evox.core.agent.IAgent;
import io.leavesfly.evox.core.llm.ILLM;
import io.leavesfly.evox.core.llm.LLMConfig;
import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.core.message.MessageType;
import io.leavesfly.evox.tools.agent.AgentTool;
import io.leavesfly.evox.tools.base.BaseTool;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ReActAgent 实现 ReAct (Reasoning + Acting) 模式
 * 通过思考-行动-观察循环来解决问题
 *
 * <p>支持 Builder 模式创建，以及将其他 Agent 作为工具使用（Subagent as Tool）。</p>
 *
 * <p>用法示例：</p>
 * <pre>{@code
 * // Builder 模式创建
 * ReActAgent agent = ReActAgent.builder()
 *     .name("ReActSolver")
 *     .description("Solve problems using ReAct approach")
 *     .llm(myLlm)
 *     .tools(List.of(searchTool, calculatorTool))
 *     .maxIterations(5)
 *     .build();
 *
 * // 将其他 Agent 作为工具
 * agent.addAgentAsTool(translatorAgent);
 * }</pre>
 *
 * @author EvoX Team
 */
@Data
@Slf4j
@EqualsAndHashCode(callSuper = true)
public class ReActAgent extends Agent {

    /**
     * 可用工具列表
     */
    private List<BaseTool> tools = new ArrayList<>();

    /**
     * 工具映射表（用于快速查找）
     */
    private transient Map<String, BaseTool> toolMap = new ConcurrentHashMap<>();

    /**
     * 最大迭代次数
     */
    private int maxIterations = 10;

    /**
     * ReAct 提示模板
     */
    private String reactPrompt = """
            你是一个使用 ReAct（思考 + 行动）方法解决问题的智能助手。
            
            请按以下格式输出：
            Thought: 下一步要做什么的思考
            Action: 要执行的动作（从可用工具中选择）
            Action Input: 动作的输入
            Observation: 动作的结果
            ...（按需重复 Thought/Action/Observation）
            Final Answer: 对问题的最终回答
            
            可用工具：
            {tools}
            
            问题：{question}
            
            开始吧！
            """;

    /**
     * 无参构造函数（兼容直接实例化和 setter 注入）
     */
    public ReActAgent() {
        super();
    }

    /**
     * Builder 构造函数
     * 注意: @lombok.Builder 生成的 build() 不会自动调用 initModule()，
     * 使用此构造函数时 autoInit 默认为 true，会自动初始化
     */
    @lombok.Builder
    public ReActAgent(
            String name,
            String description,
            String systemPrompt,
            LLMConfig llmConfig,
            ILLM llm,
            List<BaseTool> tools,
            Integer maxIterations,
            String reactPrompt,
            Boolean autoInit
    ) {
        this.setName(name != null ? name : "ReActAgent");
        this.setDescription(description != null ? description :
                "An agent that uses ReAct (Reasoning + Acting) pattern to solve problems");
        this.setSystemPrompt(systemPrompt);
        this.setLlmConfig(llmConfig);
        this.setLlm(llm);
        this.maxIterations = maxIterations != null ? maxIterations : 10;
        this.setHuman(false);

        if (reactPrompt != null) {
            this.reactPrompt = reactPrompt;
        }

        // 初始化工具
        if (tools != null) {
            this.tools = new ArrayList<>(tools);
            for (BaseTool tool : tools) {
                toolMap.put(tool.getName(), tool);
            }
        }

        // Builder.build() 自动调用 initModule()
        if (autoInit == null || autoInit) {
            initModule();
        }
    }

    @Override
    public void initModule() {
        super.initModule();

        // 同步 toolMap
        if (toolMap == null) {
            toolMap = new ConcurrentHashMap<>();
        }
        for (BaseTool tool : tools) {
            toolMap.putIfAbsent(tool.getName(), tool);
        }

        // 创建 ReAct 动作
        ReActAction action = new ReActAction();
        action.setName("react");
        action.setDescription("ReAct reasoning and acting");
        action.setLlm(getLlm());
        action.setTools(tools);
        action.setToolMap(toolMap);
        action.setMaxIterations(maxIterations);
        action.setPromptTemplate(reactPrompt);
        addAction(action);
    }

    /**
     * 添加工具
     *
     * @param tool 要添加的工具
     */
    public void addTool(BaseTool tool) {
        if (tool != null) {
            tools.add(tool);
            toolMap.put(tool.getName(), tool);
            log.debug("Added tool {} to agent {}", tool.getName(), getName());
        }
    }

    /**
     * 移除工具
     *
     * @param toolName 工具名称
     */
    public void removeTool(String toolName) {
        BaseTool removed = toolMap.remove(toolName);
        if (removed != null) {
            tools.remove(removed);
            log.debug("Removed tool {} from agent {}", toolName, getName());
        }
    }

    /**
     * 将另一个 Agent 作为工具添加（使用默认配置）
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
     * 将另一个 Agent 作为工具添加（自定义名称和描述）
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
     * 将另一个 Agent 作为工具添加（使用 Builder 进行完整配置）
     *
     * @param agent 要作为工具使用的智能体
     * @return AgentTool.Builder 供进一步配置
     */
    public AgentTool.Builder agentAsToolBuilder(IAgent agent) {
        return AgentTool.builder(agent);
    }

    @Override
    public Message execute(String actionName, List<Message> messages) {
        Action action = getAction(actionName);
        if (action == null) {
            return Message.builder()
                    .messageType(MessageType.ERROR)
                    .content("Action not found: " + actionName)
                    .build();
        }

        try {
            // 提取问题
            String question = extractQuestion(messages);
            
            // 创建输入
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("question", question);
            
            ActionInput input = new ActionInput() {
                @Override
                public Map<String, Object> toMap() {
                    return inputData;
                }

                @Override
                public boolean validate() {
                    return question != null && !question.isEmpty();
                }
            };

            // 执行动作
            ActionOutput output = action.execute(input);

            // 构建响应消息
            return Message.builder()
                    .messageType(output.isSuccess() ? MessageType.RESPONSE : MessageType.ERROR)
                    .content(output.getData())
                    .build();
        } catch (Exception e) {
            log.error("Failed to execute ReAct action", e);
            return Message.builder()
                    .messageType(MessageType.ERROR)
                    .content("Execution failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 从消息中提取问题
     */
    private String extractQuestion(List<Message> messages) {
        for (Message msg : messages) {
            if (msg.getMessageType() == MessageType.INPUT) {
                Object content = msg.getContent();
                if (content instanceof String) {
                    return (String) content;
                } else if (content instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> contentMap = (Map<String, Object>) content;
                    Object question = contentMap.get("question");
                    if (question != null) {
                        return question.toString();
                    }
                }
            }
        }
        return "";
    }

    /**
     * ReActAction 内部类
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    private static class ReActAction extends Action {
        private List<BaseTool> tools;
        private Map<String, BaseTool> toolMap;
        private int maxIterations;
        private String promptTemplate;

        @Override
        public ActionOutput execute(ActionInput input) {
            try {
                String question = (String) input.toMap().get("question");
                String history = "";
                
                for (int i = 0; i < maxIterations; i++) {
                    // 构建提示
                    String prompt = buildPrompt(question, history);
                    
                    // 获取 LLM 响应
                    String response = getLlm().generate(prompt);
                    log.debug("ReAct iteration {}: {}", i + 1, response);
                    
                    // 解析响应
                    ParsedStep step = parseResponse(response);
                    
                    // 检查是否完成
                    if (step.isFinalAnswer) {
                        Map<String, Object> result = new HashMap<>();
                        result.put("answer", step.finalAnswer);
                        result.put("steps", i + 1);
                        return SimpleActionOutput.success(result);
                    }
                    
                    // 执行动作并获取观察
                    String observation = executeToolAction(step.action, step.actionInput);
                    
                    // 更新历史
                    history += String.format("Thought: %s%nAction: %s%nAction Input: %s%nObservation: %s%n%n",
                            step.thought, step.action, step.actionInput, observation);
                }
                
                // 达到最大迭代次数
                return SimpleActionOutput.failure("Max iterations reached without final answer");
            } catch (Exception e) {
                log.error("ReActAction execution failed", e);
                return SimpleActionOutput.failure("Execution failed: " + e.getMessage());
            }
        }

        /**
         * 构建提示
         */
        private String buildPrompt(String question, String history) {
            String toolsDesc = buildToolsDescription();
            String prompt = promptTemplate
                    .replace("{tools}", toolsDesc)
                    .replace("{question}", question);
            return prompt + "\n\n" + history;
        }

        /**
         * 构建工具描述
         */
        private String buildToolsDescription() {
            if (tools == null || tools.isEmpty()) {
                return "No tools available";
            }
            
            StringBuilder sb = new StringBuilder();
            for (BaseTool tool : tools) {
                sb.append(String.format("- %s: %s%n", tool.getName(), tool.getDescription()));
            }
            return sb.toString();
        }

        /**
         * 解析 LLM 响应
         */
        private ParsedStep parseResponse(String response) {
            ParsedStep step = new ParsedStep();
            
            // 检查是否是最终答案
            Pattern finalPattern = Pattern.compile("Final Answer:\\s*(.+)", Pattern.DOTALL);
            Matcher finalMatcher = finalPattern.matcher(response);
            if (finalMatcher.find()) {
                step.isFinalAnswer = true;
                step.finalAnswer = finalMatcher.group(1).trim();
                return step;
            }
            
            // 解析思考
            Pattern thoughtPattern = Pattern.compile("Thought:\\s*(.+?)(?=Action:|$)", Pattern.DOTALL);
            Matcher thoughtMatcher = thoughtPattern.matcher(response);
            if (thoughtMatcher.find()) {
                step.thought = thoughtMatcher.group(1).trim();
            }
            
            // 解析动作
            Pattern actionPattern = Pattern.compile("Action:\\s*(.+?)(?=Action Input:|$)", Pattern.DOTALL);
            Matcher actionMatcher = actionPattern.matcher(response);
            if (actionMatcher.find()) {
                step.action = actionMatcher.group(1).trim();
            }
            
            // 解析动作输入
            Pattern inputPattern = Pattern.compile("Action Input:\\s*(.+?)(?=Observation:|$)", Pattern.DOTALL);
            Matcher inputMatcher = inputPattern.matcher(response);
            if (inputMatcher.find()) {
                step.actionInput = inputMatcher.group(1).trim();
            }
            
            return step;
        }

        /**
         * 执行工具动作（优先使用 toolMap 快速查找，回退到忽略大小写的线性匹配）
         */
        private String executeToolAction(String toolName, String input) {
            if (toolMap == null || toolMap.isEmpty()) {
                return "No tools available";
            }

            // 优先精确匹配
            BaseTool tool = toolMap.get(toolName);

            // 回退：忽略大小写匹配
            if (tool == null) {
                for (BaseTool candidate : toolMap.values()) {
                    if (candidate.getName().equalsIgnoreCase(toolName)) {
                        tool = candidate;
                        break;
                    }
                }
            }

            if (tool == null) {
                return "Tool not found: " + toolName;
            }

            try {
                Map<String, Object> params = new HashMap<>();
                params.put("input", input);
                BaseTool.ToolResult result = tool.execute(params);
                return result != null && result.isSuccess()
                        ? (result.getData() != null ? result.getData().toString() : "No result")
                        : "Error: " + (result != null ? result.getError() : "Unknown error");
            } catch (Exception e) {
                return "Error executing tool: " + e.getMessage();
            }
        }

        @Override
        public String[] getInputFields() {
            return new String[]{"question"};
        }

        @Override
        public String[] getOutputFields() {
            return new String[]{"answer", "steps"};
        }

        /**
         * 解析的步骤
         */
        private static class ParsedStep {
            String thought = "";
            String action = "";
            String actionInput = "";
            boolean isFinalAnswer = false;
            String finalAnswer = "";
        }
    }
}
