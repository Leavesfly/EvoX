package io.leavesfly.evox.claudecode.cli;

import io.leavesfly.evox.claudecode.agent.CodingAgent;
import io.leavesfly.evox.claudecode.config.ClaudeCodeConfig;
import io.leavesfly.evox.claudecode.mcp.MCPConnectionManager;
import io.leavesfly.evox.claudecode.permission.PermissionManager;
import io.leavesfly.evox.memory.session.FileSessionStore;
import io.leavesfly.evox.memory.session.SessionData;
import io.leavesfly.evox.memory.session.SessionStore;
import io.leavesfly.evox.memory.session.SessionSummary;
import lombok.extern.slf4j.Slf4j;
import org.jline.reader.*;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * ClaudeCode REPL（Read-Eval-Print Loop）
 * 提供终端交互式编程体验，支持命令解析、流式输出、用户审批和会话持久化
 */
@Slf4j
public class ClaudeCodeRepl {

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private final CodingAgent agent;
    private final CliRenderer renderer;
    private final ClaudeCodeConfig config;
    private final SessionStore sessionStore;
    private final MCPConnectionManager mcpConnectionManager;
    private String currentSessionId;
    private volatile boolean running;
    /** JLine Terminal 引用，用于审批输入（避免 Scanner/JLine stdin 冲突） */
    private volatile Terminal terminal;
    /** 会话首次创建时间（避免每次保存时覆盖） */
    private Instant sessionCreatedAt;

    public ClaudeCodeRepl(ClaudeCodeConfig config) {
        this.config = config;
        this.renderer = new CliRenderer(config.isColorEnabled(), config.isMarkdownRendering());
        this.sessionStore = new FileSessionStore();

        PermissionManager permissionManager = new PermissionManager(config, this::handleApprovalRequest);

        this.agent = new CodingAgent(config, permissionManager);
        this.agent.setStreamCallback(renderer::printStream);

        // initialize MCP connection manager
        this.mcpConnectionManager = new MCPConnectionManager(agent.getToolRegistry());
        this.mcpConnectionManager.setOnToolListChanged(agent::invalidateToolDefinitionCache);

        // start a new session
        this.currentSessionId = UUID.randomUUID().toString().substring(0, 8);
        this.sessionCreatedAt = Instant.now();
    }

    /**
     * 使用指定的会话 ID 恢复会话
     */
    public ClaudeCodeRepl(ClaudeCodeConfig config, String sessionId) {
        this(config);
        restoreSession(sessionId);
    }

