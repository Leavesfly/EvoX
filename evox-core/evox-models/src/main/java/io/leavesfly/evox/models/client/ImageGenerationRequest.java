package io.leavesfly.evox.models.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * OpenAI 兼容的图像生成请求体
 *
 * @author EvoX Team
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ImageGenerationRequest {

    private String model;

    private String prompt;

    private Integer n;

    private String size;

    private String quality;

    private String style;

    @JsonProperty("response_format")
    private String responseFormat;
}
