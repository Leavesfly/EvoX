package io.leavesfly.evox.tools.search;

import io.leavesfly.evox.tools.base.BaseTool;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Wikipedia搜索工具
 * 使用Wikipedia API搜索和获取文章内容
 * 
 * @author EvoX Team
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class WikipediaSearchTool extends BaseTool {

    private static final String WIKIPEDIA_API_URL = "https://en.wikipedia.org/w/api.php";
    private static final int DEFAULT_NUM_RESULTS = 5;
    private static final int DEFAULT_MAX_WORDS = 500;

    /**
     * 搜索结果数量
     */
    private int numSearchPages = DEFAULT_NUM_RESULTS;

    /**
     * 内容最大词数
     */
    private Integer maxContentWords = DEFAULT_MAX_WORDS;

    /**
     * 摘要最大句数
     */
    private Integer maxSummarySentences;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public WikipediaSearchTool() {
        this.name = "wikipedia_search";
        this.description = "Search Wikipedia for relevant articles and content";
        initializeSchema();
    }

    public WikipediaSearchTool(int numSearchPages, Integer maxContentWords, Integer maxSummarySentences) {
        this();
        this.numSearchPages = numSearchPages;
        this.maxContentWords = maxContentWords;
        this.maxSummarySentences = maxSummarySentences;
    }

    private void initializeSchema() {
        Map<String, Map<String, String>> inputs = new HashMap<>();
        
        inputs.put("query", Map.of(
            "type", "string",
            "description", "The search query to look up on Wikipedia"
        ));
        
        inputs.put("num_search_pages", Map.of(
            "type", "integer",
            "description", "Number of search results to retrieve. Default: 5"
        ));
        
        inputs.put("max_content_words", Map.of(
            "type", "integer",
            "description", "Maximum number of words to include in content per result"
        ));
        
        inputs.put("max_summary_sentences", Map.of(
            "type", "integer",
            "description", "Maximum number of sentences in the summary"
        ));

        this.inputs = inputs;
        this.required = List.of("query");
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        try {
            validateParameters(parameters);
            
            String query = getParameter(parameters, "query", "");
            Integer numResults = getParameter(parameters, "num_search_pages", numSearchPages);
            Integer maxWords = getParameter(parameters, "max_content_words", maxContentWords);
            Integer maxSentences = getParameter(parameters, "max_summary_sentences", maxSummarySentences);
            
            if (query.isEmpty()) {
                return ToolResult.failure("Query cannot be empty");
            }
            
            List<Map<String, Object>> results = searchWikipedia(query, numResults, maxWords, maxSentences);
            
            Map<String, Object> data = new HashMap<>();
            data.put("results", results);
            data.put("count", results.size());
            
            return ToolResult.success(data);
            
        } catch (Exception e) {
            log.error("Wikipedia search failed", e);
            return ToolResult.failure("Wikipedia search error: " + e.getMessage());
        }
    }

    /**
     * 搜索Wikipedia
     */
    private List<Map<String, Object>> searchWikipedia(String query, int numResults, 
                                                       Integer maxWords, Integer maxSentences) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        try {
            // 第一步：搜索标题
            List<String> titles = searchTitles(query, numResults);
            
            if (titles.isEmpty()) {
                return results;
            }
            
            // 第二步：获取每个页面的详细信息
            for (String title : titles) {
                try {
                    Map<String, Object> pageInfo = getPageInfo(title, maxWords, maxSentences);
                    if (pageInfo != null) {
                        results.add(pageInfo);
                    }
                } catch (Exception e) {
                    log.debug("Failed to get page info for: {}", title, e);
                }
            }
            
        } catch (Exception e) {
            log.error("Wikipedia search failed for query: {}", query, e);
        }
        
        return results;
    }

    /**
     * 搜索标题
     */
    private List<String> searchTitles(String query, int limit) throws Exception {
        String url = String.format("%s?action=opensearch&search=%s&limit=%d&format=json",
            WIKIPEDIA_API_URL,
            URLEncoder.encode(query, StandardCharsets.UTF_8),
            limit);
        
        String response = fetchUrl(url);
        JsonNode root = objectMapper.readTree(response);
        
        List<String> titles = new ArrayList<>();
        if (root.isArray() && root.size() > 1) {
            JsonNode titlesNode = root.get(1);
            if (titlesNode.isArray()) {
                for (JsonNode titleNode : titlesNode) {
                    titles.add(titleNode.asText());
                }
            }
        }
        
        return titles;
    }

    /**
     * 获取页面信息
     */
    private Map<String, Object> getPageInfo(String title, Integer maxWords, Integer maxSentences) throws Exception {
        // 构建API请求获取页面内容和摘要
        String url = String.format("%s?action=query&prop=extracts|info&titles=%s&inprop=url&format=json&explaintext=1",
            WIKIPEDIA_API_URL,
            URLEncoder.encode(title, StandardCharsets.UTF_8));
        
        if (maxSentences != null && maxSentences > 0) {
            url += "&exsentences=" + maxSentences;
        }
        
        String response = fetchUrl(url);
        JsonNode root = objectMapper.readTree(response);
        
        JsonNode pagesNode = root.path("query").path("pages");
        if (pagesNode.isMissingNode()) {
            return null;
        }
        
        // Wikipedia API返回的pages是一个对象，键是页面ID
        Iterator<JsonNode> pages = pagesNode.elements();
        if (!pages.hasNext()) {
            return null;
        }
        
        JsonNode page = pages.next();
        String pageTitle = page.path("title").asText();
        String extract = page.path("extract").asText();
        String pageUrl = page.path("fullurl").asText();
        
        // 截断内容
        String content = truncateContent(extract, maxWords);
        
        // 获取摘要（前几句）
        String summary = extractSummary(extract, maxSentences);
        
        Map<String, Object> result = new HashMap<>();
        result.put("title", pageTitle);
        result.put("summary", summary);
        result.put("content", content);
        result.put("url", pageUrl);
        
        return result;
    }

    /**
     * 提取摘要
     */
    private String extractSummary(String text, Integer maxSentences) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        if (maxSentences == null || maxSentences <= 0) {
            return text;
        }
        
        // 简单的句子分割（基于句号）
        String[] sentences = text.split("\\. ");
        
        if (sentences.length <= maxSentences) {
            return text;
        }
        
        StringBuilder summary = new StringBuilder();
        for (int i = 0; i < maxSentences; i++) {
            summary.append(sentences[i]);
            if (!sentences[i].endsWith(".")) {
                summary.append(".");
            }
            if (i < maxSentences - 1) {
                summary.append(" ");
            }
        }
        
        return summary.toString();
    }

    /**
     * 截断内容
     */
    private String truncateContent(String content, Integer maxWords) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        
        if (maxWords == null || maxWords <= 0) {
            return content;
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
     * 获取URL内容
     */
    private String fetchUrl(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "EvoX/1.0 (Educational Purpose)");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }
}
