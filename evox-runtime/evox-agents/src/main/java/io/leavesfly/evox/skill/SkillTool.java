package io.leavesfly.evox.skill;

import io.leavesfly.evox.tools.base.BaseTool;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * Skill Meta-Tool，作为 Skill 系统与 LLM function calling 之间的桥梁。
 *
 * SkillTool 不直接执行业务逻辑，核心机制：
 * 1. 出现在 LLM 的 tools 数组中
 * 2. description 动态嵌入 available_skills 列表
 * 3. LLM 通过推理决定调用哪个 skill（传入 command 参数）
 * 4. 返回 SkillActivationResult，包含上下文注入指令
 * 5. 调用方负责执行上下文注入和权限修改
 *
 * @see BaseSkill
 * @see SkillActivationResult
 * @see SkillRegistry
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class SkillTool extends BaseTool {

    /** Skill 工具的固定名称（大写 S 对齐 Claude Code 的 Skill tool） */
    public static final String TOOL_NAME = "Skill";

    private final SkillRegistry skillRegistry;

    public SkillTool(SkillRegistry skillRegistry) {
        super();
        this.skillRegistry = skillRegistry;
        this.name = TOOL_NAME;

        this.inputs = new HashMap<>();
        this.required = new ArrayList<>();

        Map<String, String> commandParam = new HashMap<>();
        commandParam.put("type", "string");
        commandParam.put("description", "The name of the skill to activate");
        this.inputs.put("command", commandParam);
        this.required.add("command");
    }

    /**
     * 动态生成 tool description，嵌入 available_skills 列表。
     * 仅包含满足 isDiscoverable 的 Skill。
     */
    @Override
    public String getDescription() {
        StringBuilder desc = new StringBuilder();
        desc.append("Activate a skill by name. Skills are specialized prompt-based capabilities ")
                .append("that inject expert instructions into the conversation context. ")
                .append("Each skill provides domain-specific guidance for tasks like code review, ")
                .append("writing tests, refactoring, etc.\n\n")
                .append("<available_skills>\n");

        skillRegistry.getAllSkills().stream()
                .filter(BaseSkill::isDiscoverable)
                .map(BaseSkill::toSkillListEntry)
                .forEach(entry -> desc.append(entry).append("\n"));

        desc.append("</available_skills>");
        return desc.toString();
    }

    /**
     * 激活指定 Skill。
     * 不直接执行业务逻辑，返回 SkillActivationResult 的序列化结果，由调用方解析并执行上下文注入。
     *
     * @param parameters 包含 "command" 参数（Skill 名称）
     * @return 包含 SkillActivationResult 序列化数据的 ToolResult
     */
    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        validateParameters(parameters);
        String skillName = extractSkillName(getParameter(parameters, "command", ""));

        if (skillName.isBlank()) {
            return ToolResult.failure(buildCommandRequiredMessage());
        }

        BaseSkill skill = skillRegistry.getSkill(skillName);
        if (skill == null) {
            return ToolResult.failure("Unknown skill: " + skillName + ". " + buildAvailableSkillsSuffix());
        }

        log.info("Activating skill: {}", skillName);
        SkillActivationResult activation = skill.activate();

        if (!activation.isSuccess()) {
            return ToolResult.failure("Skill activation failed: " + activation.getError());
        }

        return ToolResult.success(toResultData(activation));
    }

    private String extractSkillName(String command) {
        return command.trim().replaceFirst("^/", "");
    }

    private String buildCommandRequiredMessage() {
        return "'command' parameter is required. " + buildAvailableSkillsSuffix();
    }

    private String buildAvailableSkillsSuffix() {
        return "Available skills: " + String.join(", ", skillRegistry.getSkillNames());
    }

    private Map<String, Object> toResultData(SkillActivationResult activation) {
        Map<String, Object> resultData = new LinkedHashMap<>();
        resultData.put("skillName", activation.getSkillName());
        resultData.put("success", true);
        resultData.put("metadataMessage", activation.getMetadataMessage());
        resultData.put("skillPrompt", activation.getSkillPrompt());
        resultData.put("allowedTools", activation.getAllowedTools());
        if (activation.getModelOverride() != null) {
            resultData.put("modelOverride", activation.getModelOverride());
        }
        return resultData;
    }

    /**
     * 使用动态生成的 description 构建 tool schema。
     */
    @Override
    public Map<String, Object> getToolSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "function");

        Map<String, Object> function = new HashMap<>();
        function.put("name", name);
        function.put("description", getDescription());

        Map<String, Object> parametersSchema = new HashMap<>();
        parametersSchema.put("type", "object");
        parametersSchema.put("properties", inputs != null ? inputs : new HashMap<>());
        parametersSchema.put("required", required != null ? required : List.of());

        function.put("parameters", parametersSchema);
        schema.put("function", function);

        return schema;
    }
}
