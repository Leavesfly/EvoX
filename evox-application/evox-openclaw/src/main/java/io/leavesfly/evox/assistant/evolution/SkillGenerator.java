package io.leavesfly.evox.assistant.evolution;

import io.leavesfly.evox.agents.skill.BaseSkill;
import io.leavesfly.evox.agents.skill.SkillMarketplace;
import io.leavesfly.evox.agents.skill.SkillRegistry;
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
                prompt.append("- **").append(skill.getName()).append("**: ").append(skill.getDescription()).append("\n");
            }
        }

        prompt.append("\n### Output Format\n\n");
        prompt.append("Respond with a skill definition in the following exact format:\n\n");
        prompt.append("```\n");
        prompt.append("SKILL_NAME: <snake_case name>\n");
        prompt.append("SKILL_DESCRIPTION: <one-line description>\n");
        prompt.append("SYSTEM_PROMPT: <the system prompt that defines this skill's behavior>\n");
        prompt.append("REQUIRED_INPUTS: <comma-separated list of required input parameter names>\n");
        prompt.append("INPUT_PARAMS:\n");
        prompt.append("  <param_name>: <param_description>\n");
        prompt.append("```\n\n");
        prompt.append("Requirements:\n");
        prompt.append("1. The skill name must be unique and not conflict with existing skills\n");
        prompt.append("2. The system prompt should be detailed and actionable\n");
        prompt.append("3. The skill should be self-contained and useful\n");
        prompt.append("4. Do not duplicate existing skill functionality\n");

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
            String requiredInputsStr = extractField(agentResponse, "REQUIRED_INPUTS");

            if (name == null || description == null || systemPrompt == null) {
                log.warn("Missing required fields in skill definition");
                return null;
            }

            List<String> requiredInputs = new ArrayList<>();
            if (requiredInputsStr != null && !requiredInputsStr.isBlank()) {
                for (String input : requiredInputsStr.split(",")) {
                    String trimmed = input.trim();
                    if (!trimmed.isEmpty()) {
                        requiredInputs.add(trimmed);
                    }
                }
            }

            Map<String, String> inputParams = parseInputParams(agentResponse);

            return SkillDefinition.builder()
                    .name(name.trim().toLowerCase().replace(" ", "_"))
                    .description(description.trim())
                    .systemPrompt(systemPrompt.trim())
                    .requiredInputs(requiredInputs)
                    .inputParameters(inputParams)
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
        String[] fieldNames = {"SKILL_NAME:", "SKILL_DESCRIPTION:", "SYSTEM_PROMPT:", "REQUIRED_INPUTS:", "INPUT_PARAMS:"};
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
     * 解析输入参数定义
     */
    private Map<String, String> parseInputParams(String text) {
        Map<String, String> params = new LinkedHashMap<>();
        String paramsPrefix = "INPUT_PARAMS:";
        int startIndex = text.indexOf(paramsPrefix);
        if (startIndex == -1) {
            return params;
        }

        startIndex += paramsPrefix.length();
        String paramsSection = text.substring(startIndex);
        String[] lines = paramsSection.split("\n");

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("```")) {
                break;
            }
            int colonIndex = trimmed.indexOf(":");
            if (colonIndex > 0) {
                String paramName = trimmed.substring(0, colonIndex).trim();
                String paramDesc = trimmed.substring(colonIndex + 1).trim();
                if (!paramName.isEmpty() && !paramDesc.isEmpty()) {
                    params.put(paramName, paramDesc);
                }
            }
        }

        return params;
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
        private List<String> requiredInputs;
        private Map<String, String> inputParameters;
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
     * 通过委托给 Agent 执行来实现技能逻辑。
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
            setRequiredInputs(definition.getRequiredInputs() != null
                    ? definition.getRequiredInputs() : new ArrayList<>());

            if (definition.getInputParameters() != null) {
                Map<String, Map<String, String>> inputParamDefs = new LinkedHashMap<>();
                for (Map.Entry<String, String> entry : definition.getInputParameters().entrySet()) {
                    Map<String, String> paramDef = new HashMap<>();
                    paramDef.put("type", "string");
                    paramDef.put("description", entry.getValue());
                    inputParamDefs.put(entry.getKey(), paramDef);
                }
                setInputParameters(inputParamDefs);
            }
        }

        @Override
        public SkillResult execute(SkillContext context) {
            try {
                String fullPrompt = buildPrompt(context.getInput(), context.getAdditionalContext());

                Message inputMessage = Message.builder()
                        .content(fullPrompt)
                        .messageType(MessageType.INPUT)
                        .build();
                inputMessage.putMetadata("dynamicSkill", getName());

                Message result = agent.execute(null, List.of(inputMessage));

                if (result != null && result.getContent() != null) {
                    return SkillResult.success(result.getContent().toString(), Map.of(
                            "skillType", "dynamic",
                            "skillName", getName()
                    ));
                }

                return SkillResult.failure("Agent returned no response for dynamic skill: " + getName());
            } catch (Exception e) {
                return SkillResult.failure("Dynamic skill execution failed: " + e.getMessage());
            }
        }
    }
}
