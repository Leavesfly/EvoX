package io.leavesfly.evox.claudecode;

import io.leavesfly.evox.claudecode.cli.ClaudeCodeRepl;
import io.leavesfly.evox.claudecode.config.ClaudeCodeConfig;
import io.leavesfly.evox.models.config.LLMConfig;
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

        // start interactive REPL
        ClaudeCodeRepl repl = new ClaudeCodeRepl(config);
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
                config.setLlmConfig(LLMConfig.ofOpenAI(openaiKey, openaiModel));
            }
            case "aliyun", "dashscope", "qwen" -> {
                String aliyunKey = apiKey != null ? apiKey : System.getenv("DASHSCOPE_API_KEY");
                if (aliyunKey == null || aliyunKey.isBlank()) {
                    System.err.println("Error: DASHSCOPE_API_KEY environment variable is not set.");
                    return null;
                }
                String aliyunModel = model != null ? model : "qwen-max";
                config.setLlmConfig(LLMConfig.ofAliyun(aliyunKey, aliyunModel));
            }
            case "ollama" -> {
                String ollamaModel = model != null ? model : "llama3";
                config.setLlmConfig(LLMConfig.ofOllama(ollamaModel));
                config.setRequireApproval(false);
            }
            case "siliconflow" -> {
                String sfKey = apiKey != null ? apiKey : System.getenv("SILICONFLOW_API_KEY");
                if (sfKey == null || sfKey.isBlank()) {
                    System.err.println("Error: SILICONFLOW_API_KEY environment variable is not set.");
                    return null;
                }
                String sfModel = model != null ? model : "deepseek-ai/DeepSeek-V3";
                config.setLlmConfig(LLMConfig.ofSiliconFlow(sfKey, sfModel));
            }
            default -> {
                System.err.println("Unknown provider: " + provider);
                System.err.println("Supported providers: openai, aliyun, ollama, siliconflow");
                return null;
            }
        }

        // parse additional flags
        String noApproval = getArgValue(args, "--no-approval", null);
        if (noApproval != null || hasFlag(args, "--no-approval")) {
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

        return config;
    }

    private static String extractSinglePrompt(String[] args) {
        return getArgValue(args, "-p", null);
    }

    private static void executeSinglePrompt(ClaudeCodeConfig config, String prompt) {
        io.leavesfly.evox.claudecode.permission.PermissionManager permissionManager =
                new io.leavesfly.evox.claudecode.permission.PermissionManager(config, (toolName, params) -> {
                    // in single-prompt mode, auto-approve all tools
                    return true;
                });

        io.leavesfly.evox.claudecode.agent.CodingAgent agent =
                new io.leavesfly.evox.claudecode.agent.CodingAgent(config, permissionManager);
        agent.setStreamCallback(System.out::print);

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
