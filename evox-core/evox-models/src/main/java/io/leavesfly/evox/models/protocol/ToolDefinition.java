package io.leavesfly.evox.models.protocol;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容的 Tool 定义
 * 用于在请求中声明 LLM 可调用的工具
 *
 * @author EvoX Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ToolDefinition {

    /**
     * 工具类型，目前固定为 "function"
     */
    @Builder.Default
    private String type = "function";

    /**
     * 函数定义
     */
    private FunctionDefinition function;

    /**
     * 函数定义
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FunctionDefinition {

        /**
         * 函数名称
         */
        private String name;

        /**
         * 函数描述
         */
        private String description;

        /**
         * 函数参数的 JSON Schema
         */
        private ParameterSchema parameters;
    }

    /**
     * 参数 Schema（JSON Schema 格式）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ParameterSchema {

        /**
         * 类型，固定为 "object"
         */
        @Builder.Default
        private String type = "object";

        /**
         * 属性定义
         */
        private Map<String, PropertySchema> properties;

        /**
         * 必需参数列表
         */
        private List<String> required;
    }

    /**
     * 单个属性的 Schema
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PropertySchema {

        /**
         * 属性类型
         */
        private String type;

        /**
         * 属性描述
         */
        private String description;

        /**
         * 枚举值（可选）
         */
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private List<String> enumValues;
    }

    /**
     * 从 BaseTool 的 Schema Map 构建 ToolDefinition
     * 兼容 BaseTool.getToolSchema() 返回的格式
     */
    public static ToolDefinition fromToolSchema(Map<String, Object> toolSchema) {
        @SuppressWarnings("unchecked")
        Map<String, Object> functionMap = (Map<String, Object>) toolSchema.get("function");
        if (functionMap == null) {
            return null;
        }

        String name = (String) functionMap.get("name");
        String description = (String) functionMap.get("description");

        FunctionDefinition.FunctionDefinitionBuilder funcBuilder = FunctionDefinition.builder()
                .name(name)
                .description(description);

        @SuppressWarnings("unchecked")
        Map<String, Object> paramsMap = (Map<String, Object>) functionMap.get("parameters");
        if (paramsMap != null) {
            ParameterSchema.ParameterSchemaBuilder paramBuilder = ParameterSchema.builder();

            @SuppressWarnings("unchecked")
            Map<String, Map<String, String>> propertiesMap =
                    (Map<String, Map<String, String>>) paramsMap.get("properties");
            if (propertiesMap != null) {
                Map<String, PropertySchema> properties = new java.util.LinkedHashMap<>();
                propertiesMap.forEach((propName, propDef) -> {
                    properties.put(propName, PropertySchema.builder()
                            .type(propDef.get("type"))
                            .description(propDef.get("description"))
                            .build());
                });
                paramBuilder.properties(properties);
            }

            @SuppressWarnings("unchecked")
            List<String> required = (List<String>) paramsMap.get("required");
            if (required != null) {
                paramBuilder.required(required);
            }

            funcBuilder.parameters(paramBuilder.build());
        }

        return ToolDefinition.builder()
                .function(funcBuilder.build())
                .build();
    }
}
