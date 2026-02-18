package io.leavesfly.evox.models.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * OpenAI 兼容的 Embedding 请求体
 *
 * @author EvoX Team
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmbeddingRequest {

    private String model;

    private List<String> input;
}
