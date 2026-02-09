package io.leavesfly.evox.claudecode.config;

import io.leavesfly.evox.models.config.LLMConfig;
import lombok.Data;

import java.util.*;

/**
 * ClaudeCode 配置
 * 管理 LLM 模型、工具权限、项目路径等配置
 */
@Data
public class ClaudeCodeConfig {

    /** 项目工作目录 */
    private String workingDirectory;

    /** LLM 模型配置 */
    private LLMConfig llmConfig;

    /** 最大工具调用迭代次数 */
    private int maxIterations = 50;

    /** Shell 命令超时时间（秒） */
    private long shellTimeoutSeconds = 30;

    /** 是否需要用户确认危险操作 */
    private boolean requireApproval = true;

    /** 自动批准的工具列表（不需要用户确认） */
    private Set<String> autoApprovedTools = new HashSet<>(Arrays.asList(
            "grep", "glob", "file_system", "project_context"
    ));

    /** 需要用户确认的工具列表 */
    private Set<String> approvalRequiredTools = new HashSet<>(Arrays.asList(
            "shell", "file_edit", "git"
    ));

    /** 被阻止的 Shell 命令模式 */
    private Set<String> blockedCommands = new HashSet<>(Arrays.asList(
            "rm -rf /", "mkfs", "dd if=/dev/zero", ":(){ :|:& };:"
    ));

    /** 项目规则文件名（类似 CLAUDE.md） */
    private String projectRulesFileName = "CLAUDE.md";

    /** 系统提示词 */
    private String systemPrompt;

    /** 对话历史最大条数 */
    private int maxHistoryMessages = 100;

    /** 是否启用流式输出 */
    private boolean streamingEnabled = true;

    /**
     * 创建默认配置（使用 OpenAI）
     */
    public static ClaudeCodeConfig createDefault(String workingDirectory) {
        ClaudeCodeConfig config = new ClaudeCodeConfig();
        config.setWorkingDirectory(workingDirectory);
        config.setLlmConfig(LLMConfig.ofOpenAI(
                System.getenv("OPENAI_API_KEY"),
                "gpt-4o"
        ));
        return config;
    }

    /**
     * 创建使用阿里云通义千问的配置
     */
    public static ClaudeCodeConfig createWithAliyun(String workingDirectory, String apiKey) {
        ClaudeCodeConfig config = new ClaudeCodeConfig();
        config.setWorkingDirectory(workingDirectory);
        config.setLlmConfig(LLMConfig.ofAliyun(apiKey, "qwen-max"));
        return config;
    }

    /**
     * 创建使用 Ollama 本地模型的配置
     */
    public static ClaudeCodeConfig createWithOllama(String workingDirectory, String model) {
        ClaudeCodeConfig config = new ClaudeCodeConfig();
        config.setWorkingDirectory(workingDirectory);
        config.setLlmConfig(LLMConfig.ofOllama(model));
        config.setRequireApproval(false);
        return config;
    }

    /**
     * 判断工具是否需要用户审批
     */
    public boolean isApprovalRequired(String toolName) {
        if (!requireApproval) {
            return false;
        }
        if (autoApprovedTools.contains(toolName)) {
            return false;
        }
        return approvalRequiredTools.contains(toolName);
    }
}
