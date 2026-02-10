package io.leavesfly.evox.tools.api;

import io.leavesfly.evox.tools.base.BaseTool;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/**
 * 翻译工具
 * 支持多语言文本翻译
 * 
 * @author EvoX Team
 */
@Slf4j
public class TranslationTool extends BaseTool {

    private static final String LIBRE_TRANSLATE_API = "https://libretranslate.com/translate";
    private static final String MYMEMORY_API = "https://api.mymemory.translated.net/get";
    
    private final HttpClient httpClient;
    private final String apiKey;
    private final TranslationProvider provider;
    
    /**
     * 翻译服务提供商
     */
    public enum TranslationProvider {
        MYMEMORY,    // 免费，每天1000次请求限制
        LIBRE        // 需要API Key或自建服务
    }
    
    /**
     * 支持的语言映射
     */
    private static final Map<String, String> LANGUAGE_CODES = new HashMap<>();
    
    static {
        LANGUAGE_CODES.put("中文", "zh");
        LANGUAGE_CODES.put("chinese", "zh");
        LANGUAGE_CODES.put("英文", "en");
        LANGUAGE_CODES.put("english", "en");
        LANGUAGE_CODES.put("日文", "ja");
        LANGUAGE_CODES.put("japanese", "ja");
        LANGUAGE_CODES.put("韩文", "ko");
        LANGUAGE_CODES.put("korean", "ko");
        LANGUAGE_CODES.put("法文", "fr");
        LANGUAGE_CODES.put("french", "fr");
        LANGUAGE_CODES.put("德文", "de");
        LANGUAGE_CODES.put("german", "de");
        LANGUAGE_CODES.put("西班牙文", "es");
        LANGUAGE_CODES.put("spanish", "es");
        LANGUAGE_CODES.put("俄文", "ru");
        LANGUAGE_CODES.put("russian", "ru");
        LANGUAGE_CODES.put("葡萄牙文", "pt");
        LANGUAGE_CODES.put("portuguese", "pt");
        LANGUAGE_CODES.put("意大利文", "it");
        LANGUAGE_CODES.put("italian", "it");
        LANGUAGE_CODES.put("阿拉伯文", "ar");
        LANGUAGE_CODES.put("arabic", "ar");
    }
    
    /**
     * 创建翻译工具（使用免费的MyMemory API）
     */
    public TranslationTool() {
        this(TranslationProvider.MYMEMORY, null);
    }
    
    /**
     * 创建翻译工具
     * 
     * @param provider 翻译服务提供商
     * @param apiKey API Key（某些服务需要）
     */
    public TranslationTool(TranslationProvider provider, String apiKey) {
        this.provider = provider;
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        
        this.name = "translate";
        this.description = "将文本从一种语言翻译为另一种语言";
        
        this.inputs = new HashMap<>();
        
        Map<String, String> textDef = new HashMap<>();
        textDef.put("type", "string");
        textDef.put("description", "要翻译的文本");
        inputs.put("text", textDef);
        
        Map<String, String> sourceDef = new HashMap<>();
        sourceDef.put("type", "string");
        sourceDef.put("description", "源语言代码，如 'en', 'zh', '英文', '中文'。留空则自动检测");
        inputs.put("source", sourceDef);
        
        Map<String, String> targetDef = new HashMap<>();
        targetDef.put("type", "string");
        targetDef.put("description", "目标语言代码，如 'en', 'zh', '英文', '中文'");
        inputs.put("target", targetDef);
        
        this.required = List.of("text", "target");
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        try {
            validateParameters(parameters);
            
            String text = getParameter(parameters, "text", "");
            String source = getParameter(parameters, "source", "auto");
            String target = getParameter(parameters, "target", "");
            
            if (text.isEmpty()) {
                return ToolResult.failure("翻译文本不能为空");
            }
            
            if (target.isEmpty()) {
                return ToolResult.failure("目标语言不能为空");
            }
            
            // 解析语言代码
            String sourceLang = resolveLanguageCode(source);
            String targetLang = resolveLanguageCode(target);
            
            // 执行翻译
            Map<String, Object> result = switch (provider) {
                case MYMEMORY -> translateWithMyMemory(text, sourceLang, targetLang);
                case LIBRE -> translateWithLibre(text, sourceLang, targetLang);
            };
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source_language", sourceLang);
            metadata.put("target_language", targetLang);
            metadata.put("provider", provider.name());
            metadata.put("original_length", text.length());
            
            return ToolResult.success(result, metadata);
            
        } catch (Exception e) {
            log.error("翻译失败", e);
            return ToolResult.failure("翻译失败: " + e.getMessage());
        }
    }
    
