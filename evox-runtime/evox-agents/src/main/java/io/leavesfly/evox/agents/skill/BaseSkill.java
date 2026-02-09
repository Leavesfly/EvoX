package io.leavesfly.evox.agents.skill;

import io.leavesfly.evox.core.module.BaseModule;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 技能基类（Skill）
 * 将 Action + Tool + Prompt 封装为可复用的高级技能单元。
 *
 * <p>Skill 是比 Tool 更高层的抽象：
 * <ul>
 *   <li>Tool：单一的原子操作（如读文件、执行命令）</li>
 *   <li>Skill：面向特定场景的复合能力（如代码审查、写测试、重构）</li>
 * </ul>
 *
 * <p>每个 Skill 包含：
 * <ul>
 *   <li>专用的系统提示词（定义 Skill 的行为和输出格式）</li>
 *   <li>所需工具列表（Skill 执行时可使用的工具）</li>
 *   <li>输入/输出参数定义</li>
 * </ul>
 */
@Slf4j
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public abstract class BaseSkill extends BaseModule {

    /** 技能名称 */
    private String name;

    /** 技能描述（用于 LLM 理解何时使用此技能） */
    private String description;

    /** 技能专用的系统提示词 */
    private String systemPrompt;

    /** 技能所需的工具名称列表 */
    private List<String> requiredTools = new ArrayList<>();

    /** 技能输入参数定义 */
    private Map<String, Map<String, String>> inputParameters = new LinkedHashMap<>();

    /** 必需的输入参数 */
    private List<String> requiredInputs = new ArrayList<>();

    /**
     * 执行技能
     *
     * @param context 技能执行上下文
     * @return 技能执行结果
     */
    public abstract SkillResult execute(SkillContext context);

    /**
     * 生成技能的完整提示词（系统提示 + 用户输入）
     *
     * @param userInput 用户输入
     * @param additionalContext 额外上下文（如文件内容、搜索结果等）
     * @return 完整的提示词
     */
    public String buildPrompt(String userInput, String additionalContext) {
        StringBuilder prompt = new StringBuilder();

        if (systemPrompt != null && !systemPrompt.isBlank()) {
            prompt.append(systemPrompt).append("\n\n");
        }

        if (additionalContext != null && !additionalContext.isBlank()) {
            prompt.append("## Context\n\n").append(additionalContext).append("\n\n");
        }

        prompt.append("## Task\n\n").append(userInput);

        return prompt.toString();
    }

    /**
     * 验证输入参数
     *
     * @param inputs 输入参数
     * @throws IllegalArgumentException 如果缺少必需参数
     */
    public void validateInputs(Map<String, Object> inputs) {
        for (String required : requiredInputs) {
            if (!inputs.containsKey(required) || inputs.get(required) == null) {
                throw new IllegalArgumentException("Missing required input for skill '" + name + "': " + required);
            }
        }
    }

    /**
     * 技能执行上下文
     */
    @Data
    public static class SkillContext {
        /** 用户输入/任务描述 */
        private String input;

        /** 额外上下文信息（如文件内容） */
        private String additionalContext;

        /** 输入参数 */
        private Map<String, Object> parameters = new LinkedHashMap<>();

        /** 工作目录 */
        private String workingDirectory;

        public SkillContext(String input) {
            this.input = input;
        }

        public SkillContext(String input, Map<String, Object> parameters) {
            this.input = input;
            this.parameters = parameters != null ? parameters : new LinkedHashMap<>();
        }
    }

    /**
     * 技能执行结果
     */
    @Data
    public static class SkillResult {
        private boolean success;
        private String output;
        private String error;
        private Map<String, Object> metadata = new LinkedHashMap<>();

        public static SkillResult success(String output) {
            SkillResult result = new SkillResult();
            result.setSuccess(true);
            result.setOutput(output);
            return result;
        }

        public static SkillResult success(String output, Map<String, Object> metadata) {
            SkillResult result = new SkillResult();
            result.setSuccess(true);
            result.setOutput(output);
            result.setMetadata(metadata);
            return result;
        }

        public static SkillResult failure(String error) {
            SkillResult result = new SkillResult();
            result.setSuccess(false);
            result.setError(error);
            return result;
        }
    }
}
