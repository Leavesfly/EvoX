package io.leavesfly.evox.tools.git;

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
 * Git 操作工具
 * 提供 Git 仓库的常用操作，包括状态查看、提交、分支管理、日志查看等
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class GitTool extends BaseTool {

    private String workingDirectory;
    private long timeoutSeconds;

    public GitTool() {
        this(System.getProperty("user.dir"));
    }

    public GitTool(String workingDirectory) {
        super();
        this.name = "git";
        this.description = "Perform Git operations including status, diff, log, commit, branch management, and more.";
        this.workingDirectory = workingDirectory;
        this.timeoutSeconds = 30;

        this.inputs = new HashMap<>();
        this.required = new ArrayList<>();

        Map<String, String> operationParam = new HashMap<>();
        operationParam.put("type", "string");
        operationParam.put("description", "Git operation: status, diff, log, add, commit, branch, checkout, show, blame, push, pull, fetch, stash, merge, rebase, tag");
        this.inputs.put("operation", operationParam);
        this.required.add("operation");

        Map<String, String> argsParam = new HashMap<>();
        argsParam.put("type", "string");
        argsParam.put("description", "Additional arguments for the git command (optional)");
        this.inputs.put("args", argsParam);

        Map<String, String> messageParam = new HashMap<>();
        messageParam.put("type", "string");
        messageParam.put("description", "Commit message (required for 'commit' operation)");
        this.inputs.put("message", messageParam);
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        validateParameters(parameters);
        String operation = getParameter(parameters, "operation", "");
        String args = getParameter(parameters, "args", "");
        String message = getParameter(parameters, "message", "");

        if (operation.isBlank()) {
            return ToolResult.failure("Git operation cannot be empty");
        }

        return switch (operation) {
            case "status" -> executeGit("status" + (args.isBlank() ? "" : " " + args));
            case "diff" -> executeGit("diff" + (args.isBlank() ? "" : " " + args));
            case "log" -> executeGit("log --oneline -20" + (args.isBlank() ? "" : " " + args));
            case "add" -> executeGit("add " + (args.isBlank() ? "." : args));
            case "commit" -> {
                if (message.isBlank()) {
                    yield ToolResult.failure("Commit message is required for 'commit' operation");
                }
                yield executeGit("commit -m \"" + message.replace("\"", "\\\"") + "\""
                        + (args.isBlank() ? "" : " " + args));
            }
            case "branch" -> executeGit("branch" + (args.isBlank() ? "" : " " + args));
            case "checkout" -> {
                if (args.isBlank()) {
                    yield ToolResult.failure("Branch name or file path is required for 'checkout' operation");
                }
                yield executeGit("checkout " + args);
            }
            case "show" -> executeGit("show" + (args.isBlank() ? "" : " " + args));
            case "blame" -> {
                if (args.isBlank()) {
                    yield ToolResult.failure("File path is required for 'blame' operation");
                }
                yield executeGit("blame " + args);
            }
            case "push" -> executeGit("push" + (args.isBlank() ? "" : " " + args));
            case "pull" -> executeGit("pull" + (args.isBlank() ? "" : " " + args));
            case "fetch" -> executeGit("fetch" + (args.isBlank() ? "" : " " + args));
            case "stash" -> executeGit("stash" + (args.isBlank() ? "" : " " + args));
            case "merge" -> {
                if (args.isBlank()) {
                    yield ToolResult.failure("Branch name is required for 'merge' operation");
                }
                yield executeGit("merge " + args);
            }
            case "rebase" -> {
                if (args.isBlank()) {
                    yield ToolResult.failure("Branch name is required for 'rebase' operation");
                }
                yield executeGit("rebase " + args);
            }
            case "tag" -> executeGit("tag" + (args.isBlank() ? "" : " " + args));
            default -> ToolResult.failure("Unknown git operation: " + operation
                    + ". Supported: status, diff, log, add, commit, branch, checkout, show, blame, push, pull, fetch, stash, merge, rebase, tag");
        };
    }

    private ToolResult executeGit(String gitArgs) {
        try {
            Path workDir = Paths.get(workingDirectory);
            if (!Files.isDirectory(workDir)) {
                return ToolResult.failure("Working directory does not exist: " + workingDirectory);
            }

            ProcessBuilder processBuilder = new ProcessBuilder();
            String os = System.getProperty("os.name").toLowerCase();
            String fullCommand = "git " + gitArgs;
            if (os.contains("win")) {
                processBuilder.command("cmd", "/c", fullCommand);
            } else {
                processBuilder.command("sh", "-c", fullCommand);
            }
            processBuilder.directory(workDir.toFile());
            processBuilder.redirectErrorStream(false);

            Process process = processBuilder.start();

            CompletableFuture<String> stdoutFuture = readStreamAsync(process.getInputStream());
            CompletableFuture<String> stderrFuture = readStreamAsync(process.getErrorStream());

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                return ToolResult.failure("Git command timed out after " + timeoutSeconds + " seconds");
            }

            String stdout = stdoutFuture.get(5, TimeUnit.SECONDS);
            String stderr = stderrFuture.get(5, TimeUnit.SECONDS);
            int exitCode = process.exitValue();

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("exitCode", exitCode);
            result.put("output", stdout);
            if (!stderr.isBlank()) {
                result.put("stderr", stderr);
            }

            if (exitCode != 0) {
                return ToolResult.failure("Git command failed (exit code " + exitCode + "): " + stderr);
            }

            return ToolResult.success(result);

        } catch (IOException e) {
            log.error("Error executing git command", e);
            return ToolResult.failure("Failed to execute git command: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ToolResult.failure("Git command interrupted");
        } catch (Exception e) {
            log.error("Unexpected error executing git command", e);
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
}