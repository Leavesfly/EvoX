package io.leavesfly.evox.mcp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * MCP工具定义
 * 表示可调用的工具或函数
 *
 * @author EvoX Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MCPTool {

    /**
     * 工具名称
     */
    private String name;

    /**
     * 工具描述
     */
    private String description;

    /**
     * 输入参数schema
     */
    private ParameterSchema inputSchema;

    /**
     * 工具执行器
     */
    private ToolExecutor executor;

    /**
     * 工具元数据
     */
    private Map<String, Object> metadata;

    /**
     * 参数Schema定义
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParameterSchema {
        /**
         * Schema类型
         */
        private String type;

        /**
         * 属性定义
         */
        private Map<String, PropertyDefinition> properties;

        /**
         * 必需参数列表
         */
        private List<String> required;
    }

    /**
     * 属性定义
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PropertyDefinition {
        /**
         * 属性类型
         */
        private String type;

        /**
         * 属性描述
         */
        private String description;

        /**
         * 枚举值
         */
        private List<Object> enumValues;

        /**
         * 默认值
         */
        private Object defaultValue;
    }

    /**
     * 工具执行器接口
     */
    @FunctionalInterface
    public interface ToolExecutor {
        /**
         * 执行工具
         *
         * @param arguments 工具参数
         * @return 执行结果
         */
        Object execute(Map<String, Object> arguments);
    }
}
