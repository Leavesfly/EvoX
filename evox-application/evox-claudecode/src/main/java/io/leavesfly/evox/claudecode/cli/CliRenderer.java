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

    public CliRenderer() {
        this(true);
    }

    public CliRenderer(boolean colorEnabled) {
        this.colorEnabled = colorEnabled;
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
        print(text);
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

    // color helper methods
    private String bold(String text) {
        return colorEnabled ? BOLD + text + RESET : text;
    }

    private String dim(String text) {
        return colorEnabled ? DIM + text + RESET : text;
    }

    private String red(String text) {
        return colorEnabled ? RED + text + RESET : text;
    }

    private String green(String text) {
        return colorEnabled ? GREEN + text + RESET : text;
    }

    private String yellow(String text) {
        return colorEnabled ? YELLOW + text + RESET : text;
    }

    private String blue(String text) {
        return colorEnabled ? BLUE + text + RESET : text;
    }

    private String cyan(String text) {
        return colorEnabled ? CYAN + text + RESET : text;
    }

    private String white(String text) {
        return colorEnabled ? WHITE + text + RESET : text;
    }

    @SuppressWarnings("unused")
    private String magenta(String text) {
        return colorEnabled ? MAGENTA + text + RESET : text;
    }

    private void println(String text) {
        System.out.println(text);
    }

    private void print(String text) {
        System.out.print(text);
        System.out.flush();
    }
}
