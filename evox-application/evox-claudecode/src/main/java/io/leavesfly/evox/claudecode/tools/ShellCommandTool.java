package io.leavesfly.evox.claudecode.tools;

import io.leavesfly.evox.tools.base.BaseTool;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Shell 命令执行工具
 * 在指定工作目录下执行 shell 命令（如 mvn、git、npm、python 等），用于 ClaudeCode 风格编程助手。
 *
 * @author EvoX Team
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class ShellCommandTool extends BaseTool {

    private String workingDirectory;
    private long timeoutSeconds;
    private boolean allowAllCommands;

    public ShellCommandTool() {
        this(System.getProperty("user.dir"), 120L, true);
    }

    public ShellCommandTool(String workingDirectory, long timeoutSeconds, boolean allowAllCommands) {
        this.name = "shell_command";
        this.description = "Execute a shell command in the project directory. Use for build (mvn, npm), version control (git), running tests, or any CLI tool.";
        this.workingDirectory = workingDirectory != null ? workingDirectory : System.getProperty("user.dir");
        this.timeoutSeconds = timeoutSeconds > 0 ? timeoutSeconds : 120L;
        this.allowAllCommands = allowAllCommands;

        this.inputs = new HashMap<>();
        Map<String, String> cmdParam = new HashMap<>();
        cmdParam.put("type", "string");
        cmdParam.put("description", "The shell command to run (e.g. mvn test, git status, npm run build)");
        this.inputs.put("command", cmdParam);
        Map<String, String> dirParam = new HashMap<>();
        dirParam.put("type", "string");
        dirParam.put("description", "Optional working directory (relative to project root); omit to use default");
        this.inputs.put("workDir", dirParam);
        this.required = List.of("command");
    }

    @Override
    public ToolResult execute(Map<String, Object> params) {
        String command = (String) params.get("command");
        if (command == null || command.trim().isEmpty()) {
            return ToolResult.failure("command is required");
        }
        String workDir = (String) params.get("workDir");
        Path cwd = workDir != null && !workDir.isBlank()
                ? Paths.get(workingDirectory).resolve(workDir).normalize()
                : Paths.get(workingDirectory);

        if (!allowAllCommands) {
            if (!isAllowedCommand(command)) {
                return ToolResult.failure("Command not allowed: " + command);
            }
        }

        ProcessBuilder pb = new ProcessBuilder();
        String shell = System.getProperty("os.name").toLowerCase().startsWith("win") ? "cmd.exe" : "/bin/sh";
        String flag = System.getProperty("os.name").toLowerCase().startsWith("win") ? "/c" : "-c";
        pb.command(shell, flag, command.trim());
        pb.directory(cwd.toFile());
        pb.redirectErrorStream(true);
        pb.environment().put("LANG", "en_US.UTF-8");

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Process process = pb.start();
            Future<String> stdoutFuture = executor.submit(() -> {
                StringBuilder out = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        out.append(line).append("\n");
                    }
                }
                return out.toString();
            });

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            String stdout = stdoutFuture.get(timeoutSeconds + 5, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                Map<String, Object> meta = new HashMap<>();
                meta.put("timeout_seconds", timeoutSeconds);
                return ToolResult.failure("Command timed out after " + timeoutSeconds + " seconds", meta);
            }

            int exitCode = process.exitValue();
            Map<String, Object> data = new HashMap<>();
            data.put("stdout", stdout);
            data.put("exitCode", exitCode);
            data.put("command", command);
            data.put("workDir", cwd.toString());
            Map<String, Object> meta = new HashMap<>();
            meta.put("exitCode", exitCode);
            if (exitCode != 0) {
                return ToolResult.failure("Command exited with code " + exitCode + "\n" + stdout, meta);
            }
            return ToolResult.success(data, meta);
        } catch (TimeoutException e) {
            return ToolResult.failure("Command timed out: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ToolResult.failure("Interrupted: " + e.getMessage());
        } catch (Exception e) {
            log.error("Shell command failed: {}", command, e);
            return ToolResult.failure("Execution failed: " + e.getMessage());
        } finally {
            executor.shutdownNow();
        }
    }

    private boolean isAllowedCommand(String command) {
        String lower = command.trim().toLowerCase();
        return lower.startsWith("mvn ") || lower.startsWith("git ")
                || lower.startsWith("npm ") || lower.startsWith("python ") || lower.startsWith("python3 ")
                || lower.startsWith("javac ") || lower.startsWith("java ");
    }
}
