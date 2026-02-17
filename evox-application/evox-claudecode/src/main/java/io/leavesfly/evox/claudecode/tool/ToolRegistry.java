package io.leavesfly.evox.claudecode.tool;

import io.leavesfly.evox.skill.SkillRegistry;
import io.leavesfly.evox.skill.SkillTool;
import io.leavesfly.evox.tools.task.TaskDelegationTool;
import io.leavesfly.evox.tools.base.BaseTool;
import io.leavesfly.evox.tools.base.Toolkit;
import io.leavesfly.evox.tools.file.FileEditTool;
import io.leavesfly.evox.tools.file.FileSystemTool;
import io.leavesfly.evox.tools.git.GitTool;
import io.leavesfly.evox.tools.grep.GlobTool;
import io.leavesfly.evox.tools.grep.GrepTool;
import io.leavesfly.evox.tools.project.ProjectContextTool;
import io.leavesfly.evox.tools.shell.ShellTool;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 工具注册中心
 * 管理 ClaudeCode 可用的所有工具，提供工具注册、查询和 Schema 生成
 */
@Slf4j
public class ToolRegistry {

    @Getter
    private final Toolkit toolkit;
    @Getter
    private final SkillRegistry skillRegistry;
    private final String workingDirectory;

    public ToolRegistry(String workingDirectory) {
        this.workingDirectory = workingDirectory;
        this.toolkit = new Toolkit("claudecode-tools", "ClaudeCode development tools");
        this.skillRegistry = new SkillRegistry();
        registerDefaultTools();
        registerDefaultSkills();
    }

    /**
     * 注册默认的开发者工具集
     */
    private void registerDefaultTools() {
        // file operations
        FileSystemTool fileSystemTool = new FileSystemTool();
        fileSystemTool.setWorkingDirectory(workingDirectory);
        toolkit.addTool(fileSystemTool);

        // file editing (diff-based)
        FileEditTool fileEditTool = new FileEditTool(workingDirectory);
        toolkit.addTool(fileEditTool);

        // shell command execution
        ShellTool shellTool = new ShellTool(workingDirectory);
        toolkit.addTool(shellTool);

        // text search (grep)
        GrepTool grepTool = new GrepTool(workingDirectory);
        toolkit.addTool(grepTool);

        // file path search (glob)
        GlobTool globTool = new GlobTool(workingDirectory);
        toolkit.addTool(globTool);

        // git operations
        GitTool gitTool = new GitTool(workingDirectory);
        toolkit.addTool(gitTool);

        // project context analysis
        ProjectContextTool projectContextTool = new ProjectContextTool(workingDirectory);
        toolkit.addTool(projectContextTool);

        // task delegation (executor will be injected by CodingAgent)
        TaskDelegationTool taskDelegationTool = new TaskDelegationTool(workingDirectory, null);
        toolkit.addTool(taskDelegationTool);

        // skill execution
        SkillTool skillTool = new SkillTool(skillRegistry);
        toolkit.addTool(skillTool);

        log.info("Registered {} default tools for ClaudeCode", toolkit.getToolCount());
    }

    /**
     * 注册默认的内置技能。
     * 从 classpath 的 skills/ 目录加载 SKILL.md 文件，
     * 并从项目工作目录的 .claude/skills/ 加载自定义 Skill。
     */
    private void registerDefaultSkills() {
        // 从 classpath 加载内置 SKILL.md 文件
        int builtinCount = skillRegistry.loadBuiltinSkills();

        // 从项目目录加载自定义 SKILL.md 文件
        java.nio.file.Path projectSkillsDir = java.nio.file.Path.of(workingDirectory, ".claude", "skills");
        int customCount = skillRegistry.loadSkillsFromDirectory(projectSkillsDir);

        // 从用户级目录加载 SKILL.md 文件
        String userHome = System.getProperty("user.home");
        if (userHome != null) {
            java.nio.file.Path userSkillsDir = java.nio.file.Path.of(userHome, ".evox", "skills");
            int userCount = skillRegistry.loadSkillsFromDirectory(userSkillsDir);
            log.info("Registered skills for ClaudeCode: {} builtin, {} project, {} user",
                    builtinCount, customCount, userCount);
        } else {
            log.info("Registered skills for ClaudeCode: {} builtin, {} project", builtinCount, customCount);
        }
    }

    /**
     * 注册自定义工具
     */
    public void registerTool(BaseTool tool) {
        toolkit.addTool(tool);
        log.info("Registered custom tool: {}", tool.getName());
    }

    /**
     * 获取指定工具
     */
    public BaseTool getTool(String toolName) {
        return toolkit.getTool(toolName);
    }

    /**
     * 执行工具
     */
    public BaseTool.ToolResult executeTool(String toolName, Map<String, Object> parameters) {
        return toolkit.executeTool(toolName, parameters);
    }

    /**
     * 获取所有工具的 Schema（用于 LLM function calling）
     */
    public List<Map<String, Object>> getToolSchemas() {
        return toolkit.getToolSchemas();
    }

    /**
     * 获取所有工具名称
     */
    public List<String> getToolNames() {
        return toolkit.getToolNames();
    }

    /**
     * 生成工具描述文本（用于注入到系统提示词中）
     */
    public String generateToolDescriptions() {
        StringBuilder descriptions = new StringBuilder();
        descriptions.append("## Available Tools\n\n");

        for (BaseTool tool : toolkit.getTools()) {
            descriptions.append("### ").append(tool.getName()).append("\n");
            descriptions.append(tool.getDescription()).append("\n\n");

            if (tool.getInputs() != null && !tool.getInputs().isEmpty()) {
                descriptions.append("**Parameters:**\n");
                tool.getInputs().forEach((paramName, paramInfo) -> {
                    String paramType = paramInfo.getOrDefault("type", "string");
                    String paramDesc = paramInfo.getOrDefault("description", "");
                    boolean isRequired = tool.getRequired() != null && tool.getRequired().contains(paramName);
                    descriptions.append("- `").append(paramName).append("` (").append(paramType).append(")")
                            .append(isRequired ? " **required**" : " optional")
                            .append(": ").append(paramDesc).append("\n");
                });
                descriptions.append("\n");
            }
        }

        // append skill descriptions
        String skillDescriptions = skillRegistry.generateSkillDescriptions();
        if (!skillDescriptions.isBlank()) {
            descriptions.append("\n").append(skillDescriptions);
        }

        return descriptions.toString();
    }

    /**
     * 使缓存的工具定义失效（当工具列表变化时调用）
     */
    public void invalidateToolDefinitionCache() {
        log.debug("Tool definition cache invalidated due to tool list change");
        // CodingAgent holds the cache; this is a signal for it to rebuild
        // The actual cache is in CodingAgent.cachedToolDefinitions
    }

    /**
     * 获取 TaskDelegationTool 实例（用于注入 executor）
     */
    public TaskDelegationTool getTaskDelegationTool() {
        BaseTool tool = toolkit.getTool("delegate_task");
        if (tool instanceof TaskDelegationTool delegationTool) {
            return delegationTool;
        }
        return null;
    }
}
