package io.leavesfly.evox.skill;

import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 技能注册中心。
 * 管理所有可用的 Skill，提供注册、发现和激活能力。
 *
 * <p>对齐 Claude Code 的 Skill 管理机制：
 * <ul>
 *   <li>支持从 SKILL.md 文件加载 Skill（推荐方式）</li>
 *   <li>支持代码注册 Skill（兼容旧方式）</li>
 *   <li>支持从 classpath 加载内置 Skill</li>
 *   <li>支持从项目目录加载自定义 Skill</li>
 * </ul>
 *
 * @see BaseSkill
 * @see SkillLoader
 * @see SkillTool
 */
@Slf4j
public class SkillRegistry {

    private final Map<String, BaseSkill> skills = new ConcurrentHashMap<>();

    /**
     * 注册技能
     */
    public void registerSkill(BaseSkill skill) {
        if (skill == null || skill.getName() == null) {
            throw new IllegalArgumentException("Skill and skill name cannot be null");
        }
        skills.put(skill.getName(), skill);
        log.info("Registered skill: {} - {}", skill.getName(), skill.getDescription());
    }

    /**
     * 批量注册技能
     */
    public void registerSkills(List<BaseSkill> skillList) {
        for (BaseSkill skill : skillList) {
            registerSkill(skill);
        }
    }

    /**
     * 从文件系统目录加载并注册 SKILL.md 文件。
     *
     * @param skillsDirectory 技能目录路径（如 .claude/skills/）
     * @return 加载的 Skill 数量
     */
    public int loadSkillsFromDirectory(Path skillsDirectory) {
        List<SkillDefinitionFile> definitions = SkillLoader.loadFromDirectory(skillsDirectory);
        int loaded = 0;
        for (SkillDefinitionFile definition : definitions) {
            if (definition.isValid()) {
                BaseSkill skill = BaseSkill.fromDefinition(definition);
                registerSkill(skill);
                loaded++;
            }
        }
        log.info("Loaded {} skills from directory: {}", loaded, skillsDirectory);
        return loaded;
    }

    /**
     * 从 classpath 资源加载并注册内置 SKILL.md 文件。
     *
     * @param resourcePrefix classpath 资源前缀（如 "skills/"）
     * @return 加载的 Skill 数量
     */
    public int loadBuiltinSkills(String resourcePrefix) {
        List<SkillDefinitionFile> definitions = SkillLoader.loadFromClasspath(resourcePrefix);
        int loaded = 0;
        for (SkillDefinitionFile definition : definitions) {
            if (definition.isValid()) {
                BaseSkill skill = BaseSkill.fromDefinition(definition);
                registerSkill(skill);
                loaded++;
            }
        }
        log.info("Loaded {} builtin skills from classpath: {}", loaded, resourcePrefix);
        return loaded;
    }

    /**
     * 从 classpath 的默认路径（skills/）加载内置 Skill。
     *
     * @return 加载的 Skill 数量
     */
    public int loadBuiltinSkills() {
        return loadBuiltinSkills("skills/");
    }

    /**
     * 获取技能
     */
    public BaseSkill getSkill(String skillName) {
        return skills.get(skillName);
    }

    /**
     * 检查技能是否存在
     */
    public boolean hasSkill(String skillName) {
        return skills.containsKey(skillName);
    }

    /**
     * 移除技能
     */
    public BaseSkill removeSkill(String skillName) {
        BaseSkill removed = skills.remove(skillName);
        if (removed != null) {
            log.info("Removed skill: {}", skillName);
        }
        return removed;
    }

    /**
     * 获取所有技能
     */
    public List<BaseSkill> getAllSkills() {
        return new ArrayList<>(skills.values());
    }

    /**
     * 获取所有技能名称
     */
    public List<String> getSkillNames() {
        return new ArrayList<>(skills.keySet());
    }

    /**
     * 获取技能数量
     */
    public int getSkillCount() {
        return skills.size();
    }

    /**
     * 激活技能 — 返回上下文注入结果。
     *
     * <p>对齐 Claude Code 的 Skill 执行机制：
     * 不直接执行业务逻辑，而是返回 {@link SkillActivationResult}。
     *
     * @param skillName 技能名称
     * @return 激活结果
     */
    public SkillActivationResult activateSkill(String skillName) {
        BaseSkill skill = skills.get(skillName);
        if (skill == null) {
            return SkillActivationResult.failure("Skill not found: " + skillName
                    + ". Available skills: " + String.join(", ", skills.keySet()));
        }

        try {
            log.info("Activating skill: {}", skillName);
            SkillActivationResult result = skill.activate();
            log.info("Skill {} activated: success={}", skillName, result.isSuccess());
            return result;
        } catch (Exception e) {
            log.error("Skill activation failed: {}", skillName, e);
            return SkillActivationResult.failure("Skill activation failed: " + e.getMessage());
        }
    }

    /**
     * 执行技能（兼容旧 API）。
     *
     * <p>通过激活 Skill 并将 SkillContext 中的输入作为 prompt 的一部分，
     * 返回 SkillResult。此方法桥接旧的执行模型和新的声明式激活模型。
     *
     * @param skillName 技能名称
     * @param context   执行上下文
     * @return 执行结果
     */
    public BaseSkill.SkillResult executeSkill(String skillName, BaseSkill.SkillContext context) {
        BaseSkill skill = skills.get(skillName);
        if (skill == null) {
            return BaseSkill.SkillResult.failure("Skill not found: " + skillName
                    + ". Available skills: " + String.join(", ", skills.keySet()));
        }

        try {
            log.info("Executing skill: {} with input: {}", skillName, context.getInput());
            SkillActivationResult activationResult = skill.activate();

            if (!activationResult.isSuccess()) {
                return BaseSkill.SkillResult.failure("Skill activation failed: " + activationResult.getError());
            }

            String output = activationResult.getSkillPrompt() != null
                    ? activationResult.getSkillPrompt()
                    : "Skill '" + skillName + "' activated successfully";
            return BaseSkill.SkillResult.success(output);
        } catch (Exception e) {
            log.error("Skill execution failed: {}", skillName, e);
            return BaseSkill.SkillResult.failure("Skill execution failed: " + e.getMessage());
        }
    }

    /**
     * 生成所有技能的描述文本（用于注入到系统提示词中）
     */
    public String generateSkillDescriptions() {
        if (skills.isEmpty()) {
            return "";
        }

        StringBuilder descriptions = new StringBuilder();
        descriptions.append("## Available Skills\n\n");
        descriptions.append("Skills are prompt-based capabilities that inject expert instructions ")
                .append("into the conversation context. Use the Skill tool to activate a skill.\n\n");

        for (BaseSkill skill : skills.values()) {
            if (skill.isDiscoverable()) {
                descriptions.append("- ").append(skill.toSkillListEntry()).append("\n");
            }
        }

        return descriptions.toString();
    }

    /**
     * 清空所有技能
     */
    public void clear() {
        skills.clear();
    }
}
