package io.leavesfly.evox.skill;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Skills 市场
 * 管理 Skill 的发现、安装、搜索
 */
@Slf4j
public class SkillMarketplace {

    private final SkillRegistry skillRegistry;

    private final Map<String, SkillMetadata> availableSkills = new ConcurrentHashMap<>();

    public SkillMarketplace(SkillRegistry skillRegistry) {
        if (skillRegistry == null) {
            throw new IllegalArgumentException("SkillRegistry cannot be null");
        }
        this.skillRegistry = skillRegistry;
    }

    public void registerAvailableSkill(SkillMetadata metadata) {
        if (metadata == null || metadata.getSkillId() == null) {
            throw new IllegalArgumentException("SkillMetadata and skillId cannot be null");
        }
        availableSkills.put(metadata.getSkillId(), metadata);
        log.info("Registered skill to marketplace: {} - {}", metadata.getSkillId(), metadata.getName());
    }

    public boolean installSkill(String skillId, BaseSkill skill) {
        if (skillId == null || skill == null) {
            throw new IllegalArgumentException("skillId and skill cannot be null");
        }

        SkillMetadata metadata = availableSkills.get(skillId);
        if (metadata == null) {
            log.warn("Cannot install skill: metadata not found for skillId: {}", skillId);
            return false;
        }

        skillRegistry.registerSkill(skill);
        metadata.setInstalled(true);
        metadata.setInstallCount(metadata.getInstallCount() + 1);
        log.info("Installed skill: {} (install count: {})", skillId, metadata.getInstallCount());
        return true;
    }

    public boolean uninstallSkill(String skillId) {
        SkillMetadata metadata = availableSkills.get(skillId);
        if (metadata == null) {
            log.warn("Cannot uninstall skill: metadata not found for skillId: {}", skillId);
            return false;
        }

        skillRegistry.removeSkill(skillId);
        metadata.setInstalled(false);
        log.info("Uninstalled skill: {}", skillId);
        return true;
    }

    public List<SkillMetadata> getAvailableSkills() {
        return new ArrayList<>(availableSkills.values());
    }

    public List<SkillMetadata> getInstalledSkills() {
        return availableSkills.values().stream()
                .filter(SkillMetadata::isInstalled)
                .collect(Collectors.toList());
    }

    public List<SkillMetadata> searchSkills(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return getAvailableSkills();
        }

        String lowerKeyword = keyword.toLowerCase();
        return availableSkills.values().stream()
                .filter(metadata -> {
                    boolean matchesName = metadata.getName() != null && metadata.getName().toLowerCase().contains(lowerKeyword);
                    boolean matchesDesc = metadata.getDescription() != null && metadata.getDescription().toLowerCase().contains(lowerKeyword);
                    boolean matchesTags = metadata.getTags() != null && metadata.getTags().stream()
                            .anyMatch(tag -> tag.toLowerCase().contains(lowerKeyword));
                    return matchesName || matchesDesc || matchesTags;
                })
                .collect(Collectors.toList());
    }

    public List<SkillMetadata> getSkillsByCategory(String category) {
        if (category == null || category.isBlank()) {
            return getAvailableSkills();
        }

        return availableSkills.values().stream()
                .filter(metadata -> category.equals(metadata.getCategory()))
                .collect(Collectors.toList());
    }

    public Optional<SkillMetadata> getSkillMetadata(String skillId) {
        return Optional.ofNullable(availableSkills.get(skillId));
    }

    public List<String> getCategories() {
        return availableSkills.values().stream()
                .map(SkillMetadata::getCategory)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    public void initBuiltinSkills() {
        List<BaseSkill> existingSkills = skillRegistry.getAllSkills();
        for (BaseSkill skill : existingSkills) {
            String skillId = skill.getName();
            if (!availableSkills.containsKey(skillId)) {
                SkillMetadata metadata = SkillMetadata.builder()
                        .skillId(skillId)
                        .name(skill.getName())
                        .version("1.0.0")
                        .author("EvoX Team")
                        .description(skill.getDescription())
                        .category("builtin")
                        .tags(new ArrayList<>())
                        .installed(true)
                        .installCount(1)
                        .rating(0.0)
                        .build();
                availableSkills.put(skillId, metadata);
                log.info("Initialized builtin skill metadata: {}", skillId);
            }
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillMetadata {
        private String skillId;
        private String name;
        private String version;
        private String author;
        private String description;
        private String category;
        private List<String> tags;
        private boolean installed;
        @Builder.Default
        private int installCount = 0;
        @Builder.Default
        private double rating = 0.0;
    }
}
