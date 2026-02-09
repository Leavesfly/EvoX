package io.leavesfly.evox.agents.skill;

import io.leavesfly.evox.tools.base.BaseTool;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 技能工具适配器
 * 将 Skill 暴露为 BaseTool，使 LLM 可以通过 function calling 调用技能。
 *
 * <p>这是 Skill 和 Tool 体系之间的桥梁：
 * <ul>
 *   <li>Skill 定义了高级能力（代码审查、写测试等）</li>
 *   <li>SkillTool 将其适配为标准的 BaseTool 接口</li>
 *   <li>LLM 可以像调用普通工具一样调用技能</li>
 * </ul>
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class SkillTool extends BaseTool {

    private final SkillRegistry skillRegistry;

    public SkillTool(SkillRegistry skillRegistry) {
        super();
        this.skillRegistry = skillRegistry;
        this.name = "skill";
        this.description = "Execute a high-level skill. Skills are specialized capabilities like code review, "
                + "writing tests, refactoring, etc. Each skill combines multiple tools with expert prompts "
                + "to accomplish complex tasks. Use 'list' operation to see available skills.";

        this.inputs = new HashMap<>();
        this.required = new ArrayList<>();

        Map<String, String> operationParam = new HashMap<>();
        operationParam.put("type", "string");
        operationParam.put("description", "Operation: 'execute' to run a skill, 'list' to list available skills");
        this.inputs.put("operation", operationParam);
        this.required.add("operation");

        Map<String, String> skillNameParam = new HashMap<>();
        skillNameParam.put("type", "string");
        skillNameParam.put("description", "Name of the skill to execute (required for 'execute' operation)");
        this.inputs.put("skillName", skillNameParam);

        Map<String, String> inputParam = new HashMap<>();
        inputParam.put("type", "string");
        inputParam.put("description", "Input/task description for the skill (required for 'execute' operation)");
        this.inputs.put("input", inputParam);

        Map<String, String> contextParam = new HashMap<>();
        contextParam.put("type", "string");
        contextParam.put("description", "Additional context for the skill, such as file content or search results (optional)");
        this.inputs.put("context", contextParam);
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        validateParameters(parameters);
        String operation = getParameter(parameters, "operation", "");

        return switch (operation) {
            case "list" -> listSkills();
            case "execute" -> executeSkill(parameters);
            default -> ToolResult.failure("Unknown operation: " + operation + ". Use 'list' or 'execute'.");
        };
    }

    private ToolResult listSkills() {
        List<BaseSkill> skills = skillRegistry.getAllSkills();
        if (skills.isEmpty()) {
            return ToolResult.success("No skills registered.");
        }

        List<Map<String, Object>> skillInfoList = new ArrayList<>();
        for (BaseSkill skill : skills) {
            Map<String, Object> skillInfo = new LinkedHashMap<>();
            skillInfo.put("name", skill.getName());
            skillInfo.put("description", skill.getDescription());
            skillInfo.put("requiredTools", skill.getRequiredTools());
            skillInfo.put("requiredInputs", skill.getRequiredInputs());
            skillInfoList.add(skillInfo);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalSkills", skills.size());
        result.put("skills", skillInfoList);
        return ToolResult.success(result);
    }

    private ToolResult executeSkill(Map<String, Object> parameters) {
        String skillName = getParameter(parameters, "skillName", "");
        String input = getParameter(parameters, "input", "");
        String context = getParameter(parameters, "context", "");

        if (skillName.isBlank()) {
            return ToolResult.failure("'skillName' is required for 'execute' operation");
        }
        if (input.isBlank()) {
            return ToolResult.failure("'input' is required for 'execute' operation");
        }

        BaseSkill.SkillContext skillContext = new BaseSkill.SkillContext(input);
        if (!context.isBlank()) {
            skillContext.setAdditionalContext(context);
        }

        BaseSkill.SkillResult skillResult = skillRegistry.executeSkill(skillName, skillContext);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("skillName", skillName);
        result.put("success", skillResult.isSuccess());

        if (skillResult.isSuccess()) {
            result.put("output", skillResult.getOutput());
            if (skillResult.getMetadata() != null && !skillResult.getMetadata().isEmpty()) {
                result.put("metadata", skillResult.getMetadata());
            }
            return ToolResult.success(result);
        } else {
            result.put("error", skillResult.getError());
            return ToolResult.failure("Skill execution failed: " + skillResult.getError());
        }
    }
}
