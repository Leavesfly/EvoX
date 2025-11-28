package io.leavesfly.evox.tools.base;

import io.leavesfly.evox.core.module.BaseModule;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具基类 - 提供工具的统一抽象
 * 对应 Python 版本的 Tool
 */
@Slf4j
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public abstract class BaseTool extends BaseModule {

    /**
     * 工具名称
     */
    protected String name;

    /**
     * 工具描述
     */
    protected String description;

    /**
     * 输入参数定义
     * 格式: {"参数名": {"type": "string", "description": "参数描述"}}
     */
    protected Map<String, Map<String, String>> inputs;

    /**
     * 必需参数列表
     */
    protected List<String> required;

    /**
     * 执行工具
     *
     * @param parameters 工具参数
     * @return 工具执行结果
     */
    public abstract ToolResult execute(Map<String, Object> parameters);

    /**
     * 获取工具 Schema（用于 LLM function calling）
     *
     * @return 工具 Schema
     */
    public Map<String, Object> getToolSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "function");

        Map<String, Object> function = new HashMap<>();
        function.put("name", name);
        function.put("description", description);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "object");
        parameters.put("properties", inputs != null ? inputs : new HashMap<>());
        parameters.put("required", required != null ? required : List.of());

        function.put("parameters", parameters);
        schema.put("function", function);

        return schema;
    }

    /**
     * 验证参数
     *
     * @param parameters 工具参数
     * @throws IllegalArgumentException 参数验证失败
     */
    protected void validateParameters(Map<String, Object> parameters) {
        if (required != null) {
            for (String requiredParam : required) {
                if (!parameters.containsKey(requiredParam) || parameters.get(requiredParam) == null) {
                    throw new IllegalArgumentException("Missing required parameter: " + requiredParam);
                }
            }
        }
    }

    /**
     * 获取参数值
     *
     * @param parameters 工具参数
     * @param key 参数名
     * @param defaultValue 默认值
     * @return 参数值
     */
    @SuppressWarnings("unchecked")
    protected <T> T getParameter(Map<String, Object> parameters, String key, T defaultValue) {
        Object value = parameters.get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return (T) value;
        } catch (ClassCastException e) {
            log.warn("Parameter '{}' has wrong type, using default value", key);
            return defaultValue;
        }
    }

    /**
     * 工具执行结果
     */
    @Data
    @NoArgsConstructor
    public static class ToolResult {
        /**
         * 是否成功
         */
        private boolean success;

        /**
         * 结果数据
         */
        private Object data;

        /**
         * 错误信息（如果失败）
         */
        private String error;

        /**
         * 元数据
         */
        private Map<String, Object> metadata;

        public static ToolResult success(Object data) {
            ToolResult result = new ToolResult();
            result.setSuccess(true);
            result.setData(data);
            return result;
        }

        public static ToolResult success(Object data, Map<String, Object> metadata) {
            ToolResult result = new ToolResult();
            result.setSuccess(true);
            result.setData(data);
            result.setMetadata(metadata);
            return result;
        }

        public static ToolResult failure(String error) {
            ToolResult result = new ToolResult();
            result.setSuccess(false);
            result.setError(error);
            return result;
        }

        public static ToolResult failure(String error, Map<String, Object> metadata) {
            ToolResult result = new ToolResult();
            result.setSuccess(false);
            result.setError(error);
            result.setMetadata(metadata);
            return result;
        }
    }
}
