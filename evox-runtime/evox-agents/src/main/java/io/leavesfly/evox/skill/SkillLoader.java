package io.leavesfly.evox.skill;

import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/**
 * SKILL.md 文件加载器。
 *
 * <p>对齐 Claude Code 的 Skill 加载机制：
 * <ul>
 *   <li>从文件系统目录扫描 SKILL.md 文件</li>
 *   <li>从 classpath 资源加载内置 SKILL.md 文件</li>
 *   <li>解析 YAML frontmatter + Markdown 正文</li>
 * </ul>
 *
 * <p>支持两种目录结构：
 * <ol>
 *   <li>子目录模式：{skillsDir}/{skillName}/SKILL.md</li>
 *   <li>根文件模式：{skillsDir}/SKILL.md（Skill 名称从 frontmatter 中读取）</li>
 * </ol>
 *
 * <p>扫描路径优先级（对齐 Claude Code）：
 * <ol>
 *   <li>项目级：{workingDirectory}/.claude/skills/</li>
 *   <li>用户级：~/.evox/skills/</li>
 *   <li>内置级：classpath resources/skills/</li>
 * </ol>
 */
@Slf4j
public class SkillLoader {

    private static final String SKILL_FILE_NAME = "SKILL.md";
    private static final String FRONTMATTER_DELIMITER = "---";

    private SkillLoader() {
    }

    /**
     * 从文件系统目录加载所有 SKILL.md 文件。
     *
     * @param skillsDirectory 技能目录路径
     * @return 解析后的 SkillDefinitionFile 列表
     */
    public static List<SkillDefinitionFile> loadFromDirectory(Path skillsDirectory) {
        List<SkillDefinitionFile> definitions = new ArrayList<>();

        if (skillsDirectory == null || !Files.exists(skillsDirectory) || !Files.isDirectory(skillsDirectory)) {
            log.debug("Skills directory does not exist or is not a directory: {}", skillsDirectory);
            return definitions;
        }

        try (Stream<Path> entries = Files.list(skillsDirectory)) {
            entries.forEach(entry -> {
                if (Files.isDirectory(entry)) {
                    // 子目录模式: {skillsDir}/{skillName}/SKILL.md
                    Path skillFile = entry.resolve(SKILL_FILE_NAME);
                    if (Files.exists(skillFile)) {
                        loadSkillFile(skillFile, entry.getFileName().toString(), false)
                                .ifPresent(definitions::add);
                    }
                } else if (SKILL_FILE_NAME.equals(entry.getFileName().toString())) {
                    // 根文件模式: {skillsDir}/SKILL.md
                    loadSkillFile(entry, null, false).ifPresent(definitions::add);
                }
            });
        } catch (IOException e) {
            log.error("Failed to scan skills directory: {}", skillsDirectory, e);
        }

        log.info("Loaded {} skills from directory: {}", definitions.size(), skillsDirectory);
        return definitions;
    }

    /**
     * 从 classpath 资源加载内置 SKILL.md 文件。
     *
     * @param resourcePrefix classpath 资源前缀（如 "skills/"）
     * @return 解析后的 SkillDefinitionFile 列表
     */
    public static List<SkillDefinitionFile> loadFromClasspath(String resourcePrefix) {
        List<SkillDefinitionFile> definitions = new ArrayList<>();

        try {
            // 尝试通过 classpath 资源加载
            ClassLoader classLoader = SkillLoader.class.getClassLoader();
            URL resourceUrl = classLoader.getResource(resourcePrefix);

            if (resourceUrl == null) {
                log.debug("No classpath resource found at: {}", resourcePrefix);
                return definitions;
            }

            String protocol = resourceUrl.getProtocol();
            if ("file".equals(protocol)) {
                // 开发环境：直接从文件系统读取
                Path resourcePath = Paths.get(resourceUrl.toURI());
                if (Files.isDirectory(resourcePath)) {
                    try (Stream<Path> entries = Files.list(resourcePath)) {
                        entries.filter(Files::isDirectory).forEach(skillDir -> {
                            Path skillFile = skillDir.resolve(SKILL_FILE_NAME);
                            if (Files.exists(skillFile)) {
                                loadSkillFile(skillFile, skillDir.getFileName().toString(), true)
                                        .ifPresent(definitions::add);
                            }
                        });
                    }
                }
            } else {
                // JAR 环境：通过 classpath 资源索引加载
                loadFromClasspathIndex(classLoader, resourcePrefix, definitions);
            }
        } catch (Exception e) {
            log.error("Failed to load skills from classpath: {}", resourcePrefix, e);
        }

        log.info("Loaded {} builtin skills from classpath: {}", definitions.size(), resourcePrefix);
        return definitions;
    }

