package io.leavesfly.evox.skill;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * SKILL.md 文件解析后的数据结构。
 *
 * <p>对齐 Claude Code 的 Skill 标准：
 * <ul>
 *   <li>YAML frontmatter 定义元数据（name, description, when_to_use, allowed-tools, model）</li>
 *   <li>Markdown 正文作为 Skill 的 prompt 模板</li>
 * </ul>
 *
 * <p>示例 SKILL.md 格式：
 * <pre>
 * ---
 * name: code_review
 * description: "Review code for bugs, security issues, performance problems"
 * when_to_use: "When user asks to review, check, or audit code quality"
 * allowed-tools:
 *   - file_system
 *   - grep
 *   - glob
 * model: inherit
 * ---
 *
 * You are an expert code reviewer...
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillDefinitionFile {

    /** Skill 唯一标识（snake_case） */
    private String name;

    /** 简短描述（用于 Skill 列表展示和 LLM 理解） */
    private String description;

    /**
     * LLM 判断何时使用此 Skill 的依据。
     * 对齐 Claude Code 的 when_to_use 字段。
     * description 和 whenToUse 至少需要一个。
     */
    private String whenToUse;

    /**
     * Skill 执行期间预批准的工具列表。
     * 对齐 Claude Code 的 allowed-tools 字段。
     * 当 Skill 被激活时，这些工具将被临时预批准，无需用户确认。
     */
    @Builder.Default
    private List<String> allowedTools = new ArrayList<>();

    /**
     * 模型覆盖。
     * "inherit" 表示使用当前会话模型，其他值表示指定模型名称。
     * null 表示不覆盖（等同于 inherit）。
     */
    private String model;

    /**
     * SKILL.md 正文内容（Markdown 格式的 prompt 模板）。
     * 当 Skill 被激活时，此内容将作为隐藏指令注入到对话上下文中。
     */
    private String promptContent;

    /** 文件来源路径（用于调试和日志） */
    private String sourcePath;

    /** 是否为内置 Skill（从 classpath 加载） */
    @Builder.Default
    private boolean builtin = false;

    /**
     * 检查此 Skill 定义是否有效。
     * 至少需要 name 和 (description 或 whenToUse) 其中之一。
     */
    public boolean isValid() {
        return name != null && !name.isBlank()
                && (description != null && !description.isBlank()
                || whenToUse != null && !whenToUse.isBlank());
    }

    /**
     * 判断模型是否为 inherit（使用当前会话模型）
     */
    public boolean isModelInherit() {
        return model == null || "inherit".equalsIgnoreCase(model);
    }
}
