package io.leavesfly.evox.skill;

import io.leavesfly.evox.tools.base.BaseTool;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * Skill Meta-Tool — 对齐 Claude Code 的 Skill tool 架构。
 *
 * <p>SkillTool 是一个 meta-tool：它不直接执行业务逻辑，
 * 而是作为 Skill 系统和 LLM function calling 之间的桥梁。
 *
 * <p>核心机制（对齐 Claude Code）：
 * <ol>
 *   <li>SkillTool 出现在 LLM 的 tools 数组中</li>
 *   <li>其 description 动态嵌入 {@code <available_skills>} 列表</li>
 *   <li>LLM 通过推理决定调用哪个 skill（传入 command 参数）</li>
 *   <li>SkillTool 返回 {@link SkillActivationResult}，包含上下文注入指令</li>
 *   <li>调用方（如 CodingAgent）负责执行上下文注入和权限修改</li>
 * </ol>
 *
 * <p>与旧版的区别：
 * <ul>
 *   <li>旧版：直接执行 Skill 并返回结果</li>
 *   <li>新版：返回上下文修改指令，由调用方注入对话上下文</li>
 * </ul>
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
     * 动态生成 tool description，嵌入 {@code <available_skills>} 列表。
     * 对齐 Claude Code 的 Skill tool description 生成逻辑。
     *
     * <p>只有满足 {@link BaseSkill#isDiscoverable()} 的 Skill 才会出现在列表中。
     */
    @Override
    public String getDescription() {
        StringBuilder desc = new StringBuilder();
        desc.append("Activate a skill by name. Skills are specialized prompt-based capabilities ")
                .append("that inject expert instructions into the conversation context. ")
                .append("Each skill provides domain-specific guidance for tasks like code review, ")
                .append("writing tests, refactoring, etc.\n\n");
        desc.append("<available_skills>\n");

        List<BaseSkill> discoverableSkills = skillRegistry.getAllSkills().stream()
                .filter(BaseSkill::isDiscoverable)
                .toList();

        for (BaseSkill skill : discoverableSkills) {
            desc.append(skill.toSkillListEntry()).append("\n");
        }

        desc.append("</available_skills>");
        return desc.toString();
    }

    /**
     * 激活指定 Skill。
     *
     * <p>不直接执行业务逻辑，而是返回 {@link SkillActivationResult} 的序列化结果。
     * 调用方（如 CodingAgent）需要解析此结果并执行上下文注入。
     *
     * @param parameters 包含 "command" 参数（Skill 名称）
     * @return 包含 SkillActivationResult 序列化数据的 ToolResult
     */
    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        validateParameters(parameters);
        String command = getParameter(parameters, "command", "");

        if (command.isBlank()) {
            return ToolResult.failure("'command' parameter is required. "
                    + "Available skills: " + String.join(", ", skillRegistry.getSkillNames()));
        }

        // 去除可能的前导斜杠（对齐 Claude Code 的 command.trim().replace(/^\//, "")）
        String skillName = command.trim().replaceFirst("^/", "");

        BaseSkill skill = skillRegistry.getSkill(skillName);
        if (skill == null) {
            return ToolResult.failure("Unknown skill: " + skillName
                    + ". Available skills: " + String.join(", ", skillRegistry.getSkillNames()));
        }

        log.info("Activating skill: {}", skillName);

        SkillActivationResult activation = skill.activate();

        if (!activation.isSuccess()) {
            return ToolResult.failure("Skill activation failed: " + activation.getError());
        }

        // 将 SkillActivationResult 序列化为 Map 返回给调用方
        Map<String, Object> resultData = new LinkedHashMap<>();
        resultData.put("skillName", activation.getSkillName());
        resultData.put("success", true);
        resultData.put("metadataMessage", activation.getMetadataMessage());
        resultData.put("skillPrompt", activation.getSkillPrompt());
        resultData.put("allowedTools", activation.getAllowedTools());
        if (activation.getModelOverride() != null) {
            resultData.put("modelOverride", activation.getModelOverride());
        }

        return ToolResult.success(resultData);
    }

    /**
     * 覆盖 getToolSchema 以使用动态 description。
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
