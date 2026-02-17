package io.leavesfly.evox.assistant.evolution;

import io.leavesfly.evox.skill.BaseSkill;
import io.leavesfly.evox.skill.SkillMarketplace;
import io.leavesfly.evox.skill.SkillRegistry;
import io.leavesfly.evox.core.agent.IAgent;
import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.core.message.MessageType;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 技能生成器 — 让 Agent 自主生成新 Skill 并动态安装。
 *
 * <p>实现 OpenClaw 的"自我改进"核心能力：Agent 能自主编写代码生成新技能。
 *
 * <p>工作流程：
 * <ol>
 *   <li>用户描述需要的能力（或 Agent 自主识别能力缺口）</li>
 *   <li>Agent 生成 Skill 的定义（名称、描述、系统提示词、参数定义）</li>
 *   <li>SkillGenerator 将生成的定义包装为 DynamicSkill 实例</li>
 *   <li>通过 SkillMarketplace 动态安装到 SkillRegistry</li>
 *   <li>后续对话中 Agent 即可使用新 Skill</li>
 * </ol>
 */
@Slf4j
public class SkillGenerator {

    private final IAgent agent;
    private final SkillRegistry skillRegistry;
    private final SkillMarketplace skillMarketplace;

    /** 已生成的技能记录 */
    private final Map<String, GeneratedSkillRecord> generatedSkills = new ConcurrentHashMap<>();

    public SkillGenerator(IAgent agent, SkillRegistry skillRegistry, SkillMarketplace skillMarketplace) {
        this.agent = agent;
        this.skillRegistry = skillRegistry;
        this.skillMarketplace = skillMarketplace;
    }

    /**
     * 根据用户描述生成并安装新 Skill
     *
     * @param skillDescription 用户对新技能的描述
     * @return 生成结果
     */
    public GenerationResult generateAndInstall(String skillDescription) {
        log.info("Generating new skill from description: {}", skillDescription);

        try {
            String generationPrompt = buildGenerationPrompt(skillDescription);

            Message inputMessage = Message.builder()
                    .content(generationPrompt)
                    .messageType(MessageType.SYSTEM)
                    .build();
            inputMessage.putMetadata("skillGeneration", true);

            Message result = agent.execute("generate-skill", List.of(inputMessage));

            if (result == null || result.getContent() == null) {
                return GenerationResult.failure("Agent returned no response for skill generation");
            }

            String agentResponse = result.getContent().toString();
            SkillDefinition definition = parseSkillDefinition(agentResponse);

            if (definition == null) {
                return GenerationResult.failure("Failed to parse skill definition from Agent response");
            }

            if (skillRegistry.hasSkill(definition.getName())) {
                return GenerationResult.failure("Skill already exists: " + definition.getName());
            }

            DynamicSkill dynamicSkill = createDynamicSkill(definition);

            SkillMarketplace.SkillMetadata metadata = SkillMarketplace.SkillMetadata.builder()
                    .skillId(definition.getName())
                    .name(definition.getName())
                    .version("1.0.0-generated")
                    .author("EvoX Self-Evolution")
                    .description(definition.getDescription())
                    .category("generated")
                    .tags(List.of("auto-generated", "self-evolution"))
                    .installed(false)
                    .build();

            skillMarketplace.registerAvailableSkill(metadata);
            skillMarketplace.installSkill(definition.getName(), dynamicSkill);

            GeneratedSkillRecord record = GeneratedSkillRecord.builder()
                    .skillName(definition.getName())
                    .description(definition.getDescription())
                    .originalRequest(skillDescription)
                    .generatedAt(Instant.now())
                    .definition(definition)
                    .build();
            generatedSkills.put(definition.getName(), record);

            log.info("Successfully generated and installed skill: {}", definition.getName());

            return GenerationResult.builder()
                    .success(true)
                    .skillName(definition.getName())
                    .skillDescription(definition.getDescription())
                    .message("Skill '" + definition.getName() + "' generated and installed successfully")
                    .build();

        } catch (Exception e) {
            log.error("Skill generation failed", e);
            return GenerationResult.failure("Skill generation failed: " + e.getMessage());
        }
    }

