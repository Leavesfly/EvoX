package io.leavesfly.evox.agents.skill;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 技能注册中心
 * 管理所有可用的 Skill，提供注册、发现和执行能力
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
     * 执行技能
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
            log.info("Executing skill: {}", skillName);
            BaseSkill.SkillResult result = skill.execute(context);
            log.info("Skill {} completed: success={}", skillName, result.isSuccess());
            return result;
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
        descriptions.append("Skills are high-level capabilities that combine multiple tools and specialized prompts.\n\n");

        for (BaseSkill skill : skills.values()) {
            descriptions.append("### ").append(skill.getName()).append("\n");
            descriptions.append(skill.getDescription()).append("\n");

            if (!skill.getInputParameters().isEmpty()) {
                descriptions.append("**Parameters:**\n");
                skill.getInputParameters().forEach((paramName, paramInfo) -> {
                    String paramType = paramInfo.getOrDefault("type", "string");
                    String paramDesc = paramInfo.getOrDefault("description", "");
                    boolean isRequired = skill.getRequiredInputs().contains(paramName);
                    descriptions.append("- `").append(paramName).append("` (").append(paramType).append(")")
                            .append(isRequired ? " **required**" : " optional")
                            .append(": ").append(paramDesc).append("\n");
                });
            }
            descriptions.append("\n");
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
