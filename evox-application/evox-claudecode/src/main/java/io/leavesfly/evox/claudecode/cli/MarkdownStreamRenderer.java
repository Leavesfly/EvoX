package io.leavesfly.evox.claudecode.cli;

/**
 * 流式 Markdown 终端渲染器
 * 逐字符/逐 token 接收 LLM 输出，实时渲染为 ANSI 着色的终端文本。
 *
 * 支持的 Markdown 语法：
 * - 代码块（```language ... ```）→ 灰色背景 + 语言标签
 * - 行内代码（`code`）→ 青色
 * - 标题（# ## ### ####）→ 粗体 + 颜色
 * - 粗体（**text**）→ ANSI 粗体
 * - 斜体（*text*）→ ANSI 暗淡
 * - 无序列表（- item）→ 绿色圆点
 * - 有序列表（1. item）→ 绿色数字
 * - 分隔线（---）→ 横线
 */
public class MarkdownStreamRenderer {

    // ANSI codes
    private static final String RESET = "\033[0m";
    private static final String BOLD = "\033[1m";
    private static final String DIM = "\033[2m";
    private static final String ITALIC = "\033[3m";
    private static final String CYAN = "\033[36m";
    private static final String GREEN = "\033[32m";
    private static final String YELLOW = "\033[33m";
    private static final String MAGENTA = "\033[35m";
    private static final String WHITE = "\033[37m";
    private static final String BG_GRAY = "\033[48;5;236m";
    private static final String FG_GRAY = "\033[38;5;245m";
    private static final String FG_BRIGHT_WHITE = "\033[97m";

    private final boolean colorEnabled;

    /** 行缓冲区：累积当前行的文本，在换行时统一渲染 */
    private final StringBuilder lineBuffer = new StringBuilder();

    /** 是否在围栏代码块内 */
    private boolean inCodeBlock = false;

    /** 代码块的语言标签 */
    private String codeBlockLanguage = "";

    /** 是否是代码块的第一行（紧跟 ``` 之后） */
    private boolean codeBlockFirstLine = true;

    /** 是否在当前行的开头（用于检测行首语法） */
    private boolean atLineStart = true;

    /** 是否已输出过内容（用于首行不输出多余空行） */
    private boolean hasOutput = false;

    public MarkdownStreamRenderer(boolean colorEnabled) {
        this.colorEnabled = colorEnabled;
    }

    /**
     * 接收流式文本片段并渲染输出
     */
    public void feed(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);