    /**
     * 构建技能生成提示词
     */
    private String buildGenerationPrompt(String skillDescription) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("## Skill Generation Request\n\n");
        prompt.append("Generate a new skill based on the following description:\n\n");
        prompt.append("**User Request:** ").append(skillDescription).append("\n\n");

        prompt.append("### Existing Skills\n");
        List<BaseSkill> existingSkills = skillRegistry.getAllSkills();
        if (existingSkills.isEmpty()) {
            prompt.append("No existing skills.\n");
        } else {
            for (BaseSkill skill : existingSkills) {
                prompt.append("- **").append(skill.getName()).append("**: ");
                if (skill.getDescription() != null && !skill.getDescription().isBlank()) {
                    prompt.append(skill.getDescription());
                }
                if (skill.getWhenToUse() != null && !skill.getWhenToUse().isBlank()) {
                    if (skill.getDescription() != null && !skill.getDescription().isBlank()) {
                        prompt.append(" - ");
                    }
                    prompt.append(skill.getWhenToUse());
                }
                prompt.append("\n");
            }
        }

        prompt.append("\n### Output Format\n\n");
        prompt.append("Respond with a skill definition in the following exact format:\n\n");
        prompt.append("```\n");
        prompt.append("SKILL_NAME: <snake_case name>\n");
        prompt.append("SKILL_DESCRIPTION: <one-line description>\n");
        prompt.append("SYSTEM_PROMPT: <the system prompt that defines this skill's behavior>\n");
        prompt.append("WHEN_TO_USE: <description of when this skill should be used>\n");
        prompt.append("ALLOWED_TOOLS: <comma-separated list of tool names, or 'none'>\n");
        prompt.append("```\n\n");
        prompt.append("Requirements:\n");
        prompt.append("1. The skill name must be unique and not conflict with existing skills\n");
        prompt.append("2. The system prompt should be detailed and actionable\n");
        prompt.append("3. The skill should be self-contained and useful\n");
        prompt.append("4. Do not duplicate existing skill functionality\n");
        prompt.append("5. WHEN_TO_USE should guide the LLM on when to activate this skill\n");
        prompt.append("6. ALLOWED_TOOLS should list tools this skill needs (or 'none' if no tools needed)\n");

