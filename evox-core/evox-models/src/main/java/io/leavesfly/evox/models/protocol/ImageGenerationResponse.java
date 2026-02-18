package io.leavesfly.evox.models.protocol;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * OpenAI 兼容的图像生成响应体
 *
 * @author EvoX Team
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImageGenerationResponse {

    private Long created;

    private List<ImageData> data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ImageData {
        private String url;

        @JsonProperty("b64_json")
        private String b64Json;

        @JsonProperty("revised_prompt")
        private String revisedPrompt;
    }
}
