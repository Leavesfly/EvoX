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

import java.util.*;
import java.util.function.Function;

/**
 * ActionAgent 是一个专用智能体,直接执行提供的函数而不使用 LLM
 * 它创建一个使用提供的函数作为执行骨干的动作
 *
 * <p>支持 Builder 模式快速创建:</p>
 * <pre>{@code
 * ActionAgent agent = ActionAgent.builder()
 *     .name("Calculator")
 *     .description("数学计算")
 *     .inputs(FieldSpec.of("a", "int", "第一个数"), FieldSpec.of("b", "int", "第二个数"))
 *     .outputs(FieldSpec.of("result", "int", "计算结果"))
 *     .executeFunction(params -> Map.of("result", (int)params.get("a") + (int)params.get("b")))
 *     .build();  // 自动 initModule()
 * }</pre>
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
    protected String getPrimaryActionName() {
        return primaryActionName;
    }

    @Override
    protected void validateRequiredFields() {
        super.validateRequiredFields();
        if (executeFunction == null) {
            throw new IllegalStateException(
                    "ActionAgent '" + getName() + "': 'executeFunction' must be set before initModule()");
        }
    }

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
        String resolvedName = resolveActionName(actionName);
        Action action = getAction(resolvedName);
        if (action == null) {
            return Message.builder()
                    .messageType(MessageType.ERROR)
                    .content("Action not found: " + resolvedName)
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
            log.error("Failed to execute action: {}", resolvedName, e);
            return Message.builder()
                    .messageType(MessageType.ERROR)
                    .content("Execution failed: " + e.getMessage())
                    .build();
        }
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

    // ===================================================================
    // Builder 模式 — build() 自动调 initModule()
    // ===================================================================

    /**
     * 创建 ActionAgent Builder
     */
    public static ActionAgentBuilder builder() {
        return new ActionAgentBuilder();
    }

    /**
     * ActionAgent 专用 Builder
     * build() 自动调用 initModule()，无需手动调用
     */
    public static class ActionAgentBuilder {
        private String name;
        private String description;
        private List<FieldSpec> inputs;
        private List<FieldSpec> outputs;
        private Function<Map<String, Object>, Map<String, Object>> executeFunction;

        public ActionAgentBuilder name(String name) {
            this.name = name;
            return this;
        }

        public ActionAgentBuilder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * 设置输入字段（varargs，减少样板代码）
         */
        public ActionAgentBuilder inputs(FieldSpec... specs) {
            this.inputs = List.of(specs);
            return this;
        }

        /**
         * 设置输出字段（varargs）
         */
        public ActionAgentBuilder outputs(FieldSpec... specs) {
            this.outputs = List.of(specs);
            return this;
        }

        public ActionAgentBuilder executeFunction(Function<Map<String, Object>, Map<String, Object>> fn) {
            this.executeFunction = fn;
            return this;
        }

        /**
         * 构建 ActionAgent 并自动调用 initModule()
         */
        public ActionAgent build() {
            ActionAgent agent = new ActionAgent();
            agent.setName(name);
            agent.setDescription(description);
            agent.setInputs(inputs);
            agent.setOutputs(outputs);
            agent.setExecuteFunction(executeFunction);
            agent.initModule();
            return agent;
        }
    }

    // ===================================================================
    // FieldSpec — 增加 of() 静态工厂
    // ===================================================================

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

        /**
         * 静态工厂方法（必填字段）
         */
        public static FieldSpec of(String name, String type, String description) {
            return new FieldSpec(name, type, description);
        }

        /**
         * 静态工厂方法（可选字段）
         */
        public static FieldSpec optional(String name, String type, String description) {
            return new FieldSpec(name, type, description, false);
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
                Map<String, Object> result = executeFunction.apply(input.toMap());
                return SimpleActionOutput.success(result);
            } catch (Exception e) {
                log.error("FunctionAction execution failed", e);
                return SimpleActionOutput.failure("Execution failed: " + e.getMessage());
            }
        }

        @Override
        public String[] getInputFields() {
            if (inputs == null) return new String[0];
            return inputs.stream().map(FieldSpec::getName).toArray(String[]::new);
        }

        @Override
        public String[] getOutputFields() {
            if (outputs == null) return new String[0];
            return outputs.stream().map(FieldSpec::getName).toArray(String[]::new);
        }
    }
}
