package io.leavesfly.evox.tools.shell;

import io.leavesfly.evox.exception.ToolException;
import io.leavesfly.evox.tools.base.BaseTool;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

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
    private Map<String, String> environmentVariables;
    private boolean requireApproval;
    private Set<String> dangerousPatterns;

    /**
     * Compiled regex patterns for blocked commands (more secure than simple string matching)
     */
    private static final List<Pattern> BLOCKED_COMMAND_PATTERNS = Arrays.asList(
            // Recursive delete root filesystem
            Pattern.compile("rm\\s+(-[rf]+|--recursive|--force).*\\s+/(\\s|$)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("rm\\s+-rf\\s+/", Pattern.CASE_INSENSITIVE),
            // Format filesystem
            Pattern.compile("mkfs\\s+.*", Pattern.CASE_INSENSITIVE),
            // Disk destroy with dd
            Pattern.compile("dd\\s+.*if=/dev/zero.*of=/dev/", Pattern.CASE_INSENSITIVE),
            Pattern.compile("dd\\s+.*of=/dev/.*if=/dev/zero", Pattern.CASE_INSENSITIVE),
            // Fork bomb
            Pattern.compile(":\\(\\)\\s*\\{\\s*:\\|:&\\s*\\};\\s*:", Pattern.CASE_INSENSITIVE),
            // Wipe filesystem
            Pattern.compile("wipefs\\s+.*", Pattern.CASE_INSENSITIVE),
            // Block device manipulation
            Pattern.compile("blockdev\\s+.*", Pattern.CASE_INSENSITIVE)
    );

    /**
     * Compiled regex patterns for dangerous commands that require approval
     */
    private static final List<Pattern> DANGEROUS_COMMAND_PATTERNS = Arrays.asList(
            // Sudo execution
            Pattern.compile("\\bsudo\\b", Pattern.CASE_INSENSITIVE),
            // Permission changes - world writable
            Pattern.compile("chmod\\s+(-R\\s+)?777\\s+", Pattern.CASE_INSENSITIVE),
            Pattern.compile("chmod\\s+(-R\\s+)?a\\+rwx\\s+", Pattern.CASE_INSENSITIVE),
            // Ownership changes
            Pattern.compile("\\bchown\\s+(-R\\s+)?", Pattern.CASE_INSENSITIVE),
            // Force kill
            Pattern.compile("kill\\s+(-9|-KILL)\\s+", Pattern.CASE_INSENSITIVE),
            // System power control
            Pattern.compile("\\b(shutdown|reboot|poweroff|halt)\\b(\\s|$)", Pattern.CASE_INSENSITIVE),
            // Disk partitioning
            Pattern.compile("\\b(fdisk|parted|gdisk)\\s+", Pattern.CASE_INSENSITIVE),
            // Format command
            Pattern.compile("\\bformat\\s+", Pattern.CASE_INSENSITIVE),
            // Network configuration changes
            Pattern.compile("\\bipconfig\\s+.*/release", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bifconfig\\s+.*down\\s*$", Pattern.CASE_INSENSITIVE),
            // Init level changes
            Pattern.compile("\\binit\\s+[06]\\b", Pattern.CASE_INSENSITIVE),
            // Service management
            Pattern.compile("\\b(systemctl|service)\\s+.*(stop|disable|restart)\\s+", Pattern.CASE_INSENSITIVE),
            // User management
            Pattern.compile("\\b(userdel|useradd|passwd)\\s+", Pattern.CASE_INSENSITIVE),
            // iptables flush
            Pattern.compile("iptables\\s+-F(\\s|$)", Pattern.CASE_INSENSITIVE),
            // Curl/bash pipe to shell (common attack vector)
            Pattern.compile("(curl|wget)\\s+.*\\|\\s*(bash|sh|zsh)", Pattern.CASE_INSENSITIVE),
            // Environment variable manipulation
            Pattern.compile("\\bexport\\s+(PATH|LD_LIBRARY_PATH|LD_PRELOAD)\\s*=", Pattern.CASE_INSENSITIVE)
    );

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
        // Keep backward compatibility with string-based blockedCommands set
        this.blockedCommands = new HashSet<>(Arrays.asList(
                "rm -rf /", "mkfs", "dd if=/dev/zero", ":(){ :|:& };:"
        ));
        this.environmentVariables = new HashMap<>();
        this.requireApproval = false;
        // Keep backward compatibility with string-based dangerousPatterns set
        this.dangerousPatterns = new HashSet<>(Arrays.asList(
                "sudo", "chmod 777", "chown", "kill -9",
                "shutdown", "reboot", "format", "fdisk"
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

        Map<String, String> envParam = new HashMap<>();
        envParam.put("type", "object");
        envParam.put("description", "Additional environment variables as key-value pairs (optional)");
        this.inputs.put("env", envParam);
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

        if (requireApproval && isDangerousCommand(command)) {
            return ToolResult.failure("This command requires manual approval as it matches a dangerous pattern: " + command
                    + "\nMatched patterns: " + dangerousPatterns);
        }

        @SuppressWarnings("unchecked")
        Map<String, String> extraEnv = getParameter(parameters, "env", null);

        return executeCommand(command, cwd, timeout, extraEnv);
    }

    private ToolResult executeCommand(String command, String cwd, long timeout, Map<String, String> extraEnv) {
        Process process = null;
        InputStream stdoutStream = null;
        InputStream stderrStream = null;
        
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

            Map<String, String> processEnv = processBuilder.environment();
            if (environmentVariables != null && !environmentVariables.isEmpty()) {
                processEnv.putAll(environmentVariables);
            }
            if (extraEnv != null && !extraEnv.isEmpty()) {
                processEnv.putAll(extraEnv);
            }

            processBuilder.redirectErrorStream(false);

            process = processBuilder.start();
            stdoutStream = process.getInputStream();
            stderrStream = process.getErrorStream();

            CompletableFuture<String> stdoutFuture = readStreamAsync(stdoutStream);
            CompletableFuture<String> stderrFuture = readStreamAsync(stderrStream);

            boolean finished = process.waitFor(timeout, TimeUnit.SECONDS);

            if (!finished) {
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
            throw ToolException.executionError("shell", "Failed to execute command: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw ToolException.executionError("shell", "Command execution interrupted", e);
        } catch (ExecutionException | TimeoutException e) {
            log.error("Error reading command output: {}", command, e);
            throw ToolException.executionError("shell", "Error reading command output: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error executing command: {}", command, e);
            throw ToolException.executionError("shell", "Unexpected error: " + e.getMessage(), e);
        } finally {
            // 确保关闭所有流
            closeQuietly(stdoutStream);
            closeQuietly(stderrStream);
            // 确保销毁进程
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }
    
    /**
     * 安静关闭流，忽略异常
     */
    private void closeQuietly(InputStream stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                log.debug("Error closing stream", e);
            }
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

    /**
     * Check if command matches blocked patterns using regex.
     * Uses compiled patterns for better security and performance.
     */
    private boolean isBlockedCommand(String command) {
        String normalizedCommand = command.trim().toLowerCase();
        
        // Use regex pattern matching for enhanced security
        for (Pattern pattern : BLOCKED_COMMAND_PATTERNS) {
            if (pattern.matcher(normalizedCommand).find()) {
                return true;
            }
        }
        
        // Fallback to string-based check for backward compatibility
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

    /**
     * 添加环境变量
     */
    public void addEnvironmentVariable(String key, String value) {
        environmentVariables.put(key, value);
    }

    /**
     * 批量添加环境变量
     */
    public void addEnvironmentVariables(Map<String, String> vars) {
        environmentVariables.putAll(vars);
    }

    /**
     * 添加需要审批的危险命令模式
     */
    public void addDangerousPattern(String pattern) {
        dangerousPatterns.add(pattern);
    }

    /**
     * Check if command matches dangerous patterns using regex.
     * Uses compiled patterns for better security and performance.
     */
    private boolean isDangerousCommand(String command) {
        String normalizedCommand = command.trim().toLowerCase();
        
        // Use regex pattern matching for enhanced security
        for (Pattern pattern : DANGEROUS_COMMAND_PATTERNS) {
            if (pattern.matcher(normalizedCommand).find()) {
                return true;
            }
        }
        
        // Fallback to string-based check for backward compatibility
        for (String pattern : dangerousPatterns) {
            if (normalizedCommand.contains(pattern.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
