package io.leavesfly.evox.claudecode.agent;

import io.leavesfly.evox.agents.specialized.ToolAwareAgent;
import io.leavesfly.evox.claudecode.tools.ShellCommandTool;
import io.leavesfly.evox.models.base.BaseLLM;
import io.leavesfly.evox.models.config.LLMConfig;
import io.leavesfly.evox.tools.base.BaseTool;
import io.leavesfly.evox.tools.file.FileSystemTool;
import io.leavesfly.evox.tools.interpreter.CodeInterpreterTool;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Claude Code 风格编程助手
 * 基于 EvoX ToolAwareAgent，装配文件读写、代码执行、Shell 命令等工具，
 * 实现“读代码库、执行命令、修改文件、运行测试”的 Agent 能力。
 *
 * @author EvoX Team
 */
@Slf4j
public final class ClaudeCodeAgent {

    private static final String DEFAULT_SYSTEM_PROMPT =
        "You are an expert coding assistant (Claude Code style). Your job is to help the user build, debug, and improve code.\n\n"
        + "You have access to these tools:\n"
        + "- file_system: read, write, append, delete, list files; create directories. Use for editing source files and configs.\n"
        + "- code_interpreter: run small code snippets (JavaScript, Groovy, or Python) in a sandbox. Use for quick computations or script checks.\n"
        + "- shell_command: run shell commands in the project directory (e.g. mvn test, git status, npm run build, python main.py). Use for builds, tests, and version control.\n\n"
        + "Guidelines:\n"
        + "1. Read the codebase with file_system (list/read) before making changes.\n"
        + "2. Edit files with file_system write/append when implementing features or fixes.\n"
        + "3. Run builds and tests with shell_command (mvn, npm, pytest, etc.).\n"
        + "4. Use code_interpreter only for small, self-contained snippets when appropriate.\n"
        + "Output your tool calls in this format:\n"
        + "TOOL: <tool_name>\n"
        + "PARAMS: {\"param1\": \"value1\", \"param2\": \"value2\"}\n"
        + "END_TOOL";

    private ClaudeCodeAgent() {}

    /**
     * 创建用于编程助手的工具列表：文件系统、代码解释器、Shell 命令
     *
     * @param projectRoot 项目根目录（绝对或相对路径）
     * @return 工具列表
     */
    public static List<BaseTool> createCodingTools(String projectRoot) {
        Path root = projectRoot != null && !projectRoot.isBlank()
                ? Paths.get(projectRoot).toAbsolutePath().normalize()
                : Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        String rootStr = root.toString();

        FileSystemTool fileTool = new FileSystemTool();
        fileTool.setWorkingDirectory(rootStr);
        fileTool.setAllowAbsolutePaths(true);
        fileTool.setAllowedExtensions(Arrays.asList(
                ".java", ".kt", ".py", ".js", ".ts", ".jsx", ".tsx", ".go", ".rs", ".c", ".cpp", ".h",
                ".xml", ".json", ".yml", ".yaml", ".md", ".txt", ".properties", ".gradle", ".sql", ".sh", ".bat"
        ));

        CodeInterpreterTool codeTool = new CodeInterpreterTool("javascript", root.resolve("workspace/interpreter"));

        ShellCommandTool shellTool = new ShellCommandTool(rootStr, 120L, true);

        List<BaseTool> tools = new ArrayList<>();
        tools.add(fileTool);
        tools.add(codeTool);
        tools.add(shellTool);
        return tools;
    }

    /**
     * 构建 Claude Code 风格编程助手 Agent（需调用方设置 LLM 后 initModule）
     *
     * @param projectRoot 项目根目录
     * @param llmConfig   LLM 配置（可为 null，后续再 set）
     * @param llm         LLM 实例（可为 null，若提供 llmConfig 则需由调用方通过 setLlm 设置）
     * @return 配置好工具和系统提示的 ToolAwareAgent
     */
    public static ToolAwareAgent create(
            String projectRoot,
            LLMConfig llmConfig,
            BaseLLM llm) {
        List<BaseTool> tools = createCodingTools(projectRoot);
        ToolAwareAgent agent = new ToolAwareAgent(
                "ClaudeCode",
                "Agentic coding assistant: read codebase, run commands, edit files, run tests.",
                DEFAULT_SYSTEM_PROMPT,
                llmConfig,
                llm,
                tools,
                Boolean.TRUE,
                10
        );
        agent.setHuman(false);
        if (llm != null) {
            agent.setLlm(llm);
        }
        if (llmConfig != null) {
            agent.setLlmConfig(llmConfig);
        }
        if (llm != null) {
            agent.initModule();
        }
        log.info("ClaudeCode agent created with project root: {}", projectRoot);
        return agent;
    }

    /**
     * 仅用 LLM 配置创建（不传 BaseLLM 实例，由 Agent 内部按 LLMConfig 创建 LLM 时使用）
     * 若框架支持从 LLMConfig 自动创建 LLM，可直接用此方法；否则调用方需 setLlm 后 initModule
     */
    public static ToolAwareAgent create(String projectRoot, LLMConfig llmConfig) {
        return create(projectRoot, llmConfig, null);
    }

    /**
     * 仅用项目根目录创建，不设置 LLM（调用方必须 setLlm/setLlmConfig 后 initModule）
     */
    public static ToolAwareAgent create(String projectRoot) {
        return create(projectRoot, null, null);
    }
}
