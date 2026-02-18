package io.leavesfly.evox.tools.image;

import io.leavesfly.evox.models.protocol.ImageGenerationRequest;
import io.leavesfly.evox.models.protocol.ImageGenerationResponse;
import io.leavesfly.evox.models.protocol.OpenAiCompatibleClient;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.*;

/**
 * OpenAI 图像生成工具
 * 支持 DALL-E 2 和 DALL-E 3 模型
 *
 * @author EvoX Team
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class OpenAIImageGenerationTool extends ImageGenerationTool {

    /**
     * HTTP 客户端
     */
    private transient OpenAiCompatibleClient client;

    /**
     * 图像风格
     */
    private String style = "vivid";

    public OpenAIImageGenerationTool(String apiKey) {
        this(apiKey, "dall-e-3");
    }

    public OpenAIImageGenerationTool(String apiKey, String model) {
        super();
        this.apiKey = apiKey;
        this.model = model;
        this.name = "openai_image_generation";
        this.description = "Generate images using OpenAI DALL-E models";
        initializeClient();
    }

    @Override
    public void initModule() {
        super.initModule();
        initializeClient();
    }

    private void initializeClient() {
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("OpenAI API key is not set");
            return;
        }

        try {
            this.client = new OpenAiCompatibleClient(
                    "https://api.openai.com/v1", apiKey, Duration.ofSeconds(120));
            log.info("OpenAI image model initialized: {}", model);
        } catch (Exception e) {
            log.error("Failed to initialize OpenAI image client", e);
        }
    }

    @Override
    protected ToolResult generateImage(String prompt, String size, String quality, int n) {
        if (client == null) {
            return ToolResult.failure("OpenAI image client not initialized");
        }

        try {
            ImageGenerationRequest request = ImageGenerationRequest.builder()
                    .model(model)
                    .prompt(prompt)
                    .n(n)
                    .size(size)
                    .quality(quality)
                    .style(style)
                    .build();

            ImageGenerationResponse response = client.imageGeneration(request);

            if (response == null || response.getData() == null) {
                return ToolResult.failure("No image data returned");
            }

            List<Map<String, Object>> images = new ArrayList<>();
            for (ImageGenerationResponse.ImageData imageData : response.getData()) {
                Map<String, Object> entry = new HashMap<>();
                entry.put("url", imageData.getUrl());
                entry.put("b64_json", imageData.getB64Json());
                if (imageData.getRevisedPrompt() != null) {
                    entry.put("revised_prompt", imageData.getRevisedPrompt());
                }
                images.add(entry);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("images", images);
            result.put("count", images.size());
            result.put("model", model);

            return ToolResult.success(result);

        } catch (Exception e) {
            log.error("Image generation failed: {}", prompt, e);
            return ToolResult.failure("Image generation failed: " + e.getMessage());
        }
    }
}
