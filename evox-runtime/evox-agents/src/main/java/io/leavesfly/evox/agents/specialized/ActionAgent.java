package io.leavesfly.evox.agents.specialized;

import io.leavesfly.evox.agents.base.Agent;
import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.core.message.MessageType;
import io.leavesfly.evox.models.config.LLMConfig;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * ActionAgent - 动作执行代理
 * 直接执行提供的函数，不依赖LLM
 * 适用于确定性任务和工具封装
 *
 * @author EvoX Team
 */
@Data
@Slf4j
@EqualsAndHashCode(callSuper = true)
public class ActionAgent extends Agent {

    /**
     * 输入参数规范
     */
    private List<ParameterSpec> inputSpecs;

    /**
     * 输出参数规范
     */
    private List<ParameterSpec> outputSpecs;

    /**
     * 执行函数
     */
    private Function<Map<String, Object>, Map<String, Object>> executeFunction;

    /**
     * 是否验证输入
     */
    private boolean validateInputs = true;

    /**
     * 是否验证输出
     */
    private boolean validateOutputs = true;

    /**
     * 构建器模式构造函数
     */
    @Builder
    public ActionAgent(
            String name,
            String description,
            List<ParameterSpec> inputSpecs,
            List<ParameterSpec> outputSpecs,
            Function<Map<String, Object>, Map<String, Object>> executeFunction,
            LLMConfig llmConfig,
            boolean validateInputs,
            boolean validateOutputs
    ) {
        this.setName(name);
        this.setDescription(description);
        this.inputSpecs = inputSpecs;
        this.outputSpecs = outputSpecs;
        this.executeFunction = executeFunction;
        this.setLlmConfig(llmConfig);
        this.validateInputs = validateInputs;
        this.validateOutputs = validateOutputs;
        
        // ActionAgent默认为非人类代理
        this.setHuman(llmConfig == null);
    }

    @Override
    public Message execute(String actionName, List<Message> messages) {
        log.debug("ActionAgent {} executing action: {}", getName(), actionName);

        try {
            // 从消息中提取参数
            Map<String, Object> inputs = extractInputsFromMessages(messages);

            // 验证输入参数
            if (validateInputs) {
                validateInputParameters(inputs);
            }

            // 执行函数
            Map<String, Object> outputs = executeFunction.apply(inputs);

            // 验证输出参数
            if (validateOutputs) {
                validateOutputParameters(outputs);
            }

            // 构造响应消息
            return Message.builder()
                    .messageType(MessageType.RESPONSE)
                    .content(formatOutputs(outputs))
                    .build();

        } catch (Exception e) {
            log.error("Error executing ActionAgent {}: {}", getName(), e.getMessage(), e);
            return Message.builder()
                    .messageType(MessageType.RESPONSE)
                    .content("Error: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 从消息列表中提取输入参数
     */
    private Map<String, Object> extractInputsFromMessages(List<Message> messages) {
        Map<String, Object> inputs = new HashMap<>();

        if (messages == null || messages.isEmpty()) {
            return inputs;
        }

        // 从最后一条消息中提取参数
        Message lastMessage = messages.get(messages.size() - 1);
        Object content = lastMessage.getContent();

        // 如果只有一个输入参数，直接使用content
        if (inputSpecs != null && inputSpecs.size() == 1) {
            ParameterSpec spec = inputSpecs.get(0);
            inputs.put(spec.getName(), content);
        } else if (content instanceof Map) {
            // 如果 content 是 Map，直接使用
            @SuppressWarnings("unchecked")
            Map<String, Object> contentMap = (Map<String, Object>) content;
            inputs.putAll(contentMap);
        }

        return inputs;
    }

    /**
     * 验证输入参数
     */
    private void validateInputParameters(Map<String, Object> inputs) {
        if (inputSpecs == null || inputSpecs.isEmpty()) {
            return;
        }

        for (ParameterSpec spec : inputSpecs) {
            if (spec.isRequired() && !inputs.containsKey(spec.getName())) {
                throw new IllegalArgumentException("Missing required input parameter: " + spec.getName());
            }

            if (inputs.containsKey(spec.getName())) {
                Object value = inputs.get(spec.getName());
                if (spec.isRequired() && value == null) {
                    throw new IllegalArgumentException("Required input parameter cannot be null: " + spec.getName());
                }
            }
        }
    }

    /**
     * 验证输出参数
     */
    private void validateOutputParameters(Map<String, Object> outputs) {
        if (outputSpecs == null || outputSpecs.isEmpty()) {
            return;
        }

        for (ParameterSpec spec : outputSpecs) {
            if (spec.isRequired() && !outputs.containsKey(spec.getName())) {
                throw new IllegalArgumentException("Missing required output parameter: " + spec.getName());
            }

            if (outputs.containsKey(spec.getName())) {
                Object value = outputs.get(spec.getName());
                if (spec.isRequired() && value == null) {
                    throw new IllegalArgumentException("Required output parameter cannot be null: " + spec.getName());
                }
            }
        }
    }

    /**
     * 格式化输出为字符串
     */
    private String formatOutputs(Map<String, Object> outputs) {
        if (outputs == null || outputs.isEmpty()) {
            return "";
        }

        // 如果只有一个输出，直接返回其值
        if (outputs.size() == 1) {
            return String.valueOf(outputs.values().iterator().next());
        }

        // 多个输出，格式化为键值对
        StringBuilder sb = new StringBuilder();
        outputs.forEach((key, value) -> {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(key).append(": ").append(value);
        });
        return sb.toString();
    }

    /**
     * 参数规范
     */
    @Data
    @Builder
    public static class ParameterSpec {
        /**
         * 参数名称
         */
        private String name;

        /**
         * 参数类型
         */
        private String type;

        /**
         * 参数描述
         */
        private String description;

        /**
         * 是否必需
         */
        @Builder.Default
        private boolean required = true;

        /**
         * 默认值
         */
        private Object defaultValue;
    }
}
