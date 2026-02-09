package io.leavesfly.evox.cowork.config;

import io.leavesfly.evox.models.config.LLMConfig;
import lombok.Data;

import java.util.*;

@Data
public class CoworkConfig {
    private String workingDirectory;
    private LLMConfig llmConfig;
    private int maxIterations = 100;
    private int maxHistoryMessages = 200;
    private boolean requireApproval = true;
    private Set<String> autoApprovedTools = new HashSet<>(Arrays.asList(
            "file_system", "grep", "glob", "web_search", "http"
    ));
    private Set<String> approvalRequiredTools = new HashSet<>(Arrays.asList(
            "shell", "file_edit"
    ));
    private Set<String> blockedCommands = new HashSet<>(Arrays.asList(
            "rm -rf /", "mkfs", "dd if=/dev/zero", ":(){ :|:& };:"
    ));
    private String systemPrompt;
    private boolean streamingEnabled = true;
    private String pluginDirectory = System.getProperty("user.home") + "/.evox/cowork/plugins";
    private String connectorConfigPath = System.getProperty("user.home") + "/.evox/cowork/connectors.yaml";
    private boolean sandboxEnabled = true;
    private List<String> allowedDirectories = new ArrayList<>();
    private List<String> networkAllowlist = new ArrayList<>();
    private boolean deleteProtectionEnabled = true;

    public static CoworkConfig createDefault(String workingDirectory) {
        CoworkConfig config = new CoworkConfig();
        config.setWorkingDirectory(workingDirectory);
        String apiKey = System.getenv("OPENAI_API_KEY");
        config.setLlmConfig(LLMConfig.ofOpenAI(apiKey, "gpt-4o"));
        return config;
    }

    public static CoworkConfig createWithAliyun(String workingDirectory, String apiKey) {
        CoworkConfig config = new CoworkConfig();
        config.setWorkingDirectory(workingDirectory);
        config.setLlmConfig(LLMConfig.ofAliyun(apiKey, "qwen-max"));
        return config;
    }

    public static CoworkConfig createWithOllama(String workingDirectory, String model) {
        CoworkConfig config = new CoworkConfig();
        config.setWorkingDirectory(workingDirectory);
        config.setLlmConfig(LLMConfig.ofOllama(model));
        config.setRequireApproval(false);
        return config;
    }

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
        return true;
    }
}
