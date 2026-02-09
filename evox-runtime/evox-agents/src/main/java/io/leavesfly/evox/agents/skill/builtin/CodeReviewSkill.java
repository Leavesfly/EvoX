package io.leavesfly.evox.agents.skill.builtin;

import io.leavesfly.evox.agents.skill.BaseSkill;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 代码审查技能
 * 对指定代码进行全面审查，检查潜在问题、代码风格、安全隐患和改进建议
 */
@Slf4j
public class CodeReviewSkill extends BaseSkill {

    public CodeReviewSkill() {
        setName("code_review");
        setDescription("Review code for bugs, security issues, performance problems, and style improvements. "
                + "Provide actionable feedback with specific line references and suggested fixes.");

        setSystemPrompt(buildCodeReviewSystemPrompt());

        setRequiredTools(List.of("file_system", "grep", "glob"));

        Map<String, Map<String, String>> inputParams = new LinkedHashMap<>();

        Map<String, String> targetParam = new HashMap<>();
        targetParam.put("type", "string");
        targetParam.put("description", "File path or code snippet to review");
        inputParams.put("target", targetParam);

        Map<String, String> focusParam = new HashMap<>();
        focusParam.put("type", "string");
        focusParam.put("description", "Specific focus area: 'security', 'performance', 'style', 'bugs', or 'all' (default: 'all')");
        inputParams.put("focus", focusParam);

        setInputParameters(inputParams);
        setRequiredInputs(List.of("target"));
    }

    @Override
    public SkillResult execute(SkillContext context) {
        validateInputs(context.getParameters());

        String target = context.getInput();
        String focus = context.getParameters().getOrDefault("focus", "all").toString();

        String prompt = buildPrompt(target, context.getAdditionalContext());

        log.info("Code review skill executing: target={}, focus={}", target, focus);

        String reviewPrompt = prompt + "\n\nFocus area: " + focus
                + "\n\nPlease provide a structured code review with the following sections:\n"
                + "1. **Summary**: Brief overview of the code\n"
                + "2. **Issues Found**: List of problems categorized by severity (Critical/Warning/Info)\n"
                + "3. **Security Concerns**: Any security vulnerabilities\n"
                + "4. **Performance**: Performance improvement opportunities\n"
                + "5. **Style & Best Practices**: Code style and best practice suggestions\n"
                + "6. **Suggested Fixes**: Concrete code changes for each issue";

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("skillName", "code_review");
        metadata.put("target", target);
        metadata.put("focus", focus);

        return SkillResult.success(reviewPrompt, metadata);
    }

    private String buildCodeReviewSystemPrompt() {
        return """
                You are an expert code reviewer with deep knowledge of software engineering best practices.
                
                When reviewing code, you should:
                1. Identify bugs, logic errors, and edge cases
                2. Check for security vulnerabilities (injection, XSS, CSRF, etc.)
                3. Evaluate performance characteristics and suggest optimizations
                4. Assess code readability, naming conventions, and documentation
                5. Verify error handling and resource management
                6. Check for thread safety issues in concurrent code
                7. Suggest design pattern improvements where applicable
                
                Format your review as structured markdown with clear severity levels:
                - 🔴 Critical: Must fix before merge
                - 🟡 Warning: Should fix, potential issues
                - 🔵 Info: Suggestions for improvement
                
                Always provide specific line references and concrete fix suggestions.""";
    }
}
