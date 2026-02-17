package io.leavesfly.evox.agents.action;

import io.leavesfly.evox.agents.base.Agent;
import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.core.message.MessageType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.Function;

/**
 * ActionAgent 是一个专用智能体，直接执行提供的函数而不使用 LLM。
 *
 * <p>与普通 Agent 不同，ActionAgent 跳过了 Action 中间层，
 * 将用户传入的 {@code Function<Map, Map>} 直接作为执行逻辑，
 * 避免了 Message → ActionInput → ActionOutput → Message 的冗余转换。</p>
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
     * 执行函数：核心业务逻辑，接收参数 Map 并返回结果 Map
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

    @Override
    protected String getPrimaryActionName() {
        return "function_action";
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
    public Message execute(String actionName, List<Message> messages) {
        try {
            Map<String, Object> inputData = extractInputData(messages);
            validateInputData(inputData);
            Map<String, Object> result = executeFunction.apply(inputData);
            return Message.builder()
                    .messageType(MessageType.RESPONSE)
                    .content(result)
                    .build();
        } catch (Exception e) {
            log.error("ActionAgent '{}' execution failed", getName(), e);
            return Message.builder()
                    .messageType(MessageType.ERROR)
                    .content("Execution failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 从消息列表中提取输入参数
     */
    private Map<String, Object> extractInputData(List<Message> messages) {
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
     * 校验输入数据是否包含所有必填字段
     *
     * @throws IllegalArgumentException 如果缺少必填字段
     */
    private void validateInputData(Map<String, Object> inputData) {
        if (inputs == null) {
            return;
        }
        for (FieldSpec spec : inputs) {
            if (spec.isRequired() && !inputData.containsKey(spec.getName())) {
                throw new IllegalArgumentException(
                        "Missing required input field: '" + spec.getName() + "'");
            }
        }
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
    // FieldSpec — 字段规格定义
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
}
