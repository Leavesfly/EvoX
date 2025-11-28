package io.leavesfly.evox.actions.coding;

import io.leavesfly.evox.actions.base.Action;
import io.leavesfly.evox.actions.base.ActionInput;
import io.leavesfly.evox.actions.base.ActionOutput;
import io.leavesfly.evox.models.base.BaseLLM;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CodeGeneration动作 - 代码生成
 * 根据需求生成高质量的可执行代码
 * 
 * @author EvoX Team
 */
@Slf4j
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CodeGenerationAction extends Action {

    /**
     * 默认提示词
     */
    private static final String DEFAULT_PROMPT =
        "You are an expert software developer. Generate clean, efficient, and well-documented code.\n\n" +
        "### Task:\n" +
        "{requirement}\n\n" +
        "### Programming Language:\n" +
        "{language}\n\n" +
        "### Additional Context:\n" +
        "{context}\n\n" +
        "### Requirements:\n" +
        "1. Write production-ready code with proper error handling\n" +
        "2. Include comprehensive comments explaining the logic\n" +
        "3. Follow best practices and design patterns\n" +
        "4. Ensure code is testable and maintainable\n" +
        "5. Add type hints/annotations where applicable\n\n" +
        "### Output Format:\n" +
        "Provide the complete code wrapped in markdown code blocks:\n" +
        "```{language}\n" +
        "// Your code here\n" +
        "```\n\n" +
        "Also include:\n" +
        "- Brief explanation of the implementation\n" +
        "- Usage examples\n" +
        "- Any important notes or limitations\n\n" +
        "Generate the code:";

    private String prompt;

    @Override
    public void initModule() {
        super.initModule();
        this.setName("CodeGeneration");
        this.setDescription("Generate production-ready code based on requirements");
        if (this.prompt == null) {
            this.prompt = DEFAULT_PROMPT;
        }
    }

    @Override
    public ActionOutput execute(ActionInput input) {
        if (!(input instanceof CodeGenerationInput)) {
            throw new IllegalArgumentException("Input must be CodeGenerationInput");
        }

        CodeGenerationInput codeInput = (CodeGenerationInput) input;

        // 验证必需字段
        if (codeInput.getRequirement() == null || codeInput.getRequirement().trim().isEmpty()) {
            throw new IllegalArgumentException("Requirement is required for code generation");
        }

        try {
            // 填充提示词
            String filledPrompt = prompt
                    .replace("{requirement}", codeInput.getRequirement())
                    .replace("{language}", codeInput.getLanguage() != null ? codeInput.getLanguage() : "Python")
                    .replace("{context}", codeInput.getContext() != null ? codeInput.getContext() : "No additional context");

            // 调用LLM
            if (getLlm() == null) {
                throw new IllegalStateException("LLM not set for CodeGenerationAction");
            }

            String response = getLlm().generate(filledPrompt);

            // 解析响应
            CodeGenerationOutput output = parseCodeResponse(response, codeInput.getLanguage());
            return output;

        } catch (Exception e) {
            log.error("Error executing CodeGenerationAction", e);
            CodeGenerationOutput errorOutput = new CodeGenerationOutput();
            errorOutput.setSuccess(false);
            errorOutput.setError(e.getMessage());
            return errorOutput;
        }
    }

    /**
     * 解析代码响应
     */
    private CodeGenerationOutput parseCodeResponse(String response, String language) {
        CodeGenerationOutput output = new CodeGenerationOutput();
        
        try {
            // 提取代码块
            List<String> codeBlocks = extractCodeBlocks(response);
            
            if (!codeBlocks.isEmpty()) {
                output.setCode(codeBlocks.get(0)); // 主代码
                if (codeBlocks.size() > 1) {
                    output.setAdditionalCode(codeBlocks.subList(1, codeBlocks.size()));
                }
            } else {
                // 没有代码块标记，尝试提取所有代码
                output.setCode(response);
            }

            // 提取说明部分
            output.setExplanation(extractExplanation(response));
            
            // 提取使用示例
            output.setUsageExample(extractUsageExample(response));
            
            output.setLanguage(language);
            output.setRawResponse(response);
            output.setSuccess(true);

        } catch (Exception e) {
            log.warn("Failed to parse code generation response", e);
            output.setCode(response);
            output.setSuccess(true);
        }

        return output;
    }

    /**
     * 提取代码块
     */
    private List<String> extractCodeBlocks(String response) {
        List<String> codeBlocks = new ArrayList<>();
        
        // 匹配 ```language ... ``` 格式
        Pattern pattern = Pattern.compile("```\\w*\\n([\\s\\S]*?)```", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(response);
        
        while (matcher.find()) {
            String code = matcher.group(1).trim();
            codeBlocks.add(code);
        }
        
        return codeBlocks;
    }

    /**
     * 提取说明
     */
    private String extractExplanation(String response) {
        // 查找 "Explanation:" 或 "Implementation:" 等标记
        String[] markers = {"Explanation:", "Implementation:", "Description:", "Note:"};
        
        for (String marker : markers) {
            int idx = response.indexOf(marker);
            if (idx != -1) {
                int endIdx = response.indexOf("\n\n", idx);
                if (endIdx == -1) {
                    endIdx = response.length();
                }
                return response.substring(idx + marker.length(), endIdx).trim();
            }
        }
        
        return null;
    }

    /**
     * 提取使用示例
     */
    private String extractUsageExample(String response) {
        String[] markers = {"Usage:", "Example:", "How to use:"};
        
        for (String marker : markers) {
            int idx = response.indexOf(marker);
            if (idx != -1) {
                int endIdx = response.indexOf("\n\n", idx);
                if (endIdx == -1) {
                    endIdx = response.length();
                }
                return response.substring(idx + marker.length(), endIdx).trim();
            }
        }
        
        return null;
    }

    @Override
    public String[] getInputFields() {
        return new String[]{"requirement", "language", "context"};
    }

    @Override
    public String[] getOutputFields() {
        return new String[]{"code", "explanation", "usage_example"};
    }

    /**
     * 代码生成输入
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CodeGenerationInput extends ActionInput {
        /**
         * 需求描述
         */
        private String requirement;

        /**
         * 编程语言（默认Python）
         */
        @Builder.Default
        private String language = "Python";

        /**
         * 额外上下文
         */
        private String context;
    }

    /**
     * 代码生成输出
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CodeGenerationOutput extends ActionOutput {
        /**
         * 生成的主代码
         */
        private String code;

        /**
         * 额外代码（如测试、配置等）
         */
        private List<String> additionalCode;

        /**
         * 实现说明
         */
        private String explanation;

        /**
         * 使用示例
         */
        private String usageExample;

        /**
         * 编程语言
         */
        private String language;

        /**
         * 原始响应
         */
        private String rawResponse;
    }
}