    /**
     * 使用MyMemory API翻译
     */
    private Map<String, Object> translateWithMyMemory(String text, String source, String target) throws IOException, InterruptedException {
        String langPair = source + "|" + target;
        String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8);
        String url = String.format("%s?q=%s&langpair=%s", MYMEMORY_API, encodedText, langPair);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(Duration.ofSeconds(15))
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new IOException("MyMemory API返回错误: " + response.statusCode());
        }
        
        return parseMyMemoryResponse(response.body(), text);
    }
    
    /**
     * 使用LibreTranslate API翻译
     */
    private Map<String, Object> translateWithLibre(String text, String source, String target) throws IOException, InterruptedException {
        String jsonBody = String.format(
            "{\"q\":\"%s\",\"source\":\"%s\",\"target\":\"%s\"%s}",
            escapeJson(text), 
            source, 
            target,
            apiKey != null ? ",\"api_key\":\"" + apiKey + "\"" : ""
        );
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(LIBRE_TRANSLATE_API))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .timeout(Duration.ofSeconds(15))
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new IOException("LibreTranslate API返回错误: " + response.statusCode());
        }
        
        return parseLibreResponse(response.body(), text);
    }
    
    /**
     * 解析MyMemory响应
     */
    private Map<String, Object> parseMyMemoryResponse(String responseBody, String originalText) {
        Map<String, Object> result = new HashMap<>();
        
        // 提取翻译结果
        String translatedText = extractJsonString(responseBody, "\"translatedText\":\"");
        if (translatedText != null) {
            result.put("translated_text", translatedText);
        } else {
            result.put("translated_text", "翻译失败");
        }
        
        result.put("original_text", originalText);
        
        // 提取匹配质量
        String match = extractJsonValue(responseBody, "\"match\":");
        if (match != null) {
            try {
                result.put("confidence", Double.parseDouble(match));
            } catch (NumberFormatException ignored) {}
        }
        
        return result;
    }
    
    /**
     * 解析LibreTranslate响应
     */
    private Map<String, Object> parseLibreResponse(String responseBody, String originalText) {
        Map<String, Object> result = new HashMap<>();
        
        String translatedText = extractJsonString(responseBody, "\"translatedText\":\"");
        if (translatedText != null) {
            result.put("translated_text", translatedText);
        } else {
            result.put("translated_text", "翻译失败");
        }
        
        result.put("original_text", originalText);
        
        return result;
    }
    
    /**
     * 解析语言代码
     */
    private String resolveLanguageCode(String language) {
        if (language == null || language.isEmpty() || "auto".equalsIgnoreCase(language)) {
            return "auto";
        }
        
        String lowerLang = language.toLowerCase();
        
        // 检查映射表
        if (LANGUAGE_CODES.containsKey(lowerLang)) {
            return LANGUAGE_CODES.get(lowerLang);
        }
        
        // 如果已经是语言代码，直接返回
        if (language.length() == 2) {
            return lowerLang;
        }
        
        // 默认返回原值
        return lowerLang;
    }
    
    /**
     * 从JSON中提取字符串值
     */
    private String extractJsonString(String json, String key) {
        int idx = json.indexOf(key);
        if (idx == -1) return null;
        
        int start = idx + key.length();
        int end = json.indexOf("\"", start);
        
        if (end > start) {
            return unescapeJson(json.substring(start, end));
        }
        return null;
    }
    
    /**
     * 从JSON中提取数值
     */
    private String extractJsonValue(String json, String key) {
        int idx = json.indexOf(key);
        if (idx == -1) return null;
        
        int start = idx + key.length();
        int end = start;
        
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.')) {
            end++;
        }
        
        if (start < end) {
            return json.substring(start, end);
        }
        return null;
    }
    
    /**
     * JSON字符串转义
     */
    private String escapeJson(String text) {
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }
    
    /**
     * JSON字符串反转义
     */
    private String unescapeJson(String text) {
        return text
            .replace("\\\"", "\"")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\\", "\\");
    }
    
    /**
     * 获取支持的语言列表
     */
    public static List<String> getSupportedLanguages() {
        return Arrays.asList(
            "zh - 中文",
            "en - 英文",
            "ja - 日文",
            "ko - 韩文",
            "fr - 法文",
            "de - 德文",
            "es - 西班牙文",
            "ru - 俄文",
            "pt - 葡萄牙文",
            "it - 意大利文",
            "ar - 阿拉伯文"
        );
    }
}
