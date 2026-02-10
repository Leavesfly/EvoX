package io.leavesfly.evox.agents.action.extraction;

import io.leavesfly.evox.actions.base.Action;
import io.leavesfly.evox.actions.base.ActionInput;
import io.leavesfly.evox.actions.base.ActionOutput;
import io.leavesfly.evox.core.llm.ILLM;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 代码提取动作
 * 从LLM响应中提取代码块
 * 
 * @author EvoX Team
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class CodeExtractionAction extends Action {

    private static final String DEFAULT_PROMPT = """
            Please extract and generate code based on the following requirements:
            
            {requirements}
            
            Please provide your code in a code block using markdown syntax.
            """;

    /**
     * 提示词模板
     */
    private String promptTemplate = DEFAULT_PROMPT;

    /**
     * 目标编程语言
     */
    private String targetLanguage = "python";

    public CodeExtractionAction(ILLM llm) {
        this.setName("CodeExtraction");
        this.setDescription("Extract code from LLM response");
        this.setLlm(llm);
    }

    public CodeExtractionAction(ILLM llm, String targetLanguage) {
        this(llm);
        this.targetLanguage = targetLanguage;
    }

    @Override
    public ActionOutput execute(ActionInput input) {
        try {
            String requirements = String.valueOf(input.getData().getOrDefault("requirements", ""));
            String existingCode = String.valueOf(input.getData().getOrDefault("code", ""));

            String code;
            if (existingCode != null && !existingCode.isEmpty()) {
                // 从已有文本中提取代码
                code = extractCodeFromText(existingCode);
            } else {
                // 生成新代码
                code = generateCode(requirements);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("code", code);
            result.put("language", targetLanguage);

            ActionOutput output = new ActionOutput();
            output.setSuccess(true);
            output.setData(result);
            
            return output;

        } catch (Exception e) {
            log.error("Code extraction failed", e);
            ActionOutput output = new ActionOutput();
            output.setSuccess(false);
            output.setError(e.getMessage());
            return output;
        }
    }

    /**
     * 生成代码
     */
    private String generateCode(String requirements) {
        // 准备提示词
        String prompt = promptTemplate.replace("{requirements}", requirements);

        // 调用LLM生成代码
        String response = getLlm().generate(prompt);

        // 提取代码块
        return extractCodeFromText(response);
    }

    /**
     * 从文本中提取代码
     */
    private String extractCodeFromText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        // 尝试提取Markdown代码块
        String code = extractMarkdownCodeBlock(text);
        
        if (code != null && !code.isEmpty()) {
            return code;
        }

        // 如果没有找到代码块，返回原文本
        return text.trim();
    }

    /**
     * 提取Markdown代码块
     */
    private String extractMarkdownCodeBlock(String text) {
        // 匹配 ```language ... ``` 格式
        Pattern pattern = Pattern.compile(
            "```(?:" + targetLanguage + "|[a-z]*)\\s*\\n(.*?)```",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = pattern.matcher(text);
        
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        // 尝试匹配任何代码块
        pattern = Pattern.compile("```\\s*\\n(.*?)```", Pattern.DOTALL);
        matcher = pattern.matcher(text);
        
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return null;
    }

    /**
     * 清理代码（移除注释等）
     */
    private String sanitizeCode(String code, String entryPoint) {
        if (code == null || code.isEmpty()) {
            return code;
        }

        // 基本清理：移除多余的空行
        code = code.replaceAll("\\n{3,}", "\n\n");
        
        // 确保入口点存在
        if (entryPoint != null && !code.contains(entryPoint)) {
            log.warn("Entry point '{}' not found in code", entryPoint);
        }

        return code.trim();
    }

    @Override
    public String[] getInputFields() {
        return new String[]{"requirements", "code"};
    }

    @Override
    public String[] getOutputFields() {
        return new String[]{"code", "language"};
    }

    /**
     * 设置目标语言
     */
    public void setTargetLanguage(String language) {
        this.targetLanguage = language;
    }

    /**
     * 设置提示词模板
     */
    public void setPromptTemplate(String template) {
        this.promptTemplate = template;
    }
}
