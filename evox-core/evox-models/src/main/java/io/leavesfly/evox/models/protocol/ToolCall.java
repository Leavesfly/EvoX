package io.leavesfly.evox.models.protocol;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OpenAI 兼容的 Tool Call 数据模型
 * 表示 LLM 响应中的一次工具调用请求
 *
 * @author EvoX Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ToolCall {

    /**
     * 工具调用的唯一标识
     */
    private String id;

    /**
     * 工具类型，目前固定为 "function"
     */
    @Builder.Default
    private String type = "function";

    /**
     * 函数调用详情
     */
    private FunctionCall function;

    /**
     * 在流式响应中的索引位置
     */
    private Integer index;

    /**
     * 函数调用详情
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FunctionCall {

        /**
         * 函数名称
         */
        private String name;

        /**
         * 函数参数（JSON 字符串）
         */
        private String arguments;
    }
}
