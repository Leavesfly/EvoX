package io.leavesfly.evox.tools.system;

import io.leavesfly.evox.tools.base.BaseTool;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ProcessManagerTool extends BaseTool {

    public ProcessManagerTool() {
        super();
        this.name = "process_manager";
        this.description = "List running processes, get process details, or terminate processes. "
                + "Provides cross-platform process management capabilities.";

        this.inputs = new HashMap<>();
        this.required = new ArrayList<>();

        Map<String, String> operationParam = new HashMap<>();
        operationParam.put("type", "string");
        operationParam.put("description", "Operation: 'list', 'search', 'kill', 'info' (default: 'list')");
        this.inputs.put("operation", operationParam);
        this.required.add("operation");

        Map<String, String> pidParam = new HashMap<>();
        pidParam.put("type", "integer");
        pidParam.put("description", "Process ID (required for 'kill' and 'info' operations)");
        this.inputs.put("pid", pidParam);

        Map<String, String> nameParam = new HashMap<>();
        nameParam.put("type", "string");
        nameParam.put("description", "Process name filter (for 'search' operation)");
        this.inputs.put("name", nameParam);

        Map<String, String> signalParam = new HashMap<>();
        signalParam.put("type", "string");
        signalParam.put("description", "Signal to send: 'TERM', 'KILL', 'INT' (default: 'TERM', for 'kill' operation)");
        this.inputs.put("signal", signalParam);
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        validateParameters(parameters);
        String operation = getParameter(parameters, "operation", "list");

        return switch (operation) {
            case "list" -> listProcesses();
            case "search" -> searchProcesses(getParameter(parameters, "name", ""));
            case "kill" -> killProcess(parameters);
            case "info" -> processInfo(parameters);
            default -> ToolResult.failure("Unknown operation: " + operation + ". Use: list, search, kill, info");
        };
    }

    private ToolResult listProcesses() {
        try {
            ProcessHandle.allProcesses()
                    .limit(50)
                    .forEach(ph -> {});

            List<Map<String, Object>> processes = new ArrayList<>();
            ProcessHandle.allProcesses()
                    .sorted(Comparator.comparingLong(ph -> -ph.pid()))
                    .limit(50)
                    .forEach(ph -> {
                        Map<String, Object> proc = new LinkedHashMap<>();
                        proc.put("pid", ph.pid());
                        ph.info().command().ifPresent(cmd -> proc.put("command", cmd));
                        ph.info().user().ifPresent(user -> proc.put("user", user));
                        ph.info().startInstant().ifPresent(start -> proc.put("startTime", start.toString()));
                        ph.info().totalCpuDuration().ifPresent(cpu -> proc.put("cpuTime", cpu.toString()));
                        processes.add(proc);
                    });

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("totalVisible", processes.size());
            result.put("processes", processes);
            return ToolResult.success(result);
        } catch (Exception e) {
            log.error("Error listing processes", e);
            return ToolResult.failure("Failed to list processes: " + e.getMessage());
        }
    }

    private ToolResult searchProcesses(String nameFilter) {
        if (nameFilter == null || nameFilter.isBlank()) {
            return ToolResult.failure("Process name filter is required for search operation");
        }

        try {
            String lowerFilter = nameFilter.toLowerCase();
            List<Map<String, Object>> matched = new ArrayList<>();

            ProcessHandle.allProcesses()
                    .filter(ph -> ph.info().command()
                            .map(cmd -> cmd.toLowerCase().contains(lowerFilter))
                            .orElse(false))
                    .forEach(ph -> {
                        Map<String, Object> proc = new LinkedHashMap<>();
                        proc.put("pid", ph.pid());
                        ph.info().command().ifPresent(cmd -> proc.put("command", cmd));
                        ph.info().user().ifPresent(user -> proc.put("user", user));
                        ph.info().startInstant().ifPresent(start -> proc.put("startTime", start.toString()));
                        matched.add(proc);
                    });

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("filter", nameFilter);
            result.put("matchCount", matched.size());
            result.put("processes", matched);
            return ToolResult.success(result);
        } catch (Exception e) {
            log.error("Error searching processes", e);
            return ToolResult.failure("Failed to search processes: " + e.getMessage());
        }
    }

    private ToolResult killProcess(Map<String, Object> parameters) {
        Number pidNumber = getParameter(parameters, "pid", null);
        if (pidNumber == null) {
            return ToolResult.failure("'pid' is required for kill operation");
        }
        long pid = pidNumber.longValue();
        String signal = getParameter(parameters, "signal", "TERM");

        try {
            Optional<ProcessHandle> processOpt = ProcessHandle.of(pid);
            if (processOpt.isEmpty()) {
                return ToolResult.failure("Process not found: " + pid);
            }

            ProcessHandle process = processOpt.get();
            String command = process.info().command().orElse("unknown");

            boolean destroyed;
            if ("KILL".equalsIgnoreCase(signal)) {
                destroyed = process.destroyForcibly();
            } else {
                destroyed = process.destroy();
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("pid", pid);
            result.put("command", command);
            result.put("signal", signal);
            result.put("terminated", destroyed);

            if (destroyed) {
                return ToolResult.success(result);
            } else {
                return ToolResult.failure("Failed to terminate process " + pid);
            }
        } catch (Exception e) {
            log.error("Error killing process {}", pid, e);
            return ToolResult.failure("Failed to kill process: " + e.getMessage());
        }
    }

    private ToolResult processInfo(Map<String, Object> parameters) {
        Number pidNumber = getParameter(parameters, "pid", null);
        if (pidNumber == null) {
            return ToolResult.failure("'pid' is required for info operation");
        }
        long pid = pidNumber.longValue();

        try {
            Optional<ProcessHandle> processOpt = ProcessHandle.of(pid);
            if (processOpt.isEmpty()) {
                return ToolResult.failure("Process not found: " + pid);
            }

            ProcessHandle process = processOpt.get();
            ProcessHandle.Info info = process.info();

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("pid", pid);
            result.put("alive", process.isAlive());
            info.command().ifPresent(cmd -> result.put("command", cmd));
            info.commandLine().ifPresent(cmdLine -> result.put("commandLine", cmdLine));
            info.arguments().ifPresent(args -> result.put("arguments", Arrays.asList(args)));
            info.user().ifPresent(user -> result.put("user", user));
            info.startInstant().ifPresent(start -> result.put("startTime", start.toString()));
            info.totalCpuDuration().ifPresent(cpu -> result.put("cpuTime", cpu.toString()));
            process.parent().ifPresent(parent -> result.put("parentPid", parent.pid()));
            result.put("childCount", process.children().count());

            return ToolResult.success(result);
        } catch (Exception e) {
            log.error("Error getting process info for {}", pid, e);
            return ToolResult.failure("Failed to get process info: " + e.getMessage());
        }
    }
}