            if (ch == '\n') {
                // 行结束 → 渲染当前行
                flushLine();
                atLineStart = true;
            } else {
                lineBuffer.append(ch);
                atLineStart = false;
            }
        }
    }

    /**
     * 刷新剩余缓冲区（流结束时调用）
     */
    public void flush() {
        if (!lineBuffer.isEmpty()) {
            // 不追加换行，因为流可能还没结束
            String line = lineBuffer.toString();
            lineBuffer.setLength(0);

            if (inCodeBlock) {
                printCodeLine(line);
            } else {
                rawPrint(renderInlineMarkdown(line));
            }
        }
    }

    /**
     * 重置状态（新对话开始时调用）
     */
    public void reset() {
        lineBuffer.setLength(0);
        inCodeBlock = false;
        codeBlockLanguage = "";
        codeBlockFirstLine = true;
        atLineStart = true;
        hasOutput = false;
    }

    // ==================== 行级渲染 ====================

    private void flushLine() {
        String line = lineBuffer.toString();
        lineBuffer.setLength(0);

        // 检测代码块围栏
        if (line.startsWith("```")) {
            if (!inCodeBlock) {
                // 进入代码块
                inCodeBlock = true;
                codeBlockLanguage = line.length() > 3 ? line.substring(3).trim() : "";
                codeBlockFirstLine = true;

                // 输出代码块头部
                String langLabel = codeBlockLanguage.isEmpty() ? "code" : codeBlockLanguage;
                rawPrint("\n" + color(FG_GRAY, "  ┌─ " + langLabel + " ") + color(FG_GRAY, "─".repeat(Math.max(0, 40 - langLabel.length()))) + "\n");
                hasOutput = true;
                return;
            } else {
                // 退出代码块
                inCodeBlock = false;
                rawPrint(color(FG_GRAY, "  └" + "─".repeat(44)) + "\n");
                codeBlockLanguage = "";
                return;
            }
        }

        if (inCodeBlock) {
            printCodeLine(line);
            rawPrint("\n");
            codeBlockFirstLine = false;
            return;
        }

        // 非代码块：渲染 Markdown 行级语法
        renderMarkdownLine(line);
    }

    private void renderMarkdownLine(String line) {
        String trimmed = line.trim();

        // 空行
        if (trimmed.isEmpty()) {
            rawPrint("\n");
            hasOutput = true;
            return;
        }

        // 分隔线 (--- or ***)
        if (trimmed.matches("^[-*_]{3,}$")) {
            rawPrint(color(DIM, "  " + "─".repeat(44)) + "\n");
            hasOutput = true;
            return;
        }

        // 标题 (# ## ### ####)
        if (trimmed.startsWith("#")) {
            int level = 0;
            while (level < trimmed.length() && trimmed.charAt(level) == '#') {
                level++;
            }
            if (level <= 4 && level < trimmed.length() && trimmed.charAt(level) == ' ') {
                String headerText = trimmed.substring(level + 1);
                String headerColor = switch (level) {
                    case 1 -> BOLD + MAGENTA;
                    case 2 -> BOLD + CYAN;
                    case 3 -> BOLD + YELLOW;
                    default -> BOLD + WHITE;
                };
                rawPrint("\n" + color(headerColor, headerText) + "\n");
                hasOutput = true;
                return;
            }
        }

        // 无序列表 (- item 或 * item)
        if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
            String indent = extractLeadingSpaces(line);
            String itemText = trimmed.substring(2);
            rawPrint(indent + color(GREEN, "  • ") + renderInlineMarkdown(itemText) + "\n");
            hasOutput = true;
            return;
        }

        // 有序列表 (1. item)
        if (trimmed.matches("^\\d+\\.\\s.*")) {
            String indent = extractLeadingSpaces(line);
            int dotIndex = trimmed.indexOf('.');
            String number = trimmed.substring(0, dotIndex);
            String itemText = trimmed.substring(dotIndex + 2);
            rawPrint(indent + color(GREEN, "  " + number + ". ") + renderInlineMarkdown(itemText) + "\n");
            hasOutput = true;
            return;
        }

        // 引用 (> text)
        if (trimmed.startsWith("> ")) {
            String quoteText = trimmed.substring(2);
            rawPrint(color(DIM, "  │ ") + color(DIM + ITALIC, renderInlineMarkdown(quoteText)) + "\n");
            hasOutput = true;
            return;
        }

        // 普通段落
        rawPrint(renderInlineMarkdown(line) + "\n");
        hasOutput = true;
    }

    // ==================== 行内格式渲染 ====================

    /**
     * 渲染行内 Markdown 格式：粗体、斜体、行内代码
     */
    private String renderInlineMarkdown(String text) {
        if (!colorEnabled) {
            return text;
        }

        StringBuilder result = new StringBuilder();
        int i = 0;

        while (i < text.length()) {
            // 行内代码 `code`
            if (text.charAt(i) == '`') {
                int end = text.indexOf('`', i + 1);
                if (end > i) {
                    String code = text.substring(i + 1, end);
                    result.append(CYAN).append(code).append(RESET);
                    i = end + 1;
                    continue;
                }
            }

            // 粗体 **text**
            if (i + 1 < text.length() && text.charAt(i) == '*' && text.charAt(i + 1) == '*') {
                int end = text.indexOf("**", i + 2);
                if (end > i) {
                    String boldText = text.substring(i + 2, end);
                    result.append(BOLD).append(boldText).append(RESET);
                    i = end + 2;
                    continue;
                }
            }

            // 斜体 *text* (单个 *)
            if (text.charAt(i) == '*' && (i + 1 < text.length()) && text.charAt(i + 1) != '*') {
                int end = text.indexOf('*', i + 1);
                if (end > i && (end + 1 >= text.length() || text.charAt(end + 1) != '*')) {
                    String italicText = text.substring(i + 1, end);
                    result.append(DIM).append(ITALIC).append(italicText).append(RESET);
                    i = end + 1;
                    continue;
                }
            }

            // 普通字符
            result.append(text.charAt(i));
            i++;
        }

        return result.toString();
    }

    // ==================== 代码块渲染 ====================

    private void printCodeLine(String line) {
        if (colorEnabled) {
            rawPrint(FG_GRAY + "  │ " + RESET + BG_GRAY + FG_BRIGHT_WHITE + line + RESET);
        } else {
            rawPrint("  | " + line);
        }
    }

    // ==================== 工具方法 ====================

    private String color(String ansiCode, String text) {
        return colorEnabled ? ansiCode + text + RESET : text;
    }

    private String extractLeadingSpaces(String line) {
        int count = 0;
        while (count < line.length() && line.charAt(count) == ' ') {
            count++;
        }
        return " ".repeat(count);
    }

    private void rawPrint(String text) {
        System.out.print(text);
        System.out.flush();
    }
}
