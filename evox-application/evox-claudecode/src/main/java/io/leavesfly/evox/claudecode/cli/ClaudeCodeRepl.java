package io.leavesfly.evox.claudecode.cli;

import io.leavesfly.evox.claudecode.agent.CodingAgent;
import io.leavesfly.evox.claudecode.config.ClaudeCodeConfig;
import io.leavesfly.evox.claudecode.permission.PermissionManager;
import lombok.extern.slf4j.Slf4j;
import org.jline.reader.*;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.util.*;

/**
 * ClaudeCode REPL（Read-Eval-Print Loop）
 * 提供终端交互式编程体验，支持命令解析、流式输出和用户审批
 */
@Slf4j
public class ClaudeCodeRepl {

    private final CodingAgent agent;
    private final CliRenderer renderer;
    private final ClaudeCodeConfig config;
    private volatile boolean running;

    public ClaudeCodeRepl(ClaudeCodeConfig config) {
        this.config = config;
        this.renderer = new CliRenderer();

        // create permission manager with CLI-based approval callback
        PermissionManager permissionManager = new PermissionManager(config, this::handleApprovalRequest);

        this.agent = new CodingAgent(config, permissionManager);
        this.agent.setStreamCallback(renderer::printStream);
    }

    /**
     * 启动 REPL 循环
     */
    public void start() {
        running = true;
        renderer.printWelcome();

        try (Terminal terminal = TerminalBuilder.builder()
                .system(true)
                .build()) {

            LineReader lineReader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .parser(new DefaultParser())
                    .variable(LineReader.HISTORY_FILE, System.getProperty("user.home") + "/.evox_claudecode_history")
                    .build();

            while (running) {
                String input;
                try {
                    input = lineReader.readLine("\033[36m❯\033[0m ");
                } catch (UserInterruptException e) {
                    // Ctrl+C
                    renderer.printInfo("Use /quit to exit, or press Ctrl+D.");
                    continue;
                } catch (EndOfFileException e) {
                    // Ctrl+D
                    break;
                }

                if (input == null || input.isBlank()) {
                    continue;
                }

                String trimmedInput = input.trim();

                // handle slash commands
                if (trimmedInput.startsWith("/")) {
                    handleCommand(trimmedInput);
                    continue;
                }

                // process user input through the agent
                processUserInput(trimmedInput);
            }

        } catch (IOException e) {
            log.error("Failed to initialize terminal", e);
            renderer.printError("Failed to initialize terminal: " + e.getMessage());
            fallbackRepl();
        }

        renderer.printGoodbye();
    }

    /**
     * 处理用户输入（非命令）
     */
    private void processUserInput(String input) {
        renderer.printDivider();
        try {
            agent.chat(input);
        } catch (Exception e) {
            log.error("Error processing input", e);
            renderer.printError("An error occurred: " + e.getMessage());
        }
        System.out.println();
    }

    /**
     * 处理斜杠命令
     */
    private void handleCommand(String command) {
        switch (command.toLowerCase()) {
            case "/help" -> renderer.printHelp();
            case "/clear" -> {
                agent.clearHistory();
                renderer.printHistoryCleared();
            }
            case "/compact" -> {
                agent.compactHistory();
                renderer.printHistoryCompacted();
            }
            case "/tools" -> renderer.printToolList(agent.getToolRegistry().getToolNames());
            case "/skills" -> renderer.printToolList(agent.getToolRegistry().getSkillRegistry().getSkillNames());
            case "/context" -> {
                String contextSummary = agent.getProjectContext().toContextSummary();
                System.out.println(contextSummary);
            }
            case "/quit", "/exit", "/q" -> {
                running = false;
            }
            default -> renderer.printWarning("Unknown command: " + command + ". Type /help for available commands.");
        }
    }

    /**
     * 权限审批回调 - 在终端中询问用户
     */
    private boolean handleApprovalRequest(String toolName, Map<String, Object> parameters) {
        renderer.printApprovalRequest(toolName, parameters);
        System.out.print("\033[33mAllow? (y/n/always): \033[0m");
        System.out.flush();

        try {
            Scanner scanner = new Scanner(System.in);
            String response = scanner.nextLine().trim().toLowerCase();

            return switch (response) {
                case "y", "yes" -> true;
                case "always" -> {
                    agent.getPermissionManager().approveToolForSession(toolName);
                    yield true;
                }
                default -> false;
            };
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 降级 REPL（当 JLine 不可用时使用标准输入）
     */
    private void fallbackRepl() {
        renderer.printInfo("Falling back to basic input mode.");
        Scanner scanner = new Scanner(System.in);

        while (running) {
            System.out.print("❯ ");
            System.out.flush();

            if (!scanner.hasNextLine()) {
                break;
            }

            String input = scanner.nextLine();
            if (input == null || input.isBlank()) {
                continue;
            }

            String trimmedInput = input.trim();
            if (trimmedInput.startsWith("/")) {
                handleCommand(trimmedInput);
                continue;
            }

            processUserInput(trimmedInput);
        }
    }

    /**
     * 停止 REPL
     */
    public void stop() {
        running = false;
    }
}
