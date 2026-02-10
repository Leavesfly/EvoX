package io.leavesfly.evox.claudecode.tool;

import io.leavesfly.evox.agents.skill.SkillRegistry;
import io.leavesfly.evox.agents.skill.SkillTool;
import io.leavesfly.evox.agents.skill.builtin.CodeReviewSkill;
import io.leavesfly.evox.agents.skill.builtin.RefactorSkill;
import io.leavesfly.evox.agents.skill.builtin.WriteTestSkill;
import io.leavesfly.evox.tools.agent.SubAgentTool;
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

        // sub-agent delegation (executor will be injected by CodingAgent)
        SubAgentTool subAgentTool = new SubAgentTool(workingDirectory, null);
        toolkit.addTool(subAgentTool);

        // skill execution
        SkillTool skillTool = new SkillTool(skillRegistry);
        toolkit.addTool(skillTool);

        log.info("Registered {} default tools for ClaudeCode", toolkit.getToolCount());
    }

    /**
     * 注册默认的内置技能
     */
    private void registerDefaultSkills() {
        skillRegistry.registerSkill(new CodeReviewSkill());
        skillRegistry.registerSkill(new WriteTestSkill());
        skillRegistry.registerSkill(new RefactorSkill());

        log.info("Registered {} default skills for ClaudeCode", skillRegistry.getSkillCount());
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
     * 获取 SubAgentTool 实例（用于注入 executor）
     */
    public SubAgentTool getSubAgentTool() {
        BaseTool tool = toolkit.getTool("sub_agent");
        if (tool instanceof SubAgentTool subAgentTool) {
            return subAgentTool;
        }
        return null;
    }
}