        return prompt.toString();
    }

    /**
     * 解析 Agent 返回的技能定义
     */
    private SkillDefinition parseSkillDefinition(String agentResponse) {
        try {
            String name = extractField(agentResponse, "SKILL_NAME");
            String description = extractField(agentResponse, "SKILL_DESCRIPTION");
            String systemPrompt = extractField(agentResponse, "SYSTEM_PROMPT");
            String whenToUse = extractField(agentResponse, "WHEN_TO_USE");
            String allowedToolsStr = extractField(agentResponse, "ALLOWED_TOOLS");

            if (name == null || description == null || systemPrompt == null) {
                log.warn("Missing required fields in skill definition");
                return null;
            }

            List<String> allowedTools = new ArrayList<>();
            if (allowedToolsStr != null && !allowedToolsStr.isBlank() && !"none".equalsIgnoreCase(allowedToolsStr.trim())) {
                for (String tool : allowedToolsStr.split(",")) {
                    String trimmed = tool.trim();
                    if (!trimmed.isEmpty()) {
                        allowedTools.add(trimmed);
                    }
                }
            }

            return SkillDefinition.builder()
                    .name(name.trim().toLowerCase().replace(" ", "_"))
                    .description(description.trim())
                    .systemPrompt(systemPrompt.trim())
                    .whenToUse(whenToUse != null ? whenToUse.trim() : null)
                    .allowedTools(allowedTools)
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse skill definition", e);
            return null;
        }
    }

    /**
     * 从文本中提取指定字段的值
     */
    private String extractField(String text, String fieldName) {
        String prefix = fieldName + ":";
        int startIndex = text.indexOf(prefix);
        if (startIndex == -1) {
            return null;
        }

        startIndex += prefix.length();
        int endIndex = text.indexOf("\n", startIndex);

        if (fieldName.equals("SYSTEM_PROMPT")) {
            int nextFieldIndex = findNextFieldIndex(text, startIndex);
            if (nextFieldIndex > startIndex) {
                endIndex = nextFieldIndex;
            }
        }

        if (endIndex == -1) {
            endIndex = text.length();
        }

        return text.substring(startIndex, endIndex).trim();
    }

    /**
     * 查找下一个字段的起始位置
     */
    private int findNextFieldIndex(String text, int fromIndex) {
        String[] fieldNames = {"SKILL_NAME:", "SKILL_DESCRIPTION:", "SYSTEM_PROMPT:", "WHEN_TO_USE:", "ALLOWED_TOOLS:"};
        int minIndex = text.length();
        for (String field : fieldNames) {
            int index = text.indexOf(field, fromIndex);
            if (index > fromIndex && index < minIndex) {
                minIndex = index;
            }
        }
        return minIndex;
    }

    /**
     * 创建动态 Skill 实例
     */
    private DynamicSkill createDynamicSkill(SkillDefinition definition) {
        DynamicSkill skill = new DynamicSkill(agent, definition);
        return skill;
    }

    /**
     * 获取所有已生成的技能记录
     */
    public Map<String, GeneratedSkillRecord> getGeneratedSkills() {
        return Collections.unmodifiableMap(generatedSkills);
    }

    /**
     * 卸载已生成的技能
     */
    public boolean uninstallGeneratedSkill(String skillName) {
        GeneratedSkillRecord record = generatedSkills.remove(skillName);
        if (record != null) {
            skillMarketplace.uninstallSkill(skillName);
            log.info("Uninstalled generated skill: {}", skillName);
            return true;
        }
        return false;
    }

    // ---- 内部数据类 ----

    /**
     * 技能定义（从 Agent 输出解析得到）
     */
    @Data
    @Builder
    public static class SkillDefinition {
        private String name;
        private String description;
        private String systemPrompt;
        private String whenToUse;
        private List<String> allowedTools;
    }

    /**
     * 生成结果
     */
    @Data
    @Builder
    public static class GenerationResult {
        private boolean success;
        private String skillName;
        private String skillDescription;
        private String message;

        public static GenerationResult failure(String message) {
            return GenerationResult.builder()
                    .success(false)
                    .message(message)
                    .build();
        }
    }

    /**
     * 已生成技能的记录
     */
    @Data
    @Builder
    public static class GeneratedSkillRecord {
        private String skillName;
        private String description;
        private String originalRequest;
        private Instant generatedAt;
        private SkillDefinition definition;
    }

    /**
     * 动态技能 — 由 Agent 自主生成的运行时 Skill。
     * 
     * <p>新架构下，DynamicSkill 不再实现 execute() 方法。
     * 而是通过设置 name、description、systemPrompt、whenToUse、allowedTools 等字段，
     * 让 BaseSkill.activate() 方法自动生成 SkillActivationResult。
     * 
     * <p>Skill 的行为完全由 systemPrompt 定义，LLM 根据这些 prompt 指令完成任务。
     */
    public static class DynamicSkill extends BaseSkill {

        private final IAgent agent;
        private final SkillDefinition definition;

        public DynamicSkill(IAgent agent, SkillDefinition definition) {
            this.agent = agent;
            this.definition = definition;

            setName(definition.getName());
            setDescription(definition.getDescription());
            setSystemPrompt(definition.getSystemPrompt());
            setWhenToUse(definition.getWhenToUse());
            setAllowedTools(definition.getAllowedTools() != null
                    ? definition.getAllowedTools() : new ArrayList<>());
            setModel("inherit");
            setBuiltin(false);
        }
    }
}