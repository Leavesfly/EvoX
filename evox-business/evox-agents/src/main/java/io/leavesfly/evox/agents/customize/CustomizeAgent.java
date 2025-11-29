package io.leavesfly.evox.agents.customize;

import io.leavesfly.evox.actions.base.Action;
import io.leavesfly.evox.actions.base.ActionInput;
import io.leavesfly.evox.actions.base.ActionOutput;
import io.leavesfly.evox.actions.base.SimpleActionOutput;
import io.leavesfly.evox.agents.base.Agent;
import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.core.message.MessageType;
import io.leavesfly.evox.models.base.BaseLLM;
import io.leavesfly.evox.models.config.LLMConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * CustomizeAgent 提供灵活的框架来创建专用的 LLM 驱动智能体,无需编写自定义代码
 * 支持定义良好的输入输出、自定义提示模板和可配置的解析策略
 *
 * @author EvoX Team
 */
@Data
@Slf4j
@EqualsAndHashCode(callSuper = true)
public class CustomizeAgent extends Agent {

    /**
     * 输入字段规格
     */
    private List<InputSpec> inputs = new ArrayList<>();

    /**
     * 输出字段规格
     */
    private List<OutputSpec> outputs = new ArrayList<>();

    /**
     * 提示模板
     */
    private String promptTemplate;

    /**
     * 解析模式
     */
    private ParseMode parseMode = ParseMode.JSON;

    /**
     * 自定义解析函数
     */
    private transient Function<String, Map<String, Object>> parseFunction;

    /**
     * 主要动作名称
     */
    private String primaryActionName = "customize_action";

    @Override
    public void initModule() {
        super.initModule();
        // 创建主要动作
        if (promptTemplate != null) {
            CustomizeAction action = createCustomizeAction();
            addAction(action);
        }
    }