    /**
     * 启动 REPL 循环
     */
    public void start() {
        running = true;
        renderer.printWelcome();
        renderer.printInfo("Session: " + currentSessionId);

        try (Terminal jlineTerminal = TerminalBuilder.builder()
                .system(true)
                .build()) {

            this.terminal = jlineTerminal;

            LineReader lineReader = LineReaderBuilder.builder()
                    .terminal(jlineTerminal)
                    .parser(new DefaultParser())
                    .variable(LineReader.HISTORY_FILE, System.getProperty("user.home") + "/.evox_claudecode_history")
                    .build();

            while (running) {
                String input;
                try {
                    input = lineReader.readLine(renderer.cyan("❯") + " ");
                } catch (UserInterruptException e) {
                    renderer.printInfo("Use /quit to exit, or press Ctrl+D.");
                    continue;
                } catch (EndOfFileException e) {
                    break;
                }

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

        } catch (IOException e) {
            log.error("Failed to initialize terminal", e);
            renderer.printError("Failed to initialize terminal: " + e.getMessage());
            fallbackRepl();
        }

        // cleanup MCP connections and auto-save session on exit
        mcpConnectionManager.disconnectAll();
        saveCurrentSession();
        renderer.printGoodbye();
    }

    /**
     * 处理用户输入（非命令）
     */
    private void processUserInput(String input) {
        renderer.printDivider();
        renderer.resetStream();
        try {
            agent.chat(input);
            renderer.flushStream();
            // auto-save after each interaction
            saveCurrentSession();
        } catch (Exception e) {
            renderer.flushStream();
            log.error("Error processing input", e);
            renderer.printError("An error occurred: " + e.getMessage());
        }
        renderer.println("");
    }

    /**
     * 处理斜杠命令
     */
    private void handleCommand(String command) {
        String[] parts = command.split("\\s+", 2);
        String cmd = parts[0].toLowerCase();
        String argument = parts.length > 1 ? parts[1].trim() : null;

        switch (cmd) {
            case "/help" -> printExtendedHelp();
            case "/clear" -> {
                agent.clearHistory();
                currentSessionId = UUID.randomUUID().toString().substring(0, 8);
                sessionCreatedAt = Instant.now();
                renderer.printHistoryCleared();
                renderer.printInfo("New session: " + currentSessionId);
            }
            case "/compact" -> {
                agent.compactHistory();
                renderer.printHistoryCompacted();
            }
            case "/tools" -> renderer.printToolList(agent.getToolRegistry().getToolNames());
            case "/skills" -> renderer.printToolList(agent.getToolRegistry().getSkillRegistry().getSkillNames());
            case "/context" -> renderer.println(agent.getProjectContext().toContextSummary());
            case "/sessions" -> listSessions();
            case "/resume" -> {
                if (argument != null) {
                    restoreSession(argument);
                } else {
                    renderer.printWarning("Usage: /resume <session-id>");
                }
            }
            case "/save" -> {
                saveCurrentSession();
                renderer.printSuccess("Session saved: " + currentSessionId);
            }
            case "/usage" -> printTokenUsage();
            case "/mcp" -> handleMcpCommand(argument);
            case "/quit", "/exit", "/q" -> running = false;
            default -> renderer.printWarning("Unknown command: " + command + ". Type /help for available commands.");
        }
    }

    // ==================== Session Management ====================

    private void saveCurrentSession() {
        try {
            // preserve original createdAt if session already exists
            Instant createdAt = sessionCreatedAt;
            if (createdAt == null) {
                createdAt = Instant.now();
                sessionCreatedAt = createdAt;
            }

            SessionData sessionData = SessionData.builder()
                    .sessionId(currentSessionId)
                    .projectDirectory(config.getWorkingDirectory())
                    .messages(agent.getMemoryManager().getAllMessages())
                    .createdAt(createdAt)
                    .updatedAt(Instant.now())
                    .build();

            sessionStore.saveSession(currentSessionId, sessionData);
            log.debug("Session saved: {}", currentSessionId);
        } catch (Exception e) {
            log.warn("Failed to save session: {}", currentSessionId, e);
        }
    }

    private void restoreSession(String sessionId) {
        try {
            SessionData sessionData = sessionStore.loadSession(sessionId);
            if (sessionData == null) {
                renderer.printWarning("Session not found: " + sessionId);
                return;
            }

            agent.clearHistory();
            if (sessionData.getMessages() != null && !sessionData.getMessages().isEmpty()) {
                agent.getMemoryManager().addMessages(sessionData.getMessages());
            }

            this.currentSessionId = sessionId;
            this.sessionCreatedAt = sessionData.getCreatedAt();
            renderer.printSuccess("Restored session: " + sessionId
                    + " (" + (sessionData.getMessages() != null ? sessionData.getMessages().size() : 0) + " messages)");
        } catch (Exception e) {
            log.error("Failed to restore session: {}", sessionId, e);
            renderer.printError("Failed to restore session: " + e.getMessage());
        }
    }

    private void listSessions() {
        try {
            List<SessionSummary> sessions = sessionStore.listSessions();
            if (sessions.isEmpty()) {
                renderer.printInfo("No saved sessions found.");
                return;
            }

            renderer.printSectionHeader("Saved Sessions:");

            for (SessionSummary session : sessions) {
                String marker = session.getSessionId().equals(currentSessionId)
                        ? " " + renderer.green("(current)") : "";
                String time = session.getUpdatedAt() != null
                        ? TIME_FORMATTER.format(session.getUpdatedAt()) : "unknown";
                String lastMsg = session.getLastUserMessage() != null
                        ? session.getLastUserMessage() : "(no messages)";
                if (lastMsg.length() > 60) {
                    lastMsg = lastMsg.substring(0, 60) + "...";
                }

                renderer.println(String.format("  %s%s  [%s]  %d msgs  %s",
                        renderer.cyan(session.getSessionId()), marker, time,
                        session.getMessageCount(), renderer.dim(lastMsg)));
            }
            renderer.println("");
            renderer.println("  Use " + renderer.cyan("/resume <session-id>") + " to restore a session.");
            renderer.println("");
        } catch (Exception e) {
            log.error("Failed to list sessions", e);
            renderer.printError("Failed to list sessions: " + e.getMessage());
        }
    }

    private void printTokenUsage() {
        Map<String, Long> usage = agent.getTokenUsage();
        renderer.printSectionHeader("Token Usage:");
        renderer.println(String.format("  Prompt tokens:     %,d", usage.get("prompt_tokens")));
        renderer.println(String.format("  Completion tokens: %,d", usage.get("completion_tokens")));
        renderer.println(String.format("  Total tokens:      %,d", usage.get("total_tokens")));
        renderer.println("");
    }

    private void printExtendedHelp() {
        renderer.printSectionHeader("Available Commands:");
        renderer.printCommandEntry("/help      ", "Show this help message");
        renderer.printCommandEntry("/clear     ", "Clear history and start a new session");
        renderer.printCommandEntry("/compact   ", "Compact conversation history (LLM summary)");
        renderer.printCommandEntry("/tools     ", "List available tools");
        renderer.printCommandEntry("/skills    ", "List available skills");
        renderer.printCommandEntry("/context   ", "Show project context");
        renderer.printCommandEntry("/sessions  ", "List saved sessions");
        renderer.printCommandEntry("/resume    ", "Resume a saved session (/resume <id>)");
        renderer.printCommandEntry("/save      ", "Save current session");
        renderer.printCommandEntry("/usage     ", "Show token usage statistics");
        renderer.printCommandEntry("/mcp       ", "MCP server management:");
        renderer.println("                  /mcp connect <name> <url>");
        renderer.println("                  /mcp connect-stdio <name> <cmd> [args]");
        renderer.println("                  /mcp disconnect <name>");
        renderer.println("                  /mcp list");
        renderer.printCommandEntry("/quit      ", "Exit ClaudeCode");
        renderer.println("");
    }

    // ==================== MCP Management ====================

    private void handleMcpCommand(String argument) {
        if (argument == null || argument.isBlank()) {
            listMcpConnections();
            return;
        }

        String[] mcpParts = argument.split("\\s+", 3);
        String subCommand = mcpParts[0].toLowerCase();

        switch (subCommand) {
            case "connect" -> {
                if (mcpParts.length < 3) {
                    renderer.printWarning("Usage: /mcp connect <name> <url>");
                    renderer.printWarning("       /mcp connect-stdio <name> <command> [args...]");
                    return;
                }
                String serverName = mcpParts[1];
                String serverUrl = mcpParts[2];
                connectMcpServer(serverName, serverUrl);
            }
            case "connect-stdio" -> {
                if (mcpParts.length < 3) {
                    renderer.printWarning("Usage: /mcp connect-stdio <name> <command> [args...]");
                    return;
                }
                String serverName = mcpParts[1];
                String commandLine = mcpParts[2];
                connectMcpServerStdio(serverName, commandLine);
            }
            case "disconnect" -> {
                if (mcpParts.length < 2) {
                    renderer.printWarning("Usage: /mcp disconnect <name>");
                    return;
                }
                disconnectMcpServer(mcpParts[1]);
            }
            case "list" -> listMcpConnections();
            default -> renderer.printWarning("Unknown MCP command: " + subCommand
                    + ". Use: connect, connect-stdio, disconnect, list");
        }
    }

    private void connectMcpServer(String serverName, String serverUrl) {
        try {
            renderer.printInfo("Connecting to MCP server '" + serverName + "' at " + serverUrl + "...");
            int toolCount = mcpConnectionManager.connectRemote(serverName, serverUrl);
            renderer.printSuccess("Connected to '" + serverName + "', registered " + toolCount + " tools.");
        } catch (Exception e) {
            log.error("Failed to connect to MCP server '{}'", serverName, e);
            renderer.printError("Failed to connect: " + e.getMessage());
        }
    }

    private void connectMcpServerStdio(String serverName, String commandLine) {
        try {
            String[] parts = commandLine.split("\\s+");
            String command = parts[0];
            String[] args = parts.length > 1 ? Arrays.copyOfRange(parts, 1, parts.length) : new String[0];

            renderer.printInfo("Connecting to MCP server '" + serverName + "' via STDIO (command: " + command + ")...");
            int toolCount = mcpConnectionManager.connectStdio(serverName, command, args);
            renderer.printSuccess("Connected to '" + serverName + "', registered " + toolCount + " tools.");
        } catch (Exception e) {
            log.error("Failed to connect to STDIO MCP server '{}'", serverName, e);
            renderer.printError("Failed to connect via STDIO: " + e.getMessage());
        }
    }

    private void disconnectMcpServer(String serverName) {
        try {
            mcpConnectionManager.disconnect(serverName);
            renderer.printSuccess("Disconnected from '" + serverName + "'.");
        } catch (Exception e) {
            renderer.printError("Failed to disconnect: " + e.getMessage());
        }
    }

    private void listMcpConnections() {
        List<MCPConnectionManager.ServerSummary> connections = mcpConnectionManager.listConnections();
        if (connections.isEmpty()) {
            renderer.printInfo("No MCP servers connected. Use /mcp connect <name> <url> to connect.");
            return;
        }

        renderer.printSectionHeader("MCP Servers:");
        for (MCPConnectionManager.ServerSummary server : connections) {
            String statusText = "connected".equals(server.status())
                    ? renderer.green(server.status())
                    : renderer.red(server.status());
            String protocol = server.protocolVersion() != null ? server.protocolVersion() : "unknown";
            renderer.println(String.format("  %s  %s  protocol=%s  %d tools",
                    renderer.cyan(server.name()), statusText, protocol, server.toolCount()));
        }
        renderer.println("");
    }

    // ==================== Approval & Fallback ====================

    /**
     * 权限审批回调 - 通过 JLine Terminal 读取用户输入（避免与 LineReader 的 stdin 冲突）
     */
    private boolean handleApprovalRequest(String toolName, Map<String, Object> parameters) {
        renderer.printApprovalRequest(toolName, parameters);
        renderer.print(renderer.yellow("Allow? (y/n/always): "));

        try {
            String response;
            if (terminal != null) {
                // 使用 JLine Terminal 的 reader 读取，避免与 LineReader 的 stdin 冲突
                BufferedReader terminalReader = new BufferedReader(new InputStreamReader(terminal.input()));
                response = terminalReader.readLine();
            } else {
                // 降级模式下使用标准输入
                BufferedReader stdinReader = new BufferedReader(new InputStreamReader(System.in));
                response = stdinReader.readLine();
            }

            if (response == null) {
                return false;
            }

            return switch (response.trim().toLowerCase()) {
                case "y", "yes" -> true;
                case "always" -> {
                    agent.getPermissionManager().approveToolForSession(toolName);
                    yield true;
                }
                default -> false;
            };
        } catch (Exception e) {
            log.warn("Failed to read approval input", e);
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
            renderer.print("❯ ");

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
