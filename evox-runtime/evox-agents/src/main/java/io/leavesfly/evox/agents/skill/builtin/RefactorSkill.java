package io.leavesfly.evox.agents.skill.builtin;

import io.leavesfly.evox.agents.skill.BaseSkill;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 代码重构技能
 * 分析代码结构并提供重构建议，支持多种重构策略
 */
@Slf4j
public class RefactorSkill extends BaseSkill {

    public RefactorSkill() {
        setName("refactor");
        setDescription("Analyze code structure and apply refactoring techniques to improve "
                + "readability, maintainability, and design. Supports extract method, rename, "
                + "simplify conditionals, remove duplication, and design pattern application.");

        setSystemPrompt(buildRefactorSystemPrompt());

        setRequiredTools(List.of("file_system", "file_edit", "grep", "glob"));

        Map<String, Map<String, String>> inputParams = new LinkedHashMap<>();

        Map<String, String> targetParam = new HashMap<>();
        targetParam.put("type", "string");
        targetParam.put("description", "File path or code section to refactor");
        inputParams.put("target", targetParam);

        Map<String, String> strategyParam = new HashMap<>();
        strategyParam.put("type", "string");
        strategyParam.put("description", "Refactoring strategy: 'extract_method', 'simplify', 'remove_duplication', "
                + "'design_pattern', 'clean_code', or 'auto' (default: 'auto')");
        inputParams.put("strategy", strategyParam);

        Map<String, String> preserveApiParam = new HashMap<>();
        preserveApiParam.put("type", "string");
        preserveApiParam.put("description", "Whether to preserve the public API: 'true' or 'false' (default: 'true')");
        inputParams.put("preserveApi", preserveApiParam);

        setInputParameters(inputParams);
        setRequiredInputs(List.of("target"));
    }

    @Override
    public SkillResult execute(SkillContext context) {
        validateInputs(context.getParameters());

        String target = context.getInput();
        String strategy = context.getParameters().getOrDefault("strategy", "auto").toString();
        String preserveApi = context.getParameters().getOrDefault("preserveApi", "true").toString();

        String prompt = buildPrompt(target, context.getAdditionalContext());

        log.info("Refactor skill executing: target={}, strategy={}, preserveApi={}", target, strategy, preserveApi);

        String refactorPrompt = prompt
                + "\n\nRefactoring strategy: " + strategy
                + "\nPreserve public API: " + preserveApi
                + "\n\nPlease analyze the code and provide:\n"
                + "1. **Code Smells Identified**: List specific issues found\n"
                + "   - Long methods, large classes, duplicated code\n"
                + "   - Complex conditionals, feature envy, data clumps\n"
                + "   - God objects, shotgun surgery patterns\n"
                + "2. **Refactoring Plan**: Step-by-step refactoring approach\n"
                + "3. **Refactored Code**: The improved code with explanations\n"
                + "4. **Impact Analysis**: What changes affect and potential risks\n"
                + "5. **Verification Steps**: How to verify the refactoring is correct";

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("skillName", "refactor");
        metadata.put("target", target);
        metadata.put("strategy", strategy);
        metadata.put("preserveApi", preserveApi);

        return SkillResult.success(refactorPrompt, metadata);
    }

    private String buildRefactorSystemPrompt() {
        return """
                You are an expert software architect specializing in code refactoring.
                
                When refactoring code, you should:
                1. Identify code smells using Martin Fowler's catalog
                2. Apply SOLID principles to improve design
                3. Use appropriate design patterns where they simplify the code
                4. Preserve existing behavior (refactoring should not change functionality)
                5. Make changes incrementally and explain each step
                6. Consider backward compatibility and public API stability
                7. Ensure the refactored code is more testable
                
                Common refactoring techniques you should apply:
                - Extract Method: Break long methods into smaller, focused ones
                - Extract Class: Split large classes by responsibility
                - Replace Conditional with Polymorphism
                - Introduce Parameter Object for methods with many parameters
                - Replace Magic Numbers with Named Constants
                - Remove Dead Code
                - Simplify Boolean Expressions
                
                Always explain WHY each refactoring improves the code, not just WHAT changed.""";
    }
}
