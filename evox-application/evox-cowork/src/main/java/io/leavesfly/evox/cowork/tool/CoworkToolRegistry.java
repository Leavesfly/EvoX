package io.leavesfly.evox.cowork.tool;

import io.leavesfly.evox.agents.skill.SkillRegistry;
import io.leavesfly.evox.agents.skill.SkillTool;
import io.leavesfly.evox.agents.skill.builtin.CodeReviewSkill;
import io.leavesfly.evox.agents.skill.builtin.RefactorSkill;
import io.leavesfly.evox.agents.skill.builtin.WriteTestSkill;
import io.leavesfly.evox.tools.agent.SubAgentTool;
import io.leavesfly.evox.tools.base.BaseTool;
import io.leavesfly.evox.tools.base.Toolkit;
import io.leavesfly.evox.tools.browser.BrowserTool;
import io.leavesfly.evox.tools.file.FileEditTool;
import io.leavesfly.evox.tools.file.FileSystemTool;
import io.leavesfly.evox.tools.grep.GlobTool;
import io.leavesfly.evox.tools.grep.GrepTool;
import io.leavesfly.evox.tools.http.HttpTool;
import io.leavesfly.evox.tools.image.ImageTool;
import io.leavesfly.evox.tools.search.WebSearchTool;
import io.leavesfly.evox.tools.shell.ShellTool;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
@Getter
public class CoworkToolRegistry {
    private final Toolkit toolkit;
    private final SkillRegistry skillRegistry;
    private final String workingDirectory;

    public CoworkToolRegistry(String workingDirectory) {
        this.workingDirectory = workingDirectory;
        this.toolkit = new Toolkit("cowork-tools", "Cowork knowledge work tools");
        this.skillRegistry = new SkillRegistry();
        registerDefaultTools();
        registerDefaultSkills();
    }

    private void registerDefaultTools() {
        FileSystemTool fileSystemTool = new FileSystemTool();
        fileSystemTool.setWorkingDirectory(workingDirectory);
        toolkit.addTool(fileSystemTool);

        toolkit.addTool(new FileEditTool(workingDirectory));
        toolkit.addTool(new ShellTool(workingDirectory));
        toolkit.addTool(new GrepTool(workingDirectory));
        toolkit.addTool(new GlobTool(workingDirectory));
        toolkit.addTool(new SubAgentTool(workingDirectory, null));
        toolkit.addTool(new SkillTool(skillRegistry));
        toolkit.addTool(new HttpTool());
        toolkit.addTool(new WebSearchTool());
        toolkit.addTool(new BrowserTool());
        toolkit.addTool(new ImageTool());

        log.info("Registered {} default tools for Cowork", toolkit.getToolCount());
    }

    private void registerDefaultSkills() {
        skillRegistry.registerSkill(new CodeReviewSkill());
        skillRegistry.registerSkill(new WriteTestSkill());
        skillRegistry.registerSkill(new RefactorSkill());

        log.info("Registered {} default skills for Cowork", skillRegistry.getSkillCount());
    }

    public void registerTool(BaseTool tool) {
        toolkit.addTool(tool);
        log.info("Registered custom tool: {}", tool.getName());
    }

    public BaseTool getTool(String toolName) {
        return toolkit.getTool(toolName);
    }

    public BaseTool.ToolResult executeTool(String toolName, Map<String, Object> parameters) {
        return toolkit.executeTool(toolName, parameters);
    }

    public List<Map<String, Object>> getToolSchemas() {
        return toolkit.getToolSchemas();
    }

    public List<String> getToolNames() {
        return toolkit.getToolNames();
    }

    public String generateToolDescriptions() {
        StringBuilder descriptions = new StringBuilder();
        descriptions.append("## Available Tools\n\n");

        for (BaseTool tool : toolkit.getTools()) {
            descriptions.append("### ").append(tool.getName()).append("\n");
            descriptions.append(tool.getDescription()).append("\n\n");

            if (tool.getInputs() != null && !tool.getInputs().isEmpty()) {
                descriptions.append("**Parameters:**\n");
                for (Map.Entry<String, Map<String, Object>> entry : tool.getInputs().entrySet()) {
                    String paramName = entry.getKey();
                    Map<String, Object> paramInfo = entry.getValue();
                    String type = (String) paramInfo.get("type");
                    String required = (Boolean) paramInfo.getOrDefault("required", false) ? "required" : "optional";
                    String description = (String) paramInfo.get("description");
                    descriptions.append("- `").append(paramName).append("` (").append(type).append(") ")
                        .append(required).append(": ").append(description).append("\n");
                }
                descriptions.append("\n");
            }
        }

        descriptions.append(skillRegistry.generateSkillDescriptions());
        return descriptions.toString();
    }

    public SubAgentTool getSubAgentTool() {
        BaseTool tool = toolkit.getTool("sub_agent");
        if (tool instanceof SubAgentTool sat) {
            return sat;
        }
        return null;
    }
}
