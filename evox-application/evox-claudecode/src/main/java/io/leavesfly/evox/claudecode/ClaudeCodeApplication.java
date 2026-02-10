package io.leavesfly.evox.claudecode;

import io.leavesfly.evox.claudecode.agent.CodingAgent;
import io.leavesfly.evox.claudecode.cli.ClaudeCodeRepl;
import io.leavesfly.evox.claudecode.config.ClaudeCodeConfig;
import io.leavesfly.evox.claudecode.permission.PermissionManager;
import io.leavesfly.evox.models.config.LLMConfigs;
import lombok.extern.slf4j.Slf4j;

/**
 * ClaudeCode 应用入口
 * 解析命令行参数，初始化配置，启动 REPL 交互循环
 *
 * 使用方式:
 *   java -jar evox-claudecode.jar                          # 使用默认配置（OpenAI）
 *   java -jar evox-claudecode.jar --provider aliyun        # 使用阿里云通义千问
 *   java -jar evox-claudecode.jar --provider ollama --model llama3  # 使用本地 Ollama
 *   java -jar evox-claudecode.jar -p "fix the bug in Main.java"    # 单次执行模式
 */
@Slf4j
public class ClaudeCodeApplication {

    public static void main(String[] args) {
        ClaudeCodeConfig config = parseConfig(args);

        if (config == null) {
            System.err.println("Failed to initialize configuration. Please check your settings.");
            System.exit(1);
        }

        // check for single-prompt mode (-p flag)
        String singlePrompt = extractSinglePrompt(args);
        if (singlePrompt != null) {
            executeSinglePrompt(config, singlePrompt);
            return;
        }

        // check for session resume mode (--resume flag)
        String resumeSessionId = getArgValue(args, "--resume", null);
        ClaudeCodeRepl repl;
        if (resumeSessionId != null) {
            repl = new ClaudeCodeRepl(config, resumeSessionId);
        } else {
            repl = new ClaudeCodeRepl(config);
        }
        repl.start();
    }

    private static ClaudeCodeConfig parseConfig(String[] args) {
        String workingDirectory = System.getProperty("user.dir");
        String provider = getArgValue(args, "--provider", "openai");
        String model = getArgValue(args, "--model", null);
        String apiKey = getArgValue(args, "--api-key", null);

        ClaudeCodeConfig config = new ClaudeCodeConfig();
        config.setWorkingDirectory(workingDirectory);

        switch (provider.toLowerCase()) {
            case "openai" -> {
                String openaiKey = apiKey != null ? apiKey : System.getenv("OPENAI_API_KEY");
                if (openaiKey == null || openaiKey.isBlank()) {
                    System.err.println("Error: OPENAI_API_KEY environment variable is not set.");
                    System.err.println("Set it with: export OPENAI_API_KEY=your-key");
                    System.err.println("Or use --provider ollama for local models.");
                    return null;
                }
                String openaiModel = model != null ? model : "gpt-4o";
                config.setLlmConfig(LLMConfigs.openAI(openaiKey, openaiModel));
            }
            case "aliyun", "dashscope", "qwen" -> {
                String aliyunKey = apiKey != null ? apiKey : System.getenv("DASHSCOPE_API_KEY");
                if (aliyunKey == null || aliyunKey.isBlank()) {
                    System.err.println("Error: DASHSCOPE_API_KEY environment variable is not set.");
                    return null;
                }
                String aliyunModel = model != null ? model : "qwen-max";
                config.setLlmConfig(LLMConfigs.aliyun(aliyunKey, aliyunModel));
            }
            case "ollama" -> {
                String ollamaModel = model != null ? model : "llama3";
                config.setLlmConfig(LLMConfigs.ollama(ollamaModel));
                config.setRequireApproval(false);
            }
            case "siliconflow" -> {
                String sfKey = apiKey != null ? apiKey : System.getenv("SILICONFLOW_API_KEY");
                if (sfKey == null || sfKey.isBlank()) {
                    System.err.println("Error: SILICONFLOW_API_KEY environment variable is not set.");
                    return null;
                }
                String sfModel = model != null ? model : "deepseek-ai/DeepSeek-V3";
                config.setLlmConfig(LLMConfigs.siliconFlow(sfKey, sfModel));
            }
            default -> {
                System.err.println("Unknown provider: " + provider);
                System.err.println("Supported providers: openai, aliyun, ollama, siliconflow");
                return null;
            }
        }

        // parse additional flags
        if (hasFlag(args, "--no-approval")) {
            config.setRequireApproval(false);
        }

        String maxIterations = getArgValue(args, "--max-iterations", null);
        if (maxIterations != null) {
            try {
                config.setMaxIterations(Integer.parseInt(maxIterations));
            } catch (NumberFormatException e) {
                System.err.println("Warning: Invalid --max-iterations value, using default.");
            }
        }

        String maxTokens = getArgValue(args, "--max-tokens", null);
        if (maxTokens != null) {
            try {
                config.setMaxTokens(Integer.parseInt(maxTokens));
            } catch (NumberFormatException e) {
                System.err.println("Warning: Invalid --max-tokens value, using default.");
            }
        }

        String temperature = getArgValue(args, "--temperature", null);
        if (temperature != null) {
            try {
                config.setTemperature(Float.parseFloat(temperature));
            } catch (NumberFormatException e) {
                System.err.println("Warning: Invalid --temperature value, using default.");
            }
        }

        String topP = getArgValue(args, "--top-p", null);
        if (topP != null) {
            try {
                config.setTopP(Float.parseFloat(topP));
            } catch (NumberFormatException e) {
                System.err.println("Warning: Invalid --top-p value, using default.");
            }
        }

        String contextWindow = getArgValue(args, "--context-window", null);
        if (contextWindow != null) {
            try {
                config.setContextWindow(Integer.parseInt(contextWindow));
            } catch (NumberFormatException e) {
                System.err.println("Warning: Invalid --context-window value, using default.");
            }
        }

        if (hasFlag(args, "--no-color")) {
            config.setColorEnabled(false);
        }

        if (hasFlag(args, "--no-markdown")) {
            config.setMarkdownRendering(false);
        }

        // apply config overrides to LLMConfig
        config.applyToLLMConfig();

        return config;
    }

    private static String extractSinglePrompt(String[] args) {
        return getArgValue(args, "-p", null);
    }

    private static void executeSinglePrompt(ClaudeCodeConfig config, String prompt) {
        PermissionManager permissionManager = new PermissionManager(config, (toolName, params) -> {
                    // in single-prompt mode, auto-approve all tools
                    return true;
                });

        CodingAgent agent = new CodingAgent(config, permissionManager);
        agent.setStreamCallback(text -> System.out.print(text));

        String response = agent.chat(prompt);
        if (response != null && !response.isBlank()) {
            System.out.println(response);
        }
    }

    private static String getArgValue(String[] args, String flag, String defaultValue) {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals(flag)) {
                return args[i + 1];
            }
        }
        return defaultValue;
    }

    private static boolean hasFlag(String[] args, String flag) {
        for (String arg : args) {
            if (arg.equals(flag)) {
                return true;
            }
        }
        return false;
    }
}
