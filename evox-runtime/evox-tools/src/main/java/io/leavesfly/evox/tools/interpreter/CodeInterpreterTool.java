package io.leavesfly.evox.tools.interpreter;

import io.leavesfly.evox.tools.base.BaseTool;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.script.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * 代码解释器工具 - 支持安全的代码执行
 * 对应 Python 版本的 PythonInterpreter
 * 
 * 注意：当前版本支持 JavaScript (Nashorn/GraalVM)
 * Java 环境下执行 Python 需要 Jython 或外部 Python 进程
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class CodeInterpreterTool extends BaseTool {

    private ScriptEngine scriptEngine;
    private String language = "javascript"; // "javascript", "groovy", "python"
    private Path workspacePath;
    private long executionTimeoutMs = 30000; // 30秒超时
    private boolean sandboxMode = true;
    private Set<String> allowedPackages = new HashSet<>();

    public CodeInterpreterTool() {
        this("javascript", null);
    }

    public CodeInterpreterTool(String language, Path workspacePath) {
        this.name = "code_interpreter";
        this.description = "Execute code in a sandboxed environment. Supports JavaScript, Groovy, and Python (via external process).";
        this.language = language.toLowerCase();
        this.workspacePath = workspacePath != null ? workspacePath : Paths.get("./workspace/interpreter");
        
        // 初始化输入参数定义
        this.inputs = new HashMap<>();
        Map<String, String> codeParam = new HashMap<>();
        codeParam.put("type", "string");
        codeParam.put("description", "The code to execute");
        this.inputs.put("code", codeParam);
        
        Map<String, String> langParam = new HashMap<>();
        langParam.put("type", "string");
        langParam.put("description", "Programming language (javascript, groovy, python)");
        this.inputs.put("language", langParam);
        
        Map<String, String> filesParam = new HashMap<>();
        filesParam.put("type", "object");
        filesParam.put("description", "Optional files to create in workspace before execution");
        this.inputs.put("files", filesParam);
        
        this.required = List.of("code");
        
        try {
            initializeWorkspace();
            initializeScriptEngine();
            initializeAllowedPackages();
            log.info("Code interpreter initialized for language: {}", this.language);
        } catch (Exception e) {
            log.error("Failed to initialize code interpreter: {}", e.getMessage());
            throw new RuntimeException("Code interpreter initialization failed", e);
        }
    }

    private void initializeWorkspace() throws IOException {
        if (!Files.exists(workspacePath)) {
            Files.createDirectories(workspacePath);
            log.info("Created workspace directory: {}", workspacePath);
        }
    }

    private void initializeScriptEngine() {
        ScriptEngineManager manager = new ScriptEngineManager();
        
        switch (language) {
            case "javascript":
                // 尝试使用 GraalVM JavaScript，如果不可用则回退到 Nashorn
                scriptEngine = manager.getEngineByName("graal.js");
                if (scriptEngine == null) {
                    scriptEngine = manager.getEngineByName("nashorn");
                }
                if (scriptEngine == null) {
                    scriptEngine = manager.getEngineByName("JavaScript");
                }
                break;
                
            case "groovy":
                scriptEngine = manager.getEngineByName("groovy");
                break;
                
            case "python":
                // Jython 引擎（如果可用）
                scriptEngine = manager.getEngineByName("python");
                break;
                
            default:
                throw new IllegalArgumentException("Unsupported language: " + language);
        }
        
        if (scriptEngine == null) {
            throw new RuntimeException("Script engine not available for: " + language);
        }
    }

    private void initializeAllowedPackages() {
        // 默认允许的安全包
        allowedPackages.add("java.lang");
        allowedPackages.add("java.util");
        allowedPackages.add("java.math");
        allowedPackages.add("java.time");
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        try {
            validateParameters(parameters);
            
            String code = getParameter(parameters, "code", "");
            String executionLanguage = getParameter(parameters, "language", this.language);
            Map<String, Object> files = getParameter(parameters, "files", new HashMap<>());
            
            if (code.trim().isEmpty()) {
                return ToolResult.failure("Code cannot be empty");
            }
            
            // 检查代码安全性
            if (sandboxMode && !isCodeSafe(code)) {
                return ToolResult.failure("Code contains unsafe operations");
            }
            
            // 创建工作文件
            createWorkspaceFiles(files);
            
            // 执行代码
            return executeCode(code, executionLanguage);
            
        } catch (Exception e) {
            log.error("Code execution failed: {}", e.getMessage());
            return ToolResult.failure("Execution error: " + e.getMessage());
        }
    }

    private boolean isCodeSafe(String code) {
        // 基本安全检查
        String[] dangerousPatterns = {
            "System.exit",
            "Runtime.getRuntime",
            "ProcessBuilder",
            "java.io.File.delete",
            "java.nio.file.Files.delete",
            "javax.script",
            "java.lang.reflect"
        };
        
        String lowerCode = code.toLowerCase();
        for (String pattern : dangerousPatterns) {
            if (lowerCode.contains(pattern.toLowerCase())) {
                log.warn("Unsafe code pattern detected: {}", pattern);
                return false;
            }
        }
        
        return true;
    }

    private void createWorkspaceFiles(Map<String, Object> files) throws IOException {
        for (Map.Entry<String, Object> entry : files.entrySet()) {
            String fileName = entry.getKey();
            String content = entry.getValue().toString();
            
            Path filePath = workspacePath.resolve(fileName);
            Files.writeString(filePath, content);
            log.debug("Created workspace file: {}", fileName);
        }
    }

    private ToolResult executeCode(String code, String executionLanguage) {
        if ("python".equals(executionLanguage) && scriptEngine == null) {
            // 使用外部 Python 进程执行
            return executePythonExternal(code);
        }
        
        // 使用脚本引擎执行
        return executeWithScriptEngine(code);
    }

    private ToolResult executeWithScriptEngine(String code) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<ToolResult> future = executor.submit(() -> {
            try {
                // 捕获输出
                StringWriter output = new StringWriter();
                StringWriter errorOutput = new StringWriter();
                
                ScriptContext context = new SimpleScriptContext();
                context.setWriter(output);
                context.setErrorWriter(errorOutput);
                context.setAttribute("workspace", workspacePath.toString(), ScriptContext.ENGINE_SCOPE);
                
                // 执行代码
                Object result = scriptEngine.eval(code, context);
                
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("output", output.toString());
                resultData.put("error_output", errorOutput.toString());
                resultData.put("result", result != null ? result.toString() : null);
                
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("language", language);
                metadata.put("execution_time_ms", System.currentTimeMillis());
                
                return ToolResult.success(resultData, metadata);
                
            } catch (ScriptException e) {
                log.error("Script execution error: {}", e.getMessage());
                return ToolResult.failure("Script error: " + e.getMessage());
            }
        });
        
        try {
            return future.get(executionTimeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            log.error("Code execution timeout after {} ms", executionTimeoutMs);
            return ToolResult.failure("Execution timeout");
        } catch (Exception e) {
            log.error("Code execution failed: {}", e.getMessage());
            return ToolResult.failure("Execution failed: " + e.getMessage());
        } finally {
            executor.shutdownNow();
        }
    }

    private ToolResult executePythonExternal(String code) {
        try {
            // 创建临时 Python 文件
            Path tempScript = workspacePath.resolve("temp_script.py");
            Files.writeString(tempScript, code);
            
            // 执行 Python
            ProcessBuilder processBuilder = new ProcessBuilder("python3", tempScript.toString());
            processBuilder.directory(workspacePath.toFile());
            processBuilder.redirectErrorStream(true);
            
            Process process = processBuilder.start();
            
            // 读取输出
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            boolean completed = process.waitFor(executionTimeoutMs, TimeUnit.MILLISECONDS);
            
            if (!completed) {
                process.destroyForcibly();
                return ToolResult.failure("Python execution timeout");
            }
            
            int exitCode = process.exitValue();
            
            // 清理临时文件
            Files.deleteIfExists(tempScript);
            
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("output", output.toString());
            resultData.put("exit_code", exitCode);
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("language", "python");
            metadata.put("execution_mode", "external_process");
            
            if (exitCode == 0) {
                return ToolResult.success(resultData, metadata);
            } else {
                return ToolResult.failure("Python execution failed with exit code: " + exitCode);
            }
            
        } catch (Exception e) {
            log.error("Python external execution failed: {}", e.getMessage());
            return ToolResult.failure("Python execution error: " + e.getMessage());
        }
    }

    public void setExecutionTimeout(long timeoutMs) {
        this.executionTimeoutMs = timeoutMs;
        log.info("Execution timeout set to: {} ms", timeoutMs);
    }

    public void setSandboxMode(boolean enabled) {
        this.sandboxMode = enabled;
        log.info("Sandbox mode set to: {}", enabled);
    }

    public void addAllowedPackage(String packageName) {
        allowedPackages.add(packageName);
        log.info("Added allowed package: {}", packageName);
    }

    public void cleanWorkspace() throws IOException {
        if (Files.exists(workspacePath)) {
            Files.walk(workspacePath)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        if (!path.equals(workspacePath)) {
                            Files.delete(path);
                        }
                    } catch (IOException e) {
                        log.warn("Failed to delete: {}", path);
                    }
                });
            log.info("Workspace cleaned: {}", workspacePath);
        }
    }
}
