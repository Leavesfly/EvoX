package io.leavesfly.evox.cowork.config;

import io.leavesfly.evox.models.config.LLMConfig;
import io.leavesfly.evox.models.config.LLMConfigs;
import lombok.Data;

import java.nio.file.Paths;
import java.util.*;

@Data
public class CoworkConfig {
    private String workingDirectory;
    private LLMConfig llmConfig;
    private int maxIterations = 100;
    private int maxHistoryMessages = 200;
    private boolean requireApproval = true;
    
    // 自动批准的工具列表（安全只读工具）
    private Set<String> autoApprovedTools = new HashSet<>(Arrays.asList(
            "file_system", "grep", "glob", "web_search", "http"
    ));
    // 需要批准的工具列表（具有副作用的工具）
    private Set<String> approvalRequiredTools = new HashSet<>(Arrays.asList(
            "shell", "file_edit"
    ));
    // 危险命令黑名单
    private Set<String> blockedCommands = new HashSet<>(Arrays.asList(
            "rm -rf /", "mkfs", "dd if=/dev/zero", ":(){ :|:& };:"
    ));
    private String systemPrompt;
    private boolean streamingEnabled = true;
    private String pluginDirectory = Paths.get(System.getProperty("user.home"), ".evox", "cowork", "plugins").toString();
    private String connectorConfigPath = Paths.get(System.getProperty("user.home"), ".evox", "cowork", "connectors.yaml").toString();
    private boolean sandboxEnabled = true;
    private List<String> allowedDirectories = new ArrayList<>();
    private List<String> networkAllowlist = new ArrayList<>();
    private boolean deleteProtectionEnabled = true;

    /** 子代理最大递归深度（防止无限递归） */
    private int maxSubAgentDepth = 3;

    // 创建默认配置（OpenAI）
    public static CoworkConfig createDefault(String workingDirectory) {
        CoworkConfig config = new CoworkConfig();
        config.setWorkingDirectory(workingDirectory);
        String apiKey = System.getenv("OPENAI_API_KEY");
        config.setLlmConfig(LLMConfigs.openAI(apiKey, "gpt-4o"));
        return config;
    }

    // 创建阿里云配置（通义千问）
    public static CoworkConfig createWithAliyun(String workingDirectory, String apiKey) {
        CoworkConfig config = new CoworkConfig();
        config.setWorkingDirectory(workingDirectory);
        config.setLlmConfig(LLMConfigs.aliyun(apiKey, "qwen-max"));
        return config;
    }

    // 创建 Ollama 本地模型配置
    public static CoworkConfig createWithOllama(String workingDirectory, String model) {
        CoworkConfig config = new CoworkConfig();
        config.setWorkingDirectory(workingDirectory);
        config.setLlmConfig(LLMConfigs.ollama(model));
        config.setRequireApproval(false); // 本地模型默认不强制审批
        return config;
    }

    // 检查工具是否需要审批
    public boolean isApprovalRequired(String toolName) {
        if (!requireApproval) {
            return false;
        }
        if (autoApprovedTools.contains(toolName)) {
            return false;
        }
        if (approvalRequiredTools.contains(toolName)) {
            return true;
        }
        return true; // 默认需要审批
    }

    /**
     * 验证配置合法性
     */
    public void validate() {
        if (workingDirectory == null || workingDirectory.isBlank()) {
            throw new IllegalStateException("workingDirectory must not be empty");
        }
        if (llmConfig == null) {
            throw new IllegalStateException("llmConfig must not be null");
        }
        if (maxIterations < 1 || maxIterations > 1000) {
            throw new IllegalStateException("maxIterations must be between 1 and 1000");
        }
        if (maxHistoryMessages < 1 || maxHistoryMessages > 10000) {
            throw new IllegalStateException("maxHistoryMessages must be between 1 and 10000");
        }
        if (maxSubAgentDepth < 0 || maxSubAgentDepth > 10) {
            throw new IllegalStateException("maxSubAgentDepth must be between 0 and 10");
        }
    }
}