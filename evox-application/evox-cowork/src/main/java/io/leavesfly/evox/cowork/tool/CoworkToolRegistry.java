package io.leavesfly.evox.cowork.tool;

import io.leavesfly.evox.skill.BaseSkill;
import io.leavesfly.evox.skill.SkillRegistry;
import io.leavesfly.evox.skill.SkillTool;

import io.leavesfly.evox.tools.task.TaskDelegationTool;
import io.leavesfly.evox.tools.base.BaseTool;
import io.leavesfly.evox.tools.base.Toolkit;
import io.leavesfly.evox.tools.file.FileEditTool;
import io.leavesfly.evox.tools.file.FileSystemTool;
import io.leavesfly.evox.tools.grep.GlobTool;
import io.leavesfly.evox.tools.grep.GrepTool;
import io.leavesfly.evox.tools.http.HttpTool;
import io.leavesfly.evox.tools.image.OpenAIImageGenerationTool;
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
        toolkit.addTool(new TaskDelegationTool(workingDirectory, null));
        toolkit.addTool(new SkillTool(skillRegistry));
        toolkit.addTool(new HttpTool());
        toolkit.addTool(new WebSearchTool());
        String imageApiKey = System.getenv("OPENAI_API_KEY");
        if (imageApiKey != null && !imageApiKey.isEmpty()) {
            toolkit.addTool(new OpenAIImageGenerationTool(imageApiKey));
        }

        log.info("Registered {} default tools for Cowork", toolkit.getToolCount());
    }

    private void registerDefaultSkills() {
        BaseSkill codeReviewSkill = new BaseSkill();
        codeReviewSkill.setName("code_review");
        codeReviewSkill.setDescription("Review code for bugs, security issues, and performance problems");
        codeReviewSkill.setWhenToUse("When user asks to review, check, or audit code quality");
        codeReviewSkill.setAllowedTools(List.of("file_system", "grep", "glob"));
        codeReviewSkill.setSystemPrompt(
                "You are an expert code reviewer. Analyze the code thoroughly for:\n"
                + "1. Bugs and logical errors\n"
                + "2. Security vulnerabilities\n"
                + "3. Performance issues\n"
                + "4. Code style and best practices\n"
                + "Provide clear, actionable feedback with specific line references.");
        skillRegistry.registerSkill(codeReviewSkill);

        BaseSkill writeTestSkill = new BaseSkill();
        writeTestSkill.setName("write_test");
        writeTestSkill.setDescription("Generate unit tests and integration tests for code");
        writeTestSkill.setWhenToUse("When user asks to write, create, or generate tests");
        writeTestSkill.setAllowedTools(List.of("file_system", "file_edit", "grep", "glob", "shell"));
        writeTestSkill.setSystemPrompt(
                "You are an expert test engineer. Write comprehensive tests that cover:\n"
                + "1. Happy path scenarios\n"
                + "2. Edge cases and boundary conditions\n"
                + "3. Error handling paths\n"
                + "4. Integration points\n"
                + "Follow the project's existing test conventions and frameworks.");
        skillRegistry.registerSkill(writeTestSkill);

        BaseSkill refactorSkill = new BaseSkill();
        refactorSkill.setName("refactor");
        refactorSkill.setDescription("Refactor code to improve structure, readability, and maintainability");
        refactorSkill.setWhenToUse("When user asks to refactor, restructure, or improve code quality");
        refactorSkill.setAllowedTools(List.of("file_system", "file_edit", "grep", "glob", "shell"));
        refactorSkill.setSystemPrompt(
                "You are an expert software architect. Refactor code to improve:\n"
                + "1. Code structure and organization\n"
                + "2. Readability and maintainability\n"
                + "3. Design patterns and SOLID principles\n"
                + "4. Reduce duplication and complexity\n"
                + "Ensure all changes preserve existing behavior.");
        skillRegistry.registerSkill(refactorSkill);

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
                for (Map.Entry<String, Map<String, String>> entry : tool.getInputs().entrySet()) {
                    String paramName = entry.getKey();
                    Map<String, String> paramInfo = entry.getValue();
                    String type = paramInfo.get("type");
                    boolean isRequired = tool.getRequired() != null && tool.getRequired().contains(paramName);
                    String requiredLabel = isRequired ? "required" : "optional";
                    String description = paramInfo.get("description");
                    descriptions.append("- `").append(paramName).append("` (").append(type).append(") ")
                        .append(requiredLabel).append(": ").append(description).append("\n");
                }
                descriptions.append("\n");
            }
        }

        descriptions.append(skillRegistry.generateSkillDescriptions());
        return descriptions.toString();
    }

    public TaskDelegationTool getTaskDelegationTool() {
        BaseTool tool = toolkit.getTool("delegate_task");
        if (tool instanceof TaskDelegationTool delegationTool) {
            return delegationTool;
        }
        return null;
    }
}
