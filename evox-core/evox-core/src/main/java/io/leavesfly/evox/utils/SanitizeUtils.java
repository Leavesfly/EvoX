package io.leavesfly.evox.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

/**
 * 文本清理工具类
 *
 * @author EvoX Team
 */
@Slf4j
public class SanitizeUtils {

    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    /**
     * 清理HTML标签
     */
    public static String removeHtmlTags(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return HTML_TAG_PATTERN.matcher(text).replaceAll("");
    }

    /**
     * 规范化空白字符
     */
    public static String normalizeWhitespace(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return WHITESPACE_PATTERN.matcher(text.trim()).replaceAll(" ");
    }

    /**
     * 清理文本
     */
    public static String sanitize(String text) {
        if (text == null) {
            return "";
        }
        
        text = removeHtmlTags(text);
        text = normalizeWhitespace(text);
        
        return text;
    }

    /**
     * 清理代码块，移除 markdown 代码块标记（包括前后可能的空白）
     */
    public static String sanitizeCode(String code) {
        if (code == null) {
            return "";
        }
        
        // 移除 markdown 代码块的开始标记（支持前后空白和语言标识）
        code = code.replaceAll("(?m)^\\s*```[a-zA-Z]*\\s*\\n?", "");
        // 移除 markdown 代码块的结束标记
        code = code.replaceAll("(?m)\\n?\\s*```\\s*$", "");
        
        return code.trim();
    }
}
