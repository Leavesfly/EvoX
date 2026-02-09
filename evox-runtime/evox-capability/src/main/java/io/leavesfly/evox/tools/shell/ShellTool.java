package io.leavesfly.evox.tools.shell;

import io.leavesfly.evox.tools.base.BaseTool;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Shell 命令执行工具
 * 提供在指定工作目录下执行 Shell 命令的能力，支持超时控制和安全拦截
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class ShellTool extends BaseTool {

    private String workingDirectory;
    private long timeoutSeconds;
    private Set<String> blockedCommands;

    public ShellTool() {
        this(System.getProperty("user.dir"));
    }

    public ShellTool(String workingDirectory) {
        this(workingDirectory, 30);
    }

    public ShellTool(String workingDirectory, long timeoutSeconds) {
        super();
        this.name = "shell";
        this.description = "Execute shell commands in the working directory. "
                + "Supports timeout control and dangerous command blocking.";
        this.workingDirectory = workingDirectory;
        this.timeoutSeconds = timeoutSeconds;
        this.blockedCommands = new HashSet<>(Arrays.asList(
                "rm -rf /", "mkfs", "dd if=/dev/zero", ":(){ :|:& };:"
        ));

        this.inputs = new HashMap<>();
        this.required = new ArrayList<>();

        Map<String, String> commandParam = new HashMap<>();
        commandParam.put("type", "string");
        commandParam.put("description", "The shell command to execute");
        this.inputs.put("command", commandParam);
        this.required.add("command");

        Map<String, String> cwdParam = new HashMap<>();
        cwdParam.put("type", "string");
        cwdParam.put("description", "Working directory for the command (optional, defaults to project root)");
        this.inputs.put("cwd", cwdParam);

        Map<String, String> timeoutParam = new HashMap<>();
        timeoutParam.put("type", "integer");
        timeoutParam.put("description", "Timeout in seconds (optional, defaults to 30)");
        this.inputs.put("timeout", timeoutParam);
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        validateParameters(parameters);
        String command = getParameter(parameters, "command", "");
        String cwd = getParameter(parameters, "cwd", workingDirectory);
        Number timeoutValue = getParameter(parameters, "timeout", null);
        long timeout = timeoutValue != null ? timeoutValue.longValue() : timeoutSeconds;

        if (command.isBlank()) {
            return ToolResult.failure("Command cannot be empty");
        }

        if (isBlockedCommand(command)) {
            return ToolResult.failure("Command is blocked for safety: " + command);
        }

        return executeCommand(command, cwd, timeout);
    }

    private ToolResult executeCommand(String command, String cwd, long timeout) {
        try {
            Path workDir = Paths.get(cwd);
            if (!Files.isDirectory(workDir)) {
                return ToolResult.failure("Working directory does not exist: " + cwd);
            }

            ProcessBuilder processBuilder = new ProcessBuilder();
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                processBuilder.command("cmd", "/c", command);
            } else {
                processBuilder.command("sh", "-c", command);
            }
            processBuilder.directory(workDir.toFile());
            processBuilder.redirectErrorStream(false);

            Process process = processBuilder.start();

            CompletableFuture<String> stdoutFuture = readStreamAsync(process.getInputStream());
            CompletableFuture<String> stderrFuture = readStreamAsync(process.getErrorStream());

            boolean finished = process.waitFor(timeout, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                String partialStdout = stdoutFuture.getNow("");
                return ToolResult.failure("Command timed out after " + timeout + " seconds. Partial output: " + partialStdout);
            }

            String stdout = stdoutFuture.get(5, TimeUnit.SECONDS);
            String stderr = stderrFuture.get(5, TimeUnit.SECONDS);
            int exitCode = process.exitValue();

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("exitCode", exitCode);
            result.put("stdout", stdout);
            if (!stderr.isBlank()) {
                result.put("stderr", stderr);
            }

            if (exitCode != 0) {
                return ToolResult.failure("Command exited with code " + exitCode
                        + "\nstdout: " + stdout + "\nstderr: " + stderr);
            }

            return ToolResult.success(result);

        } catch (IOException e) {
            log.error("Error executing command: {}", command, e);
            return ToolResult.failure("Failed to execute command: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ToolResult.failure("Command execution interrupted");
        } catch (Exception e) {
            log.error("Unexpected error executing command: {}", command, e);
            return ToolResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    private CompletableFuture<String> readStreamAsync(InputStream inputStream) {
        return CompletableFuture.supplyAsync(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
                return output.toString().stripTrailing();
            } catch (IOException e) {
                return "Error reading output: " + e.getMessage();
            }
        });
    }

    private boolean isBlockedCommand(String command) {
        String normalizedCommand = command.trim().toLowerCase();
        for (String blocked : blockedCommands) {
            if (normalizedCommand.contains(blocked.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 添加被阻止的命令模式
     */
    public void addBlockedCommand(String commandPattern) {
        blockedCommands.add(commandPattern);
    }
}
