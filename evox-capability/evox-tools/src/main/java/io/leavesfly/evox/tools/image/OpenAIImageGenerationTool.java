package io.leavesfly.evox.tools.image;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.image.*;
import org.springframework.ai.openai.OpenAiImageModel;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.ai.openai.api.OpenAiImageApi;

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
     * OpenAI API客户端
     */
    private transient OpenAiImageModel imageModel;

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
            OpenAiImageApi imageApi = new OpenAiImageApi(apiKey);
            this.imageModel = new OpenAiImageModel(imageApi);
            log.info("OpenAI image model initialized: {}", model);
        } catch (Exception e) {
            log.error("Failed to initialize OpenAI image model", e);
        }
    }

    @Override
    protected ToolResult generateImage(String prompt, String size, String quality, int n) {
        if (imageModel == null) {
            return ToolResult.failure("OpenAI image model not initialized");
        }

        try {
            // 构建选项
            OpenAiImageOptions options = OpenAiImageOptions.builder()
                .withModel(model)
                .withN(n)
                .withWidth(parseWidth(size))
                .withHeight(parseHeight(size))
                .withQuality(quality)
                .withStyle(style)
                .build();

            // 创建请求
            ImagePrompt imagePrompt = new ImagePrompt(prompt, options);

            // 生成图像
            ImageResponse response = imageModel.call(imagePrompt);

            // 处理结果
            List<Map<String, Object>> images = new ArrayList<>();
            for (ImageGeneration generation : response.getResults()) {
                Map<String, Object> imageData = new HashMap<>();
                imageData.put("url", generation.getOutput().getUrl());
                imageData.put("b64_json", generation.getOutput().getB64Json());
                // revised_prompt may not be available in metadata
                if (generation.getMetadata() != null) {
                    imageData.put("revised_prompt", generation.getMetadata().toString());
                }
                images.add(imageData);
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

    /**
     * 解析宽度
     */
    private Integer parseWidth(String size) {
        if (size == null || !size.contains("x")) {
            return 1024;
        }
        try {
            return Integer.parseInt(size.split("x")[0]);
        } catch (NumberFormatException e) {
            return 1024;
        }
    }

    /**
     * 解析高度
     */
    private Integer parseHeight(String size) {
        if (size == null || !size.contains("x")) {
            return 1024;
        }
        try {
            return Integer.parseInt(size.split("x")[1]);
        } catch (NumberFormatException e) {
            return 1024;
        }
    }
}
