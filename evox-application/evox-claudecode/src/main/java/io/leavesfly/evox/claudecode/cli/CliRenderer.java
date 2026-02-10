package io.leavesfly.evox.claudecode.cli;

/**
 * CLI 渲染器
 * 负责终端输出的格式化和着色
 */
public class CliRenderer {

    // ANSI color codes
    private static final String RESET = "\033[0m";
    private static final String BOLD = "\033[1m";
    private static final String DIM = "\033[2m";
    private static final String RED = "\033[31m";
    private static final String GREEN = "\033[32m";
    private static final String YELLOW = "\033[33m";
    private static final String BLUE = "\033[34m";
    private static final String MAGENTA = "\033[35m";
    private static final String CYAN = "\033[36m";
    private static final String WHITE = "\033[37m";

    private final boolean colorEnabled;
    private final boolean markdownRendering;
    private final MarkdownStreamRenderer markdownRenderer;

    public CliRenderer() {
        this(true, true);
    }

    public CliRenderer(boolean colorEnabled) {
        this(colorEnabled, true);
    }

    public CliRenderer(boolean colorEnabled, boolean markdownRendering) {
        this.colorEnabled = colorEnabled;
        this.markdownRendering = markdownRendering;
        this.markdownRenderer = new MarkdownStreamRenderer(colorEnabled);
    }

    public void printWelcome() {
        println("");
        println(bold(cyan("╔══════════════════════════════════════════════╗")));
        println(bold(cyan("║")) + bold(white("     EvoX ClaudeCode - Agentic Coding CLI    ")) + bold(cyan("║")));
        println(bold(cyan("╚══════════════════════════════════════════════╝")));
        println("");
        println(dim("  Type your request and press Enter."));
        println(dim("  Commands: /help, /clear, /compact, /quit"));
        println("");
    }

    public void printHelp() {
        println("");
        println(bold("Available Commands:"));
        println("  " + cyan("/help") + "     - Show this help message");
        println("  " + cyan("/clear") + "    - Clear conversation history");
        println("  " + cyan("/compact") + "  - Compact conversation history");
        println("  " + cyan("/tools") + "    - List available tools");
        println("  " + cyan("/context") + "  - Show project context");
        println("  " + cyan("/quit") + "     - Exit ClaudeCode");
        println("");
    }

    public void printToolList(java.util.List<String> toolNames) {
        println("");
        println(bold("Available Tools:"));
        for (String name : toolNames) {
            println("  " + green("•") + " " + name);
        }
        println("");
    }

    public void printError(String message) {
        println(red("Error: ") + message);
    }

    public void printWarning(String message) {
        println(yellow("Warning: ") + message);
    }

    public void printInfo(String message) {
        println(blue("ℹ ") + message);
    }

    public void printSuccess(String message) {
        println(green("✓ ") + message);
    }

    public void printDivider() {
        println(dim("─".repeat(50)));
    }

    public void printStream(String text) {
        if (markdownRendering) {
            markdownRenderer.feed(text);
        } else {
            print(text);
        }
    }

    /**
     * 刷新 Markdown 渲染缓冲区（流结束时调用）
     */
    public void flushStream() {
        if (markdownRendering) {
            markdownRenderer.flush();
        }
    }

    /**
     * 重置 Markdown 渲染状态（新对话开始时调用）
     */
    public void resetStream() {
        if (markdownRendering) {
            markdownRenderer.reset();
        }
    }

    public void printApprovalRequest(String toolName, java.util.Map<String, Object> parameters) {
        println("");
        println(yellow("⚠ Permission required for: ") + bold(toolName));
        if (parameters != null && !parameters.isEmpty()) {
            parameters.forEach((key, value) -> {
                String valueStr = value != null ? value.toString() : "null";
                if (valueStr.length() > 100) {
                    valueStr = valueStr.substring(0, 100) + "...";
                }
                println("  " + dim(key + ": ") + valueStr);
            });
        }
    }

    public void printHistoryCleared() {
        printSuccess("Conversation history cleared.");
    }

    public void printHistoryCompacted() {
        printSuccess("Conversation history compacted.");
    }

    public void printGoodbye() {
        println("");
        println(cyan("Goodbye! 👋"));
        println("");
    }

    // ==================== Color formatting (package-visible for ClaudeCodeRepl) ====================

    String bold(String text) {
        return colorEnabled ? BOLD + text + RESET : text;
    }

    String dim(String text) {
        return colorEnabled ? DIM + text + RESET : text;
    }

    String red(String text) {
        return colorEnabled ? RED + text + RESET : text;
    }

    String green(String text) {
        return colorEnabled ? GREEN + text + RESET : text;
    }

    String yellow(String text) {
        return colorEnabled ? YELLOW + text + RESET : text;
    }

    String blue(String text) {
        return colorEnabled ? BLUE + text + RESET : text;
    }

    String cyan(String text) {
        return colorEnabled ? CYAN + text + RESET : text;
    }

    String white(String text) {
        return colorEnabled ? WHITE + text + RESET : text;
    }

    String magenta(String text) {
        return colorEnabled ? MAGENTA + text + RESET : text;
    }

    void println(String text) {
        System.out.println(text);
    }

    void print(String text) {
        System.out.print(text);
        System.out.flush();
    }

    /**
     * 打印带格式的标题行（如 "  Token Usage:"）
     */
    public void printSectionHeader(String title) {
        println("");
        println("  " + bold(title));
        println("");
    }

    /**
     * 打印带格式的命令帮助行（如 "  /help       Show this help message"）
     */
    public void printCommandEntry(String command, String description) {
        println("  " + cyan(command) + "  " + description);
    }

    /**
     * 打印带格式的键值对行
     */
    public void printKeyValue(String key, String value) {
        println("  " + key + "  " + value);
    }
}
