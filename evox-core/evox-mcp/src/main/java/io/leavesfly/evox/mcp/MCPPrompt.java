package io.leavesfly.evox.mcp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP提示模板
 * 表示可复用的提示模板，支持参数化
 *
 * @author EvoX Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MCPPrompt {

    /**
     * 提示名称（唯一标识）
     */
    private String name;

    /**
     * 提示描述
     */
    private String description;

    /**
     * 参数定义列表
     */
    @Builder.Default
    private List<PromptArgument> arguments = new ArrayList<>();

    /**
     * 提示元数据
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * 提示消息生成器
     */
    private PromptGenerator generator;

    /**
     * 参数定义
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PromptArgument {
        /**
         * 参数名称
         */
        private String name;

        /**
         * 参数描述
         */
        private String description;

        /**
         * 是否必需
         */
        @Builder.Default
        private boolean required = false;
    }

    /**
     * 提示消息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PromptMessage {
        /**
         * 角色：user, assistant, system
         */
        private String role;

        /**
         * 消息内容
         */
        private Content content;
    }

    /**
     * 消息内容
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Content {
        /**
         * 内容类型：text, image, resource
         */
        private String type;

        /**
         * 文本内容
         */
        private String text;

        /**
         * MIME类型（用于图片/资源）
         */
        private String mimeType;

        /**
         * Base64数据（用于图片）
         */
        private String data;

        /**
         * 资源URI
         */
        private String uri;

        /**
         * 创建文本内容
         */
        public static Content text(String text) {
            return Content.builder()
                    .type("text")
                    .text(text)
                    .build();
        }

        /**
         * 创建图片内容
         */
        public static Content image(String mimeType, String base64Data) {
            return Content.builder()
                    .type("image")
                    .mimeType(mimeType)
                    .data(base64Data)
                    .build();
        }

        /**
         * 创建资源引用内容
         */
        public static Content resource(String uri, String mimeType, String text) {
            return Content.builder()
                    .type("resource")
                    .uri(uri)
                    .mimeType(mimeType)
                    .text(text)
                    .build();
        }
    }

    /**
     * 提示生成结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GetPromptResult {
        /**
         * 提示描述
         */
        private String description;

        /**
         * 生成的消息列表
         */
        @Builder.Default
        private List<PromptMessage> messages = new ArrayList<>();
    }

    /**
     * 提示生成器接口
     */
    @FunctionalInterface
    public interface PromptGenerator {
        /**
         * 根据参数生成提示消息
         *
         * @param arguments 参数映射
         * @return 生成的提示结果
         */
        GetPromptResult generate(Map<String, String> arguments);
    }

    /**
     * 添加参数定义
     */
    public MCPPrompt addArgument(String name, String description, boolean required) {
        if (arguments == null) {
            arguments = new ArrayList<>();
        }
        arguments.add(PromptArgument.builder()
                .name(name)
                .description(description)
                .required(required)
                .build());
        return this;
    }

    /**
     * 生成提示
     */
    public GetPromptResult generate(Map<String, String> args) {
        // 验证必需参数
        if (arguments != null) {
            for (PromptArgument arg : arguments) {
                if (arg.isRequired() && (args == null || !args.containsKey(arg.getName()))) {
                    throw new IllegalArgumentException("缺少必需参数: " + arg.getName());
                }
            }
        }

        if (generator != null) {
            return generator.generate(args);
        }

        // 默认返回空结果
        return GetPromptResult.builder()
                .description(description)
                .messages(new ArrayList<>())
                .build();
    }

    /**
     * 快速创建简单文本提示
     */
    public static MCPPrompt simple(String name, String description, String templateText) {
        return MCPPrompt.builder()
                .name(name)
                .description(description)
                .generator(args -> {
                    String text = templateText;
                    if (args != null) {
                        for (Map.Entry<String, String> entry : args.entrySet()) {
                            text = text.replace("{" + entry.getKey() + "}", entry.getValue());
                        }
                    }
                    List<PromptMessage> messages = new ArrayList<>();
                    messages.add(PromptMessage.builder()
                            .role("user")
                            .content(Content.text(text))
                            .build());
                    return GetPromptResult.builder()
                            .description(description)
                            .messages(messages)
                            .build();
                })
                .build();
    }

    /**
     * 创建系统提示模板
     */
    public static MCPPrompt systemPrompt(String name, String description, 
                                          String systemMessage, String userTemplate) {
        return MCPPrompt.builder()
                .name(name)
                .description(description)
                .generator(args -> {
                    String userText = userTemplate;
                    if (args != null) {
                        for (Map.Entry<String, String> entry : args.entrySet()) {
                            userText = userText.replace("{" + entry.getKey() + "}", entry.getValue());
                        }
                    }
                    List<PromptMessage> messages = new ArrayList<>();
                    messages.add(PromptMessage.builder()
                            .role("system")
                            .content(Content.text(systemMessage))
                            .build());
                    messages.add(PromptMessage.builder()
                            .role("user")
                            .content(Content.text(userText))
                            .build());
                    return GetPromptResult.builder()
                            .description(description)
                            .messages(messages)
                            .build();
                })
                .build();
    }
}
