package io.leavesfly.evox.skill;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Skill 激活结果。
 *
 * <p>对齐 Claude Code 的 Skill 执行机制：
 * Skill 不直接执行业务逻辑，而是返回上下文修改指令，包括：
 * <ul>
 *   <li>要注入到对话历史中的消息（metadata message + skill prompt）</li>
 *   <li>预批准的工具列表（修改执行上下文的权限）</li>
 *   <li>模型覆盖（修改执行上下文的模型选择）</li>
 * </ul>
 *
 * <p>对话注入遵循 Claude Code 的双消息机制：
 * <ol>
 *   <li>Message 1 (isMeta=false)：用户可见的状态消息（如 "The code_review skill is loading"）</li>
 *   <li>Message 2 (isMeta=true)：隐藏的 Skill prompt，仅发送给 LLM</li>
 * </ol>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillActivationResult {

    /** 是否激活成功 */
    @Builder.Default
    private boolean success = true;

    /** Skill 名称 */
    private String skillName;

    /**
     * 用户可见的元数据消息（isMeta=false）。
     * 格式示例：{@code <command-message>The "code_review" skill is loading</command-message>}
     */
    private String metadataMessage;

    /**
     * 隐藏的 Skill prompt 内容（isMeta=true）。
     * 此内容将作为隐藏指令注入到对话上下文中，仅发送给 LLM。
     */
    private String skillPrompt;

    /**
     * 预批准的工具列表。
     * 当 Skill 被激活时，这些工具将被临时预批准，无需用户确认。
     */
    @Builder.Default
    private List<String> allowedTools = new ArrayList<>();

    /**
     * 模型覆盖（null 表示使用当前会话模型）。
     */
    private String modelOverride;

    /** 错误信息（当 success=false 时） */
    private String error;

    /**
     * 创建成功的激活结果
     */
    public static SkillActivationResult success(String skillName, String metadataMessage,
                                                 String skillPrompt, List<String> allowedTools,
                                                 String modelOverride) {
        return SkillActivationResult.builder()
                .success(true)
                .skillName(skillName)
                .metadataMessage(metadataMessage)
                .skillPrompt(skillPrompt)
                .allowedTools(allowedTools != null ? allowedTools : new ArrayList<>())
                .modelOverride(modelOverride)
                .build();
    }

    /**
     * 创建失败的激活结果
     */
    public static SkillActivationResult failure(String error) {
        return SkillActivationResult.builder()
                .success(false)
                .error(error)
                .build();
    }
}
