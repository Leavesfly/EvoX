package io.leavesfly.evox.skill;

import io.leavesfly.evox.core.module.BaseModule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 技能基类（Skill）— 声明式 Prompt 模板。
 *
 * <p>对齐 Claude Code 的 Skill 标准：
 * Skill 是 <b>prompt-based conversation and execution context modifier</b>，
 * 而非可执行代码。当 Skill 被激活时，它通过注入 prompt 到对话上下文、
 * 修改工具权限来引导 LLM 完成特定任务。
 *
 * <p>Skill 与 Tool 的区别：
 * <ul>
 *   <li>Tool：单一的原子操作（如读文件、执行命令），有 execute() 方法</li>
 *   <li>Skill：面向特定场景的 prompt 模板（如代码审查、写测试），无 execute() 方法</li>
 * </ul>
 *
 * <p>每个 Skill 包含：
 * <ul>
 *   <li>专用的 prompt 模板（定义 Skill 的行为和输出格式）</li>
 *   <li>预批准工具列表（Skill 激活期间自动批准的工具）</li>
 *   <li>when_to_use 描述（LLM 判断何时使用此 Skill 的依据）</li>
 *   <li>可选的模型覆盖</li>
 * </ul>
 *
 * <p>Skill 可以从 SKILL.md 文件加载（推荐），也可以通过代码构建。
 *
 * @see SkillDefinitionFile
 * @see SkillLoader
 * @see SkillTool
 */
@Slf4j
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BaseSkill extends BaseModule {

    /** 技能名称（唯一标识，snake_case） */
    private String name;

    /** 技能描述（用于 Skill 列表展示和 LLM 理解） */
    private String description;

    /**
     * LLM 判断何时使用此 Skill 的依据。
     * 对齐 Claude Code 的 when_to_use 字段。
     * description 和 whenToUse 至少需要一个。
     */
    private String whenToUse;

    /**
     * Skill 的 prompt 模板内容。
     * 当 Skill 被激活时，此内容将作为隐藏指令注入到对话上下文中。
     * 对应 SKILL.md 文件的 Markdown 正文部分。
     */
    private String systemPrompt;

    /**
     * Skill 激活期间预批准的工具列表。
     * 对齐 Claude Code 的 allowed-tools 字段。
     * 这些工具在 Skill 激活后无需用户确认即可使用。
     */
    private List<String> allowedTools = new ArrayList<>();

    /**
     * 模型覆盖。
     * "inherit" 或 null 表示使用当前会话模型。
     */
    private String model;

    /** 文件来源路径（用于调试和日志） */
    private String sourcePath;

    /** 是否为内置 Skill */
    private boolean builtin;

    /**
     * 激活 Skill — 生成上下文注入结果。
     *
     * <p>对齐 Claude Code 的 Skill 执行机制：
     * 不直接执行业务逻辑，而是返回 {@link SkillActivationResult}，
     * 包含要注入到对话上下文中的消息和执行上下文修改。
     *
     * @return Skill 激活结果
     */
    public SkillActivationResult activate() {
        String metadataMessage = "<command-message>The \"" + name + "\" skill is loading</command-message>\n"
                + "<command-name>" + name + "</command-name>";

        String modelOverride = isModelInherit() ? null : model;

        return SkillActivationResult.success(
                name,
                metadataMessage,
                systemPrompt,
                allowedTools,
                modelOverride
        );
    }

    /**
     * 生成 Skill 在 SkillTool description 中的展示文本。
     * 对齐 Claude Code 的 formatSkill() 逻辑。
     *
     * @return 格式化的 Skill 列表条目
     */
    public String toSkillListEntry() {
        StringBuilder entry = new StringBuilder();
        entry.append("\"").append(name).append("\": ");
        if (description != null && !description.isBlank()) {
            entry.append(description);
        }
        if (whenToUse != null && !whenToUse.isBlank()) {
            if (description != null && !description.isBlank()) {
                entry.append(" - ");
            }
            entry.append(whenToUse);
        }
        return entry.toString();
    }

    /**
     * 判断模型是否为 inherit（使用当前会话模型）
     */
    public boolean isModelInherit() {
        return model == null || "inherit".equalsIgnoreCase(model);
    }

    /**
     * 检查此 Skill 是否可以被 SkillTool 发现和调用。
     * 对齐 Claude Code 的过滤逻辑：必须有 description 或 whenToUse。
     */
    public boolean isDiscoverable() {
        return (description != null && !description.isBlank())
                || (whenToUse != null && !whenToUse.isBlank());
    }

    /**
     * 获取此 Skill 所需的工具列表（兼容旧 API）。
     * 等同于 {@link #getAllowedTools()}。
     */
    public List<String> getRequiredTools() {
        return allowedTools;
    }

    /**
     * 获取此 Skill 的输入参数描述（兼容旧 API）。
     * 声明式 Skill 没有显式参数定义，返回空 Map。
     */
    public Map<String, String> getInputParameters() {
        return new HashMap<>();
    }

    // ==================== 兼容旧执行模型的内部类 ====================

    /**
     * Skill 执行上下文（兼容旧 API）。
     * 新架构下 Skill 通过 activate() 注入 prompt，不再直接执行。
     * 此类保留用于向后兼容。
     */
    @Data
    public static class SkillContext {
        private final String input;
        private final Map<String, Object> parameters;

        public SkillContext(String input) {
            this.input = input;
            this.parameters = new HashMap<>();
        }

        public SkillContext(String input, Map<String, Object> parameters) {
            this.input = input;
            this.parameters = parameters != null ? parameters : new HashMap<>();
        }
    }

    /**
     * Skill 执行结果（兼容旧 API）。
     * 新架构下 Skill 通过 activate() 返回 SkillActivationResult。
     * 此类保留用于向后兼容。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillResult {
        private boolean success;
        private String output;
        private String error;

        public static SkillResult success(String output) {
            return SkillResult.builder().success(true).output(output).build();
        }

        public static SkillResult failure(String error) {
            return SkillResult.builder().success(false).error(error).build();
        }
    }

    /**
     * 从 {@link SkillDefinitionFile} 构建 BaseSkill 实例。
     *
     * @param definition SKILL.md 解析后的定义
     * @return BaseSkill 实例
     */
    public static BaseSkill fromDefinition(SkillDefinitionFile definition) {
        BaseSkill skill = new BaseSkill();
        skill.setName(definition.getName());
        skill.setDescription(definition.getDescription());
        skill.setWhenToUse(definition.getWhenToUse());
        skill.setSystemPrompt(definition.getPromptContent());
        skill.setAllowedTools(definition.getAllowedTools() != null
                ? new ArrayList<>(definition.getAllowedTools()) : new ArrayList<>());
        skill.setModel(definition.getModel());
        skill.setSourcePath(definition.getSourcePath());
        skill.setBuiltin(definition.isBuiltin());
        return skill;
    }
}
