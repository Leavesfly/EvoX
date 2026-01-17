package io.leavesfly.evox.actions.reflection;

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

/**
 * ReflectionAction - 反思优化动作
 * 对已有输出进行反思和改进
 * 
 * @author EvoX Team
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class ReflectionAction extends Action {

    /**
     * 默认提示词
     */
    private static final String DEFAULT_PROMPT =
        "你是一位专业的审查专家，负责批判性地分析和改进输出内容。\n\n" +
        "### 任务：\n" +
        "{task_description}\n\n" +
        "### 当前输出：\n" +
        "{current_output}\n\n" +
        "### 评估标准：\n" +
        "{criteria}\n\n" +
        "### 已有反馈：\n" +
        "{feedback}\n\n" +
        "### 你的职责：\n" +
        "1. **分析**：根据评估标准批判性地评估当前输出\n" +
        "2. **识别问题**：指出弱点、错误或需要改进的地方\n" +
        "3. **提出建议**：提供具体、可执行的改进建议\n" +
        "4. **优化**：如有需要，生成改进后的版本\n\n" +
        "### 输出格式：\n" +
        "## 分析\n" +
        "你对当前输出的批判性评估。\n\n" +
        "## 发现的问题\n" +
        "- 问题1：描述及影响\n" +
        "- 问题2：描述及影响\n" +
        "...\n\n" +
        "## 改进建议\n" +
        "- 建议1：具体的改进建议\n" +
        "- 建议2：具体的改进建议\n" +
        "...\n\n" +
        "## 改进后的输出（如适用）\n" +
        "针对已识别问题进行优化后的版本。\n\n" +
        "请提供你的反思：";

    private String prompt;

    public ReflectionAction() {
        this.setName("Reflection");
        this.setDescription("Critically analyze and improve outputs through reflection");
        this.prompt = DEFAULT_PROMPT;
    }

    public ReflectionAction(String customPrompt) {
        this();
        this.prompt = customPrompt;
    }

    @Override
    public ActionOutput execute(ActionInput input) {
        if (!(input instanceof ReflectionInput)) {
            throw new IllegalArgumentException("Input must be ReflectionInput");
        }

        ReflectionInput reflectionInput = (ReflectionInput) input;

        // 验证必需字段
        if (reflectionInput.getCurrentOutput() == null || reflectionInput.getCurrentOutput().trim().isEmpty()) {
            throw new IllegalArgumentException("Current output is required for reflection");
        }

        try {
            // 填充提示词
            String filledPrompt = prompt
                    .replace("{task_description}", 
                        reflectionInput.getTaskDescription() != null ? reflectionInput.getTaskDescription() : "No task description provided")
                    .replace("{current_output}", reflectionInput.getCurrentOutput())
                    .replace("{criteria}", 
                        reflectionInput.getCriteria() != null ? reflectionInput.getCriteria() : "Quality, correctness, completeness")
                    .replace("{feedback}", 
                        reflectionInput.getFeedback() != null ? reflectionInput.getFeedback() : "No feedback provided");

            // 调用LLM
            if (getLlm() == null) {
                throw new IllegalStateException("LLM not set for ReflectionAction");
            }

            String response = getLlm().generate(filledPrompt);

            // 解析响应
            ReflectionOutput output = parseReflectionResponse(response);
            return output;

        } catch (Exception e) {
            log.error("Error executing ReflectionAction", e);
            ReflectionOutput errorOutput = new ReflectionOutput();
            errorOutput.setSuccess(false);
            errorOutput.setError(e.getMessage());
            return errorOutput;
        }
    }

    /**
     * 解析反思响应
     */
    private ReflectionOutput parseReflectionResponse(String response) {
        ReflectionOutput output = new ReflectionOutput();
        
        try {
            // 提取分析部分
            output.setAnalysis(extractSection(response, "## Analysis", "## "));
            
            // 提取问题列表
            String issuesSection = extractSection(response, "## Issues Found", "## ");
            output.setIssues(parseListItems(issuesSection));
            
            // 提取建议列表
            String recommendationsSection = extractSection(response, "## Recommendations", "## ");
            output.setRecommendations(parseListItems(recommendationsSection));
            
            // 提取改进后的输出
            output.setImprovedOutput(extractSection(response, "## Improved Output", null));
            
            output.setRawResponse(response);
            output.setSuccess(true);

        } catch (Exception e) {
            log.warn("Failed to parse reflection response", e);
            output.setAnalysis(response);
            output.setSuccess(true);
        }

        return output;
    }

    /**
     * 提取章节内容
     */
    private String extractSection(String response, String startMarker, String endMarker) {
        int startIdx = response.indexOf(startMarker);
        if (startIdx == -1) {
            return null;
        }
        
        startIdx += startMarker.length();
        
        int endIdx;
        if (endMarker != null) {
            endIdx = response.indexOf(endMarker, startIdx);
            if (endIdx == -1) {
                endIdx = response.length();
            }
        } else {
            endIdx = response.length();
        }
        
        return response.substring(startIdx, endIdx).trim();
    }

    /**
     * 解析列表项
     */
    private List<String> parseListItems(String section) {
        List<String> items = new ArrayList<>();
        
        if (section == null || section.isEmpty()) {
            return items;
        }
        
        String[] lines = section.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("-") || trimmed.startsWith("*") || trimmed.matches("^\\d+\\..*")) {
                // 移除列表标记
                String item = trimmed.replaceFirst("^[-*]\\s*|^\\d+\\.\\s*", "").trim();
                if (!item.isEmpty()) {
                    items.add(item);
                }
            }
        }
        
        return items;
    }

    @Override
    public String[] getInputFields() {
        return new String[]{"task_description", "current_output", "criteria", "feedback"};
    }

    @Override
    public String[] getOutputFields() {
        return new String[]{"analysis", "issues", "recommendations", "improved_output"};
    }

    /**
     * 反思输入
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReflectionInput extends ActionInput {
        /**
         * 任务描述
         */
        private String taskDescription;

        /**
         * 当前输出（需要反思的内容）
         */
        private String currentOutput;

        /**
         * 评估标准
         */
        private String criteria;

        /**
         * 已有反馈
         */
        private String feedback;
    }

    /**
     * 反思输出
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    public static class ReflectionOutput extends ActionOutput {
        /**
         * 分析结果
         */
        private String analysis;

        /**
         * 发现的问题列表
         */
        private List<String> issues;

        /**
         * 改进建议列表
         */
        private List<String> recommendations;

        /**
         * 改进后的输出
         */
        private String improvedOutput;

        /**
         * 原始响应
         */
        private String rawResponse;
    }
}