    /**
     * 从 classpath 索引文件加载（JAR 环境）。
     * 使用 skills/index.txt 文件列出所有内置 Skill 名称。
     */
    private static void loadFromClasspathIndex(ClassLoader classLoader, String resourcePrefix,
                                               List<SkillDefinitionFile> definitions) {
        String indexPath = resourcePrefix + "index.txt";
        try (InputStream indexStream = classLoader.getResourceAsStream(indexPath)) {
            if (indexStream == null) {
                log.debug("No skill index file found at: {}", indexPath);
                return;
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(indexStream, StandardCharsets.UTF_8))) {
                String skillName;
                while ((skillName = reader.readLine()) != null) {
                    skillName = skillName.trim();
                    if (skillName.isEmpty() || skillName.startsWith("#")) {
                        continue;
                    }
                    String skillResourcePath = resourcePrefix + skillName + "/" + SKILL_FILE_NAME;
                    loadSkillFromClasspathResource(classLoader, skillResourcePath, skillName)
                            .ifPresent(definitions::add);
                }
            }
        } catch (IOException e) {
            log.error("Failed to read skill index: {}", indexPath, e);
        }
    }

    /**
     * 从 classpath 资源加载单个 SKILL.md 文件。
     */
    private static Optional<SkillDefinitionFile> loadSkillFromClasspathResource(
            ClassLoader classLoader, String resourcePath, String fallbackName) {
        try (InputStream inputStream = classLoader.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                log.warn("Skill resource not found: {}", resourcePath);
                return Optional.empty();
            }

            String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            SkillDefinitionFile definition = parseSkillMd(content, fallbackName);

            if (definition != null) {
                definition.setSourcePath("classpath:" + resourcePath);
                definition.setBuiltin(true);

                if (definition.isValid()) {
                    log.debug("Loaded builtin skill from classpath: {} ({})", definition.getName(), resourcePath);
                    return Optional.of(definition);
                } else {
                    log.warn("Invalid skill definition in classpath resource: {}", resourcePath);
                }
            }
        } catch (IOException e) {
            log.error("Failed to load skill from classpath: {}", resourcePath, e);
        }
        return Optional.empty();
    }

    /**
     * 从文件系统加载单个 SKILL.md 文件。
     */
    private static Optional<SkillDefinitionFile> loadSkillFile(Path skillFile, String fallbackName,
                                                               boolean isBuiltin) {
        try {
            String content = Files.readString(skillFile, StandardCharsets.UTF_8);
            SkillDefinitionFile definition = parseSkillMd(content, fallbackName);

            if (definition != null) {
                definition.setSourcePath(skillFile.toAbsolutePath().toString());
                definition.setBuiltin(isBuiltin);

                if (definition.isValid()) {
                    log.debug("Loaded skill: {} from {}", definition.getName(), skillFile);
                    return Optional.of(definition);
                } else {
                    log.warn("Invalid skill definition (missing name or description/when_to_use): {}", skillFile);
                }
            }
        } catch (IOException e) {
            log.error("Failed to read skill file: {}", skillFile, e);
        }
        return Optional.empty();
    }

    /**
     * 解析 SKILL.md 文件内容。
     * 分离 YAML frontmatter 和 Markdown 正文。
     *
     * @param content    文件完整内容
     * @param fallbackName 当 frontmatter 中没有 name 时使用的回退名称（通常是目录名）
     * @return 解析后的 SkillDefinitionFile，解析失败返回 null
     */
    @SuppressWarnings("unchecked")
    static SkillDefinitionFile parseSkillMd(String content, String fallbackName) {
        if (content == null || content.isBlank()) {
            return null;
        }

        String trimmedContent = content.trim();

        // 检查是否以 frontmatter 分隔符开头
        if (!trimmedContent.startsWith(FRONTMATTER_DELIMITER)) {
            // 没有 frontmatter，整个内容作为 prompt
            return SkillDefinitionFile.builder()
                    .name(fallbackName)
                    .promptContent(trimmedContent)
                    .build();
        }

        // 查找第二个 frontmatter 分隔符
        int secondDelimiterIndex = trimmedContent.indexOf(FRONTMATTER_DELIMITER, FRONTMATTER_DELIMITER.length());
        if (secondDelimiterIndex == -1) {
            // 没有结束分隔符，整个内容作为 prompt
            return SkillDefinitionFile.builder()
                    .name(fallbackName)
                    .promptContent(trimmedContent)
                    .build();
        }

        // 分离 frontmatter 和正文
        String frontmatterYaml = trimmedContent.substring(FRONTMATTER_DELIMITER.length(), secondDelimiterIndex).trim();
        String markdownBody = trimmedContent.substring(secondDelimiterIndex + FRONTMATTER_DELIMITER.length()).trim();

        try {
            Yaml yaml = new Yaml();
            Map<String, Object> frontmatter = yaml.load(frontmatterYaml);

            if (frontmatter == null) {
                frontmatter = new HashMap<>();
            }

            String name = getStringField(frontmatter, "name", fallbackName);
            String description = getStringField(frontmatter, "description", null);
            String whenToUse = getStringField(frontmatter, "when_to_use", null);
            String model = getStringField(frontmatter, "model", null);

            List<String> allowedTools = new ArrayList<>();
            Object allowedToolsObj = frontmatter.get("allowed-tools");
            if (allowedToolsObj == null) {
                allowedToolsObj = frontmatter.get("allowed_tools");
            }
            if (allowedToolsObj instanceof List<?> toolList) {
                for (Object tool : toolList) {
                    if (tool != null) {
                        allowedTools.add(tool.toString());
                    }
                }
            }

            return SkillDefinitionFile.builder()
                    .name(name)
                    .description(description)
                    .whenToUse(whenToUse)
                    .allowedTools(allowedTools)
                    .model(model)
                    .promptContent(markdownBody)
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse YAML frontmatter in SKILL.md", e);
            // 回退：整个内容作为 prompt
            return SkillDefinitionFile.builder()
                    .name(fallbackName)
                    .promptContent(trimmedContent)
                    .build();
        }
    }

    /**
     * 从 Map 中安全获取字符串字段。
     */
    private static String getStringField(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        if (value == null) {
            return defaultValue;
        }
        String strValue = value.toString().trim();
        return strValue.isEmpty() ? defaultValue : strValue;
    }
}
