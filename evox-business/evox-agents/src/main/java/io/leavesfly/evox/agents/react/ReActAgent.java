package io.leavesfly.evox.agents.react;

import io.leavesfly.evox.actions.base.Action;
import io.leavesfly.evox.actions.base.ActionInput;
import io.leavesfly.evox.actions.base.ActionOutput;
import io.leavesfly.evox.actions.base.SimpleActionOutput;
import io.leavesfly.evox.agents.base.Agent;
import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.core.message.MessageType;
import io.leavesfly.evox.tools.base.BaseTool;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ReActAgent 实现 ReAct (Reasoning + Acting) 模式
 * 通过思考-行动-观察循环来解决问题
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
     * 最大迭代次数
     */
    private int maxIterations = 10;

    /**
     * ReAct 提示模板
     */
    private String reactPrompt = """
            You are a helpful AI assistant that uses the ReAct (Reasoning + Acting) approach to solve problems.
            
            Follow this format:
            Thought: Your reasoning about what to do next
            Action: The action to take (choose from available tools)
            Action Input: The input for the action
            Observation: The result of the action
            ... (repeat Thought/Action/Observation as needed)
            Final Answer: Your final answer to the question
            
            Available tools:
            {tools}
            
            Question: {question}
            
            Begin!
            """;

    @Override
    public void initModule() {
        super.initModule();
        // 创建 ReAct 动作
        ReActAction action = new ReActAction();
        action.setName("react");
        action.setDescription("ReAct reasoning and acting");
        action.setLlm(getLlm());
        action.setTools(tools);
        action.setMaxIterations(maxIterations);
        action.setPromptTemplate(reactPrompt);
        addAction(action);
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
         * 执行工具动作
         */
        private String executeToolAction(String toolName, String input) {
            if (tools == null) {
                return "No tools available";
            }
            
            for (BaseTool tool : tools) {
                if (tool.getName().equalsIgnoreCase(toolName)) {
                    try {
                        Map<String, Object> params = new HashMap<>();
                        params.put("input", input);
                        BaseTool.ToolResult result = tool.execute(params);
                        return result != null && result.isSuccess() 
                            ? (result.getData() != null ? result.getData().toString() : "No result")
                            : "Error: " + result.getError();
                    } catch (Exception e) {
                        return "Error executing tool: " + e.getMessage();
                    }
                }
            }
            
            return "Tool not found: " + toolName;
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
