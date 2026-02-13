package io.leavesfly.evox.assistant.cli;

/**
 * OpenClaw CLI 终端渲染器
 * 负责终端输出的格式化、着色和美化显示
 */
public class OpenClawCliRenderer {

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

    public OpenClawCliRenderer() {
        this(true);
    }

    public OpenClawCliRenderer(boolean colorEnabled) {
        this.colorEnabled = colorEnabled;
    }

    public void printWelcome() {
        println("");
        println(bold(cyan("╔══════════════════════════════════════════════════╗")));
        println(bold(cyan("║")) + bold(white("   🐾 EvoX OpenClaw — Personal AI Assistant CLI  ")) + bold(cyan("║")));
        println(bold(cyan("╚══════════════════════════════════════════════════╝")));
        println("");
        println(dim("  Type your message and press Enter to chat."));
        println(dim("  Commands: /help, /status, /skills, /tools, /heartbeat, /quit"));
        println("");
    }

    public void printGoodbye() {
        println("");
        println(dim("  Goodbye! 🐾"));
        println("");
    }

    public void printHelp() {
        printSectionHeader("Available Commands:");
        printCommandEntry("/help       ", "Show this help message");
        printCommandEntry("/status     ", "Show system health status");
        printCommandEntry("/skills     ", "List available skills");
        printCommandEntry("/skill      ", "Execute a skill (/skill <name> <input>)");
        printCommandEntry("/tools      ", "List available tools");
        printCommandEntry("/channels   ", "List channel statuses");
        printCommandEntry("/heartbeat  ", "Show heartbeat status");
        printCommandEntry("/wake       ", "Trigger immediate heartbeat wake");
        printCommandEntry("/event      ", "Send a system event (/event <message>)");
        printCommandEntry("/evolution  ", "Show evolution capabilities status");
        printCommandEntry("/generate   ", "Generate a new skill (/generate <description>)");
        printCommandEntry("/clear      ", "Clear conversation (start new session)");
        printCommandEntry("/quit       ", "Exit OpenClaw CLI");
        println("");
    }

    public void printPrompt() {
        print(cyan("🐾 ") + bold("> "));
    }

    public void printReply(String reply) {
        println("");
        println(bold(green("  Assistant: ")) + reply);
        println("");
    }

    public void printInfo(String message) {
        println(blue("  ℹ ") + message);
    }

    public void printSuccess(String message) {
        println(green("  ✓ ") + message);
    }

    public void printWarning(String message) {
        println(yellow("  ⚠ ") + message);
    }

    public void printError(String message) {
        println(red("  ✗ ") + message);
    }

    public void printSectionHeader(String title) {
        println("");
        println("  " + bold(title));
        println("");
    }

    public void printCommandEntry(String command, String description) {
        println("  " + cyan(command) + "  " + description);
    }

    public void printKeyValue(String key, String value) {
        println("  " + bold(key) + "  " + value);
    }

    public void printDivider() {
        println(dim("  " + "─".repeat(48)));
    }

    public void println(String text) {
        System.out.println(text);
    }

    public void print(String text) {
        System.out.print(text);
        System.out.flush();
    }

    // ========== Color Helpers ==========

    public String bold(String text) {
        return colorEnabled ? BOLD + text + RESET : text;
    }

    public String dim(String text) {
        return colorEnabled ? DIM + text + RESET : text;
    }

    public String red(String text) {
        return colorEnabled ? RED + text + RESET : text;
    }

    public String green(String text) {
        return colorEnabled ? GREEN + text + RESET : text;
    }

    public String yellow(String text) {
        return colorEnabled ? YELLOW + text + RESET : text;
    }

    public String blue(String text) {
        return colorEnabled ? BLUE + text + RESET : text;
    }

    public String magenta(String text) {
        return colorEnabled ? MAGENTA + text + RESET : text;
    }

    public String cyan(String text) {
        return colorEnabled ? CYAN + text + RESET : text;
    }

    public String white(String text) {
        return colorEnabled ? WHITE + text + RESET : text;
    }
}