    /**
     * 创建自定义动作
     */
    private CustomizeAction createCustomizeAction() {
        CustomizeAction action = new CustomizeAction();
        action.setName(primaryActionName);
        action.setDescription(getDescription());
        action.setPromptTemplate(promptTemplate);
        action.setInputs(inputs);
        action.setOutputs(outputs);
        action.setParseMode(parseMode);
        action.setParseFunction(parseFunction);
        action.setLlm(getLlm());
        return action;
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
            // 准备输入
            Map<String, Object> inputData = prepareInput(messages);
            ActionInput input = createActionInput(inputData);

            // 执行动作
            ActionOutput output = action.execute(input);

            // 构建响应消息
            return Message.builder()
                    .messageType(output.isSuccess() ? MessageType.RESPONSE : MessageType.ERROR)
                    .content(output.getData())
                    .build();
        } catch (Exception e) {
            log.error("Failed to execute action: {}", actionName, e);
            return Message.builder()
                    .messageType(MessageType.ERROR)
                    .content("Execution failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 使用默认动作执行
     */
    public Message call(Map<String, Object> inputs) {
        return execute(primaryActionName, createMessagesFromInput(inputs));
    }

    /**
     * 从输入创建消息列表
     */
    private List<Message> createMessagesFromInput(Map<String, Object> inputs) {
        List<Message> messages = new ArrayList<>();
        messages.add(Message.builder()
                .messageType(MessageType.INPUT)
                .content(inputs)
                .build());
        return messages;
    }

    /**
     * 准备动作输入
     */
    private Map<String, Object> prepareInput(List<Message> messages) {
        Map<String, Object> inputData = new HashMap<>();
        
        // 从消息中提取输入
        for (Message msg : messages) {
            if (msg.getMessageType() == MessageType.INPUT) {
                Object content = msg.getContent();
                if (content instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> contentMap = (Map<String, Object>) content;
                    inputData.putAll(contentMap);
                }
            }
        }

        return inputData;
    }

    /**
     * 创建动作输入对象
     */
    private ActionInput createActionInput(Map<String, Object> inputData) {
        return new ActionInput() {
            private final Map<String, Object> data = inputData;

            @Override
            public Map<String, Object> toMap() {
                return data;
            }

            @Override
            public boolean validate() {
                // 验证所有必需字段
                for (InputSpec spec : inputs) {
                    if (spec.isRequired() && !data.containsKey(spec.getName())) {
                        return false;
                    }
                }
                return true;
            }
        };
    }

    /**
     * 输入字段规格
     */
    @Data
    public static class InputSpec {
        private String name;
        private String type;
        private String description;
        private boolean required = true;

        public InputSpec(String name, String type, String description) {
            this.name = name;
            this.type = type;
            this.description = description;
        }

        public InputSpec(String name, String type, String description, boolean required) {
            this.name = name;
            this.type = type;
            this.description = description;
            this.required = required;
        }
    }

    /**
     * 输出字段规格
     */
    @Data
    public static class OutputSpec {
        private String name;
        private String type;
        private String description;
        private boolean required = true;

        public OutputSpec(String name, String type, String description) {
            this.name = name;
            this.type = type;
            this.description = description;
        }

        public OutputSpec(String name, String type, String description, boolean required) {
            this.name = name;
            this.type = type;
            this.description = description;
            this.required = required;
        }
    }

    /**
     * 解析模式
     */
    public enum ParseMode {
        STRING,
        JSON,
        XML,
        CUSTOM
    }

    /**
     * CustomizeAction 内部类
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    private static class CustomizeAction extends Action {
        private String promptTemplate;
        private List<InputSpec> inputs;
        private List<OutputSpec> outputs;
        private ParseMode parseMode;
        private transient Function<String, Map<String, Object>> parseFunction;

        @Override
        public ActionOutput execute(ActionInput input) {
            try {
                // 格式化提示词
                String prompt = formatPrompt(input.toMap());

                // 使用 LLM 生成
                String response = getLlm().generate(prompt);

                // 解析响应
                Map<String, Object> parsed = parseResponse(response);

                return SimpleActionOutput.success(parsed);
            } catch (Exception e) {
                log.error("CustomizeAction execution failed", e);
                return SimpleActionOutput.failure("Execution failed: " + e.getMessage());
            }
        }

        /**
         * 格式化提示词
         */
        private String formatPrompt(Map<String, Object> inputData) {
            String result = promptTemplate;
            for (Map.Entry<String, Object> entry : inputData.entrySet()) {
                String placeholder = "{" + entry.getKey() + "}";
                result = result.replace(placeholder, String.valueOf(entry.getValue()));
            }
            return result;
        }

        /**
         * 解析响应
         */
        private Map<String, Object> parseResponse(String response) {
            if (parseFunction != null && parseMode == ParseMode.CUSTOM) {
                return parseFunction.apply(response);
            }

            // 默认解析逻辑
            Map<String, Object> result = new HashMap<>();
            
            switch (parseMode) {
                case JSON:
                    result = parseJsonResponse(response);
                    break;
                case XML:
                    result = parseXmlResponse(response);
                    break;
                case STRING:
                default:
                    result.put("response", response);
                    break;
            }

            return result;
        }
        
        /**
         * 解析JSON响应
         */
        private Map<String, Object> parseJsonResponse(String response) {
            Map<String, Object> result = new HashMap<>();
            try {
                // 尝试提取JSON部分
                String jsonPart = extractJsonFromResponse(response);
                if (jsonPart == null) {
                    log.warn("No JSON found in response, returning raw text");
                    result.put("response", response);
                    return result;
                }
                
                // 简单的JSON解析（手动实现）
                jsonPart = jsonPart.trim();
                if (jsonPart.startsWith("{") && jsonPart.endsWith("}")) {
                    // 移除外层花括号
                    String content = jsonPart.substring(1, jsonPart.length() - 1);
                    String[] pairs = content.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                    
                    for (String pair : pairs) {
                        int colonIndex = pair.indexOf(':');
                        if (colonIndex > 0) {
                            String key = pair.substring(0, colonIndex).trim().replaceAll("[\"\\']", "");
                            String value = pair.substring(colonIndex + 1).trim().replaceAll("^[\"\\']+|[\"\\']+$", "");
                            result.put(key, value);
                        }
                    }
                } else if (jsonPart.startsWith("[") && jsonPart.endsWith("]")) {
                    // 数组格式，返回为list
                    result.put("array", jsonPart);
                }
                
                if (result.isEmpty()) {
                    result.put("response", response);
                }
                
                log.debug("Parsed JSON response with {} fields", result.size());
            } catch (Exception e) {
                log.error("Failed to parse JSON response", e);
                result.put("response", response);
                result.put("parse_error", e.getMessage());
            }
            return result;
        }
        
        /**
         * 从响应中提取JSON字符串
         */
        private String extractJsonFromResponse(String response) {
            if (response == null || response.isEmpty()) {
                return null;
            }
            
            // 查找JSON对象
            int startIndex = response.indexOf('{');
            if (startIndex >= 0) {
                int depth = 0;
                for (int i = startIndex; i < response.length(); i++) {
                    char c = response.charAt(i);
                    if (c == '{') depth++;
                    if (c == '}') depth--;
                    if (depth == 0) {
                        return response.substring(startIndex, i + 1);
                    }
                }
            }
            
            // 查找JSON数组
            startIndex = response.indexOf('[');
            if (startIndex >= 0) {
                int depth = 0;
                for (int i = startIndex; i < response.length(); i++) {
                    char c = response.charAt(i);
                    if (c == '[') depth++;
                    if (c == ']') depth--;
                    if (depth == 0) {
                        return response.substring(startIndex, i + 1);
                    }
                }
            }
            
            return null;
        }
        
        /**
         * 解析XML响应
         */
        private Map<String, Object> parseXmlResponse(String response) {
            Map<String, Object> result = new HashMap<>();
            try {
                // 简单的XML标签提取
                String xmlPart = extractXmlFromResponse(response);
                if (xmlPart == null) {
                    log.warn("No XML found in response, returning raw text");
                    result.put("response", response);
                    return result;
                }
                
                // 提取所有XML标签和内容
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("<([^>]+)>([^<]*)</\\1>");
                java.util.regex.Matcher matcher = pattern.matcher(xmlPart);
                
                while (matcher.find()) {
                    String tagName = matcher.group(1);
                    String content = matcher.group(2).trim();
                    result.put(tagName, content);
                }
                
                if (result.isEmpty()) {
                    result.put("response", response);
                }
                
                log.debug("Parsed XML response with {} fields", result.size());
            } catch (Exception e) {
                log.error("Failed to parse XML response", e);
                result.put("response", response);
                result.put("parse_error", e.getMessage());
            }
            return result;
        }
        
        /**
         * 从响应中提取XML字符串
         */
        private String extractXmlFromResponse(String response) {
            if (response == null || response.isEmpty()) {
                return null;
            }
            
            // 查找XML声明或根元素
            int startIndex = response.indexOf("<");
            if (startIndex < 0) {
                return null;
            }
            
            int endIndex = response.lastIndexOf(">");
            if (endIndex > startIndex) {
                return response.substring(startIndex, endIndex + 1);
            }
            
            return null;
        }

        @Override
        public String[] getInputFields() {
            return inputs.stream()
                    .map(InputSpec::getName)
                    .toArray(String[]::new);
        }

        @Override
        public String[] getOutputFields() {
            return outputs.stream()
                    .map(OutputSpec::getName)
                    .toArray(String[]::new);
        }
    }
}
