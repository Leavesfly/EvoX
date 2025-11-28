package io.leavesfly.evox.tools.image;

import io.leavesfly.evox.tools.base.BaseTool;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 图像生成工具基类
 * 支持通过LLM生成图像
 * 
 * @author EvoX Team
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class ImageGenerationTool extends BaseTool {

    /**
     * API密钥
     */
    protected String apiKey;

    /**
     * 模型名称
     */
    protected String model;

    /**
     * 默认图像尺寸
     */
    protected String defaultSize = "1024x1024";

    /**
     * 默认图像质量
     */
    protected String defaultQuality = "standard";

    public ImageGenerationTool() {
        this.name = "image_generation";
        this.description = "Generate images from text descriptions using AI models";
        initializeSchema();
    }

    private void initializeSchema() {
        Map<String, Map<String, String>> inputs = new HashMap<>();
        
        inputs.put("prompt", Map.of(
            "type", "string",
            "description", "Text description of the image to generate"
        ));
        
        inputs.put("size", Map.of(
            "type", "string",
            "description", "Image size (e.g., 1024x1024, 1792x1024)",
            "default", defaultSize
        ));
        
        inputs.put("quality", Map.of(
            "type", "string",
            "description", "Image quality: standard or hd",
            "default", defaultQuality
        ));
        
        inputs.put("n", Map.of(
            "type", "integer",
            "description", "Number of images to generate",
            "default", "1"
        ));

        this.inputs = inputs;
        this.required = List.of("prompt");
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        try {
            validateParameters(parameters);
            
            String prompt = getParameter(parameters, "prompt", "");
            String size = getParameter(parameters, "size", defaultSize);
            String quality = getParameter(parameters, "quality", defaultQuality);
            Integer n = getParameter(parameters, "n", 1);

            return generateImage(prompt, size, quality, n);
            
        } catch (Exception e) {
            log.error("Image generation failed", e);
            return ToolResult.failure("Image generation error: " + e.getMessage());
        }
    }

    /**
     * 生成图像(子类实现具体逻辑)
     * 
     * @param prompt 提示词
     * @param size 尺寸
     * @param quality 质量
     * @param n 生成数量
     * @return 工具执行结果
     */
    protected abstract ToolResult generateImage(String prompt, String size, String quality, int n);
}
