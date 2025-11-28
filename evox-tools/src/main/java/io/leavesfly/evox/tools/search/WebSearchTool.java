package io.leavesfly.evox.tools.search;

import io.leavesfly.evox.tools.base.BaseTool;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 网络搜索工具 - 提供基础的网页搜索功能
 * 对应 Python 版本的 search_base.py
 * 
 * 注意：这是一个简化版本，使用简单的网页抓取
 * 生产环境建议使用专业的搜索 API（如 Google Custom Search, Bing API 等）
 */
@Slf4j
public class WebSearchTool extends BaseTool {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";
    private static final int MAX_CONTENT_LENGTH = 500;
    private static final int DEFAULT_NUM_RESULTS = 5;

    public WebSearchTool() {
        this.name = "web_search";
        this.description = "Search the web and retrieve content from web pages";
        
        this.inputs = new HashMap<>();
        
        Map<String, String> queryDef = new HashMap<>();
        queryDef.put("type", "string");
        queryDef.put("description", "Search query");
        inputs.put("query", queryDef);
        
        Map<String, String> numResultsDef = new HashMap<>();
        numResultsDef.put("type", "integer");
        numResultsDef.put("description", "Number of results to return (default: 5)");
        inputs.put("num_results", numResultsDef);
        
        Map<String, String> fetchContentDef = new HashMap<>();
        fetchContentDef.put("type", "boolean");
        fetchContentDef.put("description", "Whether to fetch page content (default: false)");
        inputs.put("fetch_content", fetchContentDef);
        
        this.required = List.of("query");
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        try {
            validateParameters(parameters);
            
            String query = getParameter(parameters, "query", "");
            Integer numResults = getParameter(parameters, "num_results", DEFAULT_NUM_RESULTS);
            Boolean fetchContent = getParameter(parameters, "fetch_content", false);
            
            if (query.isEmpty()) {
                return ToolResult.failure("Query cannot be empty");
            }
            
            List<Map<String, Object>> results = performSearch(query, numResults, fetchContent);
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("query", query);
            metadata.put("num_results", results.size());
            metadata.put("fetch_content", fetchContent);
            
            return ToolResult.success(results, metadata);
            
        } catch (Exception e) {
            log.error("Error executing web search", e);
            return ToolResult.failure(e.getMessage());
        }
    }

    /**
     * 执行搜索（简化版本，使用 DuckDuckGo HTML）
     */
    private List<Map<String, Object>> performSearch(String query, int numResults, boolean fetchContent) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        try {
            // 使用 DuckDuckGo HTML 版本进行搜索（不需要 API key）
            String searchUrl = "https://html.duckduckgo.com/html/?q=" + 
                             URLEncoder.encode(query, StandardCharsets.UTF_8);
            
            String html = fetchPage(searchUrl);
            
            // 解析搜索结果
            List<SearchResult> searchResults = parseSearchResults(html);
            
            // 限制结果数量
            int count = Math.min(numResults, searchResults.size());
            for (int i = 0; i < count; i++) {
                SearchResult sr = searchResults.get(i);
                
                Map<String, Object> result = new HashMap<>();
                result.put("title", sr.title);
                result.put("url", sr.url);
                result.put("snippet", sr.snippet);
                
                // 如果需要获取页面内容
                if (fetchContent && sr.url != null) {
                    try {
                        String content = fetchPageContent(sr.url);
                        result.put("content", truncateContent(content, MAX_CONTENT_LENGTH));
                    } catch (Exception e) {
                        log.debug("Failed to fetch content from {}", sr.url, e);
                        result.put("content", "");
                    }
                }
                
                results.add(result);
            }
            
        } catch (Exception e) {
            log.error("Search failed for query: {}", query, e);
        }
        
        return results;
    }

    /**
     * 抓取页面
     */
    private String fetchPage(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line).append("\n");
            }
            return response.toString();
        }
    }

    /**
     * 解析 DuckDuckGo 搜索结果
     */
    private List<SearchResult> parseSearchResults(String html) {
        List<SearchResult> results = new ArrayList<>();
        
        // 简单的正则表达式解析（实际应用中建议使用 Jsoup 等 HTML 解析库）
        Pattern resultPattern = Pattern.compile(
            "<a.*?class=\"result__a\".*?href=\"(.*?)\".*?>(.*?)</a>", 
            Pattern.DOTALL);
        Pattern snippetPattern = Pattern.compile(
            "<a class=\"result__snippet\".*?>(.*?)</a>", 
            Pattern.DOTALL);
        
        Matcher resultMatcher = resultPattern.matcher(html);
        Matcher snippetMatcher = snippetPattern.matcher(html);
        
        while (resultMatcher.find() && results.size() < 20) {
            String url = resultMatcher.group(1);
            String title = cleanHtml(resultMatcher.group(2));
            String snippet = "";
            
            if (snippetMatcher.find()) {
                snippet = cleanHtml(snippetMatcher.group(1));
            }
            
            results.add(new SearchResult(title, url, snippet));
        }
        
        return results;
    }

    /**
     * 获取页面文本内容
     */
    private String fetchPageContent(String urlStr) throws Exception {
        String html = fetchPage(urlStr);
        
        // 移除脚本和样式标签
        html = html.replaceAll("<script.*?</script>", "");
        html = html.replaceAll("<style.*?</style>", "");
        
        // 提取文本内容
        return cleanHtml(html);
    }

    /**
     * 清理 HTML 标签
     */
    private String cleanHtml(String html) {
        if (html == null) {
            return "";
        }
        
        // 移除 HTML 标签
        String text = html.replaceAll("<[^>]+>", " ");
        
        // 解码 HTML 实体
        text = text.replace("&nbsp;", " ")
                   .replace("&amp;", "&")
                   .replace("&lt;", "<")
                   .replace("&gt;", ">")
                   .replace("&quot;", "\"")
                   .replace("&#39;", "'");
        
        // 清理多余空白
        text = text.replaceAll("\\s+", " ").trim();
        
        return text;
    }

    /**
     * 截断内容
     */
    private String truncateContent(String content, int maxWords) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        
        String[] words = content.split("\\s+");
        if (words.length <= maxWords) {
            return content;
        }
        
        StringBuilder truncated = new StringBuilder();
        for (int i = 0; i < maxWords; i++) {
            if (i > 0) {
                truncated.append(" ");
            }
            truncated.append(words[i]);
        }
        truncated.append(" ...");
        
        return truncated.toString();
    }

    /**
     * 搜索结果内部类
     */
    private static class SearchResult {
        String title;
        String url;
        String snippet;

        SearchResult(String title, String url, String snippet) {
            this.title = title;
            this.url = url;
            this.snippet = snippet;
        }
    }
}
