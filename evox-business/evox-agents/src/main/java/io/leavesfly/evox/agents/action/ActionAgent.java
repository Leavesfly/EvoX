package io.leavesfly.evox.agents.action;

import io.leavesfly.evox.actions.base.Action;
import io.leavesfly.evox.actions.base.ActionInput;
import io.leavesfly.evox.actions.base.ActionOutput;
import io.leavesfly.evox.actions.base.SimpleActionOutput;
import io.leavesfly.evox.agents.base.Agent;
import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.core.message.MessageType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * ActionAgent 是一个专用智能体,直接执行提供的函数而不使用 LLM
 * 它创建一个使用提供的函数作为执行骨干的动作
 *
 * @author EvoX Team
 */
@Data
@Slf4j
@EqualsAndHashCode(callSuper = true)
public class ActionAgent extends Agent {

    /**
     * 执行函数
     */
    private transient Function<Map<String, Object>, Map<String, Object>> executeFunction;

    /**
     * 输入字段规格
     */
    private List<FieldSpec> inputs;

    /**
     * 输出字段规格
     */
    private List<FieldSpec> outputs;

    /**
     * 主要动作名称
     */
    private String primaryActionName = "function_action";

    @Override
    public void initModule() {
        super.initModule();
        // 创建函数动作
        if (executeFunction != null) {
            FunctionAction action = createFunctionAction();
            addAction(action);
        }
    }

    /**
     * 创建函数动作
     */
    private FunctionAction createFunctionAction() {
        FunctionAction action = new FunctionAction();
        action.setName(primaryActionName);
        action.setDescription(getDescription());
        action.setExecuteFunction(executeFunction);
        action.setInputs(inputs);
        action.setOutputs(outputs);
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
        List<Message> messages = List.of(Message.builder()
                .messageType(MessageType.INPUT)
                .content(inputs)
                .build());
        return execute(primaryActionName, messages);
    }

    /**
     * 准备动作输入
     */
    private Map<String, Object> prepareInput(List<Message> messages) {
        Map<String, Object> inputData = new HashMap<>();
        
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
                if (inputs != null) {
                    for (FieldSpec spec : inputs) {
                        if (spec.isRequired() && !data.containsKey(spec.getName())) {
                            return false;
                        }
                    }
                }
                return true;
            }
        };
    }

    /**
     * 字段规格
     */
    @Data
    public static class FieldSpec {
        private String name;
        private String type;
        private String description;
        private boolean required = true;

        public FieldSpec(String name, String type, String description) {
            this.name = name;
            this.type = type;
            this.description = description;
        }

        public FieldSpec(String name, String type, String description, boolean required) {
            this.name = name;
            this.type = type;
            this.description = description;
            this.required = required;
        }
    }

    /**
     * FunctionAction 内部类
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    private static class FunctionAction extends Action {
        private transient Function<Map<String, Object>, Map<String, Object>> executeFunction;
        private List<FieldSpec> inputs;
        private List<FieldSpec> outputs;

        @Override
        public ActionOutput execute(ActionInput input) {
            try {
                // 直接执行函数
                Map<String, Object> result = executeFunction.apply(input.toMap());
                return SimpleActionOutput.success(result);
            } catch (Exception e) {
                log.error("FunctionAction execution failed", e);
                return SimpleActionOutput.failure("Execution failed: " + e.getMessage());
            }
        }

        @Override
        public String[] getInputFields() {
            if (inputs == null) {
                return new String[0];
            }
            return inputs.stream()
                    .map(FieldSpec::getName)
                    .toArray(String[]::new);
        }

        @Override
        public String[] getOutputFields() {
            if (outputs == null) {
                return new String[0];
            }
            return outputs.stream()
                    .map(FieldSpec::getName)
                    .toArray(String[]::new);
        }
    }
}
